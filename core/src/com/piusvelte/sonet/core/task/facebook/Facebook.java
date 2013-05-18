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
package com.piusvelte.sonet.core.task.facebook;

import static com.piusvelte.sonet.core.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_COMMENTS;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.Sonet.Smessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.piusvelte.sonet.core.SonetHttpClient;

import android.util.Log;

public class Facebook {

	private static final String TAG = "Facebook";

	private String token;
	private HttpClient httpClient;

	public Facebook(String token, HttpClient httpClient) {
		this.token = token;
		this.httpClient = httpClient;
	}

	public boolean like(String statusId, boolean like) {
		if (like)
			return SonetHttpClient.httpResponse(httpClient, new HttpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, statusId, Saccess_token, token))) != null;
		else {
			HttpDelete httpDelete = new HttpDelete(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, statusId, Saccess_token, token));
			httpDelete.setHeader("Content-Length", "0");
			return SonetHttpClient.httpResponse(httpClient, httpDelete) != null;
		}
	}

	public boolean comment(String statusId, String message) {
		HttpPost httpPost = new HttpPost(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, statusId, Saccess_token, token));
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair(Smessage, message));
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(postParams));
			return SonetHttpClient.httpResponse(httpClient, httpPost) != null;
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.toString());
		}
		return false;
	}

}
