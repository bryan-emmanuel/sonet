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
import com.piusvelte.sonet.core.task.identica.Identica;
import com.piusvelte.sonet.core.task.linkedin.LinkedIn;
import com.piusvelte.sonet.core.task.twitter.Twitter;

import android.net.Uri;

public class LikeTask extends CommentsCommonTask {

	public static final int ID = 0;
	public static final int LIKE = 1;
	
	int position;

	public LikeTask(SonetComments activity, Uri data) {
		super(activity, data);
	}

	public void like(String id, int position, boolean like) {
		this.position = position;
		execute(id, Boolean.toString(like));
	}

	@Override
	protected String doInBackground(String... params) {
		loadCommon();
		if (service == Sonet.TWITTER) {
			publishProgress(serviceName,
					activity.getString((new Twitter(token, secret, httpClient).retweet(params[ID])) ? R.string.success : R.string.failure));
			return activity.getString(R.string.retweet);
		} else if (service == Sonet.FACEBOOK) {
			boolean like = Boolean.parseBoolean(params[LIKE]);
			boolean success = new Facebook(token, httpClient).like(params[ID], like);
			publishProgress(serviceName, activity.getString(success ? R.string.success : R.string.failure));
			return activity.getString((like == success) ? R.string.unlike : R.string.like);
		} else if (service == Sonet.LINKEDIN) {
			boolean like = Boolean.parseBoolean(params[LIKE]);
			boolean success = new LinkedIn(token, secret, httpClient).like(params[ID], like);
			publishProgress(serviceName, activity.getString(success ? R.string.success : R.string.failure));
			return activity.getString((like == success) ? R.string.unlike : R.string.like);
		} else if (service == Sonet.IDENTICA) {
			publishProgress(serviceName,
					activity.getString((new Identica(token, secret, httpClient).repeat(params[ID])) ? R.string.success : R.string.failure));
			return activity.getString(R.string.repeat);
		} else if (service == Sonet.CHATTER) {
			boolean like = Boolean.parseBoolean(params[LIKE]);
			boolean success = new Chatter(token, httpClient).like(accountServiceId, params[ID], like);
			publishProgress(serviceName, activity.getString(success ? R.string.success : R.string.failure));
			return activity.getString((like == success) ? R.string.unlike : R.string.like);
		}
		return "";
	}

	@Override
	protected void onProgressUpdate(String... params) {
		activity.onLikeProgress(params);
	}

	@Override
	protected void onPostExecute(String message) {
		activity.onLikeFinished(position, message);
	}

}
