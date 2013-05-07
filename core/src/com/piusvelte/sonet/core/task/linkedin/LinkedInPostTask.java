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
package com.piusvelte.sonet.core.task.linkedin;

import static com.piusvelte.sonet.core.Sonet.LINKEDIN_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_POST;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_POST_BODY;
import static com.piusvelte.sonet.core.SonetTokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.core.SonetTokens.LINKEDIN_SECRET;

import java.io.IOException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;

import android.database.Cursor;
import android.util.Log;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetOAuth;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.activity.SonetCreatePost;
import com.piusvelte.sonet.core.task.PostTask;

public class LinkedInPostTask extends PostTask {

	private static final String TAG = "LinkedInPostTask";

	public LinkedInPostTask(SonetCreatePost activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected Void doInBackground(String... params) {
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			SonetOAuth sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, sonetCrypto.Decrypt(account.getString(1)), sonetCrypto.Decrypt(account.getString(2)));
			try {
				HttpPost httpPost = new HttpPost(String.format(LINKEDIN_POST, LINKEDIN_BASE_URL));
				httpPost.setEntity(new StringEntity(String.format(LINKEDIN_POST_BODY, "", params[MESSAGE])));
				httpPost.addHeader(new BasicHeader("Content-Type", "application/xml"));
				if (SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(httpPost)) != null)
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.LINKEDIN), activity.getString(R.string.success));
				else
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.LINKEDIN), activity.getString(R.string.failure));
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.LINKEDIN), activity.getString(R.string.failure));
			}
		} else
			publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.LINKEDIN), activity.getString(R.string.failure));
		account.close();
		return null;
	}

}
