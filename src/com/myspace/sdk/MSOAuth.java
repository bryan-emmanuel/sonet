package com.myspace.sdk;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
//import oauth.signpost.signature.SignatureMethod;

import org.apache.http.client.methods.HttpRequestBase;

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
//		mOAuthConsumer = new CommonsHttpOAuthConsumer(apiKey, apiSecret, SignatureMethod.HMAC_SHA1);
		mOAuthConsumer = new CommonsHttpOAuthConsumer(apiKey, apiSecret);
	}

	private void initProvider() {
		String requestedPermissions = MSSession.getSession().getRequestedPermissions();
		String authUrl = (requestedPermissions == null) ? OAUTH_AUTHORIZATION_URL : OAUTH_AUTHORIZATION_URL + "?myspaceid.permissions=" + requestedPermissions;
//		mOAuthProvider = new DefaultOAuthProvider(mOAuthConsumer, OAUTH_REQUEST_TOKEN_URL, OAUTH_ACCESS_TOKEN_URL, authUrl);  
		mOAuthProvider = new DefaultOAuthProvider(OAUTH_REQUEST_TOKEN_URL, OAUTH_ACCESS_TOKEN_URL, authUrl);   
	}

	public String retrieveRequestToken(String callbackUrl) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		String authUrl = mOAuthProvider.retrieveRequestToken(mOAuthConsumer, callbackUrl);
		this.mRequestToken = getToken();
		this.mRequestTokenSecret = getTokenSecret();
		return authUrl;
	}

	public void retrieveAccessToken(String verifier) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
//		mOAuthProvider.retrieveAccessToken(verifier);
		mOAuthProvider.retrieveAccessToken(mOAuthConsumer, verifier);
	}

	public void setTokenWithSecret(String token, String tokenSecret) {
		mOAuthConsumer.setTokenWithSecret(token, tokenSecret);
	}

	public void sign(HttpRequestBase httpRequest) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
//	public void sign(HttpRequestBase httpRequest) throws OAuthMessageSignerException, OAuthExpectationFailedException {
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
