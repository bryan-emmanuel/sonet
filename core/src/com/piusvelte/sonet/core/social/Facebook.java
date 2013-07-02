package com.piusvelte.sonet.core.social;

import static com.piusvelte.sonet.core.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_COMMENTS;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_HOME;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_PHOTOS;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_PICTURE;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_POST;
import static com.piusvelte.sonet.core.Sonet.FACEBOOK_SEARCH;
import static com.piusvelte.sonet.core.Sonet.Saccess_token;
import static com.piusvelte.sonet.core.Sonet.Scomments;
import static com.piusvelte.sonet.core.Sonet.Screated_time;
import static com.piusvelte.sonet.core.Sonet.Sdata;
import static com.piusvelte.sonet.core.Sonet.Sfrom;
import static com.piusvelte.sonet.core.Sonet.Sid;
import static com.piusvelte.sonet.core.Sonet.Simgur;
import static com.piusvelte.sonet.core.Sonet.Slink;
import static com.piusvelte.sonet.core.Sonet.Smessage;
import static com.piusvelte.sonet.core.Sonet.Sname;
import static com.piusvelte.sonet.core.Sonet.Sphoto;
import static com.piusvelte.sonet.core.Sonet.Spicture;
import static com.piusvelte.sonet.core.Sonet.Splace;
import static com.piusvelte.sonet.core.Sonet.Ssource;
import static com.piusvelte.sonet.core.Sonet.Sstory;
import static com.piusvelte.sonet.core.Sonet.Stags;
import static com.piusvelte.sonet.core.Sonet.Sto;
import static com.piusvelte.sonet.core.Sonet.Stype;
import static com.piusvelte.sonet.core.Sonet.Suser_likes;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetHttpClient;

public class Facebook extends SocialNetwork {

	protected Facebook(Credential credential, HttpClient httpClient) {
		super(credential, httpClient);
	}

	@Override
	public List<Comment> getComments(String statusId) {
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, statusId))));
		if (response != null) {
			try {
				return parseComments(new JSONObject(response).getJSONArray(Sdata));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return new ArrayList<Comment>();
	}

	private List<Comment> parseComments(JSONArray commentsJArr) throws JSONException {
		ArrayList<Comment> comments = new ArrayList<Comment>();
		for (int i = 0, l = commentsJArr.length(); i < l; i++) {
			JSONObject comment = commentsJArr.getJSONObject(i);
			JSONObject from = comment.getJSONObject(Sfrom);
			String entityId = from.getString(Sid);
			Entity entity = new Entity(entityId,
					from.getString(Sname),
					String.format(FACEBOOK_PICTURE, entityId));
			comments.add(new Comment(comment.getString(Sid),
					comment.getString(Smessage),
					entity,
					(comment.has(Suser_likes) && comment.getBoolean(Suser_likes) ? "unlike" : "like"),
					(comment.getLong(Screated_time) * 1000)));
		}
		return comments;
	}

	@Override
	public boolean post(String message, Location location, String[] tags) {
		StringBuilder tagString = null;
		if (tags != null) {
			tagString = new StringBuilder();
			tagString.append("[");
			String tag_format = "%s";
			//			if (params[PHOTO] != null) {
			//				tag_format = "{\"tag_uid\":\"%s\",\"x\":0,\"y\":0}";
			//			}
			tagString.append(String.format(tag_format, tags[0]));
			for (int t = 1; t < tags.length; t++) {
				tagString.append(String.format(tag_format, tags[t]));
				tagString.append(",");
			}
			tagString.append("]");
		}
		// regular post
		HttpPost httpPost = new HttpPost(String.format(FACEBOOK_POST, FACEBOOK_BASE_URL, Saccess_token));
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair(Smessage, message));
		if (location != null) {
			postParams.add(new BasicNameValuePair(Splace, location.getId()));
		}
		if (tagString != null) {
			postParams.add(new BasicNameValuePair(Stags, tagString.toString()));
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(postParams));
			if (SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null) {
				return true;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean comment(String statusId, String message) {
		HttpPost httpPost = new HttpPost(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, statusId));
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair(Smessage, message));
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(postParams));
			return (SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost)) != null);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public List<Location> getLocations(double latitude, double longitude) {
		ArrayList<Location> locations = new ArrayList<Location>();
		HttpGet httpGet = new HttpGet(String.format(FACEBOOK_SEARCH, FACEBOOK_BASE_URL, latitude, longitude));
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(httpGet));
		try {
			JSONArray places = new JSONObject(response).getJSONArray(Sdata);
			for (int i = 0, i2 = places.length(); i < i2; i++) {
				JSONObject place = places.getJSONObject(i);
				locations.add(new Location(place.getString(Sid), place.getString(Sname), latitude, longitude));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return locations;
	}

	@Override
	public boolean like(String statusId, boolean like) {
		if (like) {
			return (SonetHttpClient.httpResponse(httpClient,
					credential.sign(new HttpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, statusId)))) != null);
		} else {
			HttpDelete httpDelete = new HttpDelete(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, statusId));
			httpDelete.setHeader("Content-Length", "0");
			return (SonetHttpClient.httpResponse(httpClient, credential.sign(httpDelete)) != null);
		}
	}

	@Override
	public String getLikeStatus(String statusId, String accountServiceId) {
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, statusId))));
		if (response != null) {
			try {
				JSONArray likes = new JSONObject(response).getJSONArray(Sdata);
				for (int i = 0, i2 = likes.length(); i < i2; i++) {
					JSONObject like = likes.getJSONObject(i);
					if (like.getString(Sid).equals(accountServiceId)) {
						return "unlike";
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return "like";
	}

	@Override
	public List<Status> getFeed(int limit) {
		ArrayList<Status> statuses = new ArrayList<Status>();
		String response = SonetHttpClient.httpResponse(httpClient, credential.sign(new HttpGet(String.format(FACEBOOK_HOME, FACEBOOK_BASE_URL))));
		if (response != null) {
			try {
				JSONArray statusesArray = new JSONObject(response).getJSONArray(Sdata);
				for (int d = 0, d2 = statusesArray.length(); ((d < d2) && (d < limit)); d++) {
					JSONObject statusObj = statusesArray.getJSONObject(d);
					// only parse status types, not photo, video or link
					if (statusObj.has(Stype) && statusObj.has(Sfrom) && statusObj.has(Sid)) {
						ArrayList<Link> links = new ArrayList<Link>();
						JSONObject friendObj = statusObj.getJSONObject("from");
						if (friendObj.has(Sname) && friendObj.has(Sid)) {
							String friend = friendObj.getString(Sname);
							String esid = friendObj.getString(Sid);
							Entity entity = new Entity(esid, friend, String.format(FACEBOOK_PICTURE, esid));
							String sid = statusObj.getString(Sid);
							StringBuilder message = new StringBuilder();
							if (statusObj.has(Smessage)) {
								message.append(statusObj.getString(Smessage));
							} else if (statusObj.has(Sstory)) {
								message.append(statusObj.getString(Sstory));
							}
							if (statusObj.has(Spicture)) {
								links.add(new Link(Spicture, statusObj.getString(Spicture)));
							}
							if (statusObj.has(Slink)) {
								links.add(new Link(statusObj.getString(Stype), statusObj.getString(Slink)));
								if (!statusObj.has(Spicture) || !statusObj.getString(Stype).equals(Sphoto)) {
									message.append("(");
									message.append(statusObj.getString(Stype));
									message.append(": ");
									message.append(Uri.parse(statusObj.getString(Slink)).getHost());
									message.append(")");
								}
							}
							if (statusObj.has(Ssource)) {
								links.add(new Link(statusObj.getString(Stype), statusObj.getString(Ssource)));
								if (!statusObj.has(Spicture) || !statusObj.getString(Stype).equals(Sphoto)) {
									message.append("(");
									message.append(statusObj.getString(Stype));
									message.append(": ");
									message.append(Uri.parse(statusObj.getString(Ssource)).getHost());
									message.append(")");
								}
							}
							long date = statusObj.getLong(Screated_time) * 1000;
							if (statusObj.has(Sto)) {
								// handle wall messages from one friend to another
								JSONObject t = statusObj.getJSONObject(Sto);
								if (t.has(Sdata)) {
									JSONObject n = t.getJSONArray(Sdata).getJSONObject(0);
									if (n.has(Sname)) {
										friend += " > " + n.getString(Sname);
									}
								}
							}
							List<Comment> comments;
							if (statusObj.has(Scomments)) {
								JSONObject jo = statusObj.getJSONObject(Scomments);
								if (jo.has(Sdata)) {
									comments = parseComments(jo.getJSONArray(Sdata));
								} else {
									comments = new ArrayList<Comment>();
								}
							} else {
								comments = new ArrayList<Comment>();
							}
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
							message = new StringBuilder(sb.toString());
							statuses.add(new Status(sid,
									String.format(Sonet.messageWithCommentCount, message.toString(), comments.size()),
									entity,
									links,
									imageURL,
									date)
							.setComments(comments));
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return statuses;
	}
	
	public String postPhoto(String message, String filePath, String location, String tags) {
		String result = null;
		HttpPost httpPost = new HttpPost(String.format(FACEBOOK_PHOTOS, FACEBOOK_BASE_URL, Saccess_token));
		MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		File file = new File(filePath);
		ContentBody fileBody = new FileBody(file);
		entity.addPart(Ssource, fileBody);
		try {
			entity.addPart(Smessage, new StringBody(message));
			if (location != null) {
				entity.addPart(Splace, new StringBody(location));
			}
			if (tags != null) {
				entity.addPart(Stags, new StringBody(tags));
			}
			httpPost.setEntity(entity);
			result = SonetHttpClient.httpResponse(httpClient, credential.sign(httpPost));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	//TODO return Intent for starting service?
	//	public void postPhoto(String photoPath, String message, String location) {
	//		// upload photo
	//		// uploading a photo takes a long time, have the service handle it
	//		Intent i = Sonet.getPackageIntent(activity.getApplicationContext(), PhotoUploadService.class);
	//		i.setAction(Sonet.ACTION_UPLOAD);
	//		i.putExtra(Accounts.TOKEN, account.getString(1));
	//		i.putExtra(Widgets.INSTANT_UPLOAD, params[PHOTO]);
	//		i.putExtra(Statuses.MESSAGE, params[MESSAGE]);
	//		i.putExtra(Splace, params[LOCATION]);
	//		if (tags != null)
	//			i.putExtra(Stags, tags.toString());
	//		activity.startService(i);
	//	}

}
