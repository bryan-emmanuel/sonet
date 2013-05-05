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

import static com.piusvelte.sonet.core.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_STATUSMOOD_BODY;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_URL_STATUSMOOD;
import static com.piusvelte.sonet.core.SonetTokens.MYSPACE_KEY;
import static com.piusvelte.sonet.core.SonetTokens.MYSPACE_SECRET;

import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;

import android.database.Cursor;
import android.util.Log;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetCreatePost;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetOAuth;
import com.piusvelte.sonet.core.Sonet.Accounts;

public class MySpacePostTask extends PostTask {

	private static final String TAG = "MySpacePostTask";

	public MySpacePostTask(SonetCreatePost activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected Void doInBackground(String... params) {
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			SonetOAuth sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, sonetCrypto.Decrypt(account.getString(1)), sonetCrypto.Decrypt(account.getString(2)));
			HttpPut httpPut = new HttpPut(String.format(MYSPACE_URL_STATUSMOOD, MYSPACE_BASE_URL));
			try {
				httpPut.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOOD_BODY, params[MESSAGE])));
				if (SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(httpPut)) != null)
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.MYSPACE), activity.getString(R.string.success));
				else
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.MYSPACE), activity.getString(R.string.failure));
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, e.toString());
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.MYSPACE), activity.getString(R.string.failure));
			}
		}
		account.close();
		return null;
	}

}
