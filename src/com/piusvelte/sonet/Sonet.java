package com.piusvelte.sonet;

import android.net.Uri;
import android.provider.BaseColumns;

public class Sonet {
	public static final int TWITTER = 0;
	public static final int FACEBOOK = 1;
	public static final int MYSPACE = 2;
	public static final String TWITTER_URL_REQUEST = "http://api.twitter.com/oauth/request_token";
	public static final String TWITTER_URL_ACCESS = "http://api.twitter.com/oauth/access_token";
	public static final String TWITTER_URL_AUTHORIZE = "http://api.twitter.com/oauth/authorize";
    public static final String[] FACEBOOK_PERMISSIONS = new String[] {"offline_access"};
    public static final String ACTION_REFRESH = "com.piusvelte.sonet.Sonet.REFRESH";
    public static final String ACTION_BUILD_SCROLL = "com.piusvelte.sonet.Sonet.BUILD_SCROLL";
    public static final String DONATE = "http://www.piusvelte.com?p=donate-sonet";
    
    public Sonet() {
    }
    
    public static final class Accounts implements BaseColumns {
    	
    	private Accounts() {
    	}
    	
    	public static final Uri CONTENT_URI = Uri.parse("content://"
    			+ SonetProvider.AUTHORITY + "/accounts");
    	
    	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.accounts";
    	
    	public static final String _ID = "_id";
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
    	
    	public static final Uri CONTENT_URI = Uri.parse("content://"
    			+ SonetProvider.AUTHORITY + "/widgets");
    	
    	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widgets";

    	public static final String _ID = "_id";
    	public static final String WIDGET = "widget";
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
    	
    }
    
    public static final class Statuses implements BaseColumns {
    	
    	private Statuses() {
    	}
    	
    	public static final Uri CONTENT_URI = Uri.parse("content://"
    			+ SonetProvider.AUTHORITY + "/statuses");
    	
    	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.statuses";

    	public static final String TABLE_ACCOUNTS = "accounts";
    	public static final String _ID = "_id";
    	public static final String CREATED = "created";
    	public static final String LINK = "link";
    	public static final String FRIEND = "friend";
    	public static final String PROFILE = "profile";
    	public static final String MESSAGE = "message";
    	public static final String SERVICE = "service";
    	public static final String WIDGET = "widget";
    	public static final String CREATEDTEXT = "createdText";
    	
    }
    
}
