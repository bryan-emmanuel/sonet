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
import static com.piusvelte.sonet.SonetDatabaseHelper.EXPIRY;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIMEZONE;
import static com.piusvelte.sonet.Sonet.TAG;
import static com.piusvelte.sonet.Sonet.TWITTER_KEY;
import static com.piusvelte.sonet.Sonet.TWITTER_SECRET;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_KEY;
import static com.piusvelte.sonet.Sonet.MYSPACE_SECRET;

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
import oauth.signpost.basic.DefaultOAuthConsumer;
//import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.signature.SignatureMethod;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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

public class SonetService extends Service {
	private static final String REFRESH = "com.piusvelte.Intent.REFRESH";
	private BroadcastReceiver mReceiver;

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
		SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getBroadcast(this, 0, new Intent(this, SonetService.class).setAction(REFRESH), 0));
		// Perform this loop procedure for each App Widget that belongs to this provider
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		int[] sonetWidget_4x2 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x2.class));
		int[] sonetWidget_4x3 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x3.class));
		int[] sonetWidget_4x4 = manager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x4.class));
		int max_widget_items = sonetWidget_4x4.length > 0 ? 7 : sonetWidget_4x3.length > 0 ? 5 : sonetWidget_4x2.length > 0 ? 3 : 0;
		if (max_widget_items > 0) {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
			ComponentName browser = new ComponentName("com.android.browser", "com.android.browser.BrowserActivity");
			Boolean displayButtons = sp.getBoolean(getString(R.string.key_display_buttons), true);
			RemoteViews views = new RemoteViews(getPackageName(), displayButtons ? R.layout.widget : R.layout.widget_nobuttons);
			// set messages background
			Bitmap bd_bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
			Canvas bd_canvas = new Canvas(bd_bitmap);
			bd_canvas.drawColor(Integer.parseInt(sp.getString(getString(R.string.key_body_background), getString(R.string.default_body_background))));
			views.setImageViewBitmap(R.id.body_background, bd_bitmap);
			if (cm.getBackgroundDataSetting()) {
				if ((cm.getActiveNetworkInfo() != null) && cm.getActiveNetworkInfo().isConnected()) {
					if (mReceiver != null) {
						unregisterReceiver(mReceiver);
						mReceiver = null;
					}
					// query accounts
					SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
					Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{_ID, USERNAME, TOKEN, SECRET, SERVICE, EXPIRY, TIMEZONE}, null, null, null, null, null);
					if (cursor.getCount() > 0) {
						/* get statuses for all accounts
						 * then sort them by datetime, descending
						 */
						int[] map_item = {R.id.item0, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6},
						map_profile = {R.id.profile0, R.id.profile1, R.id.profile2, R.id.profile3, R.id.profile4, R.id.profile5, R.id.profile6},
						map_message = {R.id.message0, R.id.message1, R.id.message2, R.id.message3, R.id.message4, R.id.message5, R.id.message6},
						map_screenname = {R.id.screenname0, R.id.screenname1, R.id.screenname2, R.id.screenname3, R.id.screenname4, R.id.screenname5, R.id.screenname6},
						map_created = {R.id.created0, R.id.created1, R.id.created2, R.id.created3, R.id.created4, R.id.created5, R.id.created6};
						Date now = new Date();
						boolean use24hr = sp.getBoolean(getString(R.string.key_time_12_24), false);
						List<StatusItem> status_items = new ArrayList<StatusItem>();
						cursor.moveToFirst();
						int service = cursor.getColumnIndex(SERVICE),
						token = cursor.getColumnIndex(TOKEN),
						secret = cursor.getColumnIndex(SECRET),
						expiry = cursor.getColumnIndex(EXPIRY),
						timezone = cursor.getColumnIndex(TIMEZONE),
						body_text = Integer.parseInt(sp.getString(getString(R.string.key_body_text), getString(R.string.default_body_text))),
						friend_text = Integer.parseInt(sp.getString(getString(R.string.key_friend_text), getString(R.string.default_friend_text))),
						created_text = Integer.parseInt(sp.getString(getString(R.string.key_created_text), getString(R.string.default_created_text)));
						while (!cursor.isAfterLast()) {
							switch (cursor.getInt(service)) {
							case TWITTER:
								String status_url = "http://twitter.com/%s/status/%s";
								try {
									List<Status> statuses = (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(cursor.getString(token), cursor.getString(secret)))).getFriendsTimeline(new Paging(1, max_widget_items));
									for (Status status : statuses) {
										String screenname = status.getUser().getScreenName();
										status_items.add(new StatusItem(status.getCreatedAt(),
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
								profile = "http://graph.facebook.com/%s/picture";
								int tz = cursor.getInt(timezone);
								SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+0000'");
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
											// parse the date
											Date created;
											if (o.has(created_time)) {
												try {
													created = format.parse(o.getString(created_time));
												} catch (ParseException e) {
													created = new Date();
													Log.e(TAG,e.toString());
												}
											} else created = new Date();
											// apply the timezone
											Calendar cal = Calendar.getInstance();
											cal.setTime(created);
											cal.add(Calendar.HOUR, tz);
											// parse the link
											Uri l = Uri.parse("http://www.facebook.com");
											if (o.has(actions)) {											
												JSONArray action = o.getJSONArray(actions);
												for (int a = 0; a < action.length(); a++) {
													JSONObject n = action.getJSONObject(a);
													if (n.getString(name) == comment) {
														l = Uri.parse(n.getString(link));
														break;
													}
												}
											}
											JSONObject f = o.getJSONObject(from);
											if (f.has(name) && f.has(id)) {
												status_items.add(new StatusItem(cal.getTime(),
														l,
														f.getString(name),
														new URL(String.format(profile, f.getString(id))),
														o.getString(message)));
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
								OAuthConsumer consumer = new DefaultOAuthConsumer(MYSPACE_KEY, MYSPACE_SECRET, SignatureMethod.HMAC_SHA1);
								consumer.setTokenWithSecret(cursor.getString(token), cursor.getString(secret));
								HttpClient client = new DefaultHttpClient();
								HttpGet request = new HttpGet("http://api.myspace.com/1.0/activities/?format=json");
								try {
									consumer.sign(request);
									JSONObject jobj = new JSONObject(client.execute(request).toString());
									Log.v(TAG, jobj.toString());
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
						// list content
						int count_status = 0, max_status = map_item.length;
						for  (StatusItem item : status_items) {
							if (count_status < max_status) {
								views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, (new Intent(Intent.ACTION_VIEW, item.link)).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(browser), 0));
								views.setTextViewText(map_message[count_status], item.message);
								views.setTextColor(map_message[count_status], body_text);
								views.setTextViewText(map_screenname[count_status], item.friend);
								views.setTextColor(map_screenname[count_status], friend_text);
								views.setTextViewText(map_created[count_status],
										(item.created.getDay() == now.getDay() ?
												(use24hr ?
														String.format("%d:%02d", item.created.getHours(), item.created.getMinutes())
														: String.format("%d:%02d%s", item.created.getHours() < 13 ? item.created.getHours() : item.created.getHours() - 12, item.created.getMinutes(), getString(item.created.getHours() < 13 ? R.string.am : R.string.pm)))
														: (getResources().getStringArray(R.array.months)[item.created.getMonth()] + Integer.toString(item.created.getDay()))));
								views.setTextColor(map_created[count_status], created_text);
								try {
									views.setImageViewBitmap(map_profile[count_status], BitmapFactory.decodeStream(item.profile.openConnection().getInputStream()));
								} catch (IOException e) {
									Log.e(TAG,e.getMessage());
								}										
								count_status++;
							} else break;
						}
					}
					cursor.close();
					db.close();
				} else {
					// if there's no connection, listen for one
					mReceiver = new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
								if (((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).isConnected()) init();
							}
						}
					};
				}
			}
			// Push update for this widget to the home screen
			for (int i=0; i<sonetWidget_4x2.length; i++) {
				if (displayButtons) {
					int head_text = Integer.parseInt(sp.getString(getString(R.string.key_head_text), getString(R.string.default_head_text))),
					head_background = Integer.parseInt(sp.getString(getString(R.string.key_head_background), getString(R.string.default_head_background)));
					Bitmap bg_bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
					Canvas bg_canvas = new Canvas(bg_bitmap);
					bg_canvas.drawColor(head_background);
					views.setImageViewBitmap(R.id.head_background, bg_bitmap);
					views.setTextColor(R.id.head_spacer, head_background);
					views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this, 0, (new Intent(this, PostDialog.class)), 0));
					views.setTextColor(R.id.button_post, head_text);
					views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(this, 0, (new Intent(this, UI.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, sonetWidget_4x2[i])), 0));
					views.setTextColor(R.id.button_post, head_text);
					views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(this, 0, (new Intent(this, SonetService.class)), 0));
					views.setTextColor(R.id.button_post, head_text);
				}
				manager.updateAppWidget(sonetWidget_4x2[i], views);
			}
			for (int i=0; i<sonetWidget_4x3.length; i++) {
				if (displayButtons) {
					int head_text = Integer.parseInt(sp.getString(getString(R.string.key_head_text), getString(R.string.default_head_text))),
					head_background = Integer.parseInt(sp.getString(getString(R.string.key_head_background), getString(R.string.default_head_background)));
					Bitmap bg_bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
					Canvas bg_canvas = new Canvas(bg_bitmap);
					bg_canvas.drawColor(head_background);
					views.setImageViewBitmap(R.id.head_background, bg_bitmap);
					views.setTextColor(R.id.head_spacer, head_background);
					views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this, 0, (new Intent(this, PostDialog.class)), 0));
					views.setTextColor(R.id.button_post, head_text);
					views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(this, 0, (new Intent(this, UI.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, sonetWidget_4x3[i])), 0));
					views.setTextColor(R.id.button_post, head_text);
					views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(this, 0, (new Intent(this, SonetService.class)), 0));
					views.setTextColor(R.id.button_post, head_text);
				}
				manager.updateAppWidget(sonetWidget_4x3[i], views);
			}
			for (int i=0; i<sonetWidget_4x4.length; i++) {
				if (displayButtons) {
					int head_text = Integer.parseInt(sp.getString(getString(R.string.key_head_text), getString(R.string.default_head_text))),
					head_background = Integer.parseInt(sp.getString(getString(R.string.key_head_background), getString(R.string.default_head_background)));
					Bitmap bg_bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
					Canvas bg_canvas = new Canvas(bg_bitmap);
					bg_canvas.drawColor(head_background);
					views.setImageViewBitmap(R.id.head_background, bg_bitmap);
					views.setTextColor(R.id.head_spacer, head_background);
					views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this, 0, (new Intent(this, PostDialog.class)), 0));
					views.setTextColor(R.id.button_post, head_text);
					views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(this, 0, (new Intent(this, UI.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, sonetWidget_4x4[i])), 0));
					views.setTextColor(R.id.button_post, head_text);
					views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(this, 0, (new Intent(this, SonetService.class)), 0));
					views.setTextColor(R.id.button_post, head_text);
				}
				manager.updateAppWidget(sonetWidget_4x4[i], views);
			}
		}
		if (mReceiver != null) {
			IntentFilter f = new IntentFilter();
			f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
			registerReceiver(mReceiver, f);			
		} else {
			// set alarm for the next update and stop the service
			((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, System.currentTimeMillis() + Integer.parseInt((String) sp.getString(getString(R.string.key_interval), getString(R.string.default_interval))), PendingIntent.getService(this, 0, new Intent(this, SonetService.class), 0));
			stopSelf();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't need to bind to this service
		return null;
	}

	private class StatusItem implements Comparable<StatusItem> {
		private Date created;
		private Uri link;
		private String friend;
		private URL profile;
		private String message;
		StatusItem(Date created, Uri link, String friend, URL profile, String message) {
			this.created = created;
			this.link = link;
			this.friend = friend;
			this.profile = profile;
			this.message = message;
		}

		public int compareTo(StatusItem si) {
			// sort descending
			return ((Long)si.created.getTime()).compareTo(created.getTime());
		}
	}

}
