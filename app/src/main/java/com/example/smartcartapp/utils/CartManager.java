package com.example.smartcartapp.utils;

import com.example.smartcartapp.model.CartItem;
import com.example.smartcartapp.model.Product;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import com.example.smartcartapp.api.CartApi;
import com.example.smartcartapp.api.RetrofitClient;
import com.example.smartcartapp.model.CartRequestItem;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems;
    private List<CartUpdateListener> listeners;
    
    public interface CartUpdateListener {
        void onCartUpdated();
    }

    private CartManager() {
        cartItems = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void addItem(Product product) {
        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == product.getId()) {
                item.setQuantity(item.getQuantity() + 1);
                sendToServer(product, 1, true);
                notifyListeners();
                return;
            }
        }
        cartItems.add(new CartItem(product, 1));
        sendToServer(product, 1, true);
        notifyListeners();
    }


    public void removeItem(long productId) {
        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == productId) {
                sendToServer(item.getProduct(), item.getQuantity(), false);
                break;
            }
        }
        cartItems.removeIf(item -> item.getProduct().getId() == productId);
        notifyListeners();
    }


    public void updateQuantity(long productId, int quantity) {
        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == productId) {
                if (quantity <= 0) {
                    removeItem(productId);
                } else {
                    item.setQuantity(quantity);
                    sendUpdateToServer(item.getProduct(), quantity);
                    notifyListeners();
                }
                return;
            }
        }
    }

    private void sendUpdateToServer(Product product, int quantity) {
        CartRequestItem requestItem = new CartRequestItem(
                "CART-0001",
                String.valueOf(product.getId()),
                product.getName(),
                quantity
        );

        CartApi api = RetrofitClient.getClient().create(CartApi.class);
        Call<Void> call = api.updateQuantity(requestItem);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("CartManager", "서버 수량 업데이트 성공");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("CartManager", "서버 수량 업데이트 실패", t);
            }
        });
    }



    public void clearCart() {
        cartItems.clear();
        notifyListeners();
    }
    
    public void addListener(CartUpdateListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(CartUpdateListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners() {
        for (CartUpdateListener listener : listeners) {
            listener.onCartUpdated();
        }
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }

    public int getTotalItems() {
        int total = 0;
        for (CartItem item : cartItems) {
            total += item.getQuantity();
        }
        return total;
    }
    
    public int getItemQuantity(long productId) {
        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == productId) {
                return item.getQuantity();
            }
        }
        return 0;
    }
    private void sendToServer(Product product, int quantity, boolean isAdd) {
        CartRequestItem requestItem = new CartRequestItem(
                "CART-0001",                               // 고정된 cart_id
                String.valueOf(product.getId()),           // 상품 ID
                product.getName(),                         // ✅ 상품 이름 전송
                quantity                                   // 수량
        );

        CartApi api = RetrofitClient.getClient().create(CartApi.class);
        Call<Void> call = isAdd ? api.addToCart(requestItem) : api.removeFromCart(requestItem);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("CartManager", "서버 전송 성공: " + (isAdd ? "추가" : "삭제"));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("CartManager", "서버 전송 실패", t);
            }
        });
    }


}