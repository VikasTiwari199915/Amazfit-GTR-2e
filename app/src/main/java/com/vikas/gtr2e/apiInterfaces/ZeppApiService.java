package com.vikas.gtr2e.apiInterfaces;

import com.vikas.gtr2e.beans.ZeppCloudBeans.BuiltInWatchFace;
import com.vikas.gtr2e.beans.ZeppCloudBeans.FamilyMemberResponse;
import com.vikas.gtr2e.beans.ZeppCloudBeans.StoreResponse;
import com.vikas.gtr2e.beans.zeppAuthBeans.ZeppLoginResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit interface for Zepp APIs
 * @author Vikas Tiwari
 */
public interface ZeppApiService {

    @GET("v1/client/app_tokens")
    Call<ZeppLoginResponse> getAppToken(
            @Query("app_name") String appName,
            @Query("dn") String dn,
            @Query("login_token") String loginToken,
            @Query("os_version") String osVersion
    );

    @FormUrlEncoded
    @POST("huami.health.scale.familymember.get.json")
    Call<FamilyMemberResponse> getFamilyMembers(
            @Field("fuid") String fuid,
            @Field("userid") String userId
    );

    @GET("market/devices/209/watch/builtin")
    Call<List<BuiltInWatchFace>> getBuiltinWatchfaces(
            @Query("builtin_ids") String builtinIds,
            @Query("userid") String userId
    );

    @GET("market/devices/209/watch/homepage")
    Call<StoreResponse> getWatchfaceHomepage(
            @Query("userid") String userId
    );

}
