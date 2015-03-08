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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.BaseColumns;
import android.util.Log;

public class Sonet {

	private static final String TAG = "Sonet";

	protected static final String PRO = "pro";
	public static final String Saccess_token = "access_token";
	protected static final String Sexpires_in = "expires_in";
	public static final String ACTION_REFRESH = "com.piusvelte.sonet.Sonet.REFRESH";
	protected static final String ACTION_PAGE_UP = "com.piusvelte.sonet.Sonet.PAGE_UP";
	protected static final String ACTION_PAGE_DOWN = "com.piusvelte.sonet.Sonet.PAGE_DOWN";
	public static final String ACTION_ON_CLICK = "com.piusvelte.sonet.Sonet.ON_CLICK";
	public static final String ACTION_UPLOAD = "com.piusvelte.sonet.Sonet.UPLOAD";
	protected static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	protected static final String EXTRA_ACCOUNT_ID = "com.piusvelte.sonet.Sonet.ACCOUNT_ID";
	public static final String EXTRA_SCROLLABLE_VERSION = "com.piusvelte.sonet.Sonet.SCROLLABLE_VERSION";
	public static final long INVALID_ACCOUNT_ID = -1;
	protected static final int RESULT_REFRESH = 1;
	protected static int NOTIFY_ID = 1;

	protected static final int[] map_icons = new int[]{R.drawable.twitter, R.drawable.facebook, R.drawable.myspace, R.drawable.buzz, R.drawable.foursquare, R.drawable.linkedin, R.drawable.sms, R.drawable.rss, R.drawable.identica, R.drawable.googleplus, R.drawable.salesforce};

	public static final int TWITTER = 0;
	public static final String TWITTER_BASE_URL = "https://api.twitter.com/";
	protected static final String TWITTER_URL_REQUEST = "%soauth/request_token";
	protected static final String TWITTER_URL_AUTHORIZE = "%soauth/authorize";
	protected static final String TWITTER_URL_ACCESS = "%soauth/access_token";
	public static final String TWITTER_URL_FEED = "%s1.1/statuses/home_timeline.json?count=%s";
	public static final String TWITTER_RETWEET = "%s1.1/statuses/retweets/%s.json";
	public static final String TWITTER_USER = "%s1.1/users/show.json?user_id=%s";
	public static final String TWITTER_UPDATE = "%s1.1/statuses/update.json";
	public static final String TWITTER_SEARCH = "%s1.1/geo/search.json?lat=%s&long=%s";
	protected static final String TWITTER_PROFILE = "http://twitter.com/%s";
	public static final String TWITTER_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";
	public static final String TWITTER_MENTIONS = "%s1.1/statuses/mentions_timeline.json%s";
	public static final String TWITTER_SINCE_ID = "?since_id=%s";
	protected static final String TWITTER_VERIFY_CREDENTIALS = "%s1.1/account/verify_credentials.json";

	public static final int FACEBOOK = 1;
	public static final String FACEBOOK_BASE_URL = "https://graph.facebook.com/";
	protected static final String FACEBOOK_URL_AUTHORIZE = "%soauth/authorize?client_id=%s&scope=offline_access,read_stream,publish_stream,publish_checkins&type=user_agent&redirect_uri=%s&display=touch&sdk=android";
	protected static final String FACEBOOK_URL_ME = "%sme?format=json&sdk=android&%s=%s";
	public static final String FACEBOOK_HOME = "%sme/home?date_format=U&format=json&sdk=android&%s=%s&fields=actions,link,type,from,message,created_time,to,comments,story,source,picture";
	public static final String FACEBOOK_POST = "%sme/feed?format=json&sdk=android&%s=%s";
//	protected static final String FACEBOOK_CHECKIN = "%sme/checkins?format=json&sdk=android&%s=%s";
	public static final String FACEBOOK_LIKES = "%s%s/likes?format=json&sdk=android&%s=%s";
	public static final String FACEBOOK_COMMENTS = "%s%s/comments?date_format=U&format=json&sdk=android&%s=%s";
	public static final String FACEBOOK_SEARCH = "%ssearch?type=place&center=%s,%s&distance=1000&format=json&sdk=android&%s=%s";
//	protected static final String FACEBOOK_COORDINATES = "{\"latitude\":\"%s\",\"longitude\":\"%s\"}";
	protected static final String FACEBOOK_USER = "%s%s?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_PHOTOS = "%sme/photos?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_FRIENDS = "%sme/friends?format=json&sdk=android&%s=%s";
	public static final String FACEBOOK_PICTURE = "http://graph.facebook.com/%s/picture";

	public static final int MYSPACE = 2;
	public static final String MYSPACE_BASE_URL = "http://api.myspace.com/1.0/";
	protected static final String MYSPACE_URL_REQUEST = "http://api.myspace.com/request_token";
	protected static final String MYSPACE_URL_AUTHORIZE = "http://api.myspace.com/authorize";
	protected static final String MYSPACE_URL_ACCESS = "http://api.myspace.com/access_token";
	protected static final String MYSPACE_URL_ME = "%speople/@me/@self";
	public static final String MYSPACE_HISTORY = "%sstatusmood/@me/@friends/history?includeself=true&fields=author,source,recentComments";
	public static final String MYSPACE_URL_STATUSMOOD = "%sstatusmood/@me/@self";
	public static final String MYSPACE_URL_STATUSMOODCOMMENTS = "%sstatusmoodcomments/%s/@self/%s?format=json&includeself=true&fields=author";
	public static final String MYSPACE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	public static final String MYSPACE_STATUSMOOD_BODY = "{\"status\":\"%s\"}";
	public static final String MYSPACE_STATUSMOODCOMMENTS_BODY = "{\"body\":\"%s\"}";
	protected static final String MYSPACE_USER = "%speople/%s/@self";

	public static final int FOURSQUARE = 4;
	public static final String FOURSQUARE_BASE_URL = "https://api.foursquare.com/v2/";
	protected static final String FOURSQUARE_URL_AUTHORIZE = "https://foursquare.com/oauth2/authorize?client_id=%s&response_type=token&redirect_uri=%s&display=touch";
	protected static final String FOURSQUARE_URL_ME = "%susers/self?oauth_token=%s";
	protected static final String FOURSQUARE_URL_USER = "%susers/%s?oauth_token=%s";
	protected static final String FOURSQUARE_URL_PROFILE = "https://foursquare.com/user/%s";
	public static final String FOURSQUARE_CHECKINS = "%scheckins/recent?oauth_token=%s";
	public static final String FOURSQUARE_CHECKIN = "%scheckins/add?venueId=%s&shout=%s&ll=%s,%s&broadcast=public&oauth_token=%s";
	public static final String FOURSQUARE_CHECKIN_NO_VENUE = "%scheckins/add?shout=%s&broadcast=public&oauth_token=%s";
	public static final String FOURSQUARE_CHECKIN_NO_SHOUT = "%scheckins/add?venueId=%s&ll=%s,%s&broadcast=public&oauth_token=%s";
	public static final String FOURSQUARE_ADDCOMMENT = "%scheckins/%s/addcomment?text=%s&oauth_token=%s";
	public static final String FOURSQUARE_SEARCH = "%svenues/search?ll=%s,%s&intent=checkin&oauth_token=%s";
	public static final String FOURSQUARE_GET_CHECKIN = "%scheckins/%s?oauth_token=%s";

	public static final int LINKEDIN = 5;
	public static final String LINKEDIN_BASE_URL = "https://api.linkedin.com/v1/people/~";
	protected static final String LINKEDIN_URL_REQUEST = "https://api.linkedin.com/uas/oauth/requestToken";
	protected static final String LINKEDIN_URL_AUTHORIZE = "https://www.linkedin.com/uas/oauth/authorize";
	protected static final String LINKEDIN_URL_ACCESS = "https://api.linkedin.com/uas/oauth/accessToken";
	protected static final String LINKEDIN_URL_ME = "%s:(id,first-name,last-name)";
	protected static final String LINKEDIN_URL_USER = "https://api.linkedin.com/v1/people/id=%s";
	public static final String LINKEDIN_UPDATES = "%s/network/updates?type=APPS&type=CMPY&type=CONN&type=JOBS&type=JGRP&type=PICT&type=PRFU&type=RECU&type=PRFX&type=ANSW&type=QSTN&type=SHAR&type=VIRL";
	public static final String[][] LINKEDIN_HEADERS = new String[][] {{"x-li-format", "json"}};
	public static final String LINKEDIN_IS_LIKED = "%s/network/updates/key=%s/is-liked";
	public static final String LINKEDIN_UPDATE = "%s/network/updates/key=%s";
	public static final String LINKEDIN_UPDATE_COMMENTS = "%s/network/updates/key=%s/update-comments";
	public static final String LINKEDIN_POST = "%s/person-activities";
	public static final String LINKEDIN_POST_BODY = "<?xml version='1.0' encoding='UTF-8'?><activity locale=\"%s\"><content-type>linkedin-html</content-type><body>%s</body></activity>";
	public static final String LINKEDIN_COMMENT_BODY = "<?xml version='1.0' encoding='UTF-8'?><update-comment><comment>%s</comment></update-comment>";
	public static final String LINKEDIN_LIKE_BODY = "<?xml version='1.0' encoding='UTF-8'?><is-liked>%s</is-liked>";

	protected static final int SMS = 6;
	public static final int RSS = 7;

	public static final int IDENTICA = 8;
	public static final String IDENTICA_BASE_URL = "https://identi.ca/api/";
	protected static final String IDENTICA_URL_REQUEST = "%soauth/request_token";
	protected static final String IDENTICA_URL_AUTHORIZE = "%soauth/authorize";
	protected static final String IDENTICA_URL_ACCESS = "%soauth/access_token";
	public static final String IDENTICA_URL_FEED = "%sstatuses/home_timeline.json?count=%s";
	public static final String IDENTICA_RETWEET = "%sstatuses/retweet/%s.json";
	protected static final String IDENTICA_USER = "%susers/show.json?user_id=%s";
	public static final String IDENTICA_UPDATE = "%sstatuses/update.json";
	protected static final String IDENTICA_PROFILE = "http://identi.ca/%s";
	protected static final String IDENTICA_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";
	public static final String IDENTICA_MENTIONS = "%sstatuses/mentions.json%s";
	public static final String IDENTICA_SINCE_ID = "?since_id=%s";

	public static final int GOOGLEPLUS = 9;
	protected static final String GOOGLEPLUS_AUTHORIZE = "https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&scope=https://www.googleapis.com/auth/plus.me&response_type=code";
	public static final String GOOGLE_ACCESS = "https://accounts.google.com/o/oauth2/token";
	public static final String GOOGLEPLUS_BASE_URL = "https://www.googleapis.com/plus/v1/";
	protected static final String GOOGLEPLUS_URL_ME = "%speople/me?fields=displayName,id&access_token=%s";
	public static final String GOOGLEPLUS_ACTIVITIES = "%speople/%s/activities/%s?maxResults=%s&access_token=%s";
	public static final String GOOGLEPLUS_ACTIVITY = "%sactivities/%s?access_token=%s";
	protected static final String GOOGLEPLUS_PROFILE = "https://plus.google.com/%s";
	public static final String GOOGLEPLUS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	public static final int PINTEREST = 10;
	public static final String PINTEREST_BASE_URL = "https://api.pinterest.com/v2/";
	public static final String PINTEREST_URL_FEED = "%spopular/";
	protected static final String PINTEREST_PIN = "https://pinterest.com/pin/%s/";
	protected static final String PINTEREST_PROFILE = "https://pinterest.com/%s/";
	public static final String PINTEREST_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

	public static final int CHATTER = 11;
	protected static final String CHATTER_URL_AUTHORIZE = "https://login.salesforce.com/services/oauth2/authorize?response_type=token&display=touch&client_id=%s&redirect_uri=%s";
	public static final String CHATTER_URL_ACCESS = "https://login.salesforce.com/services/oauth2/token?grant_type=refresh_token&client_id=%s&refresh_token=%s";
	protected static final String CHATTER_URL_ME = "%s/services/data/v22.0/chatter/users/me";
	public static final String CHATTER_URL_POST = "%s/services/data/v22.0/chatter/feeds/news/me/feed-items?text=%s";
	public static final String CHATTER_URL_COMMENT = "%s/services/data/v22.0/chatter/feed-items/%s/comments?text=%s";
	public static final String CHATTER_URL_FEED = "%s/services/data/v22.0/chatter/feeds/news/me/feed-items";
	public static final String CHATTER_URL_LIKES = "%s/services/data/v22.0/chatter/feed-items/%s/likes";
	public static final String CHATTER_URL_LIKE = "%s/services/data/v22.0/chatter/likes/%s";
	public static final String CHATTER_URL_COMMENTS = "%s/services/data/v22.0/chatter/feed-items/%s/comments";
	public static final String CHATTER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	protected static final String AM = "a.m.";
	protected static final String PM = "p.m.";
	protected static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

	protected static final String ACCOUNTS_QUERY = "(case when " + Accounts.SERVICE + "=" + TWITTER + " then 'Twitter: ' when "
			+ Accounts.SERVICE + "=" + FACEBOOK + " then 'Facebook: ' when "
			+ Accounts.SERVICE + "=" + MYSPACE + " then 'MySpace: ' when "
			+ Accounts.SERVICE + "=" + LINKEDIN + " then 'LinkedIn: ' when "
			+ Accounts.SERVICE + "=" + FOURSQUARE + " then 'Foursquare: ' when "
			+ Accounts.SERVICE + "=" + CHATTER + " then 'Chatter: ' when "
			+ Accounts.SERVICE + "=" + RSS + " then 'RSS: ' when "
			+ Accounts.SERVICE + "=" + IDENTICA + " then 'Identi.ca: ' when "
			+ Accounts.SERVICE + "=" + GOOGLEPLUS + " then 'Google+: ' when "
			+ Accounts.SERVICE + "=" + PINTEREST + " then 'Pinterest: ' else '' end)||" + Accounts.USERNAME + " as " + Accounts.USERNAME;

	public static final String Sid = "id";
	public static final String Sname = "name";
	public static final String Suser = "user";
	public static final String Screated_at = "created_at";
	public static final String Sprofile_image_url = "profile_image_url";
	public static final String Stext = "text";
	public static final String Sdata = "data";
	public static final String Screated_time = "created_time";
	public static final String Sfrom = "from";
	public static final String Stype = "type";
	public static final String Smessage = "message";
	public static final String Sto = "to";
	public static final String Slink = "link";
	public static final String Sstatus = "status";
	public static final String Scomment = "comment";
	public static final String Scomments = "comments";
	public static final String Sperson = "person";
	public static final String Svalues = "values";
	public static final String SupdateComments = "updateComments";
	public static final String SupdateKey = "updateKey";
	public static final String Stimestamp = "timestamp";
	public static final String ScurrentShare = "currentShare";
	public static final String SupdateType = "updateType";
	public static final String SupdateContent = "updateContent";
	public static final String SpersonActivities = "personActivities";
	public static final String Sconnections = "connections";
	public static final String S_total = "_total";
	public static final String Svenue = "venue";
	public static final String Srecent = "recent";
	public static final String SrecommendationsGiven = "recommendationsGiven";
	public static final String Sjob = "job";
	public static final String Sposition = "position";
	public static final String SmemberGroups = "memberGroups";

	public enum LinkedIn_UpdateTypes {
		ANSW("updated an answer"),
		APPS("updated the application "),
		CMPY("company update"),
		CONN("is now connected to "),
		JOBP("posted the job "),
		JGRP("joined the group "),
		PRFX("updated their extended profile"),
		PREC("recommends "),
		PROF("changed their profile"),
		QSTN("updated a question"),
		SHAR("shared something"),
		VIRL("updated the viral "),
		PICU("updated their profile picture");

		public String message = null;

		private LinkedIn_UpdateTypes(String message) {
			this.message = message;
		}

		public static boolean contains(String type) {
			for (LinkedIn_UpdateTypes t : LinkedIn_UpdateTypes.values()) {
				if (t.name().equals(type))
					return true;
			}
			return false;
		}

		public static String getMessage(String type) {
			for (LinkedIn_UpdateTypes t : LinkedIn_UpdateTypes.values()) {
				if (t.name().equals(type))
					return t.message;
			}
			return null;
		}
	}
	public static final String Sauthor = "author";
	public static final String Sentry = "entry";
	public static final String SpostedDate = "postedDate";
	public static final String SdisplayName = "displayName";
	public static final String Sphoto = "photo";
	public static final String SsmallPhotoUrl = "smallPhotoUrl";
	public static final String SmoodStatusLastUpdated = "moodStatusLastUpdated";
	public static final String SthumbnailUrl = "thumbnailUrl";
	public static final String SrecentComments = "recentComments";
	public static final String SuserId = "userId";
	public static final String SstatusId = "statusId";
	public static final String Sitem = "item";
	public static final String Sitems = "items";
	public static final String Sobject = "object";
	public static final String Spublished = "published";
	public static final String Sinstance_url = "instance_url";
	public static final String Scontent = "content";
	public static final String SoriginalContent = "originalContent";
	public static final String Sreplies = "replies";
	public static final String Simage = "image";
	public static final String Surl = "url";
	public static final String Sactor = "actor";
	public static final String StotalItems = "totalItems";
	public static final String ScreatedDate = "createdDate";
	public static final String Sbody = "body";
	public static final String Stotal = "total";
	public static final String ScreatedAt = "createdAt";
	public static final String SfirstName = "firstName";
	public static final String SlastName = "lastName";
	public static final String Sresponse = "response";
	public static final String Scheckin = "checkin";
	public static final String Sshout = "shout";
	public static final String Stitle = "title";
	public static final String Sdescription = "description";
	public static final String Spubdate = "pubdate";
	public static final String SpictureUrl = "pictureUrl";
	public static final String SisCommentable = "isCommentable";
	public static final String SrecommendationSnippet = "recommendationSnippet";
	public static final String Srecommendee = "recommendee";
	public static final String Sscreen_name = "screen_name";
	public static final String Sin_reply_to_status_id = "in_reply_to_status_id";
	public static final String Suser_likes = "user_likes";
	public static final String ScommentId = "commentId";
	public static final String Sgroups = "groups";
	public static final String SNearby = "Nearby";
	public static final String Splaces = "places";
	public static final String Sresult = "result";
	public static final String Sfull_name = "full_name";
	public static final String Ssource = "source";
	public static final String Sstory = "story";
	public static final String Smobile = "mobile";
	public static final String Simage_url = "image_url";
	public static final String Scounts = "counts";
	public static final String Simages = "images";
	public static final String Susername = "username";
	public static final String Spicture = "picture";
	public static final String Sboard = "board";
	public static final String Simgur = "i.imgur.com";
	public static final String Splace = "place";
	public static final String Stags = "tags";

	private static final String POWER_SERVICE = Context.POWER_SERVICE;
	private static WakeLock sWakeLock;

	static boolean hasLock() {
		return (sWakeLock != null);
	}

	public static void acquire(Context context) {
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

	protected static final int INVALID_SERVICE = -1;

	protected static final int default_interval = 3600000;
	protected static final int default_buttons_bg_color = 0x88000000;
	protected static final int default_buttons_color = 0xFFFFFFFF;
	public static final int default_message_bg_color = 0x88FFFFFF;
	protected static final int default_message_color = 0xFF000000;
	protected static final int default_friend_color = 0xFFFFFFFF;
	protected static final int default_created_color = 0xFFFFFFFF;
	protected static final int default_buttons_textsize = 14;
	protected static final int default_messages_textsize = 14;
	protected static final int default_friend_textsize = 14;
	protected static final int default_created_textsize = 14;
	protected static final int default_statuses_per_account = 10;
	protected static final boolean default_include_profile = true;
	protected static final int default_margin = 0;
	public static final int default_friend_bg_color = 0x88000000;
	protected static final boolean default_hasButtons = false;
	protected static final boolean default_time24hr = false;
	protected static final boolean default_hasIcon = true;
	protected static final boolean default_backgroundUpdate = true;
	protected static final boolean default_sound = false;
	protected static final boolean default_vibrate = false;
	protected static final boolean default_lights = false;
	protected static final boolean default_instantUpload = false;

	public Sonet() {
	}

	public static String getAuthority(Context context) {
		return !context.getPackageName().toLowerCase().contains(PRO) ? SonetProvider.AUTHORITY : SonetProvider.PRO_AUTHORITY;
	}

	public static final class Accounts implements BaseColumns {

		private Accounts() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/accounts");
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.accounts";

		public static final String USERNAME = "username";
		public static final String TOKEN = "token";
		public static final String SECRET = "secret";
		public static final String SERVICE = "service";
		public static final String EXPIRY = "expiry";
		// service id for posting and linking
		public static final String SID = "sid";

	}

	public static final class Widget_accounts implements BaseColumns {

		private Widget_accounts() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/widget_accounts");
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widget_accounts";

		public static final String ACCOUNT = "account";
		public static final String WIDGET = "widget";
	}

	public static final class Widget_accounts_view implements BaseColumns {

		private Widget_accounts_view() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/widget_accounts_view");
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widget_accounts_view";

		public static final String ACCOUNT = "account";
		public static final String WIDGET = "widget";
		public static final String USERNAME = "username";
		public static final String TOKEN = "token";
		public static final String SECRET = "secret";
		public static final String SERVICE = "service";
		public static final String EXPIRY = "expiry";
		public static final String SID = "sid";
	}

	public static final class Widgets implements BaseColumns {

		private Widgets() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/widgets");
		}

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
		public static final String SOUND = "sound";
		public static final String VIBRATE = "vibrate";
		public static final String LIGHTS = "lights";
		public static final String DISPLAY_PROFILE = "display_profile";
		public static final String INSTANT_UPLOAD = "instant_upload";
		public static final String MARGIN = "margin";
		public static final String PROFILES_BG_COLOR = "profiles_bg_color";
		public static final String FRIEND_BG_COLOR = "friend_bg_color";

	}

	public static final class Widgets_settings implements BaseColumns  {

		private Widgets_settings() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/widgets_settings");
		}

		public static Uri getDistinctContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/distinct_widgets_settings");
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widgets_settings";

	}

	public static final class Accounts_styles implements BaseColumns  {

		private Accounts_styles() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/accounts_styles");
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.accounts_styles";

	}

	public static final class Statuses implements BaseColumns {

		private Statuses() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/statuses");
		}

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
		public static final String FRIEND_OVERRIDE = "friend_override";
		public static final String PROFILE_BG = "profiles_bg_color";
		public static final String FRIEND_BG = "friend_bg";

	}

	public static final class Statuses_styles implements BaseColumns {

		// this is actually a view, joining the account/widget/default styles to the statuses

		private Statuses_styles() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/statuses_styles");
		}

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
		public static final String PROFILE_BG = "profiles_bg_color";
		public static final String FRIEND_BG = "friend_bg";
		public static final String IMAGE_BG = "image_bg";
		public static final String IMAGE = "image";

	}

	public static final class Entities implements BaseColumns {

		private Entities() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/entities");
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.entities";

		public static final String ESID = "esid";
		public static final String FRIEND = "friend";
		public static final String PROFILE = "profile";
		public static final String ACCOUNT = "account";

	}

	public static final class Notifications implements BaseColumns {
		// store notifications
		// notifications are marked cleared when viewed
		// notifications are deleted when the feeds are updated, they are not in the new feeds and are marked cleared
		private Notifications() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/notifications");
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.notifications";
		public static final String SID = "sid";
		public static final String ESID = "esid";
		public static final String FRIEND = "friend";
		public static final String MESSAGE = "message";
		public static final String CREATED = "created";
		public static final String ACCOUNT = "account";
		public static final String NOTIFICATION = "notification";
		public static final String CLEARED = "cleared";
		public static final String UPDATED = "updated";
	}

	public static final class Status_links implements BaseColumns  {

		private Status_links() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/status_links");
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.status_links";
		public static final String STATUS_ID = "status_id";
		public static final String LINK_URI = "link_uri";
		public static final String LINK_TYPE = "link_type";

	}

	public static final class Status_images implements BaseColumns {

		private Status_images() {
		}

		public static Uri getContentUri(Context context) {
			return Uri.parse("content://" + Sonet.getAuthority(context) + "/status_images");
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.status_images";
		public static final String STATUS_ID = "status_id";
		public static final String IMAGE = "image";
		public static final String IMAGE_BG = "image_bg";

	}

    public static final TimeZone sTimeZone = TimeZone.getTimeZone("GMT");

    public static final String[] sRFC822 = {"EEE, d MMM yy HH:mm:ss z", "EEE, d MMM yy HH:mm z", "EEE, d MMM yyyy HH:mm:ss z", "EEE, d MMM yyyy HH:mm z", "d MMM yy HH:mm z", "d MMM yy HH:mm:ss z", "d MMM yyyy HH:mm z", "d MMM yyyy HH:mm:ss z"};

    public static String getCreatedText(long epoch, boolean time24hr) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(epoch);
		Calendar todayCal = Calendar.getInstance();
		todayCal.setTimeInMillis(System.currentTimeMillis());
		int hours = calendar.get(Calendar.HOUR_OF_DAY);
		// check if the date is from the same day
		if ((calendar.get(Calendar.ERA) == todayCal.get(Calendar.ERA)) && (calendar.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)) && (calendar.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR))) {
			if (time24hr) {
				return String.format("%d:%02d", hours, calendar.get(Calendar.MINUTE));
			} else {
				// set am/pm
				if (hours == 0) {
					return String.format("%d:%02d%s", 12, calendar.get(Calendar.MINUTE), Sonet.AM);
				} else if (hours < 12) {
					return String.format("%d:%02d%s", hours, calendar.get(Calendar.MINUTE), Sonet.AM);
				} else if (hours == 12) {
					return String.format("%d:%02d%s", hours, calendar.get(Calendar.MINUTE), Sonet.PM);
				} else {
					return String.format("%d:%02d%s", hours - 12, calendar.get(Calendar.MINUTE), Sonet.PM);
				}
			}
		} else {
			return String.format("%s %d", Sonet.MONTHS[calendar.get(Calendar.MONTH)], calendar.get(Calendar.DATE));
		}
	}

    public static Class getPackageClass(Context context, Class cls) {
		try {
			return Class.forName(context.getPackageName() + "." + cls.getSimpleName());
		} catch (ClassNotFoundException e) {
			Log.e(TAG, e.getMessage());
		}
		return cls;
	}

    public static Intent getPackageIntent(Context context, Class cls) {
		return new Intent(context, getPackageClass(context, cls));
	}

    public static int[] getWidgets(Context context, AppWidgetManager awm) {
		int[] widgets = new int[0];
		Class[] clazzes = new Class[]{SonetWidget_2x2.class, SonetWidget_2x3.class, SonetWidget_2x4.class, SonetWidget_4x2.class, SonetWidget_4x3.class, SonetWidget_4x4.class};
		for (Class clazz : clazzes) {
			widgets = Sonet.arrayCat(widgets, awm.getAppWidgetIds(new ComponentName(context, getPackageClass(context, clazz))));
		}
		return widgets;
	}

    public static String getServiceName(Resources r, int service) {
		String name = null;
		String[] entries = r.getStringArray(R.array.service_entries);
		String[] values = r.getStringArray(R.array.service_values);
		for (int i = 0, l = values.length; (i < l) && (name == null); i++) {
			if (Integer.toString(service).equals(values[i]))
				name = entries[i];
		}
		return name;
	}

    public static int[] arrayCat(int[] a, int[] b) {
		int[] c;
		for (int i = 0, i2 = b.length; i < i2; i++) {
			int cLen = a.length;
			c = new int[cLen];
			for (int n = 0; n < cLen; n++)
				c[n] = a[n];
			a = new int[cLen + 1];
			for (int n = 0; n < cLen; n++)
				a[n] = c[n];
			a[cLen] = b[i];
		}
		return a;
	}

    public static int[] arrayAdd(int[] a, int b) {
		if (!Sonet.arrayContains(a, b)) {
			int cLen = a.length;
			int[] c = new int[cLen];
			for (int i = 0; i < cLen; i++)
				c[i] = a[i];
			a = new int[cLen + 1];
			for (int i = 0; i < cLen; i++)
				a[i] = c[i];
			a[cLen] = b;
		}
		return a;
	}

    public static boolean arrayContains(int[] a, int b) {
		for (int c : a) {
			if (c == b)
				return true;
		}
		return false;
	}

    public static int[] arrayRemove(int[] a, int b) {
		if (Sonet.arrayContains(a, b)) {
			int[] c = new int[a.length - 1];
			int i = 0;
			for (int d : a) {
				if (d != b)
					c[i++] = d;
			}
			return c;
		} else
			return a;
	}

    public static int arrayIndex(int[] a, int b) {
		int c = -1;
		for (int i = 0, i2 = a.length; (i < i2) && (c == -1); i++) {
			if (a[i] == b)
				c = i;
		}
		return c;
	}

	protected static int arrayIndex(long[] a, long b) {
		int c = -1;
		for (int i = 0, i2 = a.length; (i < i2) && (c == -1); i++) {
			if (a[i] == b)
				c = i;
		}
		return c;
	}

    public static BitmapFactory.Options sBFOptions = new BitmapFactory.Options();

	static {
		sBFOptions.inDither = false;
		sBFOptions.inPurgeable = true; // allow this memory to be reclaimed
		sBFOptions.inInputShareable = true; // share the reference, rather than copy
		sBFOptions.inTempStorage = new byte[32 * 1024]; // allocate temporary memory
	}

    public static boolean HasValues(String[] values) {
		boolean hasValues = values != null;
		if (hasValues) {
			for (String value : values) {
				if (value == null) {
					hasValues = false;
					break;
				}
			}
		}
		return hasValues;
	}

    public static byte[] getBlob(InputStream is) {
		Bitmap bmp = BitmapFactory.decodeStream(is, null, sBFOptions);
		if (bmp != null) {
			ByteArrayOutputStream blob = new ByteArrayOutputStream();
			bmp.compress(Bitmap.CompressFormat.PNG, 100, blob);
			bmp.recycle();
			return blob.toByteArray();
		}
		return null;
	}

    public static byte[] getBlob(Resources r, int i) {
		Bitmap bmp = BitmapFactory.decodeResource(r, i, sBFOptions);
		if (bmp != null) {
			ByteArrayOutputStream blob = new ByteArrayOutputStream();
			bmp.compress(Bitmap.CompressFormat.PNG, 100, blob);
			bmp.recycle();
			return blob.toByteArray();
		}
		return null;
	}

    public static Matcher getLinksMatcher(String raw) {
		return Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(raw);
	}

    public static byte[] createBackground(int color) {
		Bitmap b = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
		Canvas c = new Canvas(b);
		c.drawColor(color);
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		b.compress(Bitmap.CompressFormat.PNG, 100, s);
		byte[] bg = s.toByteArray();
		if (b != null) {
			b.recycle();
			b = null;
		}
		return bg;
	}

    public static String initAccountSettings(Context context, int widget, long account) {
		ContentValues values = new ContentValues();
		values.put(Widgets.WIDGET, widget);
		values.put(Widgets.ACCOUNT, account);
		return context.getContentResolver().insert(Widgets.getContentUri(context), values).getLastPathSegment();
	}

	public static int getCropSize(int src, int dst) {
		return (int) Math.round((src - dst) / 2.0);
	}

    public static boolean insertStatusImageBg(Context context, long statusId, byte[] bImg, int height) {
		Bitmap bmpBg = Bitmap.createBitmap(1, height, Config.ARGB_8888);
		ByteArrayOutputStream baosBg = new ByteArrayOutputStream();
		bmpBg.compress(Bitmap.CompressFormat.PNG, 100, baosBg);
		byte[] bBg = baosBg.toByteArray();
		bmpBg.recycle();
		bmpBg = null;

		if (bBg != null) {
			ContentValues imageValues = new ContentValues();
			imageValues.put(Status_images.STATUS_ID, statusId);
			imageValues.put(Status_images.IMAGE, (bImg != null ? bImg : bBg));
			imageValues.put(Status_images.IMAGE_BG, bBg);
			context.getContentResolver().insert(Status_images.getContentUri(context), imageValues);
			return true;
		}

		return false;
	}

}
