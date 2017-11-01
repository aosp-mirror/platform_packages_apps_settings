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

package com.android.settings.search;

import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search.DatabaseIndexingManager.DatabaseRow;
import com.android.settings.search.DatabaseIndexingManager.DatabaseRow.Builder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DatabaseRowTest {
    private Builder mBuilder;

    private static final String LOCALE = "locale";
    private static final String UPDATED_TITLE = "updated title";
    private static final String NORMALIZED_TITLE = "normal title";
    private static final String UPDATED_SUMMARY_ON = "updated summary on";
    private static final String NORMALIZED_SUMMARY_ON = "normalized summary on";
    private static final String UPDATED_SUMMARY_OFF = "updated summary off";
    private static final String NORMALIZED_SUMMARY_OFF = "normalized summary off";
    private static final String ENTRIES = "entries";
    private static final String CLASS_NAME = "class name";
    private static final String SCREEN_TITLE = "screen title";
    private static final int ICON_RES_ID = 0xff;
    private static final int RANK = 1;
    private static final String SPACE_DELIMITED_KEYWORDS = "keywords";
    private static final String INTENT_ACTION = "intent action";
    private static final String INTENT_TARGET_PACKAGE = "target package";
    private static final String INTENT_TARGET_CLASS = "target class";
    private static final boolean ENABLED = true;
    private static final String KEY = "key";
    private static final int USER_ID = 1;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mBuilder = createBuilder();
    }

    @Test
    public void testFullRowBuild_nonNull() {
        DatabaseRow row = generateRow();
        assertThat(row).isNotNull();
    }

    @Test
    public void testPrimitivesBuild_noDataLoss() {
        DatabaseRow row = generateRow();

        assertThat(row.locale).isEqualTo(LOCALE);
        assertThat(row.updatedTitle).isEqualTo(UPDATED_TITLE);
        assertThat(row.normalizedTitle).isEqualTo(NORMALIZED_TITLE);
        assertThat(row.updatedSummaryOn).isEqualTo(UPDATED_SUMMARY_ON);
        assertThat(row.normalizedSummaryOn).isEqualTo(NORMALIZED_SUMMARY_ON);
        assertThat(row.updatedSummaryOff).isEqualTo(UPDATED_SUMMARY_OFF);
        assertThat(row.normalizedSummaryOff).isEqualTo(NORMALIZED_SUMMARY_OFF);
        assertThat(row.entries).isEqualTo(ENTRIES);
        assertThat(row.className).isEqualTo(CLASS_NAME);
        assertThat(row.screenTitle).isEqualTo(SCREEN_TITLE);
        assertThat(row.iconResId).isEqualTo(ICON_RES_ID);
        assertThat(row.rank).isEqualTo(RANK);
        assertThat(row.spaceDelimitedKeywords).isEqualTo(SPACE_DELIMITED_KEYWORDS);
        assertThat(row.intentAction).isEqualTo(INTENT_ACTION);
        assertThat(row.intentTargetClass).isEqualTo(INTENT_TARGET_CLASS);
        assertThat(row.intentTargetPackage).isEqualTo(INTENT_TARGET_PACKAGE);
        assertThat(row.enabled).isEqualTo(ENABLED);
        assertThat(row.userId).isEqualTo(USER_ID);
        assertThat(row.key).isEqualTo(KEY);
        assertThat(row.payloadType).isEqualTo(ResultPayload.PayloadType.INTENT);
        assertThat(row.payload).isNotNull();
    }

    @Test
    public void testGenericIntent_addedToPayload() {
        final DatabaseRow row = generateRow();
        final ResultPayload payload = ResultPayloadUtils.unmarshall(row.payload,
                ResultPayload.CREATOR);
        final ComponentName name = payload.getIntent().getComponent();
        assertThat(name.getClassName()).isEqualTo(INTENT_TARGET_CLASS);
        assertThat(name.getPackageName()).isEqualTo(INTENT_TARGET_PACKAGE);
    }

    @Test
    public void testRowWithInlinePayload_genericPayloadNotAdded() {
        final String URI = "test uri";
        final InlineSwitchPayload payload = new InlineSwitchPayload(URI, 0 /* mSettingSource */,
                1 /* onValue */, null /* intent */, true /* isDeviceSupported */, 1 /* default */);
        mBuilder.setPayload(payload);
        final DatabaseRow row = generateRow();
        final InlineSwitchPayload unmarshalledPayload = ResultPayloadUtils
                .unmarshall(row.payload, InlineSwitchPayload.CREATOR);

        assertThat(row.payloadType).isEqualTo(ResultPayload.PayloadType.INLINE_SWITCH);
        assertThat(unmarshalledPayload.mSettingKey).isEqualTo(URI);
    }

    @Test
    public void testRowWithInlinePayload_intentAddedToInlinePayload() {
        final String URI = "test uri";
        final ComponentName component = new ComponentName(INTENT_TARGET_PACKAGE,
                INTENT_TARGET_CLASS);
        final Intent intent = new Intent();
        intent.setComponent(component);

        final InlineSwitchPayload payload = new InlineSwitchPayload(URI, 0 /* mSettingSource */,
                1 /* onValue */, intent, true /* isDeviceSupported */, 1 /* default */);
        mBuilder.setPayload(payload);
        final DatabaseRow row = generateRow();
        final InlineSwitchPayload unmarshalledPayload = ResultPayloadUtils
                .unmarshall(row.payload, InlineSwitchPayload.CREATOR);
        final ComponentName name = unmarshalledPayload.getIntent().getComponent();

        assertThat(name.getClassName()).isEqualTo(INTENT_TARGET_CLASS);
        assertThat(name.getPackageName()).isEqualTo(INTENT_TARGET_PACKAGE);
    }


    private DatabaseRow generateRow() {
        return mBuilder.build(mContext);
    }

    private DatabaseRow.Builder createBuilder() {
        mBuilder = new DatabaseRow.Builder();
        mBuilder.setLocale(LOCALE)
                .setUpdatedTitle(UPDATED_TITLE)
                .setNormalizedTitle(NORMALIZED_TITLE)
                .setUpdatedSummaryOn(UPDATED_SUMMARY_ON)
                .setNormalizedSummaryOn(NORMALIZED_SUMMARY_ON)
                .setUpdatedSummaryOff(UPDATED_SUMMARY_OFF)
                .setNormalizedSummaryOff(NORMALIZED_SUMMARY_OFF)
                .setEntries(ENTRIES)
                .setClassName(CLASS_NAME)
                .setScreenTitle(SCREEN_TITLE)
                .setIconResId(ICON_RES_ID)
                .setRank(RANK)
                .setSpaceDelimitedKeywords(SPACE_DELIMITED_KEYWORDS)
                .setIntentAction(INTENT_ACTION)
                .setIntentTargetPackage(INTENT_TARGET_PACKAGE)
                .setIntentTargetClass(INTENT_TARGET_CLASS)
                .setEnabled(ENABLED)
                .setKey(KEY)
                .setUserId(USER_ID);
        return mBuilder;
    }
}