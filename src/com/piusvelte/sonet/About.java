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
//import android.content.ComponentName;
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
		((Button) findViewById(R.id.button_remove)).setOnClickListener(this);
	}

//	public int[] arrayCat(int[] a, int[] b) {
//		int[] c;
//		for (int i : b) {
//			c = new int[a.length];
//			for (int n = 0; n < c.length; n++) c[n] = a[n];
//			a = new int[c.length + 1];
//			for (int n = 0; n < c.length; n++) a[n] = c[n];
//			a[c.length] = i;
//		}
//		return a;
//	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.widgets:
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
//			mAppWidgetIds = arrayCat(arrayCat(appWidgetManager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x2.class)), appWidgetManager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x3.class))), appWidgetManager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x4.class)));
			SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
			SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
			mAppWidgetIds = getAppWidgetIds(db);
			db.close();
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
		case R.id.button_remove:
			cleanDb();
			break;
		case R.id.donate:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DONATE)));
			break;
		}
	}
	
	private void cleanDb() {
		SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
		SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
		db.delete(TABLE_WIDGETS, WIDGET + "=\"\"", null);
		db.delete(TABLE_ACCOUNTS, WIDGET + "=\"\"", null);
		int[] appWidgetIds = getAppWidgetIds(db);
		String delete_widgets = "";
		for (int appWidgetId : appWidgetIds) {
			if (delete_widgets.length() > 0) delete_widgets += " and ";
			delete_widgets += WIDGET + "!=" + Integer.toString(appWidgetId);
		}
		if (delete_widgets.length() > 0) db.delete(TABLE_WIDGETS, delete_widgets, null);
		db.close();
		sonetDatabaseHelper.close();		
	}
	
	private int[] getAppWidgetIds(SQLiteDatabase db) {
		int[] appWidgetIds = null;
		Cursor accounts = db.rawQuery("select " + _ID + "," + WIDGET + " from " + TABLE_ACCOUNTS, null);
		if (accounts.getCount() > 0) {
			accounts.moveToFirst();
			appWidgetIds = new int[accounts.getCount()];
			int iwidget = accounts.getColumnIndex(WIDGET),
			counter = 0;
			while (!accounts.isAfterLast()) {
				appWidgetIds[counter] = accounts.getInt(iwidget);
				counter++;
				accounts.moveToNext();
			}
		} else appWidgetIds = new int[0];
		accounts.close();
		return appWidgetIds;
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		startActivity(new Intent(this, UI.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[which]));
		dialog.cancel();
	}
}
