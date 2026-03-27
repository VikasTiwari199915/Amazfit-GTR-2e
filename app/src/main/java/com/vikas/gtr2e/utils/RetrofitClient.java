package com.vikas.gtr2e.utils;

import com.vikas.gtr2e.apiInterfaces.GitHubApiService;

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
    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance(String baseURL) {
        if (retrofit == null) {
            // Enable logging for debugging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
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

    public static GitHubApiService getGithubApiService() {
        return getRetrofitInstance(GITHUB_API_BASE_URL).create(GitHubApiService.class);
    }
}