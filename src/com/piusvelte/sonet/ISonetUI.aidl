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

interface ISonetUI {
	void setDefaultSettings(int interval_value, int buttons_bg_color_value, int buttons_color_value, int buttons_textsize_value, int messages_bg_color_value, int messages_color_value, int messages_textsize_value, int friend_color_value, int friend_textsize_value, int created_color_value, int created_textsize_value, boolean hasButtons, boolean time24hr);
	void listAccounts();
	void getAuth(int service);
	void getTimezone(int account);
	void buildScrollableWidget(int messages_color, int friend_color, int created_color, int friend_textsize, int created_textsize, int messages_textsize);
	void widgetOnClick(boolean hasbuttons, int service, String link);
}