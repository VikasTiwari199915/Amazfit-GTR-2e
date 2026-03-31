package com.vikas.gtr2e.utils;

import android.content.Context;

import com.vikas.gtr2e.GTR2eApp;
import com.vikas.gtr2e.apiInterfaces.GitHubApiService;
import com.vikas.gtr2e.apiInterfaces.ZeppApiService;

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

    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance(String baseURL, Context context) {
        if (retrofit == null) {
            // Enable logging for debugging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            ZeppHeaderInterceptor zeppHeaderInterceptor = new ZeppHeaderInterceptor(context);
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(zeppHeaderInterceptor)
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseURL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static GitHubApiService getGithubApiService(Context context) {
        return getRetrofitInstance(GITHUB_API_BASE_URL, context).create(GitHubApiService.class);
    }
    public static ZeppApiService getZeppApiService(Context context) {
        return getRetrofitInstance(ZEPP_API_BASE_URL, context).create(ZeppApiService.class);
    }
}