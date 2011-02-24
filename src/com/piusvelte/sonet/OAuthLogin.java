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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import oauth.signpost.OAuth;
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

import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.MYSPACE_KEY;
import static com.piusvelte.sonet.Tokens.MYSPACE_SECRET;

import com.piusvelte.sonet.Sonet.Accounts;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class OAuthLogin extends Activity {
	private static final String TAG = "OAuthLogin";
	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");
	private static Uri BUZZ_CALLBACK = Uri.parse("sonet://buzz");
	private static Uri MYSPACE_CALLBACK = Uri.parse("sonet://myspace");
	private TextView mMessageView;
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
					switch (service) {
					case TWITTER:
						mSonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET);
						try {
							sonetWebView.open(mSonetOAuth.getAuthUrl(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE, TWITTER_CALLBACK.toString(), true));
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
						}
						break;
					case MYSPACE:
						Log.v(TAG,"MYSPACE");
						mSonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET);
						try {
							sonetWebView.open(mSonetOAuth.getAuthUrl(MYSPACE_URL_REQUEST, MYSPACE_URL_ACCESS, MYSPACE_URL_AUTHORIZE, MYSPACE_CALLBACK.toString(), true));
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
						}
						break;
					case BUZZ:
						mSonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET);
						try {
							sonetWebView.open(mSonetOAuth.getAuthUrl(BUZZ_URL_REQUEST + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8"), BUZZ_URL_ACCESS, BUZZ_URL_AUTHORIZE + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8") + "&xoauth_displayname=" + getString(R.string.app_name) + "&domain=" + getString(R.string.app_name) + "&alt=json", OAuth.OUT_OF_BAND/*BUZZ_CALLBACK.toString()*/, true));
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
						break;
					default:
						this.finish();
					}
			}
		}
	}

	private class SonetWebView {
		private WebView mWebView;

		public SonetWebView() {
			mWebView = new WebView(OAuthLogin.this);
			OAuthLogin.this.setContentView(mWebView);
			mWebView.setWebViewClient(new WebViewClient() {

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					Log.v(TAG,"shouldOverrideUrlLoading:"+url);
					if (url != null) {
						Uri uri = Uri.parse(url);
						if (TWITTER_CALLBACK.getScheme().equals(uri.getScheme())) {
								try {
									mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER));
									JSONObject jobj = new JSONObject(mSonetOAuth.get("http://api.twitter.com/1/account/verify_credentials.json"));
									ContentValues values = new ContentValues();
									values.put(Accounts.USERNAME, jobj.getString("screen_name"));
									values.put(Accounts.TOKEN, mSonetOAuth.getToken());
									values.put(Accounts.SECRET, mSonetOAuth.getTokenSecret());
									values.put(Accounts.EXPIRY, 0);
									values.put(Accounts.SERVICE, TWITTER);
									values.put(Accounts.TIMEZONE, 0);
									values.put(Accounts.WIDGET, ManageAccounts.sAppWidgetId);
									if (ManageAccounts.sAccountId != Sonet.INVALID_ACCOUNT_ID) {
										getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(ManageAccounts.sAccountId)});
										ManageAccounts.sAccountId = Sonet.INVALID_ACCOUNT_ID;
									} else getContentResolver().insert(Accounts.CONTENT_URI, values);
									ManageAccounts.sUpdateWidget = true;
								} catch (OAuthMessageSignerException e) {
									Log.e(TAG, e.getMessage());
									mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
								} catch (OAuthNotAuthorizedException e) {
									Log.e(TAG, e.getMessage());
									mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
								} catch (OAuthExpectationFailedException e) {
									Log.e(TAG, e.getMessage());
									mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
								} catch (OAuthCommunicationException e) {
									Log.e(TAG, e.getMessage());
									mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
								} catch (ClientProtocolException e) {
									Log.e(TAG, e.getMessage());
									mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
								} catch (JSONException e) {
									Log.e(TAG, e.getMessage());
									mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
								} catch (IOException e) {
									Log.e(TAG, e.getMessage());
									mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
								}
						} else if (MYSPACE_CALLBACK.getScheme().equals(uri.getScheme())) {
							try {
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER));
								String response = mSonetOAuth.get("http://opensocial.myspace.com/1.0/people/@me/@self");
								Log.v(TAG,"response:"+response);
								JSONObject jobj = new JSONObject(response);
								// need to notify user of myspace permissions
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
							} catch (OAuthMessageSignerException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (OAuthNotAuthorizedException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (OAuthExpectationFailedException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (OAuthCommunicationException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (ClientProtocolException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (JSONException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (IOException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							}

						} else if (BUZZ_CALLBACK.getScheme().equals(uri.getScheme())) {
							try {
								mSonetOAuth.retrieveAccessToken(uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER));
								String response = mSonetOAuth.get("https://www.googleapis.com/buzz/v1/people/@me/@self");
								Log.v(TAG,"response:"+response);
								JSONObject jobj = new JSONObject(response);
//								ContentValues values = new ContentValues();
								// need to get username
//								values.put(Accounts.USERNAME, "");
//								values.put(Accounts.TOKEN, mSonetOAuth.getToken());
//								values.put(Accounts.SECRET, mSonetOAuth.getTokenSecret());
//								values.put(Accounts.EXPIRY, 0);
//								values.put(Accounts.SERVICE, BUZZ);
//								values.put(Accounts.TIMEZONE, 0);
//								values.put(Accounts.WIDGET, ManageAccounts.sAppWidgetId);
//								if (ManageAccounts.sAccountId != Sonet.INVALID_ACCOUNT_ID) {
//									getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(ManageAccounts.sAccountId)});
//									ManageAccounts.sAccountId = Sonet.INVALID_ACCOUNT_ID;
//								} else getContentResolver().insert(Accounts.CONTENT_URI, values);
//								ManageAccounts.sUpdateWidget = true;
							} catch (OAuthMessageSignerException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (OAuthNotAuthorizedException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (OAuthExpectationFailedException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (OAuthCommunicationException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (ClientProtocolException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (JSONException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							} catch (IOException e) {
								Log.e(TAG, e.getMessage());
								mMessageView.setText(e.getMessage() + "\n" + getString(R.string.oauthlogin_message));
							}
						} else return false;// allow google to redirect
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
