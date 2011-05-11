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
import org.apache.http.client.methods.HttpDelete;
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
import static com.piusvelte.sonet.Sonet.BUZZ_COMMENT;
import static com.piusvelte.sonet.Sonet.BUZZ_LIKE;
import static com.piusvelte.sonet.Sonet.BUZZ_GET_LIKE;
import static com.piusvelte.sonet.Sonet.BUZZ_ACTIVITY_BODY;
import static com.piusvelte.sonet.Sonet.BUZZ_COMMENT_BODY;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.Sonet.FACEBOOK_POST;
import static com.piusvelte.sonet.Sonet.FACEBOOK_COMMENTS;
import static com.piusvelte.sonet.Sonet.FACEBOOK_SEARCH;
import static com.piusvelte.sonet.Sonet.FACEBOOK_CHECKIN;
import static com.piusvelte.sonet.Sonet.FACEBOOK_COORDINATES;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_BASE_URL;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_CHECKIN;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_ADDCOMMENT;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_SEARCH;
import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Sonet.LINKEDIN_HEADERS;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOOD;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOODCOMMENTS;
import static com.piusvelte.sonet.Sonet.MYSPACE_STATUSMOOD_BODY;
import static com.piusvelte.sonet.Sonet.MYSPACE_STATUSMOODCOMMENTS_BODY;
import static com.piusvelte.sonet.Sonet.TOKEN;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.Sonet.TWITTER_RETWEET;
import static com.piusvelte.sonet.Sonet.TWITTER_USER;
import static com.piusvelte.sonet.Sonet.TWITTER_UPDATE;
import static com.piusvelte.sonet.Sonet.TWITTER_SEARCH;
import static com.piusvelte.sonet.Sonet.LINKEDIN_BASE_URL;
import static com.piusvelte.sonet.Sonet.LINKEDIN_POST;
import static com.piusvelte.sonet.Sonet.LINKEDIN_POST_BODY;
import static com.piusvelte.sonet.Sonet.LINKEDIN_COMMENT_BODY;
import static com.piusvelte.sonet.Sonet.LINKEDIN_UPDATE;
import static com.piusvelte.sonet.Sonet.LINKEDIN_UPDATE_COMMENTS;
import static com.piusvelte.sonet.Sonet.LINKEDIN_IS_LIKED;
import static com.piusvelte.sonet.Sonet.LINKEDIN_LIKE_BODY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_API_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_SECRET;
import static com.piusvelte.sonet.SonetTokens.TWITTER_KEY;
import static com.piusvelte.sonet.SonetTokens.TWITTER_SECRET;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_KEY;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_SECRET;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_SECRET;

import com.piusvelte.sonet.Sonet.Statuses_styles;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class SonetCreatePost extends Activity implements OnKeyListener, OnClickListener {
	private static final String TAG = "SonetCreatePost";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private HashMap<Integer, String> mAccountsToPost = new HashMap<Integer, String>();
	private String mSid = null;
	private String mEsid;
	private Uri mData;
	private EditText mMessage;
	private Button mSend;
	private Button mLocation;
	private Button mComments;
	private Button mAccounts;
	private Button mLike;
	private TextView mCount;
	private ProgressDialog mLoadingDialog;
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
		mComments = (Button) findViewById(R.id.comments);
		mAccounts = (Button) findViewById(R.id.accounts);
		mLike = (Button) findViewById(R.id.like);
		mCount = (TextView) findViewById(R.id.count);
		Intent intent = getIntent();
		if (intent != null) {
			mData = intent.getData();
			// if the uri is Statuses_styles, then this is a comment or reply
			// if the uri is Accounts, then this is a post or tweet
			Cursor account;
			if (mData.toString().contains(Statuses_styles.CONTENT_URI.toString())) {
				Cursor status = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.ACCOUNT, Statuses_styles.SID, Statuses_styles.ESID, Statuses_styles.WIDGET}, Statuses_styles._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
				if (status.moveToFirst()) {
					final int service = status.getInt(status.getColumnIndex(Statuses_styles.SERVICE));
					final int accountId = status.getInt(status.getColumnIndex(Statuses_styles.ACCOUNT));
					mSid = Sonet.removeUnderscore(status.getString(status.getColumnIndex(Statuses_styles.SID)));
					mEsid = Sonet.removeUnderscore(status.getString(status.getColumnIndex(Statuses_styles.ESID)));
					mAppWidgetId = status.getInt(status.getColumnIndex(Statuses_styles.WIDGET));
					mAccountsToPost.put(accountId, null);
					// allow comment viewing for services that having commenting
					// loading liking/retweeting
					AsyncTask<String, Void, String> asyncTask;
					switch (service) {
					case TWITTER:
						// reply, load the user's name @username
						account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
						if (account.moveToFirst()) {
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, arg0[0], arg0[1]);
									return sonetOAuth.httpResponse(new HttpGet(String.format(TWITTER_USER, TWITTER_BASE_URL, mEsid)));
								}

								@Override
								protected void onPostExecute(String response) {
									if (response != null) {
										try {
											JSONArray users = new JSONArray(response);
											if (users.length() > 0) {
												mMessage.setText("");
												mMessage.append("@" + users.getJSONObject(0).getString("screen_name") + " ");
											}
										} catch (JSONException e) {
											Log.e(TAG,e.toString());
										}
									}
									mMessage.setEnabled(true);
								}
							};
							mMessage.setEnabled(false);
							mMessage.setText(R.string.loading);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
						}
						account.close();
						mLike.setText(R.string.retweet);
						mLike.setEnabled(true);
						mLike.setOnClickListener(this);
						break;
					case FACEBOOK:
						mComments.setEnabled(true);
						mComments.setOnClickListener(this);
						account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
						if (account.moveToFirst()) {
							final String sid = Sonet.removeUnderscore(account.getString(account.getColumnIndex(Accounts.SID)));
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									return Sonet.httpResponse(new HttpGet(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, mSid, TOKEN, arg0[0])));
								}

								@Override
								protected void onPostExecute(String response) {
									boolean liked = false;
									if (response != null) {
										try {
											JSONArray likes = new JSONObject(response).getJSONArray("data");
											for (int i = 0; i < likes.length(); i++) {
												JSONObject like = likes.getJSONObject(i);
												if (like.getString("id").equals(sid)) {
													liked = true;
													break;
												}
											}
										} catch (JSONException e) {
											Log.e(TAG,e.toString());
										}
									}
									mLike.setText(liked ? R.string.unlike : R.string.like);
									mLike.setEnabled(true);
									mLike.setOnClickListener(SonetCreatePost.this);
								}
							};
							mLike.setText(R.string.loading);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
						}
						account.close();
						break;
					case BUZZ:
						mComments.setEnabled(true);
						mComments.setOnClickListener(this);
						account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
						if (account.moveToFirst()) {
							final String sid = Sonet.removeUnderscore(account.getString(account.getColumnIndex(Accounts.SID)));
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, arg0[0], arg0[1]);
									return sonetOAuth.httpResponse(new HttpGet(String.format(BUZZ_GET_LIKE, BUZZ_BASE_URL, mSid, BUZZ_API_KEY)));
								}

								@Override
								protected void onPostExecute(String response) {
									boolean liked = false;
									if (response != null) {
										try {
											JSONObject data = new JSONObject(response).getJSONObject("data");
											if (data.has("entry")) {
												JSONArray entries = data.getJSONArray("entry");
												for (int i = 0; i < entries.length(); i++) {
													JSONObject entry = entries.getJSONObject(i);
													if (entry.getString("id").equals(sid)) {
														liked = true;
														break;
													}
												}
											}
										} catch (JSONException e) {
											Log.e(TAG,e.toString());
										}
									}
									mLike.setText(liked ? R.string.unlike : R.string.like);
									mLike.setEnabled(true);
									mLike.setOnClickListener(SonetCreatePost.this);
								}
							};
							mLike.setText(R.string.loading);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
						}
						account.close();
						break;
					case LINKEDIN:
						account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
						if (account.moveToFirst()) {
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, arg0[0], arg0[1]);
									HttpGet httpGet = new HttpGet(String.format(LINKEDIN_UPDATE, LINKEDIN_BASE_URL, mSid));
									for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
									return sonetOAuth.httpResponse(httpGet);
								}

								@Override
								protected void onPostExecute(String response) {
									boolean liked = false;
									if (response != null) {
										try {
											JSONObject data = new JSONObject(response);
											if (data.has("isCommentable") && data.getBoolean("isCommentable")) {
												mComments.setEnabled(true);
												mComments.setOnClickListener(SonetCreatePost.this);
											} else {
												mSend.setEnabled(false);
												mMessage.setEnabled(false);
												mMessage.setText(R.string.uncommentable);
											}
											if (data.has("isLikable")) {
												liked = data.has("isLiked") && data.getBoolean("isLiked");
												mLike.setEnabled(true);
												mLike.setOnClickListener(SonetCreatePost.this);
											} else {
												mLike.setText(R.string.unlikable);
											}
										} catch (JSONException e) {
											Log.e(TAG,e.toString());
										}
									}
									mLike.setText(liked ? R.string.unlike : R.string.like);
								}
							};
							mLike.setEnabled(false);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
						}
						account.close();
						break;
					default:
						mComments.setEnabled(true);
						mComments.setOnClickListener(this);
					}
				}
				status.close();
			} else {
				// default to the account passed in, but allow selecting additional accounts
				account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.WIDGET, ACCOUNTS_QUERY}, Accounts._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
				if (account.moveToFirst()) {
					mAppWidgetId = account.getInt(account.getColumnIndex(Accounts.WIDGET));
					mAccountsToPost.put(account.getInt(account.getColumnIndex(Accounts._ID)), null);
					mAccounts.setText(account.getString(account.getColumnIndex(Accounts.USERNAME)));
					mAccounts.setEnabled(true);
					mAccounts.setOnClickListener(this);
					mLocation.setEnabled(true);
					mLocation.setOnClickListener(this);
				}
				account.close();
			}
		}
		mMessage.setOnKeyListener(this);
		mSend.setOnClickListener(this);
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		mCount.setText(Integer.toString(mMessage.getText().toString().length()));
		return false;
	}

	private void setLocation(final int accountId) {
		final AsyncTask<String, Void, String> asyncTask;
		Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Integer.toString(accountId)}, null);
		if (account.moveToFirst()) {
			switch (account.getInt(account.getColumnIndex(Accounts.SERVICE))) {
			case TWITTER:
				asyncTask = new AsyncTask<String, Void, String>() {
					@Override
					protected String doInBackground(String... arg0) {
						return Sonet.httpResponse(new HttpGet(String.format(TWITTER_SEARCH, TWITTER_BASE_URL, mLat, mLong)));
					}
					@Override
					protected void onPostExecute(String response) {
						if (mLoadingDialog.isShowing()) mLoadingDialog.dismiss();
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
				mLoadingDialog = new ProgressDialog(this);
				mLoadingDialog.setMessage(getString(R.string.loading));
				mLoadingDialog.setCancelable(true);
				mLoadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
					@Override
					public void onCancel(DialogInterface dialog) {
						if (!asyncTask.isCancelled()) asyncTask.cancel(true);
					}
				});
				mLoadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				mLoadingDialog.show();
				asyncTask.execute();
				break;
			case FACEBOOK:
				asyncTask = new AsyncTask<String, Void, String>() {
					@Override
					protected String doInBackground(String... arg0) {
						return Sonet.httpResponse(new HttpGet(String.format(FACEBOOK_SEARCH, FACEBOOK_BASE_URL, mLat, mLong, TOKEN, arg0[0])));
					}
					@Override
					protected void onPostExecute(String response) {
						if (mLoadingDialog.isShowing()) mLoadingDialog.dismiss();
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
				mLoadingDialog = new ProgressDialog(this);
				mLoadingDialog.setMessage(getString(R.string.loading));
				mLoadingDialog.setCancelable(true);
				mLoadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
					@Override
					public void onCancel(DialogInterface dialog) {
						if (!asyncTask.isCancelled()) asyncTask.cancel(true);
					}
				});
				mLoadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				mLoadingDialog.show();
				asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
				break;
			case FOURSQUARE:
				asyncTask = new AsyncTask<String, Void, String>() {
					@Override
					protected String doInBackground(String... arg0) {
						return Sonet.httpResponse(new HttpGet(String.format(FOURSQUARE_SEARCH, FOURSQUARE_BASE_URL, mLat, mLong, arg0[0])));
					}
					@Override
					protected void onPostExecute(String response) {
						if (mLoadingDialog.isShowing()) mLoadingDialog.dismiss();
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
				mLoadingDialog = new ProgressDialog(this);
				mLoadingDialog.setMessage(getString(R.string.loading));
				mLoadingDialog.setCancelable(true);
				mLoadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
					@Override
					public void onCancel(DialogInterface dialog) {
						if (!asyncTask.isCancelled()) asyncTask.cancel(true);
					}
				});
				mLoadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				mLoadingDialog.show();
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
				mLat = Double.toString(location.getLatitude());
				mLong = Double.toString(location.getLongitude());
				if (mAccountsToPost.size() == 1) {
					setLocation(mAccountsToPost.keySet().iterator().next());
				} else {
					// dialog to select an account
					Iterator<Integer> accountIds = mAccountsToPost.keySet().iterator();
					HashMap<Integer, String> accountEntries = new HashMap<Integer, String>();
					while (accountIds.hasNext()) {
						Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Integer.toString(accountIds.next())}, null);
						if (account.moveToFirst()) {
							int service = account.getInt(account.getColumnIndex(Accounts.SERVICE));
							// only get accounts which have been selected and are supported for location
							if ((service == TWITTER) || (service == FACEBOOK) || (service == FOURSQUARE)) {
								accountEntries.put(account.getInt(account.getColumnIndex(Accounts._ID)), account.getString(account.getColumnIndex(Accounts.USERNAME)));
							}
						}
					}
					int size = accountEntries.size();
					if (size != 0) {
						final int[] accountIndexes = new int[size];
						final String[] accounts = new String[size];
						int i = 0;
						Iterator<Map.Entry<Integer, String>> entries = accountEntries.entrySet().iterator();
						while (entries.hasNext()) {
							Map.Entry<Integer, String> entry = entries.next();
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
			}
		} else if (v == mSend) {
			if ((mMessage.getText().toString() != null) && (mMessage.getText().toString().length() > 0)) {
				Iterator<Map.Entry<Integer, String>> entrySet = mAccountsToPost.entrySet().iterator();
				while (entrySet.hasNext()) {
					Map.Entry<Integer, String> entry = entrySet.next();
					final int accountId = entry.getKey();
					final String placeId = entry.getValue();
					// post or comment!
					Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Integer.toString(accountId)}, null);
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
									send = message.substring(0, 140);
									message = message.substring(141);
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
										if (mSid != null) {
											params.add(new BasicNameValuePair("in_reply_to_status_id", mSid));
										}
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
								mMessage.setEnabled(false);
								mSend.setEnabled(false);
								mAccounts.setEnabled(false);
								mLocation.setEnabled(false);
								task.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
							}
							break;
						case FACEBOOK:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									HttpPost httpPost;
									List<NameValuePair> params = new ArrayList<NameValuePair>();
									if (mSid != null) {
										httpPost = new HttpPost(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, mSid, TOKEN, arg0[0]));
									} else if (placeId != null) {
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
							mMessage.setEnabled(false);
							mSend.setEnabled(false);
							mAccounts.setEnabled(false);
							mLocation.setEnabled(false);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
							break;
						case MYSPACE:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, arg0[0], arg0[1]);
									try {
										if ((mEsid != null) && (mSid != null)) {
											HttpPost httpPost = new HttpPost(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, mEsid, mSid));
											httpPost.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOODCOMMENTS_BODY, mMessage.getText().toString())));
											return sonetOAuth.httpResponse(httpPost);
										} else {
											HttpPut httpPut = new HttpPut(String.format(MYSPACE_URL_STATUSMOOD, MYSPACE_BASE_URL));
											httpPut.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOOD_BODY, mMessage.getText().toString())));
											return sonetOAuth.httpResponse(httpPut);
										}
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
							mMessage.setEnabled(false);
							mSend.setEnabled(false);
							mAccounts.setEnabled(false);
							mLocation.setEnabled(false);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
							break;
						case BUZZ:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, arg0[0], arg0[1]);
									try {
										HttpPost httpPost;
										if (mSid != null) {
											httpPost = new HttpPost(String.format(BUZZ_COMMENT, BUZZ_BASE_URL, mSid, BUZZ_API_KEY));
											httpPost.setEntity(new StringEntity(String.format(BUZZ_COMMENT_BODY, mMessage.getText().toString())));
										} else {
											httpPost = new HttpPost(String.format(BUZZ_ACTIVITY, BUZZ_BASE_URL, BUZZ_API_KEY));
											httpPost.setEntity(new StringEntity(String.format(BUZZ_ACTIVITY_BODY, mMessage.getText().toString())));
										}
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
							mMessage.setEnabled(false);
							mSend.setEnabled(false);
							mAccounts.setEnabled(false);
							mLocation.setEnabled(false);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
							break;
						case FOURSQUARE:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									try {
										String message = URLEncoder.encode(mMessage.getText().toString(), "UTF-8");
										HttpPost httpPost;
										if (mSid != null) {
											httpPost = new HttpPost(String.format(FOURSQUARE_ADDCOMMENT, FOURSQUARE_BASE_URL, mSid, message, arg0[0]));
										} else {
											if (placeId != null) {
												httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN, FOURSQUARE_BASE_URL, placeId, message, mLat, mLong, arg0[0]));
											} else {
												httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN, FOURSQUARE_BASE_URL, "", message, mLat, mLong, arg0[0]));
											}
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
							mMessage.setEnabled(false);
							mSend.setEnabled(false);
							mAccounts.setEnabled(false);
							mLocation.setEnabled(false);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
							break;
						case LINKEDIN:
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, arg0[0], arg0[1]);
									try {
										HttpPost httpPost;
										if (mSid != null) {
											httpPost = new HttpPost(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, mSid));
											httpPost.setEntity(new StringEntity(String.format(LINKEDIN_COMMENT_BODY, mMessage.getText().toString())));
										} else {
											httpPost = new HttpPost(String.format(LINKEDIN_POST, LINKEDIN_BASE_URL));
											httpPost.setEntity(new StringEntity(String.format(LINKEDIN_POST_BODY, "", mMessage.getText().toString())));
										}
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
							mMessage.setEnabled(false);
							mSend.setEnabled(false);
							mAccounts.setEnabled(false);
							mLocation.setEnabled(false);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
						}
					}
					account.close();
				}
			} else {
				(Toast.makeText(SonetCreatePost.this, "error parsing message body", Toast.LENGTH_LONG)).show();
				mMessage.setEnabled(true);
				mSend.setEnabled(true);
				if (mSid == null) {
					mAccounts.setEnabled(true);
					mLocation.setEnabled(true);
				}
			}
		} else if (v == mComments) {
			this.startActivity(new Intent(this, SonetComments.class).setData(mData));
		}
		else if (v == mAccounts) {
			Cursor c = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY, Accounts.SERVICE}, Accounts.WIDGET + "=?", new String[]{Integer.toString(mAppWidgetId)}, null);
			if (c.moveToFirst()) {
				int iid = c.getColumnIndex(Accounts._ID),
				iusername = c.getColumnIndex(Accounts.USERNAME),
				iservice = c.getColumnIndex(Accounts.SERVICE),
				i = 0;
				final int[] accountIndexes = new int[c.getCount()];
				final String[] accounts = new String[c.getCount()];
				final boolean[] defaults = new boolean[c.getCount()];
				final int[] accountServices = new int[c.getCount()];
				while (!c.isAfterLast()) {
					int id = c.getInt(iid);
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
							mAccountsToPost.put(accountIndexes[which], null);
						} else {
							mAccountsToPost.remove(accountIndexes[which]);
						}
						if (mAccountsToPost.size() == 0) {
							mAccounts.setText(getString(R.string.accounts));
						} else if (mAccountsToPost.size() == 1) {
							mAccounts.setText(accounts[isChecked ? which : Sonet.arrayIndex(accountIndexes, mAccountsToPost.keySet().iterator().next())]);
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
		} else if ((v == mLike) && (mSid != null)) {
			Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Integer.toString(mAccountsToPost.keySet().iterator().next())}, null);
			if (account.moveToFirst()) {
				AsyncTask<String, Void, String> asyncTask;
				switch (account.getInt(account.getColumnIndex(Accounts.SERVICE))) {
				case TWITTER:
					// retweet
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, arg0[0], arg0[1]);
							HttpPost httpPost = new HttpPost(String.format(TWITTER_RETWEET, TWITTER_BASE_URL, mSid));
							// resolve Error 417 Expectation by Twitter
							httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
							return sonetOAuth.httpResponse(httpPost);
						}

						@Override
						protected void onPostExecute(String response) {
							(Toast.makeText(SonetCreatePost.this, getString(R.string.retweet) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
							mLike.setEnabled(true);
						}
					};
					mLike.setEnabled(false);
					asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
					break;
				case FACEBOOK:
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							if (mLike.getText().equals(getString(R.string.like))) {
								return Sonet.httpResponse(new HttpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, mSid, TOKEN, arg0[0])));
							} else {
								HttpDelete httpDelete = new HttpDelete(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, mSid, TOKEN, arg0[0]));
								httpDelete.setHeader("Content-Length", "0");
								return Sonet.httpResponse(httpDelete);
							}
						}

						@Override
						protected void onPostExecute(String response) {
							if (response != null) {
								(Toast.makeText(SonetCreatePost.this, getString(R.string.facebook) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
								mLike.setText(mLike.getText().equals(getString(R.string.like)) ? R.string.unlike : R.string.like);
							} else {
								(Toast.makeText(SonetCreatePost.this, getString(R.string.facebook) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
							}
							mLike.setEnabled(true);
						}
					};
					mLike.setEnabled(false);
					asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
					break;
				case BUZZ:
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							SonetOAuth sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, arg0[0], arg0[1]);
							return sonetOAuth.httpResponse(mLike.getText().equals(getString(R.string.like)) ? new HttpPut(String.format(BUZZ_LIKE, BUZZ_BASE_URL, mSid, BUZZ_API_KEY)) :  new HttpDelete(String.format(BUZZ_LIKE, BUZZ_BASE_URL, mSid, BUZZ_API_KEY)));
						}

						@Override
						protected void onPostExecute(String response) {
							if (response != null) {
								(Toast.makeText(SonetCreatePost.this, getString(R.string.buzz) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
								mLike.setText(mLike.getText().equals(getString(R.string.like)) ? R.string.unlike : R.string.like);
							} else {
								(Toast.makeText(SonetCreatePost.this, getString(R.string.buzz) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
							}
							mLike.setEnabled(true);
						}
					};
					mLike.setEnabled(false);
					asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
					break;
				case LINKEDIN:
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							SonetOAuth sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, arg0[0], arg0[1]);
							HttpPut httpPut = new HttpPut(String.format(LINKEDIN_IS_LIKED, LINKEDIN_BASE_URL, mSid));
							httpPut.addHeader(new BasicHeader("Content-Type", "application/xml"));
							try {
								httpPut.setEntity(new StringEntity(String.format(LINKEDIN_LIKE_BODY, Boolean.toString(mLike.getText() == getString(R.string.like)))));
								return sonetOAuth.httpResponse(httpPut);
							} catch (UnsupportedEncodingException e) {
								Log.e(TAG, e.toString());
							}
							return null;
						}

						@Override
						protected void onPostExecute(String response) {
							if (response != null) {
								(Toast.makeText(SonetCreatePost.this, getString(R.string.linkedin) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
								mLike.setText(mLike.getText().equals(getString(R.string.like)) ? R.string.unlike : R.string.like);
							} else {
								(Toast.makeText(SonetCreatePost.this, getString(R.string.buzz) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
							}
							mLike.setEnabled(true);
						}
					};
					mLike.setEnabled(false);
					asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
					break;
				}
			}
			account.close();
		}
	}

}