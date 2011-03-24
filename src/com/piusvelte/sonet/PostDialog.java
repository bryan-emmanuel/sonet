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
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.LINKEDIN;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class PostDialog extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] services = getResources().getStringArray(R.array.service_entries);
		CharSequence[] items = new CharSequence[services.length];
		for (int i = 0; i < services.length; i++) items[i] = services[i];
		(new AlertDialog.Builder(this))
		.setItems(items, this)
		.setCancelable(true)
		.setOnCancelListener(this)
		.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case TWITTER:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com")));
			break;
		case FACEBOOK:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com")));
			break;
		case MYSPACE:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.myspace.com")));
			break;
		case BUZZ:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com/buzz")));
			break;
		case FOURSQUARE:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.foursquare.com")));
			break;
		case LINKEDIN:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.linkedin.com")));
			break;
		}
		dialog.cancel();
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}	

}
