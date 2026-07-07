package com.example.smartcartapp.ui.cart;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcartapp.R;
import com.example.smartcartapp.ServerConfig;
import com.example.smartcartapp.AIRecommendationAdapter;
import com.example.smartcartapp.api.RecommendService;
import com.example.smartcartapp.api.RetrofitClient;
import com.example.smartcartapp.model.CartItem;
import com.example.smartcartapp.model.Product;
import com.example.smartcartapp.model.RecommendRequest;

import com.example.smartcartapp.utils.CartManager;
import com.example.smartcartapp.api.CheckoutRequest;
import com.example.smartcartapp.api.CheckoutResponse;
import com.example.smartcartapp.api.CartApi;

import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class CartFragment extends Fragment implements CartManager.CartUpdateListener {

    private RecyclerView cartRecyclerView;
    private RecyclerView aiRecommendationRecycler;
    private CartAdapter cartAdapter;
    private AIRecommendationAdapter aiRecommendationAdapter;
    private Button checkoutButton;
    private TextView totalPriceText;
    private CartManager cartManager;
    private Random random = new Random();
    private ActivityResultLauncher<Intent> paymentLauncher;
    private RecommendService recommendService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        paymentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        List<CartItem> purchasedItems = new ArrayList<>(cartManager.getCartItems());
                        double totalAmount = cartManager.getTotalPrice();
                        
                        Toast.makeText(requireContext(), "결제가 완료되었습니다", Toast.LENGTH_SHORT).show();
                        cartManager.clearCart();
                        cartAdapter.notifyDataSetChanged();
                        updateTotalPrice();
                        
                        new AlertDialog.Builder(requireContext())
                                .setTitle("결제 완료")
                                .setMessage("결제가 성공적으로 처리되었습니다.\n\n구매해주셔서 감사합니다!")
                                .setPositiveButton("확인", null)
                                .show();
                    } else {
                        String errorMessage = "결제가 취소되었습니다";
                        if (result.getData() != null && result.getData().getStringExtra("errorMessage") != null) {
                            errorMessage = result.getData().getStringExtra("errorMessage");
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cart, container, false);
        
        cartManager = CartManager.getInstance();
        
        // 자동 새로고침 리스너 등록
        cartManager.addListener(this);
        
        cartRecyclerView = root.findViewById(R.id.cart_recycler_view);
        aiRecommendationRecycler = root.findViewById(R.id.ai_recommendation_recycler);
        checkoutButton = root.findViewById(R.id.checkout_button);
        totalPriceText = root.findViewById(R.id.total_price_text);
        
        // Retrofit 초기화
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(ServerConfig.HTTP_BASE_URL)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        recommendService = retrofit.create(RecommendService.class);
        
        setupRecyclerView();
        setupAIRecommendation();
        setupButtons();
        updateTotalPrice();
        autoGetRecommendations();
        
        return root;
    }
    
    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(cartManager.getCartItems(), this::updateTotalPrice);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        cartRecyclerView.setAdapter(cartAdapter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        refreshCartList();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 리스너 제거
        if (cartManager != null) {
            cartManager.removeListener(this);
        }
    }
    
    @Override
    public void onCartUpdated() {
        // 장바구니 변경 시 자동 UI 업데이트
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                refreshCartList();
            });
        }
    }
    
    public void refreshCartList() {
        if (cartAdapter != null && cartRecyclerView != null) {
            cartAdapter.notifyDataSetChanged();
            updateTotalPrice();
            
            // 장바구니 변경 시 자동 AI 추천 업데이트
            autoGetRecommendations();
            
            if (!cartManager.getCartItems().isEmpty()) {
                cartRecyclerView.post(() -> {
                    cartRecyclerView.scrollToPosition(cartManager.getCartItems().size() - 1);
                });
            }
        }
    }
    
    private void setupButtons() {
        checkoutButton.setOnClickListener(v -> {
            if (cartManager.getCartItems().isEmpty()) {
                Toast.makeText(requireContext(), "장바구니가 비어있습니다", Toast.LENGTH_SHORT).show();
            } else {
                processPayment();
            }
        });
    }

    private void updateTotalPrice() {
        double total = cartManager.getTotalPrice();
        totalPriceText.setText("총합: " + (int)total + "원");
    }
    
    private void processPayment() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setMessage("결제 처리 중...");
        builder.setCancelable(false);
        AlertDialog loadingDialog = builder.create();
        loadingDialog.show();
        
        // 1초 시뮬레이션 후 서버 전송
        new android.os.Handler().postDelayed(() -> {
            sendCheckoutToServer(loadingDialog);
        }, 1000);
    }
    
    private void sendCheckoutToServer(AlertDialog loadingDialog) {
        if (cartManager.getCartItems().isEmpty()) {
            if (loadingDialog != null) loadingDialog.dismiss();
            return;
        }
        
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", "UNKNOWN");
        String gender = prefs.getString("gender", "M");
        String birthDate = prefs.getString("birth_date", "1990-01-01");
        String cartId = prefs.getString("cart_id", "CART_0001");
        
        int birthYear = 1990;
        try {
            birthYear = Integer.parseInt(birthDate.substring(0, 4));
        } catch (Exception e) {
            birthYear = 1990;
        }
        
        CheckoutRequest request = new CheckoutRequest();
        request.cart_id = cartId;
        
        CheckoutRequest.Customer customer = new CheckoutRequest.Customer();
        customer.id = userId;
        customer.gender = gender;
        customer.birth_year = birthYear;
        request.customer = customer;
        
        ArrayList<CheckoutRequest.Item> items = new ArrayList<>();
        for (CartItem cartItem : cartManager.getCartItems()) {
            CheckoutRequest.Item item = new CheckoutRequest.Item();
            item.barcode = String.valueOf(cartItem.getProduct().getId());
            item.qty = cartItem.getQuantity();
            item.price = cartItem.getProduct().getPrice();
            item.name = cartItem.getProduct().getName();
            items.add(item);
        }
        request.items = items;
        
        CartApi api = RetrofitClient.getClient().create(CartApi.class);
        api.checkout(request).enqueue(new retrofit2.Callback<CheckoutResponse>() {
            @Override
            public void onResponse(retrofit2.Call<CheckoutResponse> call, retrofit2.Response<CheckoutResponse> response) {
                loadingDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null && response.body().ok) {
                    CheckoutResponse result = response.body();
                    
                    Toast.makeText(requireContext(), "결제가 완료되었습니다", Toast.LENGTH_SHORT).show();
                    
                    cartManager.clearCart();
                    cartAdapter.notifyDataSetChanged();
                    updateTotalPrice();
                    
                    // 간단한 결제 완료 메시지
                    new AlertDialog.Builder(requireContext())
                            .setTitle("결제 완료")
                            .setMessage("결제가 완료되었습니다.\n감사합니다!")
                            .setPositiveButton("확인", null)
                            .show();
                } else {
                    Toast.makeText(requireContext(), "결제 처리 실패: 서버 오류", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<CheckoutResponse> call, Throwable t) {
                loadingDialog.dismiss();
                Toast.makeText(requireContext(), "결제 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
        private List<CartItem> cartItems;
        private CartManager cartManager;
        private Runnable onCartUpdated;
        private Context context;

        public CartAdapter(List<CartItem> cartItems, Runnable onCartUpdated) {
            this.cartItems = cartItems;
            this.onCartUpdated = onCartUpdated;
            this.cartManager = CartManager.getInstance();
        }

        @NonNull
        @Override
        public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            context = parent.getContext();
            View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
            return new CartViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
            CartItem item = cartItems.get(position);
            Product product = item.getProduct();
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);

            holder.productName.setText(product.getName());
            holder.productPrice.setText(formatter.format(product.getPrice()) + "원");
            holder.quantityText.setText(String.valueOf(item.getQuantity()));

            holder.decreaseButton.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;
                
                CartItem currentItem = cartItems.get(currentPos);
                int newQuantity = currentItem.getQuantity() - 1;
                if (newQuantity <= 0) {
                    cartManager.removeItem(product.getId());
                    notifyItemRemoved(currentPos);
                    notifyItemRangeChanged(currentPos, cartItems.size());
                } else {
                    cartManager.updateQuantity(product.getId(), newQuantity);
                    notifyItemChanged(currentPos);
                }
                if (onCartUpdated != null) onCartUpdated.run();
            });

            holder.increaseButton.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;
                
                CartItem currentItem = cartItems.get(currentPos);
                int newQuantity = currentItem.getQuantity() + 1;
                cartManager.updateQuantity(product.getId(), newQuantity);
                notifyItemChanged(currentPos);
                if (onCartUpdated != null) onCartUpdated.run();
            });
        }

        @Override
        public int getItemCount() {
            return cartItems.size();
        }

        class CartViewHolder extends RecyclerView.ViewHolder {
            TextView productName;
            TextView productPrice;
            TextView quantityText;
            Button decreaseButton;
            Button increaseButton;

            public CartViewHolder(@NonNull View itemView) {
                super(itemView);
                productName = itemView.findViewById(R.id.product_name);
                productPrice = itemView.findViewById(R.id.product_price);
                quantityText = itemView.findViewById(R.id.quantity_text);
                decreaseButton = itemView.findViewById(R.id.decrease_button);
                increaseButton = itemView.findViewById(R.id.increase_button);
            }
        }
    }
    
    private void setupAIRecommendation() {
        aiRecommendationRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        aiRecommendationAdapter = new AIRecommendationAdapter(this::onRecommendationClick);
        aiRecommendationRecycler.setAdapter(aiRecommendationAdapter);
    }
    
    private void onRecommendationClick(Product product) {
        showProductInfoDialog(product);
    }
    
    private void showProductInfoDialog(Product product) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_result, null);
            builder.setView(dialogView);
            
            TextView productName = dialogView.findViewById(R.id.product_name);
            TextView productPrice = dialogView.findViewById(R.id.product_price);
            TextView productLocation = dialogView.findViewById(R.id.product_location);
            TextView productStock = dialogView.findViewById(R.id.product_stock);
            
            if (product != null) {
                String name = product.getName();
                if (name != null && !name.trim().isEmpty()) {
                    productName.setText(name);
                } else {
                    productName.setText("상품명 없음");
                }
                
                productPrice.setText("가격: " + String.format("%,d", (int)product.getPrice()) + "원");
                
                String category = product.getCategory();
                String location;
                if (category != null) {
                    switch (category) {
                        case "식품":
                            location = "식품 코너";
                            break;
                        case "생활용품":
                            location = "생활용품 코너";
                            break;
                        case "전자제품":
                            location = "전자제품 코너";
                            break;
                        default:
                            location = "일반 코너";
                    }
                } else {
                    location = "일반 코너";
                }
                productLocation.setText("위치: " + location);
                
                int stock = product.getQuantity();
                if (stock > 0) {
                    productStock.setText("재고: " + stock + "개");
                } else {
                    productStock.setText("재고: 품절");
                    productStock.setTextColor(0xFFFF0000);
                }
            }
            
            AlertDialog dialog = builder.create();
            dialog.show();
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "상품 정보 표시 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void autoGetRecommendations() {
        if (cartManager.getCartItems().isEmpty()) {
            loadDefaultRecommendations();
            return;
        }
        
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", MODE_PRIVATE);
        String gender = prefs.getString("gender", "M");
        String birthDate = prefs.getString("birth_date", "1990-01-01");
        
        int age = calculateAge(birthDate);
        
        List<String> cartProductIds = new ArrayList<>();
        for (CartItem item : cartManager.getCartItems()) {
            cartProductIds.add("product_" + item.getProduct().getId());
        }
        
        RecommendRequest request = new RecommendRequest(cartProductIds, 123, gender, age);
        Log.d("AI_RECOMMEND", "요청 데이터 - 장바구니: " + cartProductIds + ", 성별: " + gender + ", 나이: " + age);
        retrofit2.Call<List<String>> call = recommendService.recommend(request);
        
        call.enqueue(new retrofit2.Callback<List<String>>() {
            @Override
            public void onResponse(retrofit2.Call<List<String>> call, 
                                 retrofit2.Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("AI_RECOMMEND", "서버 응답 성공: " + response.body());
                    handleRecommendResult(response.body());
                } else {
                    Log.e("AI_RECOMMEND", "서버 응답 실패: " + response.code());
                    Toast.makeText(requireContext(), "AI 추천 서버 오류", Toast.LENGTH_SHORT).show();
                    loadDefaultRecommendations();
                }
            }
            
            @Override
            public void onFailure(retrofit2.Call<List<String>> call, Throwable t) {
                Log.e("AI_RECOMMEND", "네트워크 연결 실패: " + t.getMessage());
                Toast.makeText(requireContext(), "AI 추천 서버 연결 실패", Toast.LENGTH_SHORT).show();
                loadDefaultRecommendations();
            }
        });
    }
    
    private int calculateAge(String birthDate) {
        try {
            String[] parts = birthDate.split("-");
            int birthYear = Integer.parseInt(parts[0]);
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            return currentYear - birthYear;
        } catch (Exception e) {
            return 25;
        }
    }
    
    private void loadDefaultRecommendations() {
        // 데이터베이스에서 기본 상품 가져오기
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(ServerConfig.HTTP_BASE_URL_WITH_SLASH)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        
        retrofit.create(com.example.smartcartapp.api.ProductService.class)
                .getProducts()
                .enqueue(new retrofit2.Callback<List<Product>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Product>> call, retrofit2.Response<List<Product>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Product> products = response.body();
                            List<Product> recommendations = products.size() > 4 ? 
                                    products.subList(0, 4) : products;
                            aiRecommendationAdapter.updateRecommendations(recommendations);
                        }
                    }
                    
                    @Override
                    public void onFailure(retrofit2.Call<List<Product>> call, Throwable t) {
                        // 서버 연결 실패 시 빈 리스트
                        aiRecommendationAdapter.updateRecommendations(new ArrayList<>());
                    }
                });
    }
    
    private void handleRecommendResult(List<String> recommendedIds) {
        Log.d("AI_RECOMMEND", "추천 결과 처리 시작: " + recommendedIds.size() + "개 상품");
        
        if (recommendedIds.isEmpty()) {
            Log.w("AI_RECOMMEND", "추천 상품이 비어있음, 기본 상품 로드");
            loadDefaultRecommendations();
            return;
        }
        
        // 데이터베이스에서 상품 정보 가져오기
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(ServerConfig.HTTP_BASE_URL_WITH_SLASH)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        
        retrofit.create(com.example.smartcartapp.api.ProductService.class)
                .getProducts()
                .enqueue(new retrofit2.Callback<List<Product>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Product>> call, retrofit2.Response<List<Product>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<Integer, Product> dbProductMap = new HashMap<>();
                            for (Product dbProduct : response.body()) {
                                dbProductMap.put((int)dbProduct.getId(), dbProduct);
                            }
                            
                            List<Product> newRecommendations = new ArrayList<>();
                            
                            for (String productId : recommendedIds) {
                                try {
                                    int id = Integer.parseInt(productId);
                                    Product dbProduct = dbProductMap.get(id);
                                    
                                    if (dbProduct != null) {
                                        newRecommendations.add(dbProduct);
                                        Log.d("AI_RECOMMEND", "추천 상품 추가: " + dbProduct.getName() + " (ID: " + id + ")");
                                    } else {
                                        Log.w("AI_RECOMMEND", "상품 ID " + id + "를 DB에서 찾을 수 없음");
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e("AI_RECOMMEND", "잘못된 상품 ID 형식: " + productId);
                                }
                            }
                            
                            if (!newRecommendations.isEmpty()) {
                                Log.d("AI_RECOMMEND", "최종 추천 상품 " + newRecommendations.size() + "개 표시");
                                aiRecommendationAdapter.updateRecommendations(newRecommendations);
                            } else {
                                Log.w("AI_RECOMMEND", "추천 상품이 비어있음, 기본 상품 로드");
                                loadDefaultRecommendations();
                            }
                        } else {
                            loadDefaultRecommendations();
                        }
                    }
                    
                    @Override
                    public void onFailure(retrofit2.Call<List<Product>> call, Throwable t) {
                        loadDefaultRecommendations();
                    }
                });
    }
}