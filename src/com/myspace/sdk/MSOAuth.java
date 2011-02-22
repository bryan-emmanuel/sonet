package com.myspace.sdk;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
// update to use signpost-commonshttp4-1.2.1.1
//import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
//import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.HmacSha1MessageSigner;
// update to use signpost-commonshttp4-1.2.1.1
//import oauth.signpost.signature.SignatureMethod;

import org.apache.http.client.methods.HttpRequestBase;

import android.util.Log;

public class MSOAuth {

	private static final String OAUTH_REQUEST_TOKEN_URL = "http://api.myspace.com/request_token";
	private static final String OAUTH_AUTHORIZATION_URL = "http://api.myspace.com/authorize";
	private static final String OAUTH_ACCESS_TOKEN_URL = "http://api.myspace.com/access_token";

	private OAuthProvider mOAuthProvider;
	private OAuthConsumer mOAuthConsumer;

	private String mRequestToken;
	private String mRequestTokenSecret;
	
	private MSOAuth() {
	}

	private MSOAuth(MSSession session) {
		initConsumer(session.getApiKey(), session.getApiSecret());
		
		String token = session.getToken();
		String tokenSecret = session.getTokenSecret();
		if(token != null && tokenSecret != null) {
			setTokenWithSecret(token, tokenSecret);
		}
		else {
			initProvider();
		}
	}

	public static MSOAuth init(MSSession session) {
		return new MSOAuth(session);
	}

	private void initConsumer(String apiKey, String apiSecret) {
		// update to use signpost-commonshttp4-1.2.1.1
//		mOAuthConsumer = new CommonsHttpOAuthConsumer(apiKey, apiSecret, SignatureMethod.HMAC_SHA1);
		Log.v("MSOAuth","initConsumer("+apiKey+","+apiSecret+")");
		mOAuthConsumer = new CommonsHttpOAuthConsumer(apiKey, apiSecret);
		mOAuthConsumer.setMessageSigner(new HmacSha1MessageSigner());
	}

	private void initProvider() {
		Log.v("MSOAuth","initProvider");
		String requestedPermissions = MSSession.getSession().getRequestedPermissions();
		String authUrl = (requestedPermissions == null) ? OAUTH_AUTHORIZATION_URL : OAUTH_AUTHORIZATION_URL + "?myspaceid.permissions=" + requestedPermissions;
		// update to use signpost-commonshttp4-1.2.1.1
		mOAuthProvider = new DefaultOAuthProvider(OAUTH_REQUEST_TOKEN_URL, OAUTH_ACCESS_TOKEN_URL, authUrl);
//		mOAuthProvider = new CommonsHttpOAuthProvider(OAUTH_REQUEST_TOKEN_URL, OAUTH_ACCESS_TOKEN_URL, authUrl);
	}

	public String retrieveRequestToken(String callbackUrl) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		// update to use signpost-commonshttp4-1.2.1.1
//		String authUrl = mOAuthProvider.retrieveRequestToken(callbackUrl);
		Log.v("MSOAuth","retrieveRequestToken("+callbackUrl+")");
		// the next line times out
		String authUrl = mOAuthProvider.retrieveRequestToken(mOAuthConsumer, callbackUrl);
		Log.v("MSOAuth","authUrl:"+authUrl);
		this.mRequestToken = getToken();
		Log.v("MSOAuth","mRequestToken:"+getToken());
		this.mRequestTokenSecret = getTokenSecret();
		Log.v("MSOAuth","mRequestTokenSecret:"+getTokenSecret());
		return authUrl;
	}

	public void retrieveAccessToken(String verifier) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		// update to use signpost-commonshttp4-1.2.1.1
//		mOAuthProvider.retrieveAccessToken(verifier);
		mOAuthProvider.retrieveAccessToken(mOAuthConsumer, verifier);
	}

	public void setTokenWithSecret(String token, String tokenSecret) {
		Log.v("MSOAuth","setTokenWithSecret("+token+","+tokenSecret+")");
		mOAuthConsumer.setTokenWithSecret(token, tokenSecret);
	}

	public void sign(HttpRequestBase httpRequest) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		mOAuthConsumer.sign(httpRequest);
	}

	public String getToken() {
		return mOAuthConsumer.getToken();
	}

	public String getTokenSecret() {
		return mOAuthConsumer.getTokenSecret();
	}
	
	public String getRequestToken() {
		return this.mRequestToken;
	}

	public String getRequestTokenSecret() {
		return this.mRequestTokenSecret;
	}
}
