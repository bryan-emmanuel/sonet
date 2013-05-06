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
package com.piusvelte.sonet.core.task;

import org.apache.http.client.HttpClient;

import com.piusvelte.sonet.core.SonetCrypto;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.activity.SonetCreatePost;

import android.os.AsyncTask;

public class PostTask extends AsyncTask<String, String, Void> {

	public static final int MESSAGE = 0;
	public static final int PHOTO = 1;
	public static final int LATITUDE = 2;
	public static final int LONGITUDE = 3;
	public static final int LOCATION = 4;
	public static final int TAGS = 5;

	protected SonetCreatePost activity;
	protected long accountId;
	protected HttpClient httpClient;
	protected SonetCrypto sonetCrypto;

	public PostTask(SonetCreatePost activity, long accountId) {
		this.activity = activity;
		this.accountId = accountId;
		httpClient = SonetHttpClient.getThreadSafeClient(activity.getApplicationContext());
		sonetCrypto = SonetCrypto.getInstance(activity.getApplicationContext());
	}

	public void post(String message, String photo, String latitude, String longitude, String location, String[] tags) {
		String[] params = new String[TAGS + tags.length];
		params[MESSAGE] = message;
		params[PHOTO] = photo;
		params[LATITUDE] = latitude;
		params[LONGITUDE] = longitude;
		params[LOCATION] = location;
		if (tags != null) {
			for (int t = 0; t < tags.length; t++)
				params[TAGS + t] = tags[t];
		}
		super.execute(params);
	}

	@Override
	protected Void doInBackground(String... params) {
		return null;
	}

	@Override
	protected void onProgressUpdate(String... params) {
		activity.onPostProgress(params);
	}

	@Override
	protected void onPostExecute(Void result) {
		activity.postNextAccount();
	}

}
