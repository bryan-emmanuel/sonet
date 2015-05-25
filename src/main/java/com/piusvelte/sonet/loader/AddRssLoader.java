package com.piusvelte.sonet.loader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.SonetHttpClient;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

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
 * Created by bemmanuel on 3/17/15.
 */
public class AddRssLoader extends BaseAsyncTaskLoader<String> {

    private static final String TAG = AddRssLoader.class.getSimpleName();

    @NonNull
    private Context mContext;
    @NonNull
    private String mUrl;

    public AddRssLoader(Context context, @NonNull String url) {
        super(context);

        mContext = context.getApplicationContext();
        mUrl = url;
    }

    @Override
    public String loadInBackground() {
        String httpResponse = SonetHttpClient.httpResponse(mUrl);

        if (!TextUtils.isEmpty(httpResponse)) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;

            try {
                db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(httpResponse));
                Document doc = db.parse(is);
                // test parsing...
                NodeList nodes = doc.getElementsByTagName(Sitem);

                if (nodes.getLength() > 0) {
                    // check for an image
                    NodeList images = doc.getElementsByTagName(Simage);

                    if (images.getLength() > 0) {
                        NodeList imageChildren = images.item(0).getChildNodes();
                        Node n = imageChildren.item(0);

                        if (n.getNodeName().toLowerCase().equals(Surl)) {
                            if (n.hasChildNodes()) {
                                n.getChildNodes().item(0).getNodeValue();
                            }
                        }
                    }

                    NodeList children = nodes.item(0).getChildNodes();
                    String date = null;
                    String title = null;
                    String description = null;
                    String link = null;
                    int values_count = 0;

                    for (int child = 0, c2 = children.getLength(); (child < c2) && (values_count < 4); child++) {
                        Node n = children.item(child);

                        if (n.getNodeName().toLowerCase().equals(Spubdate)) {
                            values_count++;

                            if (n.hasChildNodes()) {
                                date = n.getChildNodes().item(0).getNodeValue();
                            }
                        } else if (n.getNodeName().toLowerCase().equals(Stitle)) {
                            values_count++;

                            if (n.hasChildNodes()) {
                                title = n.getChildNodes().item(0).getNodeValue();
                            }
                        } else if (n.getNodeName().toLowerCase().equals(Sdescription)) {
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
                        } else if (n.getNodeName().toLowerCase().equals(Slink)) {
                            values_count++;

                            if (n.hasChildNodes()) {
                                link = n.getChildNodes().item(0).getNodeValue();
                            }
                        }
                    }

                    if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(description) && !TextUtils.isEmpty(link) && !TextUtils.isEmpty(date)) {
                        // feed contains expected fields, return the url to add the account
                        return mUrl;
                    }
                }
            } catch (ParserConfigurationException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, e.toString());
            } catch (SAXException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, e.toString());
            } catch (IOException e) {
                if (BuildConfig.DEBUG) Log.d(TAG, e.toString());
            }
        }

        return null;
    }
}
