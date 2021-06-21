/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
