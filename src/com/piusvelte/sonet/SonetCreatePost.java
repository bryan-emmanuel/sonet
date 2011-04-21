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

import com.piusvelte.sonet.Sonet.Accounts;

import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.SALESFORCE;
import static com.piusvelte.sonet.Sonet.TWITTER;
import com.piusvelte.sonet.Sonet.Statuses_styles;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

public class SonetCreatePost extends Activity implements OnKeyListener, OnClickListener, OnCancelListener, android.content.DialogInterface.OnClickListener {
	private static final String TAG = "SonetCreatePost";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private int mService = 0;
	private int mAccount = (int) Sonet.INVALID_ACCOUNT_ID;
	private int[] mAccountsToPost;
	private String mSid;
	private Uri mData;
	private static final int COMMENT = 0;
	private static final int POST = 1;
	private EditText mPost;
	private Button mSend;
	private Button mLocation;
	private Button mComments;
	private Button mAccounts;
	private ProgressDialog mLoadingDialog;
	/* buzz
POST https://www.googleapis.com/buzz/v1/activities/@me/@self?key=INSERT-YOUR-KEY&alt=json
Authorization: /* auth token here *\/
Content-Type: application/json

{
  "data": {
    "object": {
      "type": "note",
      "content": "Hey, this is my first Buzz Post!"
    }
  }
}
	 */
	/* buzz comment
POST https://www.googleapis.com/buzz/v1/activities/ted/@self/tag:google.com,2009:buzz:z13wx3b/@comments?key=INSERT-YOUR-KEY&alt=json
Authorization: /* auth token here *\/
Content-Type: application/json

{
  "data": {
    "content": "Now the whole gang is here"
  }
} 
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// allow posting to multiple services if an account is defined
		// allow selecting which accounts to use
		// get existing comments, allow liking|unliking those comments
		setContentView(R.layout.post);

		mPost = (EditText) findViewById(R.id.post);
		mSend = (Button) findViewById(R.id.send);
		mLocation = (Button) findViewById(R.id.location);
		mComments = (Button) findViewById(R.id.comments);
		mAccounts = (Button) findViewById(R.id.accounts);
		Intent intent = getIntent();
		if (intent != null) {
			mData = intent.getData();
			// if the uri is Statuses_styles, then this is a comment or reply
			// if the uri is Accounts, then this is a post or tweet
			UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
			uriMatcher.addURI(SonetProvider.AUTHORITY, Statuses_styles.CONTENT_URI.toString() + "/*", COMMENT);
			uriMatcher.addURI(SonetProvider.AUTHORITY, Accounts.CONTENT_URI.toString() + "/*", POST);
			switch (uriMatcher.match(mData)) {
			case COMMENT:
				Cursor status = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.SERVICE}, Statuses_styles._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
				if (status.moveToFirst()) {
					mService = status.getInt(status.getColumnIndex(Statuses_styles.SERVICE));
				}
				status.close();
				// allow comment viewing for services that having commenting
				if (mService != TWITTER) {
					mComments.setEnabled(true);
					mComments.setOnClickListener(this);					
				} else {
					// reply
					mLoadingDialog = new ProgressDialog(this);
					mLoadingDialog.setMessage(getString(R.string.loading));
					mLoadingDialog.setCancelable(true);
					mLoadingDialog.setOnCancelListener(this);
					mLoadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), this);
					// get the user's name @username
					mLoadingDialog.dismiss();
				}
				break;
			case POST:
				// default to the account passed in, but allow selecting additional accounts
				Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
				if (account.moveToFirst()) {
					mAccount = account.getInt(account.getColumnIndex(Accounts._ID));
					mService = account.getInt(account.getColumnIndex(Accounts.SERVICE));
					this.mAccountsToPost = Sonet.arrayPush(mAccountsToPost, mAccount);
				}
				account.close();
				mAccounts.setEnabled(true);
				mAccounts.setOnClickListener(this);
				break;
			}
		}
		mPost.setOnKeyListener(this);
		mSend.setOnClickListener(this);
		mLocation.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// check and allow like|unlike options for Facebook & Buzz
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// track the post length, if TWITTER and >140, truncate
		String text = mPost.getText().toString();
		if ((mService == TWITTER) && (text.length() > 140)) mPost.setText(text.substring(0, 140));
		return false;
	}

	@Override
	public void onClick(View v) {
		if (v == mLocation) {
			// set the location
		} else if (v == mSend) {
			String text = mPost.getText().toString();
			if ((text != null) && (text != "")) {
				// post or comment!
				mSend.setEnabled(false);
				mSend.setText(R.string.sending);
			}
		} else if (v == mComments) this.startActivity(new Intent(this, SonetComments.class).setData(mData));
		else if (v == mAccounts) {
			//TODO: dialog to allow selection of accounts to post to, defaulting the replying/commenting account if this is a reply/comment
			Cursor c = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID,
					"(case when " + Accounts.SERVICE + "='" + TWITTER + "' then 'Twitter: ' when "
					+ Accounts.SERVICE + "='" + FACEBOOK + "' then 'Facebook: ' when "
					+ Accounts.SERVICE + "='" + MYSPACE + "' then 'MySpace: ' when "
					+ Accounts.SERVICE + "='" + BUZZ + "' then 'Buzz: ' when "
					+ Accounts.SERVICE + "='" + LINKEDIN + "' then 'LinkedIn: ' when "
					+ Accounts.SERVICE + "='" + SALESFORCE + "' then 'Salesforce: ' when "
					+ Accounts.SERVICE + "='" + FOURSQUARE + "' then 'Foursquare: ' else '' end)||" + Accounts.USERNAME + " as " + Accounts.USERNAME, Accounts.SERVICE}, Accounts.WIDGET + "=?", new String[]{Integer.toString(mAppWidgetId)}, null);
			if (c.moveToFirst()) {
				int iid = c.getColumnIndex(Accounts._ID),
				iusername = c.getColumnIndex(Accounts.USERNAME),
				i = -1;
				final int[] accountIndexes = new int[c.getCount()];
				final String[] accounts = new String[c.getCount()];
				final boolean[] defaults = new boolean[c.getCount()];
				while (!c.isAfterLast()) {
					accountIndexes[i++] = c.getInt(iid);
					accounts[i] = c.getString(iusername);
					defaults[i] = c.getInt(iid) == mAccount;
					c.moveToNext();
				}
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle(R.string.accounts)
				.setMultiChoiceItems(accounts, defaults, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						mAccountsToPost = isChecked ? Sonet.arrayPush(mAccountsToPost, accountIndexes[which]) : Sonet.arrayRemove(mAccountsToPost, accountIndexes[which]);
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.show();
			}
			c.close();
		}
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		finish();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}

}