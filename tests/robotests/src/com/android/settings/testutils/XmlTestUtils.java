package com.android.settings.testutils;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import com.android.settings.search.XmlParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.List;

/**
 * Util class for parsing XML
 */
public class XmlTestUtils {

    /**
     * Parses a preference screen's xml, collects and returns all keys used by preferences
     * on the screen.
     *
     * @param context of the preference screen.
     * @param xmlId of the Preference Xml to be parsed.
     * @return List of all keys in the preference Xml
     */
    public static List<String> getKeysFromPreferenceXml(Context context, int xmlId) {
        final XmlResourceParser parser = context.getResources().getXml(xmlId);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        final List<String> keys = new ArrayList<>();
        String key;
        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                try {
                    key = XmlParserUtils.getDataKey(context, attrs);
                    if (!TextUtils.isEmpty(key)) {
                        keys.add(key);
                    }
                } catch (NullPointerException e) {
                    continue;
                } catch (Resources.NotFoundException e) {
                    continue;
                }
            }
        } catch (java.io.IOException e) {
            return null;
        } catch (XmlPullParserException e) {
            return null;
        }

        return keys;
    }
}