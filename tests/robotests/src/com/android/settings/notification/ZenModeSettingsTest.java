/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertTrue;

import android.app.NotificationManager;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class ZenModeSettingsTest {

    private ZenModeSettings.SummaryBuilder mBuilder;
    private Context mContext;
    private ZenModeSettings mSettings;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mSettings = new ZenModeSettings();
        mBuilder = new ZenModeSettings.SummaryBuilder(mContext);
    }

    @Test
    public void testGetBehaviorSettingSummary_sameOrderAsTargetPage() {
        NotificationManager.Policy policy = new NotificationManager.Policy(
                NotificationManager.Policy.PRIORITY_CATEGORY_EVENTS
                        | NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS
                        | NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
                        | NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA_SYSTEM_OTHER,
                0, 0);
        final String result = mBuilder.getBehaviorSettingSummary(policy,
                Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);

        String alarms = mContext.getString(R.string.zen_mode_alarms).toLowerCase();
        String reminders = mContext.getString(R.string.zen_mode_reminders).toLowerCase();
        String events = mContext.getString(R.string.zen_mode_events).toLowerCase();
        String media = mContext.getString(R.string.zen_mode_media_system_other).toLowerCase();

        assertThat(result).contains(alarms);
        assertThat(result).contains(reminders);
        assertThat(result).contains(events);
        assertThat(result).contains(media);
        assertTrue(result.indexOf(alarms) < result.indexOf(media)
                && result.indexOf(media) < result.indexOf(reminders)
                && result.indexOf(reminders) < result.indexOf(events));
    }

    @Test
    public void searchProvider_shouldIndexDefaultXml() {
        final List<SearchIndexableResource> sir = mSettings.SEARCH_INDEX_DATA_PROVIDER
                .getXmlResourcesToIndex(mContext, true /* enabled */);

        assertThat(sir).hasSize(1);
        assertThat(sir.get(0).xmlResId).isEqualTo(R.xml.zen_mode_settings);
    }

}
