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
import static com.piusvelte.sonet.Sonet.TWITTER;
import com.piusvelte.sonet.Sonet.Statuses_styles;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
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

public class SonetCreatePost extends Activity implements OnKeyListener, OnClickListener {
	private static final String TAG = "SonetCreatePost";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private int mService = 0;
	private long mAccount = Sonet.INVALID_ACCOUNT_ID;
	private String mSid;
	private Uri mData;
	private static final int COMMENT = 0;
	private static final int POST = 1;
	private EditText mPost;
	private Button mSubmit;
	private Button mLocation;
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
				// get any comments for this comment
				Cursor status = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.SERVICE}, Statuses_styles._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
				if (status.moveToFirst()) {
					mService = status.getInt(status.getColumnIndex(Statuses_styles.SERVICE));
				}
				status.close();
				break;
			case POST:
				// default to the account passed in, but allow selecting additional accounts
				Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{mData.getLastPathSegment()}, null);
				if (account.moveToFirst()) {
					mService = account.getInt(account.getColumnIndex(Statuses_styles.SERVICE));					
				}
				account.close();
				break;
			}
		}

		setContentView(R.layout.post);
		
		mPost = (EditText) findViewById(R.id.post);
		mSubmit = (Button) findViewById(R.id.submit);
		mLocation = (Button) findViewById(R.id.location);
		
		mPost.setOnKeyListener(this);
		mSubmit.setOnClickListener(this);
		mLocation.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// load the list of comments
		// check and allow like|unlike options for Facebook & Buzz
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// track the post length, if TWITTER and >140, truncate
		String text = mPost.getText().toString();
		if ((mService == TWITTER) && (text.length() > 140)) {
			mPost.setText(text.substring(0, 140));
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		if (v == mLocation) {
			// set the location
		} else if (v == mSubmit) {
			String text = mPost.getText().toString();
			if ((text != null) && (text != "")) {
				// post or comment!
			}
		}
	}

}