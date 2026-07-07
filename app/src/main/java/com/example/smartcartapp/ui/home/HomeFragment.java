package com.example.smartcartapp.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcartapp.R;
import com.example.smartcartapp.ServerConfig;
import com.example.smartcartapp.model.Product;
import com.example.smartcartapp.utils.CartManager;
import com.example.smartcartapp.api.ProductService;
import com.example.smartcartapp.api.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextInputEditText searchInput;
    private RecyclerView allProductsRecycler;
    private HomeProductAdapter allProductsAdapter;
    private CartManager cartManager;
    private ProductService productService;
    private List<Product> allProductsList = new ArrayList<>();
    private TextView productCountText;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        
        cartManager = CartManager.getInstance();
        
        searchInput = root.findViewById(R.id.search_input);
        allProductsRecycler = root.findViewById(R.id.all_products_recycler);
        productCountText = root.findViewById(R.id.product_count_text);
        
        productService = RetrofitClient.getClient().create(ProductService.class);
        
        setupRecyclerView();
        setupSearchBar();
        loadProductsFromServer();
        
        return root;
    }
    
    private void setupRecyclerView() {
        allProductsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        allProductsAdapter = new HomeProductAdapter(new ArrayList<>(), this::showProductDetail);
        allProductsRecycler.setAdapter(allProductsAdapter);
    }
    
    private void setupSearchBar() {
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchProduct(query);
                }
                return true;
            }
            return false;
        });
    }
    
    private void searchProduct(String query) {
        // 상품 검색 로직 (예시 데이터)
        Product foundProduct = findProductByName(query);
        
        if (foundProduct != null) {
            showProductInfoDialog(foundProduct);
        } else {
            Toast.makeText(requireContext(), "상품을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }
    
    private Product findProductByName(String query) {
        for (Product product : allProductsList) {
            if (product.getName().toLowerCase().contains(query.toLowerCase())) {
                return product;
            }
        }
        return null;
    }
    
    private List<Product> getAllProducts() {
        List<Product> allProducts = new ArrayList<>();
        allProducts.add(new Product(1, "신선한 사과", 3000, "식품"));
        allProducts.add(new Product(2, "유기농 바나나", 2500, "식품"));
        allProducts.add(new Product(3, "프리미엄 우유", 2800, "식품"));
        allProducts.add(new Product(4, "수제 식빵", 4000, "식품"));
        allProducts.add(new Product(5, "천연 치즈", 5500, "식품"));
        allProducts.add(new Product(6, "할인 요거트", 1500, "식품"));
        allProducts.add(new Product(7, "생수 2L", 800, "식품"));
        allProducts.add(new Product(8, "오렌지 주스", 1800, "식품"));
        allProducts.add(new Product(9, "초콜릿 과자", 1200, "식품"));
        allProducts.add(new Product(10, "컵라면", 3800, "식품"));
        allProducts.add(new Product(11, "세제", 5000, "생활용품"));
        allProducts.add(new Product(12, "화장지", 3000, "생활용품"));
        allProducts.add(new Product(13, "스마트폰", 800000, "전자제품"));
        allProducts.add(new Product(14, "이어폰", 150000, "전자제품"));
        return allProducts;
    }
    
    private void showProductInfoDialog(Product product) {
        try {
            if (product == null) {
                Log.e("HomeFragment", "Product is null in showProductInfoDialog");
                Toast.makeText(requireContext(), "상품 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d("HomeFragment", "Showing dialog for product: " + product.toString());
            
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_result, null);
            builder.setView(dialogView);
            
            TextView productName = dialogView.findViewById(R.id.product_name);
            TextView productPrice = dialogView.findViewById(R.id.product_price);
            TextView productLocation = dialogView.findViewById(R.id.product_location);
            TextView productStock = dialogView.findViewById(R.id.product_stock);
            
            // 상품명 설정
            String name = product.getName();
            if (name != null && !name.trim().isEmpty()) {
                productName.setText(name);
            } else {
                productName.setText("상품명 없음");
            }
            
            // 가격 설정
            productPrice.setText("가격: " + String.format("%,d", (int)product.getPrice()) + "원");
            
            // 재고 설정 (서버에서 가져온 수량 사용)
            int stock = product.getQuantity();
            if (stock > 0) {
                productStock.setText("재고: " + stock + "개");
            } else {
                productStock.setText("재고: 품절");
                productStock.setTextColor(0xFFFF0000);
            }
            
            AlertDialog dialog = builder.create();
            dialog.show();
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error showing product dialog", e);
            Toast.makeText(requireContext(), "상품 정보 표시 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    

    
    private void loadProductsFromServer() {
        // Flask 서버에서 상품 목록 가져오기
        retrofit2.Retrofit flaskRetrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(ServerConfig.HTTP_BASE_URL_WITH_SLASH)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
                
        ProductService flaskProductService = flaskRetrofit.create(ProductService.class);
        
        flaskProductService.getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                Log.d("HomeFragment", "Response received: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body();
                    Log.d("HomeFragment", "Products received: " + products.size());
                    for (Product p : products) {
                        Log.d("HomeFragment", "Product: " + p.toString());
                    }
                    
                    allProductsList = products;
                    if (allProductsAdapter != null) {
                        allProductsAdapter.updateProducts(allProductsList);
                        updateProductCount();
                        //Toast.makeText(requireContext(), "서버에서 상품 목록 불러오기 성공 (" + products.size() + "개)", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("HomeFragment", "Response not successful: " + response.code());
                    loadFallbackProducts();
                }
            }
            
            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Log.e("HomeFragment", "Network error: ", t);
                loadFallbackProducts();
                //Toast.makeText(requireContext(), "Flask 서버 연결 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadFallbackProducts() {
        allProductsList = getAllProducts();
        allProductsAdapter.updateProducts(allProductsList);
        updateProductCount();
    }
    
    private void updateProductCount() {
        if (productCountText != null && allProductsList != null) {
            productCountText.setText(allProductsList.size() + "개");
            Log.d("HomeFragment", "Product count updated: " + allProductsList.size());
        }
    }
    
    private void showProductDetail(Product product) {
        showProductInfoDialog(product);
    }
    

}