package com.vikas.gtr2e.utils;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.vikas.gtr2e.beans.ZeppCloudBeans.BuiltInWatchFace;
import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppDevicesResponse;
import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppLoginResponse;
import com.vikas.gtr2e.interfaces.RenewTokenCallback;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tools.jackson.databind.ObjectMapper;

/*
 * Based on auth logic from HuaToken project
 * recreated in java by Vikas Tiwari
 */

/**
 * Utility class for Zepp Authentication
 * @author Vikas Tiwari
 */
public class AmazfitAuthUtil {

    public static final String TAG = "AMAZFIT_AUTH_UTIL";
    public static final String ENC_KEY = "xeNtBVqzDc6tuNTh";
    public static final String ENC_IV  = "MAAAYAAAAAAAAABg";
    public static final String TOKEN_URL = "https://api-user.zepp.com/v2/registrations/tokens";
    public static final String LOGIN_URL = "https://api-mifit.zepp.com/v2/client/login";
    public static final String DEVICE_URL = "https://api-mifit.zepp.com/users/{0}/devices";


    public static HashMap<String, String> getTokens(String username, String pass) throws Exception {
        String payload = buildZeppTokenPayload(username, pass);
        byte[] encryptedPayload = encrypt(payload.getBytes(StandardCharsets.UTF_8));
        return sendTokenRequest(encryptedPayload);
    }

    public static ZeppLoginResponse login(String accessToken) throws Exception {
        String payload = buildLoginPayload(accessToken);
        HttpURLConnection conn = getZeppLoginHttpURLConnection();
        // send payload
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        Log.e(TAG,"Status: " + status);

        String response = getResponse(conn, status);
        Log.e(TAG,"Response Body: " + response);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response, ZeppLoginResponse.class);
    }

    public static ZeppDevicesResponse getDevices(String userId, String appToken) throws Exception {
        String baseUrl = MessageFormat.format(AmazfitAuthUtil.DEVICE_URL, userId);

        String r1 = UUID.randomUUID().toString();
        String r2 = UUID.randomUUID().toString();

        String query = "r=" + URLEncoder.encode(r1, StandardCharsets.UTF_8.displayName()) +
                "&r=" + URLEncoder.encode(r2, StandardCharsets.UTF_8.displayName()) +
                "&enableMultiDeviceOnMultiType=true" +
                "&enableMultiDeviceOnMultiType=true" +
                "&userid=" + URLEncoder.encode(userId, StandardCharsets.UTF_8.displayName()) +
                "&appid=" + new Random().nextLong() +
                "&channel=a100900101016" +
                "&country=US" +
                "&cv=151689_9.12.5" +
                "&device=android_32" +
                "&device_type=android_phone" +
                "&enableMultiDevice=true" +
                "&lang=en_US" +
                "&timezone=Europe/London" +
                "&v=2.0";

        HttpURLConnection conn = getZeppDevicesHttpURLConnection(appToken, baseUrl, query);

        int status = conn.getResponseCode();
        Log.e(TAG,"Device Status: " + status);
        String response = getResponse(conn, status);
        Log.e(TAG,"Devices Response: " + response);

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response, ZeppDevicesResponse.class);
        } catch (Exception e) {
            Log.e(TAG, "getDevices: ", e);
        }
        return null;
    }

    @NonNull
    private static HttpURLConnection getZeppDevicesHttpURLConnection(String appToken, String baseUrl, String query) throws IOException {
        URL url = new URL(baseUrl + "?" + query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // headers
        conn.setRequestProperty("hm-privacy-diagnostics", "false");
        conn.setRequestProperty("country", "US");
        conn.setRequestProperty("appplatform", "android_phone");
        conn.setRequestProperty("hm-privacy-ceip", "true");
        conn.setRequestProperty("x-request-id", UUID.randomUUID().toString());
        conn.setRequestProperty("timezone", "Europe/London");
        conn.setRequestProperty("channel", "a100900101016");
        conn.setRequestProperty("vb", "202509151347");
        conn.setRequestProperty("cv", "151689_9.12.5");
        conn.setRequestProperty("appname", "com.huami.midong");
        conn.setRequestProperty("v", "2.0");
        conn.setRequestProperty("vn", "9.12.5");
        conn.setRequestProperty("apptoken", appToken);
        conn.setRequestProperty("lang", "en_US");
        conn.setRequestProperty("user-agent", "Zepp/9.12.5 (Pixel 4; Android 12; Density/2.75)");
        return conn;
    }

    private static String getResponse(HttpURLConnection conn, int status) throws IOException {
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        return response.toString();
    }

    private static String buildZeppTokenPayload(String username, String pass) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("emailOrPhone", username);
        payload.put("state", "REDIRECTION");
        payload.put("client_id", "HuaMi");
        payload.put("password", pass);
        payload.put("redirect_uri", "https://s3-us-west-2.amazonaws.com/hm-registration/successsignin.html");
        payload.put("region", "us-west-2");
        payload.put("token", Arrays.asList("access", "refresh"));
        payload.put("country_code", "US");
        return getUrlEncodedPayload(payload);
    }

    private static HashMap<String, String> sendTokenRequest(byte[] encryptedPayload) throws Exception {
        HttpURLConnection conn = getZeppTokenHttpURLConnection();
        OutputStream os = conn.getOutputStream();
        os.write(encryptedPayload);  // RAW BYTES
        os.flush();
        os.close();

        int status = conn.getResponseCode();
        Log.e(TAG,"Status: " + status);
        Log.e(TAG,"Status: " + conn.getResponseMessage());

        String location = conn.getHeaderField("Location");
        Log.e(TAG,"Redirect Location: " + location);

        if(status==303 && location!=null) {
            HashMap<String, String> params = getQueryParams(location);
            Log.e(TAG,"Access: " + params.get("access"));
            Log.e(TAG,"Refresh: " + params.get("refresh"));
            return params;
        } else {
            throw new Exception("Error: "+status+"-"+conn.getResponseMessage());
        }
    }

    @NonNull
    private static HttpURLConnection getZeppTokenHttpURLConnection() throws IOException {
        URL url = new URL(AmazfitAuthUtil.TOKEN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        // headers (same as yours)
        conn.setRequestProperty("content-type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("app_name", "com.huami.midong");
        conn.setRequestProperty("appname", "com.huami.midong");
        conn.setRequestProperty("cv", "151689_9.12.5");
        conn.setRequestProperty("v", "2.0");
        conn.setRequestProperty("appplatform", "android_phone");
        conn.setRequestProperty("vb", "202509151347");
        conn.setRequestProperty("vn", "9.12.5");
        conn.setRequestProperty("user-agent", "Zepp/9.12.5 (Pixel 4; Android 12; Density/2.75)");
        conn.setRequestProperty("x-hm-ekv", "1");
        conn.setRequestProperty("accept-encoding", "gzip");
        return conn;
    }

    private static byte[] encrypt(byte[] data) throws Exception {
        byte[] key = AmazfitAuthUtil.ENC_KEY.getBytes(StandardCharsets.UTF_8);
        byte[] iv  = AmazfitAuthUtil.ENC_IV.getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(data);
    }

    private static HashMap<String, String> getQueryParams(String url) throws Exception {
        URI uri = new URI(url);
        String query = uri.getQuery();

        HashMap<String, String> params = new HashMap<>();

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.displayName());
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8.displayName()) : "";
            params.put(key, value);
        }
        return params;
    }

    private static HttpURLConnection getZeppLoginHttpURLConnection() throws IOException {
        URL url = new URL(AmazfitAuthUtil.LOGIN_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        // headers (exact match)
        conn.setRequestProperty("app_name", "com.huami.webapp");
        conn.setRequestProperty("appname", "com.huami.webapp");
        conn.setRequestProperty("origin", "https://user.zepp.com");
        conn.setRequestProperty("referer", "https://user.zepp.com/");
        conn.setRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0");
        conn.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("accept", "application/json, text/plain, */*");
        conn.setRequestProperty("accept-language", "en-US,en;q=0.5");
        return conn;
    }

    private static String buildLoginPayload(String accessToken) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", accessToken);
        payload.put("device_id", UUID.randomUUID().toString());
        payload.put("device_model", "android_phone");
        payload.put("app_version", "9.12.5");
        payload.put("dn", "api-mifit.zepp.com,api-user.zepp.com,api-mifit.zepp.com,api-watch.zepp.com,app-analytics.zepp.com,auth.zepp.com,api-analytics.zepp.com");
        payload.put("third_name", "huami");
        payload.put("source", "com.huami.watch.hmwatchmanager:9.12.5:151689");
        payload.put("app_name", "com.huami.midong");
        payload.put("country_code", "US");
        payload.put("grant_type", "access_token");
        payload.put("allow_registration", "false");
        payload.put("lang", "en");
        payload.put("countryState", "US-NY");
        return getUrlEncodedPayload(payload);
    }

    @SuppressWarnings("unchecked")
    private static String getUrlEncodedPayload(Map<String, Object> payload) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (entry.getValue() instanceof List) {
                for (String val : (List<String>) entry.getValue()) {
                    if (result.length() > 0) result.append("&");
                    result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.displayName()));
                    result.append("=");
                    result.append(URLEncoder.encode(val, StandardCharsets.UTF_8.displayName()));
                }
            } else {
                if (result.length() > 0) result.append("&");
                result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.displayName()));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8.displayName()));
            }
        }
        return result.toString();
    }

    public static void refreshToken(Context context, RenewTokenCallback callback) {
        RetrofitClient.getZeppApiService(context)
                .getAppToken("com.huami.midong",Prefs.getZeppLoginToken(context),"1706_10.1.1")
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ZeppLoginResponse> call, @NonNull Response<ZeppLoginResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getToken_info() != null) {
                            Prefs.setZeppAppToken(context, response.body().getToken_info().getApp_token());
                            callback.onTokenRenewSuccess(response.body().getToken_info().getApp_token());
                        } else {
                            if (response.body() != null && response.body().getError_code() != null) {
                                callback.onTokenRenewFailure(response.body().getError_code());
                            } else {
                                callback.onTokenRenewFailure("Unknown error");
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ZeppLoginResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "API Failure: " + t.getMessage());
                        Toast.makeText(context, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
