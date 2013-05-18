/*
 * Sonet - Android Social Networking Widget
 * Copyright (C) 2009 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.sonet.core.task.chatter;

import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_ACCESS;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_COMMENT;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_LIKE;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_LIKES;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.Stotal;
import static com.piusvelte.sonet.core.Sonet.Suser;
import static com.piusvelte.sonet.core.SonetTokens.CHATTER_KEY;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.core.SonetHttpClient;

import android.net.Uri;
import android.util.Log;

public class Chatter {

	private static final String TAG = "Chatter";

	private String token;
	private HttpClient httpClient;

	public Chatter(String token, HttpClient httpClient) {
		this.token = token;
		this.httpClient = httpClient;
	}

	public boolean comment(String statusId, String message) {
		String response = SonetHttpClient.httpResponse(httpClient, new HttpPost(String.format(CHATTER_URL_ACCESS, CHATTER_KEY, token)));
		if (response != null) {
			try {
				JSONObject jobj = new JSONObject(response);
				if (jobj.has("instance_url") && jobj.has(Saccess_token)) {
					HttpPost httpPost = new HttpPost(String.format(CHATTER_URL_COMMENT, jobj.getString("instance_url"), statusId, Uri.encode(message)));
					httpPost.setHeader("Authorization", "OAuth " + jobj.getString(Saccess_token));
					return SonetHttpClient.httpResponse(httpClient, httpPost) != null;
				}
			} catch (JSONException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return false;
	}
	
	public boolean like(String accountServiceId, String statusId, boolean like) {
		String response = SonetHttpClient.httpResponse(httpClient, new HttpPost(String.format(CHATTER_URL_ACCESS, CHATTER_KEY, token)));
		if (response != null) {
			try {
				JSONObject jobj = new JSONObject(response);
				if (jobj.has("instance_url") && jobj.has(Saccess_token)) {
					String instance = jobj.getString("instance_url");
					String token = jobj.getString(Saccess_token);
					if (like) {
						HttpPost httpPost = new HttpPost(String.format(CHATTER_URL_LIKES, instance, statusId));
						httpPost.setHeader("Authorization", "OAuth " + token);
						return SonetHttpClient.httpResponse(httpClient, httpPost) != null;
					} else {
						HttpGet httpGet = new HttpGet(String.format(CHATTER_URL_LIKES, instance, statusId));
						httpGet.setHeader("Authorization", "OAuth " + token);
						if ((response = SonetHttpClient.httpResponse(httpClient, httpGet)) != null) {
							try {
								jobj = new JSONObject(response);
								if (jobj.getInt(Stotal) > 0) {
									JSONArray likes = jobj.getJSONArray("likes");
									for (int i = 0, i2 = likes.length(); i < i2; i++) {
										JSONObject l = likes.getJSONObject(i);
										if (l.getJSONObject(Suser).getString(Sid).equals(accountServiceId)) {
											HttpDelete httpDelete = new HttpDelete(String.format(CHATTER_URL_LIKE, jobj.getString("instance_url"), l.getString(Sid)));
											httpDelete.setHeader("Authorization", "OAuth " + jobj.getString(Saccess_token));
											return SonetHttpClient.httpResponse(httpClient, httpDelete) != null;
										}
									}
								}
							} catch (JSONException e) {
								Log.e(TAG,e.toString());
							}
						}	
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return false;
	}

}
