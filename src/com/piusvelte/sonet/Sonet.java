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
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.BaseColumns;
import android.util.Log;

public class Sonet {

	private static final String TAG = "Sonet";

	protected static final String TOKEN = "access_token";
	protected static final String EXPIRES = "expires_in";

	protected static final int TWITTER = 0;
	protected static final String TWITTER_URL_REQUEST = "http://api.twitter.com/oauth/request_token";
	protected static final String TWITTER_URL_AUTHORIZE = "http://api.twitter.com/oauth/authorize";
	protected static final String TWITTER_URL_ACCESS = "http://api.twitter.com/oauth/access_token";
	protected static final String TWITTER_URL_FEED = "http://api.twitter.com/1/statuses/home_timeline.json?count=%s";
	protected static final String TWITTER_TWEET = "http://api.twitter.com/1/statuses/update.json";
	protected static final String TWITTER_RETWEET = "http://api.twitter.com/1/statuses/retweet/%s.json";

	protected static final int FACEBOOK = 1;
	protected static final String FACEBOOK_URL_AUTHORIZE = "https://graph.facebook.com/oauth/authorize?client_id=%s&scope=offline_access,publish_stream&type=user_agent&redirect_uri=%s&display=touch&sdk=android";
	protected static final String FACEBOOK_URL_ME = "https://graph.facebook.com/me?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_URL_FEED = "https://graph.facebook.com/me/home?date_format=U&format=json&sdk=android&limit=%s&%s=%s&fields=actions,link,type,from,message,created_time,to";
	protected static final String FACEBOOK_POST = "https://graph.facebook.com/%s/feed?%s=%s";
	protected static final String FACEBOOK_LIKES = "https://graph.facebook.com/%s/likes?%s=%s";

	protected static final String ACTION_REFRESH = "com.piusvelte.sonet.Sonet.REFRESH";
	protected static final String ACTION_BUILD_SCROLL = "com.piusvelte.sonet.Sonet.BUILD_SCROLL";
	protected static final String EXTRA_ACCOUNT_ID = "com.piusvelte.sonet.Sonet.ACCOUNT_ID";
	protected static final long INVALID_ACCOUNT_ID = -1;
	protected static final int RESULT_REFRESH = 1;

	protected static final int MYSPACE = 2;
	protected static final String MYSPACE_URL_REQUEST = "http://api.myspace.com/request_token";
	protected static final String MYSPACE_URL_AUTHORIZE = "http://api.myspace.com/authorize";
	protected static final String MYSPACE_URL_ACCESS = "http://api.myspace.com/access_token";
	protected static final String MYSPACE_URL_ME = "http://opensocial.myspace.com/1.0/people/@me/@self";
	protected static final String MYSPACE_URL_FEED = "http://opensocial.myspace.com/1.0/statusmood/@me/@friends/history?count=%s&includeself=true&fields=author,source";

	protected static final int BUZZ = 3;
	protected static final String BUZZ_URL_REQUEST = "https://www.google.com/accounts/OAuthGetRequestToken?scope=%s&xoauth_displayname=%s&domain=%s";
	protected static final String BUZZ_URL_AUTHORIZE = "https://www.google.com/buzz/api/auth/OAuthAuthorizeToken?scope=%s&xoauth_displayname=%s&domain=%s&btmpl=mobile";
	protected static final String BUZZ_URL_ACCESS = "https://www.google.com/accounts/OAuthGetAccessToken";
	protected static final String BUZZ_SCOPE = "https://www.googleapis.com/auth/buzz.readonly";
	protected static final String BUZZ_URL_ME = "https://www.googleapis.com/buzz/v1/people/@me/@self?alt=json";
	protected static final String BUZZ_URL_FEED = "https://www.googleapis.com/buzz/v1/activities/@me/@consumption?alt=json&max-results=%s";
	protected static final String BUZZ_LIKE = "https://www.googleapis.com/buzz/v1/activities/%s/@liked/%s?key=%s";
	protected static final String BUZZ_COMMENT = "https://www.googleapis.com/buzz/v1/activities/%s/@self/%s/@comments";

	protected static final int FOURSQUARE = 4;
	protected static final String FOURSQUARE_URL_ACCESS = "https://foursquare.com/oauth2/access_token";
	protected static final String FOURSQUARE_URL_AUTHORIZE = "https://foursquare.com/oauth2/authorize?client_id=%s&response_type=token&redirect_uri=%s&display=touch";
	protected static final String FOURSQUARE_URL_ME = "https://api.foursquare.com/v2/users/self?oauth_token=%s";
	protected static final String FOURSQUARE_URL_FEED = "https://api.foursquare.com/v2/checkins/recent?limit=%s&oauth_token=%s";

	protected static final int LINKEDIN = 5;
	protected static final String LINKEDIN_URL_REQUEST = "https://api.linkedin.com/uas/oauth/requestToken";
	protected static final String LINKEDIN_URL_AUTHORIZE = "https://www.linkedin.com/uas/oauth/authorize";
	protected static final String LINKEDIN_URL_ACCESS = "https://api.linkedin.com/uas/oauth/accessToken";
	protected static final String LINKEDIN_URL_ME = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name)";
	protected static final String LINKEDIN_URL_FEED = "https://api.linkedin.com/v1/people/~/network/updates?type=APPS&type=CMPY&type=CONN&type=JOBS&type=JGRP&type=PICT&type=PRFU&type=RECU&type=PRFX&type=ANSW&type=QSTN&type=SHAR&type=VIRL&count=%s";
	protected static final String[][] LINKEDIN_HEADERS = new String[][] {{"x-li-format", "json"}};
	protected static final HashMap<String, String> LINKEDIN_UPDATETYPES;

	protected static HashMap<Integer, Context> sWidgetsContext;

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

	protected static final int SALESFORCE = 6;
	protected static final String SALESFORCE_URL_REQUEST = "https://login.salesforce.com/_nc_external/system/security/oauth/RequestTokenHandler";
	protected static final String SALESFORCE_URL_AUTHORIZE = "https://login.salesforce.com/setup/secur/RemoteAccessAuthorizationPage.apexp";
	protected static final String SALESFORCE_URL_ACCESS = "https://login.salesforce.com/_nc_external/system/security/oauth/AccessTokenHandler";
	protected static final String SALESFORCE_FEED = "";

	protected static final int INVALID_SERVICE = -1;

	protected static final int default_interval = 3600000;
	protected static final int default_buttons_bg_color = -16777216;
	protected static final int default_buttons_color = -1;
	protected static final int default_message_bg_color = -16777216;
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

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/accounts");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.accounts";

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
		public static final String CREATEDTEXT = "createdtext";
		public static final String MESSAGES_COLOR = "messages_color";
		public static final String FRIEND_COLOR = "friend_color";
		public static final String CREATED_COLOR = "created_color";
		public static final String MESSAGES_TEXTSIZE = "messages_textsize";
		public static final String FRIEND_TEXTSIZE = "friend_textsize";
		public static final String CREATED_TEXTSIZE = "created_textsize";
		// account specific settings per widget
		public static final String ACCOUNT = "account";
		public static final String STATUS_BG = "status_bg";
		public static final String ICON = "icon";
		// service id, for posting and linking
		public static final String SID = "sid";
		// store friend and profile data in a separate table
		public static final String ENTITY = "entity";

	}
	
	public static final class Entities implements BaseColumns {
		
		private Entities() {
		}
		
		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/entities");
		
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.entities";
		
		public static final String SID = "sid";
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
					response = sb.toString();
				}
				break;
			default:
				Log.e(TAG,"http error:"+statusLine.getStatusCode()+" "+statusLine.getReasonPhrase());
				break;
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG,"error:" + e);
		} catch (IOException e) {
			Log.e(TAG,"error:" + e);
		}
		return response;
	}
	
	protected static String httpGet(String url) {
		return httpResponse(new HttpGet(url));
	}
	
	protected static String httpPost(String url) {
		return httpResponse(new HttpPost(url));
	}
	
	protected static String httpDelete(String url) {
		return httpResponse(new HttpDelete(url));
	}

}
