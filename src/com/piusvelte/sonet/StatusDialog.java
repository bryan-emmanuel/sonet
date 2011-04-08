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

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;

import com.piusvelte.sonet.Sonet.Statuses_styles;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public class StatusDialog extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	// options are:
	// Like
	// Comment|Retweet
	// Post
	// Settings
	// Refresh
	private static final int LIKE = 0;
	private static final int COMMENT = 1;
	private static final int POST = 2;
	private static final int SETTINGS = 3;
	private static final int REFRESH = 4;
	private int mService = 0;
	private Uri mData;
	// TWITTER status link String.format("%s/%s/status/%s", sWebsites[service], user.getString("screen_name"), Long.toString(entry.getLong("id")))

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null) {
			mData = intent.getData();
			Cursor c = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.WIDGET, Statuses_styles.SERVICE}, Statuses_styles._ID + "=?", new String[] {mData.getLastPathSegment()}, null);
			if (c.moveToFirst()) {
				mAppWidgetId = c.getInt(c.getColumnIndex(Statuses_styles.WIDGET));
				mService = c.getInt(c.getColumnIndex(Statuses_styles.SERVICE));
			}
			c.close();
		}
		Resources r = getResources();
		CharSequence[] items = {getString(R.string.like), r.getStringArray(R.array.service_comment)[mService], r.getStringArray(R.array.service_post)[mService], getString(R.string.settings), getString(R.string.button_refresh)};
		(new AlertDialog.Builder(this))
		.setItems(items, this)
		.setCancelable(true)
		.setOnCancelListener(this)
		.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case LIKE:
			//TODO: FB Like
			break;
		case COMMENT:
			startActivity(new Intent(this, SonetCreatePost.class).setData(mData));
			break;
		case POST:
			startActivity(new Intent(this, SonetCreatePost.class));
			break;
		case SETTINGS:
			startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
			break;
		case REFRESH:
			startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
			break;
		}
		dialog.cancel();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}	

}
