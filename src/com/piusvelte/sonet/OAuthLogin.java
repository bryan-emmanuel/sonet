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

import static com.piusvelte.sonet.Sonet.TOKEN;
import static com.piusvelte.sonet.Sonet.EXPIRES;

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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.MYSPACE_KEY;
import static com.piusvelte.sonet.Tokens.MYSPACE_SECRET;

import static com.piusvelte.sonet.Sonet.SALESFORCE;
import static com.piusvelte.sonet.Sonet.SALESFORCE_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.SALESFORCE_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.SALESFORCE_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.SALESFORCE_KEY;
import static com.piusvelte.sonet.Tokens.SALESFORCE_SECRET;

import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FACEBOOK_URL_AUTHORIZE;
import static com.piusvelte.sonet.Tokens.FACEBOOK_ID;
import static com.piusvelte.sonet.Sonet.FACEBOOK_PERMISSIONS;
import static com.piusvelte.sonet.Sonet.GRAPH_BASE_URL;

import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_URL_AUTHORIZE;
import static com.piusvelte.sonet.Tokens.FOURSQUARE_KEY;
import static com.piusvelte.sonet.Tokens.FOURSQUARE_SECRET;

import com.piusvelte.sonet.Sonet.Accounts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OAuthLogin extends Activity {
	private static final String TAG = "OAuthLogin";
	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");
	private static Uri BUZZ_CALLBACK = Uri.parse("sonet://buzz");
	private static Uri MYSPACE_CALLBACK = Uri.parse("sonet://myspace");
	private static Uri SALESFORCE_CALLBACK = Uri.parse("sonet://salesforce");
	private static Uri FACEBOOK_CALLBACK = Uri.parse("fbconnect://success");
	private static Uri FOURSQUARE_CALLBACK = Uri.parse("sonet://foursquare");
	private SonetOAuth mSonetOAuth;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				int service = extras.getInt(Sonet.Accounts.SERVICE, Sonet.INVALID_SERVICE);
				SonetWebView sonetWebView = new SonetWebView();
				try {
					switch (service) {
					case FOURSQUARE:
						sonetWebView.open(FOURSQUARE_URL_AUTHORIZE + "?client_id=" + FOURSQUARE_KEY + "&response_type=code&redirect_uri=" + FOURSQUARE_CALLBACK + "&display=touch");
						break;
					case FACEBOOK:
				        sonetWebView.open(FACEBOOK_URL_AUTHORIZE + "?client_id=" + FACEBOOK_ID + "&scope=" + TextUtils.join(",", FACEBOOK_PERMISSIONS) + "&type=user_agent&redirect_uri=" + FACEBOOK_CALLBACK.toString() + "&display=touch&sdk=android");
						break;
					case TWITTER:
						mSonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET);
						sonetWebView.open(mSonetOAuth.getAuthUrl(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE, TWITTER_CALLBACK.toString(), true));
						break;
					case MYSPACE:
						mSonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET);
						sonetWebView.open(mSonetOAuth.getAuthUrl(MYSPACE_URL_REQUEST, MYSPACE_URL_ACCESS, MYSPACE_URL_AUTHORIZE, MYSPACE_CALLBACK.toString(), true));
						break;
					case BUZZ:
						mSonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET);
						sonetWebView.open(mSonetOAuth.getAuthUrl(BUZZ_URL_REQUEST + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8") + "&xoauth_displayname=" + getString(R.string.app_name) + "&domain=" + BUZZ_KEY, BUZZ_URL_ACCESS, BUZZ_URL_AUTHORIZE + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8") + "&xoauth_displayname=" + getString(R.string.app_name) + "&domain=" + BUZZ_KEY + "&btmpl=mobile", BUZZ_CALLBACK.toString(), true));
						break;
					case SALESFORCE:
						mSonetOAuth = new SonetOAuth(SALESFORCE_KEY, SALESFORCE_SECRET);
						sonetWebView.open(mSonetOAuth.getAuthUrl(SALESFORCE_URL_REQUEST, SALESFORCE_URL_ACCESS, SALESFORCE_URL_AUTHORIZE, SALESFORCE_CALLBACK.toString(), true) + "&oauth_consumer_key=" + SALESFORCE_KEY);
						break;
					default:
						this.finish();
					}
				} catch (OAuthMessageSignerException e) {
					Log.e(TAG,e.toString());
					this.finish();
				} catch (OAuthNotAuthorizedException e) {
					Log.e(TAG,e.toString());
					this.finish();
				} catch (OAuthExpectationFailedException e) {
					Log.e(TAG,e.toString());
					this.finish();
				} catch (OAuthCommunicationException e) {
					Log.e(TAG,e.toString());
					this.finish();
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG,e.toString());
					this.finish();
				}
			}
		}
	}
	
	private String addAccount(String username, String token, String secret, int expiry, int service, int timezone) {
		String accountId;
		ContentValues values = new ContentValues();
		values.put(Accounts.USERNAME, username);
		values.put(Accounts.TOKEN, token);
		values.put(Accounts.SECRET, secret);
		values.put(Accounts.EXPIRY, expiry);
		values.put(Accounts.SERVICE, service);
		values.put(Accounts.TIMEZONE, timezone);
		values.put(Accounts.WIDGET, ManageAccounts.sAppWidgetId);
		if (ManageAccounts.sAccountId != Sonet.INVALID_ACCOUNT_ID) {
			accountId = Long.toString(ManageAccounts.sAccountId);
			getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(ManageAccounts.sAccountId)});
			ManageAccounts.sAccountId = Sonet.INVALID_ACCOUNT_ID;
		} else accountId = getContentResolver().insert(Accounts.CONTENT_URI, values).getLastPathSegment();
		ManageAccounts.sUpdateWidget = true;
		return accountId;
	}
	
	private String get(String url) {
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse httpResponse;
		String response = null;
		try {
			httpResponse = httpClient.execute(new HttpGet(url));
			StatusLine statusLine = httpResponse.getStatusLine();
			HttpEntity entity = httpResponse.getEntity();

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
				Log.e(TAG,"get error:"+statusLine.getStatusCode()+" "+statusLine.getReasonPhrase());
				break;
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG,"error:" + e);
		} catch (IOException e) {
			Log.e(TAG,"error:" + e);
		}
		return response;
	}

	private class SonetWebView {
		private WebView mWebView;

		public SonetWebView() {
			mWebView = new WebView(OAuthLogin.this);
			OAuthLogin.this.setContentView(mWebView);
			mWebView.setWebViewClient(new WebViewClient() {

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					if (url != null) {
						Uri uri = Uri.parse(url);
						try {
							if (TWITTER_CALLBACK.getHost().equals(uri.getHost())) {
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER));
								JSONObject jobj = new JSONObject(mSonetOAuth.get("http://api.twitter.com/1/account/verify_credentials.json"));
								addAccount(jobj.getString("screen_name"), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, TWITTER, 0);
							} else if (FOURSQUARE_CALLBACK.getHost().equals(uri.getHost())) {
								// get the access_token
								String response = get(FOURSQUARE_URL_ACCESS + "?client_id=" + FOURSQUARE_KEY + "&client_secret=" + FOURSQUARE_SECRET + "&grant_type=authorization_code&redirect_uri=" + FOURSQUARE_CALLBACK + "&code=" + uri.getQueryParameter("CODE"));
								Log.v(TAG,"4sq:"+response);
								JSONObject jobj = new JSONObject(response);
								String token = jobj.getString("access_token");
								Log.v(TAG, "4sq:"+token);
								jobj = new JSONObject(get("https://api.foursquare.com/v2/users/self"));
								Log.v(TAG, "4sq:"+jobj.toString());
							} else if (FACEBOOK_CALLBACK.getHost().equals(uri.getHost())) {
						        url = url.replace("fbconnect", "http");
								URL u = new URL(url);
								String token = "";
								int expiry = 0;
								String[] parameters = (u.getQuery() + "&" + u.getRef()).split("&");
								for (String parameter : parameters) {
									String[] param = parameter.split("=");
									if (TOKEN.equals(param[0])) token = param[1];
									else if (EXPIRES.equals(param[0])) expiry = param[1] == "0" ? 0 : (int) System.currentTimeMillis() + Integer.parseInt(param[1]) * 1000;
								}
					            JSONObject jobj = new JSONObject(get(GRAPH_BASE_URL + "me?format=json&sdk=android&" + TOKEN + "=" + token));
					            addAccount(jobj.getString("name"), token, "", expiry, FACEBOOK, 0);
							} else if (MYSPACE_CALLBACK.getHost().equals(uri.getHost())) {
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER));
								String response = mSonetOAuth.get("http://opensocial.myspace.com/1.0/people/@me/@self");
								JSONObject jobj = new JSONObject(response);
								final String accountId = addAccount(jobj.getJSONObject("person").getString("displayName"), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, MYSPACE, 0);
								// get the timezone, index set to GMT
								OAuthLogin.this.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										(new AlertDialog.Builder(OAuthLogin.this))
										.setTitle(R.string.timezone)
										.setSingleChoiceItems(R.array.timezone_entries, 12, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												ContentValues values = new ContentValues();
												values.put(Accounts.TIMEZONE, Integer.parseInt(getResources().getStringArray(R.array.timezone_values)[which]));
												getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{accountId});
												dialog.cancel();
												// warn about new myspace permissions
												(new AlertDialog.Builder(OAuthLogin.this))
												.setTitle(R.string.myspace_permissions_title)
												.setMessage(R.string.myspace_permissions_message)
												.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface dialog, int id) {
														dialog.cancel();
														OAuthLogin.this.finish();
													}
												})
												.show();
											}
										})
										.show();
									}
								});
								return true;
							} else if (BUZZ_CALLBACK.getHost().equals(uri.getHost())) {
								mWebView.setVisibility(View.INVISIBLE);
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER));
								String username = new JSONObject(mSonetOAuth.get("https://www.googleapis.com/buzz/v1/people/@me/@self?alt=json")).getJSONObject("data").getString("displayName");
								if (username != null) addAccount(username, mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, BUZZ, 0);
							} else if (SALESFORCE_CALLBACK.getHost().equals(uri.getHost())) {
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER));
								String response = mSonetOAuth.post("https://login.salesforce.com/services/OAuth/u/21.0");
								Log.v(TAG,"response:"+response);
								//account info
								//https://login.salesforce.com/ID/orgID/userID?Format=json
							} else if (uri.getHost().contains("salesforce.com") && (uri.getQueryParameter("oauth_consumer_key") == null)) {
								Log.v(TAG,"load:"+url + "&oauth_consumer_key=" + SALESFORCE_KEY);
								view.loadUrl(url + "&oauth_consumer_key=" + SALESFORCE_KEY);
								return true;
							} else return false;// allow google to redirect
						} catch (OAuthMessageSignerException e) {
							Log.e(TAG, e.getMessage());
						} catch (OAuthNotAuthorizedException e) {
							Log.e(TAG, e.getMessage());
						} catch (OAuthExpectationFailedException e) {
							Log.e(TAG, e.getMessage());
						} catch (OAuthCommunicationException e) {
							Log.e(TAG, e.getMessage());
						} catch (ClientProtocolException e) {
							Log.e(TAG, e.getMessage());
						} catch (JSONException e) {
							Log.e(TAG, e.getMessage());
						} catch (IOException e) {
							Log.e(TAG, e.getMessage());
						}
					}
					OAuthLogin.this.finish();
					return true;
				}

			});
			WebSettings webSettings = mWebView.getSettings();
			webSettings.setJavaScriptEnabled(true);
			webSettings.setDefaultTextEncodingName("UTF-8");
		}

		public void open(String url) {
			if (url != null) mWebView.loadUrl(url);
			else OAuthLogin.this.finish();
		}

	}

}
