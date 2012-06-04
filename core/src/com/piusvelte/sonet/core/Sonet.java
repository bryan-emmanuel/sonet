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
package com.piusvelte.sonet.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.BaseColumns;

public class Sonet {

	private static final String TAG = "Sonet";

	protected static final String PRO = "pro";
	protected static final String Saccess_token = "access_token";
	protected static final String Sexpires_in = "expires_in";
	protected static final String ACTION_REFRESH = "com.piusvelte.sonet.Sonet.REFRESH";
	protected static final String ACTION_PAGE_UP = "com.piusvelte.sonet.Sonet.PAGE_UP";
	protected static final String ACTION_PAGE_DOWN = "com.piusvelte.sonet.Sonet.PAGE_DOWN";
	protected static final String ACTION_ON_CLICK = "com.piusvelte.sonet.Sonet.ON_CLICK";
	protected static final String ACTION_UPLOAD = "com.piusvelte.sonet.Sonet.UPLOAD";
	protected static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	protected static final String EXTRA_ACCOUNT_ID = "com.piusvelte.sonet.Sonet.ACCOUNT_ID";
	protected static final String EXTRA_SCROLLABLE_VERSION = "com.piusvelte.sonet.Sonet.SCROLLABLE_VERSION";
	protected static final long INVALID_ACCOUNT_ID = -1;
	protected static final int RESULT_REFRESH = 1;
	protected static int NOTIFY_ID = 1;
	protected static final Object[] sDatabaseLock = new Object[0];

	protected static final int[] map_icons = new int[]{R.drawable.twitter, R.drawable.facebook, R.drawable.myspace, R.drawable.buzz, R.drawable.foursquare, R.drawable.linkedin, R.drawable.sms, R.drawable.rss, R.drawable.identica, R.drawable.googleplus, R.drawable.salesforce};

	protected static final int TWITTER = 0;
	protected static final String TWITTER_BASE_URL = "http://api.twitter.com/";
	protected static final String TWITTER_URL_REQUEST = "%soauth/request_token";
	protected static final String TWITTER_URL_AUTHORIZE = "%soauth/authorize";
	protected static final String TWITTER_URL_ACCESS = "%soauth/access_token";
	protected static final String TWITTER_URL_FEED = "%s1/statuses/home_timeline.json?count=%s";
	protected static final String TWITTER_RETWEET = "%s1/statuses/retweet/%s.json";
	protected static final String TWITTER_USER = "%s1/users/show.json?user_id=%s";
	protected static final String TWITTER_UPDATE = "%s1/statuses/update.json";
	protected static final String TWITTER_SEARCH = "%s1/geo/search.json?lat=%s&long=%s";
	protected static final String TWITTER_PROFILE = "http://twitter.com/%s";
	protected static final String TWITTER_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";
	protected static final String TWITTER_MENTIONS = "%s1/statuses/mentions.json%s";
	protected static final String TWITTER_SINCE_ID = "?since_id=%s";

	protected static final int FACEBOOK = 1;
	protected static final String FACEBOOK_BASE_URL = "https://graph.facebook.com/";
	protected static final String FACEBOOK_URL_AUTHORIZE = "%soauth/authorize?client_id=%s&scope=offline_access,read_stream,publish_stream,publish_checkins&type=user_agent&redirect_uri=%s&display=touch&sdk=android";
	protected static final String FACEBOOK_URL_ME = "%sme?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_HOME = "%sme/home?date_format=U&format=json&sdk=android&%s=%s&fields=actions,link,type,from,message,created_time,to,comments,story,source,picture";
	protected static final String FACEBOOK_POST = "%sme/feed?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_CHECKIN = "%sme/checkins?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_LIKES = "%s%s/likes?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_COMMENTS = "%s%s/comments?date_format=U&format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_SEARCH = "%ssearch?type=place&center=%s,%s&distance=1000&format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_COORDINATES = "{\"latitude\":\"%s\",\"longitude\":\"%s\"}";
	protected static final String FACEBOOK_USER = "%s%s?format=json&sdk=android&%s=%s";
	protected static final String FACEBOOK_PHOTOS = "%sme/photos?format=json&sdk=android&%s=%s";

	protected static final int MYSPACE = 2;
	protected static final String MYSPACE_BASE_URL = "http://api.myspace.com/1.0/";
	protected static final String MYSPACE_URL_REQUEST = "http://api.myspace.com/request_token";
	protected static final String MYSPACE_URL_AUTHORIZE = "http://api.myspace.com/authorize";
	protected static final String MYSPACE_URL_ACCESS = "http://api.myspace.com/access_token";
	protected static final String MYSPACE_URL_ME = "%speople/@me/@self";
	protected static final String MYSPACE_HISTORY = "%sstatusmood/@me/@friends/history?includeself=true&fields=author,source,recentComments";
	protected static final String MYSPACE_URL_STATUSMOOD = "%sstatusmood/@me/@self";
	protected static final String MYSPACE_URL_STATUSMOODCOMMENTS = "%sstatusmoodcomments/%s/@self/%s?format=json&includeself=true&fields=author";
	protected static final String MYSPACE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	protected static final String MYSPACE_STATUSMOOD_BODY = "{\"status\":\"%s\"}";
	protected static final String MYSPACE_STATUSMOODCOMMENTS_BODY = "{\"body\":\"%s\"}";
	protected static final String MYSPACE_USER = "%speople/%s/@self";

	protected static final int FOURSQUARE = 4;
	protected static final String FOURSQUARE_BASE_URL = "https://api.foursquare.com/v2/";
	protected static final String FOURSQUARE_URL_AUTHORIZE = "https://foursquare.com/oauth2/authorize?client_id=%s&response_type=token&redirect_uri=%s&display=touch";
	protected static final String FOURSQUARE_URL_ME = "%susers/self?oauth_token=%s";
	protected static final String FOURSQUARE_URL_USER = "%susers/%s?oauth_token=%s";
	protected static final String FOURSQUARE_URL_PROFILE = "https://foursquare.com/user/%s";
	protected static final String FOURSQUARE_CHECKINS = "%scheckins/recent?oauth_token=%s";
	protected static final String FOURSQUARE_CHECKIN = "%scheckins/add?venueId=%s&shout=%s&ll=%s,%s&broadcast=public&oauth_token=%s";
	protected static final String FOURSQUARE_CHECKIN_NO_VENUE = "%scheckins/add?shout=%s&broadcast=public&oauth_token=%s";
	protected static final String FOURSQUARE_CHECKIN_NO_SHOUT = "%scheckins/add?venueId=%s&ll=%s,%s&broadcast=public&oauth_token=%s";
	protected static final String FOURSQUARE_ADDCOMMENT = "%scheckins/%s/addcomment?text=%s&oauth_token=%s";
	protected static final String FOURSQUARE_SEARCH = "%svenues/search?ll=%s,%s&intent=checkin&oauth_token=%s";
	protected static final String FOURSQUARE_GET_CHECKIN = "%scheckins/%s?oauth_token=%s";

	protected static final int LINKEDIN = 5;
	protected static final String LINKEDIN_BASE_URL = "https://api.linkedin.com/v1/people/~";
	protected static final String LINKEDIN_URL_REQUEST = "https://api.linkedin.com/uas/oauth/requestToken";
	protected static final String LINKEDIN_URL_AUTHORIZE = "https://www.linkedin.com/uas/oauth/authorize";
	protected static final String LINKEDIN_URL_ACCESS = "https://api.linkedin.com/uas/oauth/accessToken";
	protected static final String LINKEDIN_URL_ME = "%s:(id,first-name,last-name)";
	protected static final String LINKEDIN_URL_USER = "https://api.linkedin.com/v1/people/id=%s";
	protected static final String LINKEDIN_UPDATES = "%s/network/updates?type=APPS&type=CMPY&type=CONN&type=JOBS&type=JGRP&type=PICT&type=PRFU&type=RECU&type=PRFX&type=ANSW&type=QSTN&type=SHAR&type=VIRL";
	protected static final String[][] LINKEDIN_HEADERS = new String[][] {{"x-li-format", "json"}};
	protected static final String LINKEDIN_IS_LIKED = "%s/network/updates/key=%s/is-liked";
	protected static final String LINKEDIN_UPDATE = "%s/network/updates/key=%s";
	protected static final String LINKEDIN_UPDATE_COMMENTS = "%s/network/updates/key=%s/update-comments";
	protected static final String LINKEDIN_POST = "%s/person-activities";
	protected static final String LINKEDIN_POST_BODY = "<?xml version='1.0' encoding='UTF-8'?><activity locale=\"%s\"><content-type>linkedin-html</content-type><body>%s</body></activity>";
	protected static final String LINKEDIN_COMMENT_BODY = "<?xml version='1.0' encoding='UTF-8'?><update-comment><comment>%s</comment></update-comment>";
	protected static final String LINKEDIN_LIKE_BODY = "<?xml version='1.0' encoding='UTF-8'?><is-liked>%s</is-liked>";

	protected static final int SMS = 6;
	protected static final int RSS = 7;

	protected static final int IDENTICA = 8;
	protected static final String IDENTICA_BASE_URL = "https://identi.ca/api/";
	protected static final String IDENTICA_URL_REQUEST = "%soauth/request_token";
	protected static final String IDENTICA_URL_AUTHORIZE = "%soauth/authorize";
	protected static final String IDENTICA_URL_ACCESS = "%soauth/access_token";
	protected static final String IDENTICA_URL_FEED = "%sstatuses/home_timeline.json?count=%s";
	protected static final String IDENTICA_RETWEET = "%sstatuses/retweet/%s.json";
	protected static final String IDENTICA_USER = "%susers/show.json?user_id=%s";
	protected static final String IDENTICA_UPDATE = "%sstatuses/update.json";
	protected static final String IDENTICA_PROFILE = "http://identi.ca/%s";	
	protected static final String IDENTICA_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";
	protected static final String IDENTICA_MENTIONS = "%sstatuses/mentions.json%s";
	protected static final String IDENTICA_SINCE_ID = "?since_id=%s";
	
	protected static final int GOOGLEPLUS = 9;
	protected static final String GOOGLEPLUS_AUTHORIZE = "https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&scope=https://www.googleapis.com/auth/plus.me&response_type=code";
	protected static final String GOOGLE_ACCESS = "https://accounts.google.com/o/oauth2/token";
	protected static final String GOOGLEPLUS_BASE_URL = "https://www.googleapis.com/plus/v1/";
	protected static final String GOOGLEPLUS_URL_ME = "%speople/me?fields=displayName,id&access_token=%s";
	protected static final String GOOGLEPLUS_ACTIVITIES = "%speople/%s/activities/%s?maxResults=%s&access_token=%s";
	protected static final String GOOGLEPLUS_ACTIVITY = "%sactivities/%s?access_token=%s";
	protected static final String GOOGLEPLUS_PROFILE = "https://plus.google.com/%s";
	protected static final String GOOGLEPLUS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	
	protected static final int PINTEREST = 10;
	protected static final String PINTEREST_BASE_URL = "https://api.pinterest.com/v2/";
	protected static final String PINTEREST_URL_FEED = "%spopular/";
	protected static final String PINTEREST_PIN = "https://pinterest.com/pin/%s/";
	protected static final String PINTEREST_PROFILE = "https://pinterest.com/%s/";
	protected static final String PINTEREST_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

	protected static final int CHATTER = 11;
	protected static final String CHATTER_URL_AUTHORIZE = "https://login.salesforce.com/services/oauth2/authorize?response_type=token&display=touch&client_id=%s&redirect_uri=%s";
	protected static final String CHATTER_URL_ACCESS = "https://login.salesforce.com/services/oauth2/token?grant_type=refresh_token&client_id=%s&refresh_token=%s";
	protected static final String CHATTER_URL_ME = "%s/services/data/v22.0/chatter/users/me";
	protected static final String CHATTER_URL_POST = "%s/services/data/v22.0/chatter/feeds/news/me/feed-items?text=%s";
	protected static final String CHATTER_URL_COMMENT = "%s/services/data/v22.0/chatter/feed-items/%s/comments?text=%s";
	protected static final String CHATTER_URL_FEED = "%s/services/data/v22.0/chatter/feeds/news/me/feed-items";
	protected static final String CHATTER_URL_LIKES = "%s/services/data/v22.0/chatter/feed-items/%s/likes";
	protected static final String CHATTER_URL_LIKE = "%s/services/data/v22.0/chatter/likes/%s";
	protected static final String CHATTER_URL_COMMENTS = "%s/services/data/v22.0/chatter/feed-items/%s/comments";
	protected static final String CHATTER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

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
	
	protected static final String Sid = "id";
	protected static final String Sname = "name";
	protected static final String Suser = "user";
	protected static final String Screated_at = "created_at";
	protected static final String Sprofile_image_url = "profile_image_url";
	protected static final String Stext = "text";
	protected static final String Sdata = "data";
	protected static final String Screated_time = "created_time";
	protected static final String Sfrom = "from";
	protected static final String Stype = "type";
	protected static final String Smessage = "message";
	protected static final String Sto = "to";
	protected static final String Slink = "link";
	protected static final String Sstatus = "status";
	protected static final String Scomment = "comment";
	protected static final String Scomments = "comments";
	protected static final String Sperson = "person";
	protected static final String Svalues = "values";
	protected static final String SupdateComments = "updateComments";
	protected static final String SupdateKey = "updateKey";
	protected static final String Stimestamp = "timestamp";
	protected static final String ScurrentShare = "currentShare";
	protected static final String SupdateType = "updateType";
	protected static final String SupdateContent = "updateContent";
	protected static final String SpersonActivities = "personActivities";
	protected static final String Sconnections = "connections";
	protected static final String S_total = "_total";
	protected static final String Svenue = "venue";
	protected static final String Srecent = "recent";
	protected static final String SrecommendationsGiven = "recommendationsGiven";
	protected static final String Sjob = "job";
	protected static final String Sposition = "position";
	protected static final String SmemberGroups = "memberGroups";
	protected static final String SANSW = "ANSW";
	protected static final String SAPPS = "APPS";
	protected static final String SCMPY = "CMPY";
	protected static final String SCONN = "CONN";
	protected static final String SJOBP = "JOBP";
	protected static final String SJGRP = "JGRP";
	protected static final String SPRFX = "PRFX";
	protected static final String SPREC = "PREC";
	protected static final String SPROF = "PROF";
	protected static final String SQSTN = "QSTN";
	protected static final String SSHAR = "SHAR";
	protected static final String SVIRL = "VIRL";
	protected static final String SPICU = "PICU";
	protected static final String Sauthor = "author";
	protected static final String Sentry = "entry";
	protected static final String SpostedDate = "postedDate";
	protected static final String SdisplayName = "displayName";
	protected static final String Sphoto = "photo";
	protected static final String SsmallPhotoUrl = "smallPhotoUrl";
	protected static final String SmoodStatusLastUpdated = "moodStatusLastUpdated";
	protected static final String SthumbnailUrl = "thumbnailUrl";
	protected static final String SrecentComments = "recentComments";
	protected static final String SuserId = "userId";
	protected static final String SstatusId = "statusId";
	protected static final String Sitem = "item";
	protected static final String Sitems = "items";
	protected static final String Sobject = "object";
	protected static final String Spublished = "published";
	protected static final String Sinstance_url = "instance_url";
	protected static final String Scontent = "content";
	protected static final String SoriginalContent = "originalContent";
	protected static final String Sreplies = "replies";
	protected static final String Simage = "image";
	protected static final String Surl = "url";
	protected static final String Sactor = "actor";
	protected static final String StotalItems = "totalItems";
	protected static final String ScreatedDate = "createdDate";
	protected static final String Sbody = "body";
	protected static final String Stotal = "total";
	protected static final String ScreatedAt = "createdAt";
	protected static final String SfirstName = "firstName";
	protected static final String SlastName = "lastName";
	protected static final String Sresponse = "response";
	protected static final String Scheckin = "checkin";
	protected static final String Sshout = "shout";
	protected static final String Stitle = "title";
	protected static final String Sdescription = "description";
	protected static final String Spubdate = "pubdate";
	protected static final String SpictureUrl = "pictureUrl";
	protected static final String SisCommentable = "isCommentable";
	protected static final String SrecommendationSnippet = "recommendationSnippet";
	protected static final String Srecommendee = "recommendee";
	protected static final String Sscreen_name = "screen_name";
	protected static final String Sin_reply_to_status_id = "in_reply_to_status_id";
	protected static final String Suser_likes = "user_likes";
	protected static final String ScommentId = "commentId";
	protected static final String Sgroups = "groups";
	protected static final String SNearby = "Nearby";
	protected static final String Splaces = "places";
	protected static final String Sresult = "result";
	protected static final String Sfull_name = "full_name";
	protected static final String Ssource = "source";
	protected static final String Sstory = "story";
	protected static final String Smobile = "mobile";
	protected static final String Simage_url = "image_url";
	protected static final String Scounts = "counts";
	protected static final String Simages = "images";
	protected static final String Susername = "username";
	protected static final String Spicture = "picture";
	protected static final String Sboard = "board";
	protected static final String Simgur = "i.imgur.com";
	
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

	protected static final int INVALID_SERVICE = -1;

	protected static final int default_interval = 3600000;
	protected static final int default_buttons_bg_color = 0x88000000;
	protected static final int default_buttons_color = 0xFFFFFFFF;
	protected static final int default_message_bg_color = 0x88FFFFFF;
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
	protected static final int default_friend_bg_color = 0x88000000;

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
		// service id for posting and linking
		public static final String SID = "sid";

	}

	public static final class Widget_accounts implements BaseColumns {

		private Widget_accounts() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/widget_accounts");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widget_accounts";

		public static final String ACCOUNT = "account";
		public static final String WIDGET = "widget";
	}

	public static final class Widget_accounts_view implements BaseColumns {

		private Widget_accounts_view() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/widget_accounts_view");

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

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/widgets_settings");
		public static final Uri DISTINCT_CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/distinct_widgets_settings");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widgets_settings";
		
	}

	public static final class Accounts_styles implements BaseColumns  {

		private Accounts_styles() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/accounts_styles");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.accounts_styles";
		
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
		public static final String FRIEND_OVERRIDE = "friend_override";
		public static final String PROFILE_BG = "profiles_bg_color";
		public static final String FRIEND_BG = "friend_bg";

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
		public static final String PROFILE_BG = "profiles_bg_color";
		public static final String FRIEND_BG = "friend_bg";
		public static final String IMAGE_BG = "image_bg";
		public static final String IMAGE = "image";

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

	public static final class Notifications implements BaseColumns {
		// store notifications
		// notifications are marked cleared when viewed
		// notifications are deleted when the feeds are updated, they are not in the new feeds and are marked cleared
		private Notifications() {
		}
		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/notifications");
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

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/status_links");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.status_links";
		public static final String STATUS_ID = "status_id";
		public static final String LINK_URI = "link_uri";
		public static final String LINK_TYPE = "link_type";
		
	}
	
	public static final class Status_images implements BaseColumns {

		private Status_images() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/status_images");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.status_images";
		public static final String STATUS_ID = "status_id";
		public static final String IMAGE = "image";
		public static final String IMAGE_BG = "image_bg";
		
	}

	protected static final TimeZone sTimeZone = TimeZone.getTimeZone("GMT");

	protected static final String[] sRFC822 = {"EEE, d MMM yy HH:mm:ss z", "EEE, d MMM yy HH:mm z", "EEE, d MMM yyyy HH:mm:ss z", "EEE, d MMM yyyy HH:mm z", "d MMM yy HH:mm z", "d MMM yy HH:mm:ss z", "d MMM yyyy HH:mm z", "d MMM yyyy HH:mm:ss z"};

	protected static String getCreatedText(long epoch, boolean time24hr) {
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
	
	protected static String getServiceName(Resources r, int service) {
		String name = null;
		String[] entries = r.getStringArray(R.array.service_entries);
		String[] values = r.getStringArray(R.array.service_values);
		for (int i = 0, l = values.length; i < l; i++) {
			if (Integer.toString(service).equals(values[i])) {
				name = entries[i];
				break;
			}
		}
		return name;
	}

	protected static int[] arrayCat(int[] a, int[] b) {
		int[] c;
		for (int i = 0, i2 = b.length; i < i2; i++) {
			int cLen = a.length;
			c = new int[cLen];
			for (int n = 0; n < cLen; n++) {
				c[n] = a[n];
			}
			a = new int[cLen + 1];
			for (int n = 0; n < cLen; n++) {
				a[n] = c[n];
			}
			a[cLen] = b[i];
		}
		return a;
	}

	protected static int[] arrayAdd(int[] a, int b) {
		if (!Sonet.arrayContains(a, b)) {
			int cLen = a.length;
			int[] c = new int[cLen];
			for (int i = 0; i < cLen; i++) {
				c[i] = a[i];
			}
			a = new int[cLen + 1];
			for (int i = 0; i < cLen; i++) {
				a[i] = c[i];
			}
			a[cLen] = b;
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
		for (int i = 0, i2 = a.length; i < i2; i++) {
			if (a[i] == b) {
				c = i;
				break;
			}
		}
		return c;
	}

	protected static int arrayIndex(long[] a, long b) {
		int c = -1;
		for (int i = 0, i2 = a.length; i < i2; i++) {
			if (a[i] == b) {
				c = i;
				break;
			}
		}
		return c;
	}

	protected static BitmapFactory.Options sBFOptions = new BitmapFactory.Options();

	static {
		sBFOptions.inDither = false;
		sBFOptions.inPurgeable = true; // allow this memory to be reclaimed
		sBFOptions.inInputShareable = true; // share the reference, rather than copy
		sBFOptions.inTempStorage = new byte[32 * 1024]; // allocate temporary memory
	}

	protected static boolean HasValues(String[] values) {
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

	protected static byte[] getBlob(InputStream is) {
		Bitmap bmp = BitmapFactory.decodeStream(is, null, sBFOptions);
		if (bmp != null) {
			ByteArrayOutputStream blob = new ByteArrayOutputStream();
			bmp.compress(Bitmap.CompressFormat.PNG, 100, blob);
			bmp.recycle();
			return blob.toByteArray();
		}
		return null;
	}

	protected static byte[] getBlob(Resources r, int i) {
		Bitmap bmp = BitmapFactory.decodeResource(r, i, sBFOptions);
		if (bmp != null) {
			ByteArrayOutputStream blob = new ByteArrayOutputStream();
			bmp.compress(Bitmap.CompressFormat.PNG, 100, blob);
			bmp.recycle();
			return blob.toByteArray();
		}
		return null;
	}
	
	protected static Matcher getLinksMatcher(String raw) {
		return Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(raw);
	}

}
