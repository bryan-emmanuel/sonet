package com.piusvelte.sonet.core.social;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.HmacSha1MessageSigner;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import android.util.Log;

public class OAuth1aCredential extends Credential {

	private String TAG = "OAuth1aAuthenticator";

	protected OAuth1aCredential(Builder builder) {
		super(builder);
		if (builder.hasCredentials()) {
			mOAuthConsumer = new CommonsHttpOAuthConsumer(builder.getApiKey(), builder.getApiSecret());
			mOAuthConsumer.setMessageSigner(new HmacSha1MessageSigner());
			mOAuthConsumer.setTokenWithSecret(builder.getToken(), builder.getTokenSecret());
		} else {
			mOAuthConsumer = new CommonsHttpOAuthConsumer(builder.getApiKey(), builder.getApiSecret());
			mOAuthConsumer.setMessageSigner(new HmacSha1MessageSigner());
		}
	}

	@Override
	HttpUriRequest sign(HttpUriRequest httpRequest) {
		try {
			mOAuthConsumer.sign(httpRequest);
			return httpRequest;
		} catch (OAuthMessageSignerException e) {
			Log.e(TAG,e.toString());
		} catch (OAuthExpectationFailedException e) {
			Log.e(TAG,e.toString());
		} catch (OAuthCommunicationException e) {
			Log.e(TAG,e.toString());
		}
		return null;
	}

	private OAuthConsumer mOAuthConsumer;
	private OAuthProvider mOAuthProvider;

	public String getAuthUrl(String request, String access, String authorize, String callback, boolean isOAuth10a, HttpClient httpClient) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		mOAuthProvider = new CommonsHttpOAuthProvider(request, access, authorize, httpClient);
		mOAuthProvider.setOAuth10a(isOAuth10a);
		return mOAuthProvider.retrieveRequestToken(mOAuthConsumer, callback);
	}

	public boolean retrieveAccessToken(String verifier) {
		try {
			mOAuthProvider.retrieveAccessToken(mOAuthConsumer, verifier);
			return true;
		} catch (OAuthMessageSignerException e) {
			Log.e(TAG, e.toString());
		} catch (OAuthNotAuthorizedException e) {
			Log.e(TAG, e.toString());
		} catch (OAuthExpectationFailedException e) {
			Log.e(TAG, e.toString());
		} catch (OAuthCommunicationException e) {
			Log.e(TAG, e.toString());
		}
		return false;
	}

	public HttpUriRequest getSignedRequest(HttpUriRequest httpRequest) {
		try {
			mOAuthConsumer.sign(httpRequest);
			return httpRequest;
		} catch (OAuthMessageSignerException e) {
			Log.e(TAG,e.toString());
		} catch (OAuthExpectationFailedException e) {
			Log.e(TAG,e.toString());
		} catch (OAuthCommunicationException e) {
			Log.e(TAG,e.toString());
		}
		return null;
	}

	public String getToken() {
		return mOAuthConsumer.getToken();
	}

	public String getTokenSecret() {
		return mOAuthConsumer.getTokenSecret();
	}

	class Builder extends Credential.Builder {

		private String apiKey;
		public String getApiKey() {
			return apiKey;
		}

		public Builder setApiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public String getApiSecret() {
			return apiSecret;
		}

		public Builder setApiSecret(String apiSecret) {
			this.apiSecret = apiSecret;
			return this;
		}

		private String apiSecret;

		public Builder(String apiKey, String apiSecret) {
			this.apiKey = apiKey;
			this.apiSecret = apiSecret;
		}

		private String token;
		private String tokenSecret;

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		public String getTokenSecret() {
			return tokenSecret;
		}

		public void setTokenSecret(String tokenSecret) {
			this.tokenSecret = tokenSecret;
		}

		public boolean hasCredentials() {
			return ((token != null) && (tokenSecret != null));
		}

		@Override
		public Credential build() {
			return new OAuth1aCredential(this);
		}

	}

	@Override
	boolean hasCredentials() {
		return (mOAuthConsumer != null);
	}

}
