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

import android.app.Activity;
//import android.app.AlertDialog;
//import android.appwidget.AppWidgetManager;
//import android.appwidget.AppWidgetProviderInfo;
//import android.content.ComponentName;
//import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class About extends Activity implements View.OnClickListener/*, DialogInterface.OnClickListener*/ {
//	private int[] mAppWidgetIds;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
//		((Button) findViewById(R.id.widgets)).setOnClickListener(this);
		((Button) findViewById(R.id.donate)).setOnClickListener(this);
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
//		case R.id.widgets:
//			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
//			mAppWidgetIds = arrayCat(arrayCat(appWidgetManager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x2.class)), appWidgetManager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x3.class))), appWidgetManager.getAppWidgetIds(new ComponentName(this, SonetWidget_4x4.class)));
//			String[] widgets = new String[mAppWidgetIds.length];
//			for (int i = 0; i < mAppWidgetIds.length; i++) {
//				AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(mAppWidgetIds[i]);
//				String providerName = info.provider.getClassName();
//				widgets[i] = Integer.toString(mAppWidgetIds[i]) + " (" + (providerName == SonetWidget_4x2.class.getName() ? "4x2" : providerName == SonetWidget_4x3.class.getName() ? "4x3" : "4x4") + ")";
//			}
//			(new AlertDialog.Builder(this))
//			.setItems(widgets, this)
//			.setCancelable(true)
//			.show();
//			break;
		case R.id.donate:
			startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://www.piusvelte.com?p=donate-sonet")));
			break;
		}
	}
	
//	@Override
//	public void onClick(DialogInterface dialog, int which) {
//		startActivity((new Intent(this, UI.class)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[which]));
//		dialog.cancel();
//	}
}
