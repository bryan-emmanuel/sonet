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
import static com.piusvelte.sonet.Sonet.TAG;
import static com.piusvelte.sonet.Sonet.TWITTER_KEY;
import static com.piusvelte.sonet.Sonet.TWITTER_SECRET;

import java.util.HashMap;
import java.util.List;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class SonetService extends Service {
	private static final String REFRESH = "com.piusvelte.Intent.REFRESH";
	private static final int TWITTER = 0;
	private static final int FACEBOOK = 1;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		init();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStart(intent, startId);
		init();
		return START_STICKY;
	}

	public void init() {
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getBroadcast(this, 0, new Intent(this, SonetService.class).setAction(REFRESH), 0));
		SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		boolean hasConnection = cm.getBackgroundDataSetting() && cm.getActiveNetworkInfo().isConnected();
		SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
		SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
		// Perform this loop procedure for each App Widget that belongs to this provider
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		// Build an update that holds the updated widget contents
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
		// set buttons
		views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getBroadcast(this, 0, (new Intent(this, UI.class)), 0));
		views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getBroadcast(this, 0, (new Intent(this, UI.class)), 0));
		views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getBroadcast(this, 0, (new Intent(this, SonetService.class)).setAction(REFRESH), 0));
		// set head styles
		int status_count = 0;
		HashMap<String, Integer> friend_map = new HashMap<String, Integer>();
		String r_friend = "R.id.friend_";
		friend_map.put(r_friend+"0", new Integer(R.id.friend_0));
		friend_map.put(r_friend+"1", new Integer(R.id.friend_1));
		friend_map.put(r_friend+"2", new Integer(R.id.friend_2));
		friend_map.put(r_friend+"3", new Integer(R.id.friend_3));
		friend_map.put(r_friend+"4", new Integer(R.id.friend_4));
		friend_map.put(r_friend+"5", new Integer(R.id.friend_5));
		friend_map.put(r_friend+"6", new Integer(R.id.friend_6));
		friend_map.put(r_friend+"7", new Integer(R.id.friend_7));
		friend_map.put(r_friend+"8", new Integer(R.id.friend_8));
		friend_map.put(r_friend+"9", new Integer(R.id.friend_9));
		friend_map.put(r_friend+"10", new Integer(R.id.friend_10));
		friend_map.put(r_friend+"11", new Integer(R.id.friend_11));
		friend_map.put(r_friend+"12", new Integer(R.id.friend_12));
		friend_map.put(r_friend+"13", new Integer(R.id.friend_13));
		friend_map.put(r_friend+"14", new Integer(R.id.friend_14));
		friend_map.put(r_friend+"15", new Integer(R.id.friend_15));
		friend_map.put(r_friend+"16", new Integer(R.id.friend_16));
		friend_map.put(r_friend+"17", new Integer(R.id.friend_17));
		friend_map.put(r_friend+"18", new Integer(R.id.friend_18));
		HashMap<String, Integer> status_map = new HashMap<String, Integer>();
		String r_status = "R.id.status_";
		friend_map.put(r_status+"0", new Integer(R.id.status_0));
		friend_map.put(r_status+"1", new Integer(R.id.status_1));
		friend_map.put(r_status+"2", new Integer(R.id.status_2));
		friend_map.put(r_status+"3", new Integer(R.id.status_3));
		friend_map.put(r_status+"4", new Integer(R.id.status_4));
		friend_map.put(r_status+"5", new Integer(R.id.status_5));
		friend_map.put(r_status+"6", new Integer(R.id.status_6));
		friend_map.put(r_status+"7", new Integer(R.id.status_7));
		friend_map.put(r_status+"8", new Integer(R.id.status_8));
		friend_map.put(r_status+"9", new Integer(R.id.status_9));
		friend_map.put(r_status+"10", new Integer(R.id.status_10));
		friend_map.put(r_status+"11", new Integer(R.id.status_11));
		friend_map.put(r_status+"12", new Integer(R.id.status_12));
		friend_map.put(r_status+"13", new Integer(R.id.status_13));
		friend_map.put(r_status+"14", new Integer(R.id.status_14));
		friend_map.put(r_status+"15", new Integer(R.id.status_15));
		friend_map.put(r_status+"16", new Integer(R.id.status_16));
		friend_map.put(r_status+"17", new Integer(R.id.status_17));
		friend_map.put(r_status+"18", new Integer(R.id.status_18));
		if (hasConnection) {
			// Build the widget update for today
			// sort results
			// update
			// Pick out month names from resources
			//			Resources res = context.getResources();

			// query accounts
			Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{_ID, USERNAME, TOKEN, SECRET, SERVICE}, null, null, null, null, null);
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				int service = cursor.getColumnIndex(SERVICE),
				token = cursor.getColumnIndex(TOKEN),
				secret = cursor.getColumnIndex(SECRET);
				while (!cursor.isAfterLast()) {
					switch (cursor.getInt(service)) {
					case TWITTER:
						TwitterFactory factory = new TwitterFactory();
						Twitter twitter = factory.getInstance();
						twitter.setOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
						AccessToken accessToken = new AccessToken(cursor.getString(token), cursor.getString(secret));
						twitter.setOAuthAccessToken(accessToken);
						try {
							List<Status> statuses = twitter.getFriendsTimeline();
							for (Status status : statuses) {
								if (status_count < 19) {
									views.setTextViewText(friend_map.get(r_friend+Integer.toString(status_count)).intValue(), status.getUser().getScreenName());
									views.setTextViewText(status_map.get(r_status+Integer.toString(status_count)).intValue(), status.getText());
								}
								status_count++;
							}
						} catch (TwitterException te) {
							Log.e(TAG, te.toString());
						}
						break;
					case FACEBOOK:
						break;					
					}
					cursor.moveToNext();
				}
			}
			cursor.close();
			// list content
			// empty list will display "loading..."
			// apply styles
		} else {
			views.setTextViewText(R.id.friend_0, "");
			views.setTextViewText(R.id.status_0, getString(R.string.no_connection));
			status_count++;
		}
		db.close();
		// reset unused statuses
		while (status_count < 19) {
			views.setTextViewText(friend_map.get(r_friend+Integer.toString(status_count)).intValue(), "");
			views.setTextViewText(status_map.get(r_status+Integer.toString(status_count)).intValue(), "");
			status_count++;
		}
		// Push update for this widget to the home screen
		int[] sonetWidget_4x2 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x2.class));
		for (int i=0; i<sonetWidget_4x2.length; i++) manager.updateAppWidget(sonetWidget_4x2[i], views);
		int[] sonetWidget_4x3 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x3.class));
		for (int i=0; i<sonetWidget_4x3.length; i++) manager.updateAppWidget(sonetWidget_4x3[i], views);
		int[] sonetWidget_4x4 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x4.class));
		for (int i=0; i<sonetWidget_4x4.length; i++) manager.updateAppWidget(sonetWidget_4x4[i], views);
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, System.currentTimeMillis() + Integer.parseInt((String) sp.getString(getString(R.string.key_interval), getString(R.string.default_interval))), PendingIntent.getService(this, 0, new Intent(this, SonetService.class), 0));
		// no need to keep the service around
		stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't need to bind to this service
		return null;
	}
}
