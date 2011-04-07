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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.BUZZ_SCOPE;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.BUZZ_KEY;
import static com.piusvelte.sonet.Tokens.BUZZ_SECRET;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_ME;

import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.MYSPACE_KEY;
import static com.piusvelte.sonet.Tokens.MYSPACE_SECRET;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_ME;

//import static com.piusvelte.sonet.Sonet.SALESFORCE;
//import static com.piusvelte.sonet.Sonet.SALESFORCE_URL_ACCESS;
//import static com.piusvelte.sonet.Sonet.SALESFORCE_URL_AUTHORIZE;
//import static com.piusvelte.sonet.Sonet.SALESFORCE_URL_REQUEST;
//import static com.piusvelte.sonet.Tokens.SALESFORCE_KEY;
//import static com.piusvelte.sonet.Tokens.SALESFORCE_SECRET;

import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FACEBOOK_URL_AUTHORIZE;
import static com.piusvelte.sonet.Tokens.FACEBOOK_ID;
import static com.piusvelte.sonet.Sonet.FACEBOOK_URL_ME;

import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_URL_AUTHORIZE;
import static com.piusvelte.sonet.Tokens.FOURSQUARE_KEY;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_URL_ME;

import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Tokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.Tokens.LINKEDIN_SECRET;
import static com.piusvelte.sonet.Sonet.LINKEDIN_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.LINKEDIN_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.LINKEDIN_URL_REQUEST;
import static com.piusvelte.sonet.Sonet.LINKEDIN_HEADERS;
import static com.piusvelte.sonet.Sonet.LINKEDIN_URL_ME;

import static oauth.signpost.OAuth.OAUTH_VERIFIER;

import com.piusvelte.sonet.Sonet.Accounts;

import android.app.Activity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OAuthLogin extends Activity implements OnCancelListener, OnClickListener {
	private static final String TAG = "OAuthLogin";
	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");
	private static Uri BUZZ_CALLBACK = Uri.parse("sonet://buzz");
	private static Uri MYSPACE_CALLBACK = Uri.parse("sonet://myspace");
	//	private static Uri SALESFORCE_CALLBACK = Uri.parse("sonet://salesforce");
	private static Uri FACEBOOK_CALLBACK = Uri.parse("fbconnect://success");
	private static Uri FOURSQUARE_CALLBACK = Uri.parse("sonet://foursquare");
	private static Uri LINKEDIN_CALLBACK = Uri.parse("sonet://linkedin");
	private SonetOAuth mSonetOAuth;
	private ProgressDialog mLoadingDialog;
	private int mWidgetId;
	private long mAccountId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		mLoadingDialog = new ProgressDialog(this);
		mLoadingDialog.setMessage(getString(R.string.loading));
		mLoadingDialog.setCancelable(true);
		mLoadingDialog.setOnCancelListener(this);
		mLoadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), this);
		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				int service = extras.getInt(Sonet.Accounts.SERVICE, Sonet.INVALID_SERVICE);
				mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
				mAccountId = extras.getLong(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID);
				SonetWebView sonetWebView = new SonetWebView();
				try {
					switch (service) {
					case FOURSQUARE:
						sonetWebView.open(String.format(FOURSQUARE_URL_AUTHORIZE, FOURSQUARE_KEY, FOURSQUARE_CALLBACK.toString()));
						break;
					case FACEBOOK:
						sonetWebView.open(String.format(FACEBOOK_URL_AUTHORIZE, FACEBOOK_ID, FACEBOOK_CALLBACK.toString()));
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
						sonetWebView.open(mSonetOAuth.getAuthUrl(String.format(BUZZ_URL_REQUEST, URLEncoder.encode(BUZZ_SCOPE, "utf-8"), getString(R.string.app_name), BUZZ_KEY), BUZZ_URL_ACCESS, String.format(BUZZ_URL_AUTHORIZE, URLEncoder.encode(BUZZ_SCOPE, "utf-8"), getString(R.string.app_name), BUZZ_KEY), BUZZ_CALLBACK.toString(), true));
						break;
						//					case SALESFORCE:
						//						mSonetOAuth = new SonetOAuth(SALESFORCE_KEY, SALESFORCE_SECRET);
						//						sonetWebView.open(mSonetOAuth.getAuthUrl(SALESFORCE_URL_REQUEST, SALESFORCE_URL_ACCESS, SALESFORCE_URL_AUTHORIZE, SALESFORCE_CALLBACK.toString(), true) + "&oauth_consumer_key=" + SALESFORCE_KEY);
						//						break;
					case LINKEDIN:
						mSonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET);
						sonetWebView.open(mSonetOAuth.getAuthUrl(LINKEDIN_URL_REQUEST, LINKEDIN_URL_ACCESS, LINKEDIN_URL_AUTHORIZE, LINKEDIN_CALLBACK.toString(), true));
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

	private String addAccount(String username, String token, String secret, int expiry, int service, String sid) {
		String accountId;
		ContentValues values = new ContentValues();
		values.put(Accounts.USERNAME, username);
		values.put(Accounts.TOKEN, token);
		values.put(Accounts.SECRET, secret);
		values.put(Accounts.EXPIRY, expiry);
		values.put(Accounts.SERVICE, service);
		values.put(Accounts.WIDGET, mWidgetId);
		values.put(Accounts.SID, sid);
		if (mAccountId != Sonet.INVALID_ACCOUNT_ID) {
			accountId = Long.toString(mAccountId);
			getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(mAccountId)});
		} else accountId = getContentResolver().insert(Accounts.CONTENT_URI, values).getLastPathSegment();
		setResult(RESULT_OK);
		return accountId;
	}

	private class SonetWebView {
		private WebView mWebView;

		public SonetWebView() {
			mWebView = new WebView(OAuthLogin.this);
			OAuthLogin.this.setContentView(mWebView);
			mWebView.setWebViewClient(new WebViewClient() {

				@Override
				public void onPageFinished(WebView view, String url) {
					mLoadingDialog.dismiss();
				}

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					if (url != null) {
						mLoadingDialog.show();
						Uri uri = Uri.parse(url);
						try {
							if (TWITTER_CALLBACK.getHost().equals(uri.getHost())) {
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(OAUTH_VERIFIER));
								String response = mSonetOAuth.httpGet("http://api.twitter.com/1/account/verify_credentials.json");
								if (response != null) {
									JSONObject jobj = new JSONObject(response);
									addAccount(jobj.getString("screen_name"), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, TWITTER, jobj.getString("id"));
								}
							} else if (FOURSQUARE_CALLBACK.getHost().equals(uri.getHost())) {
								// get the access_token
								url = url.replace("sonet", "http");
								URL u = new URL(url);
								String token = "";
								String[] parameters = (u.getQuery() + "&" + u.getRef()).split("&");
								for (String parameter : parameters) {
									String[] param = parameter.split("=");
									if (TOKEN.equals(param[0])) {
										token = param[1];
										break;
									}
								}
								String response = Sonet.httpGet(String.format(FOURSQUARE_URL_ME, token));
								if (response != null) {
									JSONObject jobj = (new JSONObject(response)).getJSONObject("response").getJSONObject("user");
									if (jobj.has("firstName") && jobj.has("id")) addAccount(jobj.getString("firstName") + " " + jobj.getString("lastName"), token, "", 0, FOURSQUARE, jobj.getString("id"));
								}
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
								String response = Sonet.httpGet(String.format(FACEBOOK_URL_ME, TOKEN, token));
								if (response != null) {
									JSONObject jobj = new JSONObject(response);
									if (jobj.has("name") && jobj.has("id")) addAccount(jobj.getString("name"), token, "", expiry, FACEBOOK, jobj.getString("id"));
								}
							} else if (MYSPACE_CALLBACK.getHost().equals(uri.getHost())) {
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(OAUTH_VERIFIER));
								String response = mSonetOAuth.httpGet(MYSPACE_URL_ME);
								if (response != null) {
									JSONObject jobj = new JSONObject(response);
									JSONObject person = jobj.getJSONObject("person");
									if (person.has("displayName") && person.has("id")) addAccount(person.getString("displayName"), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, MYSPACE, person.getString("id"));
								}
							} else if (BUZZ_CALLBACK.getHost().equals(uri.getHost())) {
								mWebView.setVisibility(View.INVISIBLE);
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(OAUTH_VERIFIER));
								String response = mSonetOAuth.httpGet(BUZZ_URL_ME);
								if (response != null) {
									JSONObject data = new JSONObject(response).getJSONObject("data");
									if (data.has("displayName") && data.has("id")) addAccount(data.getString("displayName"), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, BUZZ, data.getString("id"));
								}
								//							} else if (SALESFORCE_CALLBACK.getHost().equals(uri.getHost())) {
								//								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(OAUTH_VERIFIER));
								//								String response = mSonetOAuth.httpPost("https://login.salesforce.com/services/OAuth/u/21.0");
								//								Log.v(TAG,"response:"+response);
								//								//account info
								//								//https://login.salesforce.com/ID/orgID/userID?Format=json
							} else if (LINKEDIN_CALLBACK.getHost().equals(uri.getHost())) {
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(OAUTH_VERIFIER));
								String response = mSonetOAuth.httpGet(LINKEDIN_URL_ME, LINKEDIN_HEADERS);
								if (response != null) {
									JSONObject jobj = new JSONObject(response);
									if (jobj.has("firstName") && jobj.has("id")) addAccount(jobj.getString("firstName") + " " + jobj.getString("lastName"), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, LINKEDIN, jobj.getString("id"));
								}
								//							} else if (uri.getHost().contains("salesforce.com") && (uri.getQueryParameter("oauth_consumer_key") == null)) {
								//								Log.v(TAG,"load:"+url + "&oauth_consumer_key=" + SALESFORCE_KEY);
								//								view.loadUrl(url + "&oauth_consumer_key=" + SALESFORCE_KEY);
								//								return true;
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
					mLoadingDialog.dismiss();
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


	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		finish();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}

}
