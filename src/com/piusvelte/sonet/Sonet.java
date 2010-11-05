package com.piusvelte.sonet;

public class Sonet {
	public static final String TAG = "Sonet";
	public static final int TWITTER = 0;
	public static final int FACEBOOK = 1;
	public static final int MYSPACE = 2;
	public static final String TWITTER_URL_REQUEST = "http://api.twitter.com/oauth/request_token";
	public static final String TWITTER_URL_ACCESS = "http://api.twitter.com/oauth/access_token";
	public static final String TWITTER_URL_AUTHORIZE = "http://api.twitter.com/oauth/authorize";
	protected static final String TWITTER_KEY = "";
	protected static final String TWITTER_SECRET = "";
	protected static final String FACEBOOK_ID = "";
    public static final String[] FACEBOOK_PERMISSIONS = new String[] {"offline_access"};
    protected static final String MYSPACE_KEY = "";
    protected static final String MYSPACE_SECRET = "";
	
	public Sonet() {}

}
