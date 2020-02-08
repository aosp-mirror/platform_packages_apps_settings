/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.core;

import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_APPEND;
import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_KEY;
import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_KEYWORDS;
import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_SEARCHABLE;
import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_UNAVAILABLE_SLICE_SUBTITLE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;

import com.android.settings.R;
import com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * These tests use a series of preferences that have specific attributes which are sometimes
 * uncommon (such as summaryOn).
 *
 * If changing a preference file breaks a test in this test file, please replace its reference
 * with another preference with a matchin replacement attribute.
 */
@RunWith(RobolectricTestRunner.class)
public class PreferenceXmlParserUtilsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testDataTitleValid_ReturnsPreferenceTitle() {
        XmlResourceParser parser = getChildByType(R.xml.display_settings,
                "com.android.settings.display.TimeoutListPreference");
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        String title = PreferenceXmlParserUtils.getDataTitle(mContext, attrs);
        String expTitle = mContext.getString(R.string.screen_timeout);
        assertThat(title).isEqualTo(expTitle);
    }

    @Test
    public void testDataKeywordsValid_ReturnsPreferenceKeywords() {
        XmlResourceParser parser = getChildByType(R.xml.display_settings,
                "com.android.settings.display.TimeoutListPreference");
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        String keywords = PreferenceXmlParserUtils.getDataKeywords(mContext, attrs);
        String expKeywords = mContext.getString(R.string.keywords_screen_timeout);
        assertThat(keywords).isEqualTo(expKeywords);
    }

    @Test
    public void testDataKeyValid_ReturnsPreferenceKey() {
        XmlResourceParser parser = getChildByType(R.xml.display_settings,
                "com.android.settings.display.TimeoutListPreference");
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        String key = PreferenceXmlParserUtils.getDataKey(mContext, attrs);
        String expKey = "screen_timeout";
        assertThat(key).isEqualTo(expKey);
    }

    @Test
    public void testDataSummaryValid_ReturnsPreferenceSummary() {
        XmlResourceParser parser = getChildByType(R.xml.display_settings,
                "com.android.settings.display.TimeoutListPreference");
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        String summary = PreferenceXmlParserUtils.getDataSummary(mContext, attrs);
        String expSummary = mContext.getString(R.string.summary_placeholder);
        assertThat(summary).isEqualTo(expSummary);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testDataSummaryOnOffValid_ReturnsPreferenceSummaryOnOff() {
        XmlResourceParser parser = getChildByType(R.xml.display_settings, "CheckBoxPreference");
        final AttributeSet attrs = Xml.asAttributeSet(parser);

        assertThat(PreferenceXmlParserUtils.getDataSummaryOn(mContext, attrs))
                .isEqualTo("summary_on");
        assertThat(PreferenceXmlParserUtils.getDataSummaryOff(mContext, attrs))
                .isEqualTo("summary_off");
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testDataEntriesValid_ReturnsPreferenceEntries() {
        XmlResourceParser parser = getChildByType(R.xml.display_settings, "ListPreference");
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        String entries = PreferenceXmlParserUtils.getDataEntries(mContext, attrs);
        String[] expEntries = mContext.getResources()
                .getStringArray(R.array.app_install_location_entries);
        for (String expEntry : expEntries) {
            assertThat(entries).contains(expEntry);
        }
    }

    // Null checks
    @Test
    @Config(qualifiers = "mcc999")
    public void testDataKeyInvalid_ReturnsNull() {
        XmlResourceParser parser = getParentPrimedParser(R.xml.display_settings);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        String key = PreferenceXmlParserUtils.getDataKey(mContext, attrs);
        assertThat(key).isNull();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testControllerAttribute_returnsValidData() {
        XmlResourceParser parser = getChildByType(R.xml.about_legal, "Preference");
        final AttributeSet attrs = Xml.asAttributeSet(parser);

        String controller = PreferenceXmlParserUtils.getController(mContext, attrs);
        assertThat(controller).isEqualTo("mind_flayer");
    }

    @Test
    public void testDataSummaryInvalid_ReturnsNull() {
        XmlResourceParser parser = getParentPrimedParser(R.xml.display_settings);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        String summary = PreferenceXmlParserUtils.getDataSummary(mContext, attrs);
        assertThat(summary).isNull();
    }

    @Test
    public void testDataSummaryOffInvalid_ReturnsNull() {
        XmlResourceParser parser = getParentPrimedParser(R.xml.display_settings);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        String summaryOff = PreferenceXmlParserUtils.getDataSummaryOff(mContext, attrs);
        assertThat(summaryOff).isNull();
    }

    @Test
    public void testDataEntriesInvalid_ReturnsNull() {
        XmlResourceParser parser = getParentPrimedParser(R.xml.display_settings);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        String entries = PreferenceXmlParserUtils.getDataEntries(mContext, attrs);
        assertThat(entries).isNull();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_shouldContainKeyAndControllerName()
            throws IOException, XmlPullParserException {
        List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.location_settings,
                MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_NEED_PREF_CONTROLLER);

        assertThat(metadata).isNotEmpty();
        for (Bundle bundle : metadata) {
            assertThat(bundle.getString(PreferenceXmlParserUtils.METADATA_KEY)).isNotNull();
            assertThat(bundle.getString(PreferenceXmlParserUtils.METADATA_CONTROLLER)).isNotNull();
        }
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestTitle_shouldContainTitle()
            throws IOException, XmlPullParserException {
        List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.location_settings, MetadataFlag.FLAG_NEED_PREF_TITLE);
        for (Bundle bundle : metadata) {
            assertThat(bundle.getString(PreferenceXmlParserUtils.METADATA_TITLE)).isNotNull();
        }
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestSummary_shouldContainSummary()
            throws IOException, XmlPullParserException {
        List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.location_settings, MetadataFlag.FLAG_NEED_PREF_SUMMARY);
        for (Bundle bundle : metadata) {
            assertThat(bundle.getString(PreferenceXmlParserUtils.METADATA_SUMMARY)).isNotNull();
        }
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestIcon_shouldContainIcon()
            throws IOException, XmlPullParserException {
        List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.location_settings, MetadataFlag.FLAG_NEED_PREF_ICON);
        for (Bundle bundle : metadata) {
            assertThat(bundle.getInt(PreferenceXmlParserUtils.METADATA_ICON)).isNotEqualTo(0);
        }
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestPrefType_shouldContainPrefType()
            throws IOException, XmlPullParserException {
        List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.location_settings, MetadataFlag.FLAG_NEED_PREF_TYPE);
        for (Bundle bundle : metadata) {
            assertThat(bundle.getString(PreferenceXmlParserUtils.METADATA_PREF_TYPE)).isNotNull();
        }
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestIncludeScreen_shouldContainScreen()
            throws IOException, XmlPullParserException {
        List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.location_settings,
                MetadataFlag.FLAG_NEED_PREF_TYPE | MetadataFlag.FLAG_INCLUDE_PREF_SCREEN);

        boolean hasPreferenceScreen = false;
        for (Bundle bundle : metadata) {
            if (TextUtils.equals(bundle.getString(PreferenceXmlParserUtils.METADATA_PREF_TYPE),
                    PreferenceXmlParserUtils.PREF_SCREEN_TAG)) {
                hasPreferenceScreen = true;
                break;
            }
        }

        assertThat(hasPreferenceScreen).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestIncludesKeywords_shouldContainKeywords() throws Exception {
        final String expectedKeywords = "a, b, c";
        final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.location_settings,
                MetadataFlag.FLAG_NEED_PREF_TYPE | MetadataFlag.FLAG_NEED_KEYWORDS);
        final Bundle bundle = metadata.get(0);

        final String keywords = bundle.getString(METADATA_KEYWORDS);

        assertThat(keywords).isEqualTo(expectedKeywords);
    }

    @Test
    @Config(qualifiers = "mcc998")
    public void extractMetadata_requestSearchable_shouldDefaultToTrue() throws Exception {
        final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.location_settings, MetadataFlag.FLAG_NEED_SEARCHABLE);
        for (Bundle bundle : metadata) {
            assertThat(bundle.getBoolean(METADATA_SEARCHABLE)).isTrue();
        }
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestSearchable_shouldReturnAttributeValue() throws Exception {
        final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.display_settings,
                MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_NEED_SEARCHABLE);
        boolean foundKey = false;
        for (Bundle bundle : metadata) {
            if (TextUtils.equals(bundle.getString(METADATA_KEY), "pref_key_5")) {
                assertThat(bundle.getBoolean(METADATA_SEARCHABLE)).isFalse();
                foundKey = true;
                break;
            }
        }
        assertThat(foundKey).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestAppendProperty_shouldDefaultToFalse()
            throws Exception {
        final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.display_settings,
                MetadataFlag.FLAG_INCLUDE_PREF_SCREEN | MetadataFlag.FLAG_NEED_PREF_APPEND);

        for (Bundle bundle : metadata) {
            assertThat(bundle.getBoolean(METADATA_APPEND)).isFalse();
        }
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestAppendProperty_shouldReturnCorrectValue()
            throws Exception {
        final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.battery_saver_schedule_settings,
                MetadataFlag.FLAG_INCLUDE_PREF_SCREEN | MetadataFlag.FLAG_NEED_PREF_APPEND);

        for (Bundle bundle : metadata) {
            assertThat(bundle.getBoolean(METADATA_APPEND)).isTrue();
        }
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestUnavailableSliceSubtitle_shouldDefaultNull()
            throws Exception {
        final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.night_display_settings,
                MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_UNAVAILABLE_SLICE_SUBTITLE);

        boolean bundleWithKey1Found = false;
        for (Bundle bundle : metadata) {
            if (bundle.getString(METADATA_KEY).equals("key1")) {
                assertThat(bundle.getString(METADATA_UNAVAILABLE_SLICE_SUBTITLE)).isNull();
                bundleWithKey1Found = true;
                break;
            }
        }
        assertThat(bundleWithKey1Found).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void extractMetadata_requestUnavailableSliceSubtitle_shouldReturnAttributeValue()
            throws Exception {
        final String expectedSubtitle = "subtitleOfUnavailable";
        final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(mContext,
                R.xml.night_display_settings,
                MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_UNAVAILABLE_SLICE_SUBTITLE);

        boolean bundleWithKey2Found = false;
        for (Bundle bundle : metadata) {
            if (bundle.getString(METADATA_KEY).equals("key2")) {
                assertThat(bundle.getString(METADATA_UNAVAILABLE_SLICE_SUBTITLE)).isEqualTo(
                        expectedSubtitle);
                bundleWithKey2Found = true;
                break;
            }
        }
        assertThat(bundleWithKey2Found).isTrue();
    }

    /**
     * @param resId the ID for the XML preference
     * @return an XML resource parser that points to the start tag
     */
    private XmlResourceParser getParentPrimedParser(int resId) {
        XmlResourceParser parser = null;
        try {
            parser = mContext.getResources().getXml(resId);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
        } catch (Exception e) {

        }
        return parser;
    }

    private XmlResourceParser getChildByType(int resId, String xmlType) {
        XmlResourceParser parser = null;
        try {
            parser = mContext.getResources().getXml(resId);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
            while (true) {
                if (Objects.equals(parser.getName(), xmlType)) {
                    break;
                }
                final int nextEvent = parser.next();
                if (nextEvent == XmlPullParser.END_DOCUMENT) {
                    break;
                }
            }
        } catch (Exception e) {

        }
        return parser;
    }
}
