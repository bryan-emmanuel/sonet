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

import static com.piusvelte.sonet.Sonet.RESULT_REFRESH;

import com.google.ads.*;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widget_accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Widgets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class About extends ListActivity implements DialogInterface.OnClickListener {
	private int[] mAppWidgetIds;
	private AppWidgetManager mAppWidgetManager;
	private boolean mUpdateWidget = false;
	private static final int REFRESH = Menu.FIRST;
	private static final int MANAGE_ACCOUNTS = Menu.FIRST + 1;
	private static final int DEFAULT_SETTINGS = Menu.FIRST + 2;
	private static final int REFRESH_WIDGETS = Menu.FIRST + 3;
	private static final int WIDGET_SETTINGS = Menu.FIRST + 4;
	private static final int ABOUT = Menu.FIRST + 5;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		AdView adView = new AdView(this, AdSize.BANNER, Sonet.GOOGLE_AD_ID);
		((LinearLayout) findViewById(R.id.ad)).addView(adView);
		adView.loadAd(new AdRequest());
		mAppWidgetIds = new int[0];
		// validate appwidgetids from appwidgetmanager
		mAppWidgetManager = AppWidgetManager.getInstance(this);
		mAppWidgetIds = Sonet.arrayCat(
				Sonet.arrayCat(mAppWidgetManager.getAppWidgetIds(new ComponentName(
						this, SonetWidget_4x2.class)),
						mAppWidgetManager.getAppWidgetIds(new ComponentName(
								this, SonetWidget_4x3.class))),
								mAppWidgetManager.getAppWidgetIds(new ComponentName(this,
										SonetWidget_4x4.class)));
		int[] removeAppWidgets = new int[0];
		this.getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=?", new String[] { "" });
		this.getContentResolver().delete(Widget_accounts.CONTENT_URI, Widget_accounts.WIDGET + "=?", new String[] { "" });
		Cursor widgets = this.getContentResolver().query(Widgets.CONTENT_URI, new String[] {Widgets._ID, Widgets.WIDGET}, Widgets.ACCOUNT + "=?", new String[] { Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);
		if (widgets.moveToFirst()) {
			int iwidget = widgets.getColumnIndex(Widgets.WIDGET), appWidgetId;
			while (!widgets.isAfterLast()) {
				appWidgetId = widgets.getInt(iwidget);
				if ((appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) && !Sonet.arrayContains(mAppWidgetIds, appWidgetId)) removeAppWidgets = Sonet.arrayAdd(removeAppWidgets, appWidgetId);
				widgets.moveToNext();
			}
		}
		widgets.close();
		if (removeAppWidgets.length > 0) {
			// remove phantom widgets
			for (int appWidgetId : removeAppWidgets) {
				this.getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
				this.getContentResolver().delete(Widget_accounts.CONTENT_URI, Widget_accounts.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
				this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
			}
		}
		registerForContextMenu(getListView());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, REFRESH, 0, R.string.button_refresh).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, REFRESH_WIDGETS, 0, R.string.refreshallwidgets).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, WIDGET_SETTINGS, 0, R.string.settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, DEFAULT_SETTINGS, 0, R.string.defaultsettings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MANAGE_ACCOUNTS, 0, R.string.accounts).setIcon(android.R.drawable.ic_menu_manage);
		menu.add(0, ABOUT, 0, R.string.about_title).setIcon(android.R.drawable.ic_menu_more);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case REFRESH:
			startService(new Intent(this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
			return true;
		case MANAGE_ACCOUNTS:
			startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
			return true;
		case DEFAULT_SETTINGS:
			startActivityForResult(new Intent(this, Settings.class), RESULT_REFRESH);
			return true;
		case REFRESH_WIDGETS:
			startService(new Intent(this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
			return true;
		case WIDGET_SETTINGS:
			if (mAppWidgetIds.length > 0) {
				String[] widgets = new String[mAppWidgetIds.length];
				for (int i = 0; i < mAppWidgetIds.length; i++) {
					AppWidgetProviderInfo info = mAppWidgetManager.getAppWidgetInfo(mAppWidgetIds[i]);
					String providerName = info.provider.getClassName();
					widgets[i] = Integer.toString(mAppWidgetIds[i])
					+ " ("
					+ (providerName == SonetWidget_4x2.class.getName() ? "4x2"
							: providerName == SonetWidget_4x3.class
							.getName() ? "4x3" : "4x4") + ")";
				}
				(new AlertDialog.Builder(this))
				.setItems(widgets, this)
				.setCancelable(true)
				.show();
			} else {
				Toast.makeText(this, getString(R.string.nowidgets),	Toast.LENGTH_LONG).show();
			}
			return true;
		case ABOUT:
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.about_dialog);
			dialog.setTitle(R.string.about_title);
			dialog.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Cursor accounts = this.getContentResolver().query(Widget_accounts.CONTENT_URI, new String[]{Widget_accounts._ID}, Widget_accounts.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
		if (!accounts.moveToFirst()) {
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.about_dialog);
			dialog.setTitle(R.string.about_title);
			dialog.show();
		} else {
			// if there are accounts, run the initial loading for About.class
			Cursor statuses = this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID}, Statuses.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
			if (!statuses.moveToFirst()) {
				startService(new Intent(this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
			}
			statuses.close();			
		}
		accounts.close();
		loadStatuses();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mUpdateWidget) startService(new Intent(this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == RESULT_REFRESH) && (resultCode == RESULT_OK)) mUpdateWidget = true;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[which]));
		dialog.cancel();
	}
	
	@Override
	protected void onListItemClick(ListView list, final View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		startActivity(new Intent(this, StatusDialog.class).setData(Uri.withAppendedPath(Statuses_styles.CONTENT_URI, Long.toString(id))));
	}

	private final SimpleCursorAdapter.ViewBinder mViewBinder = new SimpleCursorAdapter.ViewBinder() {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (columnIndex == cursor.getColumnIndex(Statuses_styles.FRIEND)) {
				((TextView) view).setText(cursor.getString(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.FRIEND_TEXTSIZE)) {
				((TextView) view).setTextSize(cursor.getLong(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.MESSAGE)) {
				((TextView) view).setText(cursor.getString(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.MESSAGES_TEXTSIZE)) {
				((TextView) view).setTextSize(cursor.getLong(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.STATUS_BG)) {
				byte[] b = cursor.getBlob(columnIndex);
				if (b != null) {
					Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
					if (bmp != null) {
						((ImageView) view).setImageBitmap(bmp);
					}
				}
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.PROFILE)) {
				byte[] b = cursor.getBlob(columnIndex);
				if (b != null) {
					Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
					if (bmp != null) {
						((ImageView) view).setImageBitmap(bmp);
					}
				}
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.FRIEND + "2")) {
				((TextView) view).setText(cursor.getString(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.CREATEDTEXT)) {
				((TextView) view).setText(cursor.getString(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.MESSAGE + "2")) {
				((TextView) view).setText(cursor.getString(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.FRIEND_COLOR)) {
				((TextView) view).setTextColor(cursor.getInt(columnIndex));
			return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.CREATED_COLOR)) {
				((TextView) view).setTextColor(cursor.getInt(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.MESSAGES_COLOR)) {
				((TextView) view).setTextColor(cursor.getInt(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.FRIEND_TEXTSIZE)) {
				((TextView) view).setTextSize(cursor.getLong(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.CREATED_TEXTSIZE)) {
				((TextView) view).setTextSize(cursor.getLong(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.MESSAGES_TEXTSIZE)) {
				((TextView) view).setTextSize(cursor.getLong(columnIndex));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.ICON)) {
				byte[] b = cursor.getBlob(columnIndex);
				if (b != null) {
					Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
					if (bmp != null) {
						((ImageView) view).setImageBitmap(bmp);
					}
				}
				return true;
			} else {
				return false;
			}
		}
	};

	private void loadStatuses() {
		Cursor c = this.managedQuery(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.FRIEND + " as " + Statuses_styles.FRIEND + "2", Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.MESSAGE + " as " + Statuses_styles.MESSAGE + "2", Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON}, Statuses_styles.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, Statuses_styles.CREATED + " desc");
		SimpleCursorAdapter sca = new SimpleCursorAdapter(About.this, R.layout.widget_item, c, new String[] {Statuses_styles.FRIEND, Statuses_styles.FRIEND + "2", Statuses_styles.MESSAGE, Statuses_styles.MESSAGE + "2", Statuses_styles.STATUS_BG, Statuses_styles.CREATEDTEXT, Statuses_styles.PROFILE, Statuses_styles.ICON}, new int[] {R.id.friend_bg_clear, R.id.friend, R.id.message_bg_clear, R.id.message, R.id.status_bg, R.id.created, R.id.profile, R.id.icon});
		sca.setViewBinder(mViewBinder);
		setListAdapter(sca);
	}
}
