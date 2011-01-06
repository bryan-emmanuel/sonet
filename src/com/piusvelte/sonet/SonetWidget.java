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
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.Sonet.ACTION_DELETE;
import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.BoundRemoteViews;
import mobi.intuitit.android.widget.SimpleRemoteViews;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

public class SonetWidget extends AppWidgetProvider {
	private static final String TAG = "SonetWidget";
    private static final String ACTION_MAKE_SCROLLABLE = "com.piusvelte.sonet.Sonet.MAKE_SCROLLABLE";
    private static final String ACTION_ONCLICK = "com.piusvelte.sonet.Sonet.ONCLICK";
	private int mAppWidgetId;
	private Context mContext;
	private int mStatusId;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		context.startService(new Intent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (action.equals(ACTION_REFRESH)) {
			int[] appWidgetIds;
			if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) appWidgetIds = new int[]{intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)};
			else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			else appWidgetIds = new int[]{AppWidgetManager.INVALID_APPWIDGET_ID};
			context.startService(new Intent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));	
		} else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) onDeleted(context, new int[]{appWidgetId});
			else super.onReceive(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_READY)) {
			if (intent.getExtras().getInt(LauncherIntent.Extra.EXTRA_API_VERSION, 1) >= 2) {
				mAppWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
				mContext = context;
				context.startService(new Intent(context, BuildScrollableWidget.class).setAction(ACTION_MAKE_SCROLLABLE));
			}
		} else if (Sonet.ACTION_BUILD_SCROLL.equals(action)) {
			mAppWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			mContext = context;
			context.startService(new Intent(context, BuildScrollableWidget.class).setAction(ACTION_MAKE_SCROLLABLE));
		}
		else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_FINISH)) {
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_VIEW_CLICK)) {
			// onClickListener
			onClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Error.ERROR_SCROLL_CURSOR)) {
			// An error occured
			Log.d(TAG, intent.getStringExtra(LauncherIntent.Extra.EXTRA_ERROR_MESSAGE) + "");
		} else super.onReceive(context, intent);
	}

	@Override
	public final void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		context.startService(new Intent(context, SonetService.class).setAction(ACTION_DELETE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
	}

	private void onClick(Context context, Intent intent) {
		mContext = context;
		mAppWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		mStatusId = intent.getIntExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, -1);
		context.startService(new Intent(context, BuildScrollableWidget.class).setAction(ACTION_ONCLICK));
	}

	public class BuildScrollableWidget extends Service implements ServiceConnection {

		private ISonetService mSonetService;
		private ISonetUI.Stub mSonetUI = new ISonetUI.Stub() {

			@Override
			public void setDefaultSettings(int interval_value,
					int buttons_bg_color_value, int buttons_color_value,
					int buttons_textsize_value, int messages_bg_color_value,
					int messages_color_value, int messages_textsize_value,
					int friend_color_value, int friend_textsize_value,
					int created_color_value, int created_textsize_value,
					boolean hasButtons, boolean time24hr)
			throws RemoteException {
			}

			@Override
			public void listAccounts() throws RemoteException {
			}

			@Override
			public void getAuth(int service) throws RemoteException {
			}

			@Override
			public void getTimezone(int account) throws RemoteException {
			}

			@Override
			public void buildScrollableWidget(int messages_color,
					int friend_color, int created_color, int friend_textsize,
					int created_textsize, int messages_textsize)
			throws RemoteException {

				String appWidgetUri = SonetProvider.CONTENT_URI.buildUpon().appendEncodedPath(Integer.toString(mAppWidgetId)).toString();
				Intent replaceDummy = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);
				// Put widget info
				replaceDummy.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);
				replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.messages);

				SimpleRemoteViews listView = new SimpleRemoteViews(R.layout.widget_listview);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_REMOTEVIEWS, listView);

				BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.widget_item);

				itemViews.setBoundBitmap(R.id.profile, "setImageBitmap", SonetProvider.SonetProviderColumns.profile.ordinal(), 0);
				itemViews.setBoundCharSequence(R.id.friend, "setText", SonetProvider.SonetProviderColumns.friend.ordinal(), 0);
				itemViews.setBoundCharSequence(R.id.created, "setText", SonetProvider.SonetProviderColumns.createdtext.ordinal(), 0);
				itemViews.setBoundCharSequence(R.id.message, "setText", SonetProvider.SonetProviderColumns.message.ordinal(), 0);

				itemViews.setTextColor(R.id.friend, friend_color);
				itemViews.setTextColor(R.id.created, created_color);
				itemViews.setTextColor(R.id.message, messages_color);
				itemViews.setFloat(R.id.friend, "setTextSize", friend_textsize);
				itemViews.setFloat(R.id.created, "setTextSize", created_textsize);
				itemViews.setFloat(R.id.message, "setTextSize", messages_textsize);

				Intent i= new Intent(mContext, mContext.getClass())
				.setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
				.setData(Uri.parse(appWidgetUri))
				.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
				PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, i, 0);
				itemViews.SetBoundOnClickIntent(R.id.item, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.SonetProviderColumns._id.ordinal());

				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);

				String whereClause = WIDGET + "=" + Uri.parse(appWidgetUri).getLastPathSegment();
				String orderBy = CREATED + " desc";
				String[] selectionArgs = null;

				// Put the data uri in as a string. Do not use setData, Home++ does not
				// have a filter for that
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, appWidgetUri);

				// Other arguments for managed query
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, SonetProvider.PROJECTION_APPWIDGETS);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, orderBy);

				sendBroadcast(replaceDummy);
			}

			@Override
			public void widgetOnClick(boolean hasbuttons, int service,
					String link) throws RemoteException {
				if (link != null) mContext.startActivity(hasbuttons ? new Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) : new Intent(mContext, StatusDialog.class).setAction(mAppWidgetId+"`"+service+"`"+link).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			}
		};

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			onStart(intent, startId);
			return START_STICKY;
		}

		@Override
		public void onStart(Intent intent, int startId) {
			super.onStart(intent, startId);
			bindService(new Intent(this, SonetService.class), this, BIND_AUTO_CREATE);
			if (intent.getAction().equals(ACTION_MAKE_SCROLLABLE)) {
				try {
					mSonetService.getWidgetSettings(mAppWidgetId);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (intent.getAction().equals(ACTION_ONCLICK)) {
				try {
					mSonetService.widgetOnClick(mAppWidgetId, mStatusId);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}

		@Override
		public void onDestroy() {
			unbindService(this);
			super.onDestroy();
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mSonetService = ISonetService.Stub.asInterface((IBinder) service);
			if (mSonetUI != null) {
				try {
					mSonetService.setCallback(mSonetUI.asBinder());
				} catch (RemoteException e) {}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mSonetService = null;
		}

		@Override
		public IBinder onBind(Intent intent) {
			// TODO Auto-generated method stub
			return null;
		}

	}

}