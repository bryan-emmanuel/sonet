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
package com.piusvelte.sonet.core.task.chatter;

import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_ACCESS;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_LIKE;
import static com.piusvelte.sonet.core.Sonet.CHATTER_URL_LIKES;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.Stotal;
import static com.piusvelte.sonet.core.Sonet.Suser;
import static com.piusvelte.sonet.core.SonetTokens.CHATTER_KEY;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.activity.SonetComments;
import com.piusvelte.sonet.core.task.LikeTask;

import android.database.Cursor;
import android.util.Log;

public class ChatterLikeTask extends LikeTask {
	
	private static final String TAG = "ChatterLikeTask";

	public ChatterLikeTask(SonetComments activity, long accountId) {
		super(activity, accountId);
	}

	@Override
	protected String doInBackground(String... params) {
		String result = activity.getString(R.string.like);
		Cursor account = activity.getContentResolver().query(Accounts.getContentUri(activity), new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SID}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
		if (account.moveToFirst()) {
			String response = SonetHttpClient.httpResponse(httpClient, new HttpPost(String.format(CHATTER_URL_ACCESS, CHATTER_KEY, sonetCrypto.Decrypt(account.getString(1)))));
			if (response != null) {
				try {
					JSONObject jobj = new JSONObject(response);
					if (jobj.has("instance_url") && jobj.has(Saccess_token)) {
						String instance = jobj.getString("instance_url");
						String token = jobj.getString(Saccess_token);
						String accountSid = sonetCrypto.Decrypt(account.getString(2));
						if (Boolean.parseBoolean(params[LIKE])) {
							HttpPost httpPost = new HttpPost(String.format(CHATTER_URL_LIKES, instance, params[ID]));
							httpPost.setHeader("Authorization", "OAuth " + token);
							if (SonetHttpClient.httpResponse(httpClient, httpPost) != null) {
								publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.success));
								result = activity.getString(R.string.unlike);
							} else
								publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.failure));
						} else {
							HttpGet httpGet = new HttpGet(String.format(CHATTER_URL_LIKES, instance, params[ID]));
							httpGet.setHeader("Authorization", "OAuth " + token);
							if ((response = SonetHttpClient.httpResponse(httpClient, httpGet)) != null) {
								try {
									jobj = new JSONObject(response);
									if (jobj.getInt(Stotal) > 0) {
										JSONArray likes = jobj.getJSONArray("likes");
										for (int i = 0, i2 = likes.length(); i < i2; i++) {
											JSONObject like = likes.getJSONObject(i);
											if (like.getJSONObject(Suser).getString(Sid).equals(accountSid)) {
												HttpDelete httpDelete = new HttpDelete(String.format(CHATTER_URL_LIKE, jobj.getString("instance_url"), like.getString(Sid)));
												httpDelete.setHeader("Authorization", "OAuth " + jobj.getString(Saccess_token));
												if (SonetHttpClient.httpResponse(httpClient, httpDelete) != null)
													publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.success));
												else {
													publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.failure));
													result = activity.getString(R.string.unlike);
												}
												break;
											}
										}
									}
								} catch (JSONException e) {
									Log.e(TAG,e.toString());
								}
							}	
						}
					}
				} catch (JSONException e) {
					Log.e(TAG, e.getMessage());
					publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.failure));
				}
			} else
				publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.failure));
		} else
			publishProgress(Sonet.getServiceName(activity.getResources(), Sonet.CHATTER), activity.getString(R.string.failure));
		account.close();
		return result;
	}

}
