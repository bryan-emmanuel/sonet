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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.Uri;
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
	protected static final String TWITTER_FEED = "http://api.twitter.com/1/statuses/home_timeline.json";

	protected static final int FACEBOOK = 1;
	protected static final String[] FACEBOOK_PERMISSIONS = new String[] {"offline_access"};
    protected static final String FACEBOOK_URL_AUTHORIZE = "https://graph.facebook.com/oauth/authorize";
    protected static final String GRAPH_BASE_URL = "https://graph.facebook.com/";

    protected static final String ACTION_REFRESH = "com.piusvelte.sonet.Sonet.REFRESH";
    protected static final String ACTION_BUILD_SCROLL = "com.piusvelte.sonet.Sonet.BUILD_SCROLL";
    protected static final String EXTRA_ACCOUNT_ID = "com.piusvelte.sonet.Sonet.ACCOUNT_ID";
    protected static final long INVALID_ACCOUNT_ID = -1;
    protected static final String ACTION_UPDATE_SETTINGS = "com.piusvelte.sonet.Sonet.UPDATE_SETTINGS";

	protected static final int MYSPACE = 2;
    protected static final String MYSPACE_URL_REQUEST = "http://api.myspace.com/request_token";
    protected static final String MYSPACE_URL_AUTHORIZE = "http://api.myspace.com/authorize";
    protected static final String MYSPACE_URL_ACCESS = "http://api.myspace.com/access_token";
    protected static final String MYSPACE_FEED = "http://opensocial.myspace.com/1.0/statusmood/@me/@friends/history?includeself=true&fields=author,source";

    protected static final int BUZZ = 3;
    protected static final String BUZZ_URL_REQUEST = "https://www.google.com/accounts/OAuthGetRequestToken";
    protected static final String BUZZ_URL_AUTHORIZE = "https://www.google.com/buzz/api/auth/OAuthAuthorizeToken";
    protected static final String BUZZ_URL_ACCESS = "https://www.google.com/accounts/OAuthGetAccessToken";
    protected static final String BUZZ_SCOPE = "https://www.googleapis.com/auth/buzz.readonly";
    protected static final String BUZZ_FEED = "https://www.googleapis.com/buzz/v1/activities/@me/@consumption?alt=json";
    
    protected static final int FOURSQUARE = 4;
    protected static final String FOURSQUARE_URL_ACCESS = "https://foursquare.com/oauth2/access_token";
    protected static final String FOURSQUARE_URL_AUTHORIZE = "https://foursquare.com/oauth2/authorize";
    
    protected static final int LINKEDIN = 5;
    protected static final String LINKEDIN_URL_REQUEST = "";
    protected static final String LINKEDIN_URL_AUTHORIZE = "";
    protected static final String LINKEDIN_URL_ACCESS = "";
	
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
		public static final String TIMEZONE = "timezone";
		public static final String WIDGET = "widget";

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

	}

	public static final class Statuses implements BaseColumns {

		private Statuses() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/statuses");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.statuses";

		public static final String CREATED = "created";
		public static final String LINK = "link";
		public static final String FRIEND = "friend";
		public static final String PROFILE = "profile";
		public static final String MESSAGE = "message";
		public static final String SERVICE = "service";
		public static final String WIDGET = "widget";
		public static final String CREATEDTEXT = "createdtext";
		// account specific settings per widget
		public static final String ACCOUNT = "account";
		public static final String STATUS_BG = "status_bg";
		public static final String ICON = "icon";

	}

	public static final class Statuses_styles implements BaseColumns {

		// this is actually a view, joining the account/widget/default styles to the statuses

		private Statuses_styles() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/statuses_styles");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.statuses_styles";

		public static final String CREATED = "created";
		public static final String LINK = "link";
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

	}

	protected static String httpGet(String url) {
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse httpResponse;
		String response = null;
		try {
			httpResponse = httpClient.execute(new HttpGet(url));
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
				Log.e(TAG,"get error:"+statusLine.getStatusCode()+" "+statusLine.getReasonPhrase());
				break;
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG,"error:" + e);
		} catch (IOException e) {
			Log.e(TAG,"error:" + e);
		}
		return response;
	}

}
