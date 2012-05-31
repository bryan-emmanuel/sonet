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
package com.piusvelte.sonetpro;

import com.google.ads.*;

import static com.piusvelte.sonetpro.Sonet.*;
import static com.piusvelte.sonetpro.SonetTokens.*;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonetpro.Sonet.Accounts;
import com.piusvelte.sonetpro.Sonet.Notifications;
import com.piusvelte.sonetpro.Sonet.Statuses;
import com.piusvelte.sonetpro.Sonet.Widgets;
import com.piusvelte.sonetpro.Sonet.Widgets_settings;

import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SonetNotifications extends ListActivity {
	// list the current notifications
	// check for cache versions in statuses first, falling back on reloading them from the service
	private static final int CLEAR = 1;
	private static final int REFRESH = 2;
	private static final int CLEAR_ALL = 3;
	private static final String TAG = "SonetNotifications";
	private SonetCrypto mSonetCrypto;
	private SimpleDateFormat mSimpleDateFormat = null;

	// expanding notifications, check any statuses that have been commented on in the past 24 hours
	// this requires tracking the last comment date

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notifications);
		if (!getPackageName().toLowerCase().contains(PRO)) {
			AdView adView = new AdView(this, AdSize.BANNER, SonetTokens.GOOGLE_AD_ID);
			((LinearLayout) findViewById(R.id.ad)).addView(adView);
			adView.loadAd(new AdRequest());
		}
		registerForContextMenu(getListView());
		setResult(RESULT_OK);
	}

	@Override
	protected void onListItemClick(ListView list, final View view, int position, final long id) {
		super.onListItemClick(list, view, position, id);
		// load SonetComments.java, the notification will be clear there
		startActivityForResult(new Intent(this, SonetComments.class).setData(Uri.withAppendedPath(Notifications.CONTENT_URI, Long.toString(id))), RESULT_REFRESH);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		// create clearing option
		menu.add(0, CLEAR, 0, R.string.clear);
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		if (item.getItemId() == CLEAR) {
			final ProgressDialog loadingDialog = new ProgressDialog(this);
			final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... arg0) {
					// clear all notifications
					ContentValues values = new ContentValues();
					values.put(Notifications.CLEARED, 1);
					SonetNotifications.this.getContentResolver().update(Notifications.CONTENT_URI, values, Notifications._ID + "=?", new String[]{Long.toString(((AdapterContextMenuInfo) item.getMenuInfo()).id)});
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					if (loadingDialog.isShowing()) {
						loadingDialog.dismiss();
					}
					SonetNotifications.this.finish();
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
		return super.onContextItemSelected(item);
		// clear
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, REFRESH, 0, R.string.button_refresh).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, CLEAR_ALL, 0, R.string.clear_all).setIcon(android.R.drawable.ic_menu_rotate);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final ProgressDialog loadingDialog = new ProgressDialog(this);
		final AsyncTask<Integer, String, Boolean> asyncTask = new AsyncTask<Integer, String, Boolean>() {

			@Override
			protected Boolean doInBackground(Integer... arg0) {
				switch (arg0[0]) {
				case REFRESH:
					// select all accounts with notifications set
					Cursor widgets = getContentResolver().query(Widgets_settings.DISTINCT_CONTENT_URI, new String[]{Widgets.ACCOUNT}, Widgets.ACCOUNT + "!=-1 and (" + Widgets.LIGHTS + "=1 or " + Widgets.VIBRATE + "=1 or " + Widgets.SOUND + "=1)", null, null);
					if (widgets.moveToFirst()) {
						mSonetCrypto = SonetCrypto.getInstance(getApplicationContext());
						HttpClient httpClient = SonetHttpClient.getThreadSafeClient(getApplicationContext());
						while (!widgets.isAfterLast()) {
							long accountId = widgets.getLong(0);
							ArrayList<String> notificationSids = new ArrayList<String>();
							Cursor account = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE, Accounts.SID}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
							if (account.moveToFirst()) {
								// for each account, for each notification, check for updates
								// if there are no updates past 24hrs and cleared, delete
								String token = mSonetCrypto.Decrypt(account.getString(0));
								String secret = mSonetCrypto.Decrypt(account.getString(1));
								int service = account.getInt(2);
								String accountEsid = mSonetCrypto.Decrypt(account.getString(3));
								mSimpleDateFormat = null;
								if (service == TWITTER) {
									Cursor currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications.SID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(accountId)}, null);
									// loop over notifications
									if (currentNotifications.moveToFirst()) {
										// store sids, to avoid duplicates when requesting the latest feed
										String sid = mSonetCrypto.Decrypt(currentNotifications.getString(0));
										if (!notificationSids.contains(sid)) {
											notificationSids.add(sid);
										}
									}
									currentNotifications.close();
									// limit to newest status
									SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, token, secret);
									String last_sid = null;
									Cursor last_status = getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses.SID}, Statuses.ACCOUNT + "=?", new String[]{Long.toString(accountId)}, Statuses.CREATED + " ASC LIMIT 1");
									if (last_status.moveToFirst()) {
										last_sid = mSonetCrypto.Decrypt(last_status.getString(0));
									}
									last_status.close();
									// get all mentions since the oldest status for this account
									String response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(TWITTER_MENTIONS, TWITTER_BASE_URL, last_sid != null ? String.format(TWITTER_SINCE_ID, last_sid) : ""))));
									if (response != null) {
										try {
											JSONArray comments = new JSONArray(response);
											for (int i = 0, i2 = comments.length(); i < i2; i++) {
												JSONObject comment = comments.getJSONObject(i);
												JSONObject user = comment.getJSONObject(Suser);
												if (!user.getString(Sid).equals(accountEsid) && !notificationSids.contains(comment.getString(Sid))) {
													String friend = user.getString(Sname);
													addNotification(comment.getString(Sid), user.getString(Sid), friend, comment.getString("text"), parseDate(comment.getString("created_at"), TWITTER_DATE_FORMAT), accountId, friend + " mentioned you on Twitter");
												}
											}
										} catch (JSONException e) {
											Log.e(TAG, service + ":" + e.toString());
										}
									}
								} else if (service == IDENTICA) {
									Cursor currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications.SID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(accountId)}, null);
									// loop over notifications
									if (currentNotifications.moveToFirst()) {
										// store sids, to avoid duplicates when requesting the latest feed
										String sid = mSonetCrypto.Decrypt(currentNotifications.getString(0));
										if (!notificationSids.contains(sid)) {
											notificationSids.add(sid);
										}
									}
									currentNotifications.close();
									// limit to newest status
									SonetOAuth sonetOAuth = new SonetOAuth(IDENTICA_KEY, IDENTICA_SECRET, token, secret);
									String last_sid = null;
									Cursor last_status = getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses.SID}, Statuses.ACCOUNT + "=?", new String[]{Long.toString(accountId)}, Statuses.CREATED + " ASC LIMIT 1");
									if (last_status.moveToFirst()) {
										last_sid = mSonetCrypto.Decrypt(last_status.getString(0));
									}
									last_status.close();
									// get all mentions since the oldest status for this account
									String response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(IDENTICA_MENTIONS, IDENTICA_BASE_URL, last_sid != null ? String.format(IDENTICA_SINCE_ID, last_sid) : ""))));
									if (response != null) {
										try {
											JSONArray comments = new JSONArray(response);
											for (int i = 0, i2 = comments.length(); i < i2; i++) {
												JSONObject comment = comments.getJSONObject(i);
												JSONObject user = comment.getJSONObject(Suser);
												if (!user.getString(Sid).equals(accountEsid) && !notificationSids.contains(comment.getString(Sid))) {
													String friend = user.getString(Sname);
													addNotification(comment.getString(Sid), user.getString(Sid), friend, comment.getString("text"), parseDate(comment.getString("created_at"), TWITTER_DATE_FORMAT), accountId, friend + " mentioned you on Identi.ca");
												}
											}
										} catch (JSONException e) {
											Log.e(TAG, service + ":" + e.toString());
										}
									}
								} else {
									Cursor currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(accountId)}, null);
									if (currentNotifications.moveToFirst()) {
										String response;
										SonetOAuth sonetOAuth;
										switch (service) {
										case FACEBOOK:
											// loop over notifications
											while (!currentNotifications.isAfterLast()) {
												long notificationId = currentNotifications.getLong(0);
												String sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
												long updated = currentNotifications.getLong(2);
												boolean cleared = currentNotifications.getInt(3) == 1;
												// store sids, to avoid duplicates when requesting the latest feed
												if (!notificationSids.contains(sid)) {
													notificationSids.add(sid);
												}
												// get comments for current notifications
												if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, sid, Saccess_token, token)))) != null) {
													// check for a newer post, if it's the user's own, then set CLEARED=0
													try {
														JSONArray comments = new JSONObject(response).getJSONArray(Sdata);
														int i2 = comments.length();
														if (i2 > 0) {
															for (int i = 0; i < i2; i++) {
																JSONObject comment = comments.getJSONObject(i);
																long created_time = comment.getLong(Screated_time) * 1000;
																if (created_time > updated) {
																	// new comment
																	ContentValues values = new ContentValues();
																	values.put(Notifications.UPDATED, created_time);
																	JSONObject from = comment.getJSONObject(Sfrom);
																	if (accountEsid.equals(from.getString(Sid))) {
																		// user's own comment, clear the notification
																		values.put(Notifications.CLEARED, 1);
																	} else if (cleared) {
																		values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), from.getString(Sname)));
																		values.put(Notifications.CLEARED, 0);
																	} else {
																		values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), from.getString(Sname) + " and others"));
																	}
																	getContentResolver().update(Notifications.CONTENT_URI, values, Notifications._ID + "=?", new String[]{Long.toString(notificationId)});
																}
															}
														}
													} catch (JSONException e) {
														Log.e(TAG, service + ":" + e.toString());
													}
												}
												currentNotifications.moveToNext();
											}
											// check the latest feed
											if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(FACEBOOK_HOME, FACEBOOK_BASE_URL, Saccess_token, token)))) != null) {
												try {
													JSONArray jarr = new JSONObject(response).getJSONArray(Sdata);
													// if there are updates, clear the cache
													int d2 = jarr.length();
													if (d2 > 0) {
														for (int d = 0; d < d2; d++) {
															JSONObject o = jarr.getJSONObject(d);
															String sid = o.getString(Sid);
															// if already notified, ignore
															if (!notificationSids.contains(sid)) {
																// only parse status types, not photo, video or link
																if (o.has(Stype) && o.has(Sfrom)) {
																	JSONObject f = o.getJSONObject(Sfrom);
																	if (f.has(Sname) && f.has(Sid)) {
																		String notification = null;
																		String esid = f.getString(Sid);
																		String friend = f.getString(Sname);
																		if (o.has(Sto)) {
																			// handle wall messages from one friend to another
																			JSONObject t = o.getJSONObject(Sto);
																			if (t.has(Sdata)) {
																				JSONObject n = t.getJSONArray(Sdata).getJSONObject(0);
																				if (n.has(Sname)) {
																					if (n.has(Sid) && (n.getString(Sid).equals(accountEsid))) {
																						notification = String.format(getString(R.string.friendcommented), friend);
																					}
																				}
																			}												
																		}
																		int commentCount = 0;
																		if (o.has(Scomments)) {
																			JSONObject jo = o.getJSONObject(Scomments);
																			if (jo.has(Sdata)) {
																				JSONArray comments = jo.getJSONArray(Sdata);
																				commentCount = comments.length();
																				// notifications
																				if ((sid != null) && (commentCount > 0)) {
																					// default hasCommented to whether or not these comments are for the own user's status
																					boolean hasCommented = notification != null || esid.equals(accountEsid);
																					for (int c2 = 0; c2 < commentCount; c2++) {
																						JSONObject c3 = comments.getJSONObject(c2);
																						if (c3.has(Sfrom)) {
																							JSONObject c4 = c3.getJSONObject(Sfrom);
																							if (c4.getString(Sid).equals(accountEsid)) {
																								if (!hasCommented) {
																									// the user has commented on this thread, notify any updates
																									hasCommented = true;
																								}
																								// clear any notifications, as the user is already aware
																								if (notification != null) {
																									notification = null;
																								}
																							} else if (hasCommented) {
																								// don't notify about user's own comments
																								// send the parent comment sid
																								notification = String.format(getString(R.string.friendcommented), c4.getString(Sname));
																							}
																						}
																					}
																				}
																			}
																		}
																		if (notification != null) {
																			String message = o.has(Smessage) ? o.getString(Smessage) : null;
																			if (!o.getString(Stype).equals(Sstatus) && o.has(Slink)) {
																				message = message == null ? "[" + o.getString(Stype) + "]" : "[" + o.getString(Stype) + "]";
																			}
																			// new notification
																			addNotification(sid, esid, friend, message, o.getLong(Screated_time) * 1000, accountId, notification);
																		}
																	}
																}
															}
														}
													}
												} catch (JSONException e) {
													Log.e(TAG, service + ":" + e.toString());
												}
											}
											break;
										case MYSPACE:
											sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, token, secret);
											// loop over notifications
											while (!currentNotifications.isAfterLast()) {
												long notificationId = currentNotifications.getLong(0);
												String sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
												long updated = currentNotifications.getLong(2);
												boolean cleared = currentNotifications.getInt(3) == 1;
												String esid = mSonetCrypto.Decrypt(currentNotifications.getString(4));
												// store sids, to avoid duplicates when requesting the latest feed
												if (!notificationSids.contains(sid)) {
													notificationSids.add(sid);
												}
												// get comments for current notifications
												if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, esid, sid))))) != null) {
													// check for a newer post, if it's the user's own, then set CLEARED=0
													try {
														JSONArray comments = new JSONObject(response).getJSONArray(Sentry);
														int i2 = comments.length();
														if (i2 > 0) {
															for (int i = 0; i < i2; i++) {
																JSONObject comment = comments.getJSONObject(i);
																long created_time = parseDate(comment.getString(SpostedDate), MYSPACE_DATE_FORMAT);
																if (created_time > updated) {
																	// new comment
																	ContentValues values = new ContentValues();
																	values.put(Notifications.UPDATED, created_time);
																	JSONObject author = comment.getJSONObject(Sauthor);
																	if (accountEsid.equals(author.getString(Sid))) {
																		// user's own comment, clear the notification
																		values.put(Notifications.CLEARED, 1);
																	} else if (cleared) {
																		values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), comment.getString(SdisplayName)));
																		values.put(Notifications.CLEARED, 0);
																	} else {
																		values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), comment.getString(SdisplayName) + " and others"));
																	}
																	getContentResolver().update(Notifications.CONTENT_URI, values, Notifications._ID + "=?", new String[]{Long.toString(notificationId)});
																}
															}
														}
													} catch (JSONException e) {
														Log.e(TAG, service + ":" + e.toString());
													}
												}
												currentNotifications.moveToNext();
											}
											// check the latest feed
											if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_HISTORY, MYSPACE_BASE_URL))))) != null) {
												try {
													JSONArray jarr = new JSONObject(response).getJSONArray(Sentry);
													// if there are updates, clear the cache
													int d2 = jarr.length();
													if (d2 > 0) {
														for (int d = 0; d < d2; d++) {
															JSONObject o = jarr.getJSONObject(d);
															String sid = o.getString(SstatusId);
															// if already notified, ignore
															if (!notificationSids.contains(sid)) {
																if (o.has(Sauthor) && o.has(SrecentComments)) {
																	JSONObject f = o.getJSONObject(Sauthor);
																	if (f.has(SdisplayName) && f.has(Sid)) {
																		String notification = null;
																		String esid = f.getString(Sid);
																		String friend = f.getString(SdisplayName);
																		JSONArray comments = o.getJSONArray(SrecentComments);
																		int commentCount = comments.length();
																		// notifications
																		if ((sid != null) && (commentCount > 0)) {
																			// default hasCommented to whether or not these comments are for the own user's status
																			boolean hasCommented = notification != null || esid.equals(accountEsid);
																			for (int c2 = 0; c2 < commentCount; c2++) {
																				JSONObject c3 = comments.getJSONObject(c2);
																				if (c3.has(Sauthor)) {
																					JSONObject c4 = c3.getJSONObject(Sauthor);
																					if (c4.getString(Sid).equals(accountEsid)) {
																						if (!hasCommented) {
																							// the user has commented on this thread, notify any updates
																							hasCommented = true;
																						}
																						// clear any notifications, as the user is already aware
																						if (notification != null) {
																							notification = null;
																						}
																					} else if (hasCommented) {
																						// don't notify about user's own comments
																						// send the parent comment sid
																						notification = String.format(getString(R.string.friendcommented), c4.getString(SdisplayName));
																					}
																				}
																			}
																		}
																		if (notification != null) {
																			// new notification
																			addNotification(sid, esid, friend, o.getString(Sstatus), parseDate(o.getString("moodStatusLastUpdated"), MYSPACE_DATE_FORMAT), accountId, notification);
																		}
																	}
																}
															}
														}
													}
												} catch (JSONException e) {
													Log.e(TAG, service + ":" + e.toString());
												}
											}
											break;
										case FOURSQUARE:
											// loop over notifications
											while (!currentNotifications.isAfterLast()) {
												long notificationId = currentNotifications.getLong(0);
												String sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
												long updated = currentNotifications.getLong(2);
												boolean cleared = currentNotifications.getInt(3) == 1;
												// store sids, to avoid duplicates when requesting the latest feed
												if (!notificationSids.contains(sid)) {
													notificationSids.add(sid);
												}
												// get comments for current notifications
												if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, sid, token)))) != null) {
													// check for a newer post, if it's the user's own, then set CLEARED=0
													try {
														JSONArray comments = new JSONObject(response).getJSONObject(Sresponse).getJSONObject(Scheckin).getJSONObject(Scomments).getJSONArray(Sitems);
														int i2 = comments.length();
														if (i2 > 0) {
															for (int i = 0; i < i2; i++) {
																JSONObject comment = comments.getJSONObject(i);
																long created_time = comment.getLong(ScreatedAt) * 1000;
																if (created_time > updated) {
																	// new comment
																	ContentValues values = new ContentValues();
																	values.put(Notifications.UPDATED, created_time);
																	JSONObject user = comment.getJSONObject(Suser);
																	if (accountEsid.equals(user.getString(Sid))) {
																		// user's own comment, clear the notification
																		values.put(Notifications.CLEARED, 1);
																	} else if (cleared) {
																		values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), user.getString(SfirstName) + " " + user.getString(SlastName)));
																		values.put(Notifications.CLEARED, 0);
																	} else {
																		values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), user.getString(SfirstName) + " " + user.getString(SlastName) + " and others"));
																	}
																	getContentResolver().update(Notifications.CONTENT_URI, values, Notifications._ID + "=?", new String[]{Long.toString(notificationId)});
																}
															}
														}
													} catch (JSONException e) {
														Log.e(TAG, service + ":" + e.toString());
													}
												}
												currentNotifications.moveToNext();
											}
											// check the latest feed
											if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(FOURSQUARE_CHECKINS, FOURSQUARE_BASE_URL, token)))) != null) {
												try {
													JSONArray jarr = new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Srecent);
													// if there are updates, clear the cache
													int d2 = jarr.length();
													if (d2 > 0) {
														for (int d = 0; d < d2; d++) {
															JSONObject o = jarr.getJSONObject(d);
															String sid = o.getString(Sid);
															// if already notified, ignore
															if (!notificationSids.contains(sid)) {
																if (o.has(Suser) && o.has(Scomments)) {
																	JSONObject f = o.getJSONObject(Suser);
																	if (f.has(SfirstName) && f.has(SlastName) && f.has(Sid)) {
																		String notification = null;
																		String esid = f.getString(Sid);
																		String friend = f.getString(SfirstName) + " " + f.getString(SlastName);
																		JSONArray comments = o.getJSONArray(Scomments);
																		int commentCount = comments.length();
																		// notifications
																		if (commentCount > 0) {
																			// default hasCommented to whether or not these comments are for the own user's status
																			boolean hasCommented = notification != null || esid.equals(accountEsid);
																			for (int c2 = 0; c2 < commentCount; c2++) {
																				JSONObject c3 = comments.getJSONObject(c2);
																				if (c3.has(Suser)) {
																					JSONObject c4 = c3.getJSONObject(Suser);
																					if (c4.getString(Sid).equals(accountEsid)) {
																						if (!hasCommented) {
																							// the user has commented on this thread, notify any updates
																							hasCommented = true;
																						}
																						// clear any notifications, as the user is already aware
																						if (notification != null) {
																							notification = null;
																						}
																					} else if (hasCommented) {
																						// don't notify about user's own comments
																						// send the parent comment sid
																						notification = String.format(getString(R.string.friendcommented), c4.getString(SfirstName) + " " + c4.getString(SlastName));
																					}
																				}
																			}
																		}
																		if (notification != null) {
																			String message = "";
																			if (o.has(Sshout)) {
																				message = o.getString(Sshout) + "\n";
																			}
																			if (o.has(Svenue)) {
																				JSONObject venue = o.getJSONObject(Svenue);
																				if (venue.has(Sname)) {
																					message += "@" + venue.getString(Sname);																
																				}
																			}
																			// new notification
																			addNotification(sid, esid, friend, message, o.getLong(ScreatedAt) * 1000, accountId, notification);
																		}
																	}
																}
															}
														}
													}
												} catch (JSONException e) {
													Log.e(TAG, service + ":" + e.toString());
												}
											}
											break;
										case LINKEDIN:
											sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, token, secret);
											// loop over notifications
											while (!currentNotifications.isAfterLast()) {
												long notificationId = currentNotifications.getLong(0);
												String sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
												long updated = currentNotifications.getLong(2);
												boolean cleared = currentNotifications.getInt(3) == 1;
												// store sids, to avoid duplicates when requesting the latest feed
												if (!notificationSids.contains(sid)) {
													notificationSids.add(sid);
												}
												// get comments for current notifications
												HttpGet httpGet = new HttpGet(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, sid));
												for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
												if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(httpGet))) != null) {
													// check for a newer post, if it's the user's own, then set CLEARED=0
													try {
														JSONObject jsonResponse = new JSONObject(response);
														if (jsonResponse.has(S_total) && (jsonResponse.getInt(S_total) != 0)) {
															JSONArray comments = jsonResponse.getJSONArray(Svalues);
															int i2 = comments.length();
															if (i2 > 0) {
																for (int i = 0; i < i2; i++) {
																	JSONObject comment = comments.getJSONObject(i);
																	long created_time = comment.getLong(Stimestamp);
																	if (created_time > updated) {
																		// new comment
																		ContentValues values = new ContentValues();
																		values.put(Notifications.UPDATED, created_time);
																		JSONObject person = comment.getJSONObject(Sperson);
																		if (accountEsid.equals(person.getString(Sid))) {
																			// user's own comment, clear the notification
																			values.put(Notifications.CLEARED, 1);
																		} else if (cleared) {
																			values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), person.getString(SfirstName) + " " + person.getString(SlastName)));
																			values.put(Notifications.CLEARED, 0);
																		} else {
																			values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), person.getString(SfirstName) + " " + person.getString(SlastName) + " and others"));
																		}
																		getContentResolver().update(Notifications.CONTENT_URI, values, Notifications._ID + "=?", new String[]{Long.toString(notificationId)});
																	}
																}
															}
														}
													} catch (JSONException e) {
														Log.e(TAG, service + ":" + e.toString());
													}
												}
												currentNotifications.moveToNext();
											}
											// check the latest feed
											HttpGet httpGet = new HttpGet(String.format(LINKEDIN_UPDATES, LINKEDIN_BASE_URL));
											for (String[] header : LINKEDIN_HEADERS) {
												httpGet.setHeader(header[0], header[1]);
											}
											if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(httpGet))) != null) {
												try {
													JSONArray jarr = new JSONObject(response).getJSONArray(Svalues);
													// if there are updates, clear the cache
													int d2 = jarr.length();
													if (d2 > 0) {
														HashMap<String, String> LINKEDIN_UPDATETYPES = new HashMap<String, String>();
														LINKEDIN_UPDATETYPES.put(SANSW, "updated an answer");
														LINKEDIN_UPDATETYPES.put(SAPPS, "updated the application ");
														LINKEDIN_UPDATETYPES.put(SCMPY, "company update");
														LINKEDIN_UPDATETYPES.put(SCONN, "is now connected to ");
														LINKEDIN_UPDATETYPES.put(SJOBP, "posted the job ");
														LINKEDIN_UPDATETYPES.put(SJGRP, "joined the group ");
														LINKEDIN_UPDATETYPES.put(SPRFX, "updated their extended profile");
														LINKEDIN_UPDATETYPES.put(SPREC, "recommends ");
														LINKEDIN_UPDATETYPES.put(SPROF, "changed their profile");
														LINKEDIN_UPDATETYPES.put(SQSTN, "updated a question");
														LINKEDIN_UPDATETYPES.put(SSHAR, "shared something");
														LINKEDIN_UPDATETYPES.put(SVIRL, "updated the viral ");
														LINKEDIN_UPDATETYPES.put(SPICU, "updated their profile picture");
														for (int d = 0; d < d2; d++) {
															JSONObject o = jarr.getJSONObject(d);
															String sid = o.getString(SupdateKey);
															// if already notified, ignore
															if (!notificationSids.contains(sid)) {
																String updateType = o.getString(SupdateType);
																JSONObject updateContent = o.getJSONObject(SupdateContent);
																if (LINKEDIN_UPDATETYPES.containsKey(updateType) && updateContent.has(Sperson)) {
																	JSONObject f = updateContent.getJSONObject(Sperson);
																	if (f.has(SfirstName) && f.has(SlastName) && f.has(Sid) && o.has(SupdateComments)) {
																		JSONObject updateComments = o.getJSONObject(SupdateComments);
																		if (updateComments.has(Svalues)) {
																			String notification = null;
																			String esid = f.getString(Sid);
																			JSONArray comments = updateComments.getJSONArray(Svalues);
																			int commentCount = comments.length();
																			// notifications
																			if (commentCount > 0) {
																				// default hasCommented to whether or not these comments are for the own user's status
																				boolean hasCommented = notification != null || esid.equals(accountEsid);
																				for (int c2 = 0; c2 < commentCount; c2++) {
																					JSONObject c3 = comments.getJSONObject(c2);
																					if (c3.has(Sperson)) {
																						JSONObject c4 = c3.getJSONObject(Sperson);
																						if (c4.getString(Sid).equals(accountEsid)) {
																							if (!hasCommented) {
																								// the user has commented on this thread, notify any updates
																								hasCommented = true;
																							}
																							// clear any notifications, as the user is already aware
																							if (notification != null) {
																								notification = null;
																							}
																						} else if (hasCommented) {
																							// don't notify about user's own comments
																							// send the parent comment sid
																							notification = String.format(getString(R.string.friendcommented), c4.getString(SfirstName) + " " + c4.getString(SlastName));
																						}
																					}
																				}
																			}
																			if (notification != null) {
																				String update = LINKEDIN_UPDATETYPES.get(updateType);
																				if (updateType.equals(SAPPS)) {
																					if (f.has(SpersonActivities)) {
																						JSONObject personActivities = f.getJSONObject(SpersonActivities);
																						if (personActivities.has(Svalues)) {
																							JSONArray updates = personActivities.getJSONArray(Svalues);
																							for (int u = 0, u2 = updates.length(); u < u2; u++) {
																								update += updates.getJSONObject(u).getString(Sbody);
																								if (u < (updates.length() - 1)) update += ", ";
																							}
																						}
																					}
																				} else if (updateType.equals(SCONN)) {
																					if (f.has(Sconnections)) {
																						JSONObject connections = f.getJSONObject(Sconnections);
																						if (connections.has(Svalues)) {
																							JSONArray updates = connections.getJSONArray(Svalues);
																							for (int u = 0, u2 = updates.length(); u < u2; u++) {
																								update += updates.getJSONObject(u).getString(SfirstName) + " " + updates.getJSONObject(u).getString(SlastName);
																								if (u < (updates.length() - 1)) update += ", ";
																							}
																						}
																					}
																				} else if (updateType.equals(SJOBP)) {
																					if (updateContent.has(Sjob) && updateContent.getJSONObject(Sjob).has(Sposition) && updateContent.getJSONObject(Sjob).getJSONObject(Sposition).has(Stitle)) update += updateContent.getJSONObject(Sjob).getJSONObject(Sposition).getString(Stitle);
																				} else if (updateType.equals(SJGRP)) {
																					if (f.has(SmemberGroups)) {
																						JSONObject memberGroups = f.getJSONObject(SmemberGroups);
																						if (memberGroups.has(Svalues)) {
																							JSONArray updates = memberGroups.getJSONArray(Svalues);
																							for (int u = 0, u2 = updates.length(); u < u2; u++) {
																								update += updates.getJSONObject(u).getString(Sname);
																								if (u < (updates.length() - 1)) update += ", ";
																							}
																						}
																					}
																				} else if (updateType.equals(SPREC)) {
																					if (f.has(SrecommendationsGiven)) {
																						JSONObject recommendationsGiven = f.getJSONObject(SrecommendationsGiven);
																						if (recommendationsGiven.has(Svalues)) {
																							JSONArray updates = recommendationsGiven.getJSONArray(Svalues);
																							for (int u = 0, u2 = updates.length(); u < u2; u++) {
																								JSONObject recommendation = updates.getJSONObject(u);
																								JSONObject recommendee = recommendation.getJSONObject(Srecommendee);
																								if (recommendee.has(SfirstName)) update += recommendee.getString(SfirstName);
																								if (recommendee.has(SlastName)) update += recommendee.getString(SlastName);
																								if (recommendation.has(SrecommendationSnippet)) update += ":" + recommendation.getString(SrecommendationSnippet);
																								if (u < (updates.length() - 1)) update += ", ";
																							}
																						}
																					}
																				} else if (updateType.equals(SSHAR) && f.has(ScurrentShare)) {
																					JSONObject currentShare = f.getJSONObject(ScurrentShare);
																					if (currentShare.has(Scomment)) {
																						update = currentShare.getString(Scomment);
																					}
																				}
																				// new notification
																				addNotification(sid, esid, f.getString(SfirstName) + " " + f.getString(SlastName), update, o.getLong(Stimestamp), accountId, notification);
																			}
																		}
																	}
																}
															}
														}
													}
												} catch (JSONException e) {
													Log.e(TAG, service + ":" + e.toString());
												}
											}
											break;
										case GOOGLEPLUS:
											// get new access token, need different request here
											HttpPost httpPost = new HttpPost(GOOGLE_ACCESS);
											List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
											httpParams.add(new BasicNameValuePair("client_id", GOOGLE_CLIENTID));
											httpParams.add(new BasicNameValuePair("client_secret", GOOGLE_CLIENTSECRET));
											httpParams.add(new BasicNameValuePair("refresh_token", token));
											httpParams.add(new BasicNameValuePair("grant_type", "refresh_token"));
											try {
												httpPost.setEntity(new UrlEncodedFormEntity(httpParams));
												if ((response = SonetHttpClient.httpResponse(httpClient, httpPost)) != null) {
													JSONObject j = new JSONObject(response);
													if (j.has(Saccess_token)) {
														String access_token = j.getString(Saccess_token);
														while (!currentNotifications.isAfterLast()) {
															long notificationId = currentNotifications.getLong(0);
															String sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
															long updated = currentNotifications.getLong(2);
															boolean cleared = currentNotifications.getInt(3) == 1;
															// store sids, to avoid duplicates when requesting the latest feed
															if (!notificationSids.contains(sid)) {
																notificationSids.add(sid);
															}
															// get comments for current notifications
															if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(GOOGLEPLUS_ACTIVITY, GOOGLEPLUS_BASE_URL, sid, access_token)))) != null) {
																// check for a newer post, if it's the user's own, then set CLEARED=0
																try {
																	JSONObject item = new JSONObject(response);
																	if (item.has(Sobject)) {
																		JSONObject object = item.getJSONObject(Sobject);
																		if (object.has(Sreplies)) {
																			int commentCount = 0;
																			JSONObject replies = object.getJSONObject(Sreplies);
																			if (replies.has(StotalItems)) {
																				//TODO: notifications
																			}
																		}
																	}
																} catch (JSONException e) {
																	Log.e(TAG, service + ":" + e.toString());
																}
															}
															currentNotifications.moveToNext();
														}
														// get new feed
														if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(GOOGLEPLUS_ACTIVITIES, GOOGLEPLUS_BASE_URL, "me", "public", 20, access_token)))) != null) {
															JSONObject r = new JSONObject(response);
															if (r.has(Sitems)) {
																JSONArray items = r.getJSONArray(Sitems);
																for (int i1 = 0, i2 = items.length(); i1 < i2; i1++) {
																	JSONObject item = items.getJSONObject(i1);
																	if (item.has(Sactor) && item.has(Sobject)) {
																		JSONObject actor = item.getJSONObject(Sactor);
																		JSONObject object = item.getJSONObject(Sobject);
																		if (item.has(Sid) && actor.has(Sid) && actor.has(SdisplayName) && item.has(Spublished) && object.has(Sreplies) && object.has(SoriginalContent)) {
																			String sid = item.getString(Sid);
																			String esid = actor.getString(Sid);
																			String friend = actor.getString(SdisplayName);
																			String originalContent = object.getString(SoriginalContent);
																			if ((originalContent == null) || (originalContent.length() == 0)) {
																				originalContent = object.getString(Scontent);
																			}
																			String photo = null;
																			if (actor.has(Simage)) {
																				JSONObject image = actor.getJSONObject(Simage);
																				if (image.has(Surl)) {
																					photo = image.getString(Surl);
																				}
																			}
																			long date = parseDate(item.getString(Spublished), GOOGLEPLUS_DATE_FORMAT);
																			int commentCount = 0;
																			JSONObject replies = object.getJSONObject(Sreplies);
																			String notification = null;
																			if (replies.has(StotalItems)) {
																				Log.d(TAG,Sreplies+":"+replies.toString());
																				commentCount = replies.getInt(StotalItems);
																			}
																			if (notification != null) {
																				// new notification
																				addNotification(sid, esid, friend, originalContent, date, accountId, notification);
																			}
																		}
																	}
																}
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
									}
									currentNotifications.close();
								}
								// remove old notifications
								getContentResolver().delete(Notifications.CONTENT_URI, Notifications.CLEARED + "=1 and " + Notifications.ACCOUNT + "=? and " + Notifications.CREATED + "<?", new String[]{Long.toString(accountId), Long.toString(System.currentTimeMillis() - 86400000)});
							}
							account.close();
							widgets.moveToNext();
						}
					} else {
						publishProgress("No notifications have been set up on any accounts.");
					}
					widgets.close();
					return false;
				case CLEAR_ALL:
					// clear all notifications
					ContentValues values = new ContentValues();
					values.put(Notifications.CLEARED, 1);
					SonetNotifications.this.getContentResolver().update(Notifications.CONTENT_URI, values, null, null);
					return true;
				}
				return false;
			}

			@Override
			protected void onProgressUpdate(String... messages) {
				(Toast.makeText(SonetNotifications.this, messages[0], Toast.LENGTH_LONG)).show();
			}

			@Override
			protected void onPostExecute(Boolean finish) {
				if (loadingDialog.isShowing()) {
					loadingDialog.dismiss();
				}
				if (finish) {
					SonetNotifications.this.finish();
				}
			}

			private void addNotification(String sid, String esid, String friend, String message, long created, long accountId, String notification) {
				ContentValues values = new ContentValues();
				values.put(Notifications.SID, sid);
				values.put(Notifications.ESID, esid);
				values.put(Notifications.FRIEND, friend);
				values.put(Notifications.MESSAGE, message);
				values.put(Notifications.CREATED, created);
				values.put(Notifications.ACCOUNT, accountId);
				values.put(Notifications.NOTIFICATION, notification);
				values.put(Notifications.CLEARED, 0);
				values.put(Notifications.UPDATED, created);
				getContentResolver().insert(Notifications.CONTENT_URI, values);
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
		asyncTask.execute(item.getItemId());
		return true;
		//		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// cancel any notifications
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Sonet.NOTIFY_ID);
		loadNotifications();
	}

	private final SimpleCursorAdapter.ViewBinder mViewBinder = new SimpleCursorAdapter.ViewBinder() {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (columnIndex == cursor.getColumnIndex(Notifications.CLEARED)) {
				view.setEnabled(cursor.getInt(columnIndex) != 1);
				return true;
			} else {
				return false;
			}
		}
	};

	private void loadNotifications() {
		Cursor c = this.managedQuery(Notifications.CONTENT_URI, new String[]{Notifications._ID, Notifications.CLEARED, Notifications.NOTIFICATION}, Notifications.CLEARED + "!=1", null, null);
		SimpleCursorAdapter sca = new SimpleCursorAdapter(this, R.layout.notifications_row, c, new String[] {Notifications.CLEARED, Notifications.NOTIFICATION}, new int[] {R.id.notification, R.id.notification});
		sca.setViewBinder(mViewBinder);
		setListAdapter(sca);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// don't finish the activity, in case there are other notifications to view
//		if ((requestCode == RESULT_REFRESH) && (resultCode == RESULT_OK)) {
//			finish();
//		}
	}

}
