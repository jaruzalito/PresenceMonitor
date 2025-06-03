package com.example.pracaobecnosc;

import com.example.pracaobecnosc.Quote;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface QuoteApi {
    @GET("random")
    Call<List<Quote>> getRandomQuote();
}

