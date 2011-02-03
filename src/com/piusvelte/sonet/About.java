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

import static com.piusvelte.sonet.Sonet.DONATE;
import static com.piusvelte.sonet.Tokens.MYSPACE_KEY;
import static com.piusvelte.sonet.Tokens.MYSPACE_SECRET;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.signature.SignatureMethod;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.myspace.sdk.MSOAuth;
import com.myspace.sdk.MSRequest;
import com.myspace.sdk.MSSDK;
import com.myspace.sdk.MSSession;
import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Widgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class About extends Activity implements View.OnClickListener,
DialogInterface.OnClickListener {
	private int[] mAppWidgetIds;
	private AppWidgetManager mAppWidgetManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		mAppWidgetIds = new int[0];
		// validate appwidgetids from appwidgetmanager
		mAppWidgetManager = AppWidgetManager.getInstance(this);
		int[] appWidgetManagerAppWidgetIds = arrayCat(
				arrayCat(mAppWidgetManager.getAppWidgetIds(new ComponentName(
						this, SonetWidget_4x2.class)),
						mAppWidgetManager.getAppWidgetIds(new ComponentName(
								this, SonetWidget_4x3.class))),
								mAppWidgetManager.getAppWidgetIds(new ComponentName(this,
										SonetWidget_4x4.class)));
		int[] removeAppWidgets = new int[0];
		this.getContentResolver().delete(Widgets.CONTENT_URI,
				Widgets.WIDGET + "=?", new String[] { "" });
		this.getContentResolver().delete(Accounts.CONTENT_URI,
				Accounts.WIDGET + "=?", new String[] { "" });
		Cursor widgets = this.getContentResolver().query(Widgets.CONTENT_URI,
				new String[] { Widgets._ID, Widgets.WIDGET },
				Widgets.ACCOUNT + "=?",
				new String[] { Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);
		if (widgets.moveToFirst()) {
			int iwidget = widgets.getColumnIndex(Widgets.WIDGET), appWidgetId;
			while (!widgets.isAfterLast()) {
				appWidgetId = widgets.getInt(iwidget);
				if (arrayContains(appWidgetManagerAppWidgetIds, appWidgetId))
					mAppWidgetIds = arrayPush(mAppWidgetIds, appWidgetId);
				else
					removeAppWidgets = arrayPush(removeAppWidgets, appWidgetId);
				widgets.moveToNext();
			}
		}
		widgets.close();
		if (removeAppWidgets.length > 0) {
			// remove phantom widgets
			for (int appWidgetId : removeAppWidgets) {
				this.getContentResolver().delete(Widgets.CONTENT_URI,
						Widgets.WIDGET + "=?",
						new String[] { Integer.toString(appWidgetId) });
				this.getContentResolver().delete(Accounts.CONTENT_URI,
						Accounts.WIDGET + "=?",
						new String[] { Integer.toString(appWidgetId) });
				this.getContentResolver().delete(Statuses.CONTENT_URI,
						Statuses.WIDGET + "=?",
						new String[] { Integer.toString(appWidgetId) });
			}
		}
		((Button) findViewById(R.id.defaultsettings)).setOnClickListener(this);
		((Button) findViewById(R.id.widgets)).setOnClickListener(this);
		((Button) findViewById(R.id.refreshall)).setOnClickListener(this);
		((Button) findViewById(R.id.donate)).setOnClickListener(this);


		String TAG = "testing";
		Cursor accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts.SERVICE + "=?", new String[]{Integer.toString(Sonet.MYSPACE)}, null);
		if (accounts.moveToFirst()) {

			int itoken = accounts.getColumnIndex(Accounts.TOKEN),
			isecret = accounts.getColumnIndex(Accounts.SECRET);
			OAuthConsumer consumer = new CommonsHttpOAuthConsumer(MYSPACE_KEY, MYSPACE_SECRET, SignatureMethod.HMAC_SHA1);
			consumer.setTokenWithSecret(accounts.getString(itoken),	accounts.getString(isecret));
			//			HttpClient client = new DefaultHttpClient();
			//			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			//			HttpGet request = new HttpGet("http://opensocial.myspace.com/1.0/people/@me/@self");
			//			HttpGet request = new HttpGet("http://opensocial.myspace.com/1.0/statusmood/@me/@friends/history?includeself=true&fields=author,source");

			HttpRequestBase httpRequest = new
			//			 HttpGet("http://opensocial.myspace.com/1.0/people/@me/@self");
			HttpGet("http://opensocial.myspace.com/1.0/statusmood/@me/@friends/history?fields=author,recentcomments,source");
			//			 HttpGet("http://opensocial.myspace.com/1.0/statusmood/@me/@friends/history?includeself=true&fields=author,source");

			MSSession msSession = MSSession.getSession(MYSPACE_KEY, MYSPACE_SECRET, Sonet.MYSPACE_CALLBACK, null);
			msSession.setToken(accounts.getString(itoken));
			msSession.setTokenSecret(accounts.getString(isecret));

			Log.v(TAG,(msSession.resume(this)?"resuming":"wtf?!"));

			Map<String, String> queryParams = new HashMap<String, String>();
			MSSDK.getStatusMoodWithParameters(queryParams, new MSRequest.MSRequestCallback() {

				@Override
				public void requestDidFail(MSRequest arg0, Throwable arg1) {
					Log.v("testing","failed");					
				}

				@Override
				public void requestDidLoad(MSRequest arg0, Object result) {
					Map<?, ?> data = (Map<?, ?>) result;
					result = data.get("data");
					if (result instanceof Map<?, ?>) {
						Map<?, ?> userObject = (Map<?, ?>) result;
						Log.v("testing",userObject.get("numComments").toString());
					}

				}});

			MSSDK.getUserInfo(new MSRequest.MSRequestCallback() {

				@Override
				public void requestDidFail(MSRequest arg0, Throwable arg1) {
					Log.v("testing","failed");					
				}

				@Override
				public void requestDidLoad(MSRequest arg0, Object result) {
					Map<?, ?> data = (Map<?, ?>) result;
					result = data.get("data");
					if (result instanceof Map<?, ?>) {
						Map<?, ?> userObject = (Map<?, ?>) result;
						Log.v("testing",userObject.get("userName").toString());
					}

				}});
			/*
			 MSOAuth oauth = MSOAuth.init(msSession);
			 oauth.setTokenWithSecret(accounts.getString(itoken),
			 accounts.getString(isecret));

			try {

				 oauth.sign(httpRequest);

				 HttpClient httpClient = new DefaultHttpClient();
				 HttpResponse httpResponse = httpClient.execute(httpRequest);
				 HttpEntity entity = httpResponse.getEntity();
				 String response = "";
				 if (entity != null) {
				 InputStream is = entity.getContent();
				 BufferedReader reader = new BufferedReader(new
				 InputStreamReader(is));
				 StringBuilder sb = new StringBuilder();

				 String line = null;
				 try {
				 while ((line = reader.readLine()) != null) {
				 sb.append(line + "\n");
				 }
				 } catch (IOException e) {
				 e.printStackTrace();
				 } finally {
				 try {
				 is.close();
				 } catch (IOException e) {
				 e.printStackTrace();
				 }
				 }
				 response = sb.toString();
				 }

//				consumer.sign(request);
//				String response = client.execute(request, responseHandler);
				Log.v(TAG,response);

			} catch (ClientProtocolException e) {
				Log.e(TAG, e.toString());
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			} catch (OAuthMessageSignerException e) {
				Log.e(TAG, e.toString());
			} catch (OAuthExpectationFailedException e) {
				Log.e(TAG, e.toString());
			}
			 */
		}
		accounts.close();

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.defaultsettings:
			startActivity(new Intent(this, Settings.class));
			break;
		case R.id.widgets:
			if (mAppWidgetIds.length > 0) {
				String[] widgets = new String[mAppWidgetIds.length];
				for (int i = 0; i < mAppWidgetIds.length; i++) {
					AppWidgetProviderInfo info = mAppWidgetManager
					.getAppWidgetInfo(mAppWidgetIds[i]);
					String providerName = info.provider.getClassName();
					widgets[i] = Integer.toString(mAppWidgetIds[i])
					+ " ("
					+ (providerName == SonetWidget_4x2.class.getName() ? "4x2"
							: providerName == SonetWidget_4x3.class
							.getName() ? "4x3" : "4x4") + ")";
				}
				(new AlertDialog.Builder(this)).setItems(widgets, this)
				.setCancelable(true).show();
			} else
				Toast.makeText(this, getString(R.string.nowidgets),
						Toast.LENGTH_LONG).show();
			break;
		case R.id.refreshall:
			startService(new Intent(this, SonetService.class).putExtra(
					AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
			break;
		case R.id.donate:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DONATE)));
			break;
		}
	}

	private int[] arrayCat(int[] a, int[] b) {
		int[] c;
		for (int i = 0; i < b.length; i++) {
			c = new int[a.length];
			for (int n = 0; n < c.length; n++)
				c[n] = a[n];
			a = new int[c.length + 1];
			for (int n = 0; n < c.length; n++)
				a[n] = c[n];
			a[c.length] = b[i];
		}
		return a;
	}

	private int[] arrayPush(int[] a, int b) {
		int[] c = new int[a.length];
		for (int i = 0; i < a.length; i++)
			c[i] = a[i];
		a = new int[c.length + 1];
		for (int i = 0; i < c.length; i++)
			a[i] = c[i];
		a[a.length - 1] = b;
		return a;
	}

	private boolean arrayContains(int[] a, int b) {
		boolean contains = false;
		for (int c : a) {
			if (c == b) {
				contains = true;
				break;
			}
		}
		return contains;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		startActivity(new Intent(this, ManageAccounts.class).putExtra(
				AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[which]));
		dialog.cancel();
	}
}
