package com.android.settings.testutils;

import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_KEY;
import static com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag
        .FLAG_INCLUDE_PREF_SCREEN;
import static com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag.FLAG_NEED_KEY;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.android.settings.core.PreferenceXmlParserUtils;

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
     * @param xmlId   of the Preference Xml to be parsed.
     * @return List of all keys in the preference Xml
     */
    public static List<String> getKeysFromPreferenceXml(Context context, int xmlId) {
        final List<String> keys = new ArrayList<>();
        try {
            List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(context, xmlId,
                    FLAG_NEED_KEY | FLAG_INCLUDE_PREF_SCREEN);
            for (Bundle bundle : metadata) {
                final String key = bundle.getString(METADATA_KEY);
                if (!TextUtils.isEmpty(key)) {
                    keys.add(key);
                }
            }
        } catch (java.io.IOException | XmlPullParserException e) {
            return null;
        }

        return keys;
    }
}