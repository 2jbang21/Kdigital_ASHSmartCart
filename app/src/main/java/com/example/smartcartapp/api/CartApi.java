package com.example.smartcartapp.api;

import com.example.smartcartapp.model.CartRequestItem;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HTTP;
import retrofit2.http.POST;

public interface CartApi {


    @POST("cart/add")
    Call<Void> addToCart(@Body CartRequestItem item);

    @HTTP(method = "DELETE", path = "cart/remove", hasBody = true)
    Call<Void> removeFromCart(@Body CartRequestItem item);

    @POST("cart/update")
    Call<Void> updateQuantity(@Body CartRequestItem item);

    @POST("/order/checkout")
    Call<CheckoutResponse> checkout(@Body CheckoutRequest body);
}
