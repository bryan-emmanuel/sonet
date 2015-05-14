package com.piusvelte.sonet.adapter;

import android.content.Context;
import android.support.annotation.MenuRes;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Created by bemmanuel on 5/13/15.
 */
public class MenuItemAdapter extends BaseAdapter {

    private final Context mContext;
    private final Menu mMenu;

    public MenuItemAdapter(Context context, @MenuRes int menuResId) {
        mContext = context;
        mMenu = new MenuBuilder(mContext);

        MenuInflater mi = new MenuInflater(mContext);
        mi.inflate(menuResId, mMenu);
    }

    @Override
    public int getCount() {
        return mMenu.size();
    }

    @Override
    public MenuItem getItem(int position) {
        return mMenu.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getItemId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        MenuItem menuItem = getItem(position);
        convertView.setId(menuItem.getItemId());

        if (viewHolder != null) {
            viewHolder.title.setText(menuItem.getTitle());
            viewHolder.title.setCompoundDrawables(menuItem.getIcon(), null, null, null);
        }

        return convertView;
    }

    static class ViewHolder {
        public TextView title;
    }
}
