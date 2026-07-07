package com.example.smartcartapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartcartapp.model.Product;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AIRecommendationAdapter extends RecyclerView.Adapter<AIRecommendationAdapter.RecommendationViewHolder> {

    private List<Product> recommendations = new ArrayList<>();
    private OnRecommendationClickListener clickListener;

    public interface OnRecommendationClickListener {
        void onRecommendationClick(Product product);
    }

    public AIRecommendationAdapter(OnRecommendationClickListener listener) {
        this.clickListener = listener;
    }

    public void updateRecommendations(List<Product> newRecommendations) {
        this.recommendations = newRecommendations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_recommendation, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        Product product = recommendations.get(position);
        
        holder.productName.setText(product.getName());
        
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
        holder.productPrice.setText(formatter.format(product.getPrice()) + "원");
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRecommendationClick(product);
            }
        });
    }



    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        TextView productName;
        TextView productPrice;

        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
        }
    }
}