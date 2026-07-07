package com.example.smartcartapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcartapp.R;
import com.example.smartcartapp.model.Product;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class HomeProductAdapter extends RecyclerView.Adapter<HomeProductAdapter.ProductViewHolder> {

    private List<Product> products;
    private OnProductClickListener onProductClickListener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public HomeProductAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.onProductClickListener = listener;
    }

    public void updateProducts(List<Product> newProducts) {
        if (newProducts != null) {
            this.products = newProducts;
            Log.d("HomeProductAdapter", "Products updated: " + newProducts.size());
            notifyDataSetChanged();
        } else {
            Log.e("HomeProductAdapter", "Attempted to update with null products");
        }
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        if (products == null || position >= products.size()) {
            Log.e("HomeProductAdapter", "Invalid position or null products: " + position);
            return;
        }
        
        Product product = products.get(position);
        if (product == null) {
            Log.e("HomeProductAdapter", "Product is null at position: " + position);
            return;
        }
        
        // 상품명 설정
        String productName = product.getName();
        if (productName != null && !productName.trim().isEmpty()) {
            holder.productName.setText(productName);
            Log.d("HomeProductAdapter", "Product name set: " + productName);
        } else {
            holder.productName.setText("상품명 없음");
            Log.w("HomeProductAdapter", "Product name is null or empty for product ID: " + product.getId());
        }
        
        // 가격 설정
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
        holder.productPrice.setText(formatter.format(product.getPrice()) + "원");
        
        // 할인 설정
        String category = product.getCategory();
        if (category != null && category.equals("특가")) {
            holder.productDiscount.setVisibility(View.VISIBLE);
            holder.productDiscount.setText("30% 할인");
        } else {
            holder.productDiscount.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> {
            try {
                Log.d("HomeProductAdapter", "Product clicked: " + product.toString());
                if (onProductClickListener != null && product != null) {
                    onProductClickListener.onProductClick(product);
                } else {
                    Log.w("HomeProductAdapter", "Click listener or product is null");
                }
            } catch (Exception e) {
                Log.e("HomeProductAdapter", "Error handling product click", e);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productName;
        TextView productPrice;
        TextView productDiscount;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productDiscount = itemView.findViewById(R.id.product_discount);
        }
    }
    

}