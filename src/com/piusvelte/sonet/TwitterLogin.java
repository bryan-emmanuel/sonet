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

import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.TWITTER_KEY;
import static com.piusvelte.sonet.Tokens.TWITTER_SECRET;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.signature.SignatureMethod;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

import com.piusvelte.sonet.Sonet.Accounts;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class TwitterLogin extends Activity {
	private static final String TAG = "TwitterLogin";
	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");
	private static String request_token,
	request_secret;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			// switching to older signpost for myspace
			//				CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
			CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET, SignatureMethod.HMAC_SHA1);
			//				OAuthProvider provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
			OAuthProvider provider = new DefaultOAuthProvider(consumer, TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
			provider.setOAuth10a(true);
			//				String authUrl = provider.retrieveRequestToken(consumer, TWITTER_CALLBACK.toString());
			String authUrl = provider.retrieveRequestToken(TWITTER_CALLBACK.toString());
			/*
			 * need to save the requestToken and secret
			 */
			request_token = consumer.getToken();
			request_secret = consumer.getTokenSecret();
			SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
			Editor spe = sp.edit();
			spe.putString(getString(R.string.key_requesttoken), request_token);
			spe.putString(getString(R.string.key_requestsecret), request_secret);
			spe.commit();
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();
		if (uri != null) {
			if (TWITTER_CALLBACK.getScheme().equals(uri.getScheme())) {
				Log.v(TAG,"twitter_callback:"+ManageAccounts.sAppWidgetId);
				try {
					// use the requestToken and secret from earlier
					SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
					if ((request_token == null) || (request_secret == null)) {
						request_token = sp.getString(getString(R.string.key_requesttoken), "");
						request_secret = sp.getString(getString(R.string.key_requestsecret), "");
					}
					// clear the saved token/secret
					Editor spe = sp.edit();
					spe.putString(getString(R.string.key_requesttoken), "");
					spe.putString(getString(R.string.key_requestsecret), "");
					spe.commit();
					// this will populate token and token_secret in consumer
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
					//					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET, SignatureMethod.HMAC_SHA1);
					consumer.setTokenWithSecret(request_token, request_secret);
					//					OAuthProvider provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
					OAuthProvider provider = new DefaultOAuthProvider(consumer, TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
					provider.setOAuth10a(true);
					//					provider.retrieveAccessToken(consumer, verifier);
					provider.retrieveAccessToken(verifier);
					ContentValues values = new ContentValues();
					values.put(Accounts.USERNAME, (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(consumer.getToken(), consumer.getTokenSecret()))).getScreenName());
					values.put(Accounts.TOKEN, consumer.getToken());
					values.put(Accounts.SECRET, consumer.getTokenSecret());
					values.put(Accounts.EXPIRY, 0);
					values.put(Accounts.SERVICE, TWITTER);
					values.put(Accounts.TIMEZONE, 0);
					values.put(Accounts.WIDGET, ManageAccounts.sAppWidgetId);
					getContentResolver().insert(Accounts.CONTENT_URI, values);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

}
