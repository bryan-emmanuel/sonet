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

import java.io.IOException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.client.ClientProtocolException;

import static com.piusvelte.sonet.Sonet.TOKEN;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.Sonet.MYSPACE;

import static com.piusvelte.sonet.Tokens.TWITTER_KEY;
import static com.piusvelte.sonet.Tokens.TWITTER_SECRET;
import static com.piusvelte.sonet.Sonet.TWITTER_RETWEET;

import static com.piusvelte.sonet.Sonet.BUZZ;

import static com.piusvelte.sonet.Sonet.FOURSQUARE;

import static com.piusvelte.sonet.Sonet.LINKEDIN;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses_styles;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class StatusDialog extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
	private static final String TAG = "StatusDialog";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	// options are:
	// Like|Unlike|Retweet
	// Comment|Reply
	// Post|Tweet
	// Settings
	// Refresh
	private static final int LIKE = 0;
	private static final int COMMENT = 1;
	private static final int POST = 2;
	private static final int SETTINGS = 3;
	private static final int REFRESH = 4;
	private int mService = 0;
	private long mAccount = Sonet.INVALID_ACCOUNT_ID;
	private String mSid;
	private Uri mData;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null) {
			mData = intent.getData();
			Cursor c = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.WIDGET, Statuses_styles.SERVICE, Statuses_styles.ACCOUNT, Statuses_styles.SID}, Statuses_styles._ID + "=?", new String[] {mData.getLastPathSegment()}, null);
			if (c.moveToFirst()) {
				mAppWidgetId = c.getInt(c.getColumnIndex(Statuses_styles.WIDGET));
				mService = c.getInt(c.getColumnIndex(Statuses_styles.SERVICE));
				mAccount = c.getLong(c.getColumnIndex(Statuses_styles.ACCOUNT));
				mSid = c.getString(c.getColumnIndex(Statuses_styles.SID));
			}
			c.close();
		}
		String[] items;
		switch (mService) {
		case TWITTER:
			items = new String[]{getString(R.string.retweet), getString(R.string.reply), getString(R.string.tweet), getString(R.string.settings), getString(R.string.button_refresh)};
			break;
		case FACEBOOK:
			//TODO: check if user has liked this comment
			items = new String[]{getString(R.string.like), getString(R.string.comment), getString(R.string.button_post), getString(R.string.settings), getString(R.string.button_refresh)};
			break;
		default:
			items = new String[]{getString(R.string.comment), getString(R.string.button_post), getString(R.string.settings), getString(R.string.button_refresh)};
			break;
		}
		(new AlertDialog.Builder(this))
		.setItems(items, this)
		.setCancelable(true)
		.setOnCancelListener(this)
		.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case LIKE:
			switch (mService) {
			case TWITTER:
				if (mSid != null) {
					Cursor c = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (c.moveToFirst()) {
						SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, c.getString(c.getColumnIndex(Accounts.TOKEN)), c.getString(c.getColumnIndex(Accounts.SECRET)));
						try {
							String response = sonetOAuth.httpGet(String.format(TWITTER_RETWEET, mSid));
						} catch (ClientProtocolException e) {
							Log.e(TAG, "unexpected:" + e);
						} catch (OAuthMessageSignerException e) {
							Log.e(TAG, "unexpected:" + e);
						} catch (OAuthExpectationFailedException e) {
							Log.e(TAG, "unexpected:" + e);
						} catch (OAuthCommunicationException e) {
							Log.e(TAG, "unexpected:" + e);
						} catch (IOException e) {
							Log.e(TAG, "unexpected:" + e);
						}
					}
					c.close();
				}
				break;
			case FACEBOOK:
				//TODO: FB Like
				// Like POST https://graph.facebook.com/[COMMENT ID]/likes
				// Unlike DELETE https://graph.facebook.com/[COMMENT ID]/likes
				if (mSid != null) {
					Cursor c = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
					if (c.moveToFirst()) {
						String response = Sonet.httpPost(String.format(FACEBOOK_LIKES, mSid, TOKEN, c.getString(c.getColumnIndex(Accounts.TOKEN))));
					}
					c.close();
				}
				break;
			default:
				startActivity(new Intent(this, SonetCreatePost.class).setData(mData));
				break;
			}
			break;
		case COMMENT:
			switch (mService) {
			case TWITTER:
				startActivity(new Intent(this, SonetCreatePost.class).setData(mData));
				break;
			case FACEBOOK:
				startActivity(new Intent(this, SonetCreatePost.class).setData(mData));
				break;
			default:
				startActivity(new Intent(this, SonetCreatePost.class));
				break;
			}
			break;
		case POST:
			switch (mService) {
			case TWITTER:
				startActivity(new Intent(this, SonetCreatePost.class));
				break;
			case FACEBOOK:
				startActivity(new Intent(this, SonetCreatePost.class));
				break;
			default:
				startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
				break;
			}
			break;
		case SETTINGS:
			switch (mService) {
			case TWITTER:
				startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
				break;
			case FACEBOOK:
				startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
				break;
			default:
				startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
				break;
			} 
			break;
		case REFRESH:
			startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
			break;
		}
		dialog.cancel();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}	

}
