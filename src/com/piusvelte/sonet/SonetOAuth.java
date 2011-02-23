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
package com.piusvelte.sonet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.HmacSha1MessageSigner;

public class SonetOAuth {

	private CommonsHttpOAuthConsumer mOAuthConsumer;
	private CommonsHttpOAuthProvider mOAuthProvider;
	private String mApiKey;
	private String mApiSecret;
	private static final String TAG = "SonetOAuth";
	
	public SonetOAuth(String apiKey, String apiSecret) {
		Log.v(TAG,"create new oauth");
		this.mApiKey = apiKey;
		this.mApiSecret = apiSecret;
		mOAuthConsumer = new CommonsHttpOAuthConsumer(this.mApiKey, this.mApiSecret);
		mOAuthConsumer.setMessageSigner(new HmacSha1MessageSigner());
	}
	
	public String getAuthUrl(String request, String access, String authorize, String callback, boolean isOAuth10a) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		mOAuthProvider = new CommonsHttpOAuthProvider(request, access, authorize);
		mOAuthProvider.setOAuth10a(isOAuth10a);
		Log.v(TAG, "retrieveRequestToken");
		String authUrl = mOAuthProvider.retrieveRequestToken(mOAuthConsumer, callback);
		Log.v(TAG, "authUrl:"+authUrl);
		return authUrl;
	}
	
	public void retrieveAccessToken(String verifier) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		Log.v(TAG, "retrieveAccessToken");
		mOAuthProvider.retrieveAccessToken(mOAuthConsumer, verifier);
		Log.v(TAG,"accesstoken:"+mOAuthConsumer.getToken());
		Log.v(TAG,"accesssecret:"+mOAuthConsumer.getTokenSecret());
	}
	
	public String get(String url) throws ClientProtocolException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		HttpRequestBase httpRequest = new HttpGet(url);
		mOAuthConsumer.sign(httpRequest);
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse httpResponse = httpClient.execute(httpRequest);
		StatusLine statusLine = httpResponse.getStatusLine();
		HttpEntity entity = httpResponse.getEntity();
		String response = null;

		switch(statusLine.getStatusCode()) {
		case 200:
		case 201:
			if (entity != null) {
				InputStream is = entity.getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();

				String line = null;
				try {
					while ((line = reader.readLine()) != null) sb.append(line + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				response = sb.toString();
			}
			break;
		default:
			Log.v(TAG,"get error:"+statusLine.getStatusCode()+" "+statusLine.getReasonPhrase());
			break;
		}
		return response;
	}
	
	public String getToken() {
		return mOAuthConsumer.getToken();
	}
	
	public String getTokenSecret() {
		return mOAuthConsumer.getTokenSecret();
	}

}
