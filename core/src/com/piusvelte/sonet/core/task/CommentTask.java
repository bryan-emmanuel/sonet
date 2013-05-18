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

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.activity.SonetComments;
import com.piusvelte.sonet.core.task.chatter.Chatter;
import com.piusvelte.sonet.core.task.facebook.Facebook;
import com.piusvelte.sonet.core.task.foursquare.Foursquare;
import com.piusvelte.sonet.core.task.identica.Identica;
import com.piusvelte.sonet.core.task.linkedin.LinkedIn;
import com.piusvelte.sonet.core.task.myspace.MySpace;
import com.piusvelte.sonet.core.task.twitter.Twitter;

import android.net.Uri;

public class CommentTask extends CommentsCommonTask {

	public static final int MESSAGE = 0;

	public CommentTask(SonetComments activity, Uri data) {
		super(activity, data);
	}

	public void comment(String message) {
		execute(message);
	}

	@Override
	protected String doInBackground(String... params) {
		loadCommon();
		publishProgress(serviceName);
		boolean commented = false;
		if (service == Sonet.TWITTER)
			commented = new Twitter(token, secret, httpClient).comment(statusId, params[MESSAGE]);
		else if (service == Sonet.FACEBOOK)
			commented = new Facebook(token, httpClient).comment(statusId, params[MESSAGE]);
		else if (service == Sonet.MYSPACE)
			commented = new MySpace(token, secret, httpClient).comment(entityId, statusId, params[MESSAGE]);
		else if (service == Sonet.FOURSQUARE)
			commented = new Foursquare(token, httpClient).comment(statusId, params[MESSAGE]);
		else if (service == Sonet.LINKEDIN)
			commented = new LinkedIn(token, secret, httpClient).comment(statusId, params[MESSAGE]);
		else if (service == Sonet.IDENTICA)
			commented = new Identica(token, secret, httpClient).comment(statusId, params[MESSAGE]);
		else if (service == Sonet.CHATTER)
			commented = new Chatter(token, httpClient).comment(statusId, params[MESSAGE]);
		return serviceName + activity.getString(commented ? R.string.success : R.string.failure);
	}

	@Override
	protected void onProgressUpdate(String... params) {
		activity.onCommentProgress(params);
	}

	@Override
	protected void onPostExecute(String message) {
		activity.onCommentFinished(message);
	}

}
