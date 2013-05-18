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
package com.piusvelte.sonet.core.task.foursquare;

import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_ADDCOMMENT;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_BASE_URL;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;

import com.piusvelte.sonet.core.SonetHttpClient;

import android.util.Log;

public class Foursquare {

	private static final String TAG = "Foursquare";

	String token;
	HttpClient httpClient;

	public Foursquare(String token, HttpClient httpClient) {
		this.token = token;
		this.httpClient = httpClient;
	}

	public boolean comment(String statusId, String message) {
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(String.format(FOURSQUARE_ADDCOMMENT, FOURSQUARE_BASE_URL, statusId, URLEncoder.encode(message, "UTF-8"), token));
			return SonetHttpClient.httpResponse(httpClient, httpPost) != null;
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.getMessage());
		}
		return false;
	}

}
