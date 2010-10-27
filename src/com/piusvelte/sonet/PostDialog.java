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

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class PostDialog extends Dialog implements OnClickListener {
	private Button mTwitter;
	private Button mFacebook;
	private Context mContext;

	public PostDialog(Context context) {
		super(context);
		mContext = context;
		setContentView(R.layout.post_dialog);
		mTwitter = (Button) findViewById(R.id.btn_twitter);
		mTwitter.setOnClickListener(this);
		mFacebook = (Button) findViewById(R.id.btn_facebook);
		mFacebook.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v == mTwitter) mContext.startActivity((new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com"))).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity")));
		else if (v == mFacebook) mContext.startActivity((new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com"))).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity")));
		dismiss();
	}

}
