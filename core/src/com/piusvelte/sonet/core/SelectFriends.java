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
package com.piusvelte.sonet.core;

import static com.piusvelte.sonet.core.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_FRIENDS;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_PICTURE;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN;
import static com.piusvelte.sonet.core.Sonet.PRO;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.Sonet.Sdata;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.TWITTER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.piusvelte.sonet.core.BitmapDownloadTask.DownloadedDrawable;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.Sonet.Entities;
import static com.piusvelte.sonet.core.Sonet.*;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SelectFriends extends ListActivity {
	private static final String TAG = "SelectFriends";
	private HttpClient mHttpClient;
	private List<HashMap<String, String>> mFriends = new ArrayList<HashMap<String, String>>();
	private List<String> mSelectedFriends = new ArrayList<String>();
	private Uri mData = null;
	private String mToken = null;
	private String mSecret = null;
	private int mService;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// allow posting to multiple services if an account is defined
		// allow selecting which accounts to use
		// get existing comments, allow liking|unliking those comments
		setContentView(R.layout.friends);
		if (!getPackageName().toLowerCase().contains(PRO)) {
			AdView adView = new AdView(this, AdSize.BANNER, SonetTokens.GOOGLE_AD_ID);
			((LinearLayout) findViewById(R.id.ad)).addView(adView);
			adView.loadAd(new AdRequest());
		}
		Intent intent = getIntent();
		if (intent != null) {
			mData = intent.getData();
		}

		mHttpClient = SonetHttpClient.getThreadSafeClient(getApplicationContext());
		registerForContextMenu(getListView());
//		setResult(RESULT_OK);
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadFriends();
	}

	private final SimpleAdapter.ViewBinder mViewBinder = new SimpleAdapter.ViewBinder() {
		@Override
		public boolean setViewValue(View view, Object data, String textRepresentation) {
			if (view.getId() == R.id.profile) {
				BitmapDownloadTask task = new BitmapDownloadTask((ImageView) view, mHttpClient);
                DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                ((ImageView) view).setImageDrawable(downloadedDrawable);
                task.execute(textRepresentation);
				return true;
			} else if (view.getId() == R.id.selected) {
				((CheckBox) view).setChecked(mSelectedFriends.contains(textRepresentation));
				return true;
			} else
				return false;
		}
	};

	@Override
	protected void onListItemClick(ListView list, final View view, int position, final long id) {
		super.onListItemClick(list, view, position, id);
		// add to/remove from return list, update check mark
		if (mFriends.size() > position) {
			HashMap<String, String> friend = mFriends.get(position);
			String esid = friend.get(Entities.ESID);
			if (mSelectedFriends.contains(esid)) {
				mSelectedFriends.remove(esid);
				((CheckBox) view).setChecked(false);
			} else {
				mSelectedFriends.add(esid);
				((CheckBox) view).setChecked(true);
			}
		}
	}

	protected void loadFriends() {
		mFriends.clear();
		SimpleAdapter sa = new SimpleAdapter(SelectFriends.this, mFriends, R.layout.friend, new String[]{Entities.PROFILE, Entities.FRIEND, Entities.ESID}, new int[]{R.id.profile, R.id.name, R.id.selected});
		setListAdapter(sa);
		final ProgressDialog loadingDialog = new ProgressDialog(this);
		final AsyncTask<Void, String, Boolean> asyncTask = new AsyncTask<Void, String, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... none) {
				boolean loadList = false;
				SonetCrypto sonetCrypto = SonetCrypto.getInstance(getApplicationContext());
				// load the session
				Cursor account = getContentResolver().query(Accounts.getContentUri(SelectFriends.this), new String[]{Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
				if (account.moveToFirst()) {
					mToken = sonetCrypto.Decrypt(account.getString(0));
					mSecret = sonetCrypto.Decrypt(account.getString(1));
					mService = account.getInt(3);
				}
				account.close();
				String response;
				switch (mService) {
				case TWITTER:
					break;
				case FACEBOOK:
					if ((response = SonetHttpClient.httpResponse(mHttpClient, new HttpGet(String.format(FACEBOOK_FRIENDS, FACEBOOK_BASE_URL, Saccess_token, mToken)))) != null) {
						try {
							JSONArray friends = new JSONObject(response).getJSONArray(Sdata);
							for (int i = 0, i2 = friends.length(); i < i2; i++) {
								JSONObject f = friends.getJSONObject(i);
								HashMap<String, String> friend = new HashMap<String, String>();
								friend.put(Entities.ESID, f.getString(Sid));
								friend.put(Entities.PROFILE, String.format(FACEBOOK_PICTURE, f.getString(Sid)));
								friend.put(Entities.FRIEND, f.getString(Sname));
								mFriends.add(friend);
							}
							loadList = true;
						} catch (JSONException e) {
							Log.e(TAG,e.toString());
						}
					}
					break;
				case MYSPACE:
					break;
				case LINKEDIN:
					break;
				case FOURSQUARE:
					break;
				case IDENTICA:
					break;
				case GOOGLEPLUS:
					break;
				case CHATTER:
					break;
				}
				return loadList;
			}

			@Override
			protected void onPostExecute(Boolean loadList) {
				if (loadList) {
					SimpleAdapter sa = new SimpleAdapter(SelectFriends.this, mFriends, R.layout.friend, new String[]{Entities.PROFILE, Entities.FRIEND}, new int[]{R.id.profile, R.id.name});
					sa.setViewBinder(mViewBinder);
					setListAdapter(sa);
				}
				if (loadingDialog.isShowing()) loadingDialog.dismiss();
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
		asyncTask.execute();
	}
}
