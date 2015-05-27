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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.social.Facebook;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.File;

import static com.piusvelte.sonet.Sonet.NOTIFY_ID;
import static com.piusvelte.sonet.Sonet.Saccess_token;
import static com.piusvelte.sonet.Sonet.Smessage;
import static com.piusvelte.sonet.Sonet.Splace;
import static com.piusvelte.sonet.Sonet.Ssource;
import static com.piusvelte.sonet.Sonet.Stags;
import static com.piusvelte.sonet.social.Facebook.FACEBOOK_BASE_URL;

public class PhotoUploadService extends Service {
    private static final String TAG = "PhotoUploadService";
    private int mStartId = Sonet.INVALID_SERVICE;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        start(intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        mStartId = startId;
        start(intent);
    }

    private void start(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (Sonet.ACTION_UPLOAD.equals(action)) {
                if (intent.hasExtra(Accounts.TOKEN) && intent.hasExtra(Statuses.MESSAGE) && intent.hasExtra(Widgets.INSTANT_UPLOAD)) {
                    String place = null;

                    if (intent.hasExtra(Splace)) {
                        place = intent.getStringExtra(Splace);
                    }

                    String tags = null;

                    if (intent.hasExtra(Stags)) {
                        tags = intent.getStringExtra(Stags);
                    }

                    // upload a photo
                    Notification notification = new Notification(R.drawable.notification, "uploading photo", System.currentTimeMillis());
                    notification.setLatestEventInfo(getBaseContext(), "photo upload", "uploading",
                            PendingIntent.getActivity(PhotoUploadService.this, 0, new Intent(PhotoUploadService.this, About.class), 0));
                    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
                    (new AsyncTask<String, Void, String>() {

                        @Override
                        protected String doInBackground(String... params) {
                            if (params.length > 2) {
                                String filename = params[2];
                                String imageType = "jpg";

                                if (filename.endsWith("png")) {
                                    imageType = "png";
                                } else if (filename.endsWith("bmp")) {
                                    imageType = "bmp";
                                }

                                if (BuildConfig.DEBUG) Log.d(TAG, "upload file: " + filename + ", type: " + imageType);

                                MultipartBuilder builder = new MultipartBuilder()
                                        .type(MultipartBuilder.FORM)
                                        .addFormDataPart(Ssource,
                                                filename,
                                                RequestBody.create(MediaType.parse("image/" + imageType), new File(filename)))
                                        .addFormDataPart(Smessage, params[1]);

                                if (params.length > 3) {
                                    if (!TextUtils.isEmpty(params[3])) {
                                        builder.addFormDataPart(Splace, params[3]);
                                    }

                                    if (params.length > 4) {
                                        if (!TextUtils.isEmpty(params[4])) {
                                            builder.addFormDataPart(Stags, params[4]);
                                        }
                                    }
                                }

                                Request request = new Request.Builder()
                                        .url(String.format(Facebook.FACEBOOK_PHOTOS, FACEBOOK_BASE_URL, Saccess_token, params[0]))
                                        .post(builder.build())
                                        .build();

                                return SonetHttpClient.getResponse(request);
                            }

                            return null;
                        }

                        @Override
                        protected void onPostExecute(String response) {
                            // notify photo success
                            String message = getString(response != null ? R.string.success : R.string.failure);
                            Notification notification = new Notification(R.drawable.notification, "photo upload " + message,
                                    System.currentTimeMillis());
                            notification.setLatestEventInfo(getBaseContext(), "photo upload", message, PendingIntent
                                    .getActivity(PhotoUploadService.this, 0, About.createIntent(PhotoUploadService.this), 0));
                            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
                            stopSelfResult(mStartId);
                        }
                    }).execute(intent.getStringExtra(Accounts.TOKEN), intent.getStringExtra(Statuses.MESSAGE),
                            intent.getStringExtra(Widgets.INSTANT_UPLOAD), place, tags);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
