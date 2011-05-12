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
import static com.piusvelte.sonet.Sonet.RESULT_REFRESH;
import static com.piusvelte.sonet.Sonet.ACCOUNTS_QUERY;

import com.google.ads.*;
import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Widgets;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageAccounts extends ListActivity implements OnClickListener, DialogInterface.OnClickListener {
	private static final int REAUTH_ID = Menu.FIRST;
	private static final int SETTINGS_ID = Menu.FIRST + 1;
	private static final int DELETE_ID = Menu.FIRST + 2;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private boolean mHasAccounts = false,
	mAddingAccount,
	mUpdateWidget = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setResult(RESULT_CANCELED);

		setContentView(R.layout.accounts);
		AdView adView = new AdView(this, AdSize.BANNER, Sonet.GOOGLE_AD_ID);
		((LinearLayout) findViewById(R.id.ad)).addView(adView);
		adView.loadAd(new AdRequest());

		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			// if called from widget, the id is set in the action, as pendingintents must have a unique action
			else if ((intent.getAction() != null) && (!intent.getAction().equals(ACTION_REFRESH)) && (!intent.getAction().equals(Intent.ACTION_VIEW))) mAppWidgetId = Integer.parseInt(intent.getAction());
		}
		registerForContextMenu(getListView());
		((Button) findViewById(R.id.default_widget_settings)).setOnClickListener(this);
		((Button) findViewById(R.id.button_add_account)).setOnClickListener(this);
		
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish();
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		final long item = id;
		final CharSequence[] items = {getString(R.string.re_authenticate), getString(R.string.account_settings)};
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				which++; //fix indexing
				switch (which) {
				case REAUTH_ID:
					// need the account id if reauthenticating
					Cursor c = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(item)}, null);
					if (c.moveToFirst()) {
						mAddingAccount = true;
						startActivityForResult(new Intent(ManageAccounts.this, OAuthLogin.class).putExtra(Accounts.SERVICE, c.getInt(c.getColumnIndex(Accounts.SERVICE))).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, item), RESULT_REFRESH);
					}
					c.close();
					break;
				case SETTINGS_ID:
					mAddingAccount = true;
					startActivityForResult(new Intent(ManageAccounts.this, AccountSettings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, item), RESULT_REFRESH);
					break;
				}
				dialog.cancel();
			}
		}).show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.delete_account);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == DELETE_ID) {
			mUpdateWidget = true;
			getContentResolver().delete(Accounts.CONTENT_URI, Accounts._ID + "=?", new String[]{Long.toString(((AdapterContextMenuInfo) item.getMenuInfo()).id)});
			// need to delete the statuses and settings for this account
			getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(mAppWidgetId), Long.toString(((AdapterContextMenuInfo) item.getMenuInfo()).id)});
			getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(mAppWidgetId), Long.toString(((AdapterContextMenuInfo) item.getMenuInfo()).id)});
		}
		return super.onContextItemSelected(item);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_add_account:
			// add a new account
			String[] services = getResources().getStringArray(R.array.service_entries);
			(new AlertDialog.Builder(this))
			.setItems(services, this)
			.show();
			break;
		case R.id.default_widget_settings:
			mAddingAccount = true;
			startActivityForResult(new Intent(this, Settings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId), RESULT_REFRESH);
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		listAccounts();
		mAddingAccount = false;
		if (mUpdateWidget && mHasAccounts) {
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (!mAddingAccount && mUpdateWidget) startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
		else if (!mHasAccounts) {
			// clean up any setup for this widget
			getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=" + mAppWidgetId, null);
			getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=" + mAppWidgetId, null);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == RESULT_REFRESH) && (resultCode == RESULT_OK)) {
			mUpdateWidget = true;
		}
	}

	private void listAccounts() {
		// prepend service name to username
		Cursor c = this.managedQuery(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY, Accounts.SERVICE}, Accounts.WIDGET + "=?", new String[]{Integer.toString(mAppWidgetId)}, null);
		mHasAccounts = c.getCount() != 0;
		setListAdapter(new SimpleCursorAdapter(ManageAccounts.this, R.layout.accounts_row, c, new String[] {Accounts.USERNAME}, new int[] {R.id.account_username}));
	}
	

	public void onClick(DialogInterface dialog, int which) {
		mAddingAccount = true;
		startActivityForResult(new Intent(this, OAuthLogin.class).putExtra(Accounts.SERVICE, which).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID), RESULT_REFRESH);
		dialog.cancel();
	}

}
