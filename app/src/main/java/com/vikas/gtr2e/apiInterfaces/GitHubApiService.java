package com.vikas.gtr2e.apiInterfaces;

import com.vikas.gtr2e.beans.github.GithubRelease;

import retrofit2.Call;
import retrofit2.http.GET;
import java.util.List;

public interface GitHubApiService {
    @GET("repos/VikasTiwari199915/Amazfit-GTR-2e/releases")
    Call<List<GithubRelease>> getReleases();
}