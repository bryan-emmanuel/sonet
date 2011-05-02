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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;

import static com.piusvelte.sonet.Sonet.BUZZ_BASE_URL;
import static com.piusvelte.sonet.Sonet.BUZZ_COMMENT;
import static com.piusvelte.sonet.Sonet.BUZZ_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.BUZZ_GET_LIKE;
import static com.piusvelte.sonet.Sonet.BUZZ_LIKE;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.Sonet.FACEBOOK_COMMENTS;
import static com.piusvelte.sonet.Sonet.LINKEDIN_BASE_URL;
import static com.piusvelte.sonet.Sonet.LINKEDIN_HEADERS;
import static com.piusvelte.sonet.Sonet.LINKEDIN_UPDATE_COMMENTS;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOOD;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOODCOMMENTS;
import static com.piusvelte.sonet.SonetTokens.BUZZ_API_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_SECRET;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_SECRET;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_KEY;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_SECRET;
import static com.piusvelte.sonet.Sonet.TOKEN;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class SonetComments extends ListActivity implements OnClickListener, OnCancelListener {
	private static final String TAG = "SonetComments";
	private int mService = 0;
	private long mAccount = Sonet.INVALID_ACCOUNT_ID;
	private String mSid;
	private String mEsid;
	private Uri mData;
	private List<HashMap<String, String>> mComments = new ArrayList<HashMap<String, String>>();
	private ProgressDialog mLoadingDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.comments);
		Intent intent = getIntent();
		if (intent != null) {
			mData = intent.getData();
			Cursor c = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.SERVICE, Statuses_styles.ACCOUNT, Statuses_styles.SID, Statuses_styles.ESID}, Statuses_styles._ID + "=?", new String[] {mData.getLastPathSegment()}, null);
			if (c.moveToFirst()) {
				mService = c.getInt(c.getColumnIndex(Statuses_styles.SERVICE));
				mAccount = c.getLong(c.getColumnIndex(Statuses_styles.ACCOUNT));
				mSid = c.getString(c.getColumnIndex(Statuses_styles.SID));
				mEsid = c.getString(c.getColumnIndex(Statuses_styles.ESID));
			}
			c.close();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mLoadingDialog = new ProgressDialog(this);
		mLoadingDialog.setMessage(getString(R.string.loading));
		mLoadingDialog.setCancelable(true);
		mLoadingDialog.setOnCancelListener(this);
		mLoadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), this);
		mLoadingDialog.show();
		Cursor account;
		SonetOAuth sonetOAuth;
		switch (mService) {
		case FACEBOOK:
			account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
			if (account.moveToFirst()) {
				String token = account.getString(account.getColumnIndex(Accounts.TOKEN));
				String response = Sonet.httpResponse(new HttpGet(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, mSid, TOKEN, token)));
				if (response != null) {
					try {
						JSONArray comments = new JSONObject(response).getJSONArray("data");
						for (int i = 0; i < comments.length(); i++) {
							JSONObject comment = comments.getJSONObject(i);
							String id = comment.getString("id");
							boolean like = true;
							String response2 = Sonet.httpResponse(new HttpGet(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, mSid, TOKEN, token)));
							if (response2 != null) {
								try {
									JSONArray likes = new JSONObject(response2).getJSONArray("data");
									for (int l = 0; l < likes.length(); l++) {
										JSONObject like2 = likes.getJSONObject(l);
										if (like2.getString("id") == account.getString(account.getColumnIndex(Accounts.SID))) {
											like = false;
											break;
										}
									}
								} catch (JSONException e) {
									Log.e(TAG,e.toString());
								}
							}
							HashMap<String, String> commentMap = new HashMap<String, String>();
							commentMap.put(Statuses.SID, id);
							commentMap.put(Entities.FRIEND, comment.getJSONObject("from").getString("name"));
							commentMap.put(Statuses.MESSAGE, comment.getString("message"));
							//TODO: get time24hr value for this widget/account
							commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(Long.parseLong(comment.getString("created_time")) * 1000, false));
							commentMap.put(getString(R.string.like), getString(like ? R.string.like : R.string.unlike));
							mComments.add(commentMap);
						}
					} catch (JSONException e) {
						Log.e(TAG, e.toString());
					}
				}
			}
			account.close();
			break;
		case MYSPACE:
			account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
			if (account.moveToFirst()) {
				sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
				String response;
				try {
					response = sonetOAuth.httpResponse(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, mEsid, mSid)));
					if (response != null) {
						Log.v(TAG,"myspace:"+response);
						//TODO:
						JSONObject jobj = new JSONObject(response);
						//						for (int i = 0; i < comments.length(); i++) {
						//							HashMap<String, String> commentMap = new HashMap<String, String>();
						//							commentMap.put(Statuses.SID, id);
						//							commentMap.put(Entities.FRIEND, comment.getJSONObject("from").getString("name"));
						//							commentMap.put(Statuses.MESSAGE, comment.getString("message"));
						//							//TODO: get time24hr value for this widget/account
						//							commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(Long.parseLong(comment.getString("created")) * 1000, false));
						//							commentMap.put(getString(R.string.like), getString(like ? R.string.like : R.string.unlike));
						//							mComments.add(commentMap);
						//						}						
					}
				} catch (JSONException e) {
					Log.e(TAG, e.toString());
				}
			}
			account.close();
			break;
		case BUZZ:
			//TODO:
			account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
			if (account.moveToFirst()) {
				sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
				String response = sonetOAuth.httpResponse(new HttpGet(String.format(BUZZ_COMMENT, BUZZ_BASE_URL, mSid, BUZZ_API_KEY)));
				if (response != null) {
					Log.v(TAG,"response:"+response);
					try {
						JSONObject data = new JSONObject(response).getJSONObject("data");
						if (data.has("items")) {
							JSONArray items = data.getJSONArray("items");
							for (int i = 0; i < items.length(); i++) {
								JSONObject comment = items.getJSONObject(i);
								String id = comment.getString("id");
								HashMap<String, String> commentMap = new HashMap<String, String>();
								commentMap.put(Statuses.SID, id);
								commentMap.put(Entities.FRIEND, comment.getJSONObject("actor").getString("name"));
								commentMap.put(Statuses.MESSAGE, comment.getString("content"));
								//TODO: get time24hr value for this widget/account
								commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(Sonet.parseDate(comment.getString("published"), BUZZ_DATE_FORMAT), false));
								commentMap.put(getString(R.string.like), getString(R.string.like));
								mComments.add(commentMap);
							}
						}
					} catch (JSONException e) {
						Log.e(TAG,e.toString());
					}
				}
			}
			account.close();
			break;
		case LINKEDIN:
			//TODO:
			account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
			if (account.moveToFirst()) {
				sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
				HttpGet httpGet = new HttpGet(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, mSid));
				for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
				String response = sonetOAuth.httpResponse(httpGet);
				Log.v(TAG,"linkedin:"+response);
				//TODO: handle response
			}
			account.close();
			break;
		}
		mLoadingDialog.dismiss();
		this.setListAdapter(new SimpleAdapter(this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		AlertDialog.Builder dialog;
		switch (mService) {
		case FACEBOOK:
			dialog = new AlertDialog.Builder(this);
			dialog.setMessage(mComments.get(position).get(getString(R.string.like)) == getString(R.string.like) ? R.string.like : R.string.unlike)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Cursor c = SonetComments.this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (c.moveToFirst()) {
						//TODO: show progress dialog, confirm liked/unliked
						AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
							@Override
							protected String doInBackground(String... arg0) {
								return Sonet.httpResponse(arg0[2] == getString(R.string.like) ?
										new HttpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, arg0[1], TOKEN, arg0[0]))
								: new HttpDelete(String.format(FACEBOOK_LIKES, arg0[1], TOKEN, arg0[0])));
							}

							@Override
							protected void onPostExecute(String response) {
								boolean liked = false;
								if (response != null) {
									//TODO: check response, update the listview
									(Toast.makeText(SonetComments.this, getString(R.string.facebook) + " " + getString((response != null) && (response == "true") ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
								}
							}
						};
						asyncTask.execute(c.getString(c.getColumnIndex(Accounts.TOKEN)), mComments.get(which).get(Statuses.SID), mComments.get(which).get(getString(R.string.like)));
					}
					c.close();
				}
			})
			.setNegativeButton(android.R.string.cancel, this)
			.show();
			break;
		case BUZZ:
			dialog = new AlertDialog.Builder(this);
			dialog.setMessage(mComments.get(position).get(getString(R.string.like)) == getString(R.string.like) ? R.string.like : R.string.unlike)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Cursor c = SonetComments.this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (c.moveToFirst()) {
						//TODO: show progress dialog, confirm liked/unliked
						AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
							@Override
							protected String doInBackground(String... arg0) {
								SonetOAuth sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, arg0[0], arg0[1]);
								return sonetOAuth.httpResponse(arg0[3] == getString(R.string.like) ?
										new HttpPut(String.format(BUZZ_LIKE, BUZZ_BASE_URL, arg0[2], BUZZ_API_KEY))
								: new HttpDelete(String.format(BUZZ_LIKE, BUZZ_BASE_URL, arg0[2], BUZZ_API_KEY)));
							}

							@Override
							protected void onPostExecute(String response) {
								boolean liked = false;
								if (response != null) {
									//TODO: check response, update the listview
									Log.v(TAG,"like:"+response);
									(Toast.makeText(SonetComments.this, getString(R.string.buzz) + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
								}
							}
						};
						asyncTask.execute(c.getString(c.getColumnIndex(Accounts.TOKEN)), c.getString(c.getColumnIndex(Accounts.SECRET)), mComments.get(which).get(Statuses.SID), mComments.get(which).get(getString(R.string.like)));
					}
					c.close();
				}
			})
			.setNegativeButton(android.R.string.cancel, this)
			.show();
			break;
		case LINKEDIN:
			//TODO:
			break;
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		dialog.cancel();
	}

	@Override
	public void onCancel(DialogInterface arg0) {
		finish();
	}
}
