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
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_COMMENT_BODY;
import static com.piusvelte.sonet.core.Sonet.LINKEDIN_UPDATE_COMMENTS;
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
import com.piusvelte.sonet.core.activity.SonetComments;
import com.piusvelte.sonet.core.task.CommentTask;

public class LinkedInCommentTask extends CommentTask {

	private static final String TAG = "LinkedInCommentTask";

	public LinkedInCommentTask(SonetComments activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected String doInBackground(String... params) {
		String result = null;
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			SonetOAuth sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, sonetCrypto.Decrypt(account.getString(1)), sonetCrypto.Decrypt(account.getString(2)));
			try {
				HttpPost httpPost = new HttpPost(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, params[ID]));
				httpPost.setEntity(new StringEntity(String.format(LINKEDIN_COMMENT_BODY, params[MESSAGE])));
				httpPost.addHeader(new BasicHeader("Content-Type", "application/xml"));
				if (SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(httpPost)) != null)
					result = Sonet.getServiceName(activity.getResources(), Sonet.LINKEDIN) + activity.getString(R.string.success);
				else
					result = Sonet.getServiceName(activity.getResources(), Sonet.LINKEDIN) + activity.getString(R.string.failure);
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				result = Sonet.getServiceName(activity.getResources(), Sonet.LINKEDIN) + activity.getString(R.string.failure);
			}
		} else
			result = Sonet.getServiceName(activity.getResources(), Sonet.LINKEDIN) + activity.getString(R.string.failure);
		account.close();
		return result;
	}

}
