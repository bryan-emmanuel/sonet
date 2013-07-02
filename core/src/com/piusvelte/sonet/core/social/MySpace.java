package com.piusvelte.sonet.core.social;

import static com.piusvelte.sonet.core.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_DATE_FORMAT;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_HISTORY;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_STATUSMOODCOMMENTS_BODY;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_STATUSMOOD_BODY;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_URL_STATUSMOOD;
import static com.piusvelte.sonet.core.Sonet.MYSPACE_URL_STATUSMOODCOMMENTS;
import static com.piusvelte.sonet.core.Sonet.Sauthor;
import static com.piusvelte.sonet.core.Sonet.Sbody;
import static com.piusvelte.sonet.core.Sonet.ScommentId;
import static com.piusvelte.sonet.core.Sonet.SdisplayName;
import static com.piusvelte.sonet.core.Sonet.Sentry;
import static com.piusvelte.sonet.core.Sonet.Simgur;
import static com.piusvelte.sonet.core.Sonet.Slink;
import static com.piusvelte.sonet.core.Sonet.SmoodStatusLastUpdated;
import static com.piusvelte.sonet.core.Sonet.SpostedDate;
import static com.piusvelte.sonet.core.Sonet.SrecentComments;
import static com.piusvelte.sonet.core.Sonet.Sstatus;
import static com.piusvelte.sonet.core.Sonet.SstatusId;
import static com.piusvelte.sonet.core.Sonet.SthumbnailUrl;
import static com.piusvelte.sonet.core.Sonet.SuserId;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;

public class MySpace extends SocialNetwork {

	private String entityId;

	protected MySpace(Credential credential, HttpClient httpClient, String entityId) {
		super(credential, httpClient);
	}

	@Override
	public List<Status> getFeed(int limit) {
		ArrayList<Status> statuses = new ArrayList<Status>();
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(MYSPACE_HISTORY, MYSPACE_BASE_URL))));
		if (response != null) {
			try {
				JSONArray statusesArray = new JSONObject(response).getJSONArray(Sentry);
				for (int e = 0, s = statusesArray.length(); ((e < s) && (e < limit)); e++) {
					ArrayList<Link> links = new ArrayList<Link>();
					JSONObject statusObj = statusesArray.getJSONObject(e);
					JSONObject friendObj = statusObj.getJSONObject(Sauthor);
					long date = parseDate(statusObj.getString(SmoodStatusLastUpdated), MYSPACE_DATE_FORMAT);
					String esid = statusObj.getString(SuserId);
					int commentCount = 0;
					String sid = statusObj.getString(SstatusId);
					String friend = friendObj.getString(SdisplayName);
					String message = statusObj.getString(Sstatus);
					String imageURL = null;
					Matcher m = Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(message.toString());
					StringBuffer sb = new StringBuffer(message.length());
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
							m.appendReplacement(sb, "(" + Slink + ": " + Uri.parse(link).getHost() + ")");
						}
					}
					m.appendTail(sb);
					message = sb.toString();
					List<Comment> comments;
					if (statusObj.has(SrecentComments)) {
						comments = parseComments(statusObj.getJSONArray(SrecentComments));
						commentCount = comments.size();
					} else {
						comments = new ArrayList<Comment>();
					}
					Entity entity = new Entity(esid, friend, friendObj.getString(SthumbnailUrl));
					statuses.add(new Status(sid,
							String.format(Sonet.messageWithCommentCount, message, commentCount),
							entity,
							links,
							imageURL,
							date)
					.setComments(comments));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return statuses;
	}
	
	private List<Comment> parseComments(JSONArray commentsJarr) throws JSONException {
		ArrayList<Comment> comments = new ArrayList<Comment>();
		for (int i = 0, s = commentsJarr.length(); i < s; i++) {
			JSONObject entry = commentsJarr.getJSONObject(i);
			JSONObject author = entry.getJSONObject(Sauthor);
			Entity entity = new Entity(author.getString(Sonet.Sid),
					author.getString(SdisplayName),
					"");
			comments.add(new Comment(entry.getString(ScommentId),
					entry.getString(Sbody),
					entity,
					"",
					parseDate(entry.getString(SpostedDate), MYSPACE_DATE_FORMAT)));
		}
		return comments;
	}

	@Override
	public List<Comment> getComments(String statusId) {
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, entityId, statusId))));
		if (response != null) {
			try {
				return parseComments(new JSONObject(response).getJSONArray(Sentry));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return new ArrayList<Comment>();
	}

	@Override
	public boolean post(String message, Location location, String[] tags) {
		HttpPut httpPut = new HttpPut(String.format(MYSPACE_URL_STATUSMOOD, MYSPACE_BASE_URL));
		try {
			httpPut.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOOD_BODY, message)));
			return (SonetHttpClient.httpResponse(httpClient, credential.sign(httpPut)) != null);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean comment(String statusId, String message) {
		String[] idParts = statusId.split(" ", -1);
		if (idParts.length != 2) {
			return false;
		}
		HttpPost httpPost = new HttpPost(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, idParts[0], idParts[1]));
		try {
			httpPost.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOODCOMMENTS_BODY, message)));
			return SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public List<Location> getLocations(double latitude, double longitude) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean like(String statusId, boolean like) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getLikeStatus(String statusId, String accountServiceId) {
		// TODO Auto-generated method stub
		return null;
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

}
