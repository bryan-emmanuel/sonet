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
package com.piusvelte.sonet.core.tasks;

import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_CHECKIN;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_CHECKIN_NO_SHOUT;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_CHECKIN_NO_VENUE;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.client.methods.HttpPost;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetCreatePost;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.Sonet.Accounts;

import android.database.Cursor;
import android.util.Log;

public class FoursquarePostTask extends PostTask {
	
	private static final String TAG = "FoursquarePostTask";

	public FoursquarePostTask(SonetCreatePost activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected Void doInBackground(String... params) {
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			HttpPost httpPost = null;
			if (params[LOCATION] != null) {
				if (params[MESSAGE] != null) {
					try {
						httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN, FOURSQUARE_BASE_URL, params[LOCATION], URLEncoder.encode(params[MESSAGE], "UTF-8"), params[LATITUDE], params[LONGITUDE], sonetCrypto.Decrypt(account.getString(1))));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
						Log.e(TAG, e.getMessage());
						publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FOURSQUARE), activity.getString(R.string.failure));
					}
				} else
					httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_SHOUT, FOURSQUARE_BASE_URL, params[LOCATION], params[LATITUDE], params[LONGITUDE], sonetCrypto.Decrypt(account.getString(1))));
			} else {
				try {
					httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_VENUE, FOURSQUARE_BASE_URL, URLEncoder.encode(params[MESSAGE], "UTF-8"), sonetCrypto.Decrypt(account.getString(1))));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					Log.e(TAG, e.getMessage());
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FOURSQUARE), activity.getString(R.string.failure));
				}
			}
			if ((httpPost != null) && (SonetHttpClient.httpResponse(httpClient, httpPost) != null))
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FOURSQUARE), activity.getString(R.string.success));
			else
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FOURSQUARE), activity.getString(R.string.failure));
		}
		account.close();
		return null;
	}

}
