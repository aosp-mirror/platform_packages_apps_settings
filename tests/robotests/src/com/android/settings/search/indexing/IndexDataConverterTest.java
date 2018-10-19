/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 */

package com.android.settings.search.indexing;

import android.content.Context;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.search.ResultPayload;
import com.android.settings.search.ResultPayloadUtils;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(qualifiers = "mcc999")
public class IndexDataConverterTest {

    private static final String localeStr = "en_US";

    private static final String title = "title\u2011title";
    private static final String updatedTitle = "title-title";
    private static final String normalizedTitle = "titletitle";
    private static final String summaryOn = "summary\u2011on";
    private static final String updatedSummaryOn = "summary-on";
    private static final String summaryOff = "summary\u2011off";
    private static final String entries = "entries";
    private static final String keywords = "keywords, keywordss, keywordsss";
    private static final String spaceDelimittedKeywords = "keywords keywordss keywordsss";
    private static final String screenTitle = "screen title";
    private static final String className = "class name";
    private static final int iconResId = 0xff;
    private static final String action = "action";
    private static final String targetPackage = "target package";
    private static final String targetClass = "target class";
    private static final String packageName = "package name";
    private static final String key = "key";
    private static final int userId = -1;
    private static final boolean enabled = true;

    // There are 6 entries in the fake display_settings.xml preference.
    private static final int NUM_DISPLAY_ENTRIES = 6;
    private static final String PAGE_TITLE = "page_title";
    private static final String TITLE_ONE = "pref_title_1";
    private static final String TITLE_TWO = "pref_title_2";
    private static final String TITLE_THREE = "pref_title_3";
    private static final String TITLE_FOUR = "pref_title_4";
    private static final String TITLE_FIVE = "pref_title_5";
    private static final String DISPLAY_SPACE_DELIM_KEYWORDS = "keywords1 keywords2 keywords3";

    // There is a title and one preference.
    private static final int NUM_LEGAL_SETTINGS = 2;

    private Context mContext;

    private IndexDataConverter mConverter;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mConverter = spy(new IndexDataConverter(mContext));
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testInsertRawColumn_rowConverted() {
        final SearchIndexableRaw raw = getFakeRaw();
        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(raw);
        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        assertThat(indexData.size()).isEqualTo(1);
        final IndexData row = indexData.get(0);

        assertThat(row.normalizedTitle).isEqualTo(normalizedTitle);
        assertThat(row.updatedTitle).isEqualTo(updatedTitle);
        assertThat(row.locale).isEqualTo(localeStr);
        assertThat(row.updatedSummaryOn).isEqualTo(updatedSummaryOn);
        assertThat(row.entries).isEqualTo(entries);
        assertThat(row.spaceDelimitedKeywords).isEqualTo(spaceDelimittedKeywords);
        assertThat(row.screenTitle).isEqualTo(screenTitle);
        assertThat(row.className).isEqualTo(className);
        assertThat(row.iconResId).isEqualTo(iconResId);
        assertThat(row.intentAction).isEqualTo(action);
        assertThat(row.intentTargetPackage).isEqualTo(targetPackage);
        assertThat(row.intentTargetClass).isEqualTo(targetClass);
        assertThat(row.enabled).isEqualTo(enabled);
        assertThat(row.key).isEqualTo(key);
        assertThat(row.userId).isEqualTo(userId);
        assertThat(row.payloadType).isEqualTo(0);
        ResultPayload unmarshalledPayload =
            ResultPayloadUtils.unmarshall(row.payload, ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    @Test
    public void testInsertRawColumn_nonIndexableKey_resultIsDisabled() {
        final SearchIndexableRaw raw = getFakeRaw();
        // Add non-indexable key for raw row.
        Set<String> keys = new HashSet<>();
        keys.add(raw.key);

        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(raw);
        preIndexData.nonIndexableKeys.put(raw.intentTargetPackage, keys);

        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        assertThat(indexData.size()).isEqualTo(1);
        assertThat(indexData.get(0).enabled).isFalse();
    }

    /**
     * TODO (b/66916397) investigate why locale is attached to IndexData
     */
    @Test
    public void testInsertRawColumn_mismatchedLocale_rowInserted() {
        final SearchIndexableRaw raw = getFakeRaw("ca-fr");
        PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(raw);
        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        assertThat(indexData).hasSize(1);
    }

    // Tests for the flow: IndexOneResource -> IndexFromResource ->
    //                     UpdateOneRowWithFilteredData -> UpdateOneRow

    @Test
    public void testNullResource_NothingInserted() {
        PreIndexData preIndexData = new PreIndexData();
        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        assertThat(indexData).isEmpty();
    }

    @Test
    public void testAddResource_RowsInserted() {
        final SearchIndexableResource resource = getFakeResource(R.xml.display_settings);
        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(resource);

        final List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);
        int numEnabled = getEnabledResultCount(indexData);

        assertThat(numEnabled).isEqualTo(NUM_DISPLAY_ENTRIES);
    }

    @Test
    public void testAddResource_withNIKs_rowsInsertedDisabled() {
        final SearchIndexableResource resource = getFakeResource(R.xml.display_settings);
        Set<String> keys = new HashSet<>();
        keys.add("pref_key_1");
        keys.add("pref_key_3");

        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(resource);
        preIndexData.nonIndexableKeys.put(packageName, keys);

        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        assertThat(indexData.size()).isEqualTo(NUM_DISPLAY_ENTRIES);
        assertThat(getEnabledResultCount(indexData)).isEqualTo(NUM_DISPLAY_ENTRIES - 2);
    }

    @Test
    public void testAddResourceHeader_rowsMatch() {
        final SearchIndexableResource resource = getFakeResource(R.xml.display_settings);
        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(resource);

        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        final IndexData row = findIndexDataForTitle(indexData, PAGE_TITLE);

        // Header exists
        assertThat(row).isNotNull();
        assertThat(row.spaceDelimitedKeywords).isEqualTo("keywords");
    }

    @Test
    public void testAddResource_checkboxPreference_rowsMatch() {
        final SearchIndexableResource resource = getFakeResource(R.xml.display_settings);
        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(resource);

        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        String checkBoxSummaryOn = "summary_on";
        String checkBoxKey = "pref_key_5";
        final IndexData row = findIndexDataForTitle(indexData, TITLE_FIVE);

        assertDisplaySetting(row, TITLE_FIVE, checkBoxSummaryOn, checkBoxKey);
    }

    @Test
    public void testAddResource_listPreference_rowsMatch() {
        final SearchIndexableResource resource = getFakeResource(R.xml.display_settings);
        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(resource);

        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        String listSummary = "summary_4";
        String listKey = "pref_key_4";
        final IndexData row = findIndexDataForTitle(indexData, TITLE_FOUR);

        assertDisplaySetting(row, TITLE_FOUR, listSummary, listKey);
    }

    @Test
    public void testAddResource_iconAddedFromXml() {
        final SearchIndexableResource resource = getFakeResource(R.xml.display_settings);
        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(resource);

        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        final IndexData row = findIndexDataForTitle(indexData, TITLE_THREE);

        assertThat(row).isNotNull();
        assertThat(row.iconResId).isGreaterThan(0);
    }

    @Test
    public void testResource_sameTitleForSettingAndPage_titleNotInserted() {
        final SearchIndexableResource resource = getFakeResource(R.xml.about_legal);
        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(resource);

        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        int numEnabled = getEnabledResultCount(indexData);
        final IndexData nonTitlePref = findIndexDataForKey(indexData, "pref_key_1");

        assertThat(indexData.size()).isEqualTo(NUM_LEGAL_SETTINGS - 1);
        assertThat(numEnabled).isEqualTo(NUM_LEGAL_SETTINGS - 1);
        assertThat(nonTitlePref).isNotNull();
        assertThat(nonTitlePref.enabled).isTrue();
    }

    @Test
    public void testResourceWithoutXml_shouldNotCrash() {
        final SearchIndexableResource resource = getFakeResource(0);
        final PreIndexData preIndexData = new PreIndexData();
        preIndexData.dataToUpdate.add(resource);

        List<IndexData> indexData = mConverter.convertPreIndexDataToIndexData(preIndexData);

        assertThat(indexData).isEmpty();
    }

    private void assertDisplaySetting(IndexData row, String title, String summaryOn, String key) {
        assertThat(row.normalizedTitle).isEqualTo(title);
        assertThat(row.locale).isEqualTo(localeStr);
        assertThat(row.updatedSummaryOn).isEqualTo(summaryOn);
        assertThat(row.spaceDelimitedKeywords).isEqualTo(DISPLAY_SPACE_DELIM_KEYWORDS);
        assertThat(row.screenTitle).isEqualTo(PAGE_TITLE);
        assertThat(row.className).isEqualTo(className);
        assertThat(row.enabled).isEqualTo(true);
        assertThat(row.key).isEqualTo(key);
        assertThat(row.payloadType).isEqualTo(0);
        ResultPayload unmarshalledPayload =
            ResultPayloadUtils.unmarshall(row.payload, ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    private SearchIndexableRaw getFakeRaw() {
        return getFakeRaw(localeStr);
    }

    private SearchIndexableRaw getFakeRaw(String localeStr) {
        SearchIndexableRaw data = new SearchIndexableRaw(mContext);
        data.locale = new Locale(localeStr);
        data.title = title;
        data.summaryOn = summaryOn;
        data.summaryOff = summaryOff;
        data.entries = entries;
        data.keywords = keywords;
        data.screenTitle = screenTitle;
        data.className = className;
        data.packageName = packageName;
        data.iconResId = iconResId;
        data.intentAction = action;
        data.intentTargetPackage = targetPackage;
        data.intentTargetClass = targetClass;
        data.key = key;
        data.userId = userId;
        data.enabled = enabled;
        return data;
    }

    private SearchIndexableResource getFakeResource(int xml) {
        SearchIndexableResource sir = new SearchIndexableResource(mContext);
        sir.xmlResId = xml;
        sir.className = className;
        sir.packageName = packageName;
        sir.iconResId = iconResId;
        sir.intentAction = action;
        sir.intentTargetPackage = targetPackage;
        sir.intentTargetClass = targetClass;
        sir.enabled = enabled;
        return sir;
    }

    private static int getEnabledResultCount(List<IndexData> indexData) {
        int enabledCount = 0;
        for (IndexData data : indexData) {
            if (data.enabled) {
                enabledCount++;
            }
        }
        return enabledCount;
    }

    private static IndexData findIndexDataForTitle(List<IndexData> indexData,
            String indexDataTitle) {
        for (IndexData row : indexData) {
            if (TextUtils.equals(row.updatedTitle, indexDataTitle)) {
                return row;
            }
        }
        return null;
    }

    private static IndexData findIndexDataForKey(List<IndexData> indexData, String indexDataKey) {
        for (IndexData row : indexData) {
            if (TextUtils.equals(row.key, indexDataKey)) {
                return row;
            }
        }
        return null;
    }
}
