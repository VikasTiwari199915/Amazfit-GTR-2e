package com.vikas.gtr2e.utils;

import android.content.Context;

import com.vikas.gtr2e.GTR2eApp;
import com.vikas.gtr2e.apiInterfaces.GitHubApiService;
import com.vikas.gtr2e.apiInterfaces.ZeppApiService;

import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client Utility for API Services
 * @author Vikas Tiwari
 */
public class RetrofitClient {
    private static final String GITHUB_API_BASE_URL = "https://api.github.com/";
    private static final String ZEPP_API_BASE_URL = "https://api-mifit-us3.zepp.com/";

    public static Retrofit getRetrofitInstance(String baseURL, Context context) {
            // Enable logging for debugging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            ZeppHeaderInterceptor zeppHeaderInterceptor = new ZeppHeaderInterceptor(context);
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            if(Objects.equals(baseURL, ZEPP_API_BASE_URL)) {
                clientBuilder.addInterceptor(zeppHeaderInterceptor);
                clientBuilder.addInterceptor(logging);
            }
            if(Objects.equals(baseURL, GITHUB_API_BASE_URL)) {
                clientBuilder.addInterceptor(logging);
            }
            return new Retrofit.Builder()
                    .baseUrl(baseURL)
                    .client(clientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
    }

    public static GitHubApiService getGithubApiService(Context context) {
        return getRetrofitInstance(GITHUB_API_BASE_URL, context).create(GitHubApiService.class);
    }
    public static ZeppApiService getZeppApiService(Context context) {
        return getRetrofitInstance(ZEPP_API_BASE_URL, context).create(ZeppApiService.class);
    }
}