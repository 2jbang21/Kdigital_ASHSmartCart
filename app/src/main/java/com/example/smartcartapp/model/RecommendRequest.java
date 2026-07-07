package com.example.smartcartapp.model;

import java.util.List;

public class RecommendRequest {
    private List<String> cart;
    private int user_id;
    private String gender;
    private int age;

    public RecommendRequest(List<String> cart, int user_id, String gender, int age) {
        this.cart = cart;
        this.user_id = user_id;
        this.gender = gender;
        this.age = age;
    }

    public RecommendRequest() {}

    public List<String> getCart() { return cart; }
    public int getUser_id() { return user_id; }
    public String getGender() { return gender; }
    public int getAge() { return age; }
}