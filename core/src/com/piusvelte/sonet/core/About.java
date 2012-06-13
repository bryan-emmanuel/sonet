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
package com.piusvelte.sonet.core;

import static com.piusvelte.sonet.core.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.core.Sonet.PRO;
import static com.piusvelte.sonet.core.Sonet.RESULT_REFRESH;
import static com.piusvelte.sonet.core.Sonet.sBFOptions;
import mobi.intuitit.android.content.LauncherIntent;

import com.google.ads.*;
import com.piusvelte.sonet.core.Sonet.Statuses;
import com.piusvelte.sonet.core.Sonet.Statuses_styles;
import com.piusvelte.sonet.core.Sonet.Widget_accounts;
import com.piusvelte.sonet.core.Sonet.Widgets;
import com.piusvelte.sonet.core.Sonet.Widgets_settings;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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
	private static final int REFRESH = 1;
	private static final int MANAGE_ACCOUNTS = 2;
	private static final int REFRESH_WIDGETS = 3;
	private static final int DEFAULT_SETTINGS = 4;
	private static final int NOTIFICATIONS = 5;
	private static final int WIDGET_SETTINGS = 6;
	private static final int ABOUT = 7;
	private static final String TAG = "About";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		if (!getPackageName().toLowerCase().contains(PRO)) {
			AdView adView = new AdView(this, AdSize.BANNER, SonetTokens.GOOGLE_AD_ID);
			((LinearLayout) findViewById(R.id.ad)).addView(adView);
			adView.loadAd(new AdRequest());
		}
		registerForContextMenu(getListView());
		
		Drawable wp = WallpaperManager.getInstance(getApplicationContext()).getDrawable();
		if (wp != null) {
			findViewById(R.id.ad).getRootView().setBackgroundDrawable(wp);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, REFRESH, 0, R.string.button_refresh).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, MANAGE_ACCOUNTS, 0, R.string.accounts_and_settings).setIcon(android.R.drawable.ic_menu_manage);
		menu.add(0, REFRESH_WIDGETS, 0, R.string.refreshallwidgets).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, DEFAULT_SETTINGS, 0, R.string.defaultsettings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, NOTIFICATIONS, 0, R.string.notifications).setIcon(android.R.drawable.ic_menu_more);
		menu.add(0, WIDGET_SETTINGS, 0, R.string.widget_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, ABOUT, 0, R.string.about_title).setIcon(android.R.drawable.ic_menu_more);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case REFRESH:
			startService(Sonet.getPackageIntent(this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID).setAction(ACTION_REFRESH));
			return true;
		case MANAGE_ACCOUNTS:
			startActivity(Sonet.getPackageIntent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
			return true;
		case DEFAULT_SETTINGS:
			startActivityForResult(Sonet.getPackageIntent(this, Settings.class), RESULT_REFRESH);
			return true;
		case REFRESH_WIDGETS:
			startService(Sonet.getPackageIntent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
			return true;
		case NOTIFICATIONS:
			startActivity(Sonet.getPackageIntent(this, SonetNotifications.class));
			return true;
		case WIDGET_SETTINGS:
			if (mAppWidgetIds.length > 0) {
				String[] widgets = new String[mAppWidgetIds.length];
				for (int i = 0, i2 = mAppWidgetIds.length; i < i2; i++) {
					AppWidgetProviderInfo info = mAppWidgetManager.getAppWidgetInfo(mAppWidgetIds[i]);
					String providerName = info.provider.getClassName();
					widgets[i] = Integer.toString(mAppWidgetIds[i]) + " (" + providerName + ")";
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
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle(R.string.about_title);
			dialog.setMessage(R.string.about);
			dialog.setPositiveButton(android.R.string.ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					arg0.cancel();
				}

			});
			dialog.setCancelable(true);
			dialog.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG,"onResume");
		mAppWidgetIds = new int[0];
		// validate appwidgetids from appwidgetmanager
		mAppWidgetManager = AppWidgetManager.getInstance(About.this);
		mAppWidgetIds = Sonet.getWidgets(getApplicationContext(), mAppWidgetManager);
		(new WidgetsDataLoader()).execute();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mUpdateWidget) {
			(Toast.makeText(getApplicationContext(), getString(R.string.refreshing), Toast.LENGTH_LONG)).show();
			startService(Sonet.getPackageIntent(this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == RESULT_REFRESH) && (resultCode == RESULT_OK)) mUpdateWidget = true;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		startActivity(Sonet.getPackageIntent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[which]));
		dialog.cancel();
	}

	@Override
	protected void onListItemClick(ListView list, final View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		Rect r = new Rect();
		view.getHitRect(r);
		startActivity(Sonet.getPackageIntent(this, StatusDialog.class).setData(Uri.withAppendedPath(Statuses_styles.getContentUri(this), Long.toString(id))).putExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS, r).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

	private final SimpleCursorAdapter.ViewBinder mViewBinder = new SimpleCursorAdapter.ViewBinder() {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (columnIndex == cursor.getColumnIndex(Statuses_styles.FRIEND)) {
				((TextView) view).setText(cursor.getString(columnIndex));
				((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(Statuses_styles.FRIEND_TEXTSIZE)));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.MESSAGE)) {
				((TextView) view).setText(cursor.getString(columnIndex));
				((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(Statuses_styles.MESSAGES_TEXTSIZE)));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.STATUS_BG)) {
				byte[] b = cursor.getBlob(columnIndex);
				Bitmap bmp = null;
				if (b != null) {
					bmp = BitmapFactory.decodeByteArray(b, 0, b.length, sBFOptions);
					if (bmp != null) {
						((ImageView) view).setImageBitmap(bmp);
					}
				}
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.PROFILE)) {
				byte[] b = cursor.getBlob(columnIndex);
				Bitmap bmp = null;
				if (b != null) {
					bmp = BitmapFactory.decodeByteArray(b, 0, b.length, sBFOptions);
					if (bmp != null) {
						((ImageView) view).setImageBitmap(bmp);
					}
				}
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.FRIEND + "2")) {
				((TextView) view).setText(cursor.getString(columnIndex));
				((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(Statuses_styles.FRIEND_TEXTSIZE)));
				((TextView) view).setTextColor(cursor.getInt(cursor.getColumnIndex(Statuses_styles.FRIEND_COLOR)));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.CREATEDTEXT)) {
				((TextView) view).setText(cursor.getString(columnIndex));
				((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(Statuses_styles.CREATED_TEXTSIZE)));
				((TextView) view).setTextColor(cursor.getInt(cursor.getColumnIndex(Statuses_styles.CREATED_COLOR)));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.MESSAGE + "2")) {
				((TextView) view).setText(cursor.getString(columnIndex));
				((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(Statuses_styles.MESSAGES_TEXTSIZE)));
				((TextView) view).setTextColor(cursor.getInt(cursor.getColumnIndex(Statuses_styles.MESSAGES_COLOR)));
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.ICON)) {
				byte[] b = cursor.getBlob(columnIndex);
				Bitmap bmp = null;
				if (b != null) {
					bmp = BitmapFactory.decodeByteArray(b, 0, b.length, sBFOptions);
					if (bmp != null) {
						((ImageView) view).setImageBitmap(bmp);
					}
				}
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.PROFILE_BG)) {
				byte[] b = cursor.getBlob(columnIndex);
				Bitmap bmp = null;
				if (b != null) {
					bmp = BitmapFactory.decodeByteArray(b, 0, b.length, sBFOptions);
					if (bmp != null) {
						((ImageView) view).setImageBitmap(bmp);
					}
				}
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.FRIEND_BG)) {
				byte[] b = cursor.getBlob(columnIndex);
				Bitmap bmp = null;
				if (b != null) {
					bmp = BitmapFactory.decodeByteArray(b, 0, b.length, sBFOptions);
					if (bmp != null) {
						((ImageView) view).setImageBitmap(bmp);
					}
				}
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.IMAGE_BG)) {
				byte[] b = cursor.getBlob(columnIndex);
				Bitmap bmp = null;
				if (b != null) {
					bmp = BitmapFactory.decodeByteArray(b, 0, b.length, sBFOptions);
					if (bmp != null) {
						((ImageView) view).setImageBitmap(bmp);
					}
				}
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.IMAGE)) {
				byte[] b = cursor.getBlob(columnIndex);
				Bitmap bmp = null;
				if (b != null) {
					bmp = BitmapFactory.decodeByteArray(b, 0, b.length, sBFOptions);
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

	class WidgetsDataLoader extends AsyncTask<Void, Boolean, Integer> {

		@Override
		protected Integer doInBackground(Void... arg0) {
			Log.d(TAG,"WidgetsDataLoader executing");
			int[] removeAppWidgets = new int[0];
			// remove old widgets that didn't have ids
			getContentResolver().delete(Widgets.getContentUri(About.this), Widgets.WIDGET + "=?", new String[] {""});
			getContentResolver().delete(Widget_accounts.getContentUri(About.this), Widget_accounts.WIDGET + "=?", new String[] {""});
			Cursor widgets = getContentResolver().query(Widgets.getContentUri(About.this), new String[] {Widgets._ID, Widgets.WIDGET}, Widgets.ACCOUNT + "=?", new String[] { Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);
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
					getContentResolver().delete(Widgets.getContentUri(About.this), Widgets.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
					getContentResolver().delete(Widget_accounts.getContentUri(About.this), Widget_accounts.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
					getContentResolver().delete(Statuses.getContentUri(About.this), Statuses.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
				}
			}
			int result = 0;
			boolean profile = true;
			Cursor accounts = getContentResolver().query(Widget_accounts.getContentUri(About.this), new String[]{Widget_accounts._ID}, Widget_accounts.WIDGET + "=0", null, null);
			if (accounts.moveToFirst()) {
				Cursor widget = getContentResolver().query(Widgets_settings.getContentUri(About.this), new String[]{Widgets.DISPLAY_PROFILE}, Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1", null, null);
				if (widget.moveToFirst()) {
					profile = widget.getInt(0) == 1;
				} else {
					// initialize account settings
					ContentValues values = new ContentValues();
					values.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
					values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
					getContentResolver().insert(Widgets.getContentUri(About.this), values).getLastPathSegment();
				}
				widget.close();
				Cursor statuses = getContentResolver().query(Widget_accounts.getContentUri(About.this), new String[]{Statuses._ID}, Statuses.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, null);
				if (!statuses.moveToFirst()) {
					// no statuses, load them
					result = 1;
				}
				statuses.close();
			} else {
				// no account, just show about message
				result = 2;
			}
			accounts.close();
			publishProgress(profile);
			return result;
		}

		@Override
		protected void onProgressUpdate(Boolean... profile) {
			Log.d(TAG,"set query using profile:"+profile[0]);
			Cursor c;
			SimpleCursorAdapter sca;
			if (profile[0]) {
				c = managedQuery(Statuses_styles.getContentUri(About.this), new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.FRIEND + " as " + Statuses_styles.FRIEND + "2", Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.MESSAGE + " as " + Statuses_styles.MESSAGE + "2", Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON, Statuses_styles.PROFILE_BG, Statuses_styles.FRIEND_BG, Statuses_styles.IMAGE_BG, Statuses_styles.IMAGE}, Statuses_styles.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, Statuses_styles.CREATED + " desc");
				sca = new SimpleCursorAdapter(About.this, R.layout.widget_item, c, new String[] {Statuses_styles.FRIEND, Statuses_styles.FRIEND + "2", Statuses_styles.MESSAGE, Statuses_styles.MESSAGE + "2", Statuses_styles.STATUS_BG, Statuses_styles.CREATEDTEXT, Statuses_styles.PROFILE, Statuses_styles.ICON, Statuses_styles.PROFILE_BG, Statuses_styles.FRIEND_BG, Statuses_styles.IMAGE_BG, Statuses_styles.IMAGE}, new int[] {R.id.friend_bg_clear, R.id.friend, R.id.message_bg_clear, R.id.message, R.id.status_bg, R.id.created, R.id.profile, R.id.icon, R.id.profile_bg, R.id.friend_bg, R.id.image_clear, R.id.image});
			} else {
				c = managedQuery(Statuses_styles.getContentUri(About.this), new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.FRIEND + " as " + Statuses_styles.FRIEND + "2", Statuses_styles.MESSAGE, Statuses_styles.MESSAGE + " as " + Statuses_styles.MESSAGE + "2", Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON, Statuses_styles.FRIEND_BG, Statuses_styles.IMAGE_BG, Statuses_styles.IMAGE}, Statuses_styles.WIDGET + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID)}, Statuses_styles.CREATED + " desc");
				sca = new SimpleCursorAdapter(About.this, R.layout.widget_item_noprofile, c, new String[] {Statuses_styles.FRIEND, Statuses_styles.FRIEND + "2", Statuses_styles.MESSAGE, Statuses_styles.MESSAGE + "2", Statuses_styles.STATUS_BG, Statuses_styles.CREATEDTEXT, Statuses_styles.ICON, Statuses_styles.FRIEND_BG, Statuses_styles.IMAGE_BG, Statuses_styles.IMAGE}, new int[] {R.id.friend_bg_clear, R.id.friend, R.id.message_bg_clear, R.id.message, R.id.status_bg, R.id.created, R.id.icon, R.id.friend_bg, R.id.image_clear, R.id.image});
			}
			sca.setViewBinder(mViewBinder);
			setListAdapter(sca);
		}

		@Override
		protected void onPostExecute(Integer result) {
			// 0 - existing statuses, loaded by onProgressUpdate
			// 1 - no statuses, service will load them
			// 2 - no account, open alert
			Log.d(TAG,"result:"+result);
			if (result > 0) {
				// accounts, but no statuses
				startService(Sonet.getPackageIntent(About.this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID).setAction(ACTION_REFRESH));
				if (result > 1) {
					// no accounts
					AlertDialog.Builder dialog = new AlertDialog.Builder(About.this);
					dialog.setTitle(R.string.about_title);
					dialog.setMessage(R.string.about);
					dialog.setPositiveButton(android.R.string.ok, new OnClickListener() {

						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							arg0.cancel();
						}

					});
					dialog.setCancelable(true);
					dialog.show();
				}
			}
		}

	}
}
