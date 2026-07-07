package com.example.smartcartapp.model;

import com.google.gson.annotations.SerializedName;

public class Product {
    @SerializedName("product_id")
    private long id;
    
    @SerializedName("product_name")
    private String name;
    
    @SerializedName("price")
    private double price;
    
    @SerializedName("quantity")
    private int quantity;
    
    private String category = "식품";

    // 기본 생성자 (Gson용)
    public Product() {}

    public Product(long id, String name, double price, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.quantity = 0;
    }
    
    public Product(long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = "식품";
        this.quantity = 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", category='" + category + '\'' +
                '}';
    }
}