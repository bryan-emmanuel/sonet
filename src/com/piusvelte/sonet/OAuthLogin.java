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

import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.TWITTER_KEY;
import static com.piusvelte.sonet.Tokens.TWITTER_SECRET;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.BUZZ_SCOPE;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.BUZZ_KEY;
import static com.piusvelte.sonet.Tokens.BUZZ_SECRET;

import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_CALLBACK;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.MYSPACE_KEY;
import static com.piusvelte.sonet.Tokens.MYSPACE_SECRET;

import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
//import oauth.signpost.signature.SignatureMethod;
//import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.signature.HmacSha1MessageSigner;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

import com.piusvelte.sonet.Sonet.Accounts;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class OAuthLogin extends Activity {
	private static final String TAG = "OAuthLogin";
	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");
	private static Uri BUZZ_CALLBACK = Uri.parse("sonet://buzz");
	private static Uri MYSPACE_CALLBACK = Uri.parse("sonet://myspace");
	private int mService = Sonet.INVALID_SERVICE;
	private TextView mMessageView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) mService = extras.getInt(Sonet.Accounts.SERVICE, Sonet.INVALID_SERVICE);
		}
		setContentView(R.layout.oauthlogin);
		mMessageView = (TextView) findViewById(R.id.oauthlogin_message);
		Log.v(TAG,"onCreate");
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();
		Log.v(TAG,"onNewIntent:"+uri.toString());
		if (uri != null) {
			if (TWITTER_CALLBACK.getScheme().equals(uri.getScheme())) {
				try {
					// this will populate token and token_secret in consumer
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
					//					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
//					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET, SignatureMethod.HMAC_SHA1);
					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
					consumer.setMessageSigner(new HmacSha1MessageSigner());
					consumer.setTokenWithSecret(ManageAccounts.sRequest_token, ManageAccounts.sRequest_secret);
					//					OAuthProvider provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
//					OAuthProvider provider = new DefaultOAuthProvider(consumer, TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
					OAuthProvider provider = new CommonsHttpOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
					provider.setOAuth10a(true);
					//					provider.retrieveAccessToken(consumer, verifier);
					provider.retrieveAccessToken(consumer, verifier);
					ContentValues values = new ContentValues();
					values.put(Accounts.USERNAME, (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(consumer.getToken(), consumer.getTokenSecret()))).getScreenName());
					values.put(Accounts.TOKEN, consumer.getToken());
					values.put(Accounts.SECRET, consumer.getTokenSecret());
					values.put(Accounts.EXPIRY, 0);
					values.put(Accounts.SERVICE, TWITTER);
					values.put(Accounts.TIMEZONE, 0);
					values.put(Accounts.WIDGET, ManageAccounts.sAppWidgetId);
					if (ManageAccounts.sAccountId != Sonet.INVALID_ACCOUNT_ID) {
						getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(ManageAccounts.sAccountId)});
						ManageAccounts.sAccountId = Sonet.INVALID_ACCOUNT_ID;
					} else getContentResolver().insert(Accounts.CONTENT_URI, values);
					ManageAccounts.sUpdateWidget = true;
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
				}
			} else if (BUZZ_CALLBACK.getScheme().equals(uri.getScheme())) {
				try {
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
//					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(BUZZ_KEY, BUZZ_SECRET, SignatureMethod.HMAC_SHA1);
					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(BUZZ_KEY, BUZZ_SECRET);
					consumer.setMessageSigner(new HmacSha1MessageSigner());
					consumer.setTokenWithSecret(ManageAccounts.sRequest_token, ManageAccounts.sRequest_secret);
					OAuthProvider provider = new CommonsHttpOAuthProvider(BUZZ_URL_REQUEST + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8"), BUZZ_URL_ACCESS, BUZZ_URL_AUTHORIZE + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8") + "&domain=" );
					provider.setOAuth10a(true);
					provider.retrieveAccessToken(consumer, verifier);
					ContentValues values = new ContentValues();
					// need to get username
					values.put(Accounts.USERNAME, "");
					values.put(Accounts.TOKEN, consumer.getToken());
					values.put(Accounts.SECRET, consumer.getTokenSecret());
					values.put(Accounts.EXPIRY, 0);
					values.put(Accounts.SERVICE, BUZZ);
					values.put(Accounts.TIMEZONE, 0);
					values.put(Accounts.WIDGET, ManageAccounts.sAppWidgetId);
					if (ManageAccounts.sAccountId != Sonet.INVALID_ACCOUNT_ID) {
						getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(ManageAccounts.sAccountId)});
						ManageAccounts.sAccountId = Sonet.INVALID_ACCOUNT_ID;
					} else getContentResolver().insert(Accounts.CONTENT_URI, values);
					ManageAccounts.sUpdateWidget = true;
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
				}
			} else if (MYSPACE_CALLBACK.getScheme().equals(uri.getScheme())) {
				try {
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
//					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(BUZZ_KEY, BUZZ_SECRET, SignatureMethod.HMAC_SHA1);
					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(BUZZ_KEY, BUZZ_SECRET);
					consumer.setMessageSigner(new HmacSha1MessageSigner());
					consumer.setTokenWithSecret(ManageAccounts.sRequest_token, ManageAccounts.sRequest_secret);
					OAuthProvider provider = new CommonsHttpOAuthProvider(MYSPACE_URL_REQUEST, MYSPACE_URL_ACCESS, MYSPACE_URL_AUTHORIZE);
					provider.setOAuth10a(true);
					provider.retrieveAccessToken(consumer, verifier);
					// get username
					HttpRequestBase request = new HttpGet("http://opensocial.myspace.com/1.0/people/@me/@self");

					try {

						consumer.sign(request);
						HttpClient httpClient = new DefaultHttpClient();
						HttpResponse httpResponse = httpClient.execute(request);
						StatusLine statusLine = httpResponse.getStatusLine();
						HttpEntity entity = httpResponse.getEntity();

						switch(statusLine.getStatusCode()) {
						case 200:
						case 201:
							String response = "";
							if (entity != null) {
								InputStream is = entity.getContent();
								BufferedReader reader = new BufferedReader(new
										InputStreamReader(is));
								StringBuilder sb = new StringBuilder();

								String line = null;
								try {
									while ((line = reader.readLine()) != null) {
										sb.append(line + "\n");
									}
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
								JSONObject jobj = new JSONObject(response);
								Log.v(TAG,"response:"+response);
							}
							break;
						default:
							// warn about myspace permissions
							break;
						}

					} catch (ClientProtocolException e) {
						Log.e(TAG, e.toString());
					} catch (IOException e) {
						Log.e(TAG, e.toString());
					} catch (OAuthMessageSignerException e) {
						Log.e(TAG, e.toString());
					} catch (OAuthExpectationFailedException e) {
						Log.e(TAG, e.toString());
					} catch (JSONException e) {
						Log.e(TAG, e.toString());
					} catch (OAuthCommunicationException e) {
						e.printStackTrace();
					}
//					ContentValues values = new ContentValues();
//					values.put(Accounts.USERNAME, (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(consumer.getToken(), consumer.getTokenSecret()))).getScreenName());
//					values.put(Accounts.TOKEN, consumer.getToken());
//					values.put(Accounts.SECRET, consumer.getTokenSecret());
//					values.put(Accounts.EXPIRY, 0);
//					values.put(Accounts.SERVICE, MYSPACE);
//					values.put(Accounts.TIMEZONE, 0);
//					values.put(Accounts.WIDGET, ManageAccounts.sAppWidgetId);
//					if (ManageAccounts.sAccountId != Sonet.INVALID_ACCOUNT_ID) {
//						getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(ManageAccounts.sAccountId)});
//						ManageAccounts.sAccountId = Sonet.INVALID_ACCOUNT_ID;
//					} else getContentResolver().insert(Accounts.CONTENT_URI, values);
//					ManageAccounts.sUpdateWidget = true;
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG,"onResume");
		if ((ManageAccounts.sRequest_token == null) && (ManageAccounts.sRequest_secret == null)) {
			mMessageView.setText(R.string.loading);
			try {
				// switching to older signpost for myspace
				//				CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
				CommonsHttpOAuthConsumer consumer;
				//				OAuthProvider provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
				OAuthProvider provider;
				//				String authUrl = provider.retrieveRequestToken(consumer, TWITTER_CALLBACK.toString());
				String authUrl;
				switch (mService) {
				case TWITTER:
//					consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET, SignatureMethod.HMAC_SHA1);
					consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
					consumer.setMessageSigner(new HmacSha1MessageSigner());
					provider = new CommonsHttpOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
					provider.setOAuth10a(true);
					authUrl = provider.retrieveRequestToken(consumer, TWITTER_CALLBACK.toString());
					// need to save the requestToken and secret
					ManageAccounts.sRequest_token = consumer.getToken();
					ManageAccounts.sRequest_secret = consumer.getTokenSecret();
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
					break;
				case MYSPACE:
					Log.v(TAG,"MYSPACE");
					consumer = new CommonsHttpOAuthConsumer(MYSPACE_KEY, MYSPACE_SECRET);
					consumer.setMessageSigner(new HmacSha1MessageSigner());
					provider = new CommonsHttpOAuthProvider(MYSPACE_URL_REQUEST, MYSPACE_URL_ACCESS, MYSPACE_URL_AUTHORIZE);
					Log.v(TAG,"retrieveRequestToken");
					authUrl = provider.retrieveRequestToken(consumer, MYSPACE_CALLBACK.toString());
					// need to save the requestToken and secret
					ManageAccounts.sRequest_token = consumer.getToken();
					ManageAccounts.sRequest_secret = consumer.getTokenSecret();
					Log.v(TAG,"open browser");
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
					break;
				case BUZZ:
					// need to request a token and secret
					consumer = new CommonsHttpOAuthConsumer(BUZZ_KEY, BUZZ_SECRET);
					consumer.setMessageSigner(new HmacSha1MessageSigner());
					provider = new CommonsHttpOAuthProvider(BUZZ_URL_REQUEST + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8"), BUZZ_URL_ACCESS, BUZZ_URL_AUTHORIZE + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8") + "&domain=" + BUZZ_KEY + "&alt=json");
					authUrl = provider.retrieveRequestToken(consumer, BUZZ_CALLBACK.toString());
					// need to save the requestToken and secret
					ManageAccounts.sRequest_token = consumer.getToken();
					ManageAccounts.sRequest_secret = consumer.getTokenSecret();
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
					break;
				}
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
			}
		} else {
			mMessageView.setText(R.string.oauthlogin_message);
			ManageAccounts.sRequest_token = null;
			ManageAccounts.sRequest_secret = null;
		}
	}

}
