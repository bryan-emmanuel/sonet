package com.piusvelte.sonet.core.social;

import static com.piusvelte.sonet.core.Sonet.Screated_at;
import static com.piusvelte.sonet.core.Sonet.Sfull_name;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.Simgur;
import static com.piusvelte.sonet.core.Sonet.Sin_reply_to_status_id;
import static com.piusvelte.sonet.core.Sonet.Slink;
import static com.piusvelte.sonet.core.Sonet.Sname;
import static com.piusvelte.sonet.core.Sonet.Splaces;
import static com.piusvelte.sonet.core.Sonet.Sprofile_image_url;
import static com.piusvelte.sonet.core.Sonet.Sresult;
import static com.piusvelte.sonet.core.Sonet.Sstatus;
import static com.piusvelte.sonet.core.Sonet.Stext;
import static com.piusvelte.sonet.core.Sonet.Suser;
import static com.piusvelte.sonet.core.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.TWITTER_DATE_FORMAT;
import static com.piusvelte.sonet.core.Sonet.TWITTER_MENTIONS;
import static com.piusvelte.sonet.core.Sonet.TWITTER_RETWEET;
import static com.piusvelte.sonet.core.Sonet.TWITTER_SEARCH;
import static com.piusvelte.sonet.core.Sonet.TWITTER_SINCE_ID;
import static com.piusvelte.sonet.core.Sonet.TWITTER_UPDATE;
import static com.piusvelte.sonet.core.Sonet.TWITTER_URL_FEED;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;

public class Twitter extends SocialNetwork {

	protected Twitter(Credential credential, HttpClient httpClient) {
		super(credential, httpClient);
	}

	@Override
	public List<Status> getFeed(int limit) {
		ArrayList<Status> statuses = new ArrayList<Status>();
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(TWITTER_URL_FEED, TWITTER_BASE_URL, limit))));
		if (response != null) {
			// if not a full_refresh, only update the status_bg and icons
			try {
				JSONArray statusesArray = new JSONArray(response);
				// if there are updates, clear the cache
				int e2 = statusesArray.length();
				if (e2 > 0) {
					for (int e = 0; (e < e2) && (e < limit); e++) {
						JSONObject statusObj = statusesArray.getJSONObject(e);
						JSONObject friendObj = statusObj.getJSONObject(Suser);
						Entity entity = new Entity(friendObj.getString(Sid),
								friendObj.getString(Sname),
								friendObj.getString(Sprofile_image_url));
						String message = statusObj.getString(Stext);
						
						String imageURL = null;
						ArrayList<Link> links = new ArrayList<Link>();
						
						Matcher m = Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(message);
//						StringBuffer sb = new StringBuffer(message.length());
						while (m.find()) {
							String link = m.group();
							// check existing links before adding
							boolean exists = false;
							for (Link l : links) {
								if (l.getLocation().equals(link)) {
									exists = true;
									break;
								}
							}
							if (!exists) {
								links.add(new Link(Slink, link));
								if (imageURL == null) {
									Uri uri = Uri.parse(link);
									if ((uri != null) && uri.getHost().equals(Simgur)) {
										imageURL = link;
									}
								}
//								if ((service != TWITTER) && (service != IDENTICA))
//									m.appendReplacement(sb, "(" + Slink + ": " + Uri.parse(link).getHost() + ")");
							}
						}
//						m.appendTail(sb);
//						message = sb.toString();
						
						Status status = new Status(statusObj.getString(Sid),
								message,
								entity,
								links,
								imageURL,
								parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT));
						statuses.add(status);
						
//						addStatusItem(parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT),
//								friendObj.getString(Sname),
//								display_profile ? friendObj.getString(Sprofile_image_url) : null,
//										statusObj.getString(Stext),
//										service,
//										time24hr,
//										appWidgetId,
//										account,
//										statusObj.getString(Sid),
//										friendObj.getString(Sid),
//										links,
//										httpClient);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return statuses;
	}

	@Override
	public List<Comment> getComments(String statusId) {
		ArrayList<Comment> comments = new ArrayList<Comment>();
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(TWITTER_MENTIONS, TWITTER_BASE_URL, String.format(TWITTER_SINCE_ID, statusId != null ? String.format(TWITTER_SINCE_ID, statusId) : "")))));
		if (response != null) {
			try {
				JSONArray commentsJArr = new JSONArray(response);
				for (int i = 0, s = commentsJArr.length(); i < s; i++) {
					JSONObject comment = commentsJArr.getJSONObject(i);
					if (comment.getString(Sin_reply_to_status_id) == statusId) {
						JSONObject friendObj = comment.getJSONObject(Suser);
						Entity entity = new Entity(friendObj.getString(Sid),
								friendObj.getString(Sname),
								friendObj.getString(Sprofile_image_url));
						comments.add(new Comment(comment.getString(Sid),
								comment.getString(Stext),
								entity,
								"retweet",
								parseDate(comment.getString(Screated_at), TWITTER_DATE_FORMAT)));
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return comments;
	}

	@Override
	public boolean post(String message, Location location, String[] tags) {
		// limit tweets to 140, breaking up the message if necessary
		boolean success = true;
		while ((message.length() > 0) && success) {
			String send;
			if (message.length() > 140) {
				// need to break on a word
				int end = 0;
				int nextSpace = 0;
				for (int i = 0, i2 = message.length(); i < i2; i++) {
					end = nextSpace;
					if (message.substring(i, i + 1).equals(" ")) {
						nextSpace = i;
					}
				}
				// in case there are no spaces, just break on 140
				if (end == 0)
					end = 140;
				send = message.substring(0, end);
				message = message.substring(end + 1);
			} else {
				send = message;
				message = "";
			}
			HttpPost httpPost = new HttpPost(String.format(TWITTER_UPDATE, TWITTER_BASE_URL));
			// resolve Error 417 Expectation by Twitter
			httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			postParams.add(new BasicNameValuePair(Sstatus, send));
			if (location != null) {
				postParams.add(new BasicNameValuePair("place_id", location.getId()));
				postParams.add(new BasicNameValuePair("lat", Double.toString(location.getLatitude())));
				postParams.add(new BasicNameValuePair("long", Double.toString(location.getLongitude())));
			}
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(postParams));
				success = (SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				success = false;
			}
		}
		return success;
	}

	@Override
	public boolean comment(String statusId, String message) {
		// limit tweets to 140, breaking up the message if necessary
		boolean success = true;
		while ((message.length() > 0) && success) {
			String send;
			if (message.length() > 140) {
				// need to break on a word
				int end = 0;
				int nextSpace = 0;
				for (int i = 0, i2 = message.length(); i < i2; i++) {
					end = nextSpace;
					if (message.substring(i, i + 1).equals(" "))
						nextSpace = i;
				}
				// in case there are no spaces, just break on 140
				if (end == 0)
					end = 140;
				send = message.substring(0, end);
				message = message.substring(end + 1);
			} else {
				send = message;
				message = "";
			}
			HttpPost httpPost = new HttpPost(String.format(TWITTER_UPDATE, TWITTER_BASE_URL));
			// resolve Error 417 Expectation by Twitter
			httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
			List<NameValuePair> postParams = new ArrayList<NameValuePair>();
			postParams.add(new BasicNameValuePair(Sstatus, send));
			postParams.add(new BasicNameValuePair(Sin_reply_to_status_id, statusId));
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(postParams));
				success = SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				success = false;
			}
		}
		return success;
	}
	
	@Override
	public boolean like(String statusId, boolean like) {
		HttpPost httpPost = new HttpPost(String.format(TWITTER_RETWEET, TWITTER_BASE_URL, statusId));
		// resolve Error 417 Expectation by Twitter
		httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
		return (SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null);
	}

	@Override
	public String getLikeStatus(String statusId, String accountServiceId) {
		return "retweet";
	}

	private SimpleDateFormat mSimpleDateFormat;
	
	public long parseDate(String date, String format) {
		if (date != null) {
			// hack for the literal 'Z'
			if (date.substring(date.length() - 1).equals("Z")) {
				date = date.substring(0, date.length() - 2) + "+0000";
			}
			Date created = null;
			if (format != null) {
				if (mSimpleDateFormat == null) {
					mSimpleDateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
					// all dates should be GMT/UTC
					mSimpleDateFormat.setTimeZone(Sonet.sTimeZone);
				}
				try {
					created = mSimpleDateFormat.parse(date);
					return created.getTime();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else {
				// attempt to parse RSS date
				if (mSimpleDateFormat != null) {
					try {
						created = mSimpleDateFormat.parse(date);
						return created.getTime();
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				for (String rfc822 : Sonet.sRFC822) {
					mSimpleDateFormat = new SimpleDateFormat(rfc822, Locale.ENGLISH);
					mSimpleDateFormat.setTimeZone(Sonet.sTimeZone);
					try {
						if ((created = mSimpleDateFormat.parse(date)) != null) {
							return created.getTime();
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return System.currentTimeMillis();
	}

	@Override
	public List<Location> getLocations(double latitude, double longitude) {
		ArrayList<Location> locations = new ArrayList<Location>();
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(TWITTER_SEARCH, TWITTER_BASE_URL, latitude, longitude))));
		if (response != null) {
			try {
				JSONArray places = new JSONObject(response).getJSONObject(Sresult).getJSONArray(Splaces);
				for (int i = 0, i2 = places.length(); i < i2; i++) {
					JSONObject place = places.getJSONObject(i);
					locations.add(new Location(place.getString(Sid), place.getString(Sfull_name), latitude, longitude));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return locations;
	}

}
