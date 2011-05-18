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
import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Widgets;

import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.BUZZ_ACTIVITY;
import static com.piusvelte.sonet.Sonet.BUZZ_BASE_URL;
import static com.piusvelte.sonet.Sonet.BUZZ_COMMENT;
import static com.piusvelte.sonet.Sonet.BUZZ_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.BUZZ_LIKE;
import static com.piusvelte.sonet.Sonet.BUZZ_GET_LIKE;
import static com.piusvelte.sonet.Sonet.BUZZ_ACTIVITY_BODY;
import static com.piusvelte.sonet.Sonet.BUZZ_COMMENT_BODY;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.Sonet.FACEBOOK_POST;
import static com.piusvelte.sonet.Sonet.FACEBOOK_COMMENTS;
import static com.piusvelte.sonet.Sonet.FACEBOOK_CHECKIN;
import static com.piusvelte.sonet.Sonet.FACEBOOK_COORDINATES;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_BASE_URL;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_CHECKIN;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_ADDCOMMENT;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_GET_CHECKIN;
import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Sonet.LINKEDIN_HEADERS;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.MYSPACE_DATE_FORMAT;
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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SonetComments extends ListActivity implements OnKeyListener, OnClickListener, TextWatcher, DialogInterface.OnClickListener {
	private static final String TAG = "SonetComments";
	private int mService;
	private int mAccount;
	private HashMap<Integer, String> mAccountsToPost = new HashMap<Integer, String>();
	private String mSid = null;
	private String mEsid;
	private EditText mMessage;
	private Button mSend;
	private Button mLike;
	private TextView mCount;
	private String mLat = null,
	mLong = null;
	private List<HashMap<String, String>> mComments = new ArrayList<HashMap<String, String>>();
	private boolean mTime24hr = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// allow posting to multiple services if an account is defined
		// allow selecting which accounts to use
		// get existing comments, allow liking|unliking those comments
		setContentView(R.layout.comments);
		AdView adView = new AdView(this, AdSize.BANNER, Sonet.GOOGLE_AD_ID);
		((LinearLayout) findViewById(R.id.ad)).addView(adView);
		adView.loadAd(new AdRequest());

		mMessage = (EditText) findViewById(R.id.message);
		mSend = (Button) findViewById(R.id.send);
		mLike = (Button) findViewById(R.id.like);
		mCount = (TextView) findViewById(R.id.count);
		Intent intent = getIntent();
		if (intent != null) {
			Cursor status = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.ACCOUNT, Statuses_styles.SID, Statuses_styles.ESID, Statuses_styles.WIDGET, Statuses_styles.SERVICE}, Statuses_styles._ID + "=?", new String[]{intent.getData().getLastPathSegment()}, null);
			if (status.moveToFirst()) {
				mService = status.getInt(status.getColumnIndex(Statuses_styles.SERVICE));
				mAccount = status.getInt(status.getColumnIndex(Statuses_styles.ACCOUNT));
				mSid = Sonet.removeUnderscore(status.getString(status.getColumnIndex(Statuses_styles.SID)));
				mEsid = Sonet.removeUnderscore(status.getString(status.getColumnIndex(Statuses_styles.ESID)));
				Cursor widget = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(status.getInt(status.getColumnIndex(Statuses_styles.WIDGET))), Long.toString(mAccount)}, null);
				mTime24hr = widget.moveToFirst() ? widget.getInt(widget.getColumnIndex(Widgets.TIME24HR)) == 1 : false;
				widget.close();
				mAccountsToPost.put(mAccount, null);
			}
			status.close();
		}
		mMessage.addTextChangedListener(this);
		mMessage.setOnKeyListener(this);
		mSend.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// allow comment viewing for services that having commenting
		// loading liking/retweeting
		Cursor account;
		AsyncTask<String, Void, String> asyncTask;
		switch (mService) {
		case TWITTER:
			// reply, load the user's name @username
			account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
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
			account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
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
						mLike.setOnClickListener(SonetComments.this);
					}
				};
				mLike.setText(R.string.loading);
				asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
			}
			account.close();
			break;
		case BUZZ:
			account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
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
						mLike.setOnClickListener(SonetComments.this);
					}
				};
				mLike.setText(R.string.loading);
				asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
			}
			account.close();
			break;
		case LINKEDIN:
			account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
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
								if (data.has("isCommentable") && !data.getBoolean("isCommentable")) {
									mSend.setEnabled(false);
									mMessage.setEnabled(false);
									mMessage.setText(R.string.uncommentable);
								} else {
									mSend.setEnabled(false);
									mMessage.setEnabled(false);
									mMessage.setText(R.string.uncommentable);
								}
								if (data.has("isLikable")) {
									liked = data.has("isLiked") && data.getBoolean("isLiked");
									mLike.setEnabled(true);
									mLike.setOnClickListener(SonetComments.this);
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
		}
		loadComments();
	}

	@Override
	public void onClick(View v) {
		if (v == mSend) {
			if ((mMessage.getText().toString() != null) && (mMessage.getText().toString().length() > 0) && (mAccountsToPost.size() > 0)) {
				mMessage.setEnabled(false);
				mSend.setEnabled(false);
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
										if (mSid != null) {
											params.add(new BasicNameValuePair("in_reply_to_status_id", mSid));
										} else if (placeId != null) {
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
										(Toast.makeText(SonetComments.this, getString(R.string.twitter) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
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
									(Toast.makeText(SonetComments.this, getString(R.string.facebook) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
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
										(Toast.makeText(SonetComments.this, getString(R.string.myspace) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
									} else {
										(Toast.makeText(SonetComments.this, getString(R.string.myspace) + " " + getString(R.string.failure) + " " + getString(R.string.myspace_permissions_message), Toast.LENGTH_LONG)).show();
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
									(Toast.makeText(SonetComments.this, getString(R.string.buzz) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
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
									(Toast.makeText(SonetComments.this, getString(R.string.foursquare) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
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
									(Toast.makeText(SonetComments.this, getString(R.string.linkedin) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
									finish();
								}
							};
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
						}
					}
					account.close();
				}
			} else {
				(Toast.makeText(SonetComments.this, "error parsing message body", Toast.LENGTH_LONG)).show();
				mMessage.setEnabled(true);
				mSend.setEnabled(true);
			}
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
							(Toast.makeText(SonetComments.this, getString(R.string.retweet) + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
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
								(Toast.makeText(SonetComments.this, getString(R.string.facebook) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
								mLike.setText(mLike.getText().equals(getString(R.string.like)) ? R.string.unlike : R.string.like);
							} else {
								(Toast.makeText(SonetComments.this, getString(R.string.facebook) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
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
								(Toast.makeText(SonetComments.this, getString(R.string.buzz) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
								mLike.setText(mLike.getText().equals(getString(R.string.like)) ? R.string.unlike : R.string.like);
							} else {
								(Toast.makeText(SonetComments.this, getString(R.string.buzz) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
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
								(Toast.makeText(SonetComments.this, getString(R.string.linkedin) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
								mLike.setText(mLike.getText().equals(getString(R.string.like)) ? R.string.unlike : R.string.like);
							} else {
								(Toast.makeText(SonetComments.this, getString(R.string.buzz) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST, 0, getString(R.string.button_refresh)).setIcon(android.R.drawable.ic_menu_rotate);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST:
			loadComments();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		AlertDialog.Builder dialog;
		switch (mService) {
		case FACEBOOK:
			final String sid = mComments.get(position).get(Statuses.SID);
			final String liked = mComments.get(position).get(getString(R.string.like));
			dialog = new AlertDialog.Builder(this);
			dialog.setMessage(mComments.get(position).get(getString(R.string.like)) == getString(R.string.like) ? R.string.like : R.string.unlike)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Cursor c = SonetComments.this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (c.moveToFirst()) {
						AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
							@Override
							protected String doInBackground(String... arg0) {
								if (arg0[1].equals(getString(R.string.like))) {
									return Sonet.httpResponse(new HttpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, sid, TOKEN, arg0[0])));
								} else {
									HttpDelete httpDelete = new HttpDelete(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, sid, TOKEN, arg0[0]));
									httpDelete.setHeader("Content-Length", "0");
									return Sonet.httpResponse(httpDelete);
								}
							}

							@Override
							protected void onPostExecute(String response) {
								if (response != null) {
									for (int i = 0; i < mComments.size(); i++) {
										HashMap<String, String> comment = mComments.get(i);
										if (comment.get(Accounts.SID).equals(sid)) {
											comment.put(getString(R.string.like), getString(comment.get(getString(R.string.like)).equals(getString(R.string.like)) ? R.string.unlike : R.string.like));
											mComments.set(i, comment);
											setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
											break;
										}
									}
									(Toast.makeText(SonetComments.this, getString(R.string.facebook) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
								} else {
									(Toast.makeText(SonetComments.this, getString(R.string.facebook) + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
								}
							}
						};
						asyncTask.execute(c.getString(c.getColumnIndex(Accounts.TOKEN)), liked);
					}
					c.close();
				}
			})
			.setNegativeButton(android.R.string.cancel, this)
			.show();
			break;
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		dialog.cancel();
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

	private void loadComments() {
		mComments.clear();
		HashMap<String, String> commentMap = new HashMap<String, String>();
		commentMap.put(Statuses.SID, "");
		commentMap.put(Entities.FRIEND, "");
		commentMap.put(Statuses.MESSAGE, getString(R.string.loading));
		commentMap.put(Statuses.CREATEDTEXT, "");
		commentMap.put(getString(R.string.like), "");
		mComments.add(commentMap);
		setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
		final ProgressDialog loadingDialog = new ProgressDialog(this);
		final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... arg0) {
				String response = null;
				Cursor account = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
				if (account.moveToFirst()) {
					SonetOAuth sonetOAuth;
					switch (mService) {
					case FACEBOOK:
						response = Sonet.httpResponse(new HttpGet(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, mSid, TOKEN, account.getString(account.getColumnIndex(Accounts.TOKEN)))));
						break;
					case MYSPACE:
						sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
						response = sonetOAuth.httpResponse(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, mEsid, mSid)));
						break;
					case BUZZ:
						sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
						response = sonetOAuth.httpResponse(new HttpGet(String.format(BUZZ_COMMENT, BUZZ_BASE_URL, mSid, BUZZ_API_KEY)));
						break;
					case LINKEDIN:
						sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
						HttpGet httpGet = new HttpGet(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, mSid));
						for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
						response = sonetOAuth.httpResponse(httpGet);
						break;
					case FOURSQUARE:
						response = Sonet.httpResponse(new HttpGet(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, mSid, account.getString(account.getColumnIndex(Accounts.TOKEN)))));
						break;
					}
				}
				account.close();
				return response;
			}

			@Override
			protected void onPostExecute(String response) {
				mComments.clear();
				if (response != null) {
					try {
						JSONArray comments;
						switch (mService) {
						case FACEBOOK:
							comments = new JSONObject(response).getJSONArray("data");
							if (comments.length() > 0) {
								for (int i = 0; i < comments.length(); i++) {
									JSONObject comment = comments.getJSONObject(i);
									HashMap<String, String> commentMap = new HashMap<String, String>();
									commentMap.put(Statuses.SID, comment.getString("id"));
									commentMap.put(Entities.FRIEND, comment.getJSONObject("from").getString("name"));
									commentMap.put(Statuses.MESSAGE, comment.getString("message"));
									commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(comment.getLong("created_time") * 1000, mTime24hr));
									commentMap.put(getString(R.string.like), getString(comment.has("user_likes") && comment.getBoolean("user_likes") ? R.string.unlike : R.string.like));
									mComments.add(commentMap);
								}
							} else {
								HashMap<String, String> commentMap = new HashMap<String, String>();
								commentMap.put(Statuses.SID, "");
								commentMap.put(Entities.FRIEND, "");
								commentMap.put(Statuses.MESSAGE, getString(R.string.no_comments));
								commentMap.put(Statuses.CREATEDTEXT, "");
								commentMap.put(getString(R.string.like), "");
								mComments.add(commentMap);
							}
							break;
						case MYSPACE:
							comments = new JSONObject(response).getJSONArray("entry");
							if (comments.length() > 0) {
								for (int i = 0; i < comments.length(); i++) {
									JSONObject entry = comments.getJSONObject(i);
									HashMap<String, String> commentMap = new HashMap<String, String>();
									commentMap.put(Statuses.SID, entry.getString("commentId"));
									commentMap.put(Entities.FRIEND, entry.getJSONObject("author").getString("displayName"));
									commentMap.put(Statuses.MESSAGE, entry.getString("body"));
									commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(Sonet.parseDate(entry.getString("postedDate"), MYSPACE_DATE_FORMAT), mTime24hr));
									commentMap.put(getString(R.string.like), "");
									mComments.add(commentMap);
								}
							} else {
								HashMap<String, String> commentMap = new HashMap<String, String>();
								commentMap.put(Statuses.SID, "");
								commentMap.put(Entities.FRIEND, "");
								commentMap.put(Statuses.MESSAGE, getString(R.string.no_comments));
								commentMap.put(Statuses.CREATEDTEXT, "");
								commentMap.put(getString(R.string.like), "");
								mComments.add(commentMap);
							}
							break;
						case BUZZ:
							JSONObject data = new JSONObject(response).getJSONObject("data");
							if (data.has("items")) {
								comments = data.getJSONArray("items");
								if (comments.length() > 0) {
									for (int i = 0; i < comments.length(); i++) {
										JSONObject comment = comments.getJSONObject(i);
										String id = comment.getString("id");
										HashMap<String, String> commentMap = new HashMap<String, String>();
										commentMap.put(Statuses.SID, id);
										commentMap.put(Entities.FRIEND, comment.getJSONObject("actor").getString("name"));
										commentMap.put(Statuses.MESSAGE, comment.getString("content"));
										commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(Sonet.parseDate(comment.getString("published"), BUZZ_DATE_FORMAT), mTime24hr));
										commentMap.put(getString(R.string.like), "");
										mComments.add(commentMap);
									}
								} else {
									HashMap<String, String> commentMap = new HashMap<String, String>();
									commentMap.put(Statuses.SID, "");
									commentMap.put(Entities.FRIEND, "");
									commentMap.put(Statuses.MESSAGE, getString(R.string.no_comments));
									commentMap.put(Statuses.CREATEDTEXT, "");
									commentMap.put(getString(R.string.like), "");
									mComments.add(commentMap);
								}
							}
							break;
						case LINKEDIN:
							JSONObject jsonResponse = new JSONObject(response);
							if (jsonResponse.has("_total") && (jsonResponse.getInt("_total") != 0)) {
								comments = jsonResponse.getJSONArray("values");
								if (comments.length() > 0) {
									for (int i = 0; i < comments.length(); i++) {
										JSONObject comment = comments.getJSONObject(i);
										JSONObject person = comment.getJSONObject("person");
										HashMap<String, String> commentMap = new HashMap<String, String>();
										commentMap.put(Statuses.SID, comment.getString("id"));
										commentMap.put(Entities.FRIEND, person.getString("firstName") + " " + person.getString("lastName"));
										commentMap.put(Statuses.MESSAGE, comment.getString("comment"));
										commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(comment.getLong("timestamp"), mTime24hr));
										commentMap.put(getString(R.string.like), "");
										mComments.add(commentMap);
									}
								} else {
									HashMap<String, String> commentMap = new HashMap<String, String>();
									commentMap.put(Statuses.SID, "");
									commentMap.put(Entities.FRIEND, "");
									commentMap.put(Statuses.MESSAGE, getString(R.string.no_comments));
									commentMap.put(Statuses.CREATEDTEXT, "");
									commentMap.put(getString(R.string.like), "");
									mComments.add(commentMap);
								}
							}
							break;
						case FOURSQUARE:
							comments = new JSONObject(response).getJSONObject("response").getJSONObject("checkin").getJSONObject("comments").getJSONArray("items");
							if (comments.length() > 0) {
								for (int i = 0; i < comments.length(); i++) {
									JSONObject comment = comments.getJSONObject(i);
									JSONObject user = comment.getJSONObject("user");
									HashMap<String, String> commentMap = new HashMap<String, String>();
									commentMap.put(Statuses.SID, comment.getString("id"));
									commentMap.put(Entities.FRIEND, user.getString("firstName") + " " + user.getString("lastName"));
									commentMap.put(Statuses.MESSAGE, comment.getString("text"));
									commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(comment.getLong("createdAt") * 1000, mTime24hr));
									commentMap.put(getString(R.string.like), "");
									mComments.add(commentMap);
								}
							} else {
								HashMap<String, String> commentMap = new HashMap<String, String>();
								commentMap.put(Statuses.SID, "");
								commentMap.put(Entities.FRIEND, "");
								commentMap.put(Statuses.MESSAGE, getString(R.string.no_comments));
								commentMap.put(Statuses.CREATEDTEXT, "");
								commentMap.put(getString(R.string.like), "");
								mComments.add(commentMap);
							}
							break;
						}
					} catch (JSONException e) {
						Log.e(TAG, e.toString());
					}
				} else {
					HashMap<String, String> commentMap = new HashMap<String, String>();
					commentMap.put(Statuses.SID, "");
					commentMap.put(Entities.FRIEND, "");
					commentMap.put(Statuses.MESSAGE, getString(R.string.no_comments));
					commentMap.put(Statuses.CREATEDTEXT, "");
					commentMap.put(getString(R.string.like), "");
					mComments.add(commentMap);
				}
				setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
				loadingDialog.dismiss();
			}
		};
		loadingDialog.setMessage(getString(R.string.loading));
		loadingDialog.setCancelable(true);
		loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				asyncTask.cancel(true);
				dialog.dismiss();
			}
		});
		loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), this);
		loadingDialog.show();
		asyncTask.execute();
	}

}