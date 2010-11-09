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
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGE_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGE_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIME24HR;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_COLOR;
import static com.piusvelte.sonet.Sonet.TAG;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;
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
import java.util.List;

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

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

public class SonetService extends Service {
	private BroadcastReceiver mReceiver;
	private int mAppWidgetId;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		init(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStart(intent, startId);
		init(intent);
		return START_STICKY;
	}

	public void init(Intent intent) {
		if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) init(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
		else init(AppWidgetManager.INVALID_APPWIDGET_ID);
	}

	public void init(int appWidgetId) {
		mAppWidgetId = appWidgetId;
		int[] sonetWidget_4x2 = null;
		int[] sonetWidget_4x3 = null;
		int[] sonetWidget_4x4 = null;
		//		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getBroadcast(this, 0, new Intent(this, SonetService.class).setAction(REFRESH), 0));
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getBackgroundDataSetting()) {
			if ((cm.getActiveNetworkInfo() != null) && cm.getActiveNetworkInfo().isConnected()) {
				if (mReceiver != null) {
					unregisterReceiver(mReceiver);
					mReceiver = null;
				}
				if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) updateWidget(appWidgetId, 7, manager);
				else {
					sonetWidget_4x2 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x2.class));
					sonetWidget_4x3 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x3.class));
					sonetWidget_4x4 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x4.class));
					for (int i=0; i<sonetWidget_4x2.length; i++) updateWidget(sonetWidget_4x2[i], 3, manager);
					for (int i=0; i<sonetWidget_4x3.length; i++) updateWidget(sonetWidget_4x3[i], 5, manager);
					for (int i=0; i<sonetWidget_4x4.length; i++) updateWidget(sonetWidget_4x4[i], 7, manager);
				}
			} else {
				// if there's no connection, listen for one
				mReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
							if (((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).isConnected()) init(mAppWidgetId);
						}
					}
				};
				IntentFilter f = new IntentFilter();
				f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
				registerReceiver(mReceiver, f);	
			}
		}
		// if updated, then stop, otherwise wait for network connection
		if (mReceiver == null) stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't need to bind to this service
		return null;
	}

	private void updateWidget(int appWidgetId, int max_widget_items, AppWidgetManager manager) {
		SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
		SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
		Cursor settings = db.rawQuery("select " + _ID + "," + INTERVAL + "," + HASBUTTONS + ","	+ BUTTONS_BG_COLOR + "," + BUTTONS_COLOR + "," + MESSAGE_BG_COLOR + "," + MESSAGE_COLOR + "," + TIME24HR + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + appWidgetId, null);
		if (settings.getCount() > 0) {
			Boolean displayButtons = settings.getInt(settings.getColumnIndex(HASBUTTONS)) == 1;
			List<StatusItem> status_items = new ArrayList<StatusItem>();
			// query accounts
			Cursor cursor = db.rawQuery("select " + _ID + "," + USERNAME + "," + TOKEN + "," + SECRET + "," + SERVICE + "," + EXPIRY + "," + TIMEZONE + " from " + TABLE_ACCOUNTS + " where " + WIDGET + "=" + appWidgetId, null);
			if (cursor.getCount() > 0) {
				/* get statuses for all accounts
				 * then sort them by datetime, descending
				 */
				boolean use24hr = settings.getInt(settings.getColumnIndex(TIME24HR)) == 1;
				cursor.moveToFirst();
				int service = cursor.getColumnIndex(SERVICE),
				token = cursor.getColumnIndex(TOKEN),
				secret = cursor.getColumnIndex(SECRET),
				expiry = cursor.getColumnIndex(EXPIRY),
				timezone = cursor.getColumnIndex(TIMEZONE);
				String name = "name",
				created_time = "created_time",
				message = "message",
				actions = "actions",
				link = "link",
				comment = "Comment",
				from = "from",
				id = "id",
				type = "type",
				status = "status",
				profile = "http://graph.facebook.com/%s/picture",
				author = "author",
				displayName = "displayName",
				moodStatusLastUpdated = "moodStatusLastUpdated",
				thumbnailUrl = "thumbnailUrl",
				source = "source",
				url = "url",
				status_url = "http://twitter.com/%s/status/%s";
				Date now = new Date();
				while (!cursor.isAfterLast()) {
					switch (cursor.getInt(service)) {
					case TWITTER:
						try {
							List<Status> statuses = (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(cursor.getString(token), cursor.getString(secret)))).getFriendsTimeline(new Paging(1, max_widget_items));
							for (Status s : statuses) {
								String screenname = s.getUser().getScreenName();
								status_items.add(new StatusItem(s.getCreatedAt(),
										String.format(status_url, screenname, Long.toString(s.getId())),
										screenname,
										s.getUser().getProfileImageURL(),
										s.getText(),
										use24hr,
										now));
							}
						} catch (TwitterException te) {
							Log.e(TAG, te.toString());
						}
						break;
					case FACEBOOK:
						Facebook facebook = new Facebook();
						facebook.setAccessToken(cursor.getString(token));
						facebook.setAccessExpires((long)cursor.getInt(expiry));
						try {
							JSONObject jobj = Util.parseJson(facebook.request("me/home"));
							JSONArray jarr = jobj.getJSONArray("data");
							for (int i = 0; i < jarr.length(); i++) {
								JSONObject o = jarr.getJSONObject(i);
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
										status_items.add(new StatusItem(
												parseDate(o.getString(created_time), "yyyy-MM-dd'T'HH:mm:ss'+0000'", cursor.getInt(timezone)),
												l,
												f.getString(name),
												new URL(String.format(profile, f.getString(id))),
												o.getString(message),
												use24hr,
												now));
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
						OAuthConsumer consumer = new CommonsHttpOAuthConsumer(MYSPACE_KEY, MYSPACE_SECRET, SignatureMethod.HMAC_SHA1);
						consumer.setTokenWithSecret(cursor.getString(token), cursor.getString(secret));
						HttpClient client = new DefaultHttpClient();
						ResponseHandler <String> responseHandler = new BasicResponseHandler();
						HttpGet request = new HttpGet("http://opensocial.myspace.com/1.0/statusmood/@me/@friends/history?includeself=true&fields=author,source");
						try {
							consumer.sign(request);
							JSONObject jobj = new JSONObject(client.execute(request, responseHandler));
							JSONArray entries = jobj.getJSONArray("entry");
							for (int i = 0; i < entries.length(); i++) {
								JSONObject entry = entries.getJSONObject(i);
								JSONObject authorObj = entry.getJSONObject(author);
								status_items.add(new StatusItem(parseDate(entry.getString(moodStatusLastUpdated), "yyyy-MM-dd'T'HH:mm:ss'Z'", 0),
										entry.getJSONObject(source).getString(url),
										authorObj.getString(displayName),
										new URL(authorObj.getString(thumbnailUrl)),
										entry.getString(status),
										use24hr,
										now));
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
					cursor.moveToNext();
				}
				// sort statuses
				Collections.sort(status_items);
			}
			cursor.close();
			settings.close();
			// Push update for this widget to the home screen
			int body_text = settings.getInt(settings.getColumnIndex(MESSAGE_COLOR)),
			friend_text = settings.getInt(settings.getColumnIndex(FRIEND_COLOR)),
			created_text = settings.getInt(settings.getColumnIndex(CREATED_COLOR));
			// set messages background
			Bitmap bd_bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
			Canvas bd_canvas = new Canvas(bd_bitmap);
			bd_canvas.drawColor(settings.getInt(settings.getColumnIndex(MESSAGE_BG_COLOR)));
			int[] map_item = {R.id.item0, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6},
			map_profile = {R.id.profile0, R.id.profile1, R.id.profile2, R.id.profile3, R.id.profile4, R.id.profile5, R.id.profile6},
			map_message = {R.id.message0, R.id.message1, R.id.message2, R.id.message3, R.id.message4, R.id.message5, R.id.message6},
			map_screenname = {R.id.screenname0, R.id.screenname1, R.id.screenname2, R.id.screenname3, R.id.screenname4, R.id.screenname5, R.id.screenname6},
			map_created = {R.id.created0, R.id.created1, R.id.created2, R.id.created3, R.id.created4, R.id.created5, R.id.created6};
			ComponentName browser = new ComponentName("com.android.browser", "com.android.browser.BrowserActivity");
			int count_status = 0, max_status = map_item.length;
			RemoteViews views = new RemoteViews(getPackageName(), displayButtons ? R.layout.widget : R.layout.widget_nobuttons);
			views.setImageViewBitmap(R.id.body_background, bd_bitmap);
			for  (StatusItem item : status_items) {
				if (count_status < max_status) {
					// if no buttons, use StatusDialog.java with options for Config and Refresh
					if (displayButtons) views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, (new Intent(Intent.ACTION_VIEW, Uri.parse(item.link))).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(browser), 0));
					else views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, (new Intent(this, StatusDialog.class)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId), 0));
					views.setTextViewText(map_message[count_status], item.message);
					views.setTextColor(map_message[count_status], body_text);
					views.setTextViewText(map_screenname[count_status], item.friend);
					views.setTextColor(map_screenname[count_status], friend_text);
					views.setTextViewText(map_created[count_status], item.displayCreated);
					views.setTextColor(map_created[count_status], created_text);
					try {
						views.setImageViewBitmap(map_profile[count_status], BitmapFactory.decodeStream(item.profile.openConnection().getInputStream()));
					} catch (IOException e) {
						Log.e(TAG,e.getMessage());
					}										
					count_status++;
				} else break;
			}
			if (displayButtons) views = addButtons(views, appWidgetId, settings.getInt(settings.getColumnIndex(BUTTONS_COLOR)), settings.getInt(settings.getColumnIndex(BUTTONS_BG_COLOR)));
			manager.updateAppWidget(appWidgetId, views);
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, System.currentTimeMillis() + settings.getInt(settings.getColumnIndex(INTERVAL)), PendingIntent.getService(this, 0, new Intent(this, SonetService.class), 0));
		}
		db.close();
		sonetDatabaseHelper.close();
	}

	private RemoteViews addButtons(RemoteViews views, int widgetId, int textColor, int bgColor) {
		Bitmap bg_bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
		Canvas bg_canvas = new Canvas(bg_bitmap);
		bg_canvas.drawColor(bgColor);
		views.setImageViewBitmap(R.id.head_background, bg_bitmap);
		views.setTextColor(R.id.head_spacer, bgColor);
		views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this, 0, (new Intent(this, PostDialog.class)), 0));
		views.setTextColor(R.id.button_post, textColor);
		views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(this, 0, (new Intent(this, UI.class)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId), 0));
		views.setTextColor(R.id.button_post, textColor);
		views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(this, 0, (new Intent(this, SonetService.class)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId), 0));
		views.setTextColor(R.id.button_post, textColor);
		return views;
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
		if (timezone > 0) cal.add(Calendar.HOUR, timezone);
		return cal.getTime();		
	}

	private class StatusItem implements Comparable<StatusItem> {
		private Date created;
		private String link;
		private String friend;
		private URL profile;
		private String message;
		private String displayCreated;
		StatusItem(Date created, String link, String friend, URL profile, String message, boolean use24hr, Date now) {
			this.created = created;
			this.link = link;
			this.friend = friend;
			this.profile = profile;
			this.message = message;
			this.displayCreated = ((now.getTime() - this.created.getTime()) < 86400000 ?
					(use24hr ?
							String.format("%d:%02d", this.created.getHours(), this.created.getMinutes())
							: String.format("%d:%02d%s", this.created.getHours() < 13 ? this.created.getHours() : this.created.getHours() - 12, this.created.getMinutes(), getString(this.created.getHours() < 13 ? R.string.am : R.string.pm)))
							: String.format("%s %d", getResources().getStringArray(R.array.months)[this.created.getMonth()], this.created.getDate()));
		}

		public int compareTo(StatusItem si) {
			// sort descending
			return ((Long)si.created.getTime()).compareTo(created.getTime());
		}
	}

}
