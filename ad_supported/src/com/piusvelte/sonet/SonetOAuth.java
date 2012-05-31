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

import org.apache.http.client.methods.HttpUriRequest;

import android.util.Log;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.HmacSha1MessageSigner;

public class SonetOAuth {

	private OAuthConsumer mOAuthConsumer;
	private OAuthProvider mOAuthProvider;
	private static final String TAG = "SonetOAuth";

	public SonetOAuth(String apiKey, String apiSecret) {
		mOAuthConsumer = new CommonsHttpOAuthConsumer(apiKey, apiSecret);
		mOAuthConsumer.setMessageSigner(new HmacSha1MessageSigner());
	}

	public SonetOAuth(String apiKey, String apiSecret, String token, String tokenSecret) {
		mOAuthConsumer = new CommonsHttpOAuthConsumer(apiKey, apiSecret);
		mOAuthConsumer.setMessageSigner(new HmacSha1MessageSigner());
		mOAuthConsumer.setTokenWithSecret(token, tokenSecret);
	}

	public String getAuthUrl(String request, String access, String authorize, String callback, boolean isOAuth10a) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		mOAuthProvider = new CommonsHttpOAuthProvider(request, access, authorize);
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

}
