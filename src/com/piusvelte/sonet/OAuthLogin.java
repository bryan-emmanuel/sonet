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

import java.net.URLEncoder;

import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.BUZZ_SCOPE;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_REQUEST;
import static com.piusvelte.sonet.Tokens.BUZZ_KEY;
import static com.piusvelte.sonet.Tokens.BUZZ_SECRET;

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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class OAuthLogin extends Activity {
	private static final String TAG = "TwitterLogin";
	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");
	private static Uri BUZZ_CALLBACK = Uri.parse("sonet://buzz");
	private int mService = Sonet.INVALID_SERVICE;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) mService = extras.getInt(Sonet.Accounts.SERVICE, Sonet.INVALID_SERVICE);
		}
		setContentView(R.layout.twitterlogin);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();
		if (uri != null) {
			if (TWITTER_CALLBACK.getScheme().equals(uri.getScheme())) {
				try {
					// this will populate token and token_secret in consumer
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
					//					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET, SignatureMethod.HMAC_SHA1);
					consumer.setTokenWithSecret(ManageAccounts.sRequest_token, ManageAccounts.sRequest_secret);
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
					if (ManageAccounts.sAccountId != Sonet.INVALID_ACCOUNT_ID) {
						getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(ManageAccounts.sAccountId)});
						ManageAccounts.sAccountId = Sonet.INVALID_ACCOUNT_ID;
					} else getContentResolver().insert(Accounts.CONTENT_URI, values);
					ManageAccounts.sUpdateWidget = true;
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			} else if (BUZZ_CALLBACK.getScheme().equals(uri.getScheme())) {
				try {
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(BUZZ_KEY, BUZZ_SECRET, SignatureMethod.HMAC_SHA1);
					consumer.setTokenWithSecret(ManageAccounts.sRequest_token, ManageAccounts.sRequest_secret);
					OAuthProvider provider = new DefaultOAuthProvider(consumer, BUZZ_URL_REQUEST + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8"), BUZZ_URL_ACCESS, BUZZ_URL_AUTHORIZE + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8") + "&domain=" );
					provider.setOAuth10a(true);
					provider.retrieveAccessToken(verifier);
					ContentValues values = new ContentValues();
					values.put(Accounts.USERNAME, (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(consumer.getToken(), consumer.getTokenSecret()))).getScreenName());
					values.put(Accounts.TOKEN, consumer.getToken());
					values.put(Accounts.SECRET, consumer.getTokenSecret());
					values.put(Accounts.EXPIRY, 0);
					values.put(Accounts.SERVICE, TWITTER);
					values.put(Accounts.TIMEZONE, 0);
					values.put(Accounts.WIDGET, ManageAccounts.sAppWidgetId);
					if (ManageAccounts.sAccountId != Sonet.INVALID_ACCOUNT_ID) {
						getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(ManageAccounts.sAccountId)});
						ManageAccounts.sAccountId = Sonet.INVALID_ACCOUNT_ID;
					} else getContentResolver().insert(Accounts.CONTENT_URI, values);
					ManageAccounts.sUpdateWidget = true;
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if ((ManageAccounts.sRequest_token == null) && (ManageAccounts.sRequest_secret == null)) {
			((TextView) findViewById(R.id.twitterlogin_message)).setText(R.string.loading);
			try {
				// switching to older signpost for myspace
				//				CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
				CommonsHttpOAuthConsumer consumer;
				//				OAuthProvider provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
				OAuthProvider provider;
				//				String authUrl = provider.retrieveRequestToken(consumer, TWITTER_CALLBACK.toString());
				String authUrl;
				switch (mService) {
				case TWITTER:
					consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET, SignatureMethod.HMAC_SHA1);
					provider = new DefaultOAuthProvider(consumer, TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
					provider.setOAuth10a(true);
					authUrl = provider.retrieveRequestToken(TWITTER_CALLBACK.toString());
					// need to save the requestToken and secret
					ManageAccounts.sRequest_token = consumer.getToken();
					ManageAccounts.sRequest_secret = consumer.getTokenSecret();
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
					break;
				case BUZZ:
					consumer = new CommonsHttpOAuthConsumer(BUZZ_KEY, BUZZ_SECRET, SignatureMethod.HMAC_SHA1);
					provider = new DefaultOAuthProvider(consumer, BUZZ_URL_REQUEST + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8"), BUZZ_URL_ACCESS, BUZZ_URL_AUTHORIZE + "?scope=" + URLEncoder.encode(BUZZ_SCOPE, "utf-8") + "&domain=" + "&alt=json");
					provider.setOAuth10a(true);
					authUrl = provider.retrieveRequestToken(BUZZ_CALLBACK.toString());
					// need to save the requestToken and secret
					ManageAccounts.sRequest_token = consumer.getToken();
					ManageAccounts.sRequest_secret = consumer.getTokenSecret();
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
					break;
				}
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
		} else {
			((TextView) findViewById(R.id.twitterlogin_message)).setText(R.string.twitterlogin_message);
			ManageAccounts.sRequest_token = null;
			ManageAccounts.sRequest_secret = null;
		}
	}

}
