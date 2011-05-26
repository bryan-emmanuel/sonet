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

import static com.piusvelte.sonet.Sonet.TOKEN;

import static com.piusvelte.sonet.Sonet.EXTRA_SCROLLABLE_VERSION;
import static com.piusvelte.sonet.Sonet.BUZZ_BASE_URL;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_ME;
import static com.piusvelte.sonet.Sonet.BUZZ_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.FACEBOOK_URL_ME;
import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_BASE_URL;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_URL_ME;
import static com.piusvelte.sonet.Sonet.LINKEDIN_BASE_URL;
import static com.piusvelte.sonet.Sonet.LINKEDIN_URL_ME;
import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_ME;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.SID_FORMAT;

import static com.piusvelte.sonet.SonetTokens.TWITTER_KEY;
import static com.piusvelte.sonet.SonetTokens.TWITTER_SECRET;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_KEY;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_SECRET;

import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.SonetTokens.BUZZ_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_SECRET;
import static com.piusvelte.sonet.SonetTokens.BUZZ_API_KEY;

import static com.piusvelte.sonet.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_FEED;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_FEED;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_FEED;

//import static com.piusvelte.sonet.Sonet.SALESFORCE;
//import static com.piusvelte.sonet.Tokens.SALESFORCE_KEY;
//import static com.piusvelte.sonet.Tokens.SALESFORCE_SECRET;
//import static com.piusvelte.sonet.Sonet.SALESFORCE_FEED;

import static com.piusvelte.sonet.Sonet.FACEBOOK_URL_FEED;

import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_URL_FEED;

import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_SECRET;
import static com.piusvelte.sonet.Sonet.LINKEDIN_HEADERS;
import static com.piusvelte.sonet.Sonet.LINKEDIN_URL_FEED;
import static com.piusvelte.sonet.Sonet.LINKEDIN_UPDATETYPES;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.BoundRemoteViews;

import org.apache.http.client.methods.HttpGet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class SonetService extends Service {
	private static final String TAG = "SonetService";
	private static int[] map_icons = new int[]{R.drawable.twitter, R.drawable.facebook, R.drawable.myspace, R.drawable.buzz, R.drawable.foursquare, R.drawable.linkedin, R.drawable.salesforce};
	private static HashMap<String, ArrayList<AsyncTask<String, Void, String>>> sWidgetsTasks = new HashMap<String, ArrayList<AsyncTask<String, Void, String>>>();
	private AlarmManager mAlarmManager;
	private ConnectivityManager mConnectivityManager;

	@Override
	public void onCreate() {
		super.onCreate();
		mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent != null) {
			if (intent.getAction() != null) {
				if (intent.getAction().equals(ACTION_REFRESH)) {
					if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
						SonetService.updateWidgets(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
					}
				}
			} else if (intent.getData() != null) {
				SonetService.updateWidgets(new String[] {intent.getData().getLastPathSegment()});
			} else if (intent.hasExtra(EXTRA_SCROLLABLE_VERSION)) {
				int scrollableVersion = intent.getIntExtra(EXTRA_SCROLLABLE_VERSION, 1);
				int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
				ContentValues values = new ContentValues();
				values.put(Widgets.SCROLLABLE, scrollableVersion);
				// set the scrollable version
				this.getContentResolver().update(Widgets.CONTENT_URI, values, Widgets.WIDGET + "=?", new String[] {Integer.toString(appWidgetId)});
				buildScrollable(appWidgetId, scrollableVersion);				
			} else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
				SonetService.updateWidgets(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
			} else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
				SonetService.updateWidgets(new String[]{Integer.toString(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))});
			}
		}
		synchronized (sLock) {
			while (updatesQueued()) {
				// first handle deletes, then scroll updates, finally regular updates
				String appWidgetId = getNextUpdate();
				boolean backgroundUpdate = true;
				int refreshInterval = Sonet.default_interval;
				ArrayList<AsyncTask<String, Void, String>> statusesTasks = new ArrayList<AsyncTask<String, Void, String>>();
				SonetService.sWidgetsTasks.put(appWidgetId, statusesTasks);
				mAlarmManager.cancel(PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setAction(appWidgetId), 0));
				Cursor settings = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.INTERVAL, Widgets.BACKGROUND_UPDATE}, Widgets.WIDGET + "=?", new String[]{appWidgetId}, null);
				if (settings.moveToFirst()) {
					refreshInterval = settings.getInt(settings.getColumnIndex(Widgets.INTERVAL));
					backgroundUpdate = settings.getInt(settings.getColumnIndex(Widgets.BACKGROUND_UPDATE)) == 1;
				} else {
					// upgrade, moving settings from sharedpreferences to db, or initialize settings
					SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
					ContentValues values = new ContentValues();
					values.put(Widgets.INTERVAL, Integer.parseInt((String) sp.getString(getString(R.string.key_interval), Integer.toString(Sonet.default_interval))));
					values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
					values.put(Widgets.HASBUTTONS, sp.getBoolean(getString(R.string.key_display_buttons), false) ? 1 : 0);
					values.put(Widgets.BUTTONS_BG_COLOR, Integer.parseInt(sp.getString(getString(R.string.key_head_background), Integer.toString(Sonet.default_buttons_bg_color))));
					values.put(Widgets.BUTTONS_COLOR, Integer.parseInt(sp.getString(getString(R.string.key_head_text), Integer.toString(Sonet.default_buttons_color))));
					values.put(Widgets.BUTTONS_TEXTSIZE, Integer.parseInt(sp.getString(getString(R.string.key_buttons_textsize), Integer.toString(Sonet.default_buttons_textsize))));
					values.put(Widgets.WIDGET, appWidgetId);
					values.put(Widgets.ICON, 1);
					values.put(Widgets.MESSAGES_BG_COLOR, Sonet.default_message_bg_color);
					values.put(Widgets.MESSAGES_COLOR, Sonet.default_message_color);
					values.put(Widgets.MESSAGES_TEXTSIZE, Sonet.default_messages_textsize);
					values.put(Widgets.FRIEND_COLOR, Sonet.default_friend_color);
					values.put(Widgets.FRIEND_TEXTSIZE, Sonet.default_friend_textsize);
					values.put(Widgets.CREATED_COLOR, Sonet.default_created_color);
					values.put(Widgets.CREATED_TEXTSIZE, Sonet.default_created_textsize);
					values.put(Widgets.TIME24HR, 0);
					values.put(Widgets.STATUSES_PER_ACCOUNT, Sonet.default_statuses_per_account);
					values.put(Widgets.BACKGROUND_UPDATE, 1);
					values.put(Widgets.SCROLLABLE, 0);
					this.getContentResolver().insert(Widgets.CONTENT_URI, values);
				}
				settings.close();
				Cursor account_updates = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE}, Accounts.SID + " is null or " + Accounts.SID + "=\"\"", null, null);
				if (account_updates.moveToFirst()) {
					int iid = account_updates.getColumnIndex(Accounts._ID),
					itoken = account_updates.getColumnIndex(Accounts.TOKEN),
					isecret = account_updates.getColumnIndex(Accounts.SECRET),
					iservice = account_updates.getColumnIndex(Accounts.SERVICE);
					while (!account_updates.isAfterLast()) {
						int service = account_updates.getInt(iservice);
						SonetOAuth sonetOAuth;
						String response;
						switch (service) {
						case TWITTER:
							sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, account_updates.getString(itoken), account_updates.getString(isecret));
							try {
								response = sonetOAuth.httpResponse(new HttpGet("http://api.twitter.com/1/account/verify_credentials.json"));
								if (response != null) {
									JSONObject jobj = new JSONObject(response);
									updateAccount(Integer.toString(account_updates.getInt(iid)), jobj.getString("id"));
								}
							} catch (JSONException e) {
								Log.e(TAG, service + ":" + e.toString());
							}
							break;
						case FACEBOOK:
							response = Sonet.httpResponse(new HttpGet(String.format(FACEBOOK_URL_ME, FACEBOOK_BASE_URL, TOKEN, account_updates.getString(itoken))));
							if (response != null) {
								try {
									JSONObject jobj = new JSONObject(response);
									updateAccount(Integer.toString(account_updates.getInt(iid)), jobj.getString("id"));
								} catch (JSONException e) {
									Log.e(TAG, service + ":" + e.toString());
								}
							}
							break;
						case MYSPACE:
							sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, account_updates.getString(itoken), account_updates.getString(isecret));
							try {
								response = sonetOAuth.httpResponse(new HttpGet(String.format(MYSPACE_URL_ME, MYSPACE_BASE_URL)));
								if (response != null) {
									JSONObject jobj = (new JSONObject(response)).getJSONObject("person");
									updateAccount(Integer.toString(account_updates.getInt(iid)), jobj.getString("id"));
								}
							} catch (JSONException e) {
								Log.e(TAG, service + ":" + e.toString());
							}
							break;
						case BUZZ:
							sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, account_updates.getString(itoken), account_updates.getString(isecret));
							try {
								response = sonetOAuth.httpResponse(new HttpGet(String.format(BUZZ_URL_ME, BUZZ_BASE_URL, BUZZ_API_KEY)));
								if (response != null) {
									JSONObject jobj = (new JSONObject(response)).getJSONObject("data");
									updateAccount(Integer.toString(account_updates.getInt(iid)), jobj.getString("id"));
								}
							} catch (JSONException e) {
								Log.e(TAG, service + ":" + e.toString());
							}
							break;
						case FOURSQUARE:
							response = Sonet.httpResponse(new HttpGet(String.format(FOURSQUARE_URL_ME, FOURSQUARE_BASE_URL, account_updates.getString(itoken))));
							if (response != null) {
								try {
									JSONObject jobj = (new JSONObject(response)).getJSONObject("response").getJSONObject("user");
									updateAccount(Integer.toString(account_updates.getInt(iid)), jobj.getString("id"));
								} catch (JSONException e) {
									Log.e(TAG, service + ":" + e.toString());
									Log.e(TAG, response);
								}
							}
							break;
						case LINKEDIN:
							sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, account_updates.getString(itoken), account_updates.getString(isecret));
							try {
								HttpGet httpGet = new HttpGet(String.format(LINKEDIN_URL_ME, LINKEDIN_BASE_URL));
								for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
								response = sonetOAuth.httpResponse(httpGet);
								if (response != null) {
									JSONObject jobj = new JSONObject(response);
									updateAccount(Integer.toString(account_updates.getInt(iid)), jobj.getString("id"));
								}
							} catch (JSONException e) {
								Log.e(TAG, service + ":" + e.toString());
							}
							break;
						}
						account_updates.moveToNext();
					}
				}
				account_updates.close();
				// query accounts
				/* get statuses for all accounts
				 * then sort them by datetime, descending
				 */
				Cursor accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.USERNAME, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE, Accounts.EXPIRY}, Accounts.WIDGET + "=?", new String[]{appWidgetId}, null);
				if (!accounts.moveToFirst()) {
					// check for old accounts without appwidgetid
					accounts.close();
					accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.USERNAME, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE, Accounts.EXPIRY}, Accounts.WIDGET + "=?", new String[]{""}, null);
					if (accounts.moveToFirst()) {
						// upgrade the accounts, adding the appwidgetid
						int username = accounts.getColumnIndex(Accounts.USERNAME),
						token = accounts.getColumnIndex(Accounts.TOKEN),
						secret = accounts.getColumnIndex(Accounts.SECRET),
						service = accounts.getColumnIndex(Accounts.SERVICE),
						expiry = accounts.getColumnIndex(Accounts.EXPIRY);
						while (!accounts.isAfterLast()) {
							ContentValues values = new ContentValues();
							values.put(Accounts.USERNAME, accounts.getString(username));
							values.put(Accounts.TOKEN, accounts.getString(token));
							values.put(Accounts.SECRET, accounts.getString(secret));
							values.put(Accounts.SERVICE, accounts.getInt(service));
							values.put(Accounts.EXPIRY, accounts.getInt(expiry));
							values.put(Accounts.WIDGET, appWidgetId);
							values.put(Accounts.SID, "");
							this.getContentResolver().insert(Accounts.CONTENT_URI, values);
							accounts.moveToNext();
						}
					}
					accounts.close();
					this.getContentResolver().delete(Accounts.CONTENT_URI, Accounts._ID + "=?", new String[]{""});
					accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.USERNAME, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE, Accounts.EXPIRY}, Accounts.WIDGET + "=?", new String[]{appWidgetId}, null);
				}
				if (accounts.moveToFirst()) {
					// load the updates
					int iaccountid = accounts.getColumnIndex(Accounts._ID),
					iservice = accounts.getColumnIndex(Accounts.SERVICE),
					itoken = accounts.getColumnIndex(Accounts.TOKEN),
					isecret = accounts.getColumnIndex(Accounts.SECRET);
					while (!accounts.isAfterLast()) {
						String account = Long.toString(accounts.getLong(iaccountid)),
						service = Integer.toString(accounts.getInt(iservice));
						// if no connection, only update the status_bg and icons
						if ((mConnectivityManager.getActiveNetworkInfo() != null) && mConnectivityManager.getActiveNetworkInfo().isConnected()) {
							AsyncTask<String, Void, String> task;
							switch (Integer.parseInt(service)) {
							case TWITTER:
								task = new AsyncTask<String, Void, String>() {
									private String widget;
									private String account;
									private String service;
									private boolean time24hr,
									icon;
									private int status_bg_color = -1,
									status_count;

									@Override
									protected String doInBackground(String... params) {
										// get this account's statuses
										// get the settings form time24hr and bg_color
										this.widget = params[0];
										this.account = params[1];
										this.service = params[2];
										Cursor c = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, account}, null);
										if (c.moveToFirst()) {
											time24hr = c.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
											status_bg_color = c.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
											icon = c.getInt(c.getColumnIndex(Widgets.ICON)) == 1;
											status_count = c.getInt(c.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
										} else {
											Cursor d = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
											if (d.moveToFirst()) {
												time24hr = d.getInt(d.getColumnIndex(Widgets.TIME24HR)) == 1;
												status_bg_color = d.getInt(d.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
												icon = d.getInt(d.getColumnIndex(Widgets.ICON)) == 1;
												status_count = d.getInt(d.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
											} else {
												Cursor e = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
												if (e.moveToFirst()) {
													time24hr = e.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
													status_bg_color = e.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
													icon = e.getInt(e.getColumnIndex(Widgets.ICON)) == 1;
													status_count = e.getInt(e.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
												} else {
													time24hr = false;
													status_bg_color = Sonet.default_message_bg_color;
													icon = true;
													status_count = Sonet.default_statuses_per_account;
												}
												e.close();
											}
											d.close();
										}
										c.close();
										SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, params[3], params[4]);
										return sonetOAuth.httpResponse(new HttpGet(String.format(TWITTER_URL_FEED, TWITTER_BASE_URL, status_count)));
									}

									@Override
									protected void onPostExecute(String response) {
										// parse the response
										boolean updateCreatedText = false;
										if (response != null) {
											String id = "id";
											// create the status_bg
											Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
											Canvas status_bg_canvas = new Canvas(status_bg_bmp);
											status_bg_canvas.drawColor(status_bg_color);
											ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
											status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
											byte[] status_bg = status_bg_blob.toByteArray();
											// if not a full_refresh, only update the status_bg and icons
											try {
												JSONArray entries = new JSONArray(response);
												// if there are updates, clear the cache
												if (entries.length() > 0) {
													SonetService.this.getContentResolver().delete(Entities.CONTENT_URI, Entities.ACCOUNT + "=?", new String[]{account});
													SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													for (int e = 0; e < entries.length(); e++) {
														JSONObject entry = entries.getJSONObject(e);
														JSONObject user = entry.getJSONObject("user");
														addStatusItem(Sonet.parseDate(entry.getString("created_at"), "EEE MMM dd HH:mm:ss z yyyy"),
																user.getString("name"),
																user.getString("profile_image_url"),
																entry.getString("text"),
																service,
																time24hr,
																widget,
																account,
																entry.getString(id),
																user.getString(id));
													}
												} else {
													updateCreatedText = true;
												}
											} catch (JSONException e) {
												Log.e(TAG, service + ":" + e.toString());
											}
											// update the bg and icon
											ContentValues values = new ContentValues();
											values.put(Statuses.STATUS_BG, status_bg);
											values.put(Statuses.ICON, icon ? getBlob(BitmapFactory.decodeResource(getResources(), map_icons[Integer.parseInt(service)])) : null);
											SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
										} else {
											updateCreatedText = true;
										}
										if (updateCreatedText) {
											Cursor statuses = SonetService.this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.CREATED},Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account}, null);
											if (statuses.moveToFirst()) {
												int icreated = statuses.getColumnIndex(Statuses.CREATED);
												while (!statuses.isAfterLast()) {
													ContentValues values = new ContentValues();
													values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(icreated), time24hr));
													SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													statuses.moveToNext();
												}
											}
											statuses.close();
										}
										// remove self from queue
										if (!SonetService.sWidgetsTasks.isEmpty() && SonetService.sWidgetsTasks.containsKey(widget)) {
											ArrayList<AsyncTask<String, Void, String>> tasks = SonetService.sWidgetsTasks.get(widget);
											if (tasks != null) {
												SonetService.sWidgetsTasks.get(widget).remove(this);
												if (tasks.isEmpty()) {
													SonetService.sWidgetsTasks.remove(tasks);
												}
											}				
										}
										// see if the tasks are finished
										checkWidgetUpdateReady(widget);
									}
								};
								statusesTasks.add(task);
								task.execute(appWidgetId, account, service, accounts.getString(itoken), accounts.getString(isecret));
								break;
							case FACEBOOK:
								task = new AsyncTask<String, Void, String>() {
									private String widget;
									private String account;
									private String service;
									private boolean time24hr,
									icon;
									private int status_bg_color = -1,
									status_count;

									@Override
									protected String doInBackground(String... params) {
										// get this account's statuses
										// get the settings form time24hr and bg_color
										this.widget = params[0];
										this.account = params[1];
										this.service = params[2];
										Cursor c = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, account}, null);
										if (c.moveToFirst()) {
											time24hr = c.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
											status_bg_color = c.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
											icon = c.getInt(c.getColumnIndex(Widgets.ICON)) == 1;
											status_count = c.getInt(c.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
										} else {
											Cursor d = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
											if (d.moveToFirst()) {
												time24hr = d.getInt(d.getColumnIndex(Widgets.TIME24HR)) == 1;
												status_bg_color = d.getInt(d.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
												icon = d.getInt(d.getColumnIndex(Widgets.ICON)) == 1;
												status_count = d.getInt(d.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
											} else {
												Cursor e = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
												if (e.moveToFirst()) {
													time24hr = e.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
													status_bg_color = e.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
													icon = e.getInt(e.getColumnIndex(Widgets.ICON)) == 1;
													status_count = e.getInt(e.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
												} else {
													time24hr = false;
													status_bg_color = Sonet.default_message_bg_color;
													icon = true;
													status_count = Sonet.default_statuses_per_account;
												}
												e.close();
											}
											d.close();
										}
										c.close();
										return Sonet.httpResponse(new HttpGet(String.format(FACEBOOK_URL_FEED, FACEBOOK_BASE_URL, status_count, TOKEN, params[3])));
									}

									@Override
									protected void onPostExecute(String response) {
										// parse the response
										boolean updateCreatedText = false;
										if (response != null) {
											String name = "name",
											id = "id";
											// create the status_bg
											Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
											Canvas status_bg_canvas = new Canvas(status_bg_bmp);
											status_bg_canvas.drawColor(status_bg_color);
											ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
											status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
											byte[] status_bg = status_bg_blob.toByteArray();
											// if not a full_refresh, only update the status_bg and icons
											String created_time = "created_time",
											from = "from",
											type = "type",
											profile = "http://graph.facebook.com/%s/picture",
											message = "message",
											data = "data",
											to = "to";
											try {
												JSONObject jobj = new JSONObject(response);
												JSONArray jarr = jobj.getJSONArray(data);
												// if there are updates, clear the cache
												if (jarr.length() > 0) {
													SonetService.this.getContentResolver().delete(Entities.CONTENT_URI, Entities.ACCOUNT + "=?", new String[]{account});
													SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													for (int d = 0; d < jarr.length(); d++) {
														JSONObject o = jarr.getJSONObject(d);
														// only parse status types, not photo, video or link
														if (o.has(type) && o.has(from) && o.has(message) && o.has(id)) {
															JSONObject f = o.getJSONObject(from);
															if (f.has(name) && f.has(id)) {
																String friend = f.getString(name);
																if (o.has(to)) {
																	// handle wall messages from one friend to another
																	JSONObject t = o.getJSONObject(to);
																	if (t.has(data)) {
																		JSONObject n = t.getJSONArray(data).getJSONObject(0);
																		if (n.has(name)) {
																			friend += " > " + n.getString(name);
																		}
																	}												
																}
																String esid = f.getString(id);
																addStatusItem(
																		o.getLong(created_time) * 1000,
																		friend,
																		String.format(profile, esid),
																		o.getString(message),
																		service,
																		time24hr,
																		widget,
																		account,
																		o.getString(id),
																		esid);
															}
														}
													}
												} else updateCreatedText = true;
											} catch (JSONException e) {
												Log.e(TAG, service + ":" + e.toString());
											}
											// update the bg and icon
											ContentValues values = new ContentValues();
											values.put(Statuses.STATUS_BG, status_bg);
											values.put(Statuses.ICON, icon ? getBlob(BitmapFactory.decodeResource(getResources(), map_icons[Integer.parseInt(service)])) : null);
											SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
										} else {
											updateCreatedText = true;
										}
										if (updateCreatedText) {
											Cursor statuses = SonetService.this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.CREATED},Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account}, null);
											if (statuses.moveToFirst()) {
												int icreated = statuses.getColumnIndex(Statuses.CREATED);
												while (!statuses.isAfterLast()) {
													ContentValues values = new ContentValues();
													values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(icreated), time24hr));
													SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													statuses.moveToNext();
												}
											}
											statuses.close();
										}
										// remove self from queue
										if (!SonetService.sWidgetsTasks.isEmpty() && SonetService.sWidgetsTasks.containsKey(widget)) {
											ArrayList<AsyncTask<String, Void, String>> tasks = SonetService.sWidgetsTasks.get(widget);
											if (tasks != null) {
												SonetService.sWidgetsTasks.get(widget).remove(this);
												if (tasks.isEmpty()) {
													SonetService.sWidgetsTasks.remove(tasks);
												}
											}				
										}
										// see if the tasks are finished
										checkWidgetUpdateReady(widget);
									}
								};
								statusesTasks.add(task);
								task.execute(appWidgetId, account, service, accounts.getString(itoken));
								break;
							case MYSPACE:
								task = new AsyncTask<String, Void, String>() {
									private String widget;
									private String account;
									private String service;
									private boolean time24hr,
									icon;
									private int status_bg_color = -1,
									status_count;

									@Override
									protected String doInBackground(String... params) {
										// get this account's statuses
										// get the settings form time24hr and bg_color
										this.widget = params[0];
										this.account = params[1];
										this.service = params[2];
										Cursor c = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, account}, null);
										if (c.moveToFirst()) {
											time24hr = c.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
											status_bg_color = c.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
											icon = c.getInt(c.getColumnIndex(Widgets.ICON)) == 1;
											status_count = c.getInt(c.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
										} else {
											Cursor d = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
											if (d.moveToFirst()) {
												time24hr = d.getInt(d.getColumnIndex(Widgets.TIME24HR)) == 1;
												status_bg_color = d.getInt(d.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
												icon = d.getInt(d.getColumnIndex(Widgets.ICON)) == 1;
												status_count = d.getInt(d.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
											} else {
												Cursor e = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
												if (e.moveToFirst()) {
													time24hr = e.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
													status_bg_color = e.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
													icon = e.getInt(e.getColumnIndex(Widgets.ICON)) == 1;
													status_count = e.getInt(e.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
												} else {
													time24hr = false;
													status_bg_color = Sonet.default_message_bg_color;
													icon = true;
													status_count = Sonet.default_statuses_per_account;
												}
												e.close();
											}
											d.close();
										}
										c.close();
										SonetOAuth sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, params[3], params[4]);
										return sonetOAuth.httpResponse(new HttpGet(String.format(MYSPACE_URL_FEED, MYSPACE_BASE_URL, status_count)));
									}

									@Override
									protected void onPostExecute(String response) {
										// parse the response
										boolean updateCreatedText = false;
										if (response != null) {
											String status = "status";
											// create the status_bg
											Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
											Canvas status_bg_canvas = new Canvas(status_bg_bmp);
											status_bg_canvas.drawColor(status_bg_color);
											ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
											status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
											byte[] status_bg = status_bg_blob.toByteArray();
											// if not a full_refresh, only update the status_bg and icons
											String displayName = "displayName",
											moodStatusLastUpdated = "moodStatusLastUpdated",
											thumbnailUrl = "thumbnailUrl",
											author = "author";
											try {
												JSONObject jobj = new JSONObject(response);
												JSONArray entries = jobj.getJSONArray("entry");
												// if there are updates, clear the cache
												if (entries.length() > 0) {
													SonetService.this.getContentResolver().delete(Entities.CONTENT_URI, Entities.ACCOUNT + "=? or " + Entities.ESID + "=\"\"", new String[]{account});
													SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													for (int e = 0; e < entries.length(); e++) {
														JSONObject entry = entries.getJSONObject(e);
														JSONObject authorObj = entry.getJSONObject(author);
														addStatusItem(Sonet.parseDate(entry.getString(moodStatusLastUpdated), MYSPACE_DATE_FORMAT),
																authorObj.getString(displayName),
																authorObj.getString(thumbnailUrl),
																entry.getString(status),
																service,
																time24hr,
																widget,
																account,
																entry.getString("statusId"),
																entry.getString("userId"));
													}
												} else updateCreatedText = true;
											} catch (JSONException e) {
												Log.e(TAG, service + ":" + e.toString());
											}
											// update the bg and icon
											ContentValues values = new ContentValues();
											values.put(Statuses.STATUS_BG, status_bg);
											values.put(Statuses.ICON, icon ? getBlob(BitmapFactory.decodeResource(getResources(), map_icons[Integer.parseInt(service)])) : null);
											SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
										} else {
											updateCreatedText = true;
										}
										if (updateCreatedText) {
											Cursor statuses = SonetService.this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.CREATED},Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account}, null);
											if (statuses.moveToFirst()) {
												int icreated = statuses.getColumnIndex(Statuses.CREATED);
												while (!statuses.isAfterLast()) {
													ContentValues values = new ContentValues();
													values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(icreated), time24hr));
													SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													statuses.moveToNext();
												}
											} else {
												// warn about myspace permissions
												// create the status_bg
												Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
												Canvas status_bg_canvas = new Canvas(status_bg_bmp);
												status_bg_canvas.drawColor(status_bg_color);
												ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
												status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
												byte[] status_bg = status_bg_blob.toByteArray();
												addStatusItem(0,
														getString(R.string.myspace_permissions_title),
														null,
														getString(R.string.myspace_permissions_message),
														service,
														time24hr,
														widget,
														account,
														"",
												"");
												ContentValues values = new ContentValues();
												values.put(Statuses.STATUS_BG, status_bg);
												SonetService.this.getContentResolver().insert(Statuses.CONTENT_URI, values);
											}
											statuses.close();
										}
										// remove self from queue
										if (!SonetService.sWidgetsTasks.isEmpty() && SonetService.sWidgetsTasks.containsKey(widget)) {
											ArrayList<AsyncTask<String, Void, String>> tasks = SonetService.sWidgetsTasks.get(widget);
											if (tasks != null) {
												SonetService.sWidgetsTasks.get(widget).remove(this);
												if (tasks.isEmpty()) {
													SonetService.sWidgetsTasks.remove(tasks);
												}
											}				
										}
										// see if the tasks are finished
										checkWidgetUpdateReady(widget);
									}
								};
								statusesTasks.add(task);
								task.execute(appWidgetId, account, service, accounts.getString(itoken), accounts.getString(isecret));
								break;
							case BUZZ:
								task = new AsyncTask<String, Void, String>() {
									private String widget;
									private String account;
									private String service;
									private boolean time24hr,
									icon;
									private int status_bg_color = -1,
									status_count;

									@Override
									protected String doInBackground(String... params) {
										// get this account's statuses
										// get the settings form time24hr and bg_color
										this.widget = params[0];
										this.account = params[1];
										this.service = params[2];
										Cursor c = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, account}, null);
										if (c.moveToFirst()) {
											time24hr = c.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
											status_bg_color = c.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
											icon = c.getInt(c.getColumnIndex(Widgets.ICON)) == 1;
											status_count = c.getInt(c.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
										} else {
											Cursor d = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
											if (d.moveToFirst()) {
												time24hr = d.getInt(d.getColumnIndex(Widgets.TIME24HR)) == 1;
												status_bg_color = d.getInt(d.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
												icon = d.getInt(d.getColumnIndex(Widgets.ICON)) == 1;
												status_count = d.getInt(d.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
											} else {
												Cursor e = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
												if (e.moveToFirst()) {
													time24hr = e.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
													status_bg_color = e.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
													icon = e.getInt(e.getColumnIndex(Widgets.ICON)) == 1;
													status_count = e.getInt(e.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
												} else {
													time24hr = false;
													status_bg_color = Sonet.default_message_bg_color;
													icon = true;
													status_count = Sonet.default_statuses_per_account;
												}
												e.close();
											}
											d.close();
										}
										c.close();
										SonetOAuth sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, params[3], params[4]);
										return sonetOAuth.httpResponse(new HttpGet(String.format(BUZZ_URL_FEED, BUZZ_BASE_URL, status_count, BUZZ_API_KEY)));
									}

									@Override
									protected void onPostExecute(String response) {
										// parse the response
										boolean updateCreatedText = false;
										if (response != null) {
											String id = "id";
											// create the status_bg
											Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
											Canvas status_bg_canvas = new Canvas(status_bg_bmp);
											status_bg_canvas.drawColor(status_bg_color);
											ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
											status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
											byte[] status_bg = status_bg_blob.toByteArray();
											// if not a full_refresh, only update the status_bg and icons
											try {
												JSONArray entries = new JSONObject(response).getJSONObject("data").getJSONArray("items");
												// if there are updates, clear the cache
												if (entries.length() > 0) {
													SonetService.this.getContentResolver().delete(Entities.CONTENT_URI, Entities.ACCOUNT + "=?", new String[]{account});
													SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													for (int e = 0; e < entries.length(); e++) {
														JSONObject entry = entries.getJSONObject(e);
														if (entry.has("published") && entry.has("actor") && entry.has("object")) {
															JSONObject actor = entry.getJSONObject("actor");
															JSONObject object = entry.getJSONObject("object");
															if (actor.has("name") && actor.has("thumbnailUrl") && object.has("originalContent")) {
																addStatusItem(Sonet.parseDate(entry.getString("published"), BUZZ_DATE_FORMAT),
																		actor.getString("name"),
																		actor.getString("thumbnailUrl"),
																		object.getString("originalContent"),
																		service,
																		time24hr,
																		widget,
																		account,
																		entry.getString(id),
																		actor.getString(id));
															}
														}
													}
												} else {
													updateCreatedText = true;
												}
											} catch (JSONException e) {
												Log.e(TAG, service + ":" + e.toString());
											}
											// update the bg and icon
											ContentValues values = new ContentValues();
											values.put(Statuses.STATUS_BG, status_bg);
											values.put(Statuses.ICON, icon ? getBlob(BitmapFactory.decodeResource(getResources(), map_icons[Integer.parseInt(service)])) : null);
											SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
										} else {
											updateCreatedText = true;
										}
										if (updateCreatedText) {
											Cursor statuses = SonetService.this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.CREATED},Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account}, null);
											if (statuses.moveToFirst()) {
												int icreated = statuses.getColumnIndex(Statuses.CREATED);
												while (!statuses.isAfterLast()) {
													ContentValues values = new ContentValues();
													values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(icreated), time24hr));
													SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													statuses.moveToNext();
												}
											}
											statuses.close();
										}
										// remove self from queue
										if (!SonetService.sWidgetsTasks.isEmpty() && SonetService.sWidgetsTasks.containsKey(widget)) {
											ArrayList<AsyncTask<String, Void, String>> tasks = SonetService.sWidgetsTasks.get(widget);
											if (tasks != null) {
												SonetService.sWidgetsTasks.get(widget).remove(this);
												if (tasks.isEmpty()) {
													SonetService.sWidgetsTasks.remove(tasks);
												}
											}				
										}
										// see if the tasks are finished
										checkWidgetUpdateReady(widget);
									}
								};
								statusesTasks.add(task);
								task.execute(appWidgetId, account, service, accounts.getString(itoken), accounts.getString(isecret));
								break;
							case FOURSQUARE:
								task = new AsyncTask<String, Void, String>() {
									private String widget;
									private String account;
									private String service;
									private boolean time24hr,
									icon;
									private int status_bg_color = -1,
									status_count;

									@Override
									protected String doInBackground(String... params) {
										// get this account's statuses
										// get the settings form time24hr and bg_color
										this.widget = params[0];
										this.account = params[1];
										this.service = params[2];
										Cursor c = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, account}, null);
										if (c.moveToFirst()) {
											time24hr = c.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
											status_bg_color = c.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
											icon = c.getInt(c.getColumnIndex(Widgets.ICON)) == 1;
											status_count = c.getInt(c.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
										} else {
											Cursor d = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
											if (d.moveToFirst()) {
												time24hr = d.getInt(d.getColumnIndex(Widgets.TIME24HR)) == 1;
												status_bg_color = d.getInt(d.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
												icon = d.getInt(d.getColumnIndex(Widgets.ICON)) == 1;
												status_count = d.getInt(d.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
											} else {
												Cursor e = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
												if (e.moveToFirst()) {
													time24hr = e.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
													status_bg_color = e.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
													icon = e.getInt(e.getColumnIndex(Widgets.ICON)) == 1;
													status_count = e.getInt(e.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
												} else {
													time24hr = false;
													status_bg_color = Sonet.default_message_bg_color;
													icon = true;
													status_count = Sonet.default_statuses_per_account;
												}
												e.close();
											}
											d.close();
										}
										c.close();
										return Sonet.httpResponse(new HttpGet(String.format(FOURSQUARE_URL_FEED, FOURSQUARE_BASE_URL, status_count, params[3])));
									}

									@Override
									protected void onPostExecute(String response) {
										// parse the response
										boolean updateCreatedText = false;
										if (response != null) {
											String id = "id";
											// create the status_bg
											Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
											Canvas status_bg_canvas = new Canvas(status_bg_bmp);
											status_bg_canvas.drawColor(status_bg_color);
											ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
											status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
											byte[] status_bg = status_bg_blob.toByteArray();
											// if not a full_refresh, only update the status_bg and icons
											try {
												JSONArray checkins = new JSONObject(response).getJSONObject("response").getJSONArray("recent");
												// if there are updates, clear the cache
												if (checkins.length() > 0) {
													SonetService.this.getContentResolver().delete(Entities.CONTENT_URI, Entities.ACCOUNT + "=?", new String[]{account});
													SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													for (int e = 0; e < checkins.length(); e++) {
														JSONObject checkin = checkins.getJSONObject(e);
														JSONObject user = checkin.getJSONObject("user");
														String shout = "";
														if (checkin.has("shout")) {
															shout = checkin.getString("shout") + "\n";
														}
														if (checkin.has("venue")) {
															JSONObject venue = checkin.getJSONObject("venue");
															if (venue.has("name")) {
																shout += "@" + venue.getString("name");																
															}
														}
														addStatusItem(checkin.getLong("createdAt") * 1000,
																user.getString("firstName") + " " + user.getString("lastName"),
																user.getString("photo"),
																shout,
																service,
																time24hr,
																widget,
																account,
																checkin.getString(id),
																user.getString(id));
													}
												} else {
													updateCreatedText = true;
												}
											} catch (JSONException e) {
												Log.e(TAG, service + ":" + e.toString());
												Log.e(TAG, response);
											}
											// update the bg and icon
											ContentValues values = new ContentValues();
											values.put(Statuses.STATUS_BG, status_bg);
											values.put(Statuses.ICON, icon ? getBlob(BitmapFactory.decodeResource(getResources(), map_icons[Integer.parseInt(service)])) : null);
											SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
										} else {
											updateCreatedText = true;
										}
										if (updateCreatedText) {
											Cursor statuses = SonetService.this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.CREATED},Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account}, null);
											if (statuses.moveToFirst()) {
												int icreated = statuses.getColumnIndex(Statuses.CREATED);
												while (!statuses.isAfterLast()) {
													ContentValues values = new ContentValues();
													values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(icreated), time24hr));
													SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													statuses.moveToNext();
												}
											}
											statuses.close();
										}
										// remove self from queue
										if (!SonetService.sWidgetsTasks.isEmpty() && SonetService.sWidgetsTasks.containsKey(widget)) {
											ArrayList<AsyncTask<String, Void, String>> tasks = SonetService.sWidgetsTasks.get(widget);
											if (tasks != null) {
												SonetService.sWidgetsTasks.get(widget).remove(this);
												if (tasks.isEmpty()) {
													SonetService.sWidgetsTasks.remove(tasks);
												}
											}				
										}
										// see if the tasks are finished
										checkWidgetUpdateReady(widget);
									}
								};
								statusesTasks.add(task);
								task.execute(appWidgetId, account, service, accounts.getString(itoken));
								break;
							case LINKEDIN:
								task = new AsyncTask<String, Void, String>() {
									private String widget;
									private String account;
									private String service;
									private boolean time24hr,
									icon;
									private int status_bg_color = -1,
									status_count;

									@Override
									protected String doInBackground(String... params) {
										// get this account's statuses
										// get the settings form time24hr and bg_color
										this.widget = params[0];
										this.account = params[1];
										this.service = params[2];
										Cursor c = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, account}, null);
										if (c.moveToFirst()) {
											time24hr = c.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
											status_bg_color = c.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
											icon = c.getInt(c.getColumnIndex(Widgets.ICON)) == 1;
											status_count = c.getInt(c.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
										} else {
											Cursor d = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
											if (d.moveToFirst()) {
												time24hr = d.getInt(d.getColumnIndex(Widgets.TIME24HR)) == 1;
												status_bg_color = d.getInt(d.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
												icon = d.getInt(d.getColumnIndex(Widgets.ICON)) == 1;
												status_count = d.getInt(d.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
											} else {
												Cursor e = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
												if (e.moveToFirst()) {
													time24hr = e.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
													status_bg_color = e.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
													icon = e.getInt(e.getColumnIndex(Widgets.ICON)) == 1;
													status_count = e.getInt(e.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
												} else {
													time24hr = false;
													status_bg_color = Sonet.default_message_bg_color;
													icon = true;
													status_count = Sonet.default_statuses_per_account;
												}
												e.close();
											}
											d.close();
										}
										c.close();
										SonetOAuth sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, params[3], params[4]);
										HttpGet httpGet = new HttpGet(String.format(LINKEDIN_URL_FEED, LINKEDIN_BASE_URL, status_count));
										for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
										return sonetOAuth.httpResponse(httpGet);
									}

									@Override
									protected void onPostExecute(String response) {
										// parse the response
										boolean updateCreatedText = false;
										if (response != null) {
											String id = "id";
											// create the status_bg
											Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
											Canvas status_bg_canvas = new Canvas(status_bg_bmp);
											status_bg_canvas.drawColor(status_bg_color);
											ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
											status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
											byte[] status_bg = status_bg_blob.toByteArray();
											// if not a full_refresh, only update the status_bg and icons
											try {
												JSONObject jobj = new JSONObject(response);
												JSONArray values = jobj.getJSONArray("values");
												// if there are updates, clear the cache
												if (values.length() > 0) {
													SonetService.this.getContentResolver().delete(Entities.CONTENT_URI, Entities.ACCOUNT + "=?", new String[]{account});
													SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													for (int e = 0; e < values.length(); e++) {
														JSONObject value = values.getJSONObject(e);
														String updateType = value.getString("updateType");
														JSONObject updateContent = value.getJSONObject("updateContent");
														if (LINKEDIN_UPDATETYPES.containsKey(updateType) && updateContent.has("person")) {
															JSONObject person = updateContent.getJSONObject("person");
															String update = LINKEDIN_UPDATETYPES.get(updateType);
															if (updateType.equals("APPS")) {
																if (person.has("personActivities")) {
																	JSONObject personActivities = person.getJSONObject("personActivities");
																	if (personActivities.has("values")) {
																		JSONArray updates = personActivities.getJSONArray("values");
																		for (int u = 0; u < updates.length(); u++) {
																			update += updates.getJSONObject(u).getString("body");
																			if (u < (updates.length() - 1)) update += ", ";
																		}
																	}
																}
															} else if (updateType.equals("CONN")) {
																if (person.has("connections")) {
																	JSONObject connections = person.getJSONObject("connections");
																	if (connections.has("values")) {
																		JSONArray updates = connections.getJSONArray("values");
																		for (int u = 0; u < updates.length(); u++) {
																			update += updates.getJSONObject(u).getString("firstName") + " " + updates.getJSONObject(u).getString("lastName");
																			if (u < (updates.length() - 1)) update += ", ";
																		}
																	}
																}
															} else if (updateType.equals("JOBP")) {
																if (updateContent.has("job") && updateContent.getJSONObject("job").has("position") && updateContent.getJSONObject("job").getJSONObject("position").has("title")) update += updateContent.getJSONObject("job").getJSONObject("position").getString("title");
															} else if (updateType.equals("JGRP")) {
																if (person.has("memberGroups")) {
																	JSONObject memberGroups = person.getJSONObject("memberGroups");
																	if (memberGroups.has("values")) {
																		JSONArray updates = memberGroups.getJSONArray("values");
																		for (int u = 0; u < updates.length(); u++) {
																			update += updates.getJSONObject(u).getString("name");
																			if (u < (updates.length() - 1)) update += ", ";
																		}
																	}
																}
															} else if (updateType.equals("PREC")) {
																if (person.has("recommendationsGiven")) {
																	JSONObject recommendationsGiven = person.getJSONObject("recommendationsGiven");
																	if (recommendationsGiven.has("values")) {
																		JSONArray updates = recommendationsGiven.getJSONArray("values");
																		for (int u = 0; u < updates.length(); u++) {
																			JSONObject recommendation = updates.getJSONObject(u);
																			JSONObject recommendee = recommendation.getJSONObject("recommendee");
																			if (recommendee.has("firstName")) update += recommendee.getString("firstName");
																			if (recommendee.has("lastName")) update += recommendee.getString("lastName");
																			if (recommendation.has("recommendationSnippet")) update += ":" + recommendation.getString("recommendationSnippet");
																			if (u < (updates.length() - 1)) update += ", ";
																		}
																	}
																}
															}
															addStatusItem(value.getLong("timestamp"),
																	person.getString("firstName") + " " + person.getString("lastName"),
																	person.has("pictureUrl") ? person.getString("pictureUrl") : null,
																			update,
																			service,
																			time24hr,
																			widget,
																			account,
																			value.has("updateKey") ? value.getString("updateKey") : "",
																					person.getString(id));
														}
													}
												} else {
													updateCreatedText = true;
												}
											} catch (JSONException e) {
												Log.e(TAG, service + ":" + e.toString());
											}
											// update the bg and icon
											ContentValues values = new ContentValues();
											values.put(Statuses.STATUS_BG, status_bg);
											values.put(Statuses.ICON, icon ? getBlob(BitmapFactory.decodeResource(getResources(), map_icons[Integer.parseInt(service)])) : null);
											SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
										} else {
											updateCreatedText = true;
										}
										if (updateCreatedText) {
											Cursor statuses = SonetService.this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.CREATED},Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account}, null);
											if (statuses.moveToFirst()) {
												int icreated = statuses.getColumnIndex(Statuses.CREATED);
												while (!statuses.isAfterLast()) {
													ContentValues values = new ContentValues();
													values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(icreated), time24hr));
													SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, service, account});
													statuses.moveToNext();
												}
											}
											statuses.close();
										}
										// remove self from queue
										if (!SonetService.sWidgetsTasks.isEmpty() && SonetService.sWidgetsTasks.containsKey(widget)) {
											ArrayList<AsyncTask<String, Void, String>> tasks = SonetService.sWidgetsTasks.get(widget);
											if (tasks != null) {
												SonetService.sWidgetsTasks.get(widget).remove(this);
												if (tasks.isEmpty()) {
													SonetService.sWidgetsTasks.remove(tasks);
												}
											}				
										}
										// see if the tasks are finished
										checkWidgetUpdateReady(widget);
									}
								};
								statusesTasks.add(task);
								task.execute(appWidgetId, account, service, accounts.getString(itoken), accounts.getString(isecret));
								break;
							}
						} else {
							// update the bg and icon only
							// no tasks will be created, and the widget will be updated below
							boolean icon = true,
							time24hr = false;
							int status_bg_color = Sonet.default_message_bg_color;
							byte[] status_bg;
							Cursor c = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{appWidgetId, account}, null);
							if (c.moveToFirst()) {
								status_bg_color = c.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
								icon = c.getInt(c.getColumnIndex(Widgets.ICON)) == 1;
								time24hr = c.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
							} else {
								Cursor d = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{appWidgetId, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
								if (d.moveToFirst()) {
									status_bg_color = d.getInt(d.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
									icon = d.getInt(d.getColumnIndex(Widgets.ICON)) == 1;
									time24hr = d.getInt(d.getColumnIndex(Widgets.TIME24HR)) == 1;
								} else {
									Cursor e = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.TIME24HR}, Widgets.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
									if (e.moveToFirst()) {
										status_bg_color = e.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
										icon = e.getInt(e.getColumnIndex(Widgets.ICON)) == 1;
										time24hr = e.getInt(e.getColumnIndex(Widgets.TIME24HR)) == 1;
									}
									e.close();
								}
								d.close();
							}
							c.close();
							// create the status_bg
							Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
							Canvas status_bg_canvas = new Canvas(status_bg_bmp);
							status_bg_canvas.drawColor(status_bg_color);
							ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
							status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
							status_bg = status_bg_blob.toByteArray();
							ContentValues values = new ContentValues();
							values.put(Statuses.STATUS_BG, status_bg);
							values.put(Statuses.ICON, icon ? getBlob(BitmapFactory.decodeResource(getResources(), map_icons[Integer.parseInt(service)])) : null);
							this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{appWidgetId, service, account});
							Cursor statuses = this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.CREATED}, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{appWidgetId, service, account}, null);
							if (statuses.moveToFirst()) {
								int iid = statuses.getColumnIndex(Statuses._ID),
								icreated = statuses.getColumnIndex(Statuses.CREATED);
								while (!statuses.isAfterLast()) {
									values = new ContentValues();
									values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(icreated), time24hr));
									this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses._ID + "=?", new String[]{Integer.toString(statuses.getInt(iid))});
									statuses.moveToNext();
								}
							}
							statuses.close();
						}
						accounts.moveToNext();
					}
				} else this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=?", new String[]{appWidgetId}); // no accounts, clear cache
				accounts.close();
				// the alarm should always be set, rather than depend on the tasks to complete
				if (refreshInterval > 0) mAlarmManager.set(backgroundUpdate ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC, System.currentTimeMillis() + refreshInterval, PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setData(Uri.withAppendedPath(Widgets.CONTENT_URI, appWidgetId)), 0));
				checkWidgetUpdateReady(appWidgetId);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, startId);
		return START_STICKY;
	}

	private static Object sLock = new Object();
	private static Queue<String> sAppWidgetIds = new LinkedList<String>();

	public static void updateWidgets(int[] appWidgetIds) {
		String[] widgetIds = new String[appWidgetIds.length];
		for (int i = 0; i < widgetIds.length; i++) {
			widgetIds[i] = Integer.toString(appWidgetIds[i]);
		}
		SonetService.updateWidgets(widgetIds);
	}

	public static void updateWidgets(String[] appWidgetIds) {
		synchronized (sLock) {
			for (String appWidgetId : appWidgetIds) {
				if (!sAppWidgetIds.contains(appWidgetId)) {
					sAppWidgetIds.add(appWidgetId);
				}
			}
		}
	}

	private static boolean updatesQueued() {
		synchronized (sLock) {
			return !sAppWidgetIds.isEmpty();
		}
	}

	private static String getNextUpdate() {
		synchronized (sLock) {
			if (sAppWidgetIds.peek() == null) {
				return Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID);
			} else {
				return sAppWidgetIds.poll();
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private void updateAccount(String accountId, String sid) {
		ContentValues values = new ContentValues();
		values.put(Accounts.SID, String.format(SID_FORMAT, sid));
		this.getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{accountId});
	}

	private void addStatusItem(long created, String friend, String url, String message, String service, boolean time24hr, String appWidgetId, String accountId, String sid, String esid) {
		long id;
		byte[] profile = null;
		if (url != null) {
			// get profile
			Bitmap bmp = null;
			try {
				bmp = BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
			} catch (IOException e) {
				Log.e(TAG,e.getMessage());
			}
			profile = getBlob(bmp);
		}
		Cursor entity = this.getContentResolver().query(Entities.CONTENT_URI, new String[]{Entities._ID}, Entities.ACCOUNT + "=? and " + Entities.ESID + "=?", new String[]{accountId, sid}, null);
		if (entity.moveToFirst()) {
			id = entity.getInt(entity.getColumnIndex(Entities._ID));
		} else {
			ContentValues values = new ContentValues();
			values.put(Entities.ESID, String.format(SID_FORMAT, esid));
			values.put(Entities.FRIEND, friend);
			values.put(Entities.PROFILE, profile);
			values.put(Entities.ACCOUNT, accountId);
			id = Long.parseLong(this.getContentResolver().insert(Entities.CONTENT_URI, values).getLastPathSegment());
		}
		entity.close();
		ContentValues values = new ContentValues();
		values.put(Statuses.CREATED, created);
		values.put(Statuses.ENTITY, id);
		values.put(Statuses.MESSAGE, message);
		values.put(Statuses.SERVICE, Integer.parseInt(service));
		values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
		values.put(Statuses.WIDGET, Integer.parseInt(appWidgetId));
		values.put(Statuses.ACCOUNT, Integer.parseInt(accountId));
		values.put(Statuses.SID, String.format(SID_FORMAT, sid));
		this.getContentResolver().insert(Statuses.CONTENT_URI, values);
	}

	private byte[] getBlob(Bitmap bmp) {
		ByteArrayOutputStream blob = new ByteArrayOutputStream();
		if (bmp != null) {
			bmp.compress(Bitmap.CompressFormat.PNG, 100, blob);
		}
		return blob.toByteArray();		
	}

	private void checkWidgetUpdateReady(String widget) {
		// see if the tasks are finished
		boolean widgetUpdateReady = SonetService.sWidgetsTasks.isEmpty() || !SonetService.sWidgetsTasks.containsKey(widget);
		if (!widgetUpdateReady) {
			ArrayList<AsyncTask<String, Void, String>> tasks = SonetService.sWidgetsTasks.get(widget);
			if (tasks.isEmpty()) {
				widgetUpdateReady = true;
			} else {
				Iterator<AsyncTask<String, Void, String>> itr = tasks.iterator();
				while (itr.hasNext() && !widgetUpdateReady) {
					widgetUpdateReady = itr.next().getStatus() == AsyncTask.Status.FINISHED;
				}
			}
		}
		if (widgetUpdateReady) {
			boolean hasbuttons = false;
			int scrollable = 0;
			int buttons_bg_color = Sonet.default_buttons_bg_color,
			buttons_color = Sonet.default_buttons_color,
			buttons_textsize = Sonet.default_buttons_textsize;
			Cursor settings = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.INTERVAL, Widgets.HASBUTTONS, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_TEXTSIZE, Widgets.BACKGROUND_UPDATE, Widgets.SCROLLABLE}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
			if (settings.moveToFirst()) {
				hasbuttons = settings.getInt(settings.getColumnIndex(Widgets.HASBUTTONS)) == 1;
				buttons_bg_color = settings.getInt(settings.getColumnIndex(Widgets.BUTTONS_BG_COLOR));
				buttons_color = settings.getInt(settings.getColumnIndex(Widgets.BUTTONS_COLOR));
				buttons_textsize = settings.getInt(settings.getColumnIndex(Widgets.BUTTONS_TEXTSIZE));
				scrollable = settings.getInt(settings.getColumnIndex(Widgets.SCROLLABLE));
			}
			settings.close();
			// Push update for this widget to the home screen
			RemoteViews views = new RemoteViews(this.getPackageName(), hasbuttons ? R.layout.widget : R.layout.widget_nobuttons);
			if (hasbuttons) {
				Bitmap buttons_bg = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas buttons_bg_canvas = new Canvas(buttons_bg);
				buttons_bg_canvas.drawColor(buttons_bg_color);
				views.setImageViewBitmap(R.id.buttons_bg, buttons_bg);
				views.setTextColor(R.id.buttons_bg_clear, buttons_bg_color);
				views.setFloat(R.id.buttons_bg_clear, "setTextSize", buttons_textsize);
				views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this, 0, new Intent(this, SonetCreatePost.class).setAction(LauncherIntent.Action.ACTION_VIEW_CLICK).setData(Uri.withAppendedPath(Widgets.CONTENT_URI, widget)), 0));
				views.setTextColor(R.id.button_post, buttons_color);
				views.setFloat(R.id.button_post, "setTextSize", buttons_textsize);
				views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(this, 0, new Intent(this, ManageAccounts.class).setAction(widget), 0));
				views.setTextColor(R.id.button_configure, buttons_color);
				views.setFloat(R.id.button_configure, "setTextSize", buttons_textsize);
				views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setAction(widget), 0));
				views.setTextColor(R.id.button_refresh, buttons_color);
				views.setFloat(R.id.button_refresh, "setTextSize", buttons_textsize);
			}
			int[] map_message = {R.id.message0, R.id.message1, R.id.message2, R.id.message3, R.id.message4, R.id.message5, R.id.message6, R.id.message7, R.id.message8, R.id.message9, R.id.message10, R.id.message11, R.id.message12, R.id.message13, R.id.message14, R.id.message15},
			map_item = {R.id.item0, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7, R.id.item8, R.id.item9, R.id.item10, R.id.item11, R.id.item12, R.id.item13, R.id.item14, R.id.item15},
			map_profile = {R.id.profile0, R.id.profile1, R.id.profile2, R.id.profile3, R.id.profile4, R.id.profile5, R.id.profile6, R.id.profile7, R.id.profile8, R.id.profile9, R.id.profile10, R.id.profile11, R.id.profile12, R.id.profile13, R.id.profile14, R.id.profile15},
			map_screenname = {R.id.friend0, R.id.friend1, R.id.friend2, R.id.friend3, R.id.friend4, R.id.friend5, R.id.friend6, R.id.friend7, R.id.friend8, R.id.friend9, R.id.friend10, R.id.friend11, R.id.friend12, R.id.friend13, R.id.friend14, R.id.friend15},
			map_created = {R.id.created0, R.id.created1, R.id.created2, R.id.created3, R.id.created4, R.id.created5, R.id.created6, R.id.created7, R.id.created8, R.id.created9, R.id.created10, R.id.created11, R.id.created12, R.id.created13, R.id.created14, R.id.created15},
			map_status_bg = {R.id.status_bg0, R.id.status_bg1, R.id.status_bg2, R.id.status_bg3, R.id.status_bg4, R.id.status_bg5, R.id.status_bg6, R.id.status_bg7, R.id.status_bg8, R.id.status_bg9, R.id.status_bg10, R.id.status_bg11, R.id.status_bg12, R.id.status_bg13, R.id.status_bg14, R.id.status_bg15},
			map_friend_bg_clear = {R.id.friend_bg_clear0, R.id.friend_bg_clear1, R.id.friend_bg_clear2, R.id.friend_bg_clear3, R.id.friend_bg_clear4, R.id.friend_bg_clear5, R.id.friend_bg_clear6, R.id.friend_bg_clear7, R.id.friend_bg_clear8, R.id.friend_bg_clear9, R.id.friend_bg_clear10, R.id.friend_bg_clear11, R.id.friend_bg_clear12, R.id.friend_bg_clear13, R.id.friend_bg_clear14, R.id.friend_bg_clear15},
			map_message_bg_clear = {R.id.message_bg_clear0, R.id.message_bg_clear1, R.id.message_bg_clear2, R.id.message_bg_clear3, R.id.message_bg_clear4, R.id.message_bg_clear5, R.id.message_bg_clear6, R.id.message_bg_clear7, R.id.message_bg_clear8, R.id.message_bg_clear9, R.id.message_bg_clear10, R.id.message_bg_clear11, R.id.message_bg_clear12, R.id.message_bg_clear13, R.id.message_bg_clear14, R.id.message_bg_clear15},
			map_icon = {R.id.icon0, R.id.icon1, R.id.icon2, R.id.icon3, R.id.icon4, R.id.icon5, R.id.icon6, R.id.icon7, R.id.icon8, R.id.icon9, R.id.icon10, R.id.icon11, R.id.icon12, R.id.icon13, R.id.icon14, R.id.icon15};
			Cursor accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID}, Accounts.WIDGET + "=?", new String[]{widget}, null);
			if (accounts.moveToFirst()) {
				Cursor statuses_styles = this.getContentResolver().query(Uri.withAppendedPath(Statuses_styles.CONTENT_URI, widget), new String[]{Statuses_styles._ID, Statuses_styles.CREATED, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.SERVICE, Statuses_styles.CREATEDTEXT, Statuses_styles.WIDGET, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON}, null, null, Statuses_styles.CREATED + " desc");
				if (statuses_styles.moveToFirst()) {
					if (scrollable == 0) {
						int iid = statuses_styles.getColumnIndex(Statuses_styles._ID),
						iprofile = statuses_styles.getColumnIndex(Statuses_styles.PROFILE),
						ifriend = statuses_styles.getColumnIndex(Statuses_styles.FRIEND),
						imessage = statuses_styles.getColumnIndex(Statuses_styles.MESSAGE),
						icreatedText = statuses_styles.getColumnIndex(Statuses_styles.CREATEDTEXT),
						istatus_bg = statuses_styles.getColumnIndex(Statuses_styles.STATUS_BG),
						iicon = statuses_styles.getColumnIndex(Statuses_styles.ICON),
						count_status = 0;
						while (!statuses_styles.isAfterLast() && (count_status < map_item.length)) {
							int friend_color = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.FRIEND_COLOR)),
							created_color = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.CREATED_COLOR)),
							friend_textsize = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.FRIEND_TEXTSIZE)),
							created_textsize = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.CREATED_TEXTSIZE)),
							messages_color = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.MESSAGES_COLOR)),
							messages_textsize = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.MESSAGES_TEXTSIZE));
							// set icons
							byte[] icon = statuses_styles.getBlob(iicon);
							Bitmap iconbmp = BitmapFactory.decodeByteArray(icon, 0, icon.length);
							if (iconbmp != null) {
								views.setImageViewBitmap(map_icon[count_status], iconbmp);
							}
							views.setTextViewText(map_friend_bg_clear[count_status], statuses_styles.getString(ifriend));
							views.setFloat(map_friend_bg_clear[count_status], "setTextSize", friend_textsize);
							views.setTextViewText(map_message_bg_clear[count_status], statuses_styles.getString(imessage));
							views.setFloat(map_message_bg_clear[count_status], "setTextSize", messages_textsize);
							// set messages background
							byte[] status_bg = statuses_styles.getBlob(istatus_bg);
							Bitmap status_bgbmp = BitmapFactory.decodeByteArray(status_bg, 0, status_bg.length);
							if (status_bgbmp != null) {
								views.setImageViewBitmap(map_status_bg[count_status], status_bgbmp);
							}
							views.setTextViewText(map_message[count_status], statuses_styles.getString(imessage));
							views.setTextColor(map_message[count_status], messages_color);
							views.setFloat(map_message[count_status], "setTextSize", messages_textsize);
							// if no buttons, use StatusDialog.java with options for Config and Refresh
							views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, new Intent(this, StatusDialog.class).setData(Uri.withAppendedPath(Statuses_styles.CONTENT_URI, Integer.toString(statuses_styles.getInt(iid)))), 0));
							views.setTextViewText(map_screenname[count_status], statuses_styles.getString(ifriend));
							views.setTextColor(map_screenname[count_status], friend_color);
							views.setFloat(map_screenname[count_status], "setTextSize", friend_textsize);
							views.setTextViewText(map_created[count_status], statuses_styles.getString(icreatedText));
							views.setTextColor(map_created[count_status], created_color);
							views.setFloat(map_created[count_status], "setTextSize", created_textsize);
							byte[] profile = statuses_styles.getBlob(iprofile);
							Bitmap profilebmp = null;
							if (profile != null) {
								profilebmp = BitmapFactory.decodeByteArray(profile, 0, profile.length);
							}
							if (profilebmp != null) {
								views.setImageViewBitmap(map_profile[count_status], profilebmp);						
							}
							count_status++;
							statuses_styles.moveToNext();
						}
					}
				} else {
					views.setTextViewText(map_message[0], this.getString((mConnectivityManager.getActiveNetworkInfo() != null) && mConnectivityManager.getActiveNetworkInfo().isConnected() ? R.string.no_updates : R.string.no_connection));
				}
				statuses_styles.close();
			} else {
				views.setTextViewText(map_message[0], this.getString(R.string.loading));
			}
			accounts.close();
			if ((widget != null) && (views != null)) {
				AppWidgetManager.getInstance(this).updateAppWidget(Integer.parseInt(widget), views);
			}
			// replace with scrollable widget
			if (scrollable != 0) {
				buildScrollable(Integer.parseInt(widget), scrollable);
			}
		}
		if (SonetService.sWidgetsTasks.isEmpty()) {
			Sonet.release();
		}
	}
	
	private void buildScrollable(int appWidgetId, int scrollableVersion) {
		// set widget as scrollable
        Intent replaceDummy = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);
        replaceDummy.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.messages);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID, R.layout.widget_listview);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);
        
        //provider
        Uri uri = Uri.withAppendedPath(Statuses_styles.CONTENT_URI, Integer.toString(appWidgetId));
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, uri.toString());
        String[] projection = new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON};
        String sortOrder = Statuses_styles.CREATED + " desc";
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, projection);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, sortOrder);
		String whereClause = Statuses_styles.WIDGET + "=?";
		String[] selectionArgs = new String[]{Integer.toString(appWidgetId)};
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_ACTION_VIEW_URI_INDEX, SonetProvider.StatusesStylesColumns._id.ordinal());
        
        switch (scrollableVersion) {
        case 1:
    		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.widget_item);
    		int[] cursorIndices = new int[8];
    		int[] viewTypes = new int[8];
    		int[] layoutIds = new int[8];
    		int[] defaultResource = new int[8];
    		boolean[] clickable = new boolean[8];
    		// R.id.friend_bg_clear
    		cursorIndices[0] = SonetProvider.StatusesStylesColumns.friend.ordinal();
    		viewTypes[0] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
    		layoutIds[0] = R.id.friend_bg_clear;
    		defaultResource[0] = 0;
    		clickable[0] = false;
    		// R.id.message_bg_clear
    		cursorIndices[1] = SonetProvider.StatusesStylesColumns.message.ordinal();
    		viewTypes[1] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
    		layoutIds[1] = R.id.message_bg_clear;
    		defaultResource[1] = 0;
    		clickable[1] = false;
    		// R.id.status_bg
    		cursorIndices[2] = SonetProvider.StatusesStylesColumns.status_bg.ordinal();
    		viewTypes[2] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
    		layoutIds[2] = R.id.status_bg;
    		defaultResource[2] = 0;
    		clickable[2] = true;
    		// R.id.profile
    		cursorIndices[3] = SonetProvider.StatusesStylesColumns.profile.ordinal();
    		viewTypes[3] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
    		layoutIds[3] = R.id.profile;
    		defaultResource[3] = 0;
    		clickable[3] = false;
    		// R.id.friend
    		cursorIndices[4] = SonetProvider.StatusesStylesColumns.friend.ordinal();
    		viewTypes[4] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
    		layoutIds[4] = R.id.friend;
    		defaultResource[4] = 0;
    		clickable[4] = false;
    		// R.id.created
    		cursorIndices[5] = SonetProvider.StatusesStylesColumns.createdtext.ordinal();
    		viewTypes[5] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
    		layoutIds[5] = R.id.created;
    		defaultResource[5] = 0;
    		clickable[5] = false;
    		// R.id.message
    		cursorIndices[6] = SonetProvider.StatusesStylesColumns.message.ordinal();
    		viewTypes[6] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
    		layoutIds[6] = R.id.message;
    		defaultResource[6] = 0;
    		clickable[6] = false;
    		// R.id.icon
    		cursorIndices[7] = SonetProvider.StatusesStylesColumns.icon.ordinal();
    		viewTypes[7] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
    		layoutIds[7] = R.id.icon;
    		defaultResource[7] = 0;
    		clickable[7] = false;

    		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
    		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
    		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
    		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_DEFAULT_RESOURCES, defaultResource);
    		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
        	break;
        case 2:
            BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.widget_item);

            Intent i = new Intent(this, SonetWidget.class)
            .setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
            .setData(uri)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

            itemViews.SetBoundOnClickIntent(R.id.item, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.StatusesStylesColumns._id.ordinal());

            itemViews.setBoundCharSequence(R.id.friend_bg_clear, "setText", SonetProvider.StatusesStylesColumns.friend.ordinal(), 0);
            itemViews.setBoundFloat(R.id.friend_bg_clear, "setTextSize", SonetProvider.StatusesStylesColumns.friend_textsize.ordinal());

            itemViews.setBoundCharSequence(R.id.message_bg_clear, "setText", SonetProvider.StatusesStylesColumns.message.ordinal(), 0);
            itemViews.setBoundFloat(R.id.message_bg_clear, "setTextSize", SonetProvider.StatusesStylesColumns.messages_textsize.ordinal());

            itemViews.setBoundBitmap(R.id.status_bg, "setImageBitmap", SonetProvider.StatusesStylesColumns.status_bg.ordinal(), 0);

            itemViews.setBoundBitmap(R.id.profile, "setImageBitmap", SonetProvider.StatusesStylesColumns.profile.ordinal(), 0);
            itemViews.setBoundCharSequence(R.id.friend, "setText", SonetProvider.StatusesStylesColumns.friend.ordinal(), 0);
            itemViews.setBoundCharSequence(R.id.created, "setText", SonetProvider.StatusesStylesColumns.createdtext.ordinal(), 0);
            itemViews.setBoundCharSequence(R.id.message, "setText", SonetProvider.StatusesStylesColumns.message.ordinal(), 0);

            itemViews.setBoundInt(R.id.friend, "setTextColor", SonetProvider.StatusesStylesColumns.friend_color.ordinal());
            itemViews.setBoundInt(R.id.created, "setTextColor", SonetProvider.StatusesStylesColumns.created_color.ordinal());
            itemViews.setBoundInt(R.id.message, "setTextColor", SonetProvider.StatusesStylesColumns.messages_color.ordinal());

            itemViews.setBoundFloat(R.id.friend, "setTextSize", SonetProvider.StatusesStylesColumns.friend_textsize.ordinal());
            itemViews.setBoundFloat(R.id.created, "setTextSize", SonetProvider.StatusesStylesColumns.created_textsize.ordinal());
            itemViews.setBoundFloat(R.id.message, "setTextSize", SonetProvider.StatusesStylesColumns.messages_textsize.ordinal());

            itemViews.setBoundBitmap(R.id.icon, "setImageBitmap", SonetProvider.StatusesStylesColumns.icon.ordinal(), 0);

            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);
        	break;
        }
		sendBroadcast(replaceDummy);		
	}
}