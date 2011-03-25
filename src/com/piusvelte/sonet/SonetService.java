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
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.ACTION_BUILD_SCROLL;
import static com.piusvelte.sonet.Sonet.ACTION_UPDATE_SETTINGS;

import static com.piusvelte.sonet.Tokens.TWITTER_KEY;
import static com.piusvelte.sonet.Tokens.TWITTER_SECRET;
import static com.piusvelte.sonet.Tokens.MYSPACE_KEY;
import static com.piusvelte.sonet.Tokens.MYSPACE_SECRET;

import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Tokens.BUZZ_KEY;
import static com.piusvelte.sonet.Tokens.BUZZ_SECRET;

import static com.piusvelte.sonet.Sonet.TWITTER_FEED;
import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.BUZZ_BASE_URL;

//import static com.piusvelte.sonet.Sonet.SALESFORCE;
//import static com.piusvelte.sonet.Tokens.SALESFORCE_KEY;
//import static com.piusvelte.sonet.Tokens.SALESFORCE_SECRET;
//import static com.piusvelte.sonet.Sonet.SALESFORCE_FEED;

import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;

import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_BASE_URL;

import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Tokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.Tokens.LINKEDIN_SECRET;
import static com.piusvelte.sonet.Sonet.LINKEDIN_HEADERS;
import static com.piusvelte.sonet.Sonet.LINKEDIN_BASE_URL;
import static com.piusvelte.sonet.Sonet.LINKEDIN_UPDATETYPES;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.client.ClientProtocolException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.Sonet.Accounts;
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
	private long mCurrentTimeMillis;
	private int mTimezoneOffset = 0;
	private HashMap<Integer, ArrayList<GetStatusesTask>> mWidgetsTasks = new HashMap<Integer, ArrayList<GetStatusesTask>>();
	private AppWidgetManager mAppWidgetManager;
	private AlarmManager mAlarmManager;
	private ConnectivityManager mConnectivityManager;

	@Override
	public void onCreate() {
		super.onCreate();
		mAppWidgetManager = AppWidgetManager.getInstance(this);
		mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mCurrentTimeMillis = System.currentTimeMillis();
		mTimezoneOffset = (int) ((TimeZone.getDefault()).getOffset(mCurrentTimeMillis) * 3600000);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent != null) {
			if (intent.getAction() != null) {
				if (intent.getAction().equals(ACTION_REFRESH)) {
					if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) SonetService.updateWidgets(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
				} else if (intent.getAction().equals(ACTION_UPDATE_SETTINGS)) {
					if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) SonetService.updateSettingsWidgets(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));					
				} else SonetService.updateWidgets(new int[] {Integer.parseInt(intent.getAction())});
			} else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) SonetService.updateWidgets(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
			else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) SonetService.updateWidgets(new int[]{intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)});
		}
		synchronized (sLock) {
			boolean hasConnection = (mConnectivityManager.getActiveNetworkInfo() != null) && mConnectivityManager.getActiveNetworkInfo().isConnected(),
			full_refresh = true;
			while (updatesQueued() || updateSettingsQueued()) {
				// first handle deletes, then scroll updates, finally regular updates
				int appWidgetId;
				if (updatesQueued()) appWidgetId = getNextUpdate();
				else {
					appWidgetId = getNextUpdateSettings();
					full_refresh = false;
				}
				ArrayList<GetStatusesTask> statusesTasks = new ArrayList<GetStatusesTask>();
				mWidgetsTasks.put(appWidgetId, statusesTasks);
				if (full_refresh) mAlarmManager.cancel(PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setAction(Integer.toString(appWidgetId)), 0));
				Cursor settings = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID}, Widgets.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)}, null);
				if (!settings.moveToFirst()) {
					// upgrade, moving settings from sharedpreferences to db, or initialize settings
					SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
					ContentValues values = new ContentValues();
					values.put(Widgets.INTERVAL, Integer.parseInt((String) sp.getString(getString(R.string.key_interval), Integer.toString(Sonet.default_interval))));
					values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
					values.put(Widgets.HASBUTTONS, sp.getBoolean(getString(R.string.key_display_buttons), true));
					values.put(Widgets.BUTTONS_BG_COLOR, Integer.parseInt(sp.getString(getString(R.string.key_head_background), Integer.toString(Sonet.default_buttons_bg_color))));
					values.put(Widgets.BUTTONS_COLOR, Integer.parseInt(sp.getString(getString(R.string.key_head_text), Integer.toString(Sonet.default_buttons_color))));
					values.put(Widgets.BUTTONS_TEXTSIZE, Integer.parseInt(sp.getString(getString(R.string.key_buttons_textsize), Integer.toString(Sonet.default_buttons_textsize))));
					values.put(Widgets.WIDGET, appWidgetId);
					values.put(Widgets.ICON, true);
					values.put(Widgets.MESSAGES_BG_COLOR, Sonet.default_message_bg_color);
					values.put(Widgets.MESSAGES_COLOR, Sonet.default_message_color);
					values.put(Widgets.MESSAGES_TEXTSIZE, Sonet.default_messages_textsize);
					values.put(Widgets.FRIEND_COLOR, Sonet.default_friend_color);
					values.put(Widgets.FRIEND_TEXTSIZE, Sonet.default_friend_textsize);
					values.put(Widgets.CREATED_COLOR, Sonet.default_created_color);
					values.put(Widgets.CREATED_TEXTSIZE, Sonet.default_created_textsize);
					values.put(Widgets.TIME24HR, false);
					values.put(Widgets.STATUSES_PER_ACCOUNT, Sonet.default_statuses_per_account);
					this.getContentResolver().insert(Widgets.CONTENT_URI, values);
				}
				settings.close();
				// if not a full_refresh, connection is irrelevant
				if (!full_refresh || hasConnection) {
					// query accounts
					/* get statuses for all accounts
					 * then sort them by datetime, descending
					 */
					int status_bg_color = -1;
					byte[] status_bg;
					Cursor accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.USERNAME, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE, Accounts.EXPIRY}, Accounts.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)}, null);
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
								this.getContentResolver().insert(Accounts.CONTENT_URI, values);
								accounts.moveToNext();
							}
						}
						this.getContentResolver().delete(Accounts.CONTENT_URI, Accounts._ID + "=?", new String[]{""});
					}
					if (accounts.moveToFirst()) {
						// load the updates
						int iaccountid = accounts.getColumnIndex(Accounts._ID),
						iservice = accounts.getColumnIndex(Accounts.SERVICE),
						itoken = accounts.getColumnIndex(Accounts.TOKEN),
						isecret = accounts.getColumnIndex(Accounts.SECRET);
						while (!accounts.isAfterLast()) {
							int accountId = accounts.getInt(iaccountid),
							service = accounts.getInt(iservice);
							// if not a full_refresh, only update the status_bg and icons
							if (full_refresh) {
								GetStatusesTask task = new GetStatusesTask(appWidgetId, accountId, service);
								statusesTasks.add(task);
								task.execute(accounts.getString(itoken), accounts.getString(isecret));
							} else {
								// update the bg and icon only
								// no tasks will be created, and the widget will be updated below
								boolean icon = true;
								Cursor c = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.MESSAGES_BG_COLOR, Widgets.ICON}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(appWidgetId), Integer.toString(accountId)}, null);
								if (c.moveToFirst()) {
									status_bg_color = c.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
									icon = c.getInt(c.getColumnIndex(Widgets.ICON)) == 1;
								} else {
									Cursor d = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.MESSAGES_BG_COLOR, Widgets.ICON}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(appWidgetId), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
									if (d.moveToFirst()) {
										status_bg_color = d.getInt(d.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
										icon = d.getInt(d.getColumnIndex(Widgets.ICON)) == 1;
									} else {
										Cursor e = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.MESSAGES_BG_COLOR, Widgets.ICON}, Widgets.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
										if (e.moveToFirst()) {
											status_bg_color = e.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
											icon = e.getInt(e.getColumnIndex(Widgets.ICON)) == 1;
										} else {
											status_bg_color = Sonet.default_message_bg_color;
											icon = true;
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
								values.put(Statuses.ICON, icon ? getBlob(BitmapFactory.decodeResource(getResources(), map_icons[service])) : null);
								this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(appWidgetId), Integer.toString(service), Integer.toString(accountId)});
							}
							accounts.moveToNext();
						}
					} else this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)}); // no accounts, clear cache
					accounts.close();
				}
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
	private static Queue<Integer> sAppWidgetIds = new LinkedList<Integer>();

	public static void updateWidgets(int[] appWidgetIds) {
		synchronized (sLock) {
			for (int appWidgetId : appWidgetIds) {
				if (!sAppWidgetIds.contains(appWidgetId)) sAppWidgetIds.add(appWidgetId);
			}
		}
	}

	private static boolean updatesQueued() {
		synchronized (sLock) {
			return !sAppWidgetIds.isEmpty();
		}
	}

	private static int getNextUpdate() {
		synchronized (sLock) {
			if (sAppWidgetIds.peek() == null) return AppWidgetManager.INVALID_APPWIDGET_ID;
			else return sAppWidgetIds.poll();
		}
	}

	private static Object sUpdateSettingsLock = new Object();
	private static Queue<Integer> sUpdateSettingsAppWidgetIds = new LinkedList<Integer>();

	public static void updateSettingsWidgets(int[] appWidgetIds) {
		synchronized (sUpdateSettingsLock) {
			for (int appWidgetId : appWidgetIds) sUpdateSettingsAppWidgetIds.add(appWidgetId);
		}
	}

	private static boolean updateSettingsQueued() {
		synchronized (sUpdateSettingsLock) {
			return !sUpdateSettingsAppWidgetIds.isEmpty();
		}
	}

	private static int getNextUpdateSettings() {
		synchronized (sUpdateSettingsLock) {
			if (sUpdateSettingsAppWidgetIds.peek() == null) return AppWidgetManager.INVALID_APPWIDGET_ID;
			else return sUpdateSettingsAppWidgetIds.poll();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private Date parseDate(String date, String format) {
		SimpleDateFormat msformat = new SimpleDateFormat(format);
		Date created;
		try {
			created = msformat.parse(date);
		} catch (ParseException e) {
			created = new Date();
			Log.e(TAG,e.toString()); //Sun Mar 13 01:34:20 +0000 2011
		}
		return parseDate(created.getTime());
	}

	private Date parseDate(long epoch) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(epoch);
		cal.add(Calendar.MILLISECOND, mTimezoneOffset);
		return cal.getTime();		
	}

	private ContentValues statusItem(long created, String link, String friend, byte[] profile, String message, int service, String createdText, int appWidgetId, int accountId) {
		ContentValues values = new ContentValues();
		values.put(Statuses.CREATED, created);
		values.put(Statuses.LINK, link);
		values.put(Statuses.FRIEND, friend);
		values.put(Statuses.PROFILE, profile);
		values.put(Statuses.MESSAGE, message);
		values.put(Statuses.SERVICE, service);
		values.put(Statuses.CREATEDTEXT, createdText);
		values.put(Statuses.WIDGET, appWidgetId);
		values.put(Statuses.ACCOUNT, accountId);
		return values;
	}

	private String getCreatedText(Date created, boolean time24hr) {
		return mCurrentTimeMillis - created.getTime() < 86400000 ?
				(time24hr ?
						String.format("%d:%02d", created.getHours(), created.getMinutes())
						: String.format("%d:%02d%s", created.getHours() < 13 ? created.getHours() : created.getHours() - 12, created.getMinutes(), getString(created.getHours() < 13 ? R.string.am : R.string.pm)))
						: String.format("%s %d", getResources().getStringArray(R.array.months)[created.getMonth()], created.getDate());
	}

	private byte[] getBlob(Bitmap bmp) {
		ByteArrayOutputStream blob = new ByteArrayOutputStream();
		if (bmp != null) bmp.compress(Bitmap.CompressFormat.PNG, 100, blob);
		return blob.toByteArray();		
	}

	private byte[] getProfile(String url) {
		Bitmap profile = null;
		// get profile
		try {
			profile = BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
		} catch (IOException e) {
			Log.e(TAG,e.getMessage());
		}
		return getBlob(profile);
	}

	private void checkWidgetUpdateReady(int widget) {
		// see if the tasks are finished
		boolean widgetUpdateReady = mWidgetsTasks.isEmpty() || !mWidgetsTasks.containsKey(widget);
		if (!widgetUpdateReady) {
			ArrayList<GetStatusesTask> tasks = mWidgetsTasks.get(widget);
			if (tasks.isEmpty()) widgetUpdateReady = true;
			else {
				Iterator<GetStatusesTask> itr = tasks.iterator();
				while (itr.hasNext() && !widgetUpdateReady) widgetUpdateReady = itr.next().getStatus() == AsyncTask.Status.FINISHED;
			}
		}
		if (widgetUpdateReady) {
			boolean hasbuttons = true;
			int refreshInterval = Sonet.default_interval,
			buttons_bg_color = Sonet.default_buttons_bg_color,
			buttons_color = Sonet.default_buttons_color,
			buttons_textsize = Sonet.default_buttons_textsize;
			Cursor settings = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.INTERVAL, Widgets.HASBUTTONS, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_TEXTSIZE}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(widget), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
			if (settings.moveToFirst()) {
				refreshInterval = settings.getInt(settings.getColumnIndex(Widgets.INTERVAL));
				hasbuttons = settings.getInt(settings.getColumnIndex(Widgets.HASBUTTONS)) == 1;
				buttons_bg_color = settings.getInt(settings.getColumnIndex(Widgets.BUTTONS_BG_COLOR));
				buttons_color = settings.getInt(settings.getColumnIndex(Widgets.BUTTONS_COLOR));
				buttons_textsize = settings.getInt(settings.getColumnIndex(Widgets.BUTTONS_TEXTSIZE));
			}
			settings.close();
			Cursor accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID}, Accounts.WIDGET + "=?", new String[]{Integer.toString(widget)}, null);
			boolean hasAccount = accounts.moveToFirst();
			accounts.close();
			// race condition when finished configuring, the service starts.
			// meanwhile, the launcher broadcasts READY and the listview is created. it's at this point that the widget is marked scrollable
			// this run finishes after the listview is created, but is not flagged as scrollable and replaces the listview with the regular widget
			boolean scrollable = false;
			settings = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.SCROLLABLE}, Widgets.WIDGET + "=?", new String[]{Integer.toString(widget)}, null);
			if (settings.moveToFirst()) scrollable = settings.getInt(settings.getColumnIndex(Widgets.SCROLLABLE)) == 1;
			settings.close();
			// Push update for this widget to the home screen
			RemoteViews views = new RemoteViews(getPackageName(), hasbuttons ? R.layout.widget : R.layout.widget_nobuttons);
			if (hasbuttons) {
				Bitmap buttons_bg = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas buttons_bg_canvas = new Canvas(buttons_bg);
				buttons_bg_canvas.drawColor(buttons_bg_color);
				views.setImageViewBitmap(R.id.buttons_bg, buttons_bg);
				views.setTextColor(R.id.buttons_bg_clear, buttons_bg_color);
				views.setFloat(R.id.buttons_bg_clear, "setTextSize", buttons_textsize);
				views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this, 0, new Intent(this, PostDialog.class), 0));
				views.setTextColor(R.id.button_post, buttons_color);
				views.setFloat(R.id.button_post, "setTextSize", buttons_textsize);
				views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(this, 0, new Intent(this, ManageAccounts.class).setAction(Integer.toString(widget)), 0));
				views.setTextColor(R.id.button_configure, buttons_color);
				views.setFloat(R.id.button_configure, "setTextSize", buttons_textsize);
				views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setAction(Integer.toString(widget)), 0));
				views.setTextColor(R.id.button_refresh, buttons_color);
				views.setFloat(R.id.button_refresh, "setTextSize", buttons_textsize);
			}
			int[] map_message = {R.id.message0, R.id.message1, R.id.message2, R.id.message3, R.id.message4, R.id.message5, R.id.message6, R.id.message7, R.id.message8, R.id.message9, R.id.message10, R.id.message11, R.id.message12, R.id.message13, R.id.message14, R.id.message15};
			Cursor statuses_styles = this.getContentResolver().query(Uri.withAppendedPath(Statuses_styles.CONTENT_URI, Integer.toString(widget)), new String[]{Statuses_styles._ID, Statuses_styles.CREATED, Statuses_styles.LINK, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.SERVICE, Statuses_styles.CREATEDTEXT, Statuses_styles.WIDGET, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON}, null, null, Statuses_styles.CREATED + " desc");
			if (statuses_styles.moveToFirst()) {
				if (!scrollable) {
					int[] map_item = {R.id.item0, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7, R.id.item8, R.id.item9, R.id.item10, R.id.item11, R.id.item12, R.id.item13, R.id.item14, R.id.item15},
					map_profile = {R.id.profile0, R.id.profile1, R.id.profile2, R.id.profile3, R.id.profile4, R.id.profile5, R.id.profile6, R.id.profile7, R.id.profile8, R.id.profile9, R.id.profile10, R.id.profile11, R.id.profile12, R.id.profile13, R.id.profile14, R.id.profile15},
					map_screenname = {R.id.friend0, R.id.friend1, R.id.friend2, R.id.friend3, R.id.friend4, R.id.friend5, R.id.friend6, R.id.friend7, R.id.friend8, R.id.friend9, R.id.friend10, R.id.friend11, R.id.friend12, R.id.friend13, R.id.friend14, R.id.friend15},
					map_created = {R.id.created0, R.id.created1, R.id.created2, R.id.created3, R.id.created4, R.id.created5, R.id.created6, R.id.created7, R.id.created8, R.id.created9, R.id.created10, R.id.created11, R.id.created12, R.id.created13, R.id.created14, R.id.created15},
					map_status_bg = {R.id.status_bg0, R.id.status_bg1, R.id.status_bg2, R.id.status_bg3, R.id.status_bg4, R.id.status_bg5, R.id.status_bg6, R.id.status_bg7, R.id.status_bg8, R.id.status_bg9, R.id.status_bg10, R.id.status_bg11, R.id.status_bg12, R.id.status_bg13, R.id.status_bg14, R.id.status_bg15},
					map_friend_bg_clear = {R.id.friend_bg_clear0, R.id.friend_bg_clear1, R.id.friend_bg_clear2, R.id.friend_bg_clear3, R.id.friend_bg_clear4, R.id.friend_bg_clear5, R.id.friend_bg_clear6, R.id.friend_bg_clear7, R.id.friend_bg_clear8, R.id.friend_bg_clear9, R.id.friend_bg_clear10, R.id.friend_bg_clear11, R.id.friend_bg_clear12, R.id.friend_bg_clear13, R.id.friend_bg_clear14, R.id.friend_bg_clear15},
					map_message_bg_clear = {R.id.message_bg_clear0, R.id.message_bg_clear1, R.id.message_bg_clear2, R.id.message_bg_clear3, R.id.message_bg_clear4, R.id.message_bg_clear5, R.id.message_bg_clear6, R.id.message_bg_clear7, R.id.message_bg_clear8, R.id.message_bg_clear9, R.id.message_bg_clear10, R.id.message_bg_clear11, R.id.message_bg_clear12, R.id.message_bg_clear13, R.id.message_bg_clear14, R.id.message_bg_clear15},
					map_icon = {R.id.icon0, R.id.icon1, R.id.icon2, R.id.icon3, R.id.icon4, R.id.icon5, R.id.icon6, R.id.icon7, R.id.icon8, R.id.icon9, R.id.icon10, R.id.icon11, R.id.icon12, R.id.icon13, R.id.icon14, R.id.icon15};
					int count_status = 0;
					int ilink = statuses_styles.getColumnIndex(Statuses_styles.LINK),
					iprofile = statuses_styles.getColumnIndex(Statuses_styles.PROFILE),
					ifriend = statuses_styles.getColumnIndex(Statuses_styles.FRIEND),
					imessage = statuses_styles.getColumnIndex(Statuses_styles.MESSAGE),
					iservice = statuses_styles.getColumnIndex(Statuses_styles.SERVICE),
					icreatedText = statuses_styles.getColumnIndex(Statuses_styles.CREATEDTEXT),
					istatus_bg = statuses_styles.getColumnIndex(Statuses_styles.STATUS_BG),
					iicon = statuses_styles.getColumnIndex(Statuses_styles.ICON);
					while (!statuses_styles.isAfterLast() && (count_status < map_item.length)) {
						int friend_color = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.FRIEND_COLOR)),
						created_color = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.CREATED_COLOR)),
						friend_textsize = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.FRIEND_TEXTSIZE)),
						created_textsize = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.CREATED_TEXTSIZE)),
						messages_color = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.MESSAGES_COLOR)),
						messages_textsize = statuses_styles.getInt(statuses_styles.getColumnIndex(Statuses_styles.MESSAGES_TEXTSIZE));
						// set icons
						byte[] icon = statuses_styles.getBlob(iicon);
						views.setImageViewBitmap(map_icon[count_status], BitmapFactory.decodeByteArray(icon, 0, icon.length));
						views.setTextViewText(map_friend_bg_clear[count_status], statuses_styles.getString(ifriend));
						views.setFloat(map_friend_bg_clear[count_status], "setTextSize", friend_textsize);
						views.setTextViewText(map_message_bg_clear[count_status], statuses_styles.getString(imessage));
						views.setFloat(map_message_bg_clear[count_status], "setTextSize", messages_textsize);
						// set messages background
						byte[] status_bg = statuses_styles.getBlob(istatus_bg);
						views.setImageViewBitmap(map_status_bg[count_status], BitmapFactory.decodeByteArray(status_bg, 0, status_bg.length));
						views.setTextViewText(map_message[count_status], statuses_styles.getString(imessage));
						views.setTextColor(map_message[count_status], messages_color);
						views.setFloat(map_message[count_status], "setTextSize", messages_textsize);
						// if no buttons, use StatusDialog.java with options for Config and Refresh
						String url = statuses_styles.getString(ilink);
						if (hasbuttons && (url != null)) views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, new Intent(Intent.ACTION_VIEW, Uri.parse(url)), 0));
						else views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, new Intent(this, StatusDialog.class).setAction(widget+"`"+statuses_styles.getInt(iservice)+"`"+statuses_styles.getString(ilink)), 0));
						views.setTextViewText(map_screenname[count_status], statuses_styles.getString(ifriend));
						views.setTextColor(map_screenname[count_status], friend_color);
						views.setFloat(map_screenname[count_status], "setTextSize", friend_textsize);
						views.setTextViewText(map_created[count_status], statuses_styles.getString(icreatedText));
						views.setTextColor(map_created[count_status], created_color);
						views.setFloat(map_created[count_status], "setTextSize", created_textsize);
						byte[] profile = statuses_styles.getBlob(iprofile);
						if (profile != null) views.setImageViewBitmap(map_profile[count_status], BitmapFactory.decodeByteArray(profile, 0, profile.length));						
						count_status++;
						statuses_styles.moveToNext();
					}
				}
			} else {
				// no connect or account
				views.setTextViewText(map_message[0], getString(hasAccount ? (mConnectivityManager.getActiveNetworkInfo() != null) && mConnectivityManager.getActiveNetworkInfo().isConnected() ? R.string.no_updates : R.string.no_connection : R.string.no_accounts));
				views.setTextColor(map_message[0], Sonet.default_message_color);
				views.setFloat(map_message[0], "setTextSize", Sonet.default_messages_textsize);
			}
			statuses_styles.close();
			mAppWidgetManager.updateAppWidget(widget, views);
			// replace with scrollable widget
			if (scrollable) sendBroadcast(new Intent(this, SonetWidget.class).setAction(ACTION_BUILD_SCROLL).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget));
			if (hasAccount && (refreshInterval > 0)) mAlarmManager.set(AlarmManager.RTC_WAKEUP, mCurrentTimeMillis + refreshInterval, PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setAction(Integer.toString(widget)), 0));			
		}		
	}

	class GetStatusesTask extends AsyncTask<String, Void, String> {
		private int widget;
		private int account;
		private int service;
		private boolean time24hr,
		icon;
		private int status_bg_color = -1,
		status_count;

		public GetStatusesTask(int widget, int account, int service) {
			// get this account's statuses
			// get the settings form time24hr and bg_color
			this.widget = widget;
			this.account = account;
			this.service = service;
			Cursor c = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(widget), Integer.toString(account)}, null);
			if (c.moveToFirst()) {
				time24hr = c.getInt(c.getColumnIndex(Widgets.TIME24HR)) == 1;
				status_bg_color = c.getInt(c.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
				icon = c.getInt(c.getColumnIndex(Widgets.ICON)) == 1;
				status_count = c.getInt(c.getColumnIndex(Widgets.STATUSES_PER_ACCOUNT));
			} else {
				Cursor d = SonetService.this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(widget), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
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
		}

		@Override
		protected String doInBackground(String... params) {
			SonetOAuth sonetOAuth;
			try {
				switch (service) {
				case TWITTER:
					sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, params[0], params[1]);
					return sonetOAuth.httpGet(TWITTER_FEED + status_count);
				case FACEBOOK:
					return Sonet.httpGet(FACEBOOK_BASE_URL + "me/home?date_format=U&format=json&sdk=android&limit=" + status_count + "&" + TOKEN + "=" + params[0] + "&fields=actions,link,type,from,message,created_time,to");
				case MYSPACE:
					sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, params[0], params[1]);
					return sonetOAuth.httpGet(MYSPACE_BASE_URL + "statusmood/@me/@friends/history?count=" + status_count + "&includeself=true&fields=author,source");
				case BUZZ:
					sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, params[0], params[1]);
					return sonetOAuth.httpGet(BUZZ_BASE_URL + "activities/@me/@consumption?alt=json&max-results=" + status_count);
					//							case SALESFORCE:
					//								sonetOAuth = new SonetOAuth(SALESFORCE_KEY, SALESFORCE_SECRET, token, secret);
					//								return sonetOAuth.httpGet(SALESFORCE_FEED);
				case FOURSQUARE:
					return Sonet.httpGet(FOURSQUARE_BASE_URL + "checkins/recent?limit=" + status_count + "&oauth_token=" + params[0]);
				case LINKEDIN:
					sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, params[0], params[1]);
					return sonetOAuth.httpGetWithHeaders(LINKEDIN_BASE_URL + "/network/updates?type=APPS&type=CMPY&type=CONN&type=JOBS&type=JGRP&type=PICT&type=PRFU&type=RECU&type=PRFX&type=ANSW&type=QSTN&type=SHAR&type=VIRL&count=" + status_count, LINKEDIN_HEADERS);
				}
			} catch (ClientProtocolException e) {
				Log.e(TAG,e.toString());
			} catch (OAuthMessageSignerException e) {
				Log.e(TAG,e.toString());
			} catch (OAuthExpectationFailedException e) {
				Log.e(TAG,e.toString());
			} catch (OAuthCommunicationException e) {
				Log.e(TAG,e.toString());
			} catch (IOException e) {
				Log.e(TAG,e.toString());
			}
			return null;
		}

		@Override
		protected void onPostExecute(String response) {
			// parse the response
			if (response != null) {
				String name = "name",
				id = "id",
				status = "status";
				// create the status_bg
				Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas status_bg_canvas = new Canvas(status_bg_bmp);
				status_bg_canvas.drawColor(status_bg_color);
				ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
				status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
				byte[] status_bg = status_bg_blob.toByteArray();
				// if not a full_refresh, only update the status_bg and icons
				switch (service) {
				case TWITTER:
					String status_url = "http://twitter.com/%s/status/%s";
					try {
						JSONArray entries = new JSONArray(response);
						// if there are updates, clear the cache
						if (entries.length() > 0) SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(widget), Integer.toString(service), Integer.toString(account)});
						for (int e = 0; e < entries.length(); e++) {
							JSONObject entry = entries.getJSONObject(e);
							JSONObject user = entry.getJSONObject("user");
							Date created = parseDate(entry.getString("created_at"), "EEE MMM dd HH:mm:ss z yyyy");
							SonetService.this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(created.getTime(),
									String.format(status_url, user.getString("screen_name"), Long.toString(entry.getLong("id"))),
									user.getString("name"),
									getProfile(user.getString("profile_image_url")),
									entry.getString("text"),
									service,
									getCreatedText(created, time24hr),
									widget,
									account));
						}
					} catch (JSONException e) {
						Log.e(TAG,e.toString());
					}
					break;
				case FACEBOOK:
					String created_time = "created_time",
					actions = "actions",
					link = "link",
					comment = "Comment",
					from = "from",
					type = "type",
					profile = "http://graph.facebook.com/%s/picture",
					message = "message",
					data = "data",
					to = "to",
					fburl = "http://www.facebook.com";
					try {
						JSONObject jobj = new JSONObject(response);
						JSONArray jarr = jobj.getJSONArray(data);
						// if there are updates, clear the cache
						if (jarr.length() > 0) SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(widget), Integer.toString(service), Integer.toString(account)});
						for (int d = 0; d < jarr.length(); d++) {
							JSONObject o = jarr.getJSONObject(d);
							// only parse status types, not photo, video or link
							if (o.has(type) && o.has(from) && o.has(message)) {
								// parse the link
								String l = fburl;
								if (o.has(actions)) {											
									JSONArray action = o.getJSONArray(actions);
									for (int a = 0; a < action.length(); a++) {
										JSONObject n = action.getJSONObject(a);
										if (n.has(name) && n.getString(name) == comment) {
											l = n.getString(link);
											break;
										}
									}
								}
								JSONObject f = o.getJSONObject(from);
								if (f.has(name) && f.has(id)) {
									String friend = f.getString(name);
									if (o.has(to)) {
										// handle wall messages from one friend to another
										JSONObject t = o.getJSONObject(to);
										if (t.has(data)) {
											JSONObject n = t.getJSONArray(data).getJSONObject(0);
											if (n.has(name)) friend += " > " + n.getString(name);
										}												
									}
									Date created = parseDate(Long.parseLong(o.getString(created_time)) * 1000);
									SonetService.this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(
											created.getTime(),
											l,
											friend,
											getProfile(String.format(profile, f.getString(id))),
											o.getString(message),
											service,
											getCreatedText(created, time24hr),
											widget,
											account));
								}
							}
						}
					} catch (JSONException e) {
						Log.e(TAG, e.toString());
					}
					break;
				case MYSPACE:
					String displayName = "displayName",
					moodStatusLastUpdated = "moodStatusLastUpdated",
					thumbnailUrl = "thumbnailUrl",
					source = "source",
					url = "url",
					author = "author";
					try {
						JSONObject jobj = new JSONObject(response);
						JSONArray entries = jobj.getJSONArray("entry");
						// if there are updates, clear the cache
						if (entries.length() > 0) SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(widget), Integer.toString(service), Integer.toString(account)});
						for (int e = 0; e < entries.length(); e++) {
							JSONObject entry = entries.getJSONObject(e);
							JSONObject authorObj = entry.getJSONObject(author);
							Date created = parseDate(entry.getString(moodStatusLastUpdated), "yyyy-MM-dd'T'HH:mm:ss'Z'");
							SonetService.this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(created.getTime(),
									entry.getJSONObject(source).getString(url),
									authorObj.getString(displayName),
									getProfile(authorObj.getString(thumbnailUrl)),
									entry.getString(status),
									service,
									getCreatedText(created, time24hr),
									widget,
									account));
						}
					} catch (JSONException e) {
						Log.e(TAG, e.toString());
					}
					break;
				case BUZZ:
					try {
						JSONArray entries = new JSONObject(response).getJSONObject("data").getJSONArray("items");
						// if there are updates, clear the cache
						if (entries.length() > 0) SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(widget), Integer.toString(service), Integer.toString(account)});
						for (int e = 0; e < entries.length(); e++) {
							JSONObject entry = entries.getJSONObject(e);
							if (entry.has("published") && entry.has("actor") && entry.has("object")) {
								Date created = parseDate(entry.getString("published"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
								JSONObject actor = entry.getJSONObject("actor");
								JSONObject object = entry.getJSONObject("object");
								if (actor.has("name") && actor.has("thumbnailUrl") && object.has("originalContent")) {
									link = "http://www.google.com/buzz";
									if (object.has("links") && object.getJSONObject("links").has("alternate")) {
										JSONArray links = object.getJSONObject("links").getJSONArray("alternate");
										if (links.length() > 0) link = links.getJSONObject(0).getString("href");
									}
									SonetService.this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(created.getTime(),
											link,
											actor.getString("name"),
											getProfile(actor.getString("thumbnailUrl")),
											object.getString("originalContent"),
											service,
											getCreatedText(created, time24hr),
											widget,
											account));
								}
							}
						}
					} catch (JSONException e) {
						Log.e(TAG,e.toString());
					}
					break;
					//							case SALESFORCE:
					//								try {
					//										JSONArray entries = new JSONObject(response).getJSONObject("data").getJSONArray("items");
					//										// if there are updates, clear the cache
					//										if (entries.length() > 0) this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(widget), Integer.toString(service), Integer.toString(account)});
					//										for (int e = 0; e < entries.length(); e++) {
					//											JSONObject entry = entries.getJSONObject(e);
					//											if (entry.has("published") && entry.has("actor") && entry.has("object")) {
					//												Date created = parseDate(entry.getString("published"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", accounts.getDouble(itimezone));
					//												JSONObject actor = entry.getJSONObject("actor");
					//												JSONObject object = entry.getJSONObject("object");
					//												if (actor.has("name") && actor.has("thumbnailUrl") && object.has("originalContent")) {
					//													this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(created.getTime(),
					//															object.has("links") && object.getJSONObject("links").has("alternate") ? object.getJSONObject("links").getString("alternate") : "",
					//																	actor.getString("name"),
					//																	getProfile(actor.getString("thumbnailUrl")),
					//																	object.getString("originalContent"),
					//																	service,
					//																	getCreatedText(now, created, time24hr),
					//																	widget,
					//																	account));
					//												}
					//											}
					//										}
					//								} catch (ClientProtocolException e) {
					//									Log.e(TAG,e.toString());
					//								} catch (OAuthMessageSignerException e) {
					//									Log.e(TAG,e.toString());
					//								} catch (OAuthExpectationFailedException e) {
					//									Log.e(TAG,e.toString());
					//								} catch (OAuthCommunicationException e) {
					//									Log.e(TAG,e.toString());
					//								} catch (IOException e) {
					//									Log.e(TAG,e.toString());
					//								} catch (JSONException e) {
					//									Log.e(TAG,e.toString());
					//								}
					//								break;
				case FOURSQUARE:
					try {
						JSONArray checkins = new JSONObject(response).getJSONObject("response").getJSONArray("recent");
						// if there are updates, clear the cache
						if (checkins.length() > 0) SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(widget), Integer.toString(service), Integer.toString(account)});
						for (int e = 0; e < checkins.length(); e++) {
							JSONObject checkin = checkins.getJSONObject(e);
							JSONObject user = checkin.getJSONObject("user");
							JSONObject venue = checkin.getJSONObject("venue");
							String shout = (checkin.has("shout") ? checkin.getString("shout") + "\n" : "") + "@" + venue.getString("name");
							Date created = parseDate(Long.parseLong(checkin.getString("createdAt")) * 1000);
							SonetService.this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(created.getTime(),
									"http://www.foursquare.com",
									user.getString("firstName") + " " + user.getString("lastName"),
									getProfile(user.getString("photo")),
									shout,
									service,
									getCreatedText(created, time24hr),
									widget,
									account));
						}
					} catch (JSONException e) {
						Log.e(TAG,e.toString());
					}
					break;
				case LINKEDIN:
					try {
						JSONObject jobj = new JSONObject(response);
						JSONArray values = jobj.getJSONArray("values");
						// if there are updates, clear the cache
						if (values.length() > 0) SonetService.this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(widget), Integer.toString(service), Integer.toString(account)});
						for (int e = 0; e < values.length(); e++) {
							JSONObject value = values.getJSONObject(e);
							String updateType = value.getString("updateType");
							JSONObject updateContent = value.getJSONObject("updateContent");
							if (LINKEDIN_UPDATETYPES.containsKey(updateType) && updateContent.has("person")) {
								Date created = parseDate(Long.parseLong(value.getString("timestamp")));
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
								SonetService.this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(created.getTime(),
										"http://www.linkedin.com",
										person.getString("firstName") + " " + person.getString("lastName"),
										person.has("pictureUrl") ? getProfile(person.getString("pictureUrl")) : null,
												update,
												service,
												getCreatedText(created, time24hr),
												widget,
												account));
							}
						}
					} catch (JSONException e) {
						Log.e(TAG,e.toString());
					}
					break;
				}
				// update the bg and icon
				ContentValues values = new ContentValues();
				values.put(Statuses.STATUS_BG, status_bg);
				values.put(Statuses.ICON, icon ? getBlob(BitmapFactory.decodeResource(getResources(), map_icons[service])) : null);
				SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(widget), Integer.toString(service), Integer.toString(account)});
			} else if (service == MYSPACE) {
				// warn about myspace permissions
				// create the status_bg
				Bitmap status_bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas status_bg_canvas = new Canvas(status_bg_bmp);
				status_bg_canvas.drawColor(status_bg_color);
				ByteArrayOutputStream status_bg_blob = new ByteArrayOutputStream();
				status_bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, status_bg_blob);
				byte[] status_bg = status_bg_blob.toByteArray();
				ContentValues values = new ContentValues();
				values.put(Statuses.FRIEND, getString(R.string.myspace_permissions_title));
				values.put(Statuses.MESSAGE, getString(R.string.myspace_permissions_message));
				values.put(Statuses.SERVICE, service);
				values.put(Statuses.WIDGET, widget);
				values.put(Statuses.ACCOUNT, account);
				values.put(Statuses.STATUS_BG, status_bg);
				SonetService.this.getContentResolver().insert(Statuses.CONTENT_URI, values);
			}
			// remove self from queue
			if (!mWidgetsTasks.isEmpty() && mWidgetsTasks.containsKey(widget)) mWidgetsTasks.get(widget).remove(this);
			// see if the tasks are finished
			checkWidgetUpdateReady(widget);
		}

	}
}