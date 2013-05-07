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
package com.piusvelte.sonet.core.task.twitter;

import static com.piusvelte.sonet.core.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.TWITTER_RETWEET;
import static com.piusvelte.sonet.core.SonetTokens.TWITTER_KEY;
import static com.piusvelte.sonet.core.SonetTokens.TWITTER_SECRET;

import org.apache.http.client.methods.HttpPost;

import android.database.Cursor;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.SonetOAuth;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.activity.SonetComments;
import com.piusvelte.sonet.core.task.LikeTask;

public class TwitterRetweetTask extends LikeTask {

	public TwitterRetweetTask(SonetComments activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected String doInBackground(String... params) {
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, sonetCrypto.Decrypt(account.getString(1)), sonetCrypto.Decrypt(account.getString(2)));
			HttpPost httpPost = new HttpPost(String.format(TWITTER_RETWEET, TWITTER_BASE_URL, params[ID]));
			// resolve Error 417 Expectation by Twitter
			httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
			if (SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(httpPost)) != null)
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.TWITTER), activity.getString(R.string.success));
			else
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.TWITTER), activity.getString(R.string.failure));
		} else
			publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.TWITTER), activity.getString(R.string.failure));
		account.close();
		return activity.getString(R.string.retweet);
	}

}
