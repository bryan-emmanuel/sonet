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
        setResult(Activity.RESULT_OK, (new Intent()).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)));
		setContentView(R.layout.main);
		((Button) findViewById(R.id.button_accounts)).setOnClickListener(this);
		((Button) findViewById(R.id.button_settings)).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		onWidgetClick(v);	
	}
	
	public void onWidgetClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.button_accounts:
			startActivity(new Intent(this, ManageAccounts.class));
			break;
		case R.id.button_settings:
			startActivity(new Intent(this, Settings.class));
			break;
		case R.id.button_post:
			break;
		case R.id.button_configure:
			startActivity(new Intent(this, UI.class));
			break;
		case R.id.button_refresh:
			break;
		}
	}

}
