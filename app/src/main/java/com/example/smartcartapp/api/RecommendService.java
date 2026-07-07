package com.example.smartcartapp.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RecommendService {
    @POST("/recommend")
    Call<List<String>> recommend(@Body com.example.smartcartapp.model.RecommendRequest req);
}