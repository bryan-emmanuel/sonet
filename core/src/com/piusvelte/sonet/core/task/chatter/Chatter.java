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

import static com.piusvelte.sonet.core.Sonet.CHATTER_DATE_FORMAT;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_ACCESS;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_COMMENT;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_COMMENTS;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_LIKE;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_LIKES;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.Sonet.Sbody;
import static com.piusvelte.sonet.core.Sonet.Scomments;
import static com.piusvelte.sonet.core.Sonet.ScreatedDate;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.Sname;
import static com.piusvelte.sonet.core.Sonet.Stext;
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

import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.task.LoadCommentsTask;

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

	private String instanceUrl;
	private String instanceToken;

	private boolean getInstance() {
		if ((instanceUrl == null) || (instanceToken == null)) {
			String response = SonetHttpClient.httpResponse(httpClient, new HttpPost(String.format(CHATTER_URL_ACCESS, CHATTER_KEY, token)));
			if (response != null) {
				try {
					JSONObject jobj = new JSONObject(response);
					if (jobj.has("instance_url") && jobj.has(Saccess_token)) {
						instanceUrl = jobj.getString("instance_url");
						instanceToken = jobj.getString(Saccess_token);
						return true;
					}
				} catch (JSONException e) {
					Log.e(TAG, e.getMessage());
				}
			}
			return false;
		} else
			return true;
	}

	public boolean comment(String statusId, String message) {
		if (getInstance()) {
			HttpPost httpPost = new HttpPost(String.format(CHATTER_URL_COMMENT, instanceUrl, statusId, Uri.encode(message)));
			httpPost.setHeader("Authorization", "OAuth " + instanceToken);
			return SonetHttpClient.httpResponse(httpClient, httpPost) != null;
		}
		return false;
	}

	public boolean like(String accountServiceId, String statusId, boolean like) {
		if (getInstance()) {
			if (like) {
				HttpPost httpPost = new HttpPost(String.format(CHATTER_URL_LIKES, instanceUrl, statusId));
				httpPost.setHeader("Authorization", "OAuth " + instanceToken);
				return SonetHttpClient.httpResponse(httpClient, httpPost) != null;
			} else {
				HttpGet httpGet = new HttpGet(String.format(CHATTER_URL_LIKES, instanceUrl, statusId));
				httpGet.setHeader("Authorization", "OAuth " + instanceToken);
				String response = SonetHttpClient.httpResponse(httpClient, httpGet);
				if (response != null) {
					try {
						JSONObject jobj = new JSONObject(response);
						if (jobj.getInt(Stotal) > 0) {
							JSONArray likes = jobj.getJSONArray("likes");
							for (int i = 0, i2 = likes.length(); i < i2; i++) {
								JSONObject l = likes.getJSONObject(i);
								if (l.getJSONObject(Suser).getString(Sid).equals(accountServiceId)) {
									HttpDelete httpDelete = new HttpDelete(String.format(CHATTER_URL_LIKE, instanceUrl, l.getString(Sid)));
									httpDelete.setHeader("Authorization", "OAuth " + instanceToken);
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
		return false;
	}

	public String getLikeStatus(String statusId, String accountServiceId) {
		if (getInstance()) {
			HttpGet httpGet = new HttpGet(String.format(CHATTER_URL_LIKES, instanceUrl, statusId));
			httpGet.setHeader("Authorization", "OAuth " + instanceToken);
			String response = SonetHttpClient.httpResponse(httpClient, httpGet);
			if (response != null) {
				try {
					JSONObject jobj = new JSONObject(response);
					if (jobj.getInt(Stotal) > 0) {
						JSONArray likes = jobj.getJSONArray("likes");
						for (int i = 0, i2 = likes.length(); i < i2; i++) {
							JSONObject like = likes.getJSONObject(i);
							if (like.getJSONObject(Suser).getString(Sid).equals(accountServiceId))
								return "unlike";
						}
					}
				} catch (JSONException e) {
					Log.e(TAG,e.toString());
				}
			}
		}
		return "like";
	}

	public int getComments(LoadCommentsTask task, String statusId, boolean time24hr) {
		int count = 0;
		if (getInstance()) {
			HttpGet httpGet = new HttpGet(String.format(CHATTER_URL_COMMENTS, instanceUrl, statusId));
			httpGet.setHeader("Authorization", "OAuth " + instanceToken);
			String response = SonetHttpClient.httpResponse(httpClient, httpGet);
			if (response != null) {
				try {
					JSONObject chats = new JSONObject(response);
					if (chats.getInt(Stotal) > 0) {
						JSONArray comments = chats.getJSONArray(Scomments);
						for (int s = comments.length(); count < s; count++) {
							JSONObject comment = comments.getJSONObject(count);
							task.addComment(comment.getString(Sid), comment.getJSONObject(Suser).getString(Sname), comment.getJSONObject(Sbody).getString(Stext), Sonet.getCreatedText(task.parseDate(comment.getString(ScreatedDate), CHATTER_DATE_FORMAT), time24hr), "");
						}
					}
				} catch (JSONException e) {
					Log.e(TAG,e.toString());
				}
			}
		}
		return count;
	}

}
