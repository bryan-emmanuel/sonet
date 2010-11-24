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

import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import static com.piusvelte.sonet.SonetDatabaseHelper.USERNAME;
import static com.piusvelte.sonet.SonetDatabaseHelper.SECRET;
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TOKEN;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.SonetDatabaseHelper.EXPIRY;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIMEZONE;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.HASBUTTONS;
import static com.piusvelte.sonet.SonetDatabaseHelper.INTERVAL;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIME24HR;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_STATUSES;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATEDTEXT;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND;
import static com.piusvelte.sonet.SonetDatabaseHelper.PROFILE;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGE;
import static com.piusvelte.sonet.SonetDatabaseHelper.LINK;
import static com.piusvelte.sonet.Sonet.TAG;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Services.TWITTER_KEY;
import static com.piusvelte.sonet.Services.TWITTER_SECRET;
import static com.piusvelte.sonet.Services.MYSPACE_KEY;
import static com.piusvelte.sonet.Services.MYSPACE_SECRET;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
//import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.signature.SignatureMethod;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class SonetService extends Service implements Runnable {
	private BroadcastReceiver mReceiver;
	private Thread sThread;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent != null) {
			if (intent.getAction() == ACTION_REFRESH) ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getBroadcast(this, 0, new Intent(this, SonetService.class).setAction(ACTION_REFRESH), 0));
			if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) SonetService.updateWidgets(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
			else SonetService.updateWidgets(getAppWidgetIds());
		} else  SonetService.updateWidgets(getAppWidgetIds());
		synchronized (sLock) {
			if ((sThread == null) || !sThread.isAlive()) (sThread = new Thread(this)).start();
		}
	}

	private int[] getAppWidgetIds() {
		int[] appWidgetIds = null;
		SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
		SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
		Cursor accounts = db.rawQuery("select " + _ID + "," + WIDGET + " from " + TABLE_ACCOUNTS, null);
		if (accounts.getCount() > 0) {
			accounts.moveToFirst();
			appWidgetIds = new int[accounts.getCount()];
			int iwidget = accounts.getColumnIndex(WIDGET),
			counter = 0;
			while (!accounts.isAfterLast()) {
				appWidgetIds[counter] = accounts.getInt(iwidget);
				counter++;
				accounts.moveToNext();
			}
		} else appWidgetIds = new int[0];
		accounts.close();
		db.close();
		sonetDatabaseHelper.close();
		return appWidgetIds;
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
			for (int appWidgetId : appWidgetIds) sAppWidgetIds.add(appWidgetId);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't need to bind to this service
		return null;
	}

	private Date parseDate(String date, String format, int timezone) {
		SimpleDateFormat msformat = new SimpleDateFormat(format);
		Calendar cal = Calendar.getInstance();
		Date created;
		try {
			created = msformat.parse(date);
		} catch (ParseException e) {
			created = new Date();
			Log.e(TAG,e.toString());
		}
		cal.setTime(created);
		cal.add(Calendar.HOUR, timezone);
		return cal.getTime();		
	}

	private class StatusItem implements Comparable<StatusItem> {
		private Date created;
		private String link;
		private String friend;
		private URL profile;
		private String message;
		private int service;
		private String createdText;
		StatusItem(Date created, String link, String friend, URL profile, String message, int service, String createdText) {
			this.created = created;
			this.link = link;
			this.friend = friend;
			this.profile = profile;
			this.message = message;
			this.service = service;
			this.createdText = createdText;
		}

		public int compareTo(StatusItem si) {
			// sort descending
			return ((Long)si.created.getTime()).compareTo(created.getTime());
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
	
	private String getCreatedText(long now, Date created, boolean time24hr) {
		return now - created.getTime() < 86400000 ?
				(time24hr ?
						String.format("%d:%02d", created.getHours(), created.getMinutes())
						: String.format("%d:%02d%s", created.getHours() < 13 ? created.getHours() : created.getHours() - 12, created.getMinutes(), getString(created.getHours() < 13 ? R.string.am : R.string.pm)))
						: String.format("%s %d", getResources().getStringArray(R.array.months)[created.getMonth()], created.getDate());
	}

	private List<StatusItem> getStatuses(Cursor accounts, boolean time24hr) {
		List<StatusItem> status_items = new ArrayList<StatusItem>();
		accounts.moveToFirst();
		int iservice = accounts.getColumnIndex(SERVICE),
		itoken = accounts.getColumnIndex(TOKEN),
		isecret = accounts.getColumnIndex(SECRET),
		iexpiry = accounts.getColumnIndex(EXPIRY),
		itimezone = accounts.getColumnIndex(TIMEZONE);
		String name = "name",
		id = "id",
		status = "status";
		long now = new Date().getTime();
		while (!accounts.isAfterLast()) {
			int service = accounts.getInt(iservice);
			switch (service) {
			case TWITTER:
				String status_url = "http://twitter.com/%s/status/%s";
				try {
					List<Status> statuses = (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(accounts.getString(itoken), accounts.getString(isecret)))).getFriendsTimeline();
					for (Status s : statuses) {
						String screenname = s.getUser().getScreenName();
						Date created = s.getCreatedAt();
						status_items.add(new StatusItem(created,
								String.format(status_url, screenname, Long.toString(s.getId())),
								screenname,
								s.getUser().getProfileImageURL(),
								s.getText(),
								service,
								getCreatedText(now, created, time24hr)));
					}
				} catch (TwitterException te) {
					Log.e(TAG, te.toString());
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
				message = "message";
				Facebook facebook = new Facebook();
				facebook.setAccessToken(accounts.getString(itoken));
				facebook.setAccessExpires((long)accounts.getInt(iexpiry));
				try {
					JSONObject jobj = Util.parseJson(facebook.request("me/home"));
					JSONArray jarr = jobj.getJSONArray("data");
					for (int d = 0; d < jarr.length(); d++) {
						JSONObject o = jarr.getJSONObject(d);
						// only parse status types, not photo, video or link
						if (o.has(type) && o.getString(type).equals(status) && o.has(from) && o.has(message)) {
							// parse the link
							String l = "http://www.facebook.com";
							if (o.has(actions)) {											
								JSONArray action = o.getJSONArray(actions);
								for (int a = 0; a < action.length(); a++) {
									JSONObject n = action.getJSONObject(a);
									if (n.getString(name) == comment) {
										l = n.getString(link);
										break;
									}
								}
							}
							JSONObject f = o.getJSONObject(from);
							if (f.has(name) && f.has(id)) {
								Date created = parseDate(o.getString(created_time), "yyyy-MM-dd'T'HH:mm:ss'+0000'", accounts.getInt(itimezone));
								status_items.add(new StatusItem(
										created,
										l,
										f.getString(name),
										new URL(String.format(profile, f.getString(id))),
										o.getString(message),
										service,
										getCreatedText(now, created, time24hr)));
							}
						}
					}
				} catch (JSONException e) {
					Log.e(TAG, e.toString());
				} catch (FacebookError e) {
					Log.e(TAG, e.toString());
				} catch (IOException e) {
					Log.e(TAG, e.toString());
				}
				break;
			case MYSPACE:
				//								OAuthConsumer consumer = new DefaultOAuthConsumer(MYSPACE_KEY, MYSPACE_SECRET);
				String displayName = "displayName",
				moodStatusLastUpdated = "moodStatusLastUpdated",
				thumbnailUrl = "thumbnailUrl",
				source = "source",
				url = "url",
				author = "author";
				OAuthConsumer consumer = new CommonsHttpOAuthConsumer(MYSPACE_KEY, MYSPACE_SECRET, SignatureMethod.HMAC_SHA1);
				consumer.setTokenWithSecret(accounts.getString(itoken), accounts.getString(isecret));
				HttpClient client = new DefaultHttpClient();
				ResponseHandler <String> responseHandler = new BasicResponseHandler();
				HttpGet request = new HttpGet("http://opensocial.myspace.com/1.0/statusmood/@me/@friends/history?includeself=true&fields=author,source");
				try {
					consumer.sign(request);
					JSONObject jobj = new JSONObject(client.execute(request, responseHandler));
					JSONArray entries = jobj.getJSONArray("entry");
					for (int e = 0; e < entries.length(); e++) {
						JSONObject entry = entries.getJSONObject(e);
						JSONObject authorObj = entry.getJSONObject(author);
						Date created = parseDate(entry.getString(moodStatusLastUpdated), "yyyy-MM-dd'T'HH:mm:ss'Z'", accounts.getInt(itimezone));
						status_items.add(new StatusItem(created,
								entry.getJSONObject(source).getString(url),
								authorObj.getString(displayName),
								new URL(authorObj.getString(thumbnailUrl)),
								entry.getString(status),
								service,
								getCreatedText(now, created, time24hr)));
					}
				} catch (ClientProtocolException e) {
					Log.e(TAG, e.toString());
				} catch (JSONException e) {
					Log.e(TAG, e.toString());
				} catch (IOException e) {
					Log.e(TAG, e.toString());
				} catch (OAuthMessageSignerException e) {
					Log.e(TAG, e.toString());
				} catch (OAuthExpectationFailedException e) {
					Log.e(TAG, e.toString());
				}
				//								} catch (OAuthCommunicationException e) {
				//									Log.e(TAG, e.toString());
				//								}
				break;
			}
			accounts.moveToNext();
		}
		// sort statuses
		Collections.sort(status_items);
		return status_items;
	}

	@Override
	public void run() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if ((cm.getActiveNetworkInfo() != null) && cm.getActiveNetworkInfo().isConnected()) {
			if (mReceiver != null) {
				unregisterReceiver(mReceiver);
				mReceiver = null;
			}
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
			while (updatesQueued()) {
				int appWidgetId = getNextUpdate();
				SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
				SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
				Boolean hasbuttons,
				time24hr;
				int interval,
				buttons_bg_color,
				buttons_color,
				messages_bg_color,
				messages_color,
				friend_color,
				created_color;
				Cursor settings = db.rawQuery("select " + _ID + "," + INTERVAL + "," + HASBUTTONS + ","	+ BUTTONS_BG_COLOR + "," + BUTTONS_COLOR + "," + MESSAGES_BG_COLOR + "," + MESSAGES_COLOR + "," + FRIEND_COLOR + "," + CREATED_COLOR + "," + TIME24HR + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + appWidgetId, null);
				if (settings.getCount() > 0) {
					settings.moveToFirst();
					interval = settings.getInt(settings.getColumnIndex(INTERVAL));
					hasbuttons = settings.getInt(settings.getColumnIndex(HASBUTTONS)) == 1;
					buttons_bg_color = settings.getInt(settings.getColumnIndex(BUTTONS_BG_COLOR));
					buttons_color = settings.getInt(settings.getColumnIndex(BUTTONS_COLOR));
					messages_bg_color = settings.getInt(settings.getColumnIndex(MESSAGES_BG_COLOR));
					messages_color = settings.getInt(settings.getColumnIndex(MESSAGES_COLOR));
					friend_color = settings.getInt(settings.getColumnIndex(FRIEND_COLOR));
					created_color = settings.getInt(settings.getColumnIndex(CREATED_COLOR));
					time24hr = settings.getInt(settings.getColumnIndex(TIME24HR)) == 1;
				} else {
					// upgrade, moving settings from sharedpreferences to db
					SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
					interval = Integer.parseInt((String) sp.getString(getString(R.string.key_interval), getString(R.string.default_interval)));
					hasbuttons = sp.getBoolean(getString(R.string.key_display_buttons), true);
					buttons_bg_color =Integer.parseInt(sp.getString(getString(R.string.key_head_background), getString(R.string.default_buttons_bg_color)));
					buttons_color = Integer.parseInt(sp.getString(getString(R.string.key_head_text), getString(R.string.default_buttons_color)));
					messages_bg_color = Integer.parseInt(sp.getString(getString(R.string.key_body_background), getString(R.string.default_message_bg_color)));
					messages_color = Integer.parseInt(sp.getString(getString(R.string.key_body_text), getString(R.string.default_message_color)));
					friend_color = Integer.parseInt(sp.getString(getString(R.string.key_friend_text), getString(R.string.default_friend_color)));
					created_color = Integer.parseInt(sp.getString(getString(R.string.key_created_text), getString(R.string.default_created_color)));
					time24hr = sp.getBoolean(getString(R.string.key_time_12_24), false);
					ContentValues values = new ContentValues();
					values.put(INTERVAL, interval);
					values.put(HASBUTTONS, hasbuttons);
					values.put(BUTTONS_BG_COLOR, buttons_bg_color);
					values.put(BUTTONS_COLOR, buttons_color);
					values.put(MESSAGES_BG_COLOR, messages_bg_color);
					values.put(MESSAGES_COLOR, messages_color);
					values.put(FRIEND_COLOR, friend_color);
					values.put(CREATED_COLOR, created_color);
					values.put(TIME24HR, time24hr);
					values.put(WIDGET, appWidgetId);
					db.insert(TABLE_WIDGETS, _ID, values);
				}
				settings.close();
				List<StatusItem> statuses;
				// query accounts
				/* get statuses for all accounts
				 * then sort them by datetime, descending
				 */
				Cursor accounts = db.rawQuery("select " + _ID + "," + USERNAME + "," + TOKEN + "," + SECRET + "," + SERVICE + "," + EXPIRY + "," + TIMEZONE + " from " + TABLE_ACCOUNTS + " where " + WIDGET + "=" + appWidgetId, null);
				if (accounts.getCount() == 0) {
					// migrate old accounts
					Cursor c = db.rawQuery("select " + _ID + "," + USERNAME + "," + TOKEN + "," + SECRET + "," + SERVICE + "," + EXPIRY + "," + TIMEZONE + " from " + TABLE_ACCOUNTS + " where " + WIDGET + "=\"\"", null);
					if (c.getCount() > 0) statuses = getStatuses(c, time24hr);
					else statuses = new ArrayList<StatusItem>();
					c.close();
					db.delete(TABLE_ACCOUNTS, _ID + "=\"\"", null);
				} else statuses = getStatuses(accounts, time24hr);
				accounts.close();
				// Push update for this widget to the home screen
				// set messages background
				Bitmap messages_bg = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas messages_bg_canvas = new Canvas(messages_bg);
				messages_bg_canvas.drawColor(messages_bg_color);
				int[] map_item = {R.id.item0, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6},
				map_profile = {R.id.profile0, R.id.profile1, R.id.profile2, R.id.profile3, R.id.profile4, R.id.profile5, R.id.profile6},
				map_message = {R.id.message0, R.id.message1, R.id.message2, R.id.message3, R.id.message4, R.id.message5, R.id.message6},
				map_screenname = {R.id.screenname0, R.id.screenname1, R.id.screenname2, R.id.screenname3, R.id.screenname4, R.id.screenname5, R.id.screenname6},
				map_created = {R.id.created0, R.id.created1, R.id.created2, R.id.created3, R.id.created4, R.id.created5, R.id.created6};
				int count_status = 0, max_status = map_item.length;
				RemoteViews views = new RemoteViews(getPackageName(), hasbuttons ? R.layout.widget : R.layout.widget_nobuttons);
				if (hasbuttons) {
					Bitmap buttons_bg = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
					Canvas buttons_bg_canvas = new Canvas(buttons_bg);
					buttons_bg_canvas.drawColor(buttons_bg_color);
					views.setImageViewBitmap(R.id.buttons_bg, buttons_bg);
					views.setTextColor(R.id.head_spacer, buttons_bg_color);
					views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this, 0, new Intent(this, PostDialog.class).setAction(MESSAGE), 0));
					views.setTextColor(R.id.button_post, buttons_color);
					views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(this, 0, new Intent(this, UI.class).setAction(WIDGET).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId), 0));
					views.setTextColor(R.id.button_post, buttons_color);
					views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId}), 0));
					views.setTextColor(R.id.button_post, buttons_color);
				}
				views.setImageViewBitmap(R.id.messages_bg, messages_bg);
				// clear the cache
				db.execSQL("delete from " + TABLE_STATUSES + ";");
				for  (StatusItem item : statuses) {
					if (count_status < max_status) {
						// if no buttons, use StatusDialog.java with options for Config and Refresh
						if (hasbuttons) views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, new Intent(Intent.ACTION_VIEW, Uri.parse(item.link)), 0));
						else views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, new Intent(this, StatusDialog.class).setAction(appWidgetId+"`"+item.service+"`"+item.link), 0));
						views.setTextViewText(map_message[count_status], item.message);
						views.setTextColor(map_message[count_status], messages_color);
						views.setTextViewText(map_screenname[count_status], item.friend);
						views.setTextColor(map_screenname[count_status], friend_color);
						views.setTextViewText(map_created[count_status], item.createdText);
						views.setTextColor(map_created[count_status], created_color);
						try {
							views.setImageViewBitmap(map_profile[count_status], BitmapFactory.decodeStream(item.profile.openConnection().getInputStream()));
						} catch (IOException e) {
							Log.e(TAG,e.getMessage());
						}										
						count_status++;
					}
					ContentValues values = new ContentValues();
					values.put(CREATED, item.created.getTime());
					values.put(LINK, item.link);
					values.put(FRIEND, item.friend);
					values.put(PROFILE, item.profile.toString());
					values.put(MESSAGE, item.message);
					values.put(SERVICE, item.service);
					values.put(CREATEDTEXT, item.createdText);
					db.insert(TABLE_STATUSES, _ID, values);
				}
				db.close();
				sonetDatabaseHelper.close();
				appWidgetManager.updateAppWidget(appWidgetId, views);
				((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, System.currentTimeMillis() + interval, PendingIntent.getService(this, 0, (new Intent(this, SonetService.class)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId), 0));
			}
		} else if (mReceiver == null) {
			// if there's no connection, listen for one
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
						if (((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).isConnected()) {
							synchronized (sLock) {
								if ((sThread == null) || !sThread.isAlive()) (sThread = new Thread((Runnable) context)).start();
							}								
						}
					}
				}
			};
			IntentFilter f = new IntentFilter();
			f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
			registerReceiver(mReceiver, f);	
		}
		stopSelf();
	}

}
