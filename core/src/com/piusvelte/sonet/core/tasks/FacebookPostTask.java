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

import static com.piusvelte.sonet.core.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_POST;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.Sonet.Smessage;
import static com.piusvelte.sonet.core.Sonet.Splace;
import static com.piusvelte.sonet.core.Sonet.Stags;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.piusvelte.sonet.core.PhotoUploadService;
import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetCreatePost;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.Sonet.Statuses;
import com.piusvelte.sonet.core.Sonet.Widgets;

import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class FacebookPostTask extends PostTask {
	
	private static final String TAG = "FacebookPostTask";

	public FacebookPostTask(SonetCreatePost activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected Void doInBackground(String... params) {
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			StringBuilder tags = null;
			if (params.length > TAGS) {
				tags = new StringBuilder();
				tags.append("[");
				String tag_format;
				if (params[PHOTO] != null)
					tag_format = "{\"tag_uid\":\"%s\",\"x\":0,\"y\":0}";
				else
					tag_format = "%s";
				tags.append(String.format(tag_format, params[TAGS]));
				for (int t = (TAGS + 1); t < params.length; t++) {
					tags.append(String.format(tag_format, params[t]));
					tags.append(",");
				}
				tags.append("]");
			}
			if (params[PHOTO] != null) {
				// upload photo
				// uploading a photo takes a long time, have the service handle it
				Intent i = Sonet.getPackageIntent(activity.getApplicationContext(), PhotoUploadService.class);
				i.setAction(Sonet.ACTION_UPLOAD);
				i.putExtra(Accounts.TOKEN, account.getString(1));
				i.putExtra(Widgets.INSTANT_UPLOAD, params[PHOTO]);
				i.putExtra(Statuses.MESSAGE, params[MESSAGE]);
				i.putExtra(Splace, params[LOCATION]);
				if (tags != null)
					i.putExtra(Stags, tags.toString());
				activity.startService(i);
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK) + " photo");
			} else {
				// regular post
				HttpPost httpPost = new HttpPost(String.format(FACEBOOK_POST, FACEBOOK_BASE_URL, Saccess_token, sonetCrypto.Decrypt(account.getString(1))));
				List<NameValuePair> postParams = new ArrayList<NameValuePair>();
				postParams.add(new BasicNameValuePair(Smessage, params[MESSAGE]));
				if (params[LOCATION] != null)
					postParams.add(new BasicNameValuePair(Splace, params[LOCATION]));
				if (tags != null)
					postParams.add(new BasicNameValuePair(Stags, tags.toString()));
				try {
					httpPost.setEntity(new UrlEncodedFormEntity(postParams));
					if (SonetHttpClient.httpResponse(httpClient, httpPost) != null)
						publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK), activity.getString(R.string.success));
					else
						publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK), activity.getString(R.string.failure));
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, e.toString());
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.FACEBOOK), activity.getString(R.string.failure));
				}
			}
		}
		account.close();
		return null;
	}

}
