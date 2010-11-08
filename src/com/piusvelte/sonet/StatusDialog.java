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

import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

public class StatusDialog extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private static final int LINK = 0;
	private static final int POST = 1;
	private static final int SETTINGS = 2;
	private static final int REFRESH = 4;
	private int mService = 0;
	private String mLink = "";
	public static final String MESSAGE = "message";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
			mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			mService = intent.getIntExtra(SERVICE, 0);
			mLink = intent.getStringExtra(MESSAGE);
		}
		Resources r = getResources();
		CharSequence[] items = {r.getString(R.string.reply), "Post to " + r.getStringArray(R.array.service_entries)[mService], r.getString(R.string.settings), r.getString(R.string.button_refresh)};
		(new AlertDialog.Builder(this))
		.setItems(items, this)
		.setCancelable(true)
		.setOnCancelListener(this)
		.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case LINK:
			startActivity((new Intent(Intent.ACTION_VIEW, Uri.parse(mLink))).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity")));
			break;
		case POST:
			switch (mService) {
			case TWITTER:
				startActivity((new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com"))).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity")));
				break;
			case FACEBOOK:
				startActivity((new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com"))).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity")));
				break;
			case MYSPACE:
				startActivity((new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.myspace.com"))).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity")));
				break;
			}
			break;
		case SETTINGS:
			startActivity((new Intent(this, UI.class)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
			break;
		case REFRESH:
			startService((new Intent(this, SonetService.class)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
			break;
		}
		dialog.cancel();
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}	

}
