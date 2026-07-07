package com.example.smartcartapp.model;

public class CartRequestItem {
    private String cart_id;
    private String product_id;
    private String name;
    private int quantity;

    public CartRequestItem(String cart_id, String product_id, String name, int quantity) {
        this.cart_id = cart_id;
        this.product_id = product_id;
        this.name = name;
        this.quantity = quantity;
    }

    public String getCart_id() {
        return cart_id;
    }

    public String getProduct_id() {
        return product_id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}
