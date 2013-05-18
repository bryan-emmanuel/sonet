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

import org.apache.http.client.HttpClient;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetCrypto;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetProvider;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.Sonet.Notifications;
import com.piusvelte.sonet.core.Sonet.Statuses_styles;
import com.piusvelte.sonet.core.Sonet.Widgets;
import com.piusvelte.sonet.core.Sonet.Widgets_settings;
import com.piusvelte.sonet.core.activity.SonetComments;

import android.appwidget.AppWidgetManager;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

public class CommentsCommonTask extends AsyncTask<String, String, String> {

	protected SonetComments activity;
	protected long accountId;
	protected HttpClient httpClient;
	protected SonetCrypto sonetCrypto;
	protected Uri data;

	public CommentsCommonTask(SonetComments activity, Uri data) {
		this.activity = activity;
		this.data = data;
		httpClient = SonetHttpClient.getThreadSafeClient(activity.getApplicationContext());
		sonetCrypto = SonetCrypto.getInstance(activity.getApplicationContext());
	}

	int service;
	String serviceName;
	String statusId;
	String entityId;
	boolean time24hr = false;
	String token;
	String secret;
	String accountServiceId;
	
	private void loadSession() {
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts.TOKEN, Accounts.SECRET, Accounts.SID, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			token = sonetCrypto.Decrypt(account.getString(0));
			secret = sonetCrypto.Decrypt(account.getString(1));
			accountServiceId = sonetCrypto.Decrypt(account.getString(2));
			service = account.getInt(3);
			serviceName = activity.getResources().getStringArray(R.array.service_entries)[service];
		}
		account.close();
	}
	
	protected void loadCommon() {
		UriMatcher um = new UriMatcher(UriMatcher.NO_MATCH);
		String authority = Sonet.getAuthority(activity);
		um.addURI(authority, SonetProvider.VIEW_STATUSES_STYLES + "/*", SonetProvider.STATUSES_STYLES);
		um.addURI(authority, SonetProvider.TABLE_NOTIFICATIONS + "/*", SonetProvider.NOTIFICATIONS);
		switch (um.match(data)) {
		case SonetProvider.STATUSES_STYLES:
			Cursor status = activity.getContentResolver().query(Statuses_styles.getContentUri(activity), new String[]{Statuses_styles.ACCOUNT, Statuses_styles.SID, Statuses_styles.ESID, Statuses_styles.WIDGET, Statuses_styles.SERVICE, Statuses_styles.FRIEND, Statuses_styles.MESSAGE, Statuses_styles.CREATED}, Statuses_styles._ID + "=?", new String[]{data.getLastPathSegment()}, null);
			if (status.moveToFirst()) {
				accountId = status.getLong(0);
				statusId = sonetCrypto.Decrypt(status.getString(1));
				entityId = sonetCrypto.Decrypt(status.getString(2));
				Cursor widget = activity.getContentResolver().query(Widgets_settings.getContentUri(activity), new String[]{Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(status.getInt(3)), Long.toString(accountId)}, null);
				if (widget.moveToFirst())
					time24hr = widget.getInt(0) == 1;
				else {
					Cursor b = activity.getContentResolver().query(Widgets_settings.getContentUri(activity), new String[]{Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(status.getInt(3)), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
					if (b.moveToFirst())
						time24hr = b.getInt(0) == 1;
					else {
						Cursor c = activity.getContentResolver().query(Widgets_settings.getContentUri(activity), new String[]{Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
						if (c.moveToFirst())
							time24hr = c.getInt(0) == 1;
						else
							time24hr = false;
						c.close();
					}
					b.close();
				}
				widget.close();
				loadSession();
			}
			status.close();
			break;
		case SonetProvider.NOTIFICATIONS:
			Cursor notification = activity.getContentResolver().query(Notifications.getContentUri(activity), new String[]{Notifications.ACCOUNT, Notifications.SID, Notifications.ESID, Notifications.FRIEND, Notifications.MESSAGE, Notifications.CREATED}, Notifications._ID + "=?", new String[]{data.getLastPathSegment()}, null);
			if (notification.moveToFirst()) {
				accountId = notification.getLong(0);
				statusId = sonetCrypto.Decrypt(notification.getString(1));
				entityId = sonetCrypto.Decrypt(notification.getString(2));
				time24hr = false;
				loadSession();
			}
			notification.close();
			break;
		}
	}

	@Override
	protected String doInBackground(String... params) {
		loadCommon();
		return null;
	}

}
