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
package com.piusvelte.sonet.core.task.linkedin;

import static com.piusvelte.sonet.core.Sonet.LINKEDIN_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_COMMENT_BODY;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_HEADERS;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_IS_LIKED;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_LIKE_BODY;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_UPDATE;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_UPDATE_COMMENTS;
import static com.piusvelte.sonet.core.Sonet.S_total;
import static com.piusvelte.sonet.core.Sonet.Scomment;
import static com.piusvelte.sonet.core.Sonet.SfirstName;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.SlastName;
import static com.piusvelte.sonet.core.Sonet.Sperson;
import static com.piusvelte.sonet.core.Sonet.Stimestamp;
import static com.piusvelte.sonet.core.Sonet.Svalues;
import static com.piusvelte.sonet.core.SonetTokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.core.SonetTokens.LINKEDIN_SECRET;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetOAuth;
import com.piusvelte.sonet.core.task.LoadCommentsTask;

public class LinkedIn {

	private static final String TAG = "LinkedIn";

	private SonetOAuth oauth;
	private HttpClient httpClient;

	public LinkedIn(String token, String secret, HttpClient httpClient) {
		oauth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, token, secret);
		this.httpClient = httpClient;
	}

	public boolean like(String statusId, boolean like) {
		try {
			HttpPut httpPut = new HttpPut(String.format(LINKEDIN_IS_LIKED, LINKEDIN_BASE_URL, statusId));
			httpPut.addHeader(new BasicHeader("Content-Type", "application/xml"));
			httpPut.setEntity(new StringEntity(String.format(LINKEDIN_LIKE_BODY, like)));
			return SonetHttpClient.httpResponse(httpClient, oauth.getSignedRequest(httpPut)) != null;
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		return false;
	}

	public boolean comment(String statusId, String message) {
		try {
			HttpPost httpPost = new HttpPost(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, statusId));
			httpPost.setEntity(new StringEntity(String.format(LINKEDIN_COMMENT_BODY, message)));
			httpPost.addHeader(new BasicHeader("Content-Type", "application/xml"));
			return SonetHttpClient.httpResponse(httpClient, oauth.getSignedRequest(httpPost)) != null;
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		return false;
	}


	public String getLikeStatus(String statusId) {
		HttpGet httpGet = new HttpGet(String.format(LINKEDIN_UPDATE, LINKEDIN_BASE_URL, statusId));
		for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
		String response = SonetHttpClient.httpResponse(httpClient, oauth.getSignedRequest(httpGet));
		if (response != null) {
			try {
				JSONObject data = new JSONObject(response);
				if (data.has("isCommentable") && !data.getBoolean("isCommentable"))
					return "uncommentable";
				if (data.has("isLikable"))
					return data.has("isLiked") && data.getBoolean("isLiked") ? "unlike" : "like";
				else
					return "unlikable";
			} catch (JSONException e) {
				Log.e(TAG,e.toString());
			}
		}
		return "like";
	}

	public int getComments(LoadCommentsTask task, String statusId, boolean time24hr) {
		int count = 0;
		HttpGet httpGet = new HttpGet(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, statusId));
		for (String[] header : LINKEDIN_HEADERS)
			httpGet.setHeader(header[0], header[1]);
		String response = SonetHttpClient.httpResponse(httpClient, oauth.getSignedRequest(httpGet));
		if (response != null) {
			try {
				JSONObject jsonResponse = new JSONObject(response);
				if (jsonResponse.has(S_total) && (jsonResponse.getInt(S_total) != 0)) {
					JSONArray comments = jsonResponse.getJSONArray(Svalues);
					for (int s = comments.length(); count < s; count++) {
						JSONObject comment = comments.getJSONObject(count);
						JSONObject person = comment.getJSONObject(Sperson);
						task.addComment(comment.getString(Sid), person.getString(SfirstName) + " " + person.getString(SlastName), comment.getString(Scomment), Sonet.getCreatedText(comment.getLong(Stimestamp), time24hr), "");
					}
				}
			} catch (JSONException e) {
				Log.e(TAG,e.toString());
			}
		}
		return count;
	}

}
