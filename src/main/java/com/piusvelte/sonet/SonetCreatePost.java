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

import android.content.Intent;
import android.os.Bundle;

import com.piusvelte.sonet.fragment.CreatePost;

public class SonetCreatePost extends BaseActivity {
    private static final String TAG = "SonetCreatePost";

    private static final String FRAGMENT_CREATE_POST = "fragment:create_post";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // allow posting to multiple services if an account is defined
        // allow selecting which accounts to use
        // get existing comments, allow liking|unliking those comments
        setContentView(R.layout.post_container);
        setupActionBar();
        setupAd();

        setResult(RESULT_OK);

        CreatePost createPost = (CreatePost) getSupportFragmentManager().findFragmentByTag(FRAGMENT_CREATE_POST);

        if (createPost == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, CreatePost.newInstance(getIntent().getExtras()), FRAGMENT_CREATE_POST)
                    .commit();
        }
    }

//    protected void selectFriends(long accountId) {
        // TODO unsupported
//        if ((mAccountsService.get(accountId) == FACEBOOK) && (!mAccountsLocation.containsKey(accountId) || (mAccountsLocation
//                .get(accountId) == null))) {
//            (Toast.makeText(SonetCreatePost.this, "To tag friends, Facebook requires a location to be included.", Toast.LENGTH_LONG)).show();
//        } else {
//            startActivityForResult(
//                    new Intent(this, SelectFriends.class).putExtra(Accounts.SID, accountId).putExtra(Stags, mAccountsTags.get(accountId)),
//                    TAGS);
//        }
//    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent data) {
        // NO-OP
    }
}