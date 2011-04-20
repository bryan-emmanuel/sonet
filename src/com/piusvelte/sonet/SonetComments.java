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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;

import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.Sonet.FACEBOOK_COMMENTS;
import static com.piusvelte.sonet.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.Sonet.TOKEN;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SonetComments extends ListActivity implements OnClickListener {
	private static final String TAG = "SonetComments";
	private int mService = 0;
	private long mAccount = Sonet.INVALID_ACCOUNT_ID;
	private String mEsid;
	private Uri mData;
	private List<HashMap<String, String>> mComments = new ArrayList<HashMap<String, String>>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.comments);
		Intent intent = getIntent();
		if (intent != null) {
			mData = intent.getData();
			Cursor c = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.SERVICE, Statuses_styles.ACCOUNT, Statuses_styles.ESID}, Statuses_styles._ID + "=?", new String[] {mData.getLastPathSegment()}, null);
			if (c.moveToFirst()) {
				mService = c.getInt(c.getColumnIndex(Statuses_styles.SERVICE));
				mAccount = c.getLong(c.getColumnIndex(Statuses_styles.ACCOUNT));
				mEsid = c.getString(c.getColumnIndex(Statuses_styles.ESID));
			}
			c.close();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		//TODO: load comments
		switch (mService) {
		case FACEBOOK:
			Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
			if (account.moveToFirst()) {
				String token = account.getString(account.getColumnIndex(Accounts.TOKEN));
				String response = Sonet.httpGet(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, mEsid, TOKEN, token));
				if (response != null) {
					Log.v(TAG,"response:"+response);
					try {
						JSONArray comments = new JSONObject(response).getJSONArray("data");
						for (int i = 0; i < comments.length(); i++) {
							JSONObject comment = comments.getJSONObject(i);
							String id = comment.getString("id");
							int likes = comment.getInt("likes");
							boolean like = true;
							if (likes > 0) {
								// check if already liked
								String response2 = Sonet.httpGet(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, mEsid, TOKEN, token));
								if (response2 != null) {
									try {
										JSONArray likes2 = new JSONObject(response2).getJSONArray("data");
										for (int l = 0; l < likes2.length(); l++) {
											JSONObject like2 = likes2.getJSONObject(l);
											if (like2.getString("id") == account.getString(account.getColumnIndex(Accounts.SID))) {
												like = false;
												break;
											}
										}
									} catch (JSONException e) {
										Log.e(TAG,e.toString());
									}
								}
							}
							HashMap<String, String> commentMap = new HashMap<String, String>();
							commentMap.put(Statuses.SID, id);
							commentMap.put(Entities.FRIEND, comment.getJSONObject("from").getString("name"));
							commentMap.put(Statuses.MESSAGE, comment.getString("message"));
							//TODO: get time24hr value for this widget/account
							commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(Long.parseLong(comment.getString("created")) * 1000, false));
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
		case BUZZ:
			//TODO:
		}
		this.setListAdapter(new SimpleAdapter(this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		//TODO: should display a dialog confirming the like/unlike
		final int which = position;
		switch (mService) {
		case FACEBOOK:
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage(mComments.get(position).get(getString(R.string.like)) == getString(R.string.like) ? R.string.like : R.string.unlike)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Cursor c = SonetComments.this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (c.moveToFirst()) {
						//TODO: show progress dialog, confirm liked/unliked
						String response = mComments.get(which).get(getString(R.string.like)) == getString(R.string.like) ? Sonet.httpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, mComments.get(which).get(Statuses.SID), TOKEN, c.getString(c.getColumnIndex(Accounts.TOKEN)))) : Sonet.httpDelete(String.format(FACEBOOK_LIKES, mComments.get(which).get(Statuses.SID), TOKEN, c.getString(c.getColumnIndex(Accounts.TOKEN))));
						//TODO: check response
						Log.v(TAG,"like:"+response);
					}
					c.close();
				}
			})
			.setNegativeButton(android.R.string.cancel, this)
			.show();
			break;
		case BUZZ:
			//TODO:
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		dialog.cancel();
	}
}
