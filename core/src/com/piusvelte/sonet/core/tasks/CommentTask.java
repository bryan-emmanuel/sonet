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

import org.apache.http.client.HttpClient;

import com.piusvelte.sonet.core.SonetComments;
import com.piusvelte.sonet.core.SonetCrypto;
import com.piusvelte.sonet.core.SonetHttpClient;

import android.os.AsyncTask;

public class CommentTask extends AsyncTask<String, String, Void> {

	public static final int ID = 0;
	public static final int MESSAGE = 1;

	SonetComments activity;
	long accountId;
	HttpClient httpClient;
	SonetCrypto sonetCrypto;

	public CommentTask(SonetComments activity, long accountId) {
		this.activity = activity;
		this.accountId = accountId;
		httpClient = SonetHttpClient.getThreadSafeClient(activity.getApplicationContext());
		sonetCrypto = SonetCrypto.getInstance(activity.getApplicationContext());
	}

	public void comment(String id, String message) {
		super.execute(id, message);
	}

	@Override
	protected Void doInBackground(String... params) {
		return null;
	}

	@Override
	protected void onProgressUpdate(String... params) {
		activity.onCommentProgress(params);
	}

	@Override
	protected void onPostExecute(Void result) {
		activity.onCommentFinished();
	}

}
