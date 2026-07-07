package com.example.smartcartapp.api;

import java.util.List;

public class CheckoutRequest {
    public String cart_id;
    public Customer customer;
    public List<Item> items;

    public static class Customer {
        public String id;
        public String gender;
        public String birthdate;
        public Integer birth_year;
    }

    public static class Item {
        public String barcode;
        public Integer qty;
        public Double price;
        public String name;
    }
}