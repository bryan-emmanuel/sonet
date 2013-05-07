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
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_COMMENTS;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.Sonet.Smessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.activity.SonetComments;
import com.piusvelte.sonet.core.task.CommentTask;

import android.database.Cursor;
import android.util.Log;

public class FacebookCommentTask extends CommentTask {

	private static final String TAG = "FacebookCommentTask";

	public FacebookCommentTask(SonetComments activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected String doInBackground(String... params) {
		String message = null;
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			HttpPost httpPost = new HttpPost(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, params[ID], Saccess_token, sonetCrypto.Decrypt(account.getString(1))));
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			postParams.add(new BasicNameValuePair(Smessage, params[MESSAGE]));
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(postParams));
				if (SonetHttpClient.httpResponse(httpClient, httpPost) != null)
					message = Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK) + activity.getString(R.string.success);
				else
					message = Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK) + activity.getString(R.string.failure);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, e.toString());
				message = Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK) + activity.getString(R.string.failure);
			}
		} else
			message = Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK) + activity.getString(R.string.failure);
		account.close();
		return message;
	}

}
