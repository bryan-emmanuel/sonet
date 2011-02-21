package com.myspace.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.myspace.sdk.MSRequest.MSRequestCallback;

/**
 * A MSSDK represents MySpace SDK action calls for external users
 * 
 * Each method in this class describes a MySpace SDK request and is associated with a request callback which 
 * is passed in as a argument to the function call. 
 * 
 * If the request loads, the request callback load function is called with response object. 
 * If the request fails, the request callback fail function is called with error object.
 * 
 * @author Nilesh Rane
 */

public class MSSDK {
	
	public static final String CONTENT_TYPE_APP_JSON = "application/json";
	public static final String CONTENT_TYPE_APP_FORM_URLENCODED = "application/x-www-form-urlencoded";
	
	/* me
    ------------------------------------------------------------------------------------------------------------------ */
	public static void getUserInfo(MSRequestCallback requestCallback) {
		getUserInfoWithParameters(null, requestCallback);
	}
	
	public static void getUserInfoWithParameters(Map<String,String> queryStringData, MSRequestCallback requestCallback) {
		String type = "userInfo";
		MSRequest.Builder.getBuilder(requestCallback).get(type, null, queryStringData, null);
	}
	
	/* status mood
    ------------------------------------------------------------------------------------------------------------------ */
	public static void getCurrentStatusMood(MSRequestCallback requestCallback) {
		getCurrentStatusMoodWithParameters(null, requestCallback);
	}
	
	public static void getCurrentStatusMoodWithParameters(Map<String,String> queryStringData, MSRequestCallback requestCallback) {
		String type = "currentStatus";
		MSRequest.Builder.getBuilder(requestCallback).get(type, null, queryStringData, null);
	}
	
	public static void getStatusMood(MSRequestCallback requestCallback) {
		getStatusMoodWithParameters(null, requestCallback);
	}
	
	public static void getStatusMoodForFriend(String friendId, Map<String,String> queryStringData, MSRequestCallback requestCallback) {
		String type = "friendStatus";
		
		Object[] urlArgsData = new Object[1];
		urlArgsData[0] = friendId;
		
		MSRequest.Builder.getBuilder(requestCallback).get(type, urlArgsData, queryStringData, null);
	}
	
	public static void getStatusMoodHistoryForFriend(String friendId, Map<String,String> queryStringData, MSRequestCallback requestCallback) {
		String type = "friendStatusHistory";
		
		Object[] urlArgsData = new Object[1];
		urlArgsData[0] = friendId;
		
		MSRequest.Builder.getBuilder(requestCallback).get(type, urlArgsData, queryStringData, null);
	}
	
	public static void getStatusMoodWithParameters(Map<String,String> queryStringData, MSRequestCallback requestCallback) {
		String type = "friendsStatus";
		MSRequest.Builder.getBuilder(requestCallback).get(type, null, queryStringData, null);
	}
	
	public static void setStatusMood(Map<String,Object> postParams, MSRequestCallback requestCallback) {
		setStatusMood("PUT", postParams, requestCallback);
	}

	public static void setStatusMood(String method, Map<String,Object> postData, MSRequestCallback requestCallback) {
		String type = "updateStatus";
		if(method.equalsIgnoreCase("PUT")) {
			MSRequest.Builder.getBuilder(requestCallback).put(type, null, null, postData, CONTENT_TYPE_APP_JSON, null);
		} else {
			MSRequest.Builder.getBuilder(requestCallback).post(type, null, null, postData, CONTENT_TYPE_APP_JSON, null);
		}
	}

	public static void getMoods(MSRequestCallback requestCallback) {
		getMoodsWithParameters(null, requestCallback);
	}

	public static void getMoodsWithParameters(Map<String,String> queryStringData, MSRequestCallback requestCallback) {
		String type = "mood";
		MSRequest.Builder.getBuilder(requestCallback).get(type, null, queryStringData, null);
	}

	/* friend
    ------------------------------------------------------------------------------------------------------------------ */
	public static void getFriends(MSRequestCallback requestCallback) {
		getFriendsWithParameters(null, requestCallback);
	}
	
	public static void getFriendsWithParameters(Map<String,String> queryStringData, MSRequestCallback requestCallback) {
		String type = "friend";
		MSRequest.Builder.getBuilder(requestCallback).get(type, null, queryStringData, null);
	}

	/* photo
    ------------------------------------------------------------------------------------------------------------------ */
	public static void uploadPhoto(Context context, Map<String,String> queryStringData, Uri imageUri, MSRequestCallback requestCallback) throws IOException {
		String type = "uploadPhoto";
		upload(type, queryStringData, context, imageUri, requestCallback);
	}

	/* video
    ------------------------------------------------------------------------------------------------------------------ */
	public static void uploadVideo(Context context, Map<String,String> queryStringData, Uri videoUri, MSRequestCallback requestCallback) throws IOException {
		String type = "uploadVideo";
		upload(type, queryStringData, context, videoUri, requestCallback);
	}

	private static void upload(String type, Map<String,String> queryStringData, Context context, Uri uri, MSRequestCallback requestCallback) throws IOException {
		final ContentResolver contentResolver = context.getContentResolver();
		InputStream inputStream = contentResolver.openInputStream(uri);
		byte[] postData = new byte[inputStream.available()];
		inputStream.read(postData);

		MSRequest.Builder.getBuilder(requestCallback).post(type, null, queryStringData, postData, contentResolver.getType(uri), null);
	}
	
	/* activity
    ------------------------------------------------------------------------------------------------------------------ */
	public static void getActivity(MSRequestCallback requestCallback) {
		getActivityWithParameters(null, requestCallback);
	}
	
	public static void getActivityWithParameters(Map<String,String> queryStringData, MSRequestCallback requestCallback) {
		String type = "friendActivity";
		MSRequest.Builder.getBuilder(requestCallback).get(type, null, queryStringData, null);
	}
	
	public static void publishActivity(String templateTitleId, List<Map<String,String>> msParams, MSRequestCallback requestCallback) {
		String type = "publishActivity";
		
		Map<String, List<Map<String,String>>> templateData = new HashMap<String, List<Map<String,String>>>();
		templateData.put("msParameters", msParams);
		
		Map<String,Object> postData = new HashMap<String,Object>();
		postData.put("id", "myspace.com.activity.-1");
		postData.put("titleId", templateTitleId);
		postData.put("templateParams", templateData);
		
		MSRequest.Builder.getBuilder(requestCallback).post(type, null, null, postData, CONTENT_TYPE_APP_JSON, null);
	}
}
