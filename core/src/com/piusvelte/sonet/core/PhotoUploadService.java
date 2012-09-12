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
package com.piusvelte.sonet.core;

import static com.piusvelte.sonet.core.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_PHOTOS;
import static com.piusvelte.sonet.core.Sonet.NOTIFY_ID;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.Sonet.Smessage;
import static com.piusvelte.sonet.core.Sonet.Splace;
import static com.piusvelte.sonet.core.Sonet.Ssource;
import static com.piusvelte.sonet.core.Sonet.Stags;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.Sonet.Statuses;
import com.piusvelte.sonet.core.Sonet.Widgets;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

public class PhotoUploadService extends Service {
	private static final String TAG = "PhotoUploadService";
	private SonetCrypto mSonetCrypto;

	@Override
	public void onCreate() {
		super.onCreate();
		mSonetCrypto = SonetCrypto.getInstance(getApplicationContext());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, startId);
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent != null) {
			String action = intent.getAction();
			if (Sonet.ACTION_UPLOAD.equals(action)) {
				if (intent.hasExtra(Accounts.TOKEN) && intent.hasExtra(Statuses.MESSAGE) && intent.hasExtra(Widgets.INSTANT_UPLOAD)) {
					String place = null;
					if (intent.hasExtra(Splace))
						place = intent.getStringExtra(Splace);
					String tags = null;
					if (intent.hasExtra(Stags))
						tags = intent.getStringExtra(Stags);
					// upload a photo
					Notification notification = new Notification(R.drawable.notification, "uploading photo", System.currentTimeMillis());
					notification.setLatestEventInfo(getBaseContext(), "photo upload", "uploading", PendingIntent.getActivity(PhotoUploadService.this, 0, (Sonet.getPackageIntent(PhotoUploadService.this, About.class)), 0));
					((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
					(new AsyncTask<String, Void, String>() {

						@Override
						protected String doInBackground(String... params) {
							String response = null;
							if (params.length > 2) {
								Log.d(TAG, "upload file: " + params[2]);
								HttpPost httpPost = new HttpPost(String.format(FACEBOOK_PHOTOS, FACEBOOK_BASE_URL, Saccess_token, mSonetCrypto.Decrypt(params[0])));
								MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
								File file = new File(params[2]);
								ContentBody fileBody = new FileBody(file);
								entity.addPart(Ssource, fileBody);
								HttpClient httpClient = SonetHttpClient.getThreadSafeClient(getApplicationContext());
								try {
									entity.addPart(Smessage, new StringBody(params[1]));
									if (params[3] != null)
										entity.addPart(Splace, new StringBody(params[3]));
									if (params[4] != null)
										entity.addPart(Stags, new StringBody(params[4]));
									httpPost.setEntity(entity);
									response = SonetHttpClient.httpResponse(httpClient, httpPost);
								} catch (UnsupportedEncodingException e) {
									Log.e(TAG,e.toString());
								}
							}
							return response;
						}

						@Override
						protected void onPostExecute(String response) {
							// notify photo success
							String message = getString(response != null ? R.string.success : R.string.failure);
							Log.d(TAG,"upload finished:" + message);
							Notification notification = new Notification(R.drawable.notification, "photo upload " + message, System.currentTimeMillis());
							notification.setLatestEventInfo(getBaseContext(), "photo upload", message, PendingIntent.getActivity(PhotoUploadService.this, 0, (Sonet.getPackageIntent(PhotoUploadService.this, About.class)), 0));
							((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
						}

					}).execute(intent.getStringExtra(Accounts.TOKEN), intent.getStringExtra(Statuses.MESSAGE), intent.getStringExtra(Widgets.INSTANT_UPLOAD), place, tags);
				}
			}
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
