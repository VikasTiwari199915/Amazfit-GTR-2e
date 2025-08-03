package com.vikas.gtr2e.beans.github;

import com.google.gson.annotations.SerializedName;

   
public class Author {

   @SerializedName("login")
   String login;

   @SerializedName("id")
   int id;

   @SerializedName("node_id")
   String nodeId;

   @SerializedName("avatar_url")
   String avatarUrl;

   @SerializedName("gravatar_id")
   String gravatarId;

   @SerializedName("url")
   String url;

   @SerializedName("html_url")
   String htmlUrl;

   @SerializedName("followers_url")
   String followersUrl;

   @SerializedName("following_url")
   String followingUrl;

   @SerializedName("gists_url")
   String gistsUrl;

   @SerializedName("starred_url")
   String starredUrl;

   @SerializedName("subscriptions_url")
   String subscriptionsUrl;

   @SerializedName("organizations_url")
   String organizationsUrl;

   @SerializedName("repos_url")
   String reposUrl;

   @SerializedName("events_url")
   String eventsUrl;

   @SerializedName("received_events_url")
   String receivedEventsUrl;

   @SerializedName("type")
   String type;

   @SerializedName("user_view_type")
   String userViewType;

   @SerializedName("site_admin")
   boolean siteAdmin;


    public void setLogin(String login) {
        this.login = login;
    }
    public String getLogin() {
        return login;
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
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setGravatarId(String gravatarId) {
        this.gravatarId = gravatarId;
    }
    public String getGravatarId() {
        return gravatarId;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }
    
    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }
    public String getHtmlUrl() {
        return htmlUrl;
    }
    
    public void setFollowersUrl(String followersUrl) {
        this.followersUrl = followersUrl;
    }
    public String getFollowersUrl() {
        return followersUrl;
    }
    
    public void setFollowingUrl(String followingUrl) {
        this.followingUrl = followingUrl;
    }
    public String getFollowingUrl() {
        return followingUrl;
    }
    
    public void setGistsUrl(String gistsUrl) {
        this.gistsUrl = gistsUrl;
    }
    public String getGistsUrl() {
        return gistsUrl;
    }
    
    public void setStarredUrl(String starredUrl) {
        this.starredUrl = starredUrl;
    }
    public String getStarredUrl() {
        return starredUrl;
    }
    
    public void setSubscriptionsUrl(String subscriptionsUrl) {
        this.subscriptionsUrl = subscriptionsUrl;
    }
    public String getSubscriptionsUrl() {
        return subscriptionsUrl;
    }
    
    public void setOrganizationsUrl(String organizationsUrl) {
        this.organizationsUrl = organizationsUrl;
    }
    public String getOrganizationsUrl() {
        return organizationsUrl;
    }
    
    public void setReposUrl(String reposUrl) {
        this.reposUrl = reposUrl;
    }
    public String getReposUrl() {
        return reposUrl;
    }
    
    public void setEventsUrl(String eventsUrl) {
        this.eventsUrl = eventsUrl;
    }
    public String getEventsUrl() {
        return eventsUrl;
    }
    
    public void setReceivedEventsUrl(String receivedEventsUrl) {
        this.receivedEventsUrl = receivedEventsUrl;
    }
    public String getReceivedEventsUrl() {
        return receivedEventsUrl;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }
    
    public void setUserViewType(String userViewType) {
        this.userViewType = userViewType;
    }
    public String getUserViewType() {
        return userViewType;
    }
    
    public void setSiteAdmin(boolean siteAdmin) {
        this.siteAdmin = siteAdmin;
    }
    public boolean getSiteAdmin() {
        return siteAdmin;
    }
    
}