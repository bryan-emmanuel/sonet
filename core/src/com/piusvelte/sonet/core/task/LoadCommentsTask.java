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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetProvider;
import com.piusvelte.sonet.core.Sonet.Entities;
import com.piusvelte.sonet.core.Sonet.Notifications;
import com.piusvelte.sonet.core.Sonet.Statuses;
import com.piusvelte.sonet.core.Sonet.Statuses_styles;
import com.piusvelte.sonet.core.activity.SonetComments;
import com.piusvelte.sonet.core.task.chatter.Chatter;
import com.piusvelte.sonet.core.task.facebook.Facebook;
import com.piusvelte.sonet.core.task.foursquare.Foursquare;
import com.piusvelte.sonet.core.task.identica.Identica;
import com.piusvelte.sonet.core.task.linkedin.LinkedIn;
import com.piusvelte.sonet.core.task.myspace.MySpace;
import com.piusvelte.sonet.core.task.twitter.Twitter;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class LoadCommentsTask extends CommentsCommonTask {

	private static final String TAG = "LoadCommentsTask";
	
	public static final int ID = 0;
	public static final int NAME = 1;
	public static final int MESSAGE = 2;
	public static final int CREATED = 3;
	public static final int ACTION = 4;
	
	public static final int RESULT_MESSAGE = 0;
	public static final int RESULT_ACTION = 1;

	public LoadCommentsTask(SonetComments activity, Uri data) {
		super(activity, data);
	}

	public void comment(String entity, String id, String message) {
		super.execute(entity, id, message);
	}

	@Override
	protected String doInBackground(String... params) {
		super.doInBackground(params);
		UriMatcher um = new UriMatcher(UriMatcher.NO_MATCH);
		String authority = Sonet.getAuthority(activity);
		um.addURI(authority, SonetProvider.VIEW_STATUSES_STYLES + "/*", SonetProvider.STATUSES_STYLES);
		um.addURI(authority, SonetProvider.TABLE_NOTIFICATIONS + "/*", SonetProvider.NOTIFICATIONS);
		Cursor status;
		switch (um.match(data)) {
		case SonetProvider.STATUSES_STYLES:
			status = activity.getContentResolver().query(Statuses_styles.getContentUri(activity), new String[]{Statuses_styles.FRIEND, Statuses_styles.MESSAGE, Statuses_styles.CREATED}, Statuses_styles._ID + "=?", new String[]{data.getLastPathSegment()}, null);
			if (status.moveToFirst()) {
				addComment(statusId,
						status.getString(0),
						status.getString(1),
						Sonet.getCreatedText(status.getLong(2), time24hr),
						service == Sonet.TWITTER ? activity.getString(R.string.retweet) : service == Sonet.IDENTICA ? activity.getString(R.string.repeat) : "");
			}
			status.close();
			break;
		case SonetProvider.NOTIFICATIONS:
			Cursor notification = activity.getContentResolver().query(Notifications.getContentUri(activity), new String[]{Notifications.FRIEND, Notifications.MESSAGE, Notifications.CREATED}, Notifications._ID + "=?", new String[]{data.getLastPathSegment()}, null);
			if (notification.moveToFirst()) {
				// clear notification
				ContentValues values = new ContentValues();
				values.put(Notifications.CLEARED, 1);
				activity.getContentResolver().update(Notifications.getContentUri(activity), values, Notifications._ID + "=?", new String[]{data.getLastPathSegment()});
				addComment(statusId,
						notification.getString(0),
						notification.getString(1),
						Sonet.getCreatedText(notification.getLong(2), time24hr),
						service == Sonet.TWITTER ? activity.getString(R.string.retweet) : service == Sonet.IDENTICA ? activity.getString(R.string.repeat) : "");
				serviceName = activity.getResources().getStringArray(R.array.service_entries)[service];
			}
			notification.close();
			break;
		default:
			publishProgress(new String[0]);
			addComment(statusId, "", "error, status not found", "", "");
		}
		//TODO: at this point the service specific task needs to take over
		int count = 0;
		String result = "";
		if (service == Sonet.TWITTER) {
			Twitter twitter = new Twitter(token, secret, httpClient);
			count = twitter.getRetweets(this, statusId, time24hr);
			result = twitter.getScreenname(entityId);
		} else if (service == Sonet.FACEBOOK) {
			Facebook facebook = new Facebook(token, httpClient);
			count = facebook.getComments(this, statusId, time24hr);
			result = facebook.getLikeStatus(statusId, accountServiceId);
		} else if (service == Sonet.MYSPACE) {
			MySpace myspace = new MySpace(token, secret, httpClient);
			count = myspace.getComments(this, statusId, entityId, time24hr);
		} else if (service == Sonet.LINKEDIN) {
			LinkedIn linkedin = new LinkedIn(token, secret, httpClient);
			count = linkedin.getComments(this, statusId, time24hr);
			result = linkedin.getLikeStatus(statusId);
		} else if (service == Sonet.FOURSQUARE) {
			Foursquare foursquare = new Foursquare(token, httpClient);
			count = foursquare.getComments(this, statusId, time24hr);
		} else if (service == Sonet.IDENTICA) {
			Identica identica = new Identica(token, secret, httpClient);
			count = identica.getRetweets(this, statusId, time24hr);
			result = identica.getScreenname(entityId);
		} else if (service == Sonet.GOOGLEPLUS) {
			//TODO
		} else if (service == Sonet.CHATTER) {
			Chatter chatter = new Chatter(token, httpClient);
			count = chatter.getComments(this, statusId, time24hr);
			result = chatter.getLikeStatus(statusId, accountServiceId);
		}
		if (count == 0)
			addComment("", "", activity.getString(R.string.no_comments), "", "");
		return result;
	}

	public void addComment(String id, String name, String message, String created, String action) {
		publishProgress(new String[]{id, name, message, created, action});
	}

	private SimpleDateFormat mSimpleDateFormat;
	
	public long parseDate(String date, String format) {
		if (date != null) {
			// hack for the literal 'Z'
			if (date.substring(date.length() - 1).equals("Z")) {
				date = date.substring(0, date.length() - 2) + "+0000";
			}
			Date created = null;
			if (format != null) {
				if (mSimpleDateFormat == null) {
					mSimpleDateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
					// all dates should be GMT/UTC
					mSimpleDateFormat.setTimeZone(Sonet.sTimeZone);
				}
				try {
					created = mSimpleDateFormat.parse(date);
					return created.getTime();
				} catch (ParseException e) {
					Log.e(TAG, e.toString());
				}
			} else {
				// attempt to parse RSS date
				if (mSimpleDateFormat != null) {
					try {
						created = mSimpleDateFormat.parse(date);
						return created.getTime();
					} catch (ParseException e) {
						Log.e(TAG, e.toString());
					}
				}
				for (String rfc822 : Sonet.sRFC822) {
					mSimpleDateFormat = new SimpleDateFormat(rfc822, Locale.ENGLISH);
					mSimpleDateFormat.setTimeZone(Sonet.sTimeZone);
					try {
						if ((created = mSimpleDateFormat.parse(date)) != null) {
							return created.getTime();
						}
					} catch (ParseException e) {
						Log.e(TAG, e.toString());
					}
				}
			}
		}
		return System.currentTimeMillis();
	}

	@Override
	protected void onProgressUpdate(String... params) {
		if (params.length > 0) {
			HashMap<String, String> commentMap = new HashMap<String, String>();
			commentMap.put(Statuses.SID, params[ID]);
			commentMap.put(Entities.FRIEND, params[NAME]);
			commentMap.put(Statuses.MESSAGE, params[MESSAGE]);
			commentMap.put(Statuses.CREATEDTEXT, params[CREATED]);
			commentMap.put(SonetComments.ACTION, params[ACTION]);
			activity.addComment(commentMap);
		} else
			activity.addComment(null);
	}

	@Override
	protected void onPostExecute(String result) {
		if (result != null) {
			if ((service == Sonet.TWITTER) || (service == Sonet.IDENTICA))
				activity.setDefaultMessage(result);
			else {
				if (service == Sonet.LINKEDIN) {
					if (result.equals(activity.getString(R.string.uncommentable)))
						activity.uncommentable();
					else
						activity.setCommentStatus(0, result);
				} else
					activity.setCommentStatus(0, result);
			}
		}
		activity.onCommentsLoaded();
	}

}
