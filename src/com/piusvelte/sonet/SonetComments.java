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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.ads.*;
import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;

import static com.piusvelte.sonet.Sonet.BUZZ_BASE_URL;
import static com.piusvelte.sonet.Sonet.BUZZ_COMMENT;
import static com.piusvelte.sonet.Sonet.BUZZ_DATE_FORMAT;
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
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOODCOMMENTS;
import static com.piusvelte.sonet.Sonet.MYSPACE_DATE_FORMAT;
import static com.piusvelte.sonet.SonetTokens.BUZZ_API_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_SECRET;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_SECRET;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_KEY;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_SECRET;
import static com.piusvelte.sonet.Sonet.TOKEN;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_BASE_URL;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_GET_CHECKIN;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class SonetComments extends ListActivity implements OnClickListener {
	private static final String TAG = "SonetComments";
	private int mService = 0;
	private long mAccount = Sonet.INVALID_ACCOUNT_ID;
	private String mSid;
	private String mEsid;
	private Uri mData;
	private List<HashMap<String, String>> mComments = new ArrayList<HashMap<String, String>>();
	private boolean mTime24hr = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.comments);
		AdView adView = new AdView(this, AdSize.BANNER, Sonet.GOOGLE_AD_ID);
		((LinearLayout) findViewById(R.id.ad)).addView(adView);
		adView.loadAd(new AdRequest());
		Intent intent = getIntent();
		if (intent != null) {
			mData = intent.getData();
			Cursor c = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.SERVICE, Statuses_styles.ACCOUNT, Statuses_styles.SID, Statuses_styles.ESID, Statuses_styles.WIDGET}, Statuses_styles._ID + "=?", new String[] {mData.getLastPathSegment()}, null);
			if (c.moveToFirst()) {
				mService = c.getInt(c.getColumnIndex(Statuses_styles.SERVICE));
				mAccount = c.getLong(c.getColumnIndex(Statuses_styles.ACCOUNT));
				mSid = Sonet.removeUnderscore(c.getString(c.getColumnIndex(Statuses_styles.SID)));
				mEsid = Sonet.removeUnderscore(c.getString(c.getColumnIndex(Statuses_styles.ESID)));
				Cursor widget = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(c.getInt(c.getColumnIndex(Statuses_styles.WIDGET))), Long.toString(mAccount)}, null);
				mTime24hr = widget.moveToFirst() ? widget.getInt(widget.getColumnIndex(Widgets.TIME24HR)) == 1 : false;
				widget.close();
			}
			c.close();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadComments();
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
	
	private void loadComments() {
		mComments = new ArrayList<HashMap<String, String>>();
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
				if (response != null) {
					try {
						JSONArray comments;
						switch (mService) {
						case FACEBOOK:
							comments = new JSONObject(response).getJSONArray("data");
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
							break;
						case MYSPACE:
							comments = new JSONObject(response).getJSONArray("entry");
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
							break;
						case BUZZ:
							JSONObject data = new JSONObject(response).getJSONObject("data");
							if (data.has("items")) {
								comments = data.getJSONArray("items");
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
							}
							break;
						case LINKEDIN:
							JSONObject jsonResponse = new JSONObject(response);
							if (jsonResponse.has("_total") && (jsonResponse.getInt("_total") != 0)) {
								comments = jsonResponse.getJSONArray("values");
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
							}
							break;
						case FOURSQUARE:
							comments = new JSONObject(response).getJSONObject("response").getJSONObject("checkin").getJSONObject("comments").getJSONArray("items");
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
							break;
						}
					} catch (JSONException e) {
						Log.e(TAG, e.toString());
					}
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
