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

import static com.piusvelte.sonet.Sonet.*;
import static com.piusvelte.sonet.SonetTokens.*;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Notifications;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;
import com.piusvelte.sonet.Sonet.Widgets_settings;

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

public class SonetComments extends ListActivity implements OnKeyListener, OnClickListener, TextWatcher, DialogInterface.OnClickListener, OnCancelListener {
	private static final String TAG = "SonetComments";
	private int mService;
	private long mAccount;
	private String mSid = null;
	private String mEsid = null;
	private EditText mMessage;
	private Button mSend;
	private TextView mCount;
	private List<HashMap<String, String>> mComments = new ArrayList<HashMap<String, String>>();
	private boolean mTime24hr = false;
	private String mChatterInstance = null;
	private String mChatterToken = null;
	private String mChatterLikeId = null;
	private String mToken = null;
	private String mSecret = null;
	private String mAccountSid = null;
	private String mServiceName = null;
	private Uri mData = null;
	private SimpleDateFormat mSimpleDateFormat = null;
	private HttpClient mHttpClient;
	private String[] items = null;
	private AlertDialog mDialog;

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
		mSend = (Button) findViewById(R.id.send);
		mCount = (TextView) findViewById(R.id.count);
		Intent intent = getIntent();
		if (intent != null) {
			mData = intent.getData();
		}
		mMessage.addTextChangedListener(this);
		mMessage.setOnKeyListener(this);
		mSend.setOnClickListener(this);

		mHttpClient = SonetHttpClient.getThreadSafeClient(getApplicationContext());

		setResult(RESULT_OK);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mData == null) {
			(Toast.makeText(this, getString(R.string.failure), Toast.LENGTH_LONG)).show();
			finish();
		} else {
			loadComments();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if ((mDialog != null) && mDialog.isShowing()) {
			mDialog.dismiss();
		}
	}

	@Override
	public void onClick(View v) {
		if (v == mSend) {
			if ((mMessage.getText().toString() != null) && (mMessage.getText().toString().length() > 0) && (mSid != null) && (mEsid != null)) {
				mMessage.setEnabled(false);
				mSend.setEnabled(false);
				// post or comment!
				final ProgressDialog loadingDialog = new ProgressDialog(this);
				final AsyncTask<Void, String, String> asyncTask = new AsyncTask<Void, String, String>() {
					@Override
					protected String doInBackground(Void... arg0) {
						List<NameValuePair> params;
						String message;
						String response = null;
						HttpPost httpPost;
						SonetOAuth sonetOAuth;
						String serviceName = Sonet.getServiceName(getResources(), mService);
						publishProgress(serviceName);
						switch (mService) {
						case TWITTER:
							// limit tweets to 140, breaking up the message if necessary
							sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, mToken, mSecret);
							message = mMessage.getText().toString();
							while (message.length() > 0) {
								final String send;
								if (message.length() > 140) {
									// need to break on a word
									int end = 0;
									int nextSpace = 0;
									for (int i = 0, i2 = message.length(); i < i2; i++) {
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
								} else {
									send = message;
									message = "";
								}
								httpPost = new HttpPost(String.format(TWITTER_UPDATE, TWITTER_BASE_URL));
								// resolve Error 417 Expectation by Twitter
								httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
								params = new ArrayList<NameValuePair>();
								params.add(new BasicNameValuePair(Sstatus, send));
								params.add(new BasicNameValuePair(Sin_reply_to_status_id, mSid));
								try {
									httpPost.setEntity(new UrlEncodedFormEntity(params));
									response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPost));
								} catch (UnsupportedEncodingException e) {
									Log.e(TAG, e.toString());
								}
							}
							break;
						case FACEBOOK:
							httpPost = new HttpPost(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, mSid, Saccess_token, mToken));
							params = new ArrayList<NameValuePair>();
							params.add(new BasicNameValuePair(Smessage, mMessage.getText().toString()));
							try {
								httpPost.setEntity(new UrlEncodedFormEntity(params));
								response = SonetHttpClient.httpResponse(mHttpClient, httpPost);
							} catch (UnsupportedEncodingException e) {
								Log.e(TAG, e.toString());
							}
							break;
						case MYSPACE:
							sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, mToken, mSecret);
							try {
								httpPost = new HttpPost(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, mEsid, mSid));
								httpPost.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOODCOMMENTS_BODY, mMessage.getText().toString())));
								response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPost));
							} catch (IOException e) {
								Log.e(TAG, e.toString());
							}
							break;
						case FOURSQUARE:
							try {
								message = URLEncoder.encode(mMessage.getText().toString(), "UTF-8");
								httpPost = new HttpPost(String.format(FOURSQUARE_ADDCOMMENT, FOURSQUARE_BASE_URL, mSid, message, mToken));
								response = SonetHttpClient.httpResponse(mHttpClient, httpPost);
							} catch (UnsupportedEncodingException e) {
								Log.e(TAG, e.toString());
							}
							break;
						case LINKEDIN:
							sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, mToken, mSecret);
							try {
								httpPost = new HttpPost(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, mSid));
								httpPost.setEntity(new StringEntity(String.format(LINKEDIN_COMMENT_BODY, mMessage.getText().toString())));
								httpPost.addHeader(new BasicHeader("Content-Type", "application/xml"));
								response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPost));
							} catch (IOException e) {
								Log.e(TAG, e.toString());
							}
							break;
						case IDENTICA:
							// limit tweets to 140, breaking up the message if necessary
							sonetOAuth = new SonetOAuth(IDENTICA_KEY, IDENTICA_SECRET, mToken, mSecret);
							message = mMessage.getText().toString();
							while (message.length() > 0) {
								final String send;
								if (message.length() > 140) {
									// need to break on a word
									int end = 0;
									int nextSpace = 0;
									for (int i = 0, i2 = message.length(); i < i2; i++) {
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
								} else {
									send = message;
									message = "";
								}
								httpPost = new HttpPost(String.format(IDENTICA_UPDATE, IDENTICA_BASE_URL));
								// resolve Error 417 Expectation by Twitter
								httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
								params = new ArrayList<NameValuePair>();
								params.add(new BasicNameValuePair(Sstatus, send));
								params.add(new BasicNameValuePair(Sin_reply_to_status_id, mSid));
								try {
									httpPost.setEntity(new UrlEncodedFormEntity(params));
									response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPost));
								} catch (UnsupportedEncodingException e) {
									Log.e(TAG, e.toString());
								}
							}
							break;
						case GOOGLEPLUS:
							break;
						case CHATTER:
							httpPost = new HttpPost(String.format(CHATTER_URL_COMMENT, mChatterInstance, mSid, Uri.encode(mMessage.getText().toString())));
							httpPost.setHeader("Authorization", "OAuth " + mChatterToken);
							response = SonetHttpClient.httpResponse(mHttpClient, httpPost);
							break;
						}
						return ((response == null) && (mService == MYSPACE)) ? null : serviceName + " " + getString(response != null ? R.string.success : R.string.failure);
					}

					@Override
					protected void onProgressUpdate(String... params) {
						loadingDialog.setMessage(String.format(getString(R.string.sending), params[0]));
					}

					@Override
					protected void onPostExecute(String result) {
						if (result != null) {
							(Toast.makeText(SonetComments.this, result, Toast.LENGTH_LONG)).show();
						} else if (mService == MYSPACE) {
							// myspace permissions
							(Toast.makeText(SonetComments.this, SonetComments.this.getResources().getStringArray(R.array.service_entries)[MYSPACE] + getString(R.string.failure) + " " + getString(R.string.myspace_permissions_message), Toast.LENGTH_LONG)).show();
						}
						if (loadingDialog.isShowing()) loadingDialog.dismiss();
						finish();
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
			} else {
				(Toast.makeText(SonetComments.this, "error parsing message body", Toast.LENGTH_LONG)).show();
				mMessage.setEnabled(true);
				mSend.setEnabled(true);
			}
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
	protected void onListItemClick(ListView list, View view, final int position, long id) {
		super.onListItemClick(list, view, position, id);
		final String sid = mComments.get(position).get(Statuses.SID);
		final String liked = mComments.get(position).get(getString(R.string.like));
		// wait for previous attempts to finish
		if ((liked.length() > 0) && !liked.equals(getString(R.string.loading))) {
			// parse comment body, as in StatusDialog.java
			Matcher m = Sonet.getLinksMatcher(mComments.get(position).get(Statuses.MESSAGE));
			int count = 0;
			while (m.find()) {
				count++;
			}
			// like comments, the first comment is the post itself
			switch (mService) {
			case TWITTER:
				// retweet
				items = new String[count + 1];
				items[0] = getString(R.string.retweet);
				count = 1;
				m.reset();
				while (m.find()) {
					items[count++] = m.group();
				}
				mDialog = (new AlertDialog.Builder(this))
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, mToken, mSecret);
									HttpPost httpPost = new HttpPost(String.format(TWITTER_RETWEET, TWITTER_BASE_URL, sid));
									// resolve Error 417 Expectation by Twitter
									httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
									return SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPost));
								}

								@Override
								protected void onPostExecute(String response) {
									setCommentStatus(0, getString(R.string.retweet));
									(Toast.makeText(SonetComments.this, mServiceName + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
								}
							};
							setCommentStatus(0, getString(R.string.loading));
							asyncTask.execute();
						} else {
							if ((which < items.length) && (items[which] != null)) {
								// open link
								Matcher m = Sonet.getLinksMatcher(items[which]);
								if (m.find()) {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(m.group())));
								}
							} else {
								(Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
							}
						}
					}
				})
				.setCancelable(true)
				.setOnCancelListener(this)
				.create();
				mDialog.show();
				break;
			case FACEBOOK:
				items = new String[count + 1];
				items[0] = getString(mComments.get(position).get(getString(R.string.like)) == getString(R.string.like) ? R.string.like : R.string.unlike);
				count = 1;
				m.reset();
				while (m.find()) {
					items[count++] = m.group();
				}
				mDialog = (new AlertDialog.Builder(this))
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									if (liked.equals(getString(R.string.like))) {
										return SonetHttpClient.httpResponse(mHttpClient, new HttpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, sid, Saccess_token, mToken)));
									} else {
										HttpDelete httpDelete = new HttpDelete(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, sid, Saccess_token, mToken));
										httpDelete.setHeader("Content-Length", "0");
										return SonetHttpClient.httpResponse(mHttpClient, httpDelete);
									}
								}

								@Override
								protected void onPostExecute(String response) {
									if (response != null) {
										setCommentStatus(position, getString(liked.equals(getString(R.string.like)) ? R.string.unlike : R.string.like));
										(Toast.makeText(SonetComments.this, mServiceName + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
									} else {
										setCommentStatus(position, getString(liked.equals(getString(R.string.like)) ? R.string.like : R.string.unlike));
										(Toast.makeText(SonetComments.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
									}
								}
							};
							setCommentStatus(position, getString(R.string.loading));
							asyncTask.execute();
						} else {
							if ((which < items.length) && (items[which] != null)) {
								// open link
								Matcher m = Sonet.getLinksMatcher(items[which]);
								if (m.find()) {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(m.group())));
								}
							} else {
								(Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
							}
						}
					}
				})
				.setCancelable(true)
				.setOnCancelListener(this)
				.create();
				mDialog.show();
				break;
			case LINKEDIN:
				if (position == 0) {
					items = new String[count + 1];
					items[0] = getString(mComments.get(position).get(getString(R.string.like)) == getString(R.string.like) ? R.string.like : R.string.unlike);
					count = 1;
					m.reset();
					while (m.find()) {
						items[count++] = m.group();
					}
					mDialog = (new AlertDialog.Builder(this))
					.setItems(items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 0) {
								AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
									@Override
									protected String doInBackground(String... arg0) {
										SonetOAuth sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, mToken, mSecret);
										HttpPut httpPut = new HttpPut(String.format(LINKEDIN_IS_LIKED, LINKEDIN_BASE_URL, mSid));
										httpPut.addHeader(new BasicHeader("Content-Type", "application/xml"));
										try {
											httpPut.setEntity(new StringEntity(String.format(LINKEDIN_LIKE_BODY, Boolean.toString(liked.equals(getString(R.string.like))))));
											return SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPut));
										} catch (UnsupportedEncodingException e) {
											Log.e(TAG, e.toString());
										}
										return null;
									}

									@Override
									protected void onPostExecute(String response) {
										if (response != null) {
											setCommentStatus(position, getString(liked.equals(getString(R.string.like)) ? R.string.unlike : R.string.like));
											(Toast.makeText(SonetComments.this, mServiceName + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
										} else {
											setCommentStatus(position, getString(liked.equals(getString(R.string.like)) ? R.string.like : R.string.unlike));
											(Toast.makeText(SonetComments.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
										}
									}
								};
								setCommentStatus(position, getString(R.string.loading));
								asyncTask.execute();
							} else {
								if ((which < items.length) && (items[which] != null)) {
									// open link
									Matcher m = Sonet.getLinksMatcher(items[which]);
									if (m.find()) {
										startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(m.group())));
									}
								} else {
									(Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
								}
							}
						}
					})
					.setCancelable(true)
					.setOnCancelListener(this)
					.create();
					mDialog.show();
				} else {
					// no like option here
					items = new String[count];
					count = 1;
					m.reset();
					while (m.find()) {
						items[count++] = m.group();
					}
					mDialog = (new AlertDialog.Builder(this))
					.setItems(items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if ((which < items.length) && (items[which] != null)) {
								// open link
								Matcher m = Sonet.getLinksMatcher(items[which]);
								if (m.find()) {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(m.group())));
								}
							} else {
								(Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
							}
						}
					})
					.setCancelable(true)
					.setOnCancelListener(this)
					.create();
					mDialog.show();
				}
				break;
			case IDENTICA:
				// retweet
				items = new String[count + 1];
				items[0] = getString(R.string.repeat);
				count = 1;
				m.reset();
				while (m.find()) {
					items[count++] = m.group();
				}
				mDialog = (new AlertDialog.Builder(this))
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
								@Override
								protected String doInBackground(String... arg0) {
									SonetOAuth sonetOAuth = new SonetOAuth(IDENTICA_KEY, IDENTICA_SECRET, mToken, mSecret);
									HttpPost httpPost = new HttpPost(String.format(IDENTICA_RETWEET, IDENTICA_BASE_URL, sid));
									// resolve Error 417 Expectation by Twitter
									httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
									return SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPost));
								}

								@Override
								protected void onPostExecute(String response) {
									setCommentStatus(0, getString(R.string.repeat));
									(Toast.makeText(SonetComments.this, mServiceName + " " + getString(response != null ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
								}
							};
							setCommentStatus(0, getString(R.string.loading));
							asyncTask.execute();
						} else {
							if ((which < items.length) && (items[which] != null)) {
								// open link
								Matcher m = Sonet.getLinksMatcher(items[which]);
								if (m.find()) {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(m.group())));
								}
							} else {
								(Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
							}
						}
					}
				})
				.setCancelable(true)
				.setOnCancelListener(this)
				.create();
				mDialog.show();
				break;
			case GOOGLEPLUS:
				//TODO:
				// plus1
				items = new String[count];
				count = 1;
				m.reset();
				while (m.find()) {
					items[count++] = m.group();
				}
				mDialog = (new AlertDialog.Builder(this))
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if ((which < items.length) && (items[which] != null)) {
							// open link
							Matcher m = Sonet.getLinksMatcher(items[which]);
							if (m.find()) {
								startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(m.group())));
							}
						} else {
							(Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
						}
					}
				})
				.setCancelable(true)
				.setOnCancelListener(this)
				.create();
				mDialog.show();
				break;
			case CHATTER:
				if (position == 0) {
					items = new String[count + 1];
					items[0] = getString(mComments.get(position).get(getString(R.string.like)) == getString(R.string.like) ? R.string.like : R.string.unlike);
					count = 1;
					m.reset();
					while (m.find()) {
						items[count++] = m.group();
					}
					mDialog = (new AlertDialog.Builder(this))
					.setItems(items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 0) {
								AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
									@Override
									protected String doInBackground(String... arg0) {
										HttpUriRequest httpRequest;
										if (liked.equals(getString(R.string.like))) {
											httpRequest = new HttpPost(String.format(CHATTER_URL_LIKES, mChatterInstance, mSid));
										} else {
											httpRequest = new HttpDelete(String.format(CHATTER_URL_LIKE, mChatterInstance, mChatterLikeId));
										}
										httpRequest.setHeader("Authorization", "OAuth " + mChatterToken);
										return SonetHttpClient.httpResponse(mHttpClient, httpRequest);
									}

									@Override
									protected void onPostExecute(String response) {
										if (response != null) {
											setCommentStatus(position, getString(liked.equals(getString(R.string.like)) ? R.string.unlike : R.string.like));
											(Toast.makeText(SonetComments.this, mServiceName + " " + getString(R.string.success), Toast.LENGTH_LONG)).show();
										} else {
											setCommentStatus(position, getString(liked.equals(getString(R.string.like)) ? R.string.like : R.string.unlike));
											(Toast.makeText(SonetComments.this, mServiceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
										}
									}
								};
								setCommentStatus(position, getString(R.string.loading));
								asyncTask.execute();
							} else {
								if ((which < items.length) && (items[which] != null)) {
									// open link
									Matcher m = Sonet.getLinksMatcher(items[which]);
									if (m.find()) {
										startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(m.group())));
									}
								} else {
									(Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
								}
							}
						}
					})
					.setCancelable(true)
					.setOnCancelListener(this)
					.create();
					mDialog.show();
				} else {
					// no like option here
					items = new String[count];
					count = 1;
					m.reset();
					while (m.find()) {
						items[count++] = m.group();
					}
					mDialog = (new AlertDialog.Builder(this))
					.setItems(items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if ((which < items.length) && (items[which] != null)) {
								// open link
								Matcher m = Sonet.getLinksMatcher(items[which]);
								if (m.find()) {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(m.group())));
								}
							} else {
								(Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
							}
						}
					})
					.setCancelable(true)
					.setOnCancelListener(this)
					.create();
					mDialog.show();
				}
				break;
			}
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

	private void setCommentStatus(int position, String status) {
		if (mComments.size() > position) {
			HashMap<String, String> comment = mComments.get(position);
			comment.put(getString(R.string.like), status);
			mComments.set(position, comment);
			setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
		}
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
					SonetCrypto sonetCrypto = SonetCrypto.getInstance(getApplicationContext());
					UriMatcher um = new UriMatcher(UriMatcher.NO_MATCH);
					um.addURI(SonetProvider.AUTHORITY, SonetProvider.VIEW_STATUSES_STYLES + "/*", SonetProvider.STATUSES_STYLES);
					um.addURI(SonetProvider.AUTHORITY, SonetProvider.TABLE_NOTIFICATIONS + "/*", SonetProvider.NOTIFICATIONS);
					Cursor status;
					switch (um.match(mData)) {
					case SonetProvider.STATUSES_STYLES:
						status = getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles.ACCOUNT, Statuses_styles.SID, Statuses_styles.ESID, Statuses_styles.WIDGET, Statuses_styles.SERVICE, Statuses_styles.FRIEND, Statuses_styles.MESSAGE, Statuses_styles.CREATED}, Statuses_styles._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
						if (status.moveToFirst()) {
							mService = status.getInt(4);
							mServiceName = getResources().getStringArray(R.array.service_entries)[mService];
							mAccount = status.getLong(0);
							mSid = sonetCrypto.Decrypt(status.getString(1));
							mEsid = sonetCrypto.Decrypt(status.getString(2));
							Cursor widget = getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(status.getInt(3)), Long.toString(mAccount)}, null);
							if (widget.moveToFirst()) {
								mTime24hr = widget.getInt(0) == 1;
							} else {
								Cursor b = getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(status.getInt(3)), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
								if (b.moveToFirst()) {
									mTime24hr = b.getInt(0) == 1;
								} else {
									Cursor c = getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
									if (c.moveToFirst()) {
										mTime24hr = c.getInt(0) == 1;
									} else {
										mTime24hr = false;
									}
									c.close();
								}
								b.close();
							}
							widget.close();
							HashMap<String, String> commentMap = new HashMap<String, String>();
							commentMap.put(Statuses.SID, mSid);
							commentMap.put(Entities.FRIEND, status.getString(5));
							commentMap.put(Statuses.MESSAGE, status.getString(6));
							commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(status.getLong(7), mTime24hr));
							commentMap.put(getString(R.string.like), mService == TWITTER ? getString(R.string.retweet) : mService == IDENTICA ? getString(R.string.repeat) : "");
							mComments.add(commentMap);
							// load the session
							Cursor account = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts.TOKEN, Accounts.SECRET, Accounts.SID}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
							if (account.moveToFirst()) {
								mToken = sonetCrypto.Decrypt(account.getString(0));
								mSecret = sonetCrypto.Decrypt(account.getString(1));
								mAccountSid = sonetCrypto.Decrypt(account.getString(2));
							}
							account.close();
						}
						status.close();
						break;
					case SonetProvider.NOTIFICATIONS:
						Cursor notification = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications.ACCOUNT, Notifications.SID, Notifications.ESID, Notifications.FRIEND, Notifications.MESSAGE, Notifications.CREATED}, Notifications._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
						if (notification.moveToFirst()) {
							// clear notification
							ContentValues values = new ContentValues();
							values.put(Notifications.CLEARED, 1);
							getContentResolver().update(Notifications.CONTENT_URI, values, Notifications._ID + "=?", new String[]{mData.getLastPathSegment()});
							mAccount = notification.getLong(0);
							mSid = sonetCrypto.Decrypt(notification.getString(1));
							mEsid = sonetCrypto.Decrypt(notification.getString(2));
							mTime24hr = false;
							// load the session
							Cursor account = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts.TOKEN, Accounts.SECRET, Accounts.SID, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
							if (account.moveToFirst()) {
								mToken = sonetCrypto.Decrypt(account.getString(0));
								mSecret = sonetCrypto.Decrypt(account.getString(1));
								mAccountSid = sonetCrypto.Decrypt(account.getString(2));
								mService = account.getInt(3);
							}
							account.close();
							HashMap<String, String> commentMap = new HashMap<String, String>();
							commentMap.put(Statuses.SID, mSid);
							commentMap.put(Entities.FRIEND, notification.getString(3));
							commentMap.put(Statuses.MESSAGE, notification.getString(4));
							commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(notification.getLong(5), mTime24hr));
							commentMap.put(getString(R.string.like), mService == TWITTER ? getString(R.string.retweet) : getString(R.string.repeat));
							mComments.add(commentMap);
							mServiceName = getResources().getStringArray(R.array.service_entries)[mService];
						}
						notification.close();
						break;
					default:
						mComments.clear();
						HashMap<String, String> commentMap = new HashMap<String, String>();
						commentMap.put(Statuses.SID, "");
						commentMap.put(Entities.FRIEND, "");
						commentMap.put(Statuses.MESSAGE, "error, status not found");
						commentMap.put(Statuses.CREATEDTEXT, "");
						commentMap.put(getString(R.string.like), "");
						mComments.add(commentMap);
					}
					String response = null;
					HttpGet httpGet;
					SonetOAuth sonetOAuth;
					boolean liked = false;
					String screen_name = "";
					switch (mService) {
					case TWITTER:
						sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, mToken, mSecret);
						if ((response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(TWITTER_USER, TWITTER_BASE_URL, mEsid))))) != null) {
							try {
								JSONObject user = new JSONObject(response);
								screen_name = "@" + user.getString(Sscreen_name) + " ";
							} catch (JSONException e) {
								Log.e(TAG,e.toString());
							}
						}
						publishProgress(screen_name);
						response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(TWITTER_MENTIONS, TWITTER_BASE_URL, String.format(TWITTER_SINCE_ID, mSid)))));
						break;
					case FACEBOOK:
						if ((response = SonetHttpClient.httpResponse(mHttpClient, new HttpGet(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, mSid, Saccess_token, mToken)))) != null) {
							try {
								JSONArray likes = new JSONObject(response).getJSONArray(Sdata);
								for (int i = 0, i2 = likes.length(); i < i2; i++) {
									JSONObject like = likes.getJSONObject(i);
									if (like.getString(Sid).equals(mAccountSid)) {
										liked = true;
										break;
									}
								}
							} catch (JSONException e) {
								Log.e(TAG,e.toString());
							}
						}
						publishProgress(getString(liked ? R.string.unlike : R.string.like));
						response = SonetHttpClient.httpResponse(mHttpClient, new HttpGet(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, mSid, Saccess_token, mToken)));
						break;
					case MYSPACE:
						sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, mToken, mSecret);
						response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, mEsid, mSid))));
						break;
					case LINKEDIN:
						sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, mToken, mSecret);
						httpGet = new HttpGet(String.format(LINKEDIN_UPDATE, LINKEDIN_BASE_URL, mSid));
						for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
						if ((response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpGet))) != null) {
							try {
								JSONObject data = new JSONObject(response);
								if (data.has("isCommentable") && !data.getBoolean("isCommentable")) {
									publishProgress(getString(R.string.uncommentable));
								}
								if (data.has("isLikable")) {
									publishProgress(getString(data.has("isLiked") && data.getBoolean("isLiked") ? R.string.unlike : R.string.like));
								} else {
									publishProgress(getString(R.string.unlikable));
								}
							} catch (JSONException e) {
								Log.e(TAG,e.toString());
							}
						} else {
							publishProgress(getString(R.string.unlikable));							
						}
						httpGet = new HttpGet(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, mSid));
						for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
						response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpGet));
						break;
					case FOURSQUARE:
						response = SonetHttpClient.httpResponse(mHttpClient, new HttpGet(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, mSid, mToken)));
						break;
					case IDENTICA:
						sonetOAuth = new SonetOAuth(IDENTICA_KEY, IDENTICA_SECRET, mToken, mSecret);
						if ((response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(IDENTICA_USER, IDENTICA_BASE_URL, mEsid))))) != null) {
							try {
								JSONObject user = new JSONObject(response);
								screen_name = "@" + user.getString(Sscreen_name) + " ";
							} catch (JSONException e) {
								Log.e(TAG,e.toString());
							}
						}
						publishProgress(screen_name);
						response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(IDENTICA_MENTIONS, IDENTICA_BASE_URL, String.format(IDENTICA_SINCE_ID, mSid)))));
						break;
					case GOOGLEPLUS:
						//TODO:
						// get plussed status
						break;
					case CHATTER:
						// Chatter requires loading an instance
						if ((mChatterInstance == null) || (mChatterToken == null)) {
							if ((response = SonetHttpClient.httpResponse(mHttpClient, new HttpPost(String.format(CHATTER_URL_ACCESS, CHATTER_KEY, mToken)))) != null) {
								try {
									JSONObject jobj = new JSONObject(response);
									if (jobj.has("instance_url") && jobj.has(Saccess_token)) {
										mChatterInstance = jobj.getString("instance_url");
										mChatterToken = jobj.getString(Saccess_token);
									}
								} catch (JSONException e) {
									Log.e(TAG,e.toString());
								}
							}
						}
						if ((mChatterInstance != null) && (mChatterToken != null)) {
							httpGet = new HttpGet(String.format(CHATTER_URL_LIKES, mChatterInstance, mSid));
							httpGet.setHeader("Authorization", "OAuth " + mChatterToken);
							if ((response = SonetHttpClient.httpResponse(mHttpClient, httpGet)) != null) {
								try {
									JSONObject jobj = new JSONObject(response);
									if (jobj.getInt(Stotal) > 0) {
										JSONArray likes = jobj.getJSONArray("likes");
										for (int i = 0, i2 = likes.length(); i < i2; i++) {
											JSONObject like = likes.getJSONObject(i);
											if (like.getJSONObject(Suser).getString(Sid).equals(mAccountSid)) {
												mChatterLikeId = like.getString(Sid);
												liked = true;
												break;
											}
										}
									}
								} catch (JSONException e) {
									Log.e(TAG,e.toString());
								}
							}
							publishProgress(getString(liked ? R.string.unlike : R.string.like));
							httpGet = new HttpGet(String.format(CHATTER_URL_COMMENTS, mChatterInstance, mSid));
							httpGet.setHeader("Authorization", "OAuth " + mChatterToken);
							response = SonetHttpClient.httpResponse(mHttpClient, httpGet);
						} else {
							response = null;
						}
						break;
					}
					return response;
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(String... params) {
				mMessage.setText("");
				if (params != null) {
					if ((mService == TWITTER) || (mService == IDENTICA)) {
						mMessage.append(params[0]);
					} else {
						if (mService == LINKEDIN) {
							if (params[0].equals(getString(R.string.uncommentable))) {
								mSend.setEnabled(false);
								mMessage.setEnabled(false);
								mMessage.setText(R.string.uncommentable);
							} else {
								setCommentStatus(0, params[0]);
							}
						} else {
							setCommentStatus(0, params[0]);
						}
					}
				}
				mMessage.setEnabled(true);
			}

			@Override
			protected void onPostExecute(String response) {
				if (response != null) {
					int i2;
					try {
						JSONArray comments;
						mSimpleDateFormat = null;
						switch (mService) {
						case TWITTER:
							comments = new JSONArray(response);
							if ((i2 = comments.length()) > 0) {
								for (int i = 0; i < i2; i++) {
									JSONObject comment = comments.getJSONObject(i);
									if (comment.getString(Sin_reply_to_status_id) == mSid) {
										HashMap<String, String> commentMap = new HashMap<String, String>();
										commentMap.put(Statuses.SID, comment.getString(Sid));
										commentMap.put(Entities.FRIEND, comment.getJSONObject(Suser).getString(Sname));
										commentMap.put(Statuses.MESSAGE, comment.getString(Stext));
										commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(parseDate(comment.getString(Screated_at), TWITTER_DATE_FORMAT), mTime24hr));
										commentMap.put(getString(R.string.like), getString(R.string.retweet));
										mComments.add(commentMap);
									}
								}
							} else {
								noComments();
							}
							break;
						case FACEBOOK:
							comments = new JSONObject(response).getJSONArray(Sdata);
							if ((i2 = comments.length()) > 0) {
								for (int i = 0; i < i2; i++) {
									JSONObject comment = comments.getJSONObject(i);
									HashMap<String, String> commentMap = new HashMap<String, String>();
									commentMap.put(Statuses.SID, comment.getString(Sid));
									commentMap.put(Entities.FRIEND, comment.getJSONObject(Sfrom).getString(Sname));
									commentMap.put(Statuses.MESSAGE, comment.getString(Smessage));
									commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(comment.getLong(Screated_time) * 1000, mTime24hr));
									commentMap.put(getString(R.string.like), getString(comment.has(Suser_likes) && comment.getBoolean(Suser_likes) ? R.string.unlike : R.string.like));
									mComments.add(commentMap);
								}
							} else {
								noComments();
							}
							break;
						case MYSPACE:
							comments = new JSONObject(response).getJSONArray(Sentry);
							if ((i2 = comments.length()) > 0) {
								for (int i = 0; i < i2; i++) {
									JSONObject entry = comments.getJSONObject(i);
									HashMap<String, String> commentMap = new HashMap<String, String>();
									commentMap.put(Statuses.SID, entry.getString(ScommentId));
									commentMap.put(Entities.FRIEND, entry.getJSONObject(Sauthor).getString(SdisplayName));
									commentMap.put(Statuses.MESSAGE, entry.getString(Sbody));
									commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(parseDate(entry.getString(SpostedDate), MYSPACE_DATE_FORMAT), mTime24hr));
									commentMap.put(getString(R.string.like), "");
									mComments.add(commentMap);
								}
							} else {
								noComments();
							}
							break;
						case LINKEDIN:
							JSONObject jsonResponse = new JSONObject(response);
							if (jsonResponse.has(S_total) && (jsonResponse.getInt(S_total) != 0)) {
								comments = jsonResponse.getJSONArray(Svalues);
								if ((i2 = comments.length()) > 0) {
									for (int i = 0; i < i2; i++) {
										JSONObject comment = comments.getJSONObject(i);
										JSONObject person = comment.getJSONObject(Sperson);
										HashMap<String, String> commentMap = new HashMap<String, String>();
										commentMap.put(Statuses.SID, comment.getString(Sid));
										commentMap.put(Entities.FRIEND, person.getString(SfirstName) + " " + person.getString(SlastName));
										commentMap.put(Statuses.MESSAGE, comment.getString(Scomment));
										commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(comment.getLong(Stimestamp), mTime24hr));
										commentMap.put(getString(R.string.like), "");
										mComments.add(commentMap);
									}
								} else {
									noComments();
								}
							}
							break;
						case FOURSQUARE:
							comments = new JSONObject(response).getJSONObject(Sresponse).getJSONObject(Scheckin).getJSONObject(Scomments).getJSONArray(Sitems);
							if ((i2 = comments.length()) > 0) {
								for (int i = 0; i < i2; i++) {
									JSONObject comment = comments.getJSONObject(i);
									JSONObject user = comment.getJSONObject(Suser);
									HashMap<String, String> commentMap = new HashMap<String, String>();
									commentMap.put(Statuses.SID, comment.getString(Sid));
									commentMap.put(Entities.FRIEND, user.getString(SfirstName) + " " + user.getString(SlastName));
									commentMap.put(Statuses.MESSAGE, comment.getString(Stext));
									commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(comment.getLong(ScreatedAt) * 1000, mTime24hr));
									commentMap.put(getString(R.string.like), "");
									mComments.add(commentMap);
								}
							} else {
								noComments();
							}
							break;
						case IDENTICA:
							comments = new JSONArray(response);
							if ((i2 = comments.length()) > 0) {
								for (int i = 0; i < i2; i++) {
									JSONObject comment = comments.getJSONObject(i);
									if (comment.getString(Sin_reply_to_status_id) == mSid) {
										HashMap<String, String> commentMap = new HashMap<String, String>();
										commentMap.put(Statuses.SID, comment.getString(Sid));
										commentMap.put(Entities.FRIEND, comment.getJSONObject(Suser).getString(Sname));
										commentMap.put(Statuses.MESSAGE, comment.getString(Stext));
										commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(parseDate(comment.getString(Screated_at), TWITTER_DATE_FORMAT), mTime24hr));
										commentMap.put(getString(R.string.like), getString(R.string.repeat));
										mComments.add(commentMap);
									}
								}
							} else {
								noComments();
							}
							break;
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
						case CHATTER:
							JSONObject chats = new JSONObject(response);
							if (chats.getInt(Stotal) > 0) {
								comments = chats.getJSONArray(Scomments);
								if ((i2 = comments.length()) > 0) {
									for (int i = 0; i < i2; i++) {
										JSONObject comment = comments.getJSONObject(i);
										HashMap<String, String> commentMap = new HashMap<String, String>();
										commentMap.put(Statuses.SID, comment.getString(Sid));
										commentMap.put(Entities.FRIEND, comment.getJSONObject(Suser).getString(Sname));
										commentMap.put(Statuses.MESSAGE, comment.getJSONObject(Sbody).getString(Stext));
										commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(parseDate(comment.getString(ScreatedDate), CHATTER_DATE_FORMAT), mTime24hr));
										commentMap.put(getString(R.string.like), "");
										mComments.add(commentMap);
									}
								} else {
									noComments();
								}
							} else {
								noComments();
							}
							break;
						}
					} catch (JSONException e) {
						Log.e(TAG, e.toString());
					}
				} else {
					noComments();
				}
				setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
				if (loadingDialog.isShowing()) loadingDialog.dismiss();
			}

			private void noComments() {
				HashMap<String, String> commentMap = new HashMap<String, String>();
				commentMap.put(Statuses.SID, "");
				commentMap.put(Entities.FRIEND, "");
				commentMap.put(Statuses.MESSAGE, getString(R.string.no_comments));
				commentMap.put(Statuses.CREATEDTEXT, "");
				commentMap.put(getString(R.string.like), "");
				mComments.add(commentMap);
			}

			private long parseDate(String date, String format) {
				if (date != null) {
					// hack for the literal 'Z'
					if (date.substring(date.length() - 1).equals("Z")) {
						date = date.substring(0, date.length() - 2) + "+0000";
					}
					Date created = null;
					if (format != null) {
						if (mSimpleDateFormat == null) {
							mSimpleDateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
							// all dates should be GMT/UTC
							mSimpleDateFormat.setTimeZone(sTimeZone);
						}
						try {
							created = mSimpleDateFormat.parse(date);
							return created.getTime();
						} catch (ParseException e) {
							Log.e(TAG, e.toString());
						}
					} else {
						// attempt to parse RSS date
						if (mSimpleDateFormat != null) {
							try {
								created = mSimpleDateFormat.parse(date);
								return created.getTime();
							} catch (ParseException e) {
								Log.e(TAG, e.toString());
							}
						}
						for (String rfc822 : sRFC822) {
							mSimpleDateFormat = new SimpleDateFormat(rfc822, Locale.ENGLISH);
							mSimpleDateFormat.setTimeZone(sTimeZone);
							try {
								if ((created = mSimpleDateFormat.parse(date)) != null) {
									return created.getTime();
								}
							} catch (ParseException e) {
								Log.e(TAG, e.toString());
							}
						}
					}
				}
				return System.currentTimeMillis();
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