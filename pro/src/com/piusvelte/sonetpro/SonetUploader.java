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
package com.piusvelte.sonetpro;

import com.piusvelte.sonetpro.Sonet.Widgets;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

public class SonetUploader extends Service {
	private static final String TAG = "SonetUploader";
	private ContentObserver mInstantUpload = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		// check for any instant upload settings
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
							Log.d(TAG,"media changed");
							(new AsyncTask<Void, Void, String>() {

								@Override
								protected String doInBackground(Void... arg0) {
									String filepath = null;
									// limit to those from the past 10 seconds
									Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaColumns.DATA}, MediaColumns.DATE_ADDED + ">?", new String[]{Long.toString(System.currentTimeMillis() / 1000 - 10)}, MediaColumns.DATE_ADDED + " DESC");
									if (c.moveToFirst()) {
										filepath = c.getString(0);
										Log.d(TAG,"filepath:"+filepath);
									}
									c.close();
									return filepath;
								}

								@Override
								protected void onPostExecute(String filepath) {
									// launch post activity with filepath
									if (filepath != null) {
										startActivity(new Intent(getApplicationContext(), StatusDialog.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(Widgets.INSTANT_UPLOAD, filepath));
									}
								}

							}).execute();
						}

					};
					getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, mInstantUpload);
				} else if (!upload && (mInstantUpload != null)) {
					Log.d(TAG,"no instant upload");
					getContentResolver().unregisterContentObserver(mInstantUpload);
					mInstantUpload = null;
					stopSelf();
				}
			}

		}).execute();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mInstantUpload != null) {
			getContentResolver().unregisterContentObserver(mInstantUpload);
		}
	}

}
