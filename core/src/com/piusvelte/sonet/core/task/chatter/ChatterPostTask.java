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
package com.piusvelte.sonet.core.task.chatter;

import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_ACCESS;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_POST;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.SonetTokens.CHATTER_KEY;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.activity.SonetCreatePost;
import com.piusvelte.sonet.core.task.PostTask;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class ChatterPostTask extends PostTask {
	
	private static final String TAG = "ChatterPostTask";

	public ChatterPostTask(SonetCreatePost activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected Void doInBackground(String... params) {
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			String response = SonetHttpClient.httpResponse(httpClient, new HttpPost(String.format(CHATTER_URL_ACCESS, CHATTER_KEY, sonetCrypto.Decrypt(account.getString(1)))));
			if (response != null) {
				try {
					JSONObject jobj = new JSONObject(response);
					if (jobj.has("instance_url") && jobj.has(Saccess_token)) {
						HttpPost httpPost = new HttpPost(String.format(CHATTER_URL_POST, jobj.getString("instance_url"), Uri.encode(params[MESSAGE])));
						httpPost.setHeader("Authorization", "OAuth " + jobj.getString(Saccess_token));
						if (SonetHttpClient.httpResponse(httpClient, httpPost) != null)
							publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.success));
						else
							publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.failure));
					}
				} catch (JSONException e) {
					Log.e(TAG, e.getMessage());
				}
			} else
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.failure));
		} else
			publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.failure));
		account.close();
		return null;
	}

}
