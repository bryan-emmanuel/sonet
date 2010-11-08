package com.piusvelte.sonet;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class UI extends Activity implements OnClickListener {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(Activity.RESULT_OK, (new Intent()).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
		setContentView(R.layout.main);
		if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			((Button) findViewById(R.id.button_accounts)).setOnClickListener(this);
			((Button) findViewById(R.id.button_settings)).setOnClickListener(this);			
		}
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_accounts:
			startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
			break;
		case R.id.button_settings:
			startActivity(new Intent(this, Settings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
			break;
		}
	}

}
