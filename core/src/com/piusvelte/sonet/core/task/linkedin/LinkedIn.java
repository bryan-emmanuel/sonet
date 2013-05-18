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
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_IS_LIKED;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_LIKE_BODY;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_UPDATE_COMMENTS;
import static com.piusvelte.sonet.core.SonetTokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.core.SonetTokens.LINKEDIN_SECRET;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;

import android.util.Log;

import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetOAuth;

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

}
