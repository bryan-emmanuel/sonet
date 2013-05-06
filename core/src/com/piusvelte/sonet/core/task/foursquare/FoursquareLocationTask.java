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
package com.piusvelte.sonet.core.task.foursquare;

import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_SEARCH;
import static com.piusvelte.sonet.core.Sonet.SNearby;
import static com.piusvelte.sonet.core.Sonet.Sgroups;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.Sitems;
import static com.piusvelte.sonet.core.Sonet.Sname;
import static com.piusvelte.sonet.core.Sonet.Sresponse;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;

import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.activity.SonetCreatePost;
import com.piusvelte.sonet.core.task.LocationTask;

public class FoursquareLocationTask extends LocationTask {

	public FoursquareLocationTask(SonetCreatePost activity, long accountId) {
		super(activity, accountId);
	}
	
	@Override
	protected String doInBackground(String... params) {
		String response = null;
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst())
			response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(FOURSQUARE_SEARCH, FOURSQUARE_BASE_URL, params[0], params[1], sonetCrypto.Decrypt(account.getString(1)))));
		account.close();
		return response;
	}

	@Override
	protected void onPostExecute(String response) {
		try {
			JSONArray groups = new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Sgroups);
			if (groups.length() > 0) {
				JSONObject group = groups.getJSONObject(0);
				if (group.getString(Sname).equals(SNearby)) {
					JSONArray places = group.getJSONArray(Sitems);
					final String placesNames[] = new String[places.length()];
					final String placesIds[] = new String[places.length()];
					for (int i = 0, i2 = places.length(); i < i2; i++) {
						JSONObject place = places.getJSONObject(i);
						placesNames[i] = place.getString(Sname);
						placesIds[i] = place.getString(Sid);
					}
					activity.onLocationsFound(accountId, placesIds, placesNames);
					return;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		activity.onLocationsFound(accountId, new String[0], new String[0]);
	}

}
