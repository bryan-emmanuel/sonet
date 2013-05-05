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

import static com.piusvelte.sonet.core.Sonet.Sfull_name;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.Splaces;
import static com.piusvelte.sonet.core.Sonet.Sresult;
import static com.piusvelte.sonet.core.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.TWITTER_SEARCH;
import static com.piusvelte.sonet.core.SonetTokens.TWITTER_KEY;
import static com.piusvelte.sonet.core.SonetTokens.TWITTER_SECRET;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;

import com.piusvelte.sonet.core.SonetCreatePost;
import com.piusvelte.sonet.core.SonetCrypto;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetOAuth;
import com.piusvelte.sonet.core.Sonet.Accounts;

public class TwitterLocationTask extends LocationTask {

	public TwitterLocationTask(SonetCreatePost activity, long accountId) {
		super(activity, accountId);
	}
	
	@Override
	protected String doInBackground(String... params) {
		String response = null;
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			HttpClient httpClient = SonetHttpClient.getThreadSafeClient(activity.getApplicationContext());
			SonetCrypto sonetCrypto = SonetCrypto.getInstance(activity.getApplicationContext());
			SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, sonetCrypto.Decrypt(account.getString(1)), sonetCrypto.Decrypt(account.getString(2)));
			response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(TWITTER_SEARCH, TWITTER_BASE_URL, params[0], params[1]))));
		}
		account.close();
		return response;
	}

	@Override
	protected void onPostExecute(String response) {
		try {
			JSONArray places = new JSONObject(response).getJSONObject(Sresult).getJSONArray(Splaces);
			final String placesNames[] = new String[places.length()];
			final String placesIds[] = new String[places.length()];
			for (int i = 0, i2 = places.length(); i < i2; i++) {
				JSONObject place = places.getJSONObject(i);
				placesNames[i] = place.getString(Sfull_name);
				placesIds[i] = place.getString(Sid);
			}
			activity.onLocationsFound(accountId, placesIds, placesNames);
		} catch (JSONException e) {
			e.printStackTrace();
			activity.onLocationsFound(accountId, new String[0], new String[0]);
		}
	}

}
