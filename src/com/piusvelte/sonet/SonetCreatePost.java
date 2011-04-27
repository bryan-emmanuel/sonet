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
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.Sonet.Accounts;

import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.BUZZ_ACTIVITY;
import static com.piusvelte.sonet.Sonet.BUZZ_BASE_URL;
import static com.piusvelte.sonet.Sonet.BUZZ_COMMENT;
import static com.piusvelte.sonet.Sonet.BUZZ_LIKE;
import static com.piusvelte.sonet.Sonet.BUZZ_GET_LIKE;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.Sonet.FACEBOOK_POST;
import static com.piusvelte.sonet.Sonet.FACEBOOK_COMMENTS;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOOD;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOODCOMMENTS;
import static com.piusvelte.sonet.Sonet.SALESFORCE;
import static com.piusvelte.sonet.Sonet.TOKEN;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.Sonet.TWITTER_RETWEET;
import static com.piusvelte.sonet.Sonet.TWITTER_USER;
import static com.piusvelte.sonet.Sonet.TWITTER_UPDATE;
import static com.piusvelte.sonet.SonetTokens.BUZZ_API_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_SECRET;
import static com.piusvelte.sonet.SonetTokens.TWITTER_KEY;
import static com.piusvelte.sonet.SonetTokens.TWITTER_SECRET;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_KEY;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_SECRET;

import com.piusvelte.sonet.Sonet.Statuses_styles;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.TextView;
import android.widget.Toast;

public class SonetCreatePost extends Activity implements OnKeyListener, OnClickListener {
	private static final String TAG = "SonetCreatePost";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private int mService = 0;
	private int mAccount = (int) Sonet.INVALID_ACCOUNT_ID;
	private HashMap<Integer, HashMap<String, Integer>> mAccountsToPost = new HashMap<Integer, HashMap<String, Integer>>();
	private String mSid;
	private String mEsid;
	private Uri mData;
	private EditText mPost;
	private Button mSend;
	private Button mLocation;
	private Button mComments;
	private Button mAccounts;
	private Button mLike;
	private TextView mCount;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// allow posting to multiple services if an account is defined
		// allow selecting which accounts to use
		// get existing comments, allow liking|unliking those comments
		setContentView(R.layout.post);

		mPost = (EditText) findViewById(R.id.post);
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
				Cursor status = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.SERVICE, Statuses_styles.ACCOUNT, Statuses_styles.SID, Statuses_styles.ESID, Statuses_styles.WIDGET}, Statuses_styles._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
				if (status.moveToFirst()) {
					mService = status.getInt(status.getColumnIndex(Statuses_styles.SERVICE));
					mAccount = status.getInt(status.getColumnIndex(Statuses_styles.ACCOUNT));
					mSid = status.getString(status.getColumnIndex(Statuses_styles.SID));
					mEsid = status.getString(status.getColumnIndex(Statuses_styles.ESID));
					mAppWidgetId = status.getInt(status.getColumnIndex(Statuses_styles.WIDGET));
				}
				status.close();
				// allow comment viewing for services that having commenting
				// loading liking/retweeting
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
											mPost.append("@" + users.getJSONObject(0).getString("screen_name") + " ");
										}
									} catch (JSONException e) {
										Log.e(TAG,e.toString());
									}
								}
								mPost.setEnabled(true);
							}
						};
						mPost.setEnabled(false);
						mPost.setText(R.string.loading);
						asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
					}
					account.close();
					mLike.setText(R.string.retweet);
					mLike.setEnabled(true);
					break;
				case FACEBOOK:
					mComments.setEnabled(true);
					mComments.setOnClickListener(this);
					account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (account.moveToFirst()) {
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
											if (like.getString("id") == mSid) {
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
					//TODO: check if liked
					account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (account.moveToFirst()) {
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
									Log.v(TAG,"response:"+response);
									try {
										JSONObject data = new JSONObject(response).getJSONObject("data");
									} catch (JSONException e) {
										Log.e(TAG,e.toString());
									}
								}
								mLike.setText(liked ? R.string.unlike : R.string.like);
								mLike.setEnabled(true);
							}
						};
						mLike.setText(R.string.loading);
						asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
					}
					account.close();
					break;
				case LINKEDIN:
					mComments.setEnabled(true);
					mComments.setOnClickListener(this);
					//TODO: check if liked
					mLike.setEnabled(true);
					break;
				default:
					mComments.setEnabled(true);
					mComments.setOnClickListener(this);
				}
			} else {
				// default to the account passed in, but allow selecting additional accounts
				account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SERVICE, Accounts.WIDGET}, Accounts._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
				if (account.moveToFirst()) {
					mAccount = account.getInt(account.getColumnIndex(Accounts._ID));
					mService = account.getInt(account.getColumnIndex(Accounts.SERVICE));
					mAppWidgetId = account.getInt(account.getColumnIndex(Accounts.WIDGET));
					if (!mAccountsToPost.containsKey(mAccount)) {
						HashMap<String, Integer> values = new HashMap<String, Integer>();
						values.put(Accounts.SERVICE, mService);
						values.put(Accounts._ID, 1);
						mAccountsToPost.put(mAccount, values);
					}
				}
				account.close();
				mAccounts.setEnabled(true);
				mAccounts.setOnClickListener(this);
				mLocation.setEnabled(true);
				mLocation.setOnClickListener(this);
			}
		}
		mPost.setOnKeyListener(this);
		mSend.setOnClickListener(this);
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// track the post length, if TWITTER and >140, truncate
		String text = mPost.getText().toString();
		int count = text.length();
		mCount.setText(Integer.toString(count));
		mSend.setEnabled(count > 0);
		if ((mService == TWITTER) && (count > 140)) {
			mPost.setText(text.substring(0, 140));
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		if (v == mLocation) {
			// set the location
		} else if (v == mSend) {
			final String message = mPost.getText().toString();
			if ((message != null) && (message != "")) {
				//TODO: loop through AccountsToPost
				Iterator<Integer> i = mAccountsToPost.keySet().iterator();
				while (i.hasNext()) {
					HashMap<String, Integer> values = mAccountsToPost.get(i);
					if (values.get(Accounts._ID) == 1) {
						// post or comment!
						AsyncTask<String, Void, String> asyncTask;
						Cursor account;
						switch (values.get(Accounts.SERVICE)) {
						case TWITTER:
							account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
							if (account.moveToFirst()) {
								asyncTask = new AsyncTask<String, Void, String>() {
									@Override
									protected String doInBackground(String... arg0) {
										SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, arg0[0], arg0[1]);
										HttpPost httpPost = new HttpPost(String.format(TWITTER_UPDATE, TWITTER_BASE_URL));
										HttpParams httpParams = new BasicHttpParams();
										httpParams.setParameter("status", message);
										if (arg0[2] != null) {
											httpParams.setParameter("in_reply_to_status_id", mSid);
										}
										httpPost.setParams(httpParams);
										return sonetOAuth.httpResponse(httpPost);
									}

									@Override
									protected void onPostExecute(String response) {
										Log.v(TAG,"tweet:"+response);
										//TODO: handle response to user
										(Toast.makeText(SonetCreatePost.this, getString(R.string.twitter) + " " + getString(R.string.sent), Toast.LENGTH_LONG)).show();
										finish();
									}
								};
								mPost.setEnabled(false);
								mSend.setEnabled(false);
								mSend.setText(R.string.sending);
								asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
							}
							account.close();
							break;
						case FACEBOOK:
							account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
							if (account.moveToFirst()) {
								asyncTask = new AsyncTask<String, Void, String>() {
									@Override
									protected String doInBackground(String... arg0) {
										HttpPost httpPost = new HttpPost(String.format(FACEBOOK_POST, FACEBOOK_BASE_URL));
										HttpParams httpParams = new BasicHttpParams();
										httpParams.setParameter("message", message);
										if (arg0[1] != null) {
											httpPost = new HttpPost(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, TOKEN, arg0[0], mSid));
										} else {
											httpPost = new HttpPost(String.format(FACEBOOK_POST, FACEBOOK_BASE_URL, TOKEN, arg0[0]));
										}
										return Sonet.httpResponse(httpPost);
									}

									@Override
									protected void onPostExecute(String response) {
										Log.v(TAG,"facebook post:"+response);
										//TODO: handle response to user
										(Toast.makeText(SonetCreatePost.this, getString(R.string.facebook) + " " + getString(R.string.sent), Toast.LENGTH_LONG)).show();
										finish();
									}
								};
								mLike.setText(R.string.loading);
								asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
							}
							account.close();
							break;
						case MYSPACE:
							account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
							if (account.moveToFirst()) {
								asyncTask = new AsyncTask<String, Void, String>() {
									@Override
									protected String doInBackground(String... arg0) {
										SonetOAuth sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, arg0[0], arg0[1]);
										try {
											if ((arg0[2] != null) && (arg0[3] != null)) {
												HttpPost httpPost = new HttpPost(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, mEsid, mSid));
												httpPost.setEntity(new StringEntity("{\"body\":\"" + message + "\"}"));
												return sonetOAuth.httpResponse(httpPost);
											} else {
												HttpPut httpPut = new HttpPut(String.format(MYSPACE_URL_STATUSMOOD, MYSPACE_BASE_URL));
												httpPut.setEntity(new StringEntity("{\"status\":\"" + message + "\"}"));
												return sonetOAuth.httpResponse(httpPut);
											}
										} catch (IOException e) {
											Log.e(TAG, e.toString());
										}
										return null;
									}

									@Override
									protected void onPostExecute(String response) {
										Log.v(TAG,"myspace:"+response);
										//TODO: handle response to user
										(Toast.makeText(SonetCreatePost.this, getString(R.string.myspace) + " " + getString(R.string.sent), Toast.LENGTH_LONG)).show();
										finish();
									}
								};
								mPost.setEnabled(false);
								mSend.setEnabled(false);
								mSend.setText(R.string.sending);
								asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
							}
							account.close();
							break;
						case BUZZ:
							account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
							if (account.moveToFirst()) {
								asyncTask = new AsyncTask<String, Void, String>() {
									@Override
									protected String doInBackground(String... arg0) {
										SonetOAuth sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, arg0[0], arg0[1]);
										try {
											HttpPost httpPost;
											if ((arg0[2] != null) && (arg0[3] != null)) {
												httpPost = new HttpPost(String.format(BUZZ_COMMENT, BUZZ_BASE_URL, mSid, BUZZ_API_KEY));
												httpPost.setEntity(new StringEntity("{\"data\":{\"object\":{\"type\":\"note\",\"content\":\"" + message + "\"}}}"));
											} else {
												httpPost = new HttpPost(String.format(BUZZ_ACTIVITY, BUZZ_BASE_URL, "", BUZZ_API_KEY));
												httpPost.setEntity(new StringEntity("{\"data\":{\"content\":\"" + message + "\"}}"));
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
										Log.v(TAG,"buzz:"+response);
										//TODO: handle response to user
										(Toast.makeText(SonetCreatePost.this, getString(R.string.buzz) + " " + getString(R.string.sent), Toast.LENGTH_LONG)).show();
										finish();
									}
								};
								mPost.setEnabled(false);
								mSend.setEnabled(false);
								mSend.setText(R.string.sending);
								asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
							}
							account.close();
							break;
						case FOURSQUARE:
							//TODO: send
							break;
						case LINKEDIN:
							//TODO: send
						}
					}
				}
			}
		} else if (v == mComments) {
			this.startActivity(new Intent(this, SonetComments.class).setData(mData));
		}
		else if (v == mAccounts) {
			Cursor c = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID,
					"(case when " + Accounts.SERVICE + "='" + TWITTER + "' then 'Twitter: ' when "
					+ Accounts.SERVICE + "='" + FACEBOOK + "' then 'Facebook: ' when "
					+ Accounts.SERVICE + "='" + MYSPACE + "' then 'MySpace: ' when "
					+ Accounts.SERVICE + "='" + BUZZ + "' then 'Buzz: ' when "
					+ Accounts.SERVICE + "='" + LINKEDIN + "' then 'LinkedIn: ' when "
					+ Accounts.SERVICE + "='" + SALESFORCE + "' then 'Salesforce: ' when "
					+ Accounts.SERVICE + "='" + FOURSQUARE + "' then 'Foursquare: ' else '' end)||" + Accounts.USERNAME + " as " + Accounts.USERNAME, Accounts.SERVICE}, Accounts.WIDGET + "=?", new String[]{Integer.toString(mAppWidgetId)}, null);
			if (c.moveToFirst()) {
				int iid = c.getColumnIndex(Accounts._ID),
				iusername = c.getColumnIndex(Accounts.USERNAME),
				iservice = c.getColumnIndex(Accounts.SERVICE),
				i = 0;
				final int[] accountIndexes = new int[c.getCount()];
				final String[] accounts = new String[c.getCount()];
				final boolean[] defaults = new boolean[c.getCount()];
				while (!c.isAfterLast()) {
					int id = c.getInt(iid);
					HashMap<String, Integer> values = new HashMap<String, Integer>();
					values.put(Accounts.SERVICE, c.getInt(iservice));
					values.put(Accounts._ID, id == mAccount ? 1 : 0);
					mAccountsToPost.put(id, values);
					accountIndexes[i] = id;
					accounts[i] = c.getString(iusername);
					defaults[i++] = id == mAccount;
					c.moveToNext();
				}
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle(R.string.accounts)
				.setMultiChoiceItems(accounts, defaults, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						HashMap<String, Integer> values = mAccountsToPost.get(accountIndexes[which]);
						values.put(Accounts._ID, isChecked ? 1 : 0);
						mAccountsToPost.put(accountIndexes[which], values);
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
		} else if (v == mLike) {
			Cursor account;
			AsyncTask<String, Void, String> asyncTask;
			switch (mService) {
			case TWITTER:
				// retweet
				if (mSid != null) {
					account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (account.moveToFirst()) {
						asyncTask = new AsyncTask<String, Void, String>() {
							@Override
							protected String doInBackground(String... arg0) {
								SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, arg0[0], arg0[1]);
								return sonetOAuth.httpResponse(new HttpGet(String.format(TWITTER_RETWEET, TWITTER_BASE_URL, mSid)));
							}

							@Override
							protected void onPostExecute(String response) {
								mLike.setText(R.string.retweet);
								Log.v(TAG,"retweet:"+response);
								//TODO: handle response to user
								(Toast.makeText(SonetCreatePost.this, getString(R.string.retweet) + " " + getString(R.string.sent), Toast.LENGTH_LONG)).show();
							}
						};
						mLike.setEnabled(false);
						mLike.setText(R.string.loading);
						asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
					}
					account.close();
				}
				break;
			case FACEBOOK:
				if (mSid != null) {
					Cursor c = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (c.moveToFirst()) {
						account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
						if (account.moveToFirst()) {
							asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									return Sonet.httpResponse(mLike.getText() == getString(R.string.like) ? new HttpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, mSid, TOKEN, arg0[0])) : new HttpDelete(String.format(FACEBOOK_LIKES, mSid, TOKEN, arg0[0])));
								}

								@Override
								protected void onPostExecute(String response) {
									//TODO: check response
									Log.v(TAG,"like:"+response);
									//TODO: handle response to user
									(Toast.makeText(SonetCreatePost.this, getString(R.string.facebook) + " " + getString(R.string.sent), Toast.LENGTH_LONG)).show();
									mLike.setText(R.string.unlike);
									mLike.setEnabled(true);
								}
							};
							mLike.setEnabled(false);
							mLike.setText(R.string.loading);
							asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
						}
						account.close();
					}
					c.close();
				}
				break;
			case BUZZ:
				//TODO:like
				if (mSid != null) {
					account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (account.moveToFirst()) {
						asyncTask = new AsyncTask<String, Void, String>() {
							@Override
							protected String doInBackground(String... arg0) {
								SonetOAuth sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, arg0[0], arg0[1]);
								return sonetOAuth.httpResponse(mLike.getText() == getString(R.string.like) ? new HttpPut(String.format(BUZZ_LIKE, BUZZ_BASE_URL, mSid, BUZZ_API_KEY)) :  new HttpDelete(String.format(BUZZ_LIKE, BUZZ_BASE_URL, mSid, BUZZ_API_KEY)));
							}

							@Override
							protected void onPostExecute(String response) {
								Log.v(TAG,"buzz like:"+response);
								//TODO: handle response to user
								(Toast.makeText(SonetCreatePost.this, getString(R.string.buzz) + " " + getString(R.string.sent), Toast.LENGTH_LONG)).show();
								mLike.setText(R.string.unlike);
								mLike.setEnabled(true);
							}
						};
						mLike.setEnabled(false);
						mLike.setText(R.string.loading);
						asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
					}
					account.close();
				}
				break;
			case LINKEDIN:
				//TODO:like
			}
		}
	}

}