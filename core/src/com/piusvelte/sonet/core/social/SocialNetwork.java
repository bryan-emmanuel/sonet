package com.piusvelte.sonet.core.social;

import java.util.List;

import org.apache.http.client.HttpClient;

abstract class SocialNetwork {
	
	protected SocialNetwork(Credential credential, HttpClient httpClient) {
		this.credential = credential;
		this.httpClient = httpClient;
	}
	
	protected Credential credential;
	protected HttpClient httpClient;
	
	public abstract List<Status> getFeed(int limit);
	public abstract List<Comment> getComments(String statusId);
	public abstract boolean post(String message, Location location, String[] tags);
	public abstract boolean comment(String statusId, String message);
	public abstract List<Location> getLocations(double latitude, double longitude);
	public abstract boolean like(String statusId, boolean like);
	public abstract String getLikeStatus(String statusId, String accountServiceId);
	
//	public interface SocialListener {
//		
//		public void setLocations(long accountId, String[] locationIds, String[] locationNames);
//		public void setLikeStatus(boolean liked);
//		public void commentFinished(boolean success);
//		public void postFinished(boolean success);
//		public void setComment(String id, String name, String message, String created, String action);
//		public void setCommentCount(int count);
//		
//	}
	
}
