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

import static com.piusvelte.sonet.Sonet.*;
import static com.piusvelte.sonet.SonetTokens.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static oauth.signpost.OAuth.OAUTH_VERIFIER;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Widget_accounts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.UriMatcher;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
//import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

public class OAuthLogin extends Activity implements OnCancelListener, OnClickListener {
	private static final String TAG = "OAuthLogin";
	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");
	private static Uri MYSPACE_CALLBACK = Uri.parse("sonet://myspace");
	private static Uri CHATTER_CALLBACK = Uri.parse("sonet://chatter");
	private static Uri FACEBOOK_CALLBACK = Uri.parse("fbconnect://success");
	private static Uri FOURSQUARE_CALLBACK = Uri.parse("sonet://foursquare");
	private static Uri LINKEDIN_CALLBACK = Uri.parse("sonet://linkedin");
	private static Uri IDENTICA_CALLBACK = Uri.parse("sonet://identi.ca");
	//	private static Uri GOOGLEPLUS_CALLBACK = Uri.parse("http://localhost");
	private SonetOAuth mSonetOAuth;
	private ProgressDialog mLoadingDialog;
	private int mWidgetId;
	private long mAccountId;
	private String mServiceName = "unknown";
	private SonetWebView mSonetWebView;
	private HttpClient mHttpClient;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		mHttpClient = SonetHttpClient.getThreadSafeClient(getApplicationContext());
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
				mServiceName = Sonet.getServiceName(getResources(), service);
				mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
				mAccountId = extras.getLong(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID);
				mSonetWebView = new SonetWebView();
				final ProgressDialog loadingDialog = new ProgressDialog(this);
				final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
					@Override
					protected String doInBackground(String... args) {
						try {
							return mSonetOAuth.getAuthUrl(args[0], args[1], args[2], args[3], Boolean.parseBoolean(args[4]));
						} catch (OAuthMessageSignerException e) {
							e.printStackTrace();
						} catch (OAuthNotAuthorizedException e) {
							e.printStackTrace();
						} catch (OAuthExpectationFailedException e) {
							e.printStackTrace();
						} catch (OAuthCommunicationException e) {
							e.printStackTrace();
						}
						return null;
					}

					@Override
					protected void onPostExecute(String url) {
						if (loadingDialog.isShowing()) loadingDialog.dismiss();
						// load the webview
						if (url != null) {
							mSonetWebView.open(url);
						} else {
							(Toast.makeText(OAuthLogin.this, String.format(getString(R.string.oauth_error), mServiceName), Toast.LENGTH_LONG)).show();
							OAuthLogin.this.finish();
						}
					}
				};
				loadingDialog.setMessage(getString(R.string.loading));
				loadingDialog.setCancelable(true);
				loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
					@Override
					public void onCancel(DialogInterface dialog) {
						if (!asyncTask.isCancelled()) asyncTask.cancel(true);
					}
				});
				loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				switch (service) {
				case TWITTER:
					mSonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET);
					asyncTask.execute(String.format(TWITTER_URL_REQUEST, TWITTER_BASE_URL), String.format(TWITTER_URL_ACCESS, TWITTER_BASE_URL), String.format(TWITTER_URL_AUTHORIZE, TWITTER_BASE_URL), TWITTER_CALLBACK.toString(), Boolean.toString(true));
					loadingDialog.show();
					break;
				case FACEBOOK:
					mSonetWebView.open(String.format(FACEBOOK_URL_AUTHORIZE, FACEBOOK_BASE_URL, FACEBOOK_ID, FACEBOOK_CALLBACK.toString()));
					break;
				case MYSPACE:
					mSonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET);
					asyncTask.execute(MYSPACE_URL_REQUEST, MYSPACE_URL_ACCESS, MYSPACE_URL_AUTHORIZE, MYSPACE_CALLBACK.toString(), Boolean.toString(true));
					loadingDialog.show();
					break;
				case FOURSQUARE:
					mSonetWebView.open(String.format(FOURSQUARE_URL_AUTHORIZE, FOURSQUARE_KEY, FOURSQUARE_CALLBACK.toString()));
					break;
				case LINKEDIN:
					mSonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET);
					asyncTask.execute(LINKEDIN_URL_REQUEST, LINKEDIN_URL_ACCESS, LINKEDIN_URL_AUTHORIZE, LINKEDIN_CALLBACK.toString(), Boolean.toString(true));
					loadingDialog.show();
					break;
				case SMS:
					Cursor c = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID}, Accounts.SERVICE + "=?", new String[]{Integer.toString(SMS)}, null);
					if (c.moveToFirst()) {
						(Toast.makeText(OAuthLogin.this, "SMS has already been added.", Toast.LENGTH_LONG)).show();
					} else {
						addAccount(getResources().getStringArray(R.array.service_entries)[SMS], null, null, 0, SMS, null);
					}
					c.close();
					finish();
					break;
				case RSS:
					// prompt for RSS url
					final EditText rss_url = new EditText(this);
					rss_url.setSingleLine();
					new AlertDialog.Builder(OAuthLogin.this)
					.setTitle(R.string.rss_url)
					.setView(rss_url)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, int which) {
							// test the url and add if valid, else Toast error
							mLoadingDialog.show();
							(new AsyncTask<String, Void, String>() {
								String url;

								@Override
								protected String doInBackground(String... params) {
									url = rss_url.getText().toString();
									return SonetHttpClient.httpResponse(mHttpClient, new HttpGet(url));
								}

								@Override
								protected void onPostExecute(String response) {
									mLoadingDialog.dismiss();
									if (response != null) {
										DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
										DocumentBuilder db;
										try {
											db = dbf.newDocumentBuilder();
											InputSource is = new InputSource();
											is.setCharacterStream(new StringReader(response));
											Document doc = db.parse(is);
											// test parsing...
											NodeList nodes = doc.getElementsByTagName(Sitem);
											if (nodes.getLength() > 0) {
												// check for an image
												NodeList images = doc.getElementsByTagName(Simage);
												if (images.getLength() > 0) {
													NodeList imageChildren = images.item(0).getChildNodes();
													Node n = imageChildren.item(0);
													if (n.getNodeName().toLowerCase().equals(Surl)) {
														if (n.hasChildNodes()) {
															n.getChildNodes().item(0).getNodeValue();
														}
													}
												}
												NodeList children = nodes.item(0).getChildNodes();
												String date = null;
												String title = null;
												String description = null;
												String link = null;
												int values_count = 0;
												for (int child = 0, c2 = children.getLength(); (child < c2) && (values_count < 4); child++) {
													Node n = children.item(child);
													if (n.getNodeName().toLowerCase().equals(Spubdate)) {
														values_count++;
														if (n.hasChildNodes()) {
															date = n.getChildNodes().item(0).getNodeValue();
														}
													} else if (n.getNodeName().toLowerCase().equals(Stitle)) {
														values_count++;
														if (n.hasChildNodes()) {
															title = n.getChildNodes().item(0).getNodeValue();
														}
													} else if (n.getNodeName().toLowerCase().equals(Sdescription)) {
														values_count++;
														if (n.hasChildNodes()) {
															StringBuilder sb = new StringBuilder();
															NodeList descNodes = n.getChildNodes();
															for (int dn = 0, dn2 = descNodes.getLength(); dn < dn2; dn++) {
																Node descNode = descNodes.item(dn);
																if (descNode.getNodeType() == Node.TEXT_NODE) {
																	sb.append(descNode.getNodeValue());
																}
															}
															// strip out the html tags
															description = sb.toString().replaceAll("\\<(.|\n)*?>", "");
														}
													} else if (n.getNodeName().toLowerCase().equals(Slink)) {
														values_count++;
														if (n.hasChildNodes()) {
															link = n.getChildNodes().item(0).getNodeValue();
														}
													}
												}
												if (Sonet.HasValues(new String[]{title, description, link, date})) {
													final EditText url_name = new EditText(OAuthLogin.this);
													url_name.setSingleLine();
													new AlertDialog.Builder(OAuthLogin.this)
													.setTitle(R.string.rss_channel)
													.setView(url_name)
													.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialog1, int which) {
															addAccount(url_name.getText().toString(), null, null, 0, RSS, url);
															dialog1.dismiss();
															dialog.dismiss();
															finish();
														}
													})
													.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialog1, int which) {
															dialog1.dismiss();
															dialog.dismiss();
															finish();
														}
													})
													.show();
												} else {
													(Toast.makeText(OAuthLogin.this, "Feed is missing standard fields", Toast.LENGTH_LONG)).show();
												}
											} else {
												(Toast.makeText(OAuthLogin.this, "Invalid feed", Toast.LENGTH_LONG)).show();
												dialog.dismiss();
												finish();
											}
										} catch (ParserConfigurationException e) {
											Log.e(TAG, e.toString());
											(Toast.makeText(OAuthLogin.this, "Invalid feed", Toast.LENGTH_LONG)).show();
											dialog.dismiss();
											finish();
										} catch (SAXException e) {
											Log.e(TAG, e.toString());
											(Toast.makeText(OAuthLogin.this, "Invalid feed", Toast.LENGTH_LONG)).show();
											dialog.dismiss();
											finish();
										} catch (IOException e) {
											Log.e(TAG, e.toString());
											(Toast.makeText(OAuthLogin.this, "Invalid feed", Toast.LENGTH_LONG)).show();
											dialog.dismiss();
											finish();
										}
									} else {
										(Toast.makeText(OAuthLogin.this, "Invalid URL", Toast.LENGTH_LONG)).show();
										dialog.dismiss();
										finish();
									}
								}
							}).execute(rss_url.getText().toString());
						}
					})
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}
					})
					.show();
					break;
				case IDENTICA:
					mSonetOAuth = new SonetOAuth(IDENTICA_KEY, IDENTICA_SECRET);
					asyncTask.execute(String.format(IDENTICA_URL_REQUEST, IDENTICA_BASE_URL), String.format(IDENTICA_URL_ACCESS, IDENTICA_BASE_URL), String.format(IDENTICA_URL_AUTHORIZE, IDENTICA_BASE_URL), IDENTICA_CALLBACK.toString(), Boolean.toString(true));
					loadingDialog.show();
					break;
				case GOOGLEPLUS:
					mSonetWebView.open(String.format(GOOGLEPLUS_AUTHORIZE, GOOGLE_CLIENTID, "urn:ietf:wg:oauth:2.0:oob"));
					break;
				case CHATTER:
					mSonetWebView.open(String.format(CHATTER_URL_AUTHORIZE, CHATTER_KEY, CHATTER_CALLBACK.toString()));
					break;
				case PINTEREST:
					Cursor pinterestAccount = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID}, Accounts.SERVICE + "=?", new String[]{Integer.toString(PINTEREST)}, null);
					if (pinterestAccount.moveToFirst()) {
						(Toast.makeText(OAuthLogin.this, "Pinterest has already been added.", Toast.LENGTH_LONG)).show();
					} else {
						(Toast.makeText(OAuthLogin.this, "Pinterest currently allows only public, non-authenticated viewing.", Toast.LENGTH_LONG)).show();
						String[] values = getResources().getStringArray(R.array.service_values);
						String[] entries = getResources().getStringArray(R.array.service_entries);
						for (int i = 0, l = values.length; i < l; i++) {
							if (Integer.toString(PINTEREST).equals(values[i])) {
								addAccount(entries[i], null, null, 0, PINTEREST, null);
								break;
							}
						}
					}
					pinterestAccount.close();
					finish();
					break;
				default:
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
		values.put(Accounts.SID, sid);
		if (mAccountId != Sonet.INVALID_ACCOUNT_ID) {
			// re-authenticating
			accountId = Long.toString(mAccountId);
			getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(mAccountId)});
		} else {
			// new account
			accountId = getContentResolver().insert(Accounts.CONTENT_URI, values).getLastPathSegment();
			values.clear();
			values.put(Widget_accounts.ACCOUNT, accountId);
			values.put(Widget_accounts.WIDGET, mWidgetId);
			getContentResolver().insert(Widget_accounts.CONTENT_URI, values);
		}
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
					if (url != null) {
						Uri uri = Uri.parse(url);
						UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
						matcher.addURI("accounts.google.com", "o/oauth2/approval", 1);
						if (matcher.match(uri) == 1) {
							// get the access_token
							String code = view.getTitle().split("=")[1];
							String[] title = view.getTitle().split("=");
							if (title.length > 0) {
								code = title[1];
							}
							if (code != null) {
								final ProgressDialog loadingDialog = new ProgressDialog(OAuthLogin.this);
								final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {

									String refresh_token = null;

									@Override
									protected String doInBackground(String... args) {
										HttpPost httpPost = new HttpPost(GOOGLE_ACCESS);
										List<NameValuePair> params = new ArrayList<NameValuePair>();
										params.add(new BasicNameValuePair("code", args[0]));
										params.add(new BasicNameValuePair("client_id", GOOGLE_CLIENTID));
										params.add(new BasicNameValuePair("client_secret", GOOGLE_CLIENTSECRET));
										params.add(new BasicNameValuePair("redirect_uri", "urn:ietf:wg:oauth:2.0:oob"));
										params.add(new BasicNameValuePair("grant_type", "authorization_code"));
										String response = null;
										try {
											httpPost.setEntity(new UrlEncodedFormEntity(params));
											if ((response = SonetHttpClient.httpResponse(mHttpClient, httpPost)) != null) {
												JSONObject j = new JSONObject(response);
												if (j.has("access_token") && j.has("refresh_token")) {
													refresh_token = j.getString("refresh_token");
													return SonetHttpClient.httpResponse(mHttpClient, new HttpGet(String.format(GOOGLEPLUS_URL_ME, GOOGLEPLUS_BASE_URL, j.getString("access_token"))));
												}
											} else {
												return null;
											}
										} catch (UnsupportedEncodingException e) {
											Log.e(TAG,e.toString());
										} catch (JSONException e) {
											Log.e(TAG,e.toString());
										}
										return null;
									}

									@Override
									protected void onPostExecute(String response) {
										if (loadingDialog.isShowing()) loadingDialog.dismiss();
										boolean finish = true;
										if (response != null) {
											try {
												JSONObject j = new JSONObject(response);
												if (j.has(Sid) && j.has(SdisplayName)) {
													addAccount(j.getString(SdisplayName), refresh_token, "", 0, GOOGLEPLUS, j.getString(Sid));
													// beta message to user
													finish = false;
													AlertDialog.Builder dialog = new AlertDialog.Builder(OAuthLogin.this);
													dialog.setTitle(getResources().getStringArray(R.array.service_entries)[GOOGLEPLUS]);
													dialog.setMessage(R.string.googleplusbeta);
													dialog.setPositiveButton(android.R.string.ok, new OnClickListener() {

														@Override
														public void onClick(DialogInterface arg0, int arg1) {
															arg0.cancel();
															OAuthLogin.this.finish();
														}

													});
													dialog.setCancelable(true);
													dialog.show();
												} else {
													(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
												}
											} catch (JSONException e) {
												Log.e(TAG, e.getMessage());
											}
										} else {
											(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
										}
										if (finish) {
											OAuthLogin.this.finish();
										}
									}
								};
								loadingDialog.setMessage(getString(R.string.loading));
								loadingDialog.setCancelable(true);
								loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
									@Override
									public void onCancel(DialogInterface dialog) {
										if (!asyncTask.isCancelled()) asyncTask.cancel(true);
									}
								});
								loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.cancel();
									}
								});
								asyncTask.execute(code);
								loadingDialog.show();
							} else {
								(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
								OAuthLogin.this.finish();
							}
						}
					}
				}

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					if (url != null) {
						mLoadingDialog.show();
						Uri uri = Uri.parse(url);
						if (TWITTER_CALLBACK.getHost().equals(uri.getHost())) {
							final ProgressDialog loadingDialog = new ProgressDialog(OAuthLogin.this);
							final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {

								@Override
								protected String doInBackground(String... args) {
									if (mSonetOAuth.retrieveAccessToken(args[0])) {
										return SonetHttpClient.httpResponse(mHttpClient, mSonetOAuth.getSignedRequest(new HttpGet("http://api.twitter.com/1/account/verify_credentials.json")));
									} else {
										return null;
									}
								}

								@Override
								protected void onPostExecute(String response) {
									if (loadingDialog.isShowing()) loadingDialog.dismiss();
									if (response != null) {
										try {
											JSONObject jobj = new JSONObject(response);
											addAccount(jobj.getString(Sscreen_name), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, TWITTER, jobj.getString(Sid));
										} catch (JSONException e) {
											Log.e(TAG, e.getMessage());
										}
									} else {
										(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
									}
									OAuthLogin.this.finish();
								}
							};
							loadingDialog.setMessage(getString(R.string.loading));
							loadingDialog.setCancelable(true);
							loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
								@Override
								public void onCancel(DialogInterface dialog) {
									if (!asyncTask.isCancelled()) asyncTask.cancel(true);
								}
							});
							loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
							asyncTask.execute(uri.getQueryParameter(OAUTH_VERIFIER));
							mLoadingDialog.dismiss();
							loadingDialog.show();
						} else if (FOURSQUARE_CALLBACK.getHost().equals(uri.getHost())) {
							// get the access_token
							String token = "";
							String[] parameters = getParams(url);
							for (String parameter : parameters) {
								String[] param = parameter.split("=");
								if (Saccess_token.equals(param[0])) {
									token = param[1];
									break;
								}
							}
							final ProgressDialog loadingDialog = new ProgressDialog(OAuthLogin.this);
							final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
								String token;

								@Override
								protected String doInBackground(String... args) {
									token = args[0];
									return SonetHttpClient.httpResponse(mHttpClient, new HttpGet(args[1]));
								}

								@Override
								protected void onPostExecute(String response) {
									if (loadingDialog.isShowing()) loadingDialog.dismiss();
									if (response != null) {
										JSONObject jobj;
										try {
											jobj = (new JSONObject(response)).getJSONObject("response").getJSONObject("user");
											if (jobj.has("firstName") && jobj.has(Sid)) {
												addAccount(jobj.getString("firstName") + " " + jobj.getString("lastName"), token, "", 0, FOURSQUARE, jobj.getString(Sid));
											} else {
												(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
											}
										} catch (JSONException e) {
											Log.e(TAG, e.getMessage());
										}
									} else {
										(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
									}
									OAuthLogin.this.finish();
								}
							};
							loadingDialog.setMessage(getString(R.string.loading));
							loadingDialog.setCancelable(true);
							loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
								@Override
								public void onCancel(DialogInterface dialog) {
									if (!asyncTask.isCancelled()) asyncTask.cancel(true);
								}
							});
							loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
							asyncTask.execute(token, String.format(FOURSQUARE_URL_ME, FOURSQUARE_BASE_URL, token));
							mLoadingDialog.dismiss();
							loadingDialog.show();
						} else if (FACEBOOK_CALLBACK.getHost().equals(uri.getHost())) {
							String token = "";
							int expiry = 0;
							String[] parameters = getParams(url);
							for (String parameter : parameters) {
								String[] param = parameter.split("=");
								if (Saccess_token.equals(param[0])) {
									token = param[1];
								} else if (Sexpires_in.equals(param[0])) {
									expiry = param[1] == "0" ? 0 : (int) System.currentTimeMillis() + Integer.parseInt(param[1]) * 1000;
								}
							}
							final ProgressDialog loadingDialog = new ProgressDialog(OAuthLogin.this);
							final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
								String token;
								int expiry;

								@Override
								protected String doInBackground(String... args) {
									token = args[0];
									expiry = Integer.parseInt(args[1]);
									return SonetHttpClient.httpResponse(mHttpClient, new HttpGet(args[2]));
								}

								@Override
								protected void onPostExecute(String response) {
									if (loadingDialog.isShowing()) loadingDialog.dismiss();
									if (response != null) {
										try {
											JSONObject jobj = new JSONObject(response);
											if (jobj.has(Sname) && jobj.has(Sid)) {
												addAccount(jobj.getString(Sname), token, "", expiry, FACEBOOK, jobj.getString(Sid));
											} else {
												(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
											}
										} catch (JSONException e) {
											Log.e(TAG, e.getMessage());
										}
									} else {
										(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
									}
									OAuthLogin.this.finish();
								}
							};
							loadingDialog.setMessage(getString(R.string.loading));
							loadingDialog.setCancelable(true);
							loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
								@Override
								public void onCancel(DialogInterface dialog) {
									if (!asyncTask.isCancelled()) asyncTask.cancel(true);
								}
							});
							loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
							asyncTask.execute(token, Integer.toString(expiry), String.format(FACEBOOK_URL_ME, FACEBOOK_BASE_URL, Saccess_token, token));
							mLoadingDialog.dismiss();
							loadingDialog.show();
						} else if (MYSPACE_CALLBACK.getHost().equals(uri.getHost())) {
							final ProgressDialog loadingDialog = new ProgressDialog(OAuthLogin.this);
							final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {

								@Override
								protected String doInBackground(String... args) {
									if (mSonetOAuth.retrieveAccessToken(args[0])) {
										return SonetHttpClient.httpResponse(mHttpClient, mSonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_URL_ME, MYSPACE_BASE_URL))));
									} else {
										return null;
									}
								}

								@Override
								protected void onPostExecute(String response) {
									if (loadingDialog.isShowing()) loadingDialog.dismiss();
									if (response != null) {
										try {
											JSONObject jobj = new JSONObject(response);
											JSONObject person = jobj.getJSONObject("person");
											if (person.has(SdisplayName) && person.has(Sid)) {
												addAccount(person.getString(SdisplayName), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, MYSPACE, person.getString(Sid));
											} else {
												(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
											}
										} catch (JSONException e) {
											Log.e(TAG, e.getMessage());
										}
									} else {
										(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
									}
									OAuthLogin.this.finish();
								}
							};
							loadingDialog.setMessage(getString(R.string.loading));
							loadingDialog.setCancelable(true);
							loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
								@Override
								public void onCancel(DialogInterface dialog) {
									if (!asyncTask.isCancelled()) asyncTask.cancel(true);
								}
							});
							loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
							asyncTask.execute(uri.getQueryParameter(OAUTH_VERIFIER));
							mLoadingDialog.dismiss();
							loadingDialog.show();
						} else if (LINKEDIN_CALLBACK.getHost().equals(uri.getHost())) {
							final ProgressDialog loadingDialog = new ProgressDialog(OAuthLogin.this);
							final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {

								@Override
								protected String doInBackground(String... args) {
									if (mSonetOAuth.retrieveAccessToken(args[0])) {
										HttpGet httpGet = new HttpGet(String.format(LINKEDIN_URL_ME, LINKEDIN_BASE_URL));
										for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
										return SonetHttpClient.httpResponse(mHttpClient, mSonetOAuth.getSignedRequest(httpGet));
									} else {
										return null;
									}
								}

								@Override
								protected void onPostExecute(String response) {
									if (loadingDialog.isShowing()) loadingDialog.dismiss();
									if (response != null) {
										try {
											JSONObject jobj = new JSONObject(response);
											if (jobj.has("firstName") && jobj.has(Sid)) {
												addAccount(jobj.getString("firstName") + " " + jobj.getString("lastName"), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, LINKEDIN, jobj.getString(Sid));
											} else {
												(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
											}
										} catch (JSONException e) {
											Log.e(TAG, e.getMessage());
										}
									} else {
										(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
									}
									OAuthLogin.this.finish();
								}
							};
							loadingDialog.setMessage(getString(R.string.loading));
							loadingDialog.setCancelable(true);
							loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
								@Override
								public void onCancel(DialogInterface dialog) {
									if (!asyncTask.isCancelled()) asyncTask.cancel(true);
								}
							});
							loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
							asyncTask.execute(uri.getQueryParameter(OAUTH_VERIFIER));
							mLoadingDialog.dismiss();
							loadingDialog.show();
						} else if (IDENTICA_CALLBACK.getHost().equals(uri.getHost())) {
							final ProgressDialog loadingDialog = new ProgressDialog(OAuthLogin.this);
							final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {

								@Override
								protected String doInBackground(String... args) {
									if (mSonetOAuth.retrieveAccessToken(args[0])) {
										return SonetHttpClient.httpResponse(mHttpClient, mSonetOAuth.getSignedRequest(new HttpGet("https://identi.ca/api/account/verify_credentials.json")));
									} else {
										return null;
									}
								}

								@Override
								protected void onPostExecute(String response) {
									if (loadingDialog.isShowing()) loadingDialog.dismiss();
									if (response != null) {
										try {
											JSONObject jobj = new JSONObject(response);
											addAccount(jobj.getString(Sscreen_name), mSonetOAuth.getToken(), mSonetOAuth.getTokenSecret(), 0, IDENTICA, jobj.getString(Sid));
										} catch (JSONException e) {
											Log.e(TAG, e.getMessage());
										}
									} else {
										(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
									}
									OAuthLogin.this.finish();
								}
							};
							loadingDialog.setMessage(getString(R.string.loading));
							loadingDialog.setCancelable(true);
							loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
								@Override
								public void onCancel(DialogInterface dialog) {
									if (!asyncTask.isCancelled()) asyncTask.cancel(true);
								}
							});
							loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
							asyncTask.execute(uri.getQueryParameter(OAUTH_VERIFIER));
							mLoadingDialog.dismiss();
							loadingDialog.show();
						} else if (CHATTER_CALLBACK.getHost().equals(uri.getHost())) {
							// get the access_token
							String token = null,
							refresh_token = null,
							instance_url = null;
							String[] parameters = getParams(url);
							for (String parameter : parameters) {
								String[] param = parameter.split("=");
								if (Saccess_token.equals(param[0])) {
									token = Uri.decode(param[1]);
								} else if ("refresh_token".equals(param[0])) {
									refresh_token = Uri.decode(param[1]);
								} else if ("instance_url".equals(param[0])) {
									instance_url = Uri.decode(param[1]);
								}
							}
							if ((token != null) && (refresh_token != null) && (instance_url != null)) {
								final ProgressDialog loadingDialog = new ProgressDialog(OAuthLogin.this);
								final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
									String refresh_token;

									@Override
									protected String doInBackground(String... args) {
										refresh_token = args[2];
										HttpGet httpGet = new HttpGet(String.format(CHATTER_URL_ME, args[1]));
										httpGet.setHeader("Authorization", "OAuth " + args[0]);
										return SonetHttpClient.httpResponse(mHttpClient, httpGet);
									}

									@Override
									protected void onPostExecute(String response) {
										if (loadingDialog.isShowing()) loadingDialog.dismiss();
										if (response != null) {
											try {
												JSONObject jobj = new JSONObject(response);
												if (jobj.has(Sname) && jobj.has(Sid)) {
													// save the refresh_token to retrieve updated access_token
													addAccount(jobj.getString(Sname), refresh_token, "", 0, CHATTER, jobj.getString(Sid));
												} else {
													(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
												}
											} catch (JSONException e) {
												Log.e(TAG, e.getMessage());
											}
										} else {
											// check for REST_API enabled
											(Toast.makeText(OAuthLogin.this, "Salesforce does not allow REST API access for Professional and Group Editions. Please ask them to make it available.", Toast.LENGTH_LONG)).show();
										}
										OAuthLogin.this.finish();
									}
								};
								loadingDialog.setMessage(getString(R.string.loading));
								loadingDialog.setCancelable(true);
								loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
									@Override
									public void onCancel(DialogInterface dialog) {
										if (!asyncTask.isCancelled()) asyncTask.cancel(true);
									}
								});
								loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.cancel();
									}
								});
								asyncTask.execute(token, instance_url, refresh_token);
								mLoadingDialog.dismiss();
								loadingDialog.show();
							} else {
								(Toast.makeText(OAuthLogin.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
								mLoadingDialog.dismiss();
								OAuthLogin.this.finish();
							}
						} else {
							return false;// allow google to redirect
						}
					}
					return true;
				}

			});
			WebSettings webSettings = mWebView.getSettings();
			webSettings.setJavaScriptEnabled(true);
			webSettings.setDefaultTextEncodingName("UTF-8");
		}

		public void open(String url) {
			if (url != null) {
				mWebView.loadUrl(url);
			} else {
				OAuthLogin.this.finish();
			}
		}

	}

	private String[] getParams(String url) {
		if (url.contains("?")) {
			return url.substring(url.indexOf("?") + 1).replace("#", "&").split("&");
		} else if (url.contains("#")) {
			return url.substring(url.indexOf("#") + 1).split("&");
		} else {
			return new String[0];
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
