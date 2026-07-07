package com.example.smartcartapp.api;

import java.util.List;

public class CheckoutResponse {
    public boolean ok;
    public int order_id;
    public String cart_id;
    public String customer_id;
    public String gender;
    public String age_group;
    public double total;
    public List<Item> items;

    public static class Item {
        public String barcode;
        public String name;
        public double unit_price;
        public int qty;
        public double line_total;
    }
}