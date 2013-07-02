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
package com.piusvelte.sonet.core.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.ads.*;

import static com.piusvelte.sonet.core.Sonet.*;
import static com.piusvelte.sonet.core.SonetTokens.*;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetCrypto;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetOAuth;
import com.piusvelte.sonet.core.SonetProvider;
import com.piusvelte.sonet.core.SonetTokens;
import com.piusvelte.sonet.core.R.array;
import com.piusvelte.sonet.core.R.id;
import com.piusvelte.sonet.core.R.layout;
import com.piusvelte.sonet.core.R.menu;
import com.piusvelte.sonet.core.R.string;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.Sonet.Entities;
import com.piusvelte.sonet.core.Sonet.Notifications;
import com.piusvelte.sonet.core.Sonet.Statuses;
import com.piusvelte.sonet.core.Sonet.Statuses_styles;
import com.piusvelte.sonet.core.Sonet.Widgets;
import com.piusvelte.sonet.core.Sonet.Widgets_settings;
import com.piusvelte.sonet.core.task.CommentTask;
import com.piusvelte.sonet.core.task.LikeTask;
import com.piusvelte.sonet.core.task.chatter.Chatter;
import com.piusvelte.sonet.core.task.chatter.ChatterLikeTask;
import com.piusvelte.sonet.core.task.facebook.Facebook;
import com.piusvelte.sonet.core.task.facebook.FacebookLikeTask;
import com.piusvelte.sonet.core.task.foursquare.Foursquare;
import com.piusvelte.sonet.core.task.identica.IdenticaCommentTask;
import com.piusvelte.sonet.core.task.identica.IdenticaRepeatTask;
import com.piusvelte.sonet.core.task.linkedin.LinkedIn;
import com.piusvelte.sonet.core.task.linkedin.LinkedInLikeTask;
import com.piusvelte.sonet.core.task.myspace.MySpace;
import com.piusvelte.sonet.core.task.twitter.TwitterCommentTask;
import com.piusvelte.sonet.core.task.twitter.TwitterRetweetTask;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SonetComments extends ListActivity implements OnKeyListener, OnClickListener, TextWatcher, DialogInterface.OnClickListener, OnCancelListener {
	
	public static final String ACTION = "action";
	
	private static final String TAG = "SonetComments";
//	private int mService;
//	private long mAccount;
//	private String mSid = null;
//	private String mEsid = null;
	private EditText mMessage;
	private ImageButton mSend;
	private TextView mCount;
	private List<HashMap<String, String>> mComments = new ArrayList<HashMap<String, String>>();
//	private boolean mTime24hr = false;
//	private String mChatterInstance = null;
//	private String mChatterToken = null;
//	private String mChatterLikeId = null;
//	private String mToken = null;
//	private String mSecret = null;
//	private String mAccountSid = null;
//	private String mServiceName = null;
	private Uri mData = null;
//	private SimpleDateFormat mSimpleDateFormat = null;
//	private HttpClient mHttpClient;
	private String[] items = null;
	private AlertDialog mDialog;
	private ProgressDialog loadingDialog = new ProgressDialog(this);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// allow posting to multiple services if an account is defined
		// allow selecting which accounts to use
		// get existing comments, allow liking|unliking those comments
		setContentView(R.layout.comments);
		if (!getPackageName().toLowerCase().contains(PRO)) {
			AdView adView = new AdView(this, AdSize.BANNER, SonetTokens.GOOGLE_AD_ID);
			((LinearLayout) findViewById(R.id.ad)).addView(adView);
			adView.loadAd(new AdRequest());
		}
		mMessage = (EditText) findViewById(R.id.message);
		mSend = (ImageButton) findViewById(R.id.send);
		mCount = (TextView) findViewById(R.id.count);
		mMessage.addTextChangedListener(this);
		mMessage.setOnKeyListener(this);
		mSend.setOnClickListener(this);
		loadingDialog.setMessage(getString(R.string.loading));
		loadingDialog.setCancelable(true);
		loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		setResult(RESULT_OK);
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if (intent != null)
			mData = intent.getData();
		if (mData == null) {
			(Toast.makeText(this, getString(R.string.failure), Toast.LENGTH_LONG)).show();
			finish();
		} else
			loadComments();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if ((mDialog != null) && mDialog.isShowing())
			mDialog.dismiss();
	}

	@Override
	public void onClick(View v) {
		if (v == mSend) {
			if ((mMessage.getText().toString() != null) && (mMessage.getText().toString().length() > 0)) {
				mMessage.setEnabled(false);
				mSend.setEnabled(false);
				final CommentTask commentTask = new CommentTask(this, mData);
				loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
					@Override
					public void onCancel(DialogInterface dialog) {
						if (!commentTask.isCancelled())
							commentTask.cancel(true);
					}
				});
				loadingDialog.show();
				commentTask.comment(mMessage.getText().toString());
			} else {
				(Toast.makeText(SonetComments.this, "error parsing message body", Toast.LENGTH_LONG)).show();
				mMessage.setEnabled(true);
				mSend.setEnabled(true);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_comments, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_comments_refresh)
			loadComments();
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView list, View view, final int position, long id) {
		super.onListItemClick(list, view, position, id);
		final String sid = mComments.get(position).get(Statuses.SID);
		final String liked = mComments.get(position).get(getString(R.string.like));
		// wait for previous attempts to finish
		if ((liked.length() > 0) && !liked.equals(getString(R.string.loading))) {
			// parse comment body, as in StatusDialog.java
			Matcher m = Sonet.getLinksMatcher(mComments.get(position).get(Statuses.MESSAGE));
			int count = 0;
			while (m.find())
				count++;
			// like comments, the first comment is the post itself
			items = new String[count + 1];
			items[0] = mComments.get(position).get(getString(R.string.like));
			count = 1;
			m.reset();
			while (m.find())
				items[count++] = m.group();
			mDialog = (new AlertDialog.Builder(this))
			.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == 0) {
						setCommentStatus(0, getString(R.string.loading));
						new LikeTask(SonetComments.this, mData).like(sid, position, liked);
					} else {
						if ((which < items.length) && (items[which] != null))
							startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(items[which])));
						else
							(Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
					}
				}
			})
			.setCancelable(true)
			.setOnCancelListener(this)
			.create();
			mDialog.show();
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
	
	public void onCommentProgress(String[] params) {
		loadingDialog.setMessage(String.format(getString(R.string.sending), params[0]));
	}
	
	public void onCommentFinished(String message) {
		if (message != null)
			(Toast.makeText(this, message, Toast.LENGTH_LONG)).show();
		if (loadingDialog.isShowing())
			loadingDialog.dismiss();
		finish();
	}
	
	public void onLikeProgress(String[] result) {
		Toast.makeText(this, result[0], Toast.LENGTH_LONG).show();
	}
	
	public void onLikeFinished(int position, String like) {
		setCommentStatus(position, like);
	}

	public void setCommentStatus(int position, String status) {
		if (mComments.size() > position) {
			HashMap<String, String> comment = mComments.get(position);
			comment.put(getString(R.string.like), status);
			mComments.set(position, comment);
			setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
		}
	}
	
	public void setDefaultMessage(String message) {
		mMessage.setText("");
		if (message != null)
			mMessage.append(message);
	}
	
	public void setLikeable(String like) {
		if (like != null) {
			mSend.setEnabled(false);
			mMessage.setEnabled(false);
			mMessage.setText(R.string.uncommentable);
		} else {
			setCommentStatus(0, like);
			mMessage.setEnabled(true);
		}
	}
	
	public void uncommentable() {
		mSend.setEnabled(false);
		mMessage.setEnabled(false);
		mMessage.setText(R.string.uncommentable);
	}
	
	public void onCommentsLoaded() {
		mMessage.setEnabled(true);
		setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
		if (loadingDialog.isShowing())
			loadingDialog.dismiss();
	}
	
	public void addComment(HashMap<String, String> commentMap) {
		if (commentMap == null)
			mComments.clear();
		else
			mComments.add(commentMap);
	}

	private void loadComments() {
		mComments.clear();
		setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
		mMessage.setEnabled(false);
		mMessage.setText(R.string.loading);
		final ProgressDialog loadingDialog = new ProgressDialog(this);
		final AsyncTask<Void, String, String> asyncTask = new AsyncTask<Void, String, String>() {
			@Override
			protected String doInBackground(Void... none) {
				// load the status itself
				if (mData != null) {
					//TODO next
					String response = null;
					HttpGet httpGet;
					SonetOAuth sonetOAuth;
					boolean liked = false;
					String screen_name = "";
					switch (mService) {
					case GOOGLEPLUS:
						//TODO:
						// get plussed status
						break;
					}
					return response;
				}
				return null;
			}

			@Override
			protected void onPostExecute(String response) {
				if (response != null) {
					int i2;
					try {
						JSONArray comments;
						mSimpleDateFormat = null;
						switch (mService) {
						case GOOGLEPLUS:
							//TODO: load comments
							HttpPost httpPost = new HttpPost(GOOGLE_ACCESS);
							List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
							httpParams.add(new BasicNameValuePair("client_id", GOOGLE_CLIENTID));
							httpParams.add(new BasicNameValuePair("client_secret", GOOGLE_CLIENTSECRET));
							httpParams.add(new BasicNameValuePair("refresh_token", mToken));
							httpParams.add(new BasicNameValuePair("grant_type", "refresh_token"));
							try {
								httpPost.setEntity(new UrlEncodedFormEntity(httpParams));
								if ((response = SonetHttpClient.httpResponse(mHttpClient, httpPost)) != null) {
									JSONObject j = new JSONObject(response);
									if (j.has(Saccess_token)) {
										String access_token = j.getString(Saccess_token);
										if ((response = SonetHttpClient.httpResponse(mHttpClient, new HttpGet(String.format(GOOGLEPLUS_ACTIVITY, GOOGLEPLUS_BASE_URL, mSid, access_token)))) != null) {
											// check for a newer post, if it's the user's own, then set CLEARED=0
											try {
												JSONObject item = new JSONObject(response);
												if (item.has(Sobject)) {
													JSONObject object = item.getJSONObject(Sobject);
													if (object.has(Sreplies)) {
														int commentCount = 0;
														JSONObject replies = object.getJSONObject(Sreplies);
														if (replies.has(StotalItems)) {
															//TODO: load comments
															commentCount = replies.getInt(StotalItems);
														}
													}
												}
											} catch (JSONException e) {
												Log.e(TAG,e.toString());
											}
										}
									}
								}
							} catch (UnsupportedEncodingException e) {
								Log.e(TAG,e.toString());
							} catch (JSONException e) {
								Log.e(TAG,e.toString());
							}
							break;
						}
					} catch (JSONException e) {
						Log.e(TAG, e.toString());
					}
				}
				setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
				if (loadingDialog.isShowing())
					loadingDialog.dismiss();
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

	@Override
	public void onCancel(DialogInterface dialog) {
	}
}