package com.example.pracaobecnosc;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("PublicHolidays/{year}/{countryCode}")
    Call<List<HolidayResponse>> getPublicHolidays(
            @Path("year") int year,
            @Path("countryCode") String countryCode
    );
}