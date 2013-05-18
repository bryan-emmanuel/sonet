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
package com.piusvelte.sonet.core.task;

import static com.piusvelte.sonet.core.Sonet.IDENTICA;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN;
import static com.piusvelte.sonet.core.Sonet.TWITTER;

import java.util.HashMap;

import org.apache.http.client.HttpClient;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetCrypto;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetProvider;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.Sonet.Entities;
import com.piusvelte.sonet.core.Sonet.Notifications;
import com.piusvelte.sonet.core.Sonet.Statuses;
import com.piusvelte.sonet.core.Sonet.Statuses_styles;
import com.piusvelte.sonet.core.Sonet.Widgets;
import com.piusvelte.sonet.core.Sonet.Widgets_settings;
import com.piusvelte.sonet.core.activity.SonetComments;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class LoadCommentsTask extends CommentsCommonTask {

	public static final int ID = 0;
	public static final int NAME = 1;
	public static final int MESSAGE = 2;
	public static final int CREATED = 3;
	public static final int ACTION = 4;

	public LoadCommentsTask(SonetComments activity, Uri data) {
		super(activity, data);
	}

	public void comment(String entity, String id, String message) {
		super.execute(entity, id, message);
	}

	@Override
	protected String doInBackground(String... params) {
		super.doInBackground(params);
		UriMatcher um = new UriMatcher(UriMatcher.NO_MATCH);
		String authority = Sonet.getAuthority(activity);
		um.addURI(authority, SonetProvider.VIEW_STATUSES_STYLES + "/*", SonetProvider.STATUSES_STYLES);
		um.addURI(authority, SonetProvider.TABLE_NOTIFICATIONS + "/*", SonetProvider.NOTIFICATIONS);
		Cursor status;
		switch (um.match(data)) {
		case SonetProvider.STATUSES_STYLES:
			status = activity.getContentResolver().query(Statuses_styles.getContentUri(activity), new String[]{Statuses_styles.FRIEND, Statuses_styles.MESSAGE, Statuses_styles.CREATED}, Statuses_styles._ID + "=?", new String[]{data.getLastPathSegment()}, null);
			if (status.moveToFirst()) {
				addComment(statusId,
						status.getString(0),
						status.getString(1),
						Sonet.getCreatedText(status.getLong(2), time24hr),
						service == TWITTER ? activity.getString(R.string.retweet) : service == IDENTICA ? activity.getString(R.string.repeat) : "");
			}
			status.close();
			break;
		case SonetProvider.NOTIFICATIONS:
			Cursor notification = activity.getContentResolver().query(Notifications.getContentUri(activity), new String[]{Notifications.FRIEND, Notifications.MESSAGE, Notifications.CREATED}, Notifications._ID + "=?", new String[]{data.getLastPathSegment()}, null);
			if (notification.moveToFirst()) {
				// clear notification
				ContentValues values = new ContentValues();
				values.put(Notifications.CLEARED, 1);
				activity.getContentResolver().update(Notifications.getContentUri(activity), values, Notifications._ID + "=?", new String[]{data.getLastPathSegment()});
				addComment(statusId,
						notification.getString(0),
						notification.getString(1),
						Sonet.getCreatedText(notification.getLong(2), time24hr),
						service == TWITTER ? activity.getString(R.string.retweet) : service == IDENTICA ? activity.getString(R.string.repeat) : "");
				serviceName = activity.getResources().getStringArray(R.array.service_entries)[service];
			}
			notification.close();
			break;
		default:
			publishProgress(new String[0]);
			addComment(statusId, "", "error, status not found", "", "");
		}
		//TODO: at this point the service specific task needs to take over
		return null;
	}

	protected void addComment(String id, String name, String message, String created, String action) {
		publishProgress(new String[]{id, name, message, created, action});
	}

	@Override
	protected void onProgressUpdate(String... params) {
		if (params.length > 0) {
			HashMap<String, String> commentMap = new HashMap<String, String>();
			commentMap.put(Statuses.SID, params[ID]);
			commentMap.put(Entities.FRIEND, params[NAME]);
			commentMap.put(Statuses.MESSAGE, params[MESSAGE]);
			commentMap.put(Statuses.CREATEDTEXT, params[CREATED]);
			commentMap.put(SonetComments.ACTION, params[ACTION]);
			activity.addComment(commentMap);
		} else
			activity.addComment(null);
	}

	@Override
	protected void onPostExecute(String message) {
		//TODO set default message handle in sub classes
		if (params != null) {
			if ((mService == TWITTER) || (mService == IDENTICA)) {
				mMessage.append(params[0]);
			} else {
				if (mService == LINKEDIN) {
					if (params[0].equals(getString(R.string.uncommentable))) {
						mSend.setEnabled(false);
						mMessage.setEnabled(false);
						mMessage.setText(R.string.uncommentable);
					} else
						setCommentStatus(0, params[0]);
				} else {
					setCommentStatus(0, params[0]);
				}
			}
		}
		activity.onCommentsLoaded(message);
	}

}
