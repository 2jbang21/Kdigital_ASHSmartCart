package com.example.smartcartapp.model;

public class CartItem {
    private Product product;
    private int quantity;
    
    // barcode 앱 호환성을 위한 필드들
    public int id;
    public String barcode;
    public String name;
    public double price;
    public String added_at;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.id = (int)product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return product.getPrice() * quantity;
    }
    
    // barcode 앱 호환성을 위한 getter들
    public int getId() { return id; }
    public String getBarcode() { return barcode; }
    public String getName() { return name != null ? name : (product != null ? product.getName() : ""); }
    public double getPrice() { return price > 0 ? price : (product != null ? product.getPrice() : 0); }
    public String getAddedAt() { return added_at; }
}
