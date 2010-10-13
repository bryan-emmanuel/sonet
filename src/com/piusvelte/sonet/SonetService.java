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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

//import com.facebook.android.Facebook;

import twitter4j.Paging;
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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class SonetService extends Service {
	private static final String REFRESH = "com.piusvelte.Intent.REFRESH";
	private static final int TWITTER = 0;
	private static final int FACEBOOK = 1;
//	private Facebook mFacebook;

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
		// Perform this loop procedure for each App Widget that belongs to this provider
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		int[] sonetWidget_4x2 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x2.class));
		int[] sonetWidget_4x3 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x3.class));
		int[] sonetWidget_4x4 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x4.class));
		int max_widget_items = sonetWidget_4x4.length > 0 ? 6 : sonetWidget_4x3.length > 0 ? 5 : sonetWidget_4x2.length > 0 ? 3 : 0;
		if (max_widget_items > 0) {
			SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
			ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
			SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
			ComponentName browser = new ComponentName("com.android.browser", "com.android.browser.BrowserActivity");
			RemoteViews views;
			if (sp.getBoolean(getString(R.string.key_display_buttons), true)) {
				// display the buttons, setting style on onclick
				views = new RemoteViews(getPackageName(), R.layout.widget);
				// set buttons
				int head_text = Integer.parseInt(sp.getString(getString(R.string.key_head_text), getString(R.string.default_head_text))),
				head_background = Integer.parseInt(sp.getString(getString(R.string.key_head_background), getString(R.string.default_head_background)));
				Bitmap bg_bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas bg_canvas = new Canvas(bg_bitmap);
				bg_canvas.drawColor(head_background);
				views.setImageViewBitmap(R.id.head_background, bg_bitmap);
				views.setTextColor(R.id.head_spacer, head_background);
				views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this, 0, (new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com"))).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(browser), 0));
				views.setTextColor(R.id.button_post, head_text);
				views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(this, 0, (new Intent(this, UI.class)), 0));
				views.setTextColor(R.id.button_post, head_text);
				views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(this, 0, (new Intent(this, SonetService.class)), 0));
				views.setTextColor(R.id.button_post, head_text);
			} else views = new RemoteViews(getPackageName(), R.layout.widget_nobuttons);
			// set messages background
			Bitmap bd_bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
			Canvas bd_canvas = new Canvas(bd_bitmap);
			bd_canvas.drawColor(Integer.parseInt(sp.getString(getString(R.string.key_body_background), getString(R.string.default_body_background))));
			views.setImageViewBitmap(R.id.body_background, bd_bitmap);
			if (cm.getBackgroundDataSetting() && (cm.getActiveNetworkInfo() != null) && cm.getActiveNetworkInfo().isConnected()) {
				// query accounts
				Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{_ID, USERNAME, TOKEN, SECRET, SERVICE}, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					/* get statuses for all accounts
					 * then sort them by datetime, descending
					 */
					int[] map_item = {R.id.item0, R.id.icon1, R.id.item2, R.id.item3, R.id.item4, R.id.item5},
					map_icon = {R.id.icon0, R.id.icon1, R.id.icon2, R.id.icon3, R.id.icon4, R.id.icon5},
					map_status = {R.id.status0, R.id.status1, R.id.status2, R.id.status3, R.id.status4, R.id.status5},
					map_friend = {R.id.friend0, R.id.friend1, R.id.friend2, R.id.friend3, R.id.friend4, R.id.friend5};
					Long current_time = (new Date()).getTime();
					int day = 86400000;
					int hour = 3600000;
					int minute = 60000;
					int second = 1000;
					List<StatusItem> status_items = new ArrayList<StatusItem>();
					cursor.moveToFirst();
					int service = cursor.getColumnIndex(SERVICE),
					token = cursor.getColumnIndex(TOKEN),
					secret = cursor.getColumnIndex(SECRET),
					body_text = Integer.parseInt(sp.getString(getString(R.string.key_body_text), getString(R.string.default_body_text)));
					while (!cursor.isAfterLast()) {
						switch (cursor.getInt(service)) {
						case TWITTER:
							String status_url = "http://twitter.com/%s/status/%s";
							TwitterFactory factory = new TwitterFactory();
							Twitter twitter = factory.getInstance();
							twitter.setOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
							AccessToken accessToken = new AccessToken(cursor.getString(token), cursor.getString(secret));
							twitter.setOAuthAccessToken(accessToken);
							try {
								List<Status> statuses = twitter.getFriendsTimeline(new Paging(1, max_widget_items));
								for (Status status : statuses) {
									String screenname = status.getUser().getScreenName();
									status_items.add(new StatusItem(status.getCreatedAt().getTime(),
											Uri.parse(String.format(status_url, screenname, Long.toString(status.getId()))),
											screenname,
											status.getUser().getProfileImageURL(),
											status.getText()));
								}
							} catch (TwitterException te) {
								Log.e(TAG, te.toString());
							}
							break;
						case FACEBOOK:
//							mFacebook = new Facebook();
//							mFacebook.setAccessToken(cursor.getString(token));
//							mFacebook.setAccessExpires(Long.parseLong(cursor.getString(secret)));
							break;					
						}
						cursor.moveToNext();
					}
					// sort statuses
					Collections.sort(status_items);
					// list content
					int count_status = 0, max_status = map_item.length;
					for  (StatusItem item : status_items) {
						if (count_status < max_status) {
							String screenname = item.friend;
							Long elapsed_time = current_time - item.created;
							int time_unit;
							views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, (new Intent(Intent.ACTION_VIEW, item.link)).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(browser), 0));
							views.setTextViewText(map_status[count_status], item.message);
							views.setTextColor(map_status[count_status], body_text);
							if (elapsed_time > day) {
								elapsed_time = (long) Math.floor(elapsed_time / day);
								time_unit = R.string.days;
							} else if (elapsed_time > hour) {
								elapsed_time = (long) Math.floor(elapsed_time / hour);
								time_unit = R.string.hours;
							} else if (elapsed_time > minute) {
								elapsed_time = (long) Math.floor(elapsed_time / minute);
								time_unit = R.string.minutes;
							} else if (elapsed_time > second){
								elapsed_time = (long) Math.floor(elapsed_time / second);
								time_unit = R.string.seconds;
							} else {
								elapsed_time = (long) 0;
								time_unit = R.string.seconds;
							}
							views.setTextViewText(map_friend[count_status], String.format(getString(R.string.status_detail), screenname, Long.toString(elapsed_time), getString(time_unit)));
							views.setTextColor(map_friend[count_status], body_text);
							try {
								views.setImageViewBitmap(map_icon[count_status], BitmapFactory.decodeStream(item.profile.openConnection().getInputStream()));
							} catch (IOException e) {
								Log.e(TAG,e.getMessage());
							}										
							count_status++;
						} else break;
					}
				}
				cursor.close();
			}
			db.close();
			// Push update for this widget to the home screen
			for (int i=0; i<sonetWidget_4x2.length; i++) manager.updateAppWidget(sonetWidget_4x2[i], views);
			for (int i=0; i<sonetWidget_4x3.length; i++) manager.updateAppWidget(sonetWidget_4x3[i], views);
			for (int i=0; i<sonetWidget_4x4.length; i++) manager.updateAppWidget(sonetWidget_4x4[i], views);
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, System.currentTimeMillis() + Integer.parseInt((String) sp.getString(getString(R.string.key_interval), getString(R.string.default_interval))), PendingIntent.getService(this, 0, new Intent(this, SonetService.class), 0));
		}
		// no need to keep the service around
		stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't need to bind to this service
		return null;
	}

	private class StatusItem implements Comparable<StatusItem> {
		private Long created;
		private Uri link;
		private String friend;
		private URL profile;
		private String message;
		StatusItem(Long created, Uri link, String friend, URL profile, String message) {
			this.created = created;
			this.link = link;
			this.friend = friend;
			this.profile = profile;
			this.message = message;
		}
		@Override
		public int compareTo(StatusItem si) {
			// sort descending
			return created.compareTo(si.created);
		}
	}

}
