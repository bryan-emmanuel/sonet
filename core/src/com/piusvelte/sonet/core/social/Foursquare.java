package com.piusvelte.sonet.core.social;

import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_ADDCOMMENT;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_CHECKIN;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_CHECKINS;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_CHECKIN_NO_SHOUT;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_CHECKIN_NO_VENUE;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_GET_CHECKIN;
import static com.piusvelte.sonet.core.Sonet.FOURSQUARE_SEARCH;
import static com.piusvelte.sonet.core.Sonet.SNearby;
import static com.piusvelte.sonet.core.Sonet.Scheckin;
import static com.piusvelte.sonet.core.Sonet.Scomments;
import static com.piusvelte.sonet.core.Sonet.ScreatedAt;
import static com.piusvelte.sonet.core.Sonet.SfirstName;
import static com.piusvelte.sonet.core.Sonet.Sgroups;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.Simgur;
import static com.piusvelte.sonet.core.Sonet.Sitems;
import static com.piusvelte.sonet.core.Sonet.SlastName;
import static com.piusvelte.sonet.core.Sonet.Slink;
import static com.piusvelte.sonet.core.Sonet.Sname;
import static com.piusvelte.sonet.core.Sonet.Sphoto;
import static com.piusvelte.sonet.core.Sonet.Srecent;
import static com.piusvelte.sonet.core.Sonet.Sresponse;
import static com.piusvelte.sonet.core.Sonet.Sshout;
import static com.piusvelte.sonet.core.Sonet.Stext;
import static com.piusvelte.sonet.core.Sonet.Suser;
import static com.piusvelte.sonet.core.Sonet.Svenue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;

public class Foursquare extends SocialNetwork {

	protected Foursquare(Credential credential, HttpClient httpClient) {
		super(credential, httpClient);
	}

	@Override
	public List<Status> getFeed(int limit) {
		// TODO Auto-generated method stub
		ArrayList<Status> statuses = new ArrayList<Status>();
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(FOURSQUARE_CHECKINS, FOURSQUARE_BASE_URL))));
		if (response != null) {
			try {
				JSONArray statusesArray = new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Srecent);
				// if there are updates, clear the cache
				for (int i = 0, s = statusesArray.length(); ((i < s) && (i < limit)); i++) {
					JSONObject statusObj = statusesArray.getJSONObject(i);
					JSONObject friendObj = statusObj.getJSONObject(Suser);
					String shout = "";
					if (statusObj.has(Sshout)) {
						shout = statusObj.getString(Sshout) + "\n";
					}
					if (statusObj.has(Svenue)) {
						JSONObject venue = statusObj.getJSONObject(Svenue);
						if (venue.has(Sname)) {
							shout += "@" + venue.getString(Sname);
						}
					}
					long date = statusObj.getLong(ScreatedAt) * 1000;
					// notifications
					String esid = friendObj.getString(Sid);
					String sid = statusObj.getString(Sid);
					String friend = friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName);
					Entity entity = new Entity(esid, friend, friendObj.getString(Sphoto));
					List<Comment> comments = parseComments(statusObj.getJSONObject(Scomments).getJSONArray(Sitems));
					int commentCount = comments.size();
					String imageURL = null;
					ArrayList<Link> links = new ArrayList<Link>();
					Matcher m = Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(shout);
					StringBuffer sb = new StringBuffer(shout.length());
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
					shout = sb.toString();
					statuses.add(new Status(sid,
							String.format(Sonet.messageWithCommentCount, shout, commentCount),
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

	@Override
	public List<Comment> getComments(String statusId) {
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, statusId))));
		if (response != null) {
			try {
				return parseComments(new JSONObject(response).getJSONObject(Sresponse).getJSONObject(Scheckin).getJSONObject(Scomments).getJSONArray(Sitems));
			} catch (JSONException e) {
				e.printStackTrace();
				return new ArrayList<Comment>();
			}
		} else {
			return new ArrayList<Comment>();
		}
	}
	
	private List<Comment> parseComments(JSONArray commentsJarr) throws JSONException {
		ArrayList<Comment> comments = new ArrayList<Comment>();
		for (int i = 0, s = commentsJarr.length(); i < s; i++) {
			JSONObject comment = commentsJarr.getJSONObject(i);
			JSONObject user = comment.getJSONObject(Suser);
			Entity entity = new Entity(user.getString(Sid),
					user.getString(SfirstName) + " " + user.getString(SlastName),
					null);
			comments.add(new Comment(comment.getString(Sid),
					comment.getString(Stext),
					entity,
					"",
					comment.getLong(ScreatedAt) * 1000));
		}
		return comments;
	}

	@Override
	public boolean post(String message, Location location, String[] tags) {
		HttpPost httpPost = null;
		if (location != null) {
			if (message != null) {
				try {
					httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN, FOURSQUARE_BASE_URL, location.getId(), URLEncoder.encode(message, "UTF-8"), location.getLatitude(), location.getLongitude()));
					return (SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else {
				httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_SHOUT, FOURSQUARE_BASE_URL, location.getId(), location.getLatitude(), location.getLongitude()));
				return (SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null);
			}
		} else {
			try {
				httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_VENUE, FOURSQUARE_BASE_URL, URLEncoder.encode(message, "UTF-8")));
				return (SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public boolean comment(String statusId, String message) {
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(String.format(FOURSQUARE_ADDCOMMENT, FOURSQUARE_BASE_URL, statusId, URLEncoder.encode(message, "UTF-8")));
			return SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public List<Location> getLocations(double latitude, double longitude) {
		ArrayList<Location> locations = new ArrayList<Location>();
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(FOURSQUARE_SEARCH, FOURSQUARE_BASE_URL, latitude, longitude))));
		if (response != null) {
			try {
				JSONArray groups = new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Sgroups);
				if (groups.length() > 0) {
					JSONObject group = groups.getJSONObject(0);
					if (group.getString(Sname).equals(SNearby)) {
						JSONArray places = group.getJSONArray(Sitems);
						for (int i = 0, i2 = places.length(); i < i2; i++) {
							JSONObject place = places.getJSONObject(i);
							locations.add(new Location(place.getString(Sid),
									place.getString(Sname),
									latitude,
									longitude));
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return locations;
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

}
