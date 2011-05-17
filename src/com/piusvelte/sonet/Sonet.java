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
package com.piusvelte.sonet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.BaseColumns;
import android.util.Log;

public class Sonet {

	private static final String TAG = "Sonet";
	
	protected static final String DATABASE_NAME = "sonet.db";
	protected static final int DATABASE_VERSION = 12;
	protected static final String TABLE_ACCOUNTS = "accounts";
	protected static final String TABLE_WIDGETS = "widgets";
	protected static final String TABLE_STATUSES = "statuses";
	protected static final String VIEW_STATUSES_STYLES = "statuses_styles";
	protected static final String TABLE_ENTITIES = "entities";

	protected static final String TOKEN = "access_token";
	protected static final String EXPIRES = "expires_in";
	protected static final String ACTION_REFRESH = "com.piusvelte.sonet.Sonet.REFRESH";
	protected static final String ACTION_BUILD_SCROLL = "com.piusvelte.sonet.Sonet.BUILD_SCROLL";
	protected static final String EXTRA_ACCOUNT_ID = "com.piusvelte.sonet.Sonet.ACCOUNT_ID";
	protected static final long INVALID_ACCOUNT_ID = -1;
	protected static final int RESULT_REFRESH = 1;
	protected static final String GOOGLE_AD_ID = "a14d5598b4afd11";
	protected static final String SID_FORMAT = "_%s";

	protected static final int TWITTER = 0;
	protected static final String TWITTER_BASE_URL = "http://api.twitter.com/";
	protected static final String TWITTER_URL_REQUEST = "%soauth/request_token";
	protected static final String TWITTER_URL_AUTHORIZE = "%soauth/authorize";
	protected static final String TWITTER_URL_ACCESS = "%soauth/access_token";
	protected static final String TWITTER_URL_FEED = "%s1/statuses/home_timeline.json?count=%s";
	protected static final String TWITTER_TWEET = "%s1/statuses/update.json";
	protected static final String TWITTER_RETWEET = "%s1/statuses/retweet/%s.json";
	protected static final String TWITTER_USER = "%s1/users/lookup.json?user_id=%s";
	protected static final String TWITTER_UPDATE = "%s1/statuses/update.json";
	protected static final String TWITTER_SEARCH = "%s1/geo/search.json?lat=%s&long=%s";

	protected static final int FACEBOOK = 1;
	protected static final String FACEBOOK_BASE_URL = "https://graph.facebook.com/";
	protected static final String FACEBOOK_URL_AUTHORIZE = "%Soauth/authorize?client_id=%s&scope=offline_access,read_stream,publish_stream,publish_checkins&type=user_agent&redirect_uri=%s&display=touch&sdk=android";
	protected static final String FACEBOOK_URL_ME = "%Sme?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_URL_FEED = "%Sme/home?date_format=U&format=json&sdk=android&limit=%s&%s=%s&fields=actions,link,type,from,message,created_time,to";
	protected static final String FACEBOOK_POST = "%sme/feed?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_CHECKIN = "%sme/checkins?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_LIKES = "%s%s/likes?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_COMMENTS = "%s%s/comments?date_format=U&format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_SEARCH = "%ssearch?type=place&center=%s,%s&distance=1000&format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_COORDINATES = "{\"latitude\":\"%s\",\"longitude\":\"%s\"}";

	protected static final int MYSPACE = 2;
	protected static final String MYSPACE_BASE_URL = "http://api.myspace.com/1.0/";
	protected static final String MYSPACE_URL_REQUEST = "http://api.myspace.com/request_token";
	protected static final String MYSPACE_URL_AUTHORIZE = "http://api.myspace.com/authorize";
	protected static final String MYSPACE_URL_ACCESS = "http://api.myspace.com/access_token";
	protected static final String MYSPACE_URL_ME = "%speople/@me/@self";
	protected static final String MYSPACE_URL_FEED = "%sstatusmood/@me/@friends/history?count=%s&includeself=true&fields=author,source";
	protected static final String MYSPACE_URL_STATUSMOOD = "%sstatusmood/@me/@self";
	protected static final String MYSPACE_URL_STATUSMOODCOMMENTS = "%sstatusmoodcomments/%s/@self/%s?format=json&includeself=true&fields=author";
	protected static final String MYSPACE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	protected static final String MYSPACE_STATUSMOOD_BODY = "{\"status\":\"%s\"}";
	protected static final String MYSPACE_STATUSMOODCOMMENTS_BODY = "{\"body\":\"%s\"}";

	protected static final int BUZZ = 3;
	protected static final String BUZZ_BASE_URL = "https://www.googleapis.com/buzz/v1/";
	protected static final String BUZZ_URL_REQUEST = "https://www.google.com/accounts/OAuthGetRequestToken?scope=%s&xoauth_displayname=%s&domain=%s";
	protected static final String BUZZ_URL_AUTHORIZE = "https://www.google.com/buzz/api/auth/OAuthAuthorizeToken?scope=%s&xoauth_displayname=%s&domain=%s&btmpl=mobile";
	protected static final String BUZZ_URL_ACCESS = "https://www.google.com/accounts/OAuthGetAccessToken";
	protected static final String BUZZ_SCOPE = "https://www.googleapis.com/auth/buzz";
	protected static final String BUZZ_URL_ME = "%speople/@me/@self?alt=json&key=%s";
	protected static final String BUZZ_URL_FEED = "%sactivities/@me/@consumption?alt=json&max-results=%s&key=%s";
	protected static final String BUZZ_LIKE = "%sactivities/@me/@liked/%s?alt=json&key=%s";
	protected static final String BUZZ_GET_LIKE = "%sactivities/@me/@self/%s/@liked?alt=json&key=%s";
	protected static final String BUZZ_COMMENT = "%sactivities/@me/@self/%s/@comments?alt=json&key=%s";
	protected static final String BUZZ_ACTIVITY = "%sactivities/@me/@self?alt=json&key=%s";
	protected static final String BUZZ_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	protected static final String BUZZ_COMMENT_BODY = "{\"data\":{\"content\":\"%s\"}}";
	protected static final String BUZZ_ACTIVITY_BODY = "{\"data\":{\"object\":{\"type\":\"note\",\"content\":\"%s\"}}}";

	protected static final int FOURSQUARE = 4;
	protected static final String FOURSQUARE_BASE_URL = "https://api.foursquare.com/v2/";
	protected static final String FOURSQUARE_URL_ACCESS = "https://foursquare.com/oauth2/access_token";
	protected static final String FOURSQUARE_URL_AUTHORIZE = "https://foursquare.com/oauth2/authorize?client_id=%s&response_type=token&redirect_uri=%s&display=touch";
	protected static final String FOURSQUARE_URL_ME = "%susers/self?oauth_token=%s";
	protected static final String FOURSQUARE_URL_FEED = "%scheckins/recent?limit=%s&oauth_token=%s";
	protected static final String FOURSQUARE_CHECKIN = "%scheckins/add?venueId=%s&shout=%s&ll=%s,%s&broadcast=public&oauth_token=%s";
	protected static final String FOURSQUARE_ADDCOMMENT = "%scheckins/%s/addcomment?text=%s&oauth_token=%s";
	protected static final String FOURSQUARE_SEARCH = "%svenues/search?ll=%s,%s&intent=checkin&oauth_token=%s";
	protected static final String FOURSQUARE_GET_CHECKIN = "%scheckins/%s?oauth_token=%s";

	protected static final int LINKEDIN = 5;
	protected static final String LINKEDIN_BASE_URL = "https://api.linkedin.com/v1/people/~";
	protected static final String LINKEDIN_URL_REQUEST = "https://api.linkedin.com/uas/oauth/requestToken";
	protected static final String LINKEDIN_URL_AUTHORIZE = "https://www.linkedin.com/uas/oauth/authorize";
	protected static final String LINKEDIN_URL_ACCESS = "https://api.linkedin.com/uas/oauth/accessToken";
	protected static final String LINKEDIN_URL_ME = "%s:(id,first-name,last-name)";
	protected static final String LINKEDIN_URL_FEED = "%s/network/updates?type=APPS&type=CMPY&type=CONN&type=JOBS&type=JGRP&type=PICT&type=PRFU&type=RECU&type=PRFX&type=ANSW&type=QSTN&type=SHAR&type=VIRL&count=%s";
	protected static final String[][] LINKEDIN_HEADERS = new String[][] {{"x-li-format", "json"}};
	protected static final HashMap<String, String> LINKEDIN_UPDATETYPES;
	protected static final String LINKEDIN_IS_LIKED = "%s/network/updates/key=%s/is-liked";
	protected static final String LINKEDIN_UPDATE = "%s/network/updates/key=%s";
	protected static final String LINKEDIN_UPDATE_COMMENTS = "%s/network/updates/key=%s/update-comments";
	protected static final String LINKEDIN_POST = "%s/person-activities";
	protected static final String LINKEDIN_POST_BODY = "<?xml version='1.0' encoding='UTF-8'?><activity locale=\"%s\"><content-type>linkedin-html</content-type><body>%s</body></activity>";
	protected static final String LINKEDIN_COMMENT_BODY = "<?xml version='1.0' encoding='UTF-8'?><update-comment><comment>%s</comment></update-comment>";
	protected static final String LINKEDIN_LIKE_BODY = "<?xml version='1.0' encoding='UTF-8'?><is-liked>%s</is-liked>";

	protected static final String AM = "a.m.";
	protected static final String PM = "p.m.";
	protected static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	protected static HashMap<Integer, Context> sWidgetsContext;

	protected static final String ACCOUNTS_QUERY = "(case when " + Accounts.SERVICE + "='" + TWITTER + "' then 'Twitter: ' when "
	+ Accounts.SERVICE + "='" + FACEBOOK + "' then 'Facebook: ' when "
	+ Accounts.SERVICE + "='" + MYSPACE + "' then 'MySpace: ' when "
	+ Accounts.SERVICE + "='" + BUZZ + "' then 'Buzz: ' when "
	+ Accounts.SERVICE + "='" + LINKEDIN + "' then 'LinkedIn: ' when "
	+ Accounts.SERVICE + "='" + FOURSQUARE + "' then 'Foursquare: ' else '' end)||" + Accounts.USERNAME + " as " + Accounts.USERNAME;

	private static final String POWER_SERVICE = Context.POWER_SERVICE;
	private static WakeLock sWakeLock;
	static boolean hasLock() {
		return (sWakeLock != null);
	}

	static void acquire(Context context) {
		if (hasLock()) sWakeLock.release();
		PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
		sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		sWakeLock.acquire();
	}

	static void release() {
		if (hasLock()) {
			sWakeLock.release();
			sWakeLock = null;
		}
	}

	static {
		LINKEDIN_UPDATETYPES = new HashMap<String, String>();
		LINKEDIN_UPDATETYPES.put("ANSW", "updated an answer");
		LINKEDIN_UPDATETYPES.put("APPS", "updated the application ");
		LINKEDIN_UPDATETYPES.put("CMPY", "company update");
		LINKEDIN_UPDATETYPES.put("CONN", "is now connected to ");
		LINKEDIN_UPDATETYPES.put("JOBP", "posted the job ");
		LINKEDIN_UPDATETYPES.put("JGRP", "joined the group ");
		LINKEDIN_UPDATETYPES.put("PRFX", "updated their extended profile");
		LINKEDIN_UPDATETYPES.put("PREC", "recommends ");
		LINKEDIN_UPDATETYPES.put("PROF", "changed their profile");
		LINKEDIN_UPDATETYPES.put("QSTN", "updated a question");
		LINKEDIN_UPDATETYPES.put("SHAR", "shared something");
		LINKEDIN_UPDATETYPES.put("VIRL", "updated the viral ");
		LINKEDIN_UPDATETYPES.put("PICU", "updated their profile picture");

		sWidgetsContext = new HashMap<Integer, Context>();
	}

//	protected static final int SALESFORCE = 6;
//	protected static final String SALESFORCE_URL_REQUEST = "https://login.salesforce.com/_nc_external/system/security/oauth/RequestTokenHandler";
//	protected static final String SALESFORCE_URL_AUTHORIZE = "https://login.salesforce.com/setup/secur/RemoteAccessAuthorizationPage.apexp";
//	protected static final String SALESFORCE_URL_ACCESS = "https://login.salesforce.com/_nc_external/system/security/oauth/AccessTokenHandler";
//	protected static final String SALESFORCE_FEED = "";

	protected static final int INVALID_SERVICE = -1;

	protected static final int default_interval = 3600000;
	protected static final int default_buttons_bg_color = 0xAA000000;
	protected static final int default_buttons_color = -1;
	protected static final int default_message_bg_color = 0xAA000000;
	protected static final int default_message_color = -1;
	protected static final int default_friend_color = -1;
	protected static final int default_created_color = -1;
	protected static final int default_buttons_textsize = 14;
	protected static final int default_messages_textsize = 14;
	protected static final int default_friend_textsize = 14;
	protected static final int default_created_textsize = 14;
	protected static final int default_statuses_per_account = 10;

	public Sonet() {
	}

	public static final class Accounts implements BaseColumns {

		private Accounts() {
		}

		public static final String USERNAME = "username";
		public static final String TOKEN = "token";
		public static final String SECRET = "secret";
		public static final String SERVICE = "service";
		public static final String EXPIRY = "expiry";
		public static final String WIDGET = "widget";
		// service id for posting and linking
		public static final String SID = "sid";

	}

	public static final class Widgets implements BaseColumns {

		private Widgets() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/widgets");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widgets";

		public static final String WIDGET = "widget";
		// account specific settings per widget
		public static final String ACCOUNT = "account";
		public static final String INTERVAL = "interval";
		public static final String HASBUTTONS = "hasbuttons";
		public static final String BUTTONS_BG_COLOR = "buttons_bg_color";
		public static final String BUTTONS_COLOR = "buttons_color";
		public static final String MESSAGES_BG_COLOR = "messages_bg_color";
		public static final String MESSAGES_COLOR = "messages_color";
		public static final String TIME24HR = "time24hr";
		public static final String FRIEND_COLOR = "friend_color";
		public static final String CREATED_COLOR = "created_color";
		public static final String SCROLLABLE = "scrollable";
		public static final String BUTTONS_TEXTSIZE = "buttons_textsize";
		public static final String MESSAGES_TEXTSIZE = "messages_textsize";
		public static final String FRIEND_TEXTSIZE = "friend_textsize";
		public static final String CREATED_TEXTSIZE = "created_textsize";
		public static final String ICON = "icon";
		public static final String STATUSES_PER_ACCOUNT = "statuses_per_account";
		// make background updating optional
		public static final String BACKGROUND_UPDATE = "background_update";

	}

	public static final class Statuses implements BaseColumns {

		private Statuses() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/statuses");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.statuses";

		public static final String CREATED = "created";
		public static final String MESSAGE = "message";
		public static final String SERVICE = "service";
		public static final String WIDGET = "widget";
		public static final String CREATEDTEXT = "createdtext";
		// account specific settings per widget
		public static final String ACCOUNT = "account";
		public static final String STATUS_BG = "status_bg";
		public static final String ICON = "icon";
		// service id for posting and linking
		public static final String SID = "sid";
		// store friend and profile data in a separate table
		public static final String ENTITY = "entity";

	}

	public static final class Statuses_styles implements BaseColumns {

		// this is actually a view, joining the account/widget/default styles to the statuses

		private Statuses_styles() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/statuses_styles");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.statuses_styles";

		public static final String CREATED = "created";
		public static final String FRIEND = "friend";
		public static final String PROFILE = "profile";
		public static final String MESSAGE = "message";
		public static final String SERVICE = "service";
		public static final String WIDGET = "widget";
		// account specific settings per widget
		public static final String ACCOUNT = "account";
		public static final String CREATEDTEXT = "createdtext";
		public static final String MESSAGES_COLOR = "messages_color";
		public static final String FRIEND_COLOR = "friend_color";
		public static final String CREATED_COLOR = "created_color";
		public static final String MESSAGES_TEXTSIZE = "messages_textsize";
		public static final String FRIEND_TEXTSIZE = "friend_textsize";
		public static final String CREATED_TEXTSIZE = "created_textsize";
		public static final String STATUS_BG = "status_bg";
		public static final String ICON = "icon";
		// service id, for posting and linking
		public static final String SID = "sid";
		// store friend and profile data in a separate table
		public static final String ENTITY = "entity";
		public static final String ESID = "esid";

	}

	public static final class Entities implements BaseColumns {

		private Entities() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/entities");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.entities";

		public static final String ESID = "esid";
		public static final String FRIEND = "friend";
		public static final String PROFILE = "profile";
		public static final String ACCOUNT = "account";

	}

	protected static String httpResponse(HttpUriRequest httpRequest) {
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse httpResponse;
		String response = null;
		try {
			httpResponse = httpClient.execute(httpRequest);
			StatusLine statusLine = httpResponse.getStatusLine();
			HttpEntity entity = httpResponse.getEntity();

			switch(statusLine.getStatusCode()) {
			case 200:
			case 201:
			case 204:
				if (entity != null) {
					InputStream is = entity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					StringBuilder sb = new StringBuilder();

					String line = null;
					try {
						while ((line = reader.readLine()) != null) {
							sb.append(line + "\n");
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					response = sb.toString();
				} else {
					response = "OK";
				}
				break;
			default:
				Log.e(TAG,httpRequest.getURI().toString());
				Log.e(TAG,""+statusLine.getStatusCode()+" "+statusLine.getReasonPhrase());
				if (entity != null) {
					InputStream is = entity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					StringBuilder sb = new StringBuilder();

					String line = null;
					try {
						while ((line = reader.readLine()) != null) sb.append(line + "\n");
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					Log.e(TAG,"response:"+sb.toString());
				}
				break;
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.toString());
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		return response;
	}

	protected static long parseDate(String date, String format) {
		SimpleDateFormat msformat = new SimpleDateFormat(format);
		// all dates should be GMT/UTC
		msformat.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date created;
		try {
			created = msformat.parse(date);
		} catch (ParseException e) {
			created = new Date();
			Log.e(TAG,e.toString());
		}
		return created.getTime();
	}

	protected static String getCreatedText(long epoch, boolean time24hr) {
		Calendar mCalendar = Calendar.getInstance();
		mCalendar.setTimeInMillis(epoch);
		int hours = mCalendar.get(Calendar.HOUR_OF_DAY);
		if (System.currentTimeMillis() - mCalendar.getTimeInMillis() < 86400000) {
			if (time24hr) {
				return String.format("%d:%02d", hours, mCalendar.get(Calendar.MINUTE));
			} else {
				if (hours < 13) {
					return String.format("%d:%02d%s", hours, mCalendar.get(Calendar.MINUTE), Sonet.AM);
				} else {
					return String.format("%d:%02d%s", hours - 12, mCalendar.get(Calendar.MINUTE), Sonet.PM);
				}
			}
		} else return String.format("%s %d", Sonet.MONTHS[mCalendar.get(Calendar.MONTH)], mCalendar.get(Calendar.DATE));
	}

	protected static int[] arrayCat(int[] a, int[] b) {
		int[] c;
		for (int i = 0; i < b.length; i++) {
			c = new int[a.length];
			for (int n = 0; n < c.length; n++) {
				c[n] = a[n];
			}
			a = new int[c.length + 1];
			for (int n = 0; n < c.length; n++) {
				a[n] = c[n];
			}
			a[c.length] = b[i];
		}
		return a;
	}

	protected static int[] arrayAdd(int[] a, int b) {
		if (!Sonet.arrayContains(a, b)) {
			int[] c = new int[a.length];
			for (int i = 0; i < a.length; i++) {
				c[i] = a[i];
			}
			a = new int[c.length + 1];
			for (int i = 0; i < c.length; i++) {
				a[i] = c[i];
			}
			a[a.length - 1] = b;
		}
		return a;
	}

	protected static boolean arrayContains(int[] a, int b) {
		boolean contains = false;
		for (int c : a) {
			if (c == b) {
				contains = true;
				break;
			}
		}
		return contains;
	}

	protected static int[] arrayRemove(int[] a, int b) {
		if (Sonet.arrayContains(a, b)) {
			int[] c = new int[a.length - 1];
			int i = 0;
			for (int d : a) {
				if (d != b) {
					c[i++] = d;
				}
			}
			return c;			
		} else {
			return a;
		}
	}
	
	protected static int arrayIndex(int[] a, int b) {
		int c = -1;
		for (int i = 0; i < a.length; i++) {
			if (a[i] == b) {
				c = i;
				break;
			}
		}
		return c;
	}
	
	protected static String removeUnderscore(String sid) {
		// this is a fix for an issue where inserting a big integer, such as the id's from BUZZ into sqlite,
		// causing the value to convert to scientific notation. to avoid that, an underscore is prepended
		// this must be removed when querying the column
		return (sid != null) && (sid.length() != 0) && sid.substring(0, 1).equals("_") ? sid.substring(1) : sid;
	}

	protected static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table if not exists " + TABLE_ACCOUNTS
					+ " (" + Accounts._ID + " integer primary key autoincrement, "
					+ Accounts.USERNAME + " text, "
					+ Accounts.TOKEN + " text, "
					+ Accounts.SECRET + " text, "
					+ Accounts.SERVICE + " integer, "
					+ Accounts.EXPIRY + " integer, "
					+ Accounts.WIDGET + " integer, "
					+ Accounts.SID + " text);");
			db.execSQL("create table if not exists " + TABLE_WIDGETS
					+ " (" + Widgets._ID + " integer primary key autoincrement, "
					+ Widgets.WIDGET + " integer, "
					+ Widgets.INTERVAL + " integer, "
					+ Widgets.HASBUTTONS + " integer, "
					+ Widgets.BUTTONS_BG_COLOR + " integer, "
					+ Widgets.BUTTONS_COLOR + " integer, "
					+ Widgets.FRIEND_COLOR + " integer, "
					+ Widgets.CREATED_COLOR + " integer, "
					+ Widgets.MESSAGES_BG_COLOR + " integer, "
					+ Widgets.MESSAGES_COLOR + " integer, "
					+ Widgets.TIME24HR + " integer, "
					+ Widgets.SCROLLABLE + " integer, "
					+ Widgets.BUTTONS_TEXTSIZE + " integer, "
					+ Widgets.MESSAGES_TEXTSIZE + " integer, "
					+ Widgets.FRIEND_TEXTSIZE + " integer, "
					+ Widgets.CREATED_TEXTSIZE + " integer, "
					+ Widgets.ACCOUNT + " integer, "
					+ Widgets.ICON + " integer, "
					+ Widgets.STATUSES_PER_ACCOUNT + " integer, "
					+ Widgets.BACKGROUND_UPDATE + " integer);");
			db.execSQL("create table if not exists " + TABLE_STATUSES
					+ " (" + Statuses._ID + " integer primary key autoincrement, "
					+ Statuses.CREATED + " integer, "
					+ Statuses.MESSAGE + " text, "
					+ Statuses.SERVICE + " integer, "
					+ Statuses.CREATEDTEXT + " text, "
					+ Statuses.WIDGET + " integer, "
					+ Statuses.ACCOUNT + " integer, "
					+ Statuses.STATUS_BG + " blob, "
					+ Statuses.ICON + " blob, "
					+ Statuses.SID + " text, "
					+ Statuses.ENTITY + " integer);");
			db.execSQL("create table if not exists " + TABLE_ENTITIES
					+ " (" + Entities._ID + " integer primary key autoincrement, "
					+ Entities.FRIEND + " text, "
					+ Entities.PROFILE + " blob, "
					+ Entities.ACCOUNT + " integer, "
					+ Entities.ESID + " text);");
			db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
					TABLE_STATUSES + "." + Statuses._ID + ","
					+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
					+ Entities.FRIEND + " as " + Statuses_styles.FRIEND + ","
					+ Entities.PROFILE + " as " + Statuses_styles.PROFILE + ","
					+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
					+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
					+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
					+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
					+ TABLE_STATUSES + "." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
					+ "(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR + ","
					+ "(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR + ","
					+ "(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR + ","
					+ "(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
					+ "(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
					+ "(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
					+ Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG + ","
					+ Statuses.ICON + " as " + Statuses_styles.ICON + ","
					+ Statuses.SID + " as " + Statuses_styles.SID + ","
					+ TABLE_ENTITIES + "." + Entities._ID + " as " + Statuses_styles.ENTITY + ","
					+ Entities.ESID + " as " + Statuses_styles.ESID
					+ " from " + TABLE_STATUSES + "," + TABLE_ENTITIES
					+ " where " + TABLE_ENTITIES + "." + Entities._ID + "=" + Statuses.ENTITY);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				// add column for expiry
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text not null, "
						+ Accounts.TOKEN + " text not null, "
						+ Accounts.SECRET + " text not null, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," + Accounts.SECRET + "," + Accounts.SERVICE + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 3) {
				// remove not null constraints as facebook uses oauth2 and doesn't require a secret, add timezone
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ "timezone integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," + Accounts.SECRET + "," + Accounts.SERVICE + "," + Accounts.EXPIRY + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 4) {
				// add column for widget
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ "timezone integer, "
						+ Accounts.WIDGET + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," + Accounts.SECRET + "," + Accounts.SERVICE + "," + Accounts.EXPIRY + ",timezone,\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				// move preferences to db
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer);");
			}
			if (oldVersion < 5) {
				// cache for statuses
				db.execSQL("create table if not exists " + TABLE_STATUSES
						+ " (" + Statuses._ID + " integer primary key autoincrement, "
						+ Statuses.CREATED + " integer, "
						+ "link text, "
						+ "friend text, "
						+ "profile blob, "
						+ Statuses.MESSAGE + " text, "
						+ Statuses.SERVICE + " integer, "
						+ Statuses.CREATEDTEXT + " text, "
						+ Statuses.WIDGET + " integer);");
				// column for scrollable
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ",0 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
			}
			if (oldVersion < 6) {
				// add columns for textsize
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ",14,14,14,14 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
			}
			if (oldVersion < 7) {
				// add column for account to handle account specific widget settings
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer, "
						+ Widgets.ACCOUNT + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ","
						+ Widgets.BUTTONS_TEXTSIZE + ","
						+ Widgets.MESSAGES_TEXTSIZE + ","
						+ Widgets.FRIEND_TEXTSIZE + ","
						+ Widgets.CREATED_TEXTSIZE + ","
						+ Sonet.INVALID_ACCOUNT_ID + " from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				// add column for account to handle account specific widget settings
				// add column for status background and rename createdText > createdtext
				db.execSQL("create temp table " + TABLE_STATUSES + "_bkp as select * from " + TABLE_STATUSES + ";");
				db.execSQL("drop table if exists " + TABLE_STATUSES + ";");
				db.execSQL("create table if not exists " + TABLE_STATUSES
						+ " (" + Statuses._ID + " integer primary key autoincrement, "
						+ Statuses.CREATED + " integer, "
						+ "link text, "
						+ "friend text, "
						+ "profile blob, "
						+ Statuses.MESSAGE + " text, "
						+ Statuses.SERVICE + " integer, "
						+ Statuses.CREATEDTEXT + " text, "
						+ Statuses.WIDGET + " integer, "
						+ Statuses.ACCOUNT + " integer, "
						+ Statuses.STATUS_BG + " blob);");
				db.execSQL("insert into " + TABLE_STATUSES
						+ " select "
						+ Statuses._ID + ","
						+ Statuses.CREATED + ","
						+ "link,"
						+ "profile,"
						+ Statuses.MESSAGE + ","
						+ Statuses.SERVICE + ","
						+ "createdText,"
						+ Statuses.WIDGET + ","
						+ Sonet.INVALID_ACCOUNT_ID + ",null from " + TABLE_STATUSES + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				// create a view for the statuses and account/widget/default styles
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + " as " + Statuses_styles._ID + ","
						+ Statuses.CREATED + ","
						+ "link,"
						+ "friend,"
						+ "profile,"
						+ Statuses.MESSAGE + ","
						+ Statuses.SERVICE + ","
						+ Statuses.CREATEDTEXT + ","
						+ Statuses.WIDGET + ","
						+ Statuses.ACCOUNT + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_friend_color
						+ " else (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.FRIEND_COLOR + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_created_color
						+ " else (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.CREATED_COLOR + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_message_color
						+ " else (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.MESSAGES_COLOR + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_messages_textsize
						+ " else (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_friend_textsize
						+ " else (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_created_textsize
						+ " else (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
						+ Statuses.STATUS_BG
						+ " from " + TABLE_STATUSES);
			}
			if (oldVersion < 8) {
				// change the view to be more efficient
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + ","
						+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
						+ "link as link,"
						+ "friend as " + Statuses_styles.FRIEND + ","
						+ "profile as " + Statuses_styles.PROFILE + ","
						+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
						+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
						+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
						+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
						+ Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
						+ "(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR + ","
						+ "(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
						+ "(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
						+ "(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
						+ Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG
						+ " from " + TABLE_STATUSES);
			}
			if (oldVersion < 9) {
				// support additional timezones, with partial hour increments
				// change timezone column from integer to real
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ "timezone real, "
						+ Accounts.WIDGET + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS
						+ " select "
						+ Accounts._ID + ","
						+ Accounts.USERNAME + ","
						+ Accounts.TOKEN + ","
						+ Accounts.SECRET + ","
						+ Accounts.SERVICE + ","
						+ Accounts.EXPIRY + ","
						+ "timezone,"
						+ Accounts.WIDGET + " from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 10) {
				// add support for service icons
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer, "
						+ Widgets.ACCOUNT + " integer, "
						+ Widgets.ICON + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ","
						+ Widgets.BUTTONS_TEXTSIZE + ","
						+ Widgets.MESSAGES_TEXTSIZE + ","
						+ Widgets.FRIEND_TEXTSIZE + ","
						+ Widgets.CREATED_TEXTSIZE + ","
						+ Widgets.ACCOUNT + ",1 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("create temp table " + TABLE_STATUSES + "_bkp as select * from " + TABLE_STATUSES + ";");
				db.execSQL("drop table if exists " + TABLE_STATUSES + ";");
				db.execSQL("create table if not exists " + TABLE_STATUSES
						+ " (" + Statuses._ID + " integer primary key autoincrement, "
						+ Statuses.CREATED + " integer, "
						+ "link text, "
						+ "friend text, "
						+ "profile blob, "
						+ Statuses.MESSAGE + " text, "
						+ Statuses.SERVICE + " integer, "
						+ Statuses.CREATEDTEXT + " text, "
						+ Statuses.WIDGET + " integer, "
						+ Statuses.ACCOUNT + " integer, "
						+ Statuses.STATUS_BG + " blob, "
						+ Statuses.ICON + " blob);");
				db.execSQL("insert into " + TABLE_STATUSES
						+ " select "
						+ Statuses._ID + ","
						+ Statuses.CREATED + ","
						+ "link,"
						+ "friend,"
						+ "profile,"
						+ Statuses.MESSAGE + ","
						+ Statuses.SERVICE + ","
						+ Statuses.CREATEDTEXT + ","
						+ Statuses.WIDGET + ","
						+ Statuses.ACCOUNT + ","
						+ Statuses.STATUS_BG + ",null from " + TABLE_STATUSES + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + ","
						+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
						+ "link as link,"
						+ "friend as " + Statuses_styles.FRIEND + ","
						+ "profile as " + Statuses_styles.PROFILE + ","
						+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
						+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
						+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
						+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
						+ Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
						+ "(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR + ","
						+ "(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
						+ "(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
						+ "(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
						+ Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG + ","
						+ Statuses.ICON + " as " + Statuses_styles.ICON
						+ " from " + TABLE_STATUSES);
			}
			if (oldVersion < 11) {
				// add support for status limit
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer, "
						+ Widgets.ACCOUNT + " integer, "
						+ Widgets.ICON + " integer, "
						+ Widgets.STATUSES_PER_ACCOUNT + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ","
						+ Widgets.BUTTONS_TEXTSIZE + ","
						+ Widgets.MESSAGES_TEXTSIZE + ","
						+ Widgets.FRIEND_TEXTSIZE + ","
						+ Widgets.CREATED_TEXTSIZE + ","
						+ Widgets.ACCOUNT + ","
						+ Widgets.ICON + "," + Sonet.default_statuses_per_account + " from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				// using device timezone, doesn't need to be stored now
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ Accounts.WIDGET + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS
						+ " select "
						+ Accounts._ID + ","
						+ Accounts.USERNAME + ","
						+ Accounts.TOKEN + ","
						+ Accounts.SECRET + ","
						+ Accounts.SERVICE + ","
						+ Accounts.EXPIRY + ","
						+ Accounts.WIDGET + " from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 12) {
				// store the service id's for posting and linking
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ Accounts.WIDGET + " integer, "
						+ Accounts.SID + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS
						+ " select "
						+ Accounts._ID + ","
						+ Accounts.USERNAME + ","
						+ Accounts.TOKEN + ","
						+ Accounts.SECRET + ","
						+ Accounts.SERVICE + ","
						+ Accounts.EXPIRY + ","
						+ Accounts.WIDGET + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("create temp table " + TABLE_STATUSES + "_bkp as select * from " + TABLE_STATUSES + ";");
				db.execSQL("drop table if exists " + TABLE_STATUSES + ";");
				db.execSQL("create table if not exists " + TABLE_STATUSES
						+ " (" + Statuses._ID + " integer primary key autoincrement, "
						+ Statuses.CREATED + " integer, "
						+ Statuses.MESSAGE + " text, "
						+ Statuses.SERVICE + " integer, "
						+ Statuses.CREATEDTEXT + " text, "
						+ Statuses.WIDGET + " integer, "
						+ Statuses.ACCOUNT + " integer, "
						+ Statuses.STATUS_BG + " blob, "
						+ Statuses.ICON + " blob, "
						+ Statuses.SID + " integer, "
						+ Statuses.ENTITY + " integer);");
				db.execSQL("insert into " + TABLE_STATUSES
						+ " select "
						+ Statuses._ID + ","
						+ Statuses.CREATED + ","
						+ Statuses.MESSAGE + ","
						+ Statuses.SERVICE + ","
						+ Statuses.CREATEDTEXT + ","
						+ Statuses.WIDGET + ","
						+ Statuses.ACCOUNT + ","
						+ Statuses.STATUS_BG + ","
						+ Statuses.ICON + ",\"\",\"\" from " + TABLE_STATUSES + "_bkp;");
				db.execSQL("create table if not exists " + TABLE_ENTITIES
						+ " (" + Entities._ID + " integer primary key autoincrement, "
						+ Entities.FRIEND + " text, "
						+ Entities.PROFILE + " blob, "
						+ Entities.ACCOUNT + " integer, "
						+ Entities.ESID + " text);");
				Cursor from_bkp =  db.query(TABLE_STATUSES + "_bkp", new String[]{Statuses._ID, "friend", "profile"}, null, null, null, null, null);
				if (from_bkp.moveToFirst()) {
					int iid = from_bkp.getColumnIndex(Statuses._ID),
					ifriend = from_bkp.getColumnIndex("friend"),
					iprofile = from_bkp.getColumnIndex("profile");
					while (!from_bkp.isAfterLast()) {
						ContentValues values = new ContentValues();
						values.put(Entities.FRIEND, from_bkp.getString(ifriend));
						values.put(Entities.PROFILE, from_bkp.getBlob(iprofile));
						int id = (int) db.insert(TABLE_ENTITIES, Entities._ID, values);
						values = new ContentValues();
						values.put(Statuses.ENTITY, id);
						db.update(TABLE_STATUSES, values, Statuses._ID + "=?", new String[]{Integer.toString(from_bkp.getInt(iid))});
						from_bkp.moveToNext();
					}
				}
				from_bkp.close();
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + ","
						+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
						+ Entities.FRIEND + " as " + Statuses_styles.FRIEND + ","
						+ Entities.PROFILE + " as " + Statuses_styles.PROFILE + ","
						+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
						+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
						+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
						+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
						+ TABLE_STATUSES + "." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
						+ "(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR + ","
						+ "(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
						+ "(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
						+ "(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
						+ Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG + ","
						+ Statuses.ICON + " as " + Statuses_styles.ICON + ","
						+ Statuses.SID + " as " + Statuses_styles.SID + ","
						+ TABLE_ENTITIES + "." + Entities._ID + " as " + Statuses_styles.ENTITY + ","
						+ Entities.ESID + " as " + Statuses_styles.ESID
						+ " from " + TABLE_STATUSES + "," + TABLE_ENTITIES
						+ " where " + TABLE_ENTITIES + "." + Entities._ID + "=" + Statuses.ENTITY);
				// background updating option
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer, "
						+ Widgets.ACCOUNT + " integer, "
						+ Widgets.ICON + " integer, "
						+ Widgets.STATUSES_PER_ACCOUNT + " integer, "
						+ Widgets.BACKGROUND_UPDATE + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ","
						+ Widgets.BUTTONS_TEXTSIZE + ","
						+ Widgets.MESSAGES_TEXTSIZE + ","
						+ Widgets.FRIEND_TEXTSIZE + ","
						+ Widgets.CREATED_TEXTSIZE + ","
						+ Widgets.ACCOUNT + ","
						+ Widgets.ICON + ","
						+ Widgets.STATUSES_PER_ACCOUNT + ",1 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
			}
		}
	}

}
