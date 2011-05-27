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

import static com.piusvelte.sonet.Sonet.ACCOUNTS_QUERY;
import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class StatusDialog extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private long mAccount = Sonet.INVALID_ACCOUNT_ID;
	private Uri mData;
	private static final int COMMENT = 0;
	private static final int POST = COMMENT + 1;
	private static final int SETTINGS = POST + 1;
	private static final int REFRESH = SETTINGS + 1;
	private int[] mAppWidgetIds;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ((getIntent() != null) && (getIntent().getData() != null)) {
			mData = getIntent().getData();
			Cursor c = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.WIDGET, Statuses_styles.ACCOUNT, Statuses_styles.SID, Statuses_styles.ESID}, Statuses_styles._ID + "=?", new String[] {mData.getLastPathSegment()}, null);
			if (c.moveToFirst()) {
				mAppWidgetId = c.getInt(c.getColumnIndex(Statuses_styles.WIDGET));
				mAccount = c.getLong(c.getColumnIndex(Statuses_styles.ACCOUNT));
			} else {
				mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
			}
			c.close();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// offer options for Comment, Post, Settings and Refresh
		// loading the likes/retweet and other options takes too long, so load them in the SonetCreatePost.class
		(new AlertDialog.Builder(this))
		.setItems(new String[]{getString(R.string.comment), getString(R.string.button_post), getString(R.string.settings), getString(R.string.button_refresh)}, this)
		.setCancelable(true)
		.setOnCancelListener(this)
		.show();
	}

	@Override
	public void onClick(final DialogInterface dialog, int which) {
		switch (which) {
		case COMMENT:
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				startActivity(new Intent(this, SonetComments.class).setData(mData));
			} else {
				(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
			}
			dialog.cancel();
			break;
		case POST:
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {				
				startActivity(new Intent(this, SonetCreatePost.class).setData(Uri.withAppendedPath(Accounts.CONTENT_URI, Long.toString(mAccount))));
				dialog.cancel();
			} else {
				// no widget sent in, dialog to select one
				String[] widgets = getAllWidgets();
				if (widgets.length > 0) {
					(new AlertDialog.Builder(this))
					.setItems(widgets, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// no account, dialog to select one
							Cursor c = StatusDialog.this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY}, Accounts.WIDGET + "=?", new String[]{Integer.toString(mAppWidgetIds[arg1])}, null);
							if (c.moveToFirst()) {
								int iid = c.getColumnIndex(Accounts._ID),
								iusername = c.getColumnIndex(Accounts.USERNAME),
								i = 0;
								final long[] accountIndexes = new long[c.getCount()];
								final String[] accounts = new String[c.getCount()];
								while (!c.isAfterLast()) {
									long id = c.getLong(iid);
									accountIndexes[i] = id;
									accounts[i++] = c.getString(iusername);
									c.moveToNext();
								}
								AlertDialog.Builder accountsDialog = new AlertDialog.Builder(StatusDialog.this);
								accountsDialog.setTitle(R.string.accounts)
								.setSingleChoiceItems(accounts, -1, new OnClickListener() {
									@Override
									public void onClick(DialogInterface arg0, int which) {
										startActivity(new Intent(StatusDialog.this, SonetCreatePost.class).setData(Uri.withAppendedPath(Accounts.CONTENT_URI, Long.toString(accountIndexes[which]))));
										arg0.cancel();
									}
								})
								.setCancelable(true)
								.setOnCancelListener(new OnCancelListener() {
									@Override
									public void onCancel(DialogInterface arg0) {
										dialog.cancel();
									}
								})
								.show();
							} else {
								(Toast.makeText(StatusDialog.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
								dialog.cancel();
							}
							c.close();
						}					
					})
					.setCancelable(true)
					.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface arg0) {
							dialog.cancel();
						}						
					})
					.show();
				} else {
					(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
					dialog.cancel();
				}
			}
			break;
		case SETTINGS:
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
				dialog.cancel();
			} else {
				// no widget sent in, dialog to select one
				String[] widgets = getAllWidgets();
				if (widgets.length > 0) {
					(new AlertDialog.Builder(this))
					.setItems(widgets, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							startActivity(new Intent(StatusDialog.this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[arg1]));
							arg0.cancel();
						}					
					})
					.setCancelable(true)
					.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface arg0) {
							dialog.cancel();
						}
					})
					.show();
				} else {
					(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
					dialog.cancel();
				}
			}
			break;
		case REFRESH:
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
				dialog.cancel();
			} else {
				// no widget sent in, dialog to select one
				String[] widgets = getAllWidgets();
				if (widgets.length > 0) {
					(new AlertDialog.Builder(this))
					.setItems(widgets, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							startService(new Intent(StatusDialog.this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetIds[arg1]}));
							arg0.cancel();
						}					
					})
					.setPositiveButton(R.string.refreshall, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int which) {
							// refresh all
							startService(new Intent(StatusDialog.this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
							arg0.cancel();
						}
					})
					.setCancelable(true)
					.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface arg0) {
							dialog.cancel();
						}						
					})
					.show();
				} else {
					dialog.cancel();
				}
			}
			break;
		}
	}

	private String[] getAllWidgets() {
		mAppWidgetIds = new int[0];
		// validate appwidgetids from appwidgetmanager
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		mAppWidgetIds = Sonet.arrayCat(
				Sonet.arrayCat(appWidgetManager.getAppWidgetIds(new ComponentName(
						this, SonetWidget_4x2.class)),
						appWidgetManager.getAppWidgetIds(new ComponentName(
								this, SonetWidget_4x3.class))),
								appWidgetManager.getAppWidgetIds(new ComponentName(this,
										SonetWidget_4x4.class)));
		int[] removeAppWidgets = new int[0];
		this.getContentResolver().delete(Widgets.CONTENT_URI,
				Widgets.WIDGET + "=?", new String[] { "" });
		this.getContentResolver().delete(Accounts.CONTENT_URI,
				Accounts.WIDGET + "=?", new String[] { "" });
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
				this.getContentResolver().delete(Accounts.CONTENT_URI, Accounts.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
				this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
			}
		}
		String[] widgetsOptions = new String[mAppWidgetIds.length];
		for (int i = 0; i < mAppWidgetIds.length; i++) {
			AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(mAppWidgetIds[i]);
			String providerName = info.provider.getClassName();
			widgetsOptions[i] = Integer.toString(mAppWidgetIds[i])
			+ " ("
			+ (providerName == SonetWidget_4x2.class.getName() ? "4x2"
					: providerName == SonetWidget_4x3.class
					.getName() ? "4x3" : "4x4") + ")";
		}
		return widgetsOptions;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}	

}
