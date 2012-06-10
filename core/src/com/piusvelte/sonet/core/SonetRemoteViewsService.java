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

import static com.piusvelte.sonet.core.Sonet.getBlob;
import static com.piusvelte.sonet.core.Sonet.sBFOptions;

import com.piusvelte.sonet.core.Sonet.Statuses_styles;
import com.piusvelte.sonet.core.Sonet.Widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class SonetRemoteViewsService extends android.widget.RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new SonetRemoteViewsFactory(this.getApplicationContext(), intent);
	}

}

class SonetRemoteViewsFactory implements android.widget.RemoteViewsService.RemoteViewsFactory {
	private static final String TAG = "SonetRemoteViewsFactory";
	private Context mContext;
	private Cursor mCursor;
	private int mAppWidgetId;
	private boolean mDisplay_profile;

	public SonetRemoteViewsFactory(Context context, Intent intent) {
		mContext = context;
		mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		mDisplay_profile = intent.getBooleanExtra(Widgets.DISPLAY_PROFILE, true);
	}

	@Override
	public int getCount() {
		if (mCursor != null) {
			return mCursor.getCount();
		} else {
			return 0;
		}
	}

	@Override
	public long getItemId(int position) {
		if (mCursor.moveToPosition(position)) {
			return mCursor.getLong(mCursor.getColumnIndex(Statuses_styles._ID));
		}
		return position;
	}

	@Override
	public RemoteViews getLoadingView() {
		return null;
	}

	@Override
	public RemoteViews getViewAt(int position) {
		// load the item
		RemoteViews views;
		if (mCursor.moveToPosition(position)) {
			int friend_color = mCursor.getInt(6),
					created_color = mCursor.getInt(7),
					friend_textsize = mCursor.getInt(9),
					created_textsize = mCursor.getInt(10),
					messages_color = mCursor.getInt(5),
					messages_textsize = mCursor.getInt(8);
			views = new RemoteViews(mContext.getPackageName(), mDisplay_profile ? R.layout.widget_item : R.layout.widget_item_noprofile);
			// set icons
			byte[] icon = mCursor.getBlob(12);
			if (icon != null) {
				Bitmap iconbmp = BitmapFactory.decodeByteArray(icon, 0, icon.length, sBFOptions);
				if (iconbmp != null) {
					views.setImageViewBitmap(R.id.icon, iconbmp);
				}
			}
			views.setTextViewText(R.id.friend_bg_clear, mCursor.getString(1));
			views.setFloat(R.id.friend_bg_clear, "setTextSize", friend_textsize);
			views.setTextViewText(R.id.message_bg_clear, mCursor.getString(3));
			views.setFloat(R.id.message_bg_clear, "setTextSize", messages_textsize);
			// set messages background
			byte[] status_bg = mCursor.getBlob(11);
			if (status_bg != null) {
				Bitmap status_bgbmp = BitmapFactory.decodeByteArray(status_bg, 0, status_bg.length, sBFOptions);
				if (status_bgbmp != null) {
					views.setImageViewBitmap(R.id.status_bg, status_bgbmp);
				}
			}
			views.setTextViewText(R.id.message, mCursor.getString(3));
			views.setTextColor(R.id.message, messages_color);
			views.setFloat(R.id.message, "setTextSize", messages_textsize);

			// Set the click intent so that we can handle it and show a toast message
			final Intent fillInIntent = new Intent();
			final Bundle extras = new Bundle();
			extras.putString(Sonet.Status_links.STATUS_ID, Long.toString(mCursor.getLong(0)));
			fillInIntent.putExtras(extras);
			views.setOnClickFillInIntent(R.id.item, fillInIntent);

			byte[] friend_bg = mCursor.getBlob(14);
			if (friend_bg != null) {
				Bitmap friendbmp = BitmapFactory.decodeByteArray(friend_bg, 0, friend_bg.length, sBFOptions);
				if (friendbmp != null) {
					views.setImageViewBitmap(R.id.friend_bg, friendbmp);
				}
			}

			views.setTextViewText(R.id.friend, mCursor.getString(1));
			views.setTextColor(R.id.friend, friend_color);
			views.setFloat(R.id.friend, "setTextSize", friend_textsize);
			views.setTextViewText(R.id.created, mCursor.getString(4));
			views.setTextColor(R.id.created, created_color);
			views.setFloat(R.id.created, "setTextSize", created_textsize);
			byte[] image_bg = mCursor.getBlob(15);
			if (image_bg != null) {
				Bitmap image_bgbmp = BitmapFactory.decodeByteArray(image_bg, 0, image_bg.length, sBFOptions);
				if (image_bgbmp != null) {
					views.setImageViewBitmap(R.id.image_clear, image_bgbmp);
					byte[] image = mCursor.getBlob(16);
					if (image != null) {
						Bitmap imagebmp = BitmapFactory.decodeByteArray(image, 0, image.length, sBFOptions);
						if (imagebmp != null) {
							views.setImageViewBitmap(R.id.image, imagebmp);
						}
					}
				}
			}
			byte[] profile_bg = mCursor.getBlob(13);
			if (mDisplay_profile) {
				if (profile_bg != null) {
					Bitmap profilebmp = BitmapFactory.decodeByteArray(profile_bg, 0, profile_bg.length, sBFOptions);
					if (profilebmp != null) {
						views.setImageViewBitmap(R.id.profile_bg, profilebmp);
					}
				}
			}
			byte[] profile = mCursor.getBlob(2);
			if (mDisplay_profile) {
				if (profile == null) {
					profile = getBlob(mContext.getResources(), R.drawable.ic_contact_picture);
				}
				Bitmap profilebmp = BitmapFactory.decodeByteArray(profile, 0, profile.length, sBFOptions);
				if (profilebmp != null) {
					views.setImageViewBitmap(R.id.profile, profilebmp);
				}
			}
		} else {
			views = null;
		}

		return views;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public void onDataSetChanged() {
		// Refresh the cursor
		if (mCursor != null) {
			mCursor.close();
		}
		mCursor = mContext.getContentResolver().query(Uri.withAppendedPath(Statuses_styles.getContentUri(mContext), Integer.toString(mAppWidgetId)), new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON, Statuses_styles.PROFILE_BG, Statuses_styles.FRIEND_BG, Statuses_styles.IMAGE_BG, Statuses_styles.IMAGE}, null, null, Statuses_styles.CREATED + " DESC");
	}

	@Override
	public void onDestroy() {
		if (mCursor != null) {
			mCursor.close();
		}
	}

}
