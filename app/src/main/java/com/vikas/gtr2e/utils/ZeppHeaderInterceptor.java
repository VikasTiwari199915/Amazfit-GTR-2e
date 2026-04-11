package com.vikas.gtr2e.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ZeppHeaderInterceptor implements Interceptor {
    private final Context context;

    public ZeppHeaderInterceptor(Context context) {
        this.context = context;
    }
    @NonNull
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {

        Request original = chain.request();

        Request request = original.newBuilder()
                .header("callid", String.valueOf(System.currentTimeMillis()))
                .header("X-Request-Id", UUID.randomUUID().toString())
                .header("lang", "en_US")
                .header("appplatform", "ios_phone")
                .header("country", Prefs.getZeppCountryCode(context))
                .header("User-Agent", "Zepp/10.1.1 (Android)")
                .header("appname", "com.huami.midong")
                .header("apptoken", Prefs.getZeppAppToken(context))
                .header("os_version", "10.1.1")
                .header("cv", "1706_10.1.1")
                .build();

        return chain.proceed(request);
    }
}