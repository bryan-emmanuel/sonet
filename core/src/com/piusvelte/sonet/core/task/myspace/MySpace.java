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
package com.piusvelte.sonet.core.task.myspace;

import static com.piusvelte.sonet.core.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_STATUSMOODCOMMENTS_BODY;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_URL_STATUSMOODCOMMENTS;
import static com.piusvelte.sonet.core.SonetTokens.MYSPACE_KEY;
import static com.piusvelte.sonet.core.SonetTokens.MYSPACE_SECRET;

import java.io.UnsupportedEncodingException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import android.util.Log;

import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetOAuth;

public class MySpace {

	private static final String TAG = "MySpace";

	private SonetOAuth oauth;
	private HttpClient httpClient;

	public MySpace(String token, String secret, HttpClient httpClient) {
		oauth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, token, secret);
		this.httpClient = httpClient;
	}

	public boolean comment(String entityId, String statusId, String message) {
		HttpPost httpPost = new HttpPost(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, entityId, statusId));
		try {
			httpPost.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOODCOMMENTS_BODY, message)));
			return SonetHttpClient.httpResponse(httpClient, oauth.getSignedRequest(httpPost)) != null;
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.toString());
		}
		return false;
	}

}
