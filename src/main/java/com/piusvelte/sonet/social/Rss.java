package com.piusvelte.sonet.social;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static com.piusvelte.sonet.Sonet.Sdescription;
import static com.piusvelte.sonet.Sonet.Simage;
import static com.piusvelte.sonet.Sonet.Sitem;
import static com.piusvelte.sonet.Sonet.Slink;
import static com.piusvelte.sonet.Sonet.Spubdate;
import static com.piusvelte.sonet.Sonet.Stitle;
import static com.piusvelte.sonet.Sonet.Surl;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class Rss extends Client {

    public Rss(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public String getProfileUrl(@NonNull String esid) {
        return null;
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return null;
    }

    @Override
    String getRequestUrl() {
        return null;
    }

    @Override
    String getAccessUrl() {
        return null;
    }

    @Override
    String getAuthorizeUrl() {
        return null;
    }

    @Override
    public String getCallbackUrl() {
        return null;
    }

    @Override
    boolean isOAuth10a() {
        return false;
    }

    @Override
    public MemberAuthentication getMemberAuthentication(@NonNull SonetOAuth sonetOAuth, @NonNull String authenticatedUrl) {
        return null;
    }

    @Nullable
    @Override
    public Set<String> getNotificationStatusIds(long account, String[] notificationMessage) {
        return null;
    }

    @Nullable
    @Override
    public String getFeedResponse(int status_count) {
        return SonetHttpClient.httpResponse(mAccountEsid);
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        // RSS isn't JSON, so we need to coerce this
        JSONArray feedItems = new JSONArray();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(response));
            Document doc = db.parse(is);
            NodeList nodes = doc.getElementsByTagName(Sitem);
            int i2 = nodes.getLength();

            if (i2 > 0) {
                // check for an image
                String image_url = null;
                NodeList images = doc.getElementsByTagName(Simage);
                int i3 = images.getLength();

                if (i3 > 0) {
                    NodeList imageChildren = images.item(0).getChildNodes();

                    for (int i = 0; (i < i3) && (image_url == null); i++) {
                        Node n = imageChildren.item(i);

                        if (n.getNodeName().toLowerCase().equals(Surl)) {
                            if (n.hasChildNodes()) {
                                image_url = n.getChildNodes().item(0).getNodeValue();
                            }
                        }
                    }
                }

                for (int i = 0; i < i2; i++) {
                    NodeList children = nodes.item(i).getChildNodes();
                    String date = null;
                    String title = null;
                    String description = null;
                    String link = null;
                    int values_count = 0;
                    for (int child = 0, c2 = children.getLength(); (child < c2) && (values_count < 4); child++) {
                        Node n = children.item(child);
                        final String nodeName = n.getNodeName().toLowerCase();

                        if (nodeName.equals(Spubdate)) {
                            values_count++;

                            if (n.hasChildNodes()) {
                                date = n.getChildNodes().item(0).getNodeValue();
                            }
                        } else if (nodeName.equals(Stitle)) {
                            values_count++;

                            if (n.hasChildNodes()) {
                                title = n.getChildNodes().item(0).getNodeValue();
                            }
                        } else if (nodeName.equals(Sdescription)) {
                            values_count++;

                            if (n.hasChildNodes()) {
                                StringBuilder sb = new StringBuilder();
                                NodeList descNodes = n.getChildNodes();

                                for (int dn = 0, dn2 = descNodes.getLength(); dn < dn2; dn++) {
                                    Node descNode = descNodes.item(dn);

                                    if (descNode.getNodeType() == Node.TEXT_NODE) {
                                        sb.append(descNode.getNodeValue());
                                    }
                                }

                                // strip out the html tags
                                description = sb.toString().replaceAll("\\<(.|\n)*?>", "");
                            }
                        } else if (nodeName.equals(Slink)) {
                            values_count++;

                            if (n.hasChildNodes()) {
                                link = n.getChildNodes().item(0).getNodeValue();
                            }
                        }
                    }

                    if (Sonet.HasValues(new String[] { title, description, link, date })) {
                        feedItems.put(new JSONObject()
                                .put(Stitle, title)
                                .put(Sdescription, description)
                                .put(Slink, link)
                                .put(Spubdate, parseDate(date, null))
                                .put(Simage, image_url)
                                .toString());
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            Log.e(mTag, "RSS:" + e.toString());
        } catch (SAXException e) {
            Log.e(mTag, "RSS:" + e.toString());
        } catch (IOException e) {
            Log.e(mTag, "RSS:" + e.toString());
        }

        return feedItems;
    }

    @Nullable
    @Override
    public void addFeedItem(@NonNull JSONObject item,
            boolean display_profile,
            boolean time24hr,
            int appWidgetId,
            long account,
            Set<String> notificationSids,
            String[] notificationMessage,
            boolean doNotify) throws JSONException {
        addStatusItem(item.getLong(Spubdate), item.getString(Stitle), display_profile ? item.getString(Simage) : null, item.getString(Sdescription),
                time24hr, appWidgetId, account, null, item.getString(Slink), new ArrayList<String[]>());
    }

    @Nullable
    @Override
    public void getNotificationMessage(long account, String[] notificationMessage) {
        // NO-OP
    }

    @Override
    public void getNotifications(long account, String[] notificationMessage) {
        // NO-OP
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        return false;
    }

    @Override
    public boolean isLikeable(String statusId) {
        return false;
    }

    @Override
    public boolean isLiked(String statusId, String accountId) {
        return false;
    }

    @Override
    public boolean likeStatus(String statusId, String accountId, boolean doLike) {
        return false;
    }

    @Override
    public String getLikeText(boolean isLiked) {
        return null;
    }

    @Override
    public boolean isCommentable(String statusId) {
        return false;
    }

    @Override
    public String getCommentPretext(String accountId) {
        return null;
    }

    @Nullable
    @Override
    public String getCommentsResponse(String statusId) {
        return null;
    }

    @Nullable
    @Override
    public JSONArray parseComments(@NonNull String response) throws JSONException {
        return null;
    }

    @Nullable
    @Override
    public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr) throws JSONException {
        return null;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        return null;
    }

    @Override
    public boolean sendComment(@NonNull String statusId, @NonNull String message) {
        return false;
    }

    @Override
    public List<HashMap<String, String>> getFriends() {
        return null;
    }

    @Override
    String getApiKey() {
        return null;
    }

    @Override
    String getApiSecret() {
        return null;
    }
}
