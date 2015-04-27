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

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Entities;
import com.piusvelte.sonet.social.Facebook;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.CHATTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.social.Facebook.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.social.Facebook.FACEBOOK_FRIENDS;
import static com.piusvelte.sonet.social.Facebook.FACEBOOK_PICTURE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.GOOGLEPLUS;
import static com.piusvelte.sonet.Sonet.IDENTICA;
import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.PRO;
import static com.piusvelte.sonet.Sonet.Saccess_token;
import static com.piusvelte.sonet.Sonet.Sdata;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Sname;
import static com.piusvelte.sonet.Sonet.Stags;
import static com.piusvelte.sonet.Sonet.TWITTER;

public class SelectFriends extends ListActivity {
    private static final String TAG = "SelectFriends";
    private HttpClient mHttpClient;
    private List<HashMap<String, String>> mFriends = new ArrayList<HashMap<String, String>>();
    private List<String> mSelectedFriends = new ArrayList<String>();
    private long mAccountId = Sonet.INVALID_ACCOUNT_ID;
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
            AdView adView = new AdView(this, AdSize.BANNER, BuildConfig.GOOGLEAD_ID);
            ((LinearLayout) findViewById(R.id.ad)).addView(adView);
            adView.loadAd(new AdRequest());
        }

        Intent intent = getIntent();

        if ((intent != null) && intent.hasExtra(Accounts.SID)) {
            mAccountId = intent.getLongExtra(Accounts.SID, Sonet.INVALID_ACCOUNT_ID);
            String[] tags = intent.getStringArrayExtra(Stags);
            if (tags != null) {
                for (String tag : tags) {
                    mSelectedFriends.add(tag);
                }
            }
        } else {
            finish();
        }

        mHttpClient = SonetHttpClient.getThreadSafeClient(getApplicationContext());
        registerForContextMenu(getListView());
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFriends();
    }

    private final SimpleAdapter.ViewBinder mViewBinder = new SimpleAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Object data, String textRepresentation) {
//			if (view.getId() == R.id.profile) {
//				BitmapDownloadTask task = new BitmapDownloadTask((ImageView) view, mHttpClient);
//				DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
//				((ImageView) view).setImageDrawable(downloadedDrawable);
//				task.execute(textRepresentation);
//				return true;
//			} else 
            if (view.getId() == R.id.selected) {
                ((CheckBox) view).setChecked(mSelectedFriends.contains(textRepresentation));
                return true;
            } else {
                return false;
            }
        }
    };

    @Override
    protected void onListItemClick(ListView list, final View view, int position, final long id) {
        super.onListItemClick(list, view, position, id);
        // add to/remove from return list, update check mark
        if (mFriends.size() > position) {
            HashMap<String, String> friend = mFriends.get(position);
            String esid = friend.get(Entities.ESID);
            boolean checked = false;
            if (mSelectedFriends.contains(esid)) {
                mSelectedFriends.remove(esid);
            } else {
                mSelectedFriends.add(esid);
                checked = true;
            }
            ((CheckBox) ((RelativeLayout) view).getChildAt(0)).setChecked(checked);
            String[] friends = new String[mSelectedFriends.size()];
            for (int i = 0, l = friends.length; i < l; i++) {
                friends[i] = mSelectedFriends.get(i);
            }
            Intent i = new Intent();
            i.putExtra(Accounts.SID, mAccountId);
            i.putExtra(Stags, friends);
            setResult(RESULT_OK, i);
        }
    }

    protected void loadFriends() {
        mFriends.clear();
//		SimpleAdapter sa = new SimpleAdapter(SelectFriends.this, mFriends, R.layout.friend, new String[]{Entities.PROFILE, Entities.FRIEND, Entities
// .ESID}, new int[]{R.id.profile, R.id.name, R.id.selected});
        SimpleAdapter sa = new SimpleAdapter(SelectFriends.this, mFriends, R.layout.friend, new String[] { Entities.FRIEND, Entities.ESID },
                new int[] { R.id.name, R.id.selected });
        setListAdapter(sa);
        final ProgressDialog loadingDialog = new ProgressDialog(this);
        final AsyncTask<Long, String, Boolean> asyncTask = new AsyncTask<Long, String, Boolean>() {
            @Override
            protected Boolean doInBackground(Long... params) {
                boolean loadList = false;
                SonetCrypto sonetCrypto = SonetCrypto.getInstance(getApplicationContext());
                // load the session
                Cursor account = getContentResolver()
                        .query(Accounts.getContentUri(SelectFriends.this), new String[] { Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE },
                                Accounts._ID + "=?", new String[] { Long.toString(params[0]) }, null);
                if (account.moveToFirst()) {
                    mToken = sonetCrypto.Decrypt(account.getString(0));
                    mSecret = sonetCrypto.Decrypt(account.getString(1));
                    mService = account.getInt(2);
                }
                account.close();
                String response;
                switch (mService) {
                    case TWITTER:
                        break;
                    case FACEBOOK:
                        if ((response = SonetHttpClient.httpResponse(mHttpClient,
                                new HttpGet(String.format(Facebook.FACEBOOK_FRIENDS, Facebook.FACEBOOK_BASE_URL, Saccess_token, mToken)))) != null) {
                            try {
                                JSONArray friends = new JSONObject(response).getJSONArray(Sdata);
                                for (int i = 0, l = friends.length(); i < l; i++) {
                                    JSONObject f = friends.getJSONObject(i);
                                    HashMap<String, String> newFriend = new HashMap<String, String>();
                                    newFriend.put(Entities.ESID, f.getString(Sid));
                                    newFriend.put(Entities.PROFILE, String.format(Facebook.FACEBOOK_PICTURE, f.getString(Sid)));
                                    newFriend.put(Entities.FRIEND, f.getString(Sname));
                                    // need to alphabetize
                                    if (mFriends.isEmpty()) {
                                        mFriends.add(newFriend);
                                    } else {
                                        String fullName = f.getString(Sname);
                                        int spaceIdx = fullName.lastIndexOf(" ");
                                        String newFirstName = null;
                                        String newLastName = null;
                                        if (spaceIdx == -1) {
                                            newFirstName = fullName;
                                        } else {
                                            newFirstName = fullName.substring(0, spaceIdx++);
                                            newLastName = fullName.substring(spaceIdx);
                                        }
                                        List<HashMap<String, String>> newFriends = new ArrayList<HashMap<String, String>>();
                                        for (int i2 = 0, l2 = mFriends.size(); i2 < l2; i2++) {
                                            HashMap<String, String> oldFriend = mFriends.get(i2);
                                            if (newFriend == null) {
                                                newFriends.add(oldFriend);
                                            } else {
                                                fullName = oldFriend.get(Entities.FRIEND);
                                                spaceIdx = fullName.lastIndexOf(" ");
                                                String oldFirstName = null;
                                                String oldLastName = null;
                                                if (spaceIdx == -1) {
                                                    oldFirstName = fullName;
                                                } else {
                                                    oldFirstName = fullName.substring(0, spaceIdx++);
                                                    oldLastName = fullName.substring(spaceIdx);
                                                }
                                                if (newFirstName == null) {
                                                    newFriends.add(newFriend);
                                                    newFriend = null;
                                                } else {
                                                    int comparison = oldFirstName.compareToIgnoreCase(newFirstName);
                                                    if (comparison == 0) {
                                                        // compare firstnames
                                                        if (newLastName == null) {
                                                            newFriends.add(newFriend);
                                                            newFriend = null;
                                                        } else if (oldLastName != null) {
                                                            comparison = oldLastName.compareToIgnoreCase(newLastName);
                                                            if (comparison == 0) {
                                                                newFriends.add(newFriend);
                                                                newFriend = null;
                                                            } else if (comparison > 0) {
                                                                newFriends.add(newFriend);
                                                                newFriend = null;
                                                            }
                                                        }
                                                    } else if (comparison > 0) {
                                                        newFriends.add(newFriend);
                                                        newFriend = null;
                                                    }
                                                }
                                                newFriends.add(oldFriend);
                                            }
                                        }
                                        if (newFriend != null) {
                                            newFriends.add(newFriend);
                                        }
                                        mFriends = newFriends;
                                    }
                                }
                                loadList = true;
                            } catch (JSONException e) {
                                Log.e(TAG, e.toString());
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
//					SimpleAdapter sa = new SimpleAdapter(SelectFriends.this, mFriends, R.layout.friend, new String[]{Entities.PROFILE, Entities
// .FRIEND}, new int[]{R.id.profile, R.id.name});
                    SimpleAdapter sa = new SimpleAdapter(SelectFriends.this, mFriends, R.layout.friend,
                            new String[] { Entities.FRIEND, Entities.ESID }, new int[] { R.id.name, R.id.selected });
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
                if (!asyncTask.isCancelled()) {
                    asyncTask.cancel(true);
                }
            }
        });
        loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        loadingDialog.show();
        asyncTask.execute(mAccountId);
    }
}
