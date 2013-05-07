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

import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_ADDCOMMENT;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_BASE_URL;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.client.methods.HttpPost;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.activity.SonetComments;
import com.piusvelte.sonet.core.task.CommentTask;

import android.database.Cursor;
import android.util.Log;

public class FoursquareCommentTask extends CommentTask {

	private static final String TAG = "FoursquarePostTask";

	public FoursquareCommentTask(SonetComments activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected String doInBackground(String... params) {
		String result = null;
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			HttpPost httpPost = null;
			try {
				httpPost = new HttpPost(String.format(FOURSQUARE_ADDCOMMENT, FOURSQUARE_BASE_URL, params[ID], URLEncoder.encode(params[MESSAGE], "UTF-8"), sonetCrypto.Decrypt(account.getString(1))));
				if (SonetHttpClient.httpResponse(httpClient, httpPost) != null)
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FOURSQUARE), activity.getString(R.string.success));
				else
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FOURSQUARE), activity.getString(R.string.failure));
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, e.getMessage());
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FOURSQUARE), activity.getString(R.string.failure));
			}
		} else
			publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FOURSQUARE), activity.getString(R.string.failure));
		account.close();
		return result;
	}

}
