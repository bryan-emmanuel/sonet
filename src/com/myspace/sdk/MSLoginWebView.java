package com.myspace.sdk;

import oauth.signpost.OAuth;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.app.Activity;
import android.net.Uri;

public class MSLoginWebView extends MSWebView {

	private MSOAuth mOAuth;
	private static final String TAG = "MSLoginWebView";
	
	public MSLoginWebView(Activity context, MSSession session, IMSWebViewCallback webViewCallback) {
		super(context, session, webViewCallback);
		mOAuth = MSOAuth.init(session);
	}

	@Override
	public void show() {
		try {
			String authUrl = mOAuth.retrieveRequestToken(mSession.getApiCallBackUrl());
			open(authUrl);
		} catch (Exception e) {
			doFailCallback(TAG, e);
		}
	}

	@Override
	public void process(Uri uri) {
		if(uri.getQueryParameter("oauth_problem") != null) {
			doCancelCallback();
		} else if(uri.getQueryParameter(OAuth.OAUTH_TOKEN) != null) {
			try {
				if(!retrieveAccessTokenSuccess(uri)) {
					// try again in 0.5 seconds
					Thread.currentThread();
					Thread.sleep(500);
					// override and set token and secret as request token and request secret 
					mOAuth.setTokenWithSecret(mOAuth.getRequestToken(), mOAuth.getRequestTokenSecret());
					
					if(!retrieveAccessTokenSuccess(uri)) {
						String errorString = String.format("Error retrieving access token second time. token=%s tokenSecret=%s", mOAuth.getToken(), mOAuth.getTokenSecret());
						doFailCallback(TAG, new MSRequestException(errorString)); 
					}
				}
			} catch (Exception e) {
				doFailCallback(TAG, e);
			}
		}
	}
	
	public boolean retrieveAccessTokenSuccess(Uri uri) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		mOAuth.retrieveAccessToken(uri.getQueryParameter(OAuth.OAUTH_VERIFIER));
		String token = mOAuth.getToken();
		String tokenSecret = mOAuth.getTokenSecret();
		if(token != null && tokenSecret != null) {
			mSession.begin(mContext, token, tokenSecret);
			doSucceedCallback();
			return true;
		} 
		return false;
	}
}
