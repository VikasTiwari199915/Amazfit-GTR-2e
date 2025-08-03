package com.vikas.gtr2e.beans.github;
import java.util.Date;

import com.google.gson.annotations.SerializedName;

   
public class ReleaseAssets {

   @SerializedName("url")
   String url;

   @SerializedName("id")
   int id;

   @SerializedName("node_id")
   String nodeId;

   @SerializedName("name")
   String name;

   @SerializedName("label")
   String label;

   @SerializedName("uploader")
   Uploader uploader;

   @SerializedName("content_type")
   String contentType;

   @SerializedName("state")
   String state;

   @SerializedName("size")
   int size;

   @SerializedName("digest")
   String digest;

   @SerializedName("download_count")
   int downloadCount;

   @SerializedName("created_at")
   Date createdAt;

   @SerializedName("updated_at")
   Date updatedAt;

   @SerializedName("browser_download_url")
   String browserDownloadUrl;


    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return id;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    public String getNodeId() {
        return nodeId;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    public String getLabel() {
        return label;
    }
    
    public void setUploader(Uploader uploader) {
        this.uploader = uploader;
    }
    public Uploader getUploader() {
        return uploader;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public String getContentType() {
        return contentType;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    public String getState() {
        return state;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    public int getSize() {
        return size;
    }
    
    public void setDigest(String digest) {
        this.digest = digest;
    }
    public String getDigest() {
        return digest;
    }
    
    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }
    public int getDownloadCount() {
        return downloadCount;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setBrowserDownloadUrl(String browserDownloadUrl) {
        this.browserDownloadUrl = browserDownloadUrl;
    }
    public String getBrowserDownloadUrl() {
        return browserDownloadUrl;
    }
    
}