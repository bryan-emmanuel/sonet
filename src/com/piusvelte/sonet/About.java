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
import static com.piusvelte.sonet.Sonet.DONATE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.SonetDatabaseHelper._ID;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class About extends Activity implements View.OnClickListener, DialogInterface.OnClickListener {
	private int[] mAppWidgetIds;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		((Button) findViewById(R.id.widgets)).setOnClickListener(this);
		((Button) findViewById(R.id.refreshall)).setOnClickListener(this);
		((Button) findViewById(R.id.donate)).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.widgets:
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
			SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
			SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
			db.delete(TABLE_WIDGETS, WIDGET + "=\"\"", null);
			db.delete(TABLE_ACCOUNTS, WIDGET + "=\"\"", null);
			Cursor accounts = db.rawQuery("select " + _ID + "," + WIDGET + " from " + TABLE_ACCOUNTS, null);
			if (accounts.getCount() > 0) {
				accounts.moveToFirst();
				mAppWidgetIds = new int[accounts.getCount()];
				int iwidget = accounts.getColumnIndex(WIDGET),
				counter = 0;
				while (!accounts.isAfterLast()) {
					mAppWidgetIds[counter] = accounts.getInt(iwidget);
					counter++;
					accounts.moveToNext();
				}
			} else mAppWidgetIds = new int[0];
			// delete records without accounts
			String delete_widgets = "";
			for (int appWidgetId : mAppWidgetIds) {
				if (delete_widgets.length() > 0) delete_widgets += " and ";
				delete_widgets += WIDGET + "!=" + Integer.toString(appWidgetId);
			}
			if (delete_widgets.length() > 0) db.delete(TABLE_WIDGETS, delete_widgets, null);
			accounts.close();
			db.close();
			sonetDatabaseHelper.close();
			String[] widgets = new String[mAppWidgetIds.length];
			for (int i = 0; i < mAppWidgetIds.length; i++) {
				AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(mAppWidgetIds[i]);
				String providerName = info.provider.getClassName();
				widgets[i] = Integer.toString(mAppWidgetIds[i]) + " (" + (providerName == SonetWidget_4x2.class.getName() ? "4x2" : providerName == SonetWidget_4x3.class.getName() ? "4x3" : "4x4") + ")";
			}
			(new AlertDialog.Builder(this))
			.setItems(widgets, this)
			.setCancelable(true)
			.show();
			break;
		case R.id.refreshall:
			startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH));
			break;
		case R.id.donate:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DONATE)));
			break;
		}
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		startActivity(new Intent(this, UI.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[which]));
		dialog.cancel();
	}
}
