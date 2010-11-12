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

//import static com.piusvelte.sonet.Sonet.ACTION_REMOVE;
import static com.piusvelte.sonet.Sonet.TAG;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
//import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class UI extends Activity implements OnClickListener {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		if ((i != null) && i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(Activity.RESULT_OK, (new Intent()).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
		setContentView(R.layout.main);
		Log.v(TAG,"UI:"+mAppWidgetId);
		if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			Log.v(TAG,"is valid");
			((Button) findViewById(R.id.button_accounts)).setOnClickListener(this);
			((Button) findViewById(R.id.button_settings)).setOnClickListener(this);
//			((Button) findViewById(R.id.button_remove)).setOnClickListener(this);
		}
		((Button) findViewById(R.id.donate)).setOnClickListener(this);
	}

	public void onClick(View v) {
		Log.v(TAG,"UI onClick");
		switch (v.getId()) {
		case R.id.button_accounts:
			startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
			break;
		case R.id.button_settings:
			startActivity(new Intent(this, Settings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
			break;
//		case R.id.button_remove:
//			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
//			AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(mAppWidgetId);
//			String providerName = info.provider.getClassName();
//			sendBroadcast((new Intent(this, providerName == SonetWidget_4x2.class.getName() ? SonetWidget_4x2.class : providerName == SonetWidget_4x3.class.getName() ? SonetWidget_4x3.class : SonetWidget_4x4.class)).setAction(ACTION_REMOVE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
//			finish();
//			break;
		case R.id.donate:
			startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://www.piusvelte.com?p=donate-sonet")));
			break;
		}
	}

}
