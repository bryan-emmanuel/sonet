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
package com.piusvelte.sonet.core.task.twitter;

import static com.piusvelte.sonet.core.Sonet.Sin_reply_to_status_id;
import static com.piusvelte.sonet.core.Sonet.Sstatus;
import static com.piusvelte.sonet.core.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.TWITTER_RETWEET;
import static com.piusvelte.sonet.core.Sonet.TWITTER_UPDATE;
import static com.piusvelte.sonet.core.SonetTokens.TWITTER_KEY;
import static com.piusvelte.sonet.core.SonetTokens.TWITTER_SECRET;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetOAuth;

public class Twitter {

	private static final String TAG = "Twitter";

	private SonetOAuth oauth;
	private HttpClient httpClient;

	public Twitter(String token, String secret, HttpClient httpClient) {
		oauth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, token, secret);
		this.httpClient = httpClient;
	}

	public boolean retweet(String statusId) {
		HttpPost httpPost = new HttpPost(String.format(TWITTER_RETWEET, TWITTER_BASE_URL, statusId));
		// resolve Error 417 Expectation by Twitter
		httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
		return SonetHttpClient.httpResponse(httpClient, oauth.getSignedRequest(httpPost)) != null;
	}

	public boolean comment(String statusId, String message) {
		// limit tweets to 140, breaking up the message if necessary
		while (message.length() > 0) {
			String send;
			if (message.length() > 140) {
				// need to break on a word
				int end = 0;
				int nextSpace = 0;
				for (int i = 0, i2 = message.length(); i < i2; i++) {
					end = nextSpace;
					if (message.substring(i, i + 1).equals(" "))
						nextSpace = i;
				}
				// in case there are no spaces, just break on 140
				if (end == 0)
					end = 140;
				send = message.substring(0, end);
				message = message.substring(end + 1);
			} else {
				send = message;
				message = "";
			}
			HttpPost httpPost = new HttpPost(String.format(TWITTER_UPDATE, TWITTER_BASE_URL));
			// resolve Error 417 Expectation by Twitter
			httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			postParams.add(new BasicNameValuePair(Sstatus, send));
			postParams.add(new BasicNameValuePair(Sin_reply_to_status_id, statusId));
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(postParams));
				return SonetHttpClient.httpResponse(httpClient, oauth.getSignedRequest(httpPost)) != null;
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, e.toString());
			}
		}
		return false;
	}

}
