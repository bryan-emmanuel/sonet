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
package com.piusvelte.sonet.core.task.facebook;

import static com.piusvelte.sonet.core.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.activity.SonetComments;
import com.piusvelte.sonet.core.task.LikeTask;

import android.database.Cursor;

public class FacebookLikeTask extends LikeTask {

	public FacebookLikeTask(SonetComments activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected String doInBackground(String... params) {
		String message = null;
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			if (Boolean.parseBoolean(params[LIKE])) {
				if (SonetHttpClient.httpResponse(httpClient, new HttpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, params[ID], Saccess_token, sonetCrypto.Decrypt(account.getString(1))))) != null) {
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK), activity.getString(R.string.success));
					message = activity.getString(R.string.unlike);
				} else {
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK), activity.getString(R.string.failure));
					message = activity.getString(R.string.like);
				}
			} else {
				HttpDelete httpDelete = new HttpDelete(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, params[ID], Saccess_token, sonetCrypto.Decrypt(account.getString(1))));
				httpDelete.setHeader("Content-Length", "0");
				if (SonetHttpClient.httpResponse(httpClient, httpDelete) != null) {
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK), activity.getString(R.string.success));
					message = activity.getString(R.string.like);
				} else {
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK), activity.getString(R.string.failure));
					message = activity.getString(R.string.unlike);
				}
			}
		} else {
			if (Boolean.parseBoolean(params[LIKE])) {
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK), activity.getString(R.string.failure));
				message = activity.getString(R.string.like);
			} else {
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK), activity.getString(R.string.failure));
				message = activity.getString(R.string.unlike);
			}
		}
		account.close();
		return message;
	}

}
