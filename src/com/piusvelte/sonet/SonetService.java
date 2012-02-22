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

import static com.piusvelte.sonet.Sonet.*;
import static com.piusvelte.sonet.SonetTokens.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.BoundRemoteViews;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet.Notifications;
import com.piusvelte.sonet.Sonet.Widget_accounts_view;
import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.RemoteViews;

public class SonetService extends Service {
	private static final String TAG = "SonetService";
	private final static HashMap<Integer, AsyncTask<Integer, String, Integer>> mStatusesLoaders = new HashMap<Integer, AsyncTask<Integer, String, Integer>>();
	private final ArrayList<AsyncTask<SmsMessage, String, int[]>> mSMSLoaders = new ArrayList<AsyncTask<SmsMessage, String, int[]>>();
	private AlarmManager mAlarmManager;
	private ConnectivityManager mConnectivityManager;
	private SonetCrypto mSonetCrypto;
	private String mNotify = null;
	private ContentObserver mInstantUpload = null;
	private SimpleDateFormat mSimpleDateFormat = null;

	private static Method sSetRemoteAdapter;
	private static Method sSetPendingIntentTemplate;
	private static Method sSetEmptyView;
	private static Method sNotifyAppWidgetViewDataChanged;
	private static boolean sNativeScrollingSupported = false;

	static {
		if (Integer.valueOf(android.os.Build.VERSION.SDK) >= 11) {
			try {
				sSetEmptyView = RemoteViews.class.getMethod("setEmptyView", new Class[]{int.class, int.class});
				sSetPendingIntentTemplate = RemoteViews.class.getMethod("setPendingIntentTemplate", new Class[]{int.class, PendingIntent.class});
				sSetRemoteAdapter = RemoteViews.class.getMethod("setRemoteAdapter", new Class[]{int.class, int.class, Intent.class});
				sNotifyAppWidgetViewDataChanged = AppWidgetManager.class.getMethod("notifyAppWidgetViewDataChanged", new Class[]{int.class, int.class});
				sNativeScrollingSupported = true;
			} catch (NoSuchMethodException nsme) {
				Log.d(TAG, "native scrolling not supported: " + nsme.toString());
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mSonetCrypto = SonetCrypto.getInstance(getApplicationContext());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, startId);
		return START_STICKY;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		// check for any facebook instant upload settings
		(new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... arg0) {
				Boolean upload = false;
				Cursor c = getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID}, Widgets.INSTANT_UPLOAD + "=1", null, null);
				upload = c.moveToFirst();
				c.close();
				return upload;
			}

			@Override
			protected void onPostExecute(Boolean upload) {
				if (upload && (mInstantUpload == null)) {
					mInstantUpload = new ContentObserver(null) {

						@Override
						public void onChange(boolean selfChange) {
							super.onChange(selfChange);
							(new AsyncTask<Void, Void, String>() {

								@Override
								protected String doInBackground(Void... arg0) {
									String filepath = null;
									// on boot triggers this, to avoid it, limit the query to changes in the past 8 seconds
									Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaColumns.DATA}, MediaColumns.DATE_ADDED + ">?", new String[]{Long.toString(System.currentTimeMillis() / 1000 - 8)}, MediaColumns.DATE_ADDED + " DESC");
									if (c.moveToFirst()) {
										filepath = c.getString(0);
									}
									c.close();
									return filepath;
								}

								@Override
								protected void onPostExecute(String filepath) {
									// launch post activity with filepath
									if (filepath != null) {
										startActivity(new Intent(getApplicationContext(), SonetUploadDialog.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(Widgets.INSTANT_UPLOAD, filepath));
									}
								}

							}).execute();
						}

					};
					getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, mInstantUpload);
				} else if (!upload && (mInstantUpload != null)) {
					getContentResolver().unregisterContentObserver(mInstantUpload);
					mInstantUpload = null;
				}
			}

		}).execute();
		Log.d(TAG,"onCreate");
		if (intent != null) {
			String action = intent.getAction();
			if (action != null) {
				Log.d(TAG,"action:" + action);
				if (action.equals(ACTION_REFRESH)) {
					if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
						int[] widgets = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
						for (int widget : widgets) {
							putNewUpdate(widget, 1);
						}
					} else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
						putNewUpdate(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID), 1);
					} else if (intent.getData() != null) {
						putNewUpdate(Integer.parseInt(intent.getData().getLastPathSegment()), 1);
					}
				} else if (action.equals(LauncherIntent.Action.ACTION_READY)) {
					if (intent.hasExtra(EXTRA_SCROLLABLE_VERSION) && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
						int scrollableVersion = intent.getIntExtra(EXTRA_SCROLLABLE_VERSION, 1);
						int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
						// check if the scrollable needs to be built
						Cursor widget = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.SCROLLABLE}, Widgets.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)}, null);
						if (widget.moveToFirst()) {
							if (widget.getInt(widget.getColumnIndex(Widgets.SCROLLABLE)) < scrollableVersion) {
								ContentValues values = new ContentValues();
								values.put(Widgets.SCROLLABLE, scrollableVersion);
								// set the scrollable version
								this.getContentResolver().update(Widgets.CONTENT_URI, values, Widgets.WIDGET + "=?", new String[] {Integer.toString(appWidgetId)});
								putNewUpdate(appWidgetId, 1);
							} else {
								putNewUpdate(appWidgetId, 0);
							}
						} else {
							ContentValues values = new ContentValues();
							values.put(Widgets.SCROLLABLE, scrollableVersion);
							// set the scrollable version
							this.getContentResolver().update(Widgets.CONTENT_URI, values, Widgets.WIDGET + "=?", new String[] {Integer.toString(appWidgetId)});
							putNewUpdate(appWidgetId, 1);
						}
						widget.close();
					} else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
						// requery
						putNewUpdate(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID), 0);
					}
				} else if (action.equals(SMS_RECEIVED)) {
					// parse the sms, and notify any widgets which have sms enabled
					Bundle bundle = intent.getExtras();
					Object[] pdus = (Object[]) bundle.get("pdus");
					for (int i = 0; i < pdus.length; i++) {
						SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
						AsyncTask<SmsMessage, String, int[]> smsLoader = new AsyncTask<SmsMessage, String, int[]>() {

							@Override
							protected int[] doInBackground(SmsMessage... msg) {
								// check if SMS is enabled anywhere
								Cursor widgets = getContentResolver().query(Widget_accounts_view.CONTENT_URI, new String[]{Widget_accounts_view._ID, Widget_accounts_view.WIDGET, Widget_accounts_view.ACCOUNT}, Widget_accounts_view.SERVICE + "=?", new String[]{Integer.toString(SMS)}, null);
								int[] appWidgetIds = new int[widgets.getCount()];
								if (widgets.moveToFirst()) {
									// insert this message to the statuses db and requery scrollable/rebuild widget
									// check if this is a contact
									String phone = msg[0].getOriginatingAddress();
									String friend = phone;
									byte[] profile = null;
									Uri content_uri = null;
									// unknown numbers crash here in the emulator
									Cursor phones = getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone)), new String[]{ContactsContract.PhoneLookup._ID}, null, null, null);
									if (phones.moveToFirst()) {
										content_uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, phones.getLong(0));
									} else {
										Cursor emails = getContentResolver().query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(phone)), new String[]{ContactsContract.CommonDataKinds.Email._ID}, null, null, null);
										if (emails.moveToFirst()) {
											content_uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, emails.getLong(0));
										}
										emails.close();
									}
									phones.close();
									if (content_uri != null) {
										// load contact
										Cursor contacts = getContentResolver().query(content_uri, new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
										if (contacts.moveToFirst()) {
											friend = contacts.getString(0);
										}
										contacts.close();
										profile = getBlob(ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), content_uri));
									}
									long accountId = widgets.getLong(2);
									long id;
									Cursor entity = getContentResolver().query(Entities.CONTENT_URI, new String[]{Entities._ID}, Entities.ACCOUNT + "=? and " + Entities.ESID + "=?", new String[]{Long.toString(accountId), mSonetCrypto.Encrypt(phone)}, null);
									if (entity.moveToFirst()) {
										id = entity.getLong(0);
										ContentValues values = new ContentValues();
										values.put(Entities.ESID, phone);
										values.put(Entities.FRIEND, friend);
										values.put(Entities.PROFILE, profile);
										values.put(Entities.ACCOUNT, accountId);
										getContentResolver().update(Entities.CONTENT_URI, values, Entities._ID + "=?", new String[]{Long.toString(id)});
									} else {
										ContentValues values = new ContentValues();
										values.put(Entities.ESID, phone);
										values.put(Entities.FRIEND, friend);
										values.put(Entities.PROFILE, profile);
										values.put(Entities.ACCOUNT, accountId);
										id = Long.parseLong(getContentResolver().insert(Entities.CONTENT_URI, values).getLastPathSegment());
									}
									entity.close();
									ContentValues values = new ContentValues();
									Long created = msg[0].getTimestampMillis();
									values.put(Statuses.CREATED, created);
									values.put(Statuses.ENTITY, id);
									values.put(Statuses.MESSAGE, msg[0].getMessageBody());
									values.put(Statuses.SERVICE, SMS);
									while (!widgets.isAfterLast()) {
										int widget = widgets.getInt(1);
										appWidgetIds[widgets.getPosition()] = widget;
										// get settings
										boolean time24hr = true;
										int status_bg_color = Sonet.default_message_bg_color;
										int profile_bg_color = Sonet.default_message_bg_color;
										int friend_bg_color = Sonet.default_friend_bg_color;
										boolean icon = true;
										int status_count = Sonet.default_statuses_per_account;
										int notifications = 0;
										Cursor c = SonetService.this.getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(widget), Long.toString(accountId)}, null);
										if (!c.moveToFirst()) {
											c.close();
											c = SonetService.this.getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(widget), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
											if (!c.moveToFirst()) {
												c.close();
												c = SonetService.this.getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
												if (!c.moveToFirst()) {
													// initialize account settings
													ContentValues v = new ContentValues();
													v.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
													v.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
													getContentResolver().insert(Widgets.CONTENT_URI, v).getLastPathSegment();													
												}
												if (widget != AppWidgetManager.INVALID_APPWIDGET_ID) {
													// initialize account settings
													ContentValues v = new ContentValues();
													v.put(Widgets.WIDGET, widget);
													v.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
													getContentResolver().insert(Widgets.CONTENT_URI, v).getLastPathSegment();
												}
											}
											// initialize account/widget settings
											ContentValues v = new ContentValues();
											v.put(Widgets.WIDGET, widget);
											v.put(Widgets.ACCOUNT, Long.toString(accountId));
											getContentResolver().insert(Widgets.CONTENT_URI, v).getLastPathSegment();
										}
										if (c.moveToFirst()) {
											time24hr = c.getInt(0) == 1;
											status_bg_color = c.getInt(1);
											icon = c.getInt(2) == 1;
											status_count = c.getInt(3);
											if (c.getInt(4) == 1) {
												notifications |= Notification.DEFAULT_SOUND;
											}
											if (c.getInt(5) == 1) {
												notifications |= Notification.DEFAULT_VIBRATE;
											}
											if (c.getInt(6) == 1) {
												notifications |= Notification.DEFAULT_LIGHTS;
											}
											profile_bg_color = c.getInt(7);
											friend_bg_color = c.getInt(8);
										}
										c.close();
										values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
										// update the bg and icon
										byte[] bg;
										// create the status_bg
										Bitmap bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
										Canvas bg_canvas = new Canvas(bg_bmp);
										bg_canvas.drawColor(status_bg_color);
										ByteArrayOutputStream bg_blob = new ByteArrayOutputStream();
										bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, bg_blob);
										bg = bg_blob.toByteArray();
										values.put(Statuses.STATUS_BG, bg);
										if (bg_bmp != null) {
											bg_bmp.recycle();
											bg_bmp = null;
										}
										// friend_bg
										bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
										bg_canvas = new Canvas(bg_bmp);
										bg_canvas.drawColor(friend_bg_color);
										bg_blob = new ByteArrayOutputStream();
										bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, bg_blob);
										bg = bg_blob.toByteArray();
										values.put(Statuses.FRIEND_BG, bg);
										if (bg_bmp != null) {
											bg_bmp.recycle();
											bg_bmp = null;
										}
										// profile_bg
										bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
										bg_canvas = new Canvas(bg_bmp);
										bg_canvas.drawColor(profile_bg_color);
										bg_blob = new ByteArrayOutputStream();
										bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, bg_blob);
										bg = bg_blob.toByteArray();
										values.put(Statuses.PROFILE_BG, bg);
										if (bg_bmp != null) {
											bg_bmp.recycle();
											bg_bmp = null;
										}
										values.put(Statuses.ICON, icon ? getBlob(getResources(), map_icons[SMS]) : null);
										// insert the message
										values.put(Statuses.WIDGET, widget);
										values.put(Statuses.ACCOUNT, accountId);
										getContentResolver().insert(Statuses.CONTENT_URI, values);
										// check the status count, removing old sms
										Cursor statuses = getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID}, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(widget), Long.toString(accountId)}, Statuses.CREATED + " desc");
										if (statuses.moveToFirst()) {
											while (!statuses.isAfterLast()) {
												if (statuses.getPosition() >= status_count) {
													getContentResolver().delete(Statuses.CONTENT_URI, Statuses._ID + "=?", new String[]{Long.toString(statuses.getLong(statuses.getColumnIndex(Statuses._ID)))});
												}
												statuses.moveToNext();
											}
										}
										statuses.close();
										if (notifications != 0) {
											publishProgress(Integer.toString(notifications), friend + " sent a message");
										}
										widgets.moveToNext();
									}
								}
								widgets.close();
								return appWidgetIds;
							}

							@Override
							protected void onProgressUpdate(String... updates) {
								int notifications = Integer.parseInt(updates[0]);
								if (notifications != 0) {
									Notification notification = new Notification(R.drawable.notification, updates[1], System.currentTimeMillis());
									notification.setLatestEventInfo(getBaseContext(), "New messages", updates[1], PendingIntent.getActivity(SonetService.this, 0, (new Intent(SonetService.this, SonetNotifications.class)), 0));
									notification.defaults |= notifications;
									((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
								}
							}

							@Override
							protected void onPostExecute(int[] appWidgetIds) {
								// remove self from thread list
								if (!mSMSLoaders.isEmpty()) {
									mSMSLoaders.remove(this);
								}
								for (int appWidgetId : appWidgetIds) {
									putNewUpdate(appWidgetId, 0);
								}
								processUpdates(SonetService.this);
							}

						};
						mSMSLoaders.add(smsLoader);
						smsLoader.execute(msg);
					}
				} else if (action.equals(ACTION_PAGE_DOWN)) {
					(new AsyncTask<Integer, Void, Void>() {
						@Override
						protected Void doInBackground(Integer... arg0) {
							// rebuild the widget, using the paging criteria passed in
							buildWidgetButtons(arg0[0], true, arg0[1]);
							return null;
						}
					}).execute(Integer.parseInt(intent.getData().getLastPathSegment()), intent.getIntExtra(ACTION_PAGE_DOWN, 0));
				} else if (action.equals(ACTION_PAGE_UP)) {
					(new AsyncTask<Integer, Void, Void>() {
						@Override
						protected Void doInBackground(Integer... arg0) {
							// rebuild the widget, using the paging criteria passed in
							buildWidgetButtons(arg0[0], true, arg0[1]);
							return null;
						}
					}).execute(Integer.parseInt(intent.getData().getLastPathSegment()), intent.getIntExtra(ACTION_PAGE_UP, 0));
				} else {
					// this might be a widget update from the widget refresh button
					int appWidgetId;
					try {
						appWidgetId = Integer.parseInt(action);
						putNewUpdate(appWidgetId, 1);
					} catch (NumberFormatException e) {
						Log.d(TAG,"unknown action:" + action);
					}
				}
			}
		}
		processUpdates(this);
	}

	private static synchronized void processUpdates(SonetService service) {
		HashMap<Integer, Integer> appWidgetIdsReprocessing = new HashMap<Integer, Integer>();
		for (Map.Entry<Integer, Integer> entry : mAppWidgetIdsQueued.entrySet()) {
			// check if this update is already processing
			if (mAppWidgetIdsProcessing.isEmpty() || !mAppWidgetIdsProcessing.containsKey(entry.getKey())) {
				// start processing
				mAppWidgetIdsProcessing.put(entry.getKey(), entry.getValue());
				StatusesLoader loader = service.new StatusesLoader();
				mStatusesLoaders.put(entry.getKey(), loader);
				loader.execute(entry.getKey(), entry.getValue());
			} else if (appWidgetIdsReprocessing.isEmpty() || !appWidgetIdsReprocessing.containsKey(entry.getKey()) || (appWidgetIdsReprocessing.get(entry.getKey()) < entry.getValue())) {
				// already processing, shelve to reprocessing until current processing is complete
				// check that a reload doesn't override a non-reload
				appWidgetIdsReprocessing.put(entry.getKey(), entry.getValue());
			}
		}
		mAppWidgetIdsQueued.clear();
		if (!appWidgetIdsReprocessing.isEmpty()) {
			// reset the queue to those that are awaiting processing
			mAppWidgetIdsQueued = appWidgetIdsReprocessing;
		}

	}

	private static HashMap<Integer, Integer> mAppWidgetIdsQueued = new HashMap<Integer, Integer>();
	private static HashMap<Integer, Integer> mAppWidgetIdsProcessing = new HashMap<Integer, Integer>();

	protected static synchronized void putNewUpdate(int widget, int reload) {
		// if the widget is already loading, don't load another
		// always add this update to the queue
		if (mAppWidgetIdsQueued.isEmpty() || !mAppWidgetIdsQueued.containsKey(widget) || (mAppWidgetIdsQueued.get(widget) < reload)) {
			mAppWidgetIdsQueued.put(widget, reload);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		// clean up
		if (mInstantUpload != null) {
			getContentResolver().unregisterContentObserver(mInstantUpload);
			mInstantUpload = null;
		}
		if (!mStatusesLoaders.isEmpty()) {
			Iterator<AsyncTask<Integer, String, Integer>> itr = mStatusesLoaders.values().iterator();
			while (itr.hasNext()) {
				AsyncTask<Integer, String, Integer> statusesLoader = itr.next();
				statusesLoader.cancel(true);
			}
			mStatusesLoaders.clear();
		}
		if (!mSMSLoaders.isEmpty()) {
			Iterator<AsyncTask<SmsMessage, String, int[]>> itr = mSMSLoaders.iterator();
			while (itr.hasNext()) {
				AsyncTask<SmsMessage, String, int[]> smsLoader = itr.next();
				smsLoader.cancel(true);
			}
			mSMSLoaders.clear();
		}
		super.onDestroy();
	}

	class StatusesLoader extends AsyncTask<Integer, String, Integer> {

		@Override
		protected Integer doInBackground(Integer... params) {
			// first handle deletes, then scroll updates, finally regular updates
			final int appWidgetId = params[0];
			final String widget = Integer.toString(appWidgetId);
			final boolean reload = params[1] != 0;
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				Log.d(TAG,(reload ? "reload" : "load from cache"));
			}
			// the widget will start out as the default widget.xml, which simply says "loading..."
			// if there's a cache, that should be quickly reloaded while new updates come down
			// otherwise, replace the widget with "loading..."
			// clear any messages
			getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)});
			Cursor statuses = getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID}, Statuses.WIDGET + "=?", new String[]{widget}, null);
			boolean hasCache = statuses.moveToFirst();
			statuses.close();
			if (!hasCache) {
				// if there's a cache, let it remain out there, otherwise inform the user that the widget is loading
				addStatusItem(System.currentTimeMillis(), "", null, getString(R.string.loading), 0, false, appWidgetId, Sonet.INVALID_ACCOUNT_ID, "-1", "-1", new ArrayList<String[]>());
			}
			// loading takes time, so don't leave an empty widget sitting there
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				// build the widget
				buildWidgetButtons(appWidgetId, false, 0);
			} else {
				// update the About.java for in-app viewing
				getContentResolver().notifyChange(Statuses_styles.CONTENT_URI, null);
			}
			int refreshInterval = Sonet.default_interval;
			boolean backgroundUpdate = true;
			boolean display_profile = true;
			Cursor settings = getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.INTERVAL, Widgets.BACKGROUND_UPDATE, Widgets.DISPLAY_PROFILE}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
			if (!settings.moveToFirst()) {
				settings.close();
				settings = getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.INTERVAL, Widgets.BACKGROUND_UPDATE, Widgets.DISPLAY_PROFILE}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
				if (!settings.moveToFirst()) {
					// initialize account settings
					ContentValues values = new ContentValues();
					values.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
					values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
					getContentResolver().insert(Widgets.CONTENT_URI, values);
				}
				// don't insert a duplicate row
				if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
					// initialize account settings
					ContentValues values = new ContentValues();
					values.put(Widgets.WIDGET, widget);
					values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
					getContentResolver().insert(Widgets.CONTENT_URI, values);
				}
			}
			if (settings.moveToFirst()) {
				refreshInterval = settings.getInt(0);
				backgroundUpdate = settings.getInt(1) == 1;
				display_profile = settings.getInt(2) == 1;
			}
			settings.close();
			// the alarm should always be set, rather than depend on the tasks to complete
			if ((appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) && (!hasCache || reload) && (refreshInterval > 0)) {
				mAlarmManager.cancel(PendingIntent.getService(SonetService.this, 0, new Intent(SonetService.this, SonetService.class).setAction(widget), 0));
				mAlarmManager.set(backgroundUpdate ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC, System.currentTimeMillis() + refreshInterval, PendingIntent.getService(SonetService.this, 0, new Intent(SonetService.this, SonetService.class).setData(Uri.withAppendedPath(Widgets.CONTENT_URI, widget)).setAction(ACTION_REFRESH), 0));
			}
			// query accounts
			/* get statuses for all accounts
			 * then sort them by datetime, descending
			 */
			Cursor accounts = getContentResolver().query(Widget_accounts_view.CONTENT_URI, new String[]{Widget_accounts_view.ACCOUNT, Widget_accounts_view.TOKEN, Widget_accounts_view.SECRET, Widget_accounts_view.SERVICE, Widget_accounts_view.SID}, Widget_accounts_view.WIDGET + "=?", new String[]{widget}, null);
			if (accounts.moveToFirst()) {
				// only reload if the token's can be decrypted and if there's no cache or a reload is requested
				if (!hasCache || reload) {
					mNotify = null;
					int notifications = 0;
					final SonetHttpClient sonetHttpClient = SonetHttpClient.getInstance(getApplicationContext());
					final ArrayList<String[]> links = new ArrayList<String[]>();
					// load the updates
					while (!accounts.isAfterLast()) {
						final long account = accounts.getLong(0);
						final int service = accounts.getInt(3);
						final String token = mSonetCrypto.Decrypt(accounts.getString(1));
						final String secret = mSonetCrypto.Decrypt(accounts.getString(2));
						final String accountEsid = mSonetCrypto.Decrypt(accounts.getString(4));
						// get the settings form time24hr and bg_color
						boolean time24hr = false;
						int status_bg_color = Sonet.default_message_bg_color;
						int profile_bg_color = Sonet.default_message_bg_color;
						int friend_bg_color = Sonet.default_friend_bg_color;
						boolean icon = true;
						int status_count = Sonet.default_statuses_per_account;
						Cursor c = SonetService.this.getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(account)}, null);
						if (!c.moveToFirst()) {
							// no account settings
							c.close();
							c = SonetService.this.getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
							if (!c.moveToFirst()) {
								// no widget settings
								c.close();
								c = SonetService.this.getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
								if (!c.moveToFirst()) {
									// initialize widget settings
									ContentValues values = new ContentValues();
									values.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
									values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
									getContentResolver().insert(Widgets.CONTENT_URI, values).getLastPathSegment();
								}
								// don't insert a duplicate row
								if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
									// initialize account settings
									ContentValues values = new ContentValues();
									values.put(Widgets.WIDGET, widget);
									values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
									getContentResolver().insert(Widgets.CONTENT_URI, values).getLastPathSegment();
								}
							}
							// initialize account settings
							ContentValues values = new ContentValues();
							values.put(Widgets.WIDGET, widget);
							values.put(Widgets.ACCOUNT, Long.toString(account));
							getContentResolver().insert(Widgets.CONTENT_URI, values).getLastPathSegment();
						}
						if (c.moveToFirst()) {
							time24hr = c.getInt(0) == 1;
							status_bg_color = c.getInt(1);
							icon = c.getInt(2) == 1;
							status_count = c.getInt(3);
							if (c.getInt(4) == 1) {
								notifications |= Notification.DEFAULT_SOUND;
							}
							if (c.getInt(5) == 1) {
								notifications |= Notification.DEFAULT_VIBRATE;
							}
							if (c.getInt(6) == 1) {
								notifications |= Notification.DEFAULT_LIGHTS;
							}
							profile_bg_color = c.getInt(7);
							friend_bg_color = c.getInt(8);
						}
						c.close();
						// if no connection, only update the status_bg and icons
						if ((mConnectivityManager.getActiveNetworkInfo() != null) && mConnectivityManager.getActiveNetworkInfo().isConnected()) {
							// get this account's statuses
							boolean updateCreatedText = false;
							SonetOAuth sonetOAuth;
							String response;
							HttpGet httpGet;
							final ArrayList<String> notificationSids = new ArrayList<String>();
							mSimpleDateFormat = null;
							JSONArray statusesArray;
							JSONObject statusObj;
							JSONObject friendObj;
							JSONArray commentsArray;
							JSONObject commentObj;
							Cursor currentNotifications;
							String sid;
							String esid;
							long notificationId;
							long updated;
							boolean cleared;
							String friend;
							switch (service) {
							case TWITTER:
								sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, token, secret);
								// parse the response
								if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(new HttpGet(String.format(TWITTER_URL_FEED, TWITTER_BASE_URL, status_count))))) != null) {
									// if not a full_refresh, only update the status_bg and icons
									try {
										statusesArray = new JSONArray(response);
										// if there are updates, clear the cache
										int e2 = statusesArray.length();
										if (e2 > 0) {
											removeOldStatuses(widget, Long.toString(account));
											for (int e = 0; (e < e2) && (e < status_count); e++) {
												links.clear();
												statusObj = statusesArray.getJSONObject(e);
												friendObj = statusObj.getJSONObject(Suser);
												addStatusItem(parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT),
														friendObj.getString(Sname),
														display_profile ? friendObj.getString(Sprofile_image_url) : null,
																statusObj.getString(Stext),
																service,
																time24hr,
																appWidgetId,
																account,
																statusObj.getString(Sid),
																friendObj.getString(Sid),
																links);
											}
										} else {
											updateCreatedText = true;
										}
									} catch (JSONException e) {
										Log.e(TAG, service + ":" + e.toString());
									}
								} else {
									updateCreatedText = true;
								}
								// notifications
								if (notifications != 0) {
									currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications.SID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);
									// loop over notifications
									if (currentNotifications.moveToFirst()) {
										// store sids, to avoid duplicates when requesting the latest feed
										sid = mSonetCrypto.Decrypt(currentNotifications.getString(0));
										if (!notificationSids.contains(sid)) {
											notificationSids.add(sid);
										}
									}
									currentNotifications.close();
									// limit to oldest status
									String last_sid = null;
									Cursor last_status = getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses.SID}, Statuses.ACCOUNT + "=?", new String[]{Long.toString(account)}, Statuses.CREATED + " ASC LIMIT 1");
									if (last_status.moveToFirst()) {
										last_sid = mSonetCrypto.Decrypt(last_status.getString(0));
									}
									last_status.close();
									// get all mentions since the oldest status for this account
									if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(new HttpGet(String.format(TWITTER_MENTIONS, TWITTER_BASE_URL, last_sid != null ? String.format(TWITTER_SINCE_ID, last_sid) : ""))))) != null) {
										try {
											statusesArray = new JSONArray(response);
											for (int i = 0, i2 = statusesArray.length(); i < i2; i++) {
												statusObj = statusesArray.getJSONObject(i);
												friendObj = statusObj.getJSONObject(Suser);
												if (!friendObj.getString(Sid).equals(accountEsid) && !notificationSids.contains(statusObj.getString(Sid))) {
													friend = friendObj.getString(Sname);
													addNotification(statusObj.getString(Sid), friendObj.getString(Sid), friend, statusObj.getString(Stext), parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT), account, friend + " mentioned you on Twitter");
												}
											}
										} catch (JSONException e) {
											Log.e(TAG, service + ":" + e.toString());
										}
									}
								}
								break;
							case FACEBOOK:
								// notifications first to populate notificationsSids
								if (notifications != 0) {
									currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);
									// loop over notifications
									if (currentNotifications.moveToFirst()) {
										while (!currentNotifications.isAfterLast()) {
											notificationId = currentNotifications.getLong(0);
											sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
											updated = currentNotifications.getLong(2);
											cleared = currentNotifications.getInt(3) == 1;
											// store sids, to avoid duplicates when requesting the latest feed
											if (!notificationSids.contains(sid)) {
												notificationSids.add(sid);
											}
											// get comments for current notifications
											if ((response = sonetHttpClient.httpResponse(new HttpGet(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, sid, Saccess_token, token)))) != null) {
												// check for a newer post, if it's the user's own, then set CLEARED=0
												try {
													commentsArray = new JSONObject(response).getJSONArray(Sdata);
													final int i2 = commentsArray.length();
													if (i2 > 0) {
														for (int i = 0; i < i2; i++) {
															commentObj = commentsArray.getJSONObject(i);
															final long created_time = commentObj.getLong(Screated_time) * 1000;
															if (created_time > updated) {
																final JSONObject from = commentObj.getJSONObject(Sfrom);
																updateNotification(notificationId, created_time, accountEsid, from.getString(Sid), from.getString(Sname), cleared);
															}
														}
													}
												} catch (JSONException e) {
													Log.e(TAG, service + ":" + e.toString() + ":" + response);
												}
											}
											currentNotifications.moveToNext();
										}
									}
									currentNotifications.close();
								}
								// parse the response
								if ((response = sonetHttpClient.httpResponse(new HttpGet(String.format(FACEBOOK_HOME, FACEBOOK_BASE_URL, Saccess_token, token)))) != null) {
									String profile = "http://graph.facebook.com/%s/picture";
									try {
										statusesArray = new JSONObject(response).getJSONArray(Sdata);
										// if there are updates, clear the cache
										int d2 = statusesArray.length();
										if (d2 > 0) {
											removeOldStatuses(widget, Long.toString(account));
											for (int d = 0; d < d2; d++) {
												links.clear();
												statusObj = statusesArray.getJSONObject(d);
												// only parse status types, not photo, video or link
												if (statusObj.has(Stype) && statusObj.has(Sfrom) && statusObj.has(Sid)) {
													friendObj = statusObj.getJSONObject("from");
													if (friendObj.has(Sname) && friendObj.has(Sid)) {
														friend = friendObj.getString(Sname);
														esid = friendObj.getString(Sid);
														sid = statusObj.getString(Sid);
														StringBuilder message = new StringBuilder();
														if (statusObj.has(Smessage)) {
															message.append(statusObj.getString(Smessage));
														} else if (statusObj.has(Sstory)) {
															message.append(statusObj.getString(Sstory));
														}
														if (statusObj.has(Slink)) {
															links.add(new String[]{statusObj.getString(Stype), statusObj.getString(Slink)});
															message.append("(");
															message.append(statusObj.getString(Stype));
															message.append(": ");
															message.append(Uri.parse(statusObj.getString(Slink)).getHost());
															message.append(")");
														}
														if (statusObj.has(Ssource)) {
															links.add(new String[]{statusObj.getString(Stype), statusObj.getString(Ssource)});
															message.append("(");
															message.append(statusObj.getString(Stype));
															message.append(": ");
															message.append(Uri.parse(statusObj.getString(Ssource)).getHost());
															message.append(")");
														}
														long date = statusObj.getLong(Screated_time) * 1000;
														String notification = null;
														if (statusObj.has(Sto)) {
															// handle wall messages from one friend to another
															JSONObject t = statusObj.getJSONObject(Sto);
															if (t.has(Sdata)) {
																JSONObject n = t.getJSONArray(Sdata).getJSONObject(0);
																if (n.has(Sname)) {
																	friend += " > " + n.getString(Sname);
																	if (!notificationSids.contains(sid) && n.has(Sid) && (n.getString(Sid).equals(accountEsid))) {
																		notification = String.format(getString(R.string.friendcommented), friend);
																	}
																}
															}												
														}
														int commentCount = 0;
														if (statusObj.has(Scomments)) {
															JSONObject jo = statusObj.getJSONObject(Scomments);
															if (jo.has(Sdata)) {
																commentsArray = jo.getJSONArray(Sdata);
																commentCount = commentsArray.length();
																if (!notificationSids.contains(sid) && (commentCount > 0)) {
																	// default hasCommented to whether or not these comments are for the own user's status
																	boolean hasCommented = notification != null || esid.equals(accountEsid);
																	for (int c2 = 0; c2 < commentCount; c2++) {
																		commentObj = commentsArray.getJSONObject(c2);
																		// if new notification, or updated
																		if (commentObj.has(Sfrom)) {
																			JSONObject c4 = commentObj.getJSONObject(Sfrom);
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
														if ((notifications != 0) && (notification != null)) {
															// new notification
															addNotification(sid, esid, friend, message.toString(), statusObj.getLong(Screated_time) * 1000, account, notification);
														}
														if (d < status_count) {
															addStatusItem(date,
																	friend,
																	display_profile ? String.format(profile, esid) : null,
																			String.format(getString(R.string.messageWithCommentCount), message.toString(), commentCount),
																			service,
																			time24hr,
																			appWidgetId,
																			account,
																			sid,
																			esid,
																			links);
														}
													}
												}
											}
										} else {
											updateCreatedText = true;
										}
									} catch (JSONException e) {
										Log.e(TAG, service + ":" + e.toString() + ":" + response);
									}
								} else {
									updateCreatedText = true;
								}
								break;
							case MYSPACE:
								sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, token, secret);
								// notifications
								if (notifications != 0) {
									currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);
									// loop over notifications
									if (currentNotifications.moveToFirst()) {
										while (!currentNotifications.isAfterLast()) {
											notificationId = currentNotifications.getLong(0);
											sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
											updated = currentNotifications.getLong(2);
											cleared = currentNotifications.getInt(3) == 1;
											esid = mSonetCrypto.Decrypt(currentNotifications.getString(4));
											// store sids, to avoid duplicates when requesting the latest feed
											if (!notificationSids.contains(sid)) {
												notificationSids.add(sid);
											}
											// get comments for current notifications
											if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, esid, sid))))) != null) {
												// check for a newer post, if it's the user's own, then set CLEARED=0
												try {
													commentsArray = new JSONObject(response).getJSONArray(Sentry);
													final int i2 = commentsArray.length();
													if (i2 > 0) {
														for (int i = 0; i < i2; i++) {
															commentObj = commentsArray.getJSONObject(i);
															long created_time = parseDate(commentObj.getString(SpostedDate), MYSPACE_DATE_FORMAT);
															if (created_time > updated) {
																friendObj = commentObj.getJSONObject(Sauthor);
																updateNotification(notificationId, created_time, accountEsid, friendObj.getString(Sid), friendObj.getString(SdisplayName), cleared);
															}
														}
													}
												} catch (JSONException e) {
													Log.e(TAG, service + ":" + e.toString());
												}
											}
											currentNotifications.moveToNext();
										}
									}
									currentNotifications.close();
								}
								// parse the response
								if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_HISTORY, MYSPACE_BASE_URL))))) != null) {
									try {
										statusesArray = new JSONObject(response).getJSONArray(Sentry);
										// if there are updates, clear the cache
										int e2 = statusesArray.length();
										if (e2 > 0) {
											removeOldStatuses(widget, Long.toString(account));
											for (int e = 0; e < e2; e++) {
												links.clear();
												statusObj = statusesArray.getJSONObject(e);
												friendObj = statusObj.getJSONObject(Sauthor);
												long date = parseDate(statusObj.getString(SmoodStatusLastUpdated), MYSPACE_DATE_FORMAT);
												esid = statusObj.getString(SuserId);
												int commentCount = 0;
												sid = statusObj.getString(SstatusId);
												friend = friendObj.getString(SdisplayName);
												String statusValue = statusObj.getString(Sstatus);
												String notification = null;
												if (statusObj.has(SrecentComments)) {
													commentsArray = statusObj.getJSONArray(SrecentComments);
													commentCount = commentsArray.length();
													// notifications
													if ((sid != null) && !notificationSids.contains(sid) && (commentCount > 0)) {
														// default hasCommented to whether or not these comments are for the own user's status
														boolean hasCommented = notification != null || esid.equals(accountEsid);
														for (int c2 = 0; c2 < commentCount; c2++) {
															commentObj = commentsArray.getJSONObject(c2);
															if (commentObj.has(Sauthor)) {
																JSONObject c4 = commentObj.getJSONObject(Sauthor);
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
												}
												if ((notifications != 0) && (notification != null)) {
													// new notification
													addNotification(sid, esid, friend, statusValue, date, account, notification);
												}
												if (e < status_count) {
													addStatusItem(date,
															friend,
															display_profile ? friendObj.getString(SthumbnailUrl) : null,
																	String.format(getString(R.string.messageWithCommentCount), statusValue, commentCount),
																	service,
																	time24hr,
																	appWidgetId,
																	account,
																	sid,
																	esid,
																	links);
												}
											}
										} else {
											updateCreatedText = true;
										}
									} catch (JSONException e) {
										Log.e(TAG, service + ":" + e.toString());
									}
								} else {
									updateCreatedText = true;
								}
								break;
							case BUZZ:
								sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, token, secret);
								// notifications
								if (notifications != 0) {
									currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);
									// loop over notifications
									if (currentNotifications.moveToFirst()) {
										while (!currentNotifications.isAfterLast()) {
											notificationId = currentNotifications.getLong(0);
											sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
											updated = currentNotifications.getLong(2);
											cleared = currentNotifications.getInt(3) == 1;
											// store sids, to avoid duplicates when requesting the latest feed
											if (!notificationSids.contains(sid)) {
												notificationSids.add(sid);
											}
											// get comments for current notifications
											if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(new HttpGet(String.format(BUZZ_COMMENT, BUZZ_BASE_URL, sid, BUZZ_API_KEY))))) != null) {
												// check for a newer post, if it's the user's own, then set CLEARED=0
												try {
													commentsArray = new JSONObject(response).getJSONObject(Sdata).getJSONArray(Sitems);
													int i2 = commentsArray.length();
													if (i2 > 0) {
														for (int i = 0; i < i2; i++) {
															JSONObject comment = commentsArray.getJSONObject(i);
															long created_time = parseDate(comment.getString(Spublished), BUZZ_DATE_FORMAT);
															if (created_time > updated) {
																JSONObject actor = comment.getJSONObject(Sactor);
																updateNotification(notificationId, created_time, accountEsid, actor.getString(Sid), actor.getString(Sname), cleared);
															}
														}
													}
												} catch (JSONException e) {
													Log.e(TAG, service + ":" + e.toString());
												}
											}
											currentNotifications.moveToNext();
										}
									}
									currentNotifications.close();
								}
								// parse the response
								if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(new HttpGet(String.format(BUZZ_ACTIVITIES, BUZZ_BASE_URL, BUZZ_API_KEY))))) != null) {
									try {
										statusesArray = new JSONObject(response).getJSONObject(Sdata).getJSONArray(Sitems);
										// if there are updates, clear the cache
										int e2 = statusesArray.length();
										if (e2 > 0) {
											removeOldStatuses(widget, Long.toString(account));
											for (int e = 0; e < e2; e++) {
												links.clear();
												statusObj = statusesArray.getJSONObject(e);
												if (statusObj.has(Spublished) && statusObj.has(Sactor) && statusObj.has(Sobject)) {
													friendObj = statusObj.getJSONObject(Sactor);
													JSONObject object = statusObj.getJSONObject(Sobject);
													if (friendObj.has(Sname) && friendObj.has(SthumbnailUrl) && object.has(SoriginalContent)) {
														long date = parseDate(statusObj.getString(Spublished), BUZZ_DATE_FORMAT);
														esid = friendObj.getString(Sid);
														int commentCount = 0;
														sid = statusObj.getString(Sid);
														String originalContent = object.getString(SoriginalContent);
														friend = friendObj.getString(Sname);
														String notification = null;
														if (object.has(Scomments)) {
															commentsArray = object.getJSONArray(Scomments);
															commentCount = commentsArray.length();
															if (!notificationSids.contains(sid) && (commentCount > 0)) {
																// default hasCommented to whether or not these comments are for the own user's status
																boolean hasCommented = notification != null || esid.equals(accountEsid);
																for (int c2 = 0; c2 < commentCount; c2++) {
																	commentObj = commentsArray.getJSONObject(c2);
																	if (commentObj.has(Sactor)) {
																		JSONObject c4 = commentObj.getJSONObject(Sactor);
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
														if ((notifications != 0) && (notification != null)) {
															// new notification
															addNotification(sid, esid, friend, originalContent, date, account, notification);
														}
														if (e < status_count) {
															addStatusItem(date,
																	friend,
																	display_profile ? friendObj.getString(SthumbnailUrl) : null,
																			String.format(getString(R.string.messageWithCommentCount), originalContent, commentCount),
																			service,
																			time24hr,
																			appWidgetId,
																			account,
																			sid,
																			esid,
																			links);
														}
													}
												}
											}
										} else {
											updateCreatedText = true;
										}
									} catch (JSONException e) {
										Log.e(TAG, service + ":" + e.toString());
									}
								} else {
									updateCreatedText = true;
								}
								break;
							case FOURSQUARE:
								// notifications
								if (notifications != 0) {
									currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);
									// loop over notifications
									if (currentNotifications.moveToFirst()) {
										while (!currentNotifications.isAfterLast()) {
											notificationId = currentNotifications.getLong(0);
											sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
											updated = currentNotifications.getLong(2);
											cleared = currentNotifications.getInt(3) == 1;
											// store sids, to avoid duplicates when requesting the latest feed
											if (!notificationSids.contains(sid)) {
												notificationSids.add(sid);
											}
											// get comments for current notifications
											if ((response = sonetHttpClient.httpResponse(new HttpGet(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, sid, token)))) != null) {
												// check for a newer post, if it's the user's own, then set CLEARED=0
												try {
													commentsArray = new JSONObject(response).getJSONObject(Sresponse).getJSONObject(Scheckin).getJSONObject(Scomments).getJSONArray(Sitems);
													int i2 = commentsArray.length();
													if (i2 > 0) {
														for (int i = 0; i < i2; i++) {
															commentObj = commentsArray.getJSONObject(i);
															long created_time = commentObj.getLong(ScreatedAt) * 1000;
															if (created_time > updated) {
																friendObj = commentObj.getJSONObject(Suser);
																updateNotification(notificationId, created_time, accountEsid, friendObj.getString(Sid), friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName), cleared);
															}
														}
													}
												} catch (JSONException e) {
													Log.e(TAG, service + ":" + e.toString());
												}
											}
											currentNotifications.moveToNext();
										}
									}
									currentNotifications.close();
								}
								// parse the response
								if ((response = sonetHttpClient.httpResponse(new HttpGet(String.format(FOURSQUARE_CHECKINS, FOURSQUARE_BASE_URL, token)))) != null) {
									try {
										statusesArray = new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Srecent);
										// if there are updates, clear the cache
										int e2 = statusesArray.length();
										if (e2 > 0) {
											removeOldStatuses(widget, Long.toString(account));
											for (int e = 0; e < e2; e++) {
												links.clear();
												statusObj = statusesArray.getJSONObject(e);
												friendObj = statusObj.getJSONObject(Suser);
												String shout = "";
												if (statusObj.has(Sshout)) {
													shout = statusObj.getString(Sshout) + "\n";
												}
												if (statusObj.has(Svenue)) {
													JSONObject venue = statusObj.getJSONObject(Svenue);
													if (venue.has(Sname)) {
														shout += "@" + venue.getString(Sname);																
													}
												}
												long date = statusObj.getLong(ScreatedAt) * 1000;
												// notifications
												esid = friendObj.getString(Sid);
												int commentCount = 0;
												sid = statusObj.getString(Sid);
												friend = friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName);
												String notification = null;
												if (statusObj.has(Scomments)) {
													commentsArray = statusObj.getJSONObject(Scomments).getJSONArray(Sitems);
													commentCount = commentsArray.length();
													if (!notificationSids.contains(sid) && (commentCount > 0)) {
														// default hasCommented to whether or not these comments are for the own user's status
														boolean hasCommented = notification != null || esid.equals(accountEsid);
														for (int c2 = 0; c2 < commentCount; c2++) {
															commentObj = commentsArray.getJSONObject(c2);
															if (commentObj.has(Suser)) {
																JSONObject c4 = commentObj.getJSONObject(Suser);
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
												}
												if ((notifications != 0) && (notification != null)) {
													// new notification
													addNotification(sid, esid, friend, shout, date, account, notification);
												}
												if (e < status_count) {
													addStatusItem(date,
															friend,
															display_profile ? friendObj.getString(Sphoto) : null,
																	String.format(getString(R.string.messageWithCommentCount), shout, commentCount),
																	service,
																	time24hr,
																	appWidgetId,
																	account,
																	sid,
																	esid,
																	links);
												}
											}
										} else {
											updateCreatedText = true;
										}
									} catch (JSONException e) {
										Log.e(TAG, service + ":" + e.toString());
										Log.e(TAG, response);
									}
								} else {
									updateCreatedText = true;
								}
								break;
							case LINKEDIN:
								sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, token, secret);
								// notifications
								if (notifications != 0) {
									currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);
									// loop over notifications
									if (currentNotifications.moveToFirst()) {
										while (!currentNotifications.isAfterLast()) {
											notificationId = currentNotifications.getLong(0);
											sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
											updated = currentNotifications.getLong(2);
											cleared = currentNotifications.getInt(3) == 1;
											// store sids, to avoid duplicates when requesting the latest feed
											if (!notificationSids.contains(sid)) {
												notificationSids.add(sid);
											}
											// get comments for current notifications
											httpGet = new HttpGet(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, sid));
											for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
											if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(httpGet))) != null) {
												// check for a newer post, if it's the user's own, then set CLEARED=0
												try {
													JSONObject jsonResponse = new JSONObject(response);
													if (jsonResponse.has(S_total) && (jsonResponse.getInt(S_total) != 0)) {
														commentsArray = jsonResponse.getJSONArray(Svalues);
														int i2 = commentsArray.length();
														if (i2 > 0) {
															for (int i = 0; i < i2; i++) {
																commentObj = commentsArray.getJSONObject(i);
																long created_time = commentObj.getLong(Stimestamp);
																if (created_time > updated) {
																	friendObj = commentObj.getJSONObject(Sperson);
																	updateNotification(notificationId, created_time, accountEsid, friendObj.getString(Sid), friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName), cleared);
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
									}
									currentNotifications.close();
								}
								httpGet = new HttpGet(String.format(LINKEDIN_UPDATES, LINKEDIN_BASE_URL));
								for (String[] header : LINKEDIN_HEADERS) {
									httpGet.setHeader(header[0], header[1]);
								}
								// parse the response
								if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(httpGet))) != null) {
									try {
										statusesArray = new JSONObject(response).getJSONArray(Svalues);
										// if there are updates, clear the cache
										int e2 = statusesArray.length();
										if (e2 > 0) {
											removeOldStatuses(widget, Long.toString(account));
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
											for (int e = 0; e < e2; e++) {
												links.clear();
												statusObj = statusesArray.getJSONObject(e);
												String updateType = statusObj.getString(SupdateType);
												JSONObject updateContent = statusObj.getJSONObject(SupdateContent);
												if (LINKEDIN_UPDATETYPES.containsKey(updateType) && updateContent.has(Sperson)) {
													friendObj = updateContent.getJSONObject(Sperson);
													String update = LINKEDIN_UPDATETYPES.get(updateType);
													if (updateType.equals(SAPPS)) {
														if (friendObj.has(SpersonActivities)) {
															JSONObject personActivities = friendObj.getJSONObject(SpersonActivities);
															if (personActivities.has(Svalues)) {
																JSONArray updates = personActivities.getJSONArray(Svalues);
																for (int u = 0, u2 = updates.length(); u < u2; u++) {
																	update += updates.getJSONObject(u).getString(Sbody);
																	if (u < (updates.length() - 1)) update += ", ";
																}
															}
														}
													} else if (updateType.equals(SCONN)) {
														if (friendObj.has(Sconnections)) {
															JSONObject connections = friendObj.getJSONObject(Sconnections);
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
														if (friendObj.has(SmemberGroups)) {
															JSONObject memberGroups = friendObj.getJSONObject(SmemberGroups);
															if (memberGroups.has(Svalues)) {
																JSONArray updates = memberGroups.getJSONArray(Svalues);
																for (int u = 0, u2 = updates.length(); u < u2; u++) {
																	update += updates.getJSONObject(u).getString(Sname);
																	if (u < (updates.length() - 1)) update += ", ";
																}
															}
														}
													} else if (updateType.equals(SPREC)) {
														if (friendObj.has(SrecommendationsGiven)) {
															JSONObject recommendationsGiven = friendObj.getJSONObject(SrecommendationsGiven);
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
													} else if (updateType.equals(SSHAR) && friendObj.has(ScurrentShare)) {
														JSONObject currentShare = friendObj.getJSONObject(ScurrentShare);
														if (currentShare.has(Scomment)) {
															update = currentShare.getString(Scomment);
														}
													}
													long date = statusObj.getLong(Stimestamp);
													sid = statusObj.has(SupdateKey) ? statusObj.getString(SupdateKey) : null;
													esid = friendObj.getString(Sid);
													friend = friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName);
													int commentCount = 0;
													String notification = null;
													if (statusObj.has(SupdateComments)) {
														JSONObject updateComments = statusObj.getJSONObject(SupdateComments);
														if (updateComments.has(Svalues)) {
															commentsArray = updateComments.getJSONArray(Svalues);
															commentCount = commentsArray.length();
															if (!notificationSids.contains(sid) && (commentCount > 0)) {
																// default hasCommented to whether or not these comments are for the own user's status
																boolean hasCommented = notification != null || esid.equals(accountEsid);
																for (int c2 = 0; c2 < commentCount; c2++) {
																	commentObj = commentsArray.getJSONObject(c2);
																	if (commentObj.has(Sperson)) {
																		JSONObject c4 = commentObj.getJSONObject(Sperson);
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
														}
													}
													if ((notifications != 0) && (notification != null)) {
														// new notification
														addNotification(sid, esid, friend, update, date, account, notification);
													}
													if (e < status_count) {
														addStatusItem(date,
																friend,
																display_profile && friendObj.has(SpictureUrl) ? friendObj.getString(SpictureUrl) : null,
																		(statusObj.has(SisCommentable) && statusObj.getBoolean(SisCommentable) ? String.format(getString(R.string.messageWithCommentCount), update, commentCount) : update),
																		service,
																		time24hr,
																		appWidgetId,
																		account,
																		sid,
																		esid,
																		links);
													}
												}
											}
										} else {
											updateCreatedText = true;
										}
									} catch (JSONException e) {
										Log.e(TAG, service + ":" + e.toString());
									}
								} else {
									updateCreatedText = true;
								}
								break;
							case RSS:
								processRss((response = sonetHttpClient.httpResponse(new HttpGet(accountEsid))), widget, account, status_count, links, display_profile, service, time24hr, appWidgetId);
								break;
							case IDENTICA:
								sonetOAuth = new SonetOAuth(IDENTICA_KEY, IDENTICA_SECRET, token, secret);
								// parse the response
								if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(new HttpGet(String.format(IDENTICA_URL_FEED, IDENTICA_BASE_URL, status_count))))) != null) {
									// if not a full_refresh, only update the status_bg and icons
									try {
										statusesArray = new JSONArray(response);
										// if there are updates, clear the cache
										int e2 = statusesArray.length();
										if (e2 > 0) {
											removeOldStatuses(widget, Long.toString(account));
											for (int e = 0; e < e2; e++) {
												links.clear();
												statusObj = statusesArray.getJSONObject(e);
												friendObj = statusObj.getJSONObject(Suser);
												long date = parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT);
												addStatusItem(date,
														friendObj.getString(Sname),
														display_profile ? friendObj.getString(Sprofile_image_url) : null,
																statusObj.getString(Stext),
																service,
																time24hr,
																appWidgetId,
																account,
																statusObj.getString(Sid),
																friendObj.getString(Sid),
																links);
											}
										} else {
											updateCreatedText = true;
										}
									} catch (JSONException e) {
										Log.e(TAG, service + ":" + e.toString());
									}
								} else {
									updateCreatedText = true;
								}
								// notifications
								if (notifications != 0) {
									currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications.SID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);
									// loop over notifications
									if (currentNotifications.moveToFirst()) {
										// store sids, to avoid duplicates when requesting the latest feed
										sid = mSonetCrypto.Decrypt(currentNotifications.getString(0));
										if (!notificationSids.contains(sid)) {
											notificationSids.add(sid);
										}
									}
									currentNotifications.close();
									// limit to oldest status
									String last_sid = null;
									Cursor last_status = getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses.SID}, Statuses.ACCOUNT + "=?", new String[]{Long.toString(account)}, Statuses.CREATED + " ASC LIMIT 1");
									if (last_status.moveToFirst()) {
										last_sid = mSonetCrypto.Decrypt(last_status.getString(0));
									}
									last_status.close();
									// get all mentions since the oldest status for this account
									if ((response = sonetHttpClient.httpResponse(sonetOAuth.getSignedRequest(new HttpGet(String.format(IDENTICA_MENTIONS, IDENTICA_BASE_URL, last_sid != null ? String.format(IDENTICA_SINCE_ID, last_sid) : ""))))) != null) {
										try {
											statusesArray = new JSONArray(response);
											for (int i = 0, i2 = statusesArray.length(); i < i2; i++) {
												statusObj = statusesArray.getJSONObject(i);
												friendObj = statusObj.getJSONObject(Suser);
												if (!friendObj.getString(Sid).equals(accountEsid) && !notificationSids.contains(statusObj.getString(Sid))) {
													friend = friendObj.getString(Sname);
													addNotification(statusObj.getString(Sid), friendObj.getString(Sid), friend, statusObj.getString(Stext), parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT), account, friend + " mentioned you on Identi.ca");
												}
											}
										} catch (JSONException e) {
											Log.e(TAG, service + ":" + e.toString());
										}
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
									if ((response = sonetHttpClient.httpResponse(httpPost)) != null) {
										JSONObject j = new JSONObject(response);
										if (j.has("access_token")) {
											String access_token = j.getString("access_token");
											// notifications
											if (notifications != 0) {
												currentNotifications = getContentResolver().query(Notifications.CONTENT_URI, new String[]{Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);
												// loop over notifications
												if (currentNotifications.moveToFirst()) {
													while (!currentNotifications.isAfterLast()) {
														notificationId = currentNotifications.getLong(0);
														sid = mSonetCrypto.Decrypt(currentNotifications.getString(1));
														updated = currentNotifications.getLong(2);
														cleared = currentNotifications.getInt(3) == 1;
														// store sids, to avoid duplicates when requesting the latest feed
														if (!notificationSids.contains(sid)) {
															notificationSids.add(sid);
														}
														// get comments for current notifications
														if ((response = sonetHttpClient.httpResponse(new HttpGet(String.format(GOOGLEPLUS_ACTIVITY, GOOGLEPLUS_BASE_URL, sid, access_token)))) != null) {
															// check for a newer post, if it's the user's own, then set CLEARED=0
															try {
																JSONObject item = new JSONObject(response);
																if (item.has(Sobject)) {
																	JSONObject object = item.getJSONObject(Sobject);
																	if (object.has(Sreplies)) {
																		int commentCount = 0;
																		JSONObject replies = object.getJSONObject(Sreplies);
																		if (replies.has(StotalItems)) {
																			commentCount = replies.getInt(StotalItems);
																		}
																	}
																}
															} catch (JSONException e) {
																Log.e(TAG, service + ":" + e.toString());
															}
														}
														currentNotifications.moveToNext();
													}
												}
												currentNotifications.close();
											}
											// get new feed
											if ((response = sonetHttpClient.httpResponse(new HttpGet(String.format(GOOGLEPLUS_ACTIVITIES, GOOGLEPLUS_BASE_URL, "me", "public", status_count, access_token)))) != null) {
												JSONObject r = new JSONObject(response);
												if (r.has(Sitems)) {
													statusesArray = r.getJSONArray(Sitems);
													removeOldStatuses(widget, Long.toString(account));
													for (int i1 = 0, i2 = statusesArray.length(); i1 < i2; i1++) {
														statusObj = statusesArray.getJSONObject(i1);
														if (statusObj.has(Sactor) && statusObj.has(Sobject)) {
															friendObj = statusObj.getJSONObject(Sactor);
															JSONObject object = statusObj.getJSONObject(Sobject);
															if (statusObj.has(Sid) && friendObj.has(Sid) && friendObj.has(SdisplayName) && statusObj.has(Spublished) && object.has(Sreplies) && object.has(SoriginalContent)) {
																sid = statusObj.getString(Sid);
																esid = friendObj.getString(Sid);
																friend = friendObj.getString(SdisplayName);
																String originalContent = object.getString(SoriginalContent);
																if ((originalContent == null) || (originalContent.length() == 0)) {
																	originalContent = object.getString(Scontent);
																}
																String photo = null;
																if (display_profile && friendObj.has(Simage)) {
																	JSONObject image = friendObj.getJSONObject(Simage);
																	if (image.has(Surl)) {
																		photo = image.getString(Surl);
																	}
																}
																long date = parseDate(statusObj.getString(Spublished), BUZZ_DATE_FORMAT);
																int commentCount = 0;
																JSONObject replies = object.getJSONObject(Sreplies);
																String notification = null;
																if (replies.has(StotalItems)) {
																	commentCount = replies.getInt(StotalItems);
																}
																if ((notifications != 0) && (notification != null)) {
																	// new notification
																	addNotification(sid, esid, friend, originalContent, date, account, notification);
																}
																if (i1 < status_count) {
																	addStatusItem(date,
																			friend,
																			photo,
																			String.format(getString(R.string.messageWithCommentCount), originalContent, commentCount),
																			service,
																			time24hr,
																			appWidgetId,
																			account,
																			sid,
																			esid,
																			links);
																}
															}
														}
													}
												}
											} else {
												updateCreatedText = true;
											}
										}
									} else {
										updateCreatedText = true;
									}
								} catch (UnsupportedEncodingException e) {
									Log.e(TAG,e.toString());
								} catch (JSONException e) {
									Log.e(TAG,e.toString());
								}
								break;
							case CHATTER:
								// need to get an updated access_token
								String accessResponse = sonetHttpClient.httpResponse(new HttpPost(String.format(CHATTER_URL_ACCESS, CHATTER_KEY, token)));
								if (accessResponse != null) {
									try {
										JSONObject jobj = new JSONObject(accessResponse);
										if (jobj.has(Sinstance_url) && jobj.has(Saccess_token)) {
											httpGet = new HttpGet(String.format(CHATTER_URL_FEED, jobj.getString(Sinstance_url)));
											String chatterToken = jobj.getString(Saccess_token);
											httpGet.setHeader("Authorization", "OAuth " + chatterToken);
											if ((response = sonetHttpClient.httpResponse(httpGet)) != null) {
												try {
													statusesArray = new JSONObject(response).getJSONArray(Sitems);
													// if there are updates, clear the cache
													int e2 = statusesArray.length();
													if (e2 > 0) {
														removeOldStatuses(widget, Long.toString(account));
														for (int e = 0; (e < e2) && (e < status_count); e++) {
															links.clear();
															statusObj = statusesArray.getJSONObject(e);
															friendObj = statusObj.getJSONObject(Suser);
															JSONObject photo = friendObj.getJSONObject(Sphoto);
															JSONObject comments = statusObj.getJSONObject(Scomments);
															long date = parseDate(statusObj.getString(ScreatedDate), CHATTER_DATE_FORMAT);
															if (e < status_count) {
																addStatusItem(date,
																		friendObj.getString(Sname),
																		display_profile ? photo.getString(SsmallPhotoUrl) + "?oauth_token=" + chatterToken : null,
																				String.format(getString(R.string.messageWithCommentCount), statusObj.getJSONObject(Sbody).getString(Stext), comments.getInt(Stotal)),
																				service,
																				time24hr,
																				appWidgetId,
																				account,
																				statusObj.getString(Sid),
																				friendObj.getString(Sid),
																				links);
															}
														}
													} else {
														updateCreatedText = true;
													}
												} catch (JSONException e) {
													Log.e(TAG, service + ":" + e.toString());
													Log.e(TAG, response);
												}
											} else {
												updateCreatedText = true;
											}
										}
									} catch (JSONException e) {
										Log.e(TAG, service + ":" + e.toString());
										Log.e(TAG, accessResponse);
									}
								}
							}
							// remove old notifications
							getContentResolver().delete(Notifications.CONTENT_URI, Notifications.CLEARED + "=1 and " + Notifications.ACCOUNT + "=? and " + Notifications.CREATED + "<?", new String[]{Long.toString(account), Long.toString(System.currentTimeMillis() - 86400000)});
							if (updateCreatedText) {
								if (updateCreatedText(widget, Long.toString(account), time24hr) && (service == MYSPACE)) {
									// warn about myspace permissions
									addStatusItem(0,
											getString(R.string.myspace_permissions_title),
											null,
											getString(R.string.myspace_permissions_message),
											service,
											time24hr,
											appWidgetId,
											account,
											"",
											"",
											links);
								}
							}
						} else {
							// no network connection
							if (hasCache) {
								// update created text
								updateCreatedText(widget, Long.toString(account), time24hr);
							} else {
								// clear the "loading" message and display "no connection"
								getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=?", new String[]{widget});
								addStatusItem(System.currentTimeMillis(), "", null, getString(R.string.no_connection), 0, false, appWidgetId, Sonet.INVALID_ACCOUNT_ID, "-1", "-1", new ArrayList<String[]>());
							}
						}
						// update the bg and icon
						byte[] bg;
						// create the status_bg
						Bitmap bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
						Canvas bg_canvas = new Canvas(bg_bmp);
						bg_canvas.drawColor(status_bg_color);
						ByteArrayOutputStream bg_blob = new ByteArrayOutputStream();
						bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, bg_blob);
						bg = bg_blob.toByteArray();
						ContentValues values = new ContentValues();
						values.put(Statuses.STATUS_BG, bg);
						if (bg_bmp != null) {
							bg_bmp.recycle();
							bg_bmp = null;
						}
						// friend_bg
						bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
						bg_canvas = new Canvas(bg_bmp);
						bg_canvas.drawColor(friend_bg_color);
						bg_blob = new ByteArrayOutputStream();
						bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, bg_blob);
						bg = bg_blob.toByteArray();
						values.put(Statuses.FRIEND_BG, bg);
						if (bg_bmp != null) {
							bg_bmp.recycle();
							bg_bmp = null;
						}
						// profile_bg
						bg_bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
						bg_canvas = new Canvas(bg_bmp);
						bg_canvas.drawColor(profile_bg_color);
						bg_blob = new ByteArrayOutputStream();
						bg_bmp.compress(Bitmap.CompressFormat.PNG, 100, bg_blob);
						bg = bg_blob.toByteArray();
						values.put(Statuses.PROFILE_BG, bg);
						if (bg_bmp != null) {
							bg_bmp.recycle();
							bg_bmp = null;
						}
						values.put(Statuses.ICON, icon ? getBlob(getResources(), map_icons[service]) : null);
						SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, Integer.toString(service), Long.toString(account)});
						accounts.moveToNext();
					}
					if ((notifications != 0) && (mNotify != null)) {
						publishProgress(Integer.toString(notifications));
					}
				}
				// delete the existing loading and informational messages
				getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)});
				// check statuses again
				Cursor statusesCheck = getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID}, Statuses.WIDGET + "=?", new String[]{widget}, null);
				hasCache = statusesCheck.moveToFirst();
				statusesCheck.close();
				if (!hasCache) {
					// there should be a loading message displaying
					// if no updates have been loaded, display "no updates"
					addStatusItem(System.currentTimeMillis(), "", null, getString(R.string.no_updates), 0, false, appWidgetId, Sonet.INVALID_ACCOUNT_ID, "-1", "-1", new ArrayList<String[]>());
				}
			} else {
				// no accounts, clear cache
				getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=?", new String[]{widget});
				// insert no accounts message
				addStatusItem(System.currentTimeMillis(), "", null, getString(R.string.no_accounts), 0, false, appWidgetId, Sonet.INVALID_ACCOUNT_ID, "-1", "-1", new ArrayList<String[]>());
			}
			accounts.close();
			// always update buttons, if !scrollable update widget both times, otherwise build scrollable first, requery second
			// see if the tasks are finished
			// non-scrollable widgets will be completely rebuilt, while scrollable widgets while be notified to requery
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				buildWidgetButtons(appWidgetId, true, 0);
			} else {
				// notify change to About.java
				getContentResolver().notifyChange(Statuses_styles.CONTENT_URI, null);
			}
			return appWidgetId;
		}

		@Override
		protected void onProgressUpdate(String... updates) {
			int notifications = Integer.parseInt(updates[0]);
			if (notifications != 0) {
				Notification notification = new Notification(R.drawable.notification, mNotify, System.currentTimeMillis());
				notification.setLatestEventInfo(getBaseContext(), "New messages", mNotify, PendingIntent.getActivity(SonetService.this, 0, (new Intent(SonetService.this, SonetNotifications.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), 0));
				notification.defaults |= notifications;
				((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
			}
		}

		@Override
		protected void onPostExecute(Integer appWidgetId) {
			// remove widget from list
			if (!mAppWidgetIdsProcessing.isEmpty() && mAppWidgetIdsProcessing.containsKey(appWidgetId)) {
				mAppWidgetIdsProcessing.remove(appWidgetId);
			}
			// remove self from thread list
			if (!mStatusesLoaders.isEmpty() && mStatusesLoaders.containsKey(appWidgetId)) {
				mStatusesLoaders.remove(appWidgetId);
			}
			if (mAppWidgetIdsQueued.isEmpty() && mStatusesLoaders.isEmpty()) {
				Sonet.release();
			} else {
				processUpdates(SonetService.this);
			}
		}

		private void processRss(String response, String widget, long account, int status_count, ArrayList<String[]> links, boolean display_profile, int service, boolean time24hr, int appWidgetId) {
			if (response != null) {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				try {
					DocumentBuilder db = dbf.newDocumentBuilder();
					InputSource is = new InputSource();
					is.setCharacterStream(new StringReader(response));
					Document doc = db.parse(is);
					NodeList nodes = doc.getElementsByTagName(Sitem);
					int i2 = nodes.getLength();
					if (i2 > 0) {
						// check for an image
						String image_url = null;
						NodeList images = doc.getElementsByTagName(Simage);
						int i3 = images.getLength();
						if (i3 > 0) {
							NodeList imageChildren = images.item(0).getChildNodes();
							for (int i = 0; (i < i3) && (image_url == null); i++) {
								Node n = imageChildren.item(i);
								if (n.getNodeName().toLowerCase().equals(Surl)) {
									if (n.hasChildNodes()) {
										image_url = n.getChildNodes().item(0).getNodeValue();
									}
								}
							}
						}
						removeOldStatuses(widget, Long.toString(account));
						int item_count = 0;
						for (int i = 0; (i < i2) && (item_count < status_count); i++) {
							links.clear();
							NodeList children = nodes.item(i).getChildNodes();
							String date = null;
							String title = null;
							String description = null;
							String link = null;
							int values_count = 0;
							for (int child = 0, c2 = children.getLength(); (child < c2) && (values_count < 4); child++) {
								Node n = children.item(child);
								final String nodeName = n.getNodeName().toLowerCase();
								if (nodeName.equals(Spubdate)) {
									values_count++;
									if (n.hasChildNodes()) {
										date = n.getChildNodes().item(0).getNodeValue();
									}
								} else if (nodeName.equals(Stitle)) {
									values_count++;
									if (n.hasChildNodes()) {
										title = n.getChildNodes().item(0).getNodeValue();
									}
								} else if (nodeName.equals(Sdescription)) {
									values_count++;
									if (n.hasChildNodes()) {
										StringBuilder sb = new StringBuilder();
										NodeList descNodes = n.getChildNodes();
										for (int dn = 0, dn2 = descNodes.getLength(); dn < dn2; dn++) {
											Node descNode = descNodes.item(dn);
											if (descNode.getNodeType() == Node.TEXT_NODE) {
												sb.append(descNode.getNodeValue());
											}
										}
										// strip out the html tags
										description = sb.toString().replaceAll("\\<(.|\n)*?>", "");
									}
								} else if (nodeName.equals("link")) {
									values_count++;
									if (n.hasChildNodes()) {
										link = n.getChildNodes().item(0).getNodeValue();
									}
								}
							}
							if (Sonet.HasValues(new String[]{title, description, link, date})) {
								item_count++;
								addStatusItem(parseDate(date, null), title, display_profile ? image_url : null, description, service, time24hr, appWidgetId, account, null, link, links);
							}
						}
					}
				} catch (ParserConfigurationException e) {
					Log.e(TAG, "RSS:" + e.toString());
				} catch (SAXException e) {
					Log.e(TAG, "RSS:" + e.toString());
				} catch (IOException e) {
					Log.e(TAG, "RSS:" + e.toString());
				}
			}
		}

		private void removeOldStatuses(String widgetId, String accountId) {
			Cursor statuses = getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID}, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widgetId, accountId}, null);
			if (statuses.moveToFirst()) {
				while (!statuses.isAfterLast()) {
					getContentResolver().delete(Status_links.CONTENT_URI, Status_links.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
					statuses.moveToNext();
				}
			}
			statuses.close();
			getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widgetId, accountId});
			Cursor entities = getContentResolver().query(Entities.CONTENT_URI, new String[]{Entities._ID}, Entities.ACCOUNT + "=?", new String[]{accountId}, null);
			if (entities.moveToFirst()) {
				while (!entities.isAfterLast()) {
					Cursor s = getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID}, Statuses.ACCOUNT + "=? and " + Statuses.WIDGET + " !=?", new String[]{accountId, widgetId}, null);
					if (!s.moveToFirst()) {
						// not in use, remove it
						getContentResolver().delete(Entities.CONTENT_URI, Entities._ID + "=?", new String[]{Long.toString(entities.getLong(0))});
					}
					s.close();
					entities.moveToNext();
				}
			}
			entities.close();
		}

		private void addStatusItem(long created, String friend, String url, String message, int service, boolean time24hr, int appWidgetId, long accountId, String sid, String esid, ArrayList<String[]> links) {
			long id;
			byte[] profile = null;
			if (url != null) {
				// get profile
				final SonetHttpClient sonetHttpClient = SonetHttpClient.getInstance(getApplicationContext());
				profile = sonetHttpClient.httpBlobResponse(new HttpGet(url));
			}
			if (profile == null) {
				profile = getBlob(getResources(), R.drawable.ic_contact_picture);
			}
			// facebook wall post handling
			String friend_override = null;
			if ((service == FACEBOOK) && friend.indexOf(">") > 0) {
				friend_override = friend;
				friend = friend.substring(0, friend.indexOf(">") - 1);
			}
			Cursor entity = getContentResolver().query(Entities.CONTENT_URI, new String[]{Entities._ID}, Entities.ACCOUNT + "=? and " + Entities.ESID + "=?", new String[]{Long.toString(accountId), mSonetCrypto.Encrypt(esid)}, null);
			if (entity.moveToFirst()) {
				id = entity.getLong(0);
			} else {
				ContentValues entityValues = new ContentValues();
				entityValues.put(Entities.ESID, esid);
				entityValues.put(Entities.FRIEND, friend);
				entityValues.put(Entities.PROFILE, profile);
				entityValues.put(Entities.ACCOUNT, accountId);
				id = Long.parseLong(getContentResolver().insert(Entities.CONTENT_URI, entityValues).getLastPathSegment());
			}
			entity.close();
			// facebook sid comes in as esid_sid, the esid_ may need to be removed
			//		if (serviceId == FACEBOOK) {
			//			int split = sid.indexOf("_");
			//			if ((split > 0) && (split < sid.length())) {
			//				sid = sid.substring(sid.indexOf("_") + 1);
			//			}
			//		}
			// update the account statuses

			// parse any links
			if ((service != TWITTER) && (service != IDENTICA)) {
				Matcher m = Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(message);
				StringBuffer sb = new StringBuffer(message.length());
				while (m.find()) {
					String link = m.group();
					links.add(new String[]{Slink, link});
					m.appendReplacement(sb, "(" + Slink + ": " + Uri.parse(m.group()).getHost() + ")");
				}
				m.appendTail(sb);
				message = sb.toString();
			}
			ContentValues values = new ContentValues();
			values.put(Statuses.CREATED, created);
			values.put(Statuses.ENTITY, id);
			values.put(Statuses.MESSAGE, message);
			values.put(Statuses.SERVICE, service);
			values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
			values.put(Statuses.WIDGET, appWidgetId);
			values.put(Statuses.ACCOUNT, accountId);
			values.put(Statuses.SID, sid);
			values.put(Statuses.FRIEND_OVERRIDE, friend_override);
			long statusId = Long.parseLong(getContentResolver().insert(Statuses.CONTENT_URI, values).getLastPathSegment());
			for (String[] s : links) {
				ContentValues linkValues = new ContentValues();
				linkValues.put(Status_links.STATUS_ID, statusId);
				linkValues.put(Status_links.LINK_TYPE, s[0]);
				linkValues.put(Status_links.LINK_URI, s[1]);
				getContentResolver().insert(Status_links.CONTENT_URI, linkValues);
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
			updateNotify(notification);
		}

		private void updateNotification(long notificationId, long created_time, String accountEsid, String esid, String name, boolean cleared) {
			// new comment
			ContentValues values = new ContentValues();
			values.put(Notifications.UPDATED, created_time);
			if (accountEsid.equals(esid)) {
				// user's own comment, clear the notification
				values.put(Notifications.CLEARED, 1);
			} else if (cleared) {
				values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), name));
				values.put(Notifications.CLEARED, 0);
				updateNotify(String.format(getString(R.string.friendcommented), name));
			} else {
				values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), name + " and others"));
				updateNotify(String.format(getString(R.string.friendcommented), name + " and others"));
			}
			getContentResolver().update(Notifications.CONTENT_URI, values, Notifications._ID + "=?", new String[]{Long.toString(notificationId)});
		}

		private void updateNotify(String notification) {
			if (mNotify == null) {
				mNotify = notification;
			} else {
				mNotify = "multiple updates";
			}
		}

		private boolean updateCreatedText(String widget, String account, boolean time24hr) {
			boolean statuses_updated = false;
			Cursor statuses = SonetService.this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.CREATED}, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widget, account}, null);
			if (statuses.moveToFirst()) {
				while (!statuses.isAfterLast()) {
					ContentValues values = new ContentValues();
					values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(1), time24hr));
					SonetService.this.getContentResolver().update(Statuses.CONTENT_URI, values, Statuses._ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
					statuses.moveToNext();
				}
				statuses_updated = true;
			}
			statuses.close();
			return statuses_updated;
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
	}

	private void buildWidgetButtons(Integer appWidgetId, boolean updatesReady, int page) {
		boolean hasbuttons = false;
		int scrollable = 0;
		int buttons_bg_color = Sonet.default_buttons_bg_color,
		buttons_color = Sonet.default_buttons_color,
		buttons_textsize = Sonet.default_buttons_textsize;
		boolean display_profile = true;
		int margin = Sonet.default_margin;
		final String widget = Integer.toString(appWidgetId);
		Cursor settings = getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.HASBUTTONS, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_TEXTSIZE, Widgets.SCROLLABLE, Widgets.DISPLAY_PROFILE, Widgets.MARGIN}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
		if (!settings.moveToFirst()) {
			settings.close();
			settings = getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets.HASBUTTONS, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_TEXTSIZE, Widgets.SCROLLABLE, Widgets.DISPLAY_PROFILE, Widgets.MARGIN}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
			if (!settings.moveToFirst()) {
				// initialize account settings
				ContentValues values = new ContentValues();
				values.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
				values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
				getContentResolver().insert(Widgets.CONTENT_URI, values);
			}
			// don't insert a duplicate row
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				// initialize account settings
				ContentValues values = new ContentValues();
				values.put(Widgets.WIDGET, appWidgetId);
				values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
				getContentResolver().insert(Widgets.CONTENT_URI, values);
			}
		}
		if (settings.moveToFirst()) {
			hasbuttons = settings.getInt(0) == 1;
			buttons_color = settings.getInt(1);
			buttons_bg_color = settings.getInt(2);
			buttons_textsize = settings.getInt(3);
			scrollable = settings.getInt(4);
			display_profile = settings.getInt(5) == 1;
			margin = settings.getInt(6);
		}
		settings.close();
		// Push update for this widget to the home screen
		int layout;
		if (hasbuttons) {
			if (sNativeScrollingSupported) {
				if (margin > 0) {
					layout = R.layout.widget_margin_scrollable;
				} else {
					layout = R.layout.widget_scrollable;
				}
			} else if (display_profile) {
				if (margin > 0) {
					layout = R.layout.widget_margin;
				} else {
					layout = R.layout.widget;
				}
			} else {
				if (margin > 0) {
					layout = R.layout.widget_noprofile_margin;
				} else {
					layout = R.layout.widget_noprofile;
				}
			}
		} else {
			if (sNativeScrollingSupported) {
				if (margin > 0) {
					layout = R.layout.widget_nobuttons_margin_scrollable;
				} else {
					layout = R.layout.widget_nobuttons_scrollable;
				}
			} else if (display_profile) {
				if (margin > 0) {
					layout = R.layout.widget_nobuttons_margin;
				} else {
					layout = R.layout.widget_nobuttons;
				}
			} else {
				if (margin > 0) {
					layout = R.layout.widget_nobuttons_noprofile_margin;
				} else {
					layout = R.layout.widget_nobuttons_noprofile;
				}
			}
		}
		// wrap RemoteViews for backward compatibility
		RemoteViews views = new RemoteViews(getPackageName(), layout);
		if (hasbuttons) {
			Bitmap buttons_bg = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
			Canvas buttons_bg_canvas = new Canvas(buttons_bg);
			buttons_bg_canvas.drawColor(buttons_bg_color);
			views.setImageViewBitmap(R.id.buttons_bg, buttons_bg);
			views.setTextColor(R.id.buttons_bg_clear, buttons_bg_color);
			views.setFloat(R.id.buttons_bg_clear, "setTextSize", buttons_textsize);
			views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(SonetService.this, 0, new Intent(SonetService.this, SonetCreatePost.class).setAction(LauncherIntent.Action.ACTION_VIEW_CLICK).setData(Uri.withAppendedPath(Widgets.CONTENT_URI, widget)), 0));
			views.setTextColor(R.id.button_post, buttons_color);
			views.setFloat(R.id.button_post, "setTextSize", buttons_textsize);
			views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(SonetService.this, 0, new Intent(SonetService.this, ManageAccounts.class).setAction(widget), 0));
			views.setTextColor(R.id.button_configure, buttons_color);
			views.setFloat(R.id.button_configure, "setTextSize", buttons_textsize);
			views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(SonetService.this, 0, new Intent(SonetService.this, SonetService.class).setAction(widget), 0));
			views.setTextColor(R.id.button_refresh, buttons_color);
			views.setFloat(R.id.button_refresh, "setTextSize", buttons_textsize);
			views.setTextColor(R.id.page_up, buttons_color);
			views.setFloat(R.id.page_up, "setTextSize", buttons_textsize);
			views.setTextColor(R.id.page_down, buttons_color);
			views.setFloat(R.id.page_down, "setTextSize", buttons_textsize);
		}
		// set margin
		if (scrollable == 0) {
			final AppWidgetManager mgr = AppWidgetManager.getInstance(SonetService.this);
			// check if native scrolling is supported
			if (sNativeScrollingSupported) {
				// native scrolling

				try {
					final Intent intent = SonetRemoteViewsServiceWrapper.getRemoteAdapterIntent(SonetService.this);
					if (intent != null) {
						intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
						intent.putExtra(Widgets.DISPLAY_PROFILE, display_profile);
						intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
						sSetRemoteAdapter.invoke(views, appWidgetId, R.id.messages, intent);
						// empty
						sSetEmptyView.invoke(views, R.id.messages, R.id.empty_messages);
						// onclick
						// Bind a click listener template for the contents of the message list
						final Intent onClickIntent = new Intent(SonetService.this, SonetWidget.class);
						onClickIntent.setAction(ACTION_ON_CLICK);
						onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
						onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
						final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(SonetService.this, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
						sSetPendingIntentTemplate.invoke(views, R.id.messages, onClickPendingIntent);
					} else {
						// fallback on non-scrolling widget
						sNativeScrollingSupported = false;
					}
				} catch (NumberFormatException e) {
					Log.e(TAG, e.toString());
				} catch (IllegalArgumentException e) {
					Log.e(TAG, e.toString());
				} catch (IllegalAccessException e) {
					Log.e(TAG, e.toString());
				} catch (InvocationTargetException e) {
					Log.e(TAG, e.toString());
				}

				mgr.updateAppWidget(appWidgetId, views);

				try {
					// trigger query
					sNotifyAppWidgetViewDataChanged.invoke(mgr, appWidgetId, R.id.messages);
				} catch (NumberFormatException e) {
					Log.e(TAG, e.toString());
				} catch (IllegalArgumentException e) {
					Log.e(TAG, e.toString());
				} catch (IllegalAccessException e) {
					Log.e(TAG, e.toString());
				} catch (InvocationTargetException e) {
					Log.e(TAG, e.toString());
				}
			}
			if (!sNativeScrollingSupported) {
				int[] map_message = {R.id.message0, R.id.message1, R.id.message2, R.id.message3, R.id.message4, R.id.message5, R.id.message6, R.id.message7, R.id.message8, R.id.message9, R.id.message10, R.id.message11, R.id.message12, R.id.message13, R.id.message14, R.id.message15},
				map_item = {R.id.item0, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6, R.id.item7, R.id.item8, R.id.item9, R.id.item10, R.id.item11, R.id.item12, R.id.item13, R.id.item14, R.id.item15},
				map_profile = {R.id.profile0, R.id.profile1, R.id.profile2, R.id.profile3, R.id.profile4, R.id.profile5, R.id.profile6, R.id.profile7, R.id.profile8, R.id.profile9, R.id.profile10, R.id.profile11, R.id.profile12, R.id.profile13, R.id.profile14, R.id.profile15},
				map_screenname = {R.id.friend0, R.id.friend1, R.id.friend2, R.id.friend3, R.id.friend4, R.id.friend5, R.id.friend6, R.id.friend7, R.id.friend8, R.id.friend9, R.id.friend10, R.id.friend11, R.id.friend12, R.id.friend13, R.id.friend14, R.id.friend15},
				map_created = {R.id.created0, R.id.created1, R.id.created2, R.id.created3, R.id.created4, R.id.created5, R.id.created6, R.id.created7, R.id.created8, R.id.created9, R.id.created10, R.id.created11, R.id.created12, R.id.created13, R.id.created14, R.id.created15},
				map_status_bg = {R.id.status_bg0, R.id.status_bg1, R.id.status_bg2, R.id.status_bg3, R.id.status_bg4, R.id.status_bg5, R.id.status_bg6, R.id.status_bg7, R.id.status_bg8, R.id.status_bg9, R.id.status_bg10, R.id.status_bg11, R.id.status_bg12, R.id.status_bg13, R.id.status_bg14, R.id.status_bg15},
				map_friend_bg_clear = {R.id.friend_bg_clear0, R.id.friend_bg_clear1, R.id.friend_bg_clear2, R.id.friend_bg_clear3, R.id.friend_bg_clear4, R.id.friend_bg_clear5, R.id.friend_bg_clear6, R.id.friend_bg_clear7, R.id.friend_bg_clear8, R.id.friend_bg_clear9, R.id.friend_bg_clear10, R.id.friend_bg_clear11, R.id.friend_bg_clear12, R.id.friend_bg_clear13, R.id.friend_bg_clear14, R.id.friend_bg_clear15},
				map_message_bg_clear = {R.id.message_bg_clear0, R.id.message_bg_clear1, R.id.message_bg_clear2, R.id.message_bg_clear3, R.id.message_bg_clear4, R.id.message_bg_clear5, R.id.message_bg_clear6, R.id.message_bg_clear7, R.id.message_bg_clear8, R.id.message_bg_clear9, R.id.message_bg_clear10, R.id.message_bg_clear11, R.id.message_bg_clear12, R.id.message_bg_clear13, R.id.message_bg_clear14, R.id.message_bg_clear15},
				map_icon = {R.id.icon0, R.id.icon1, R.id.icon2, R.id.icon3, R.id.icon4, R.id.icon5, R.id.icon6, R.id.icon7, R.id.icon8, R.id.icon9, R.id.icon10, R.id.icon11, R.id.icon12, R.id.icon13, R.id.icon14, R.id.icon15},
				map_profile_bg = {R.id.profile_bg0, R.id.profile_bg1, R.id.profile_bg2, R.id.profile_bg3, R.id.profile_bg4, R.id.profile_bg5, R.id.profile_bg6, R.id.profile_bg7, R.id.profile_bg8, R.id.profile_bg9, R.id.profile_bg10, R.id.profile_bg11, R.id.profile_bg12, R.id.profile_bg13, R.id.profile_bg14, R.id.profile_bg15},
				map_friend_bg = {R.id.friend_bg0, R.id.friend_bg1, R.id.friend_bg2, R.id.friend_bg3, R.id.friend_bg4, R.id.friend_bg5, R.id.friend_bg6, R.id.friend_bg7, R.id.friend_bg8, R.id.friend_bg9, R.id.friend_bg10, R.id.friend_bg11, R.id.friend_bg12, R.id.friend_bg13, R.id.friend_bg14, R.id.friend_bg15};
				Cursor statuses_styles = getContentResolver().query(Uri.withAppendedPath(Statuses_styles.CONTENT_URI, widget), new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON, Statuses_styles.PROFILE_BG, Statuses_styles.FRIEND_BG}, null, null, Statuses_styles.CREATED + " DESC LIMIT " + page + ",-1");
				if (statuses_styles.moveToFirst()) {
					int count_status = 0;
					while (!statuses_styles.isAfterLast() && (count_status < map_item.length)) {
						int friend_color = statuses_styles.getInt(6),
						created_color = statuses_styles.getInt(7),
						friend_textsize = statuses_styles.getInt(9),
						created_textsize = statuses_styles.getInt(10),
						messages_color = statuses_styles.getInt(5),
						messages_textsize = statuses_styles.getInt(8);
						// set icons
						byte[] icon = statuses_styles.getBlob(12);
						if (icon != null) {
							Bitmap iconbmp = BitmapFactory.decodeByteArray(icon, 0, icon.length, sBFOptions);
							if (iconbmp != null) {
								views.setImageViewBitmap(map_icon[count_status], iconbmp);
							}
						}
						views.setTextViewText(map_friend_bg_clear[count_status], statuses_styles.getString(1));
						views.setFloat(map_friend_bg_clear[count_status], "setTextSize", friend_textsize);
						views.setTextViewText(map_message_bg_clear[count_status], statuses_styles.getString(3));
						views.setFloat(map_message_bg_clear[count_status], "setTextSize", messages_textsize);
						if (display_profile) {
							// set profiles background
							byte[] profile_bg = statuses_styles.getBlob(13);
							if (profile_bg != null) {
								Bitmap profile_bgbmp = BitmapFactory.decodeByteArray(profile_bg, 0, profile_bg.length, sBFOptions);
								if (profile_bgbmp != null) {
									views.setImageViewBitmap(map_profile_bg[count_status], profile_bgbmp);
								}
							}
						}
						// set friends background
						byte[] friend_bg = statuses_styles.getBlob(14);
						if (friend_bg != null) {
							Bitmap friend_bgbmp = BitmapFactory.decodeByteArray(friend_bg, 0, friend_bg.length, sBFOptions);
							if (friend_bgbmp != null) {
								views.setImageViewBitmap(map_friend_bg[count_status], friend_bgbmp);
							}
						}
						// set messages background
						byte[] status_bg = statuses_styles.getBlob(11);
						if (status_bg != null) {
							Bitmap status_bgbmp = BitmapFactory.decodeByteArray(status_bg, 0, status_bg.length, sBFOptions);
							if (status_bgbmp != null) {
								views.setImageViewBitmap(map_status_bg[count_status], status_bgbmp);
							}
						}
						views.setTextViewText(map_message[count_status], statuses_styles.getString(3));
						views.setTextColor(map_message[count_status], messages_color);
						views.setFloat(map_message[count_status], "setTextSize", messages_textsize);
						views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(SonetService.this, 0, new Intent(SonetService.this, StatusDialog.class).setData(Uri.withAppendedPath(Statuses_styles.CONTENT_URI, Long.toString(statuses_styles.getLong(0)))), 0));
						views.setTextViewText(map_screenname[count_status], statuses_styles.getString(1));
						views.setTextColor(map_screenname[count_status], friend_color);
						views.setFloat(map_screenname[count_status], "setTextSize", friend_textsize);
						views.setTextViewText(map_created[count_status], statuses_styles.getString(4));
						views.setTextColor(map_created[count_status], created_color);
						views.setFloat(map_created[count_status], "setTextSize", created_textsize);
						if (display_profile) {
							byte[] profile = statuses_styles.getBlob(2);
							if (profile != null) {
								Bitmap profilebmp = BitmapFactory.decodeByteArray(profile, 0, profile.length, sBFOptions);
								if (profilebmp != null) {
									views.setImageViewBitmap(map_profile[count_status], profilebmp);
								}
							}
						}
						count_status++;
						statuses_styles.moveToNext();
					}
					if (hasbuttons && (page < statuses_styles.getCount())) {
						// there are more statuses to show, allow paging down
						views.setOnClickPendingIntent(R.id.page_down, PendingIntent.getService(SonetService.this, 0, new Intent(SonetService.this, SonetService.class).setAction(ACTION_PAGE_DOWN).setData(Uri.withAppendedPath(Widgets.CONTENT_URI, widget)).putExtra(ACTION_PAGE_DOWN, page + 1), PendingIntent.FLAG_UPDATE_CURRENT));
					}
				}
				statuses_styles.close();
				if (hasbuttons && (page > 0)) {
					views.setOnClickPendingIntent(R.id.page_up, PendingIntent.getService(SonetService.this, 0, new Intent(SonetService.this, SonetService.class).setAction(ACTION_PAGE_UP).setData(Uri.withAppendedPath(Widgets.CONTENT_URI, widget)).putExtra(ACTION_PAGE_UP, page - 1), PendingIntent.FLAG_UPDATE_CURRENT));
				}
				mgr.updateAppWidget(appWidgetId, views);
			}
		} else if (updatesReady) {
			getContentResolver().notifyChange(Statuses_styles.CONTENT_URI, null);
		} else {
			AppWidgetManager.getInstance(SonetService.this).updateAppWidget(Integer.parseInt(widget), views);
			buildScrollableWidget(appWidgetId, scrollable, display_profile);
		}
	}

	private void buildScrollableWidget(Integer appWidgetId, int scrollableVersion, boolean display_profile) {
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
		String[] projection = display_profile ? new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON, Statuses_styles.PROFILE_BG, Statuses_styles.FRIEND_BG}
		: new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON, Statuses_styles.FRIEND_BG};
		String sortOrder = Statuses_styles.CREATED + " DESC";
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, projection);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, sortOrder);
		String whereClause = Statuses_styles.WIDGET + "=?";
		String[] selectionArgs = new String[]{Integer.toString(appWidgetId)};
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_ACTION_VIEW_URI_INDEX, SonetProvider.StatusesStylesColumns._id.ordinal());

		switch (scrollableVersion) {
		case 1:
			if (display_profile) {
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.widget_item);
				int[] cursorIndices = new int[10];
				int[] viewTypes = new int[10];
				int[] layoutIds = new int[10];
				int[] defaultResource = new int[10];
				boolean[] clickable = new boolean[10];
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
				// R.id.profile_bg
				cursorIndices[8] = SonetProvider.StatusesStylesColumns.profile_bg.ordinal();
				viewTypes[8] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
				layoutIds[8] = R.id.profile_bg;
				defaultResource[8] = 0;
				clickable[8] = false;
				// R.id.friende_bg
				cursorIndices[9] = SonetProvider.StatusesStylesColumns.friend_bg.ordinal();
				viewTypes[9] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
				layoutIds[9] = R.id.friend_bg;
				defaultResource[9] = 0;
				clickable[9] = false;
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_DEFAULT_RESOURCES, defaultResource);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);			
			} else {
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.widget_item_noprofile);
				int[] cursorIndices = new int[8];
				int[] viewTypes = new int[8];
				int[] layoutIds = new int[8];
				int[] defaultResource = new int[8];
				boolean[] clickable = new boolean[8];
				// R.id.friend_bg_clear
				cursorIndices[0] = SonetProvider.StatusesStylesColumnsNoProfile.friend.ordinal();
				viewTypes[0] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
				layoutIds[0] = R.id.friend_bg_clear;
				defaultResource[0] = 0;
				clickable[0] = false;
				// R.id.message_bg_clear
				cursorIndices[1] = SonetProvider.StatusesStylesColumnsNoProfile.message.ordinal();
				viewTypes[1] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
				layoutIds[1] = R.id.message_bg_clear;
				defaultResource[1] = 0;
				clickable[1] = false;
				// R.id.status_bg
				cursorIndices[2] = SonetProvider.StatusesStylesColumnsNoProfile.status_bg.ordinal();
				viewTypes[2] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
				layoutIds[2] = R.id.status_bg;
				defaultResource[2] = 0;
				clickable[2] = true;
				// R.id.friend
				cursorIndices[3] = SonetProvider.StatusesStylesColumnsNoProfile.friend.ordinal();
				viewTypes[3] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
				layoutIds[3] = R.id.friend;
				defaultResource[3] = 0;
				clickable[3] = false;
				// R.id.created
				cursorIndices[4] = SonetProvider.StatusesStylesColumnsNoProfile.createdtext.ordinal();
				viewTypes[4] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
				layoutIds[4] = R.id.created;
				defaultResource[4] = 0;
				clickable[4] = false;
				// R.id.message
				cursorIndices[5] = SonetProvider.StatusesStylesColumnsNoProfile.message.ordinal();
				viewTypes[5] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
				layoutIds[5] = R.id.message;
				defaultResource[5] = 0;
				clickable[5] = false;
				// R.id.icon
				cursorIndices[6] = SonetProvider.StatusesStylesColumnsNoProfile.icon.ordinal();
				viewTypes[6] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
				layoutIds[6] = R.id.icon;
				defaultResource[6] = 0;
				clickable[6] = false;
				// R.id.friend_bg
				cursorIndices[7] = SonetProvider.StatusesStylesColumnsNoProfile.friend_bg.ordinal();
				viewTypes[7] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
				layoutIds[7] = R.id.friend_bg;
				defaultResource[7] = 0;
				clickable[7] = false;
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_DEFAULT_RESOURCES, defaultResource);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
			}
			break;
		case 2:
			if (display_profile) {
				BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.widget_item);

				Intent i = new Intent(SonetService.this, SonetWidget.class)
				.setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
				.setData(uri)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				PendingIntent pi = PendingIntent.getBroadcast(SonetService.this, 0, i, 0);

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
				
				itemViews.setBoundBitmap(R.id.profile_bg, "setImageBitmap", SonetProvider.StatusesStylesColumns.profile_bg.ordinal(), 0);

				itemViews.setBoundBitmap(R.id.friend_bg, "setImageBitmap", SonetProvider.StatusesStylesColumns.friend_bg.ordinal(), 0);

				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);
			} else {
				BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.widget_item_noprofile);

				Intent i = new Intent(SonetService.this, SonetWidget.class)
				.setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
				.setData(uri)
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				PendingIntent pi = PendingIntent.getBroadcast(SonetService.this, 0, i, 0);

				itemViews.SetBoundOnClickIntent(R.id.item, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.StatusesStylesColumnsNoProfile._id.ordinal());

				itemViews.setBoundCharSequence(R.id.friend_bg_clear, "setText", SonetProvider.StatusesStylesColumnsNoProfile.friend.ordinal(), 0);
				itemViews.setBoundFloat(R.id.friend_bg_clear, "setTextSize", SonetProvider.StatusesStylesColumnsNoProfile.friend_textsize.ordinal());

				itemViews.setBoundCharSequence(R.id.message_bg_clear, "setText", SonetProvider.StatusesStylesColumnsNoProfile.message.ordinal(), 0);
				itemViews.setBoundFloat(R.id.message_bg_clear, "setTextSize", SonetProvider.StatusesStylesColumnsNoProfile.messages_textsize.ordinal());

				itemViews.setBoundBitmap(R.id.status_bg, "setImageBitmap", SonetProvider.StatusesStylesColumnsNoProfile.status_bg.ordinal(), 0);

				itemViews.setBoundCharSequence(R.id.friend, "setText", SonetProvider.StatusesStylesColumnsNoProfile.friend.ordinal(), 0);
				itemViews.setBoundCharSequence(R.id.created, "setText", SonetProvider.StatusesStylesColumnsNoProfile.createdtext.ordinal(), 0);
				itemViews.setBoundCharSequence(R.id.message, "setText", SonetProvider.StatusesStylesColumnsNoProfile.message.ordinal(), 0);

				itemViews.setBoundInt(R.id.friend, "setTextColor", SonetProvider.StatusesStylesColumnsNoProfile.friend_color.ordinal());
				itemViews.setBoundInt(R.id.created, "setTextColor", SonetProvider.StatusesStylesColumnsNoProfile.created_color.ordinal());
				itemViews.setBoundInt(R.id.message, "setTextColor", SonetProvider.StatusesStylesColumnsNoProfile.messages_color.ordinal());

				itemViews.setBoundFloat(R.id.friend, "setTextSize", SonetProvider.StatusesStylesColumnsNoProfile.friend_textsize.ordinal());
				itemViews.setBoundFloat(R.id.created, "setTextSize", SonetProvider.StatusesStylesColumnsNoProfile.created_textsize.ordinal());
				itemViews.setBoundFloat(R.id.message, "setTextSize", SonetProvider.StatusesStylesColumnsNoProfile.messages_textsize.ordinal());

				itemViews.setBoundBitmap(R.id.icon, "setImageBitmap", SonetProvider.StatusesStylesColumnsNoProfile.icon.ordinal(), 0);

				itemViews.setBoundBitmap(R.id.friend_bg, "setImageBitmap", SonetProvider.StatusesStylesColumns.friend_bg.ordinal(), 0);

				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);
			}
			break;
		}
		sendBroadcast(replaceDummy);
	}
}
