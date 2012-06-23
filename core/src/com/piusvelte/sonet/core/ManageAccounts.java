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
import static com.piusvelte.sonet.core.Sonet.RSS;
import static com.piusvelte.sonet.core.Sonet.SMS;
import static com.piusvelte.sonet.core.Sonet.map_icons;
import static com.piusvelte.sonet.core.Sonet.sBFOptions;
import static com.piusvelte.sonet.core.SonetProvider.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.core.SonetProvider.TABLE_WIDGETS;
import static com.piusvelte.sonet.core.SonetProvider.TABLE_WIDGET_ACCOUNTS;

import com.example.android.actionbarcompat.ActionBarListActivity;
import com.google.ads.*;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.Sonet.Accounts_styles;
import com.piusvelte.sonet.core.Sonet.Notifications;
import com.piusvelte.sonet.core.Sonet.Status_images;
import com.piusvelte.sonet.core.Sonet.Status_links;
import com.piusvelte.sonet.core.Sonet.Statuses;
import com.piusvelte.sonet.core.Sonet.Statuses_styles;
import com.piusvelte.sonet.core.Sonet.Widget_accounts;
import com.piusvelte.sonet.core.Sonet.Widgets;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageAccounts extends ActionBarListActivity implements DialogInterface.OnClickListener {
	private static final int REAUTH_ID = Menu.FIRST;
	private static final int SETTINGS_ID = Menu.FIRST + 1;
	private static final int ENABLE_ID = Menu.FIRST + 2;
	private static final int DELETE_ID = Menu.FIRST + 3;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private boolean mAddingAccount,
	mUpdateWidget = false;
	private AlertDialog mDialog;
	private static final String TAG = "ManageAccounts";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.accounts);
		if (!getPackageName().toLowerCase().contains(PRO)) {
			AdView adView = new AdView(this, AdSize.BANNER, SonetTokens.GOOGLE_AD_ID);
			((LinearLayout) findViewById(R.id.ad)).addView(adView);
			adView.loadAd(new AdRequest());
		}

		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null)
				mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			// if called from widget, the id is set in the action, as pendingintents must have a unique action
			else if ((intent.getAction() != null) && (!intent.getAction().equals(ACTION_REFRESH)) && (!intent.getAction().equals(Intent.ACTION_VIEW)))
				mAppWidgetId = Integer.parseInt(intent.getAction());
		}

		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);

		registerForContextMenu(getListView());
		
		Drawable wp = WallpaperManager.getInstance(getApplicationContext()).getDrawable();
		if (wp != null)
			findViewById(R.id.ad).getRootView().setBackgroundDrawable(wp);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_manageaccounts, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.button_add_account) {
			// add a new account
			String[] services = getResources().getStringArray(R.array.service_entries);
			mDialog = (new AlertDialog.Builder(this))
			.setItems(services, this)
			.create();
			mDialog.show();
		} else if (itemId == R.id.default_widget_settings) {
			mAddingAccount = true;
			startActivityForResult(Sonet.getPackageIntent(this, Settings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId), RESULT_REFRESH);
		}
		return super.onOptionsItemSelected(item);
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
				Bitmap bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas canvas = new Canvas(bmp);
				canvas.drawColor(cursor.getInt(columnIndex));
				((ImageView) view).setImageBitmap(bmp);
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.PROFILE)) {
				Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_contact_picture, sBFOptions);
				if (bmp != null) {
					((ImageView) view).setImageBitmap(bmp);
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
				Bitmap bmp = BitmapFactory.decodeResource(getResources(), map_icons[cursor.getInt(columnIndex)], sBFOptions);
				if (bmp != null)
					((ImageView) view).setImageBitmap(bmp);
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.PROFILE_BG)) {
				Bitmap bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas canvas = new Canvas(bmp);
				canvas.drawColor(cursor.getInt(columnIndex));
				((ImageView) view).setImageBitmap(bmp);
				return true;
			} else if (columnIndex == cursor.getColumnIndex(Statuses_styles.FRIEND_BG)) {
				Bitmap bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas canvas = new Canvas(bmp);
				canvas.drawColor(cursor.getInt(columnIndex));
				((ImageView) view).setImageBitmap(bmp);
				return true;
			} else
				return false;
		}
	};
	
	@Override
	protected void onListItemClick(ListView list, final View view, int position, final long id) {
		super.onListItemClick(list, view, position, id);
		final CharSequence[] items = {getString(R.string.re_authenticate), getString(R.string.account_settings), getString(((TextView) view.findViewById(R.id.message)).getText().toString().contains("enabled") ? R.string.disable : R.string.enable)};
		mDialog = (new AlertDialog.Builder(this))
		.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				which++; //fix indexing
				switch (which) {
				case REAUTH_ID:
					// need the account id if reauthenticating
					Cursor c = getContentResolver().query(Accounts.getContentUri(ManageAccounts.this), new String[]{Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(id)}, null);
					if (c.moveToFirst()) {
						int service = c.getInt(0);
						if ((service != SMS) && (service != RSS)) {
							mAddingAccount = true;
							startActivityForResult(Sonet.getPackageIntent(ManageAccounts.this, OAuthLogin.class).putExtra(Accounts.SERVICE, service).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, id), RESULT_REFRESH);
						}
					}
					c.close();
					break;
				case SETTINGS_ID:
					mAddingAccount = true;
					startActivityForResult(Sonet.getPackageIntent(ManageAccounts.this, AccountSettings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, id), RESULT_REFRESH);
					break;
				case ENABLE_ID:
					if (((TextView) view.findViewById(R.id.message)).getText().toString().contains("enabled")) {
						// disable the account, remove settings and statuses
						getContentResolver().delete(Widgets.getContentUri(ManageAccounts.this), Widgets.ACCOUNT + "=? and " + Widgets.WIDGET + "=?", new String[]{Long.toString(id), Integer.toString(mAppWidgetId)});
						getContentResolver().delete(Widget_accounts.getContentUri(ManageAccounts.this), Widget_accounts.ACCOUNT + "=? and " + Widget_accounts.WIDGET + "=?", new String[]{Long.toString(id), Integer.toString(mAppWidgetId)});
						Cursor statuses = getContentResolver().query(Statuses.getContentUri(ManageAccounts.this), new String[]{Statuses._ID}, Statuses.ACCOUNT + "=? and " + Statuses.WIDGET + "=?", new String[]{Long.toString(id), Integer.toString(mAppWidgetId)}, null);
						if (statuses.moveToFirst()) {
							while (!statuses.isAfterLast()) {
								getContentResolver().delete(Status_links.getContentUri(ManageAccounts.this), Status_links.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
								getContentResolver().delete(Status_images.getContentUri(ManageAccounts.this), Status_images.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
								statuses.moveToNext();
							}
						}
						statuses.close();
						getContentResolver().delete(Statuses.getContentUri(ManageAccounts.this), Statuses.ACCOUNT + "=? and " + Statuses.WIDGET + "=?", new String[]{Long.toString(id), Integer.toString(mAppWidgetId)});
						listAccounts();
					} else {
						// enable the account
						ContentValues values = new ContentValues();
						values.put(Widget_accounts.ACCOUNT, id);
						values.put(Widget_accounts.WIDGET, mAppWidgetId);
						ManageAccounts.this.getContentResolver().insert(Widget_accounts.getContentUri(ManageAccounts.this), values);
						listAccounts();
					}
					mUpdateWidget = true;
					listAccounts();
					break;
				}
				dialog.cancel();
			}
		})
		.create();
		mDialog.show();
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
			String accountId = Long.toString(((AdapterContextMenuInfo) item.getMenuInfo()).id);
			getContentResolver().delete(Accounts.getContentUri(this), Accounts._ID + "=?", new String[]{accountId});
			// need to delete the statuses and settings for all accounts
			getContentResolver().delete(Widgets.getContentUri(this), Widgets.ACCOUNT + "=?", new String[]{accountId});
			Cursor statuses = getContentResolver().query(Statuses.getContentUri(this), new String[]{Statuses._ID}, Statuses.ACCOUNT + "=?", new String[]{accountId}, null);
			if (statuses.moveToFirst()) {
				while (!statuses.isAfterLast()) {
					getContentResolver().delete(Status_links.getContentUri(this), Status_links.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
					getContentResolver().delete(Status_images.getContentUri(this), Status_images.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
					statuses.moveToNext();
				}
			}
			statuses.close();
			getContentResolver().delete(Statuses.getContentUri(this), Statuses.ACCOUNT + "=?", new String[]{accountId});
			getContentResolver().delete(Widget_accounts.getContentUri(this), Widget_accounts.ACCOUNT + "=?", new String[]{accountId});
			getContentResolver().delete(Notifications.getContentUri(this), Notifications.ACCOUNT + "=?", new String[]{accountId});
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		listAccounts();
		mAddingAccount = false;
		// check for old settings and offer to load them for restoring a previous widget
		int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
		Cursor widgets = getContentResolver().query(Widgets.getContentUri(this), new String[] {Widgets._ID, Widgets.WIDGET}, Widgets.ACCOUNT + "=?", new String[] { Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);
		if (widgets.moveToFirst()) {
			int[] appWidgetIds = Sonet.getWidgets(getApplicationContext(), AppWidgetManager.getInstance(getApplicationContext()));
			int iwidget = widgets.getColumnIndex(Widgets.WIDGET);
			while (!widgets.isAfterLast()) {
				appWidgetId = widgets.getInt(iwidget);
				if ((appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) && !Sonet.arrayContains(appWidgetIds, appWidgetId))
					break;
				appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
				widgets.moveToNext();
			}
		}
		widgets.close();
		if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			mDialog = (new AlertDialog.Builder(this))
					.setTitle(Integer.toString(appWidgetId))
					.setMessage(R.string.widget_load_msg)
					.setPositiveButton(R.string.load, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							TextView titleView = (TextView) mDialog.findViewById(android.R.id.title);
							int appWidgetId = Integer.parseInt(titleView.getText().toString());
							// load widget
							Log.d(TAG, "load widget: " + appWidgetId + ", into: " + mAppWidgetId);
							ContentValues values = new ContentValues();
							values.put(Widgets.WIDGET, appWidgetId);
							getContentResolver().update(Widgets.getContentUri(ManageAccounts.this), values, Widgets.WIDGET + "=?", new String[] { Integer.toString(mAppWidgetId) });
							values.clear();
							values.put(Widget_accounts.WIDGET, appWidgetId);
							getContentResolver().update(Widget_accounts.getContentUri(ManageAccounts.this), values, Widget_accounts.WIDGET + "=?", new String[] { Integer.toString(mAppWidgetId) });
							values.clear();
							values.put(Statuses.WIDGET, appWidgetId);
							getContentResolver().update(Statuses.getContentUri(ManageAccounts.this), values, Statuses.WIDGET + "=?", new String[] { Integer.toString(mAppWidgetId) });
							listAccounts();
						}})
					.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							TextView titleView = (TextView) mDialog.findViewById(android.R.id.title);
							String appWidgetId = titleView.getText().toString();
							// remove unused widget
							getContentResolver().delete(Widgets.getContentUri(ManageAccounts.this), Widgets.WIDGET + "=?", new String[] {appWidgetId});
							getContentResolver().delete(Widget_accounts.getContentUri(ManageAccounts.this), Widget_accounts.WIDGET + "=?", new String[] {appWidgetId});
							getContentResolver().delete(Statuses.getContentUri(ManageAccounts.this), Statuses.WIDGET + "=?", new String[] {appWidgetId});
						}
					})
					.create();
			mDialog.show();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (!mAddingAccount && mUpdateWidget) {
			(Toast.makeText(getApplicationContext(), getString(R.string.refreshing), Toast.LENGTH_LONG)).show();
			startService(Sonet.getPackageIntent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
		}
		if ((mDialog != null) && mDialog.isShowing())
			mDialog.dismiss();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == RESULT_REFRESH) && (resultCode == RESULT_OK))
			mUpdateWidget = true;
	}

	private void listAccounts() {
		// list all accounts, checking the checkbox if they are enabled for this widget
		// prepend service name to username
		
		Cursor c = this.managedQuery(Accounts_styles.getContentUri(this), new String[]{
				Accounts._ID,
				
				"(case when " + Accounts.SERVICE + "=" + Sonet.TWITTER + " then 'Twitter: ' when "
				+ Accounts.SERVICE + "=" + Sonet.FACEBOOK + " then 'Facebook: ' when "
				+ Accounts.SERVICE + "=" + Sonet.MYSPACE + " then 'MySpace: ' when "
				+ Accounts.SERVICE + "=" + Sonet.LINKEDIN + " then 'LinkedIn: ' when "
				+ Accounts.SERVICE + "=" + Sonet.FOURSQUARE + " then 'Foursquare: ' when "
				+ Accounts.SERVICE + "=" + Sonet.CHATTER + " then 'Chatter: ' when "
				+ Accounts.SERVICE + "=" + Sonet.RSS + " then 'RSS: ' when "
				+ Accounts.SERVICE + "=" + Sonet.IDENTICA + " then 'Identi.ca: ' when "
				+ Accounts.SERVICE + "=" + Sonet.GOOGLEPLUS + " then 'Google+: ' when "
				+ Accounts.SERVICE + "=" + Sonet.PINTEREST + " then 'Pinterest: ' else '' end) as " + Statuses_styles.FRIEND,
				
				"(case when " + Accounts.SERVICE + "=" + Sonet.TWITTER + " then 'Twitter: ' when "
				+ Accounts.SERVICE + "=" + Sonet.FACEBOOK + " then 'Facebook: ' when "
				+ Accounts.SERVICE + "=" + Sonet.MYSPACE + " then 'MySpace: ' when "
				+ Accounts.SERVICE + "=" + Sonet.LINKEDIN + " then 'LinkedIn: ' when "
				+ Accounts.SERVICE + "=" + Sonet.FOURSQUARE + " then 'Foursquare: ' when "
				+ Accounts.SERVICE + "=" + Sonet.CHATTER + " then 'Chatter: ' when "
				+ Accounts.SERVICE + "=" + Sonet.RSS + " then 'RSS: ' when "
				+ Accounts.SERVICE + "=" + Sonet.IDENTICA + " then 'Identi.ca: ' when "
				+ Accounts.SERVICE + "=" + Sonet.GOOGLEPLUS + " then 'Google+: ' when "
				+ Accounts.SERVICE + "=" + Sonet.PINTEREST + " then 'Pinterest: ' else '' end) as " + Statuses_styles.FRIEND + "2",
				
				"(case when (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else 1 end) as " + Statuses_styles.PROFILE,
				
				"(case when (select " + Widget_accounts.WIDGET + " from " + TABLE_WIDGET_ACCOUNTS + " where " + Widget_accounts.WIDGET + "=" + mAppWidgetId + " and " + Widget_accounts.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1) is null then 'this account is disabled for this widget, select to enable' else 'account is enabled for this widget, select to change settings' end) as " + Statuses_styles.MESSAGE,
				
				"(case when (select " + Widget_accounts.WIDGET + " from " + TABLE_WIDGET_ACCOUNTS + " where " + Widget_accounts.WIDGET + "=" + mAppWidgetId + " and " + Widget_accounts.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1) is null then 'this account is disabled for this widget, select to enable' else 'account is enabled for this widget, select to change settings' end) as " + Statuses_styles.MESSAGE + "2",
				
				Accounts.USERNAME + " as " + Statuses_styles.CREATEDTEXT,

				"(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else " + Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR,

				"(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else " + Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR,

				"(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else " + Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR,

				"(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else " + Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE,

				"(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else " + Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE,

				"(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else " + Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE,

				"(case when (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else " + Sonet.default_message_bg_color + " end) as " + Statuses_styles.STATUS_BG,

				Accounts.SERVICE + " as " + Statuses_styles.ICON,
				
				"(case when (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else " + Sonet.default_message_bg_color + " end) as " + Statuses_styles.PROFILE_BG,
				
				"(case when (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
				+ "when (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "when (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
				+ "else " + Sonet.default_friend_bg_color + " end) as " + Statuses_styles.FRIEND_BG
		}, null, null, null);
		SimpleCursorAdapter sca = new SimpleCursorAdapter(ManageAccounts.this, R.layout.widget_item, c, new String[] {Statuses_styles.FRIEND, Statuses_styles.FRIEND + "2", Statuses_styles.MESSAGE, Statuses_styles.MESSAGE + "2", Statuses_styles.STATUS_BG, Statuses_styles.CREATEDTEXT, Statuses_styles.PROFILE, Statuses_styles.ICON, Statuses_styles.PROFILE_BG, Statuses_styles.FRIEND_BG}, new int[] {R.id.friend_bg_clear, R.id.friend, R.id.message_bg_clear, R.id.message, R.id.status_bg, R.id.created, R.id.profile, R.id.icon, R.id.profile_bg, R.id.friend_bg});
		sca.setViewBinder(mViewBinder);
		setListAdapter(sca);
	}

	public void onClick(DialogInterface dialog, int which) {
		mAddingAccount = true;
		startActivityForResult(Sonet.getPackageIntent(this, OAuthLogin.class).putExtra(Accounts.SERVICE, Integer.parseInt(getResources().getStringArray(R.array.service_values)[which])).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID), RESULT_REFRESH);
		dialog.cancel();
	}

}
