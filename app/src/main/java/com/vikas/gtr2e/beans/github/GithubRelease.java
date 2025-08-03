package com.vikas.gtr2e.beans.github;
import java.util.Date;
import java.util.List;

import com.google.gson.annotations.SerializedName;

   
public class GithubRelease {

   @SerializedName("url")
   String url;

   @SerializedName("assets_url")
   String assetsUrl;

   @SerializedName("upload_url")
   String uploadUrl;

   @SerializedName("html_url")
   String htmlUrl;

   @SerializedName("id")
   int id;

   @SerializedName("author")
   Author author;

   @SerializedName("node_id")
   String nodeId;

   @SerializedName("tag_name")
   String tagName;

   @SerializedName("target_commitish")
   String targetCommitish;

   @SerializedName("name")
   String name;

   @SerializedName("draft")
   boolean draft;

   @SerializedName("immutable")
   boolean immutable;

   @SerializedName("prerelease")
   boolean prerelease;

   @SerializedName("created_at")
   Date createdAt;

   @SerializedName("published_at")
   Date publishedAt;

   @SerializedName("assets")
   List<ReleaseAssets> assets;

   @SerializedName("tarball_url")
   String tarballUrl;

   @SerializedName("zipball_url")
   String zipballUrl;

   @SerializedName("body")
   String body;


    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }
    
    public void setAssetsUrl(String assetsUrl) {
        this.assetsUrl = assetsUrl;
    }
    public String getAssetsUrl() {
        return assetsUrl;
    }
    
    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }
    public String getUploadUrl() {
        return uploadUrl;
    }
    
    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }
    public String getHtmlUrl() {
        return htmlUrl;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }
    
    public void setAuthor(Author author) {
        this.author = author;
    }
    public Author getAuthor() {
        return author;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    public String getNodeId() {
        return nodeId;
    }
    
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
    public String getTagName() {
        return tagName;
    }
    
    public void setTargetCommitish(String targetCommitish) {
        this.targetCommitish = targetCommitish;
    }
    public String getTargetCommitish() {
        return targetCommitish;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    
    public void setDraft(boolean draft) {
        this.draft = draft;
    }
    public boolean getDraft() {
        return draft;
    }
    
    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }
    public boolean getImmutable() {
        return immutable;
    }
    
    public void setPrerelease(boolean prerelease) {
        this.prerelease = prerelease;
    }
    public boolean getPrerelease() {
        return prerelease;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
    }
    public Date getPublishedAt() {
        return publishedAt;
    }
    
    public void setAssets(List<ReleaseAssets> assets) {
        this.assets = assets;
    }
    public List<ReleaseAssets> getAssets() {
        return assets;
    }
    
    public void setTarballUrl(String tarballUrl) {
        this.tarballUrl = tarballUrl;
    }
    public String getTarballUrl() {
        return tarballUrl;
    }
    
    public void setZipballUrl(String zipballUrl) {
        this.zipballUrl = zipballUrl;
    }
    public String getZipballUrl() {
        return zipballUrl;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    public String getBody() {
        return body;
    }
    
}