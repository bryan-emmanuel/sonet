package com.piusvelte.sonet.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.social.Client;

import java.util.HashMap;
import java.util.List;

/**
 * Created by bemmanuel on 5/16/15.
 */
public class AccountProfileAdapter extends AccountAdapter {

    public AccountProfileAdapter(Context context, List<HashMap<String, String>> account) {
        super(context, account);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView profile;
        ImageView icon;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.account_profile, parent, false);
        }

        profile = (ImageView) convertView.findViewById(R.id.profile);
        icon = (ImageView) convertView.findViewById(R.id.icon);

        HashMap<String, String> account = getItem(position);
        String url = getAccountProfileUrl(account);

        if (!TextUtils.isEmpty(url)) {
            mPicasso.load(url)
                    .transform(mCircleTransformation)
                    .into(profile);
        } else {
            mPicasso.load(R.drawable.ic_account_box_grey600_48dp)
                    .transform(mCircleTransformation)
                    .into(profile);
        }

        int service = getAccountService(account);
        Client.Network network = Client.Network.get(service);
        mPicasso.load(network.getIcon())
                .into(icon);

        return convertView;
    }
}
