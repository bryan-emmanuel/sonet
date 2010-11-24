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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class SonetProvider extends ContentProvider {

    public static final String AUTHORITY = "com.piusvelte.sonet.provider";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final Uri CONTENT_URI_MESSAGES = CONTENT_URI.buildUpon().appendEncodedPath("data").build();
    
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int URI_DATA = 0;
    
    public static final String[] PROJECTION_APPWIDGETS = new String[] { DataProviderColumns._id.toString(),
    	DataProviderColumns.created.toString(),
    	DataProviderColumns.link.toString(),
    	DataProviderColumns.friend.toString(),
    	DataProviderColumns.profile.toString(),
    	DataProviderColumns.message.toString(),
    	DataProviderColumns.service.toString() };
    
    static {
            URI_MATCHER.addURI(AUTHORITY, "data/*", URI_DATA);
    }

    public enum DataProviderColumns {
		_id, created, link, friend, profile, message, service
    }
    
	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
        switch (URI_MATCHER.match(uri)) {

        case URI_DATA:
        	// pull from statuses

        default:
                throw new IllegalStateException("Unrecognized URI:" + uri);
        }
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

}
