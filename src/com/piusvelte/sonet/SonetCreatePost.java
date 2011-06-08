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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.ads.*;
import com.piusvelte.sonet.Sonet.Accounts;

import static com.piusvelte.sonet.Sonet.ACCOUNTS_QUERY;
import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.BUZZ_ACTIVITY;
import static com.piusvelte.sonet.Sonet.BUZZ_BASE_URL;
import static com.piusvelte.sonet.Sonet.BUZZ_ACTIVITY_BODY;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.Sonet.FACEBOOK_POST;
import static com.piusvelte.sonet.Sonet.FACEBOOK_SEARCH;
import static com.piusvelte.sonet.Sonet.FACEBOOK_CHECKIN;
import static com.piusvelte.sonet.Sonet.FACEBOOK_COORDINATES;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_BASE_URL;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_CHECKIN;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_CHECKIN_NO_VENUE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_CHECKIN_NO_SHOUT;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_SEARCH;
import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOOD;
import static com.piusvelte.sonet.Sonet.MYSPACE_STATUSMOOD_BODY;
import static com.piusvelte.sonet.Sonet.TOKEN;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.Sonet.TWITTER_UPDATE;
import static com.piusvelte.sonet.Sonet.TWITTER_SEARCH;
import static com.piusvelte.sonet.Sonet.LINKEDIN_BASE_URL;
import static com.piusvelte.sonet.Sonet.LINKEDIN_POST;
import static com.piusvelte.sonet.Sonet.LINKEDIN_POST_BODY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_API_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_SECRET;
import static com.piusvelte.sonet.SonetTokens.TWITTER_KEY;
import static com.piusvelte.sonet.SonetTokens.TWITTER_SECRET;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_KEY;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_SECRET;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_SECRET;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SonetCreatePost extends Activity implements OnKeyListener, OnClickListener, TextWatcher {
	private static final String TAG = "SonetCreatePost";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private HashMap<Long, String> mAccountsToPost = new HashMap<Long, String>();
	private EditText mMessage;
	private Button mSend;
	private Button mLocation;
	private Button mAccounts;
	private TextView mCount;
	//	private ProgressDialog loadingDialog;
	private String mLat = null,
	mLong = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// allow posting to multiple services if an account is defined
		// allow selecting which accounts to use
		// get existing comments, allow liking|unliking those comments
		setContentView(R.layout.post);
		AdView adView = new AdView(this, AdSize.BANNER, Sonet.GOOGLE_AD_ID);
		((LinearLayout) findViewById(R.id.ad)).addView(adView);
		adView.loadAd(new AdRequest());

		mMessage = (EditText) findViewById(R.id.message);
		mSend = (Button) findViewById(R.id.send);
		mLocation = (Button) findViewById(R.id.location);
		mAccounts = (Button) findViewById(R.id.accounts);
		mCount = (TextView) findViewById(R.id.count);
		if ((getIntent() != null) && (getIntent().getData() != null)) {
			Uri data = getIntent().getData();
			if (data.toString().contains(Accounts.CONTENT_URI.toString())) {
				// default to the account passed in, but allow selecting additional accounts
				Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.WIDGET, ACCOUNTS_QUERY}, Accounts._ID + "=?", new String[]{data.getLastPathSegment()}, null);
				if (account.moveToFirst()) {
					mAppWidgetId = account.getInt(account.getColumnIndex(Accounts.WIDGET));
					mAccountsToPost.put(account.getLong(account.getColumnIndex(Accounts._ID)), null);
					mAccounts.setText(account.getString(account.getColumnIndex(Accounts.USERNAME)));
				}
				account.close();
			} else {
				// default widget post
				mAppWidgetId = Integer.parseInt(data.getLastPathSegment());
			}
		}
		mAccounts.setEnabled(true);
		mAccounts.setOnClickListener(this);
		mLocation.setEnabled(true);
		mLocation.setOnClickListener(this);
		mMessage.addTextChangedListener(this);
		mMessage.setOnKeyListener(this);
		mSend.setOnClickListener(this);
	}

	private void setLocation(final long accountId) {
		final AsyncTask<String, Void, String> asyncTask;
		Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SERVICE, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			final ProgressDialog loadingDialog = new ProgressDialog(this);
			switch (account.getInt(account.getColumnIndex(Accounts.SERVICE))) {
			case TWITTER:
				asyncTask = new AsyncTask<String, Void, String>() {
					@Override
					protected String doInBackground(String... arg0) {
						// anonymous requests are rate limited to 150 per hour
						// authenticated requests are rate limited to 350 per hour, so authenticate this!
						SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, arg0[0], arg0[1]);
						return sonetOAuth.httpResponse(new HttpGet(String.format(TWITTER_SEARCH, TWITTER_BASE_URL, mLat, mLong)));
					}
					@Override
					protected void onPostExecute(String response) {
						if (loadingDialog.isShowing()) loadingDialog.dismiss();
						if (response != null) {
							try {
								JSONArray places = new JSONObject(response).getJSONObject("result").getJSONArray("places");
								final String placesNames[] = new String[places.length()];
								final String placesIds[] = new String[places.length()];
								for (int i = 0; i < places.length(); i++) {
									JSONObject place = places.getJSONObject(i);
									placesNames[i] = place.getString("full_name");
									placesIds[i] = place.getString("id");
								}
								AlertDialog.Builder dialog = new AlertDialog.Builder(SonetCreatePost.this);
								dialog.setSingleChoiceItems(placesNames, -1, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										mLocation.setText(placesNames[which]);
										mAccountsToPost.put(accountId, placesIds[which]);
										dialog.dismiss();
									}
								})
								.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.cancel();
									}
								})
								.show();
							} catch (JSONException e) {
								Log.e(TAG, e.toString());
							}
						} else {
							(Toast.makeText(SonetCreatePost.this, getString(R.string.twitter) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
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
				loadingDialog.show();
				asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
				break;
			case FACEBOOK:
				asyncTask = new AsyncTask<String, Void, String>() {
					@Override
					protected String doInBackground(String... arg0) {
						return Sonet.httpResponse(new HttpGet(String.format(FACEBOOK_SEARCH, FACEBOOK_BASE_URL, mLat, mLong, TOKEN, arg0[0])));
					}
					@Override
					protected void onPostExecute(String response) {
						if (loadingDialog.isShowing()) loadingDialog.dismiss();
						if (response != null) {
							try {
								JSONArray places = new JSONObject(response).getJSONArray("data");
								final String placesNames[] = new String[places.length()];
								final String placesIds[] = new String[places.length()];
								for (int i = 0; i < places.length(); i++) {
									JSONObject place = places.getJSONObject(i);
									placesNames[i] = place.getString("name");
									placesIds[i] = place.getString("id");
								}
								AlertDialog.Builder dialog = new AlertDialog.Builder(SonetCreatePost.this);
								dialog.setSingleChoiceItems(placesNames, -1, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										mLocation.setText(placesNames[which]);
										mAccountsToPost.put(accountId, placesIds[which]);
										dialog.dismiss();
									}
								})
								.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.cancel();
									}
								})
								.show();
							} catch (JSONException e) {
								Log.e(TAG, e.toString());
							}
						} else {
							(Toast.makeText(SonetCreatePost.this, getString(R.string.twitter) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
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
				loadingDialog.show();
				asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
				break;
			case FOURSQUARE:
				asyncTask = new AsyncTask<String, Void, String>() {
					@Override
					protected String doInBackground(String... arg0) {
						return Sonet.httpResponse(new HttpGet(String.format(FOURSQUARE_SEARCH, FOURSQUARE_BASE_URL, mLat, mLong, arg0[0])));
					}
					@Override
					protected void onPostExecute(String response) {
						if (loadingDialog.isShowing()) loadingDialog.dismiss();
						if (response != null) {
							try {
								JSONArray groups = new JSONObject(response).getJSONObject("response").getJSONArray("groups");
								for (int g = 0; g < groups.length(); g++) {
									JSONObject group = groups.getJSONObject(g);
									if (group.getString("name").equals("Nearby")) {
										JSONArray places = group.getJSONArray("items");
										final String placesNames[] = new String[places.length()];
										final String placesIds[] = new String[places.length()];
										for (int i = 0; i < places.length(); i++) {
											JSONObject place = places.getJSONObject(i);
											placesNames[i] = place.getString("name");
											placesIds[i] = place.getString("id");
										}
										AlertDialog.Builder dialog = new AlertDialog.Builder(SonetCreatePost.this);
										dialog.setSingleChoiceItems(placesNames, -1, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												mLocation.setText(placesNames[which]);
												mAccountsToPost.put(accountId, placesIds[which]);
												dialog.dismiss();
											}
										})
										.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												dialog.cancel();
											}
										})
										.show();
										break;
									}
								}
							} catch (JSONException e) {
								Log.e(TAG, e.toString());
							}
						} else {
							(Toast.makeText(SonetCreatePost.this, getString(R.string.foursquare) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
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
				loadingDialog.show();
				asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
				break;
			}
		}
		account.close();
	}

	@Override
	public void onClick(View v) {
		if (v == mLocation) {
			// set the location
			if (mAccountsToPost.size() > 0) {
				LocationManager locationManager = (LocationManager) SonetCreatePost.this.getSystemService(Context.LOCATION_SERVICE);
				Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				if (location != null) {
					mLat = Double.toString(location.getLatitude());
					mLong = Double.toString(location.getLongitude());
					if (mAccountsToPost.size() == 1) {
						setLocation(mAccountsToPost.keySet().iterator().next());
					} else {
						// dialog to select an account
						Iterator<Long> accountIds = mAccountsToPost.keySet().iterator();
						HashMap<Long, String> accountEntries = new HashMap<Long, String>();
						while (accountIds.hasNext()) {
							Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(accountIds.next())}, null);
							if (account.moveToFirst()) {
								int service = account.getInt(account.getColumnIndex(Accounts.SERVICE));
								// only get accounts which have been selected and are supported for location
								if ((service == TWITTER) || (service == FACEBOOK) || (service == FOURSQUARE)) {
									accountEntries.put(account.getLong(account.getColumnIndex(Accounts._ID)), account.getString(account.getColumnIndex(Accounts.USERNAME)));
								}
							}
						}
						int size = accountEntries.size();
						if (size != 0) {
							final long[] accountIndexes = new long[size];
							final String[] accounts = new String[size];
							int i = 0;
							Iterator<Map.Entry<Long, String>> entries = accountEntries.entrySet().iterator();
							while (entries.hasNext()) {
								Map.Entry<Long, String> entry = entries.next();
								accountIndexes[i] = entry.getKey();
								accounts[i++] = entry.getValue();
							}
							AlertDialog.Builder dialog = new AlertDialog.Builder(this);
							dialog.setTitle(R.string.accounts)
							.setSingleChoiceItems(accounts, -1, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									setLocation(accountIndexes[which]);
									dialog.dismiss();
								}
							})
							.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})
							.show();
						}
					}
				} else {
					(Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_LONG)).show();
				}
			}
		} else if (v == mSend) {
			if ((mMessage.getText().toString() != null) && (mMessage.getText().toString().length() > 0) && (mAccountsToPost.size() > 0)) {
				mMessage.setEnabled(false);
				mSend.setEnabled(false);
				mAccounts.setEnabled(false);
				mLocation.setEnabled(false);
				Iterator<Map.Entry<Long, String>> entrySet = mAccountsToPost.entrySet().iterator();
				while (entrySet.hasNext()) {
					Map.Entry<Long, String> entry = entrySet.next();
					final long accountId = entry.getKey();
					final String placeId = entry.getValue();
					// post or comment!
					Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
					if (account.moveToFirst()) {
						AsyncTask<String, Void, String> asyncTask;
						switch (account.getInt(account.getColumnIndex(Accounts.SERVICE))) {
						case TWITTER:
							// limit tweets to 140, breaking up the message if necessary
							String message = mMessage.getText().toString();
							while (message.length() > 0) {
								final String send;
								final boolean finish;
								if (message.length() > 140) {
									// need to break on a word
									int end = 0;
									int nextSpace = 0;
									for (int i = 0; i < message.length(); i++) {
										end = nextSpace;
										if (message.substring(i, i + 1).equals(" ")) {
											nextSpace = i;
										}
									}
									// in case there are no spaces, just break on 140
									if (end == 0) {
										end = 140;
									}
									send = message.substring(0, end);
									message = message.substring(end + 1);
									finish = false;
								} else {
									send = message;
									message = "";
									finish = true;
								}
								AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {
									@Override
									protected String doInBackground(String... arg0) {
										SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, arg0[0], arg0[1]);
										HttpPost httpPost = new HttpPost(String.format(TWITTER_UPDATE, TWITTER_BASE_URL));
										// resolve Error 417 Expectation by Twitter
										httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
										List<NameValuePair> params = new ArrayList<NameValuePair>();
										params.add(new BasicNameValuePair("status", send));
										if (placeId != null) {
											params.add(new BasicNameValuePair("place_id", placeId));
											params.add(new BasicNameValuePair("lat", mLat));
											params.add(new BasicNameValuePair("long", mLong));
										}
										try {
											httpPost.setEntity(new UrlEncodedFormEntity(params));
											return sonetOAuth.httpResponse(httpPost);
										} catch (UnsupportedEncodingException e) {
											Log.e(TAG, e.toString());
										}
										return null;
									}

									@Override
									protected void onPostExecute(String response) {
										(Toast.makeText(SonetCreatePost.this, getString(R.string.twitter) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
										if (finish) {
											finish();
										}
									}
								};
								task.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
							}
							break;
						case FACEBOOK:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									HttpPost httpPost;
									List<NameValuePair> params = new ArrayList<NameValuePair>();
									if (placeId != null) {
										httpPost = new HttpPost(String.format(FACEBOOK_CHECKIN, FACEBOOK_BASE_URL, TOKEN, arg0[0]));
										params.add(new BasicNameValuePair("place", placeId));
										params.add(new BasicNameValuePair("coordinates", String.format(FACEBOOK_COORDINATES, mLat, mLong)));
									} else {
										httpPost = new HttpPost(String.format(FACEBOOK_POST, FACEBOOK_BASE_URL, TOKEN, arg0[0]));
									}
									params.add(new BasicNameValuePair("message", mMessage.getText().toString()));
									try {
										httpPost.setEntity(new UrlEncodedFormEntity(params));
										return Sonet.httpResponse(httpPost);
									} catch (UnsupportedEncodingException e) {
										Log.e(TAG, e.toString());
									}
									return null;
								}

								@Override
								protected void onPostExecute(String response) {
									(Toast.makeText(SonetCreatePost.this, getString(R.string.facebook) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
									finish();
								}
							};
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
							break;
						case MYSPACE:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, arg0[0], arg0[1]);
									try {
										HttpPut httpPut = new HttpPut(String.format(MYSPACE_URL_STATUSMOOD, MYSPACE_BASE_URL));
										httpPut.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOOD_BODY, mMessage.getText().toString())));
										return sonetOAuth.httpResponse(httpPut);
									} catch (IOException e) {
										Log.e(TAG, e.toString());
									}
									return null;
								}

								@Override
								protected void onPostExecute(String response) {
									// warn users about myspace permissions
									if (response != null) {
										(Toast.makeText(SonetCreatePost.this, getString(R.string.myspace) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
									} else {
										(Toast.makeText(SonetCreatePost.this, getString(R.string.myspace) + " " + getString(R.string.failure) + " " + getString(R.string.myspace_permissions_message), Toast.LENGTH_LONG)).show();
									}
									finish();
								}
							};
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
							break;
						case BUZZ:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, arg0[0], arg0[1]);
									try {
										HttpPost httpPost = new HttpPost(String.format(BUZZ_ACTIVITY, BUZZ_BASE_URL, BUZZ_API_KEY));
										httpPost.setEntity(new StringEntity(String.format(BUZZ_ACTIVITY_BODY, mMessage.getText().toString())));
										httpPost.addHeader(new BasicHeader("Content-Type", "application/json"));
										return sonetOAuth.httpResponse(httpPost);
									} catch (IOException e) {
										Log.e(TAG, e.toString());
									}
									return null;
								}

								@Override
								protected void onPostExecute(String response) {
									(Toast.makeText(SonetCreatePost.this, getString(R.string.buzz) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
									finish();
								}
							};
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
							break;
						case FOURSQUARE:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									try {
										String message = URLEncoder.encode(mMessage.getText().toString(), "UTF-8");
										HttpPost httpPost;
										if (placeId != null) {
											if ((message != null) && (message.length() > 0)) {
												httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN, FOURSQUARE_BASE_URL, placeId, message, mLat, mLong, arg0[0]));
											} else {
												httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_SHOUT, FOURSQUARE_BASE_URL, placeId, mLat, mLong, arg0[0]));												
											}
										} else {
											httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_VENUE, FOURSQUARE_BASE_URL, message, arg0[0]));
										}
										return Sonet.httpResponse(httpPost);
									} catch (UnsupportedEncodingException e) {
										Log.e(TAG, e.toString());
									}
									return null;
								}

								@Override
								protected void onPostExecute(String response) {
									(Toast.makeText(SonetCreatePost.this, getString(R.string.foursquare) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
									finish();
								}
							};
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
							break;
						case LINKEDIN:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, arg0[0], arg0[1]);
									try {
										HttpPost httpPost = new HttpPost(String.format(LINKEDIN_POST, LINKEDIN_BASE_URL));
										httpPost.setEntity(new StringEntity(String.format(LINKEDIN_POST_BODY, "", mMessage.getText().toString())));
										httpPost.addHeader(new BasicHeader("Content-Type", "application/xml"));
										return sonetOAuth.httpResponse(httpPost);
									} catch (IOException e) {
										Log.e(TAG, e.toString());
									}
									return null;
								}

								@Override
								protected void onPostExecute(String response) {
									(Toast.makeText(SonetCreatePost.this, getString(R.string.linkedin) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
									finish();
								}
							};
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
						}
					}
					account.close();
				}
			} else {
				(Toast.makeText(SonetCreatePost.this, "error parsing message body", Toast.LENGTH_LONG)).show();
				mMessage.setEnabled(true);
				mSend.setEnabled(true);
				mAccounts.setEnabled(true);
				mLocation.setEnabled(true);
			}
		} else if (v == mAccounts) {
			Cursor c = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY, Accounts.SERVICE}, Accounts.WIDGET + "=?", new String[]{Integer.toString(mAppWidgetId)}, null);
			if (c.moveToFirst()) {
				int iid = c.getColumnIndex(Accounts._ID),
				iusername = c.getColumnIndex(Accounts.USERNAME),
				iservice = c.getColumnIndex(Accounts.SERVICE),
				i = 0;
				final long[] accountIndexes = new long[c.getCount()];
				final String[] accounts = new String[c.getCount()];
				final boolean[] defaults = new boolean[c.getCount()];
				final int[] accountServices = new int[c.getCount()];
				while (!c.isAfterLast()) {
					long id = c.getLong(iid);
					accountIndexes[i] = id;
					accounts[i] = c.getString(iusername);
					accountServices[i] = c.getInt(iservice);
					defaults[i++] = mAccountsToPost.containsKey(id);
					c.moveToNext();
				}
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle(R.string.accounts)
				.setMultiChoiceItems(accounts, defaults, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						if (isChecked) {
							final long accountId = accountIndexes[which];
							mAccountsToPost.put(accountId, null);
							mAccounts.setText(accounts[which]);
							// set location, only for supported services, TWITTER, FACEBOOK, FOURSQUARE
							switch (accountServices[which]) {
							case TWITTER:
							case FACEBOOK:
							case FOURSQUARE:
								if (mLat == null) {
									LocationManager locationManager = (LocationManager) SonetCreatePost.this.getSystemService(Context.LOCATION_SERVICE);
									Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
									if (location != null) {
										mLat = Double.toString(location.getLatitude());
										mLong = Double.toString(location.getLongitude());
									}										
								}
								if ((mLat != null) && (mLong != null)) {
									AlertDialog.Builder locationDialog = new AlertDialog.Builder(SonetCreatePost.this);
									locationDialog.setTitle(R.string.set_location)
									.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											setLocation(accountId);
											dialog.dismiss();
										}
									})
									.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											dialog.dismiss();
										}
									})
									.show();
								}
							}
						} else {
							mAccountsToPost.remove(accountIndexes[which]);
							mAccounts.setText(mAccountsToPost.size() == 0 ? getString(R.string.accounts) : accounts[Sonet.arrayIndex(accountIndexes, mAccountsToPost.keySet().iterator().next())]);
							mLocation.setText(R.string.location);
						}
					}
				})
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
			}
			c.close();
		}
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		mCount.setText(Integer.toString(mMessage.getText().toString().length()));
		return false;
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		mCount.setText(Integer.toString(arg0.toString().length()));
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

}