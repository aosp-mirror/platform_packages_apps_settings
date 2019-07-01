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
 */

package com.android.settings.dream;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settings.R;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.WhenToDream;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DreamSettingsTest {

    private static final List<String> KEYS = Arrays.asList(
        DreamSettings.WHILE_CHARGING_ONLY,
        DreamSettings.WHILE_DOCKED_ONLY,
        DreamSettings.EITHER_CHARGING_OR_DOCKED,
        DreamSettings.NEVER_DREAM
    );

    private static final @WhenToDream int[] SETTINGS = {
        DreamBackend.WHILE_CHARGING,
        DreamBackend.WHILE_DOCKED,
        DreamBackend.EITHER,
        DreamBackend.NEVER,
    };

    private static final int[] RES_IDS = {
        R.string.screensaver_settings_summary_sleep,
        R.string.screensaver_settings_summary_dock,
        R.string.screensaver_settings_summary_either_long,
        R.string.screensaver_settings_summary_never
    };

    @Test
    public void getSettingFromPrefKey() {
        for (int i = 0; i < KEYS.size(); i++) {
            assertThat(DreamSettings.getSettingFromPrefKey(KEYS.get(i))).isEqualTo(SETTINGS[i]);
        }
        // Default case
        assertThat(DreamSettings.getSettingFromPrefKey("garbage value"))
                .isEqualTo(DreamBackend.NEVER);
    }

    @Test
    public void getKeyFromSetting() {
        for (int i = 0; i < SETTINGS.length; i++) {
            assertThat(DreamSettings.getKeyFromSetting(SETTINGS[i])).isEqualTo(KEYS.get(i));
        }
        // Default
        assertThat(DreamSettings.getKeyFromSetting(-1))
                .isEqualTo(DreamSettings.NEVER_DREAM);
    }

    @Test
    public void getDreamSettingDescriptionResId() {
        for (int i = 0; i < SETTINGS.length; i++) {
            assertThat(DreamSettings.getDreamSettingDescriptionResId(SETTINGS[i]))
                    .isEqualTo(RES_IDS[i]);
        }
        // Default
        assertThat(DreamSettings.getDreamSettingDescriptionResId(-1))
                .isEqualTo(R.string.screensaver_settings_summary_never);
    }

    @Test
    public void summaryText_whenDreamsAreOff() {
        DreamBackend mockBackend = mock(DreamBackend.class);
        Context mockContext = mock(Context.class);
        when(mockBackend.isEnabled()).thenReturn(false);

        assertThat(DreamSettings.getSummaryTextFromBackend(mockBackend, mockContext))
                .isEqualTo(mockContext.getString(R.string.screensaver_settings_summary_off));
    }

    @Test
    public void summaryTest_WhenDreamsAreOn() {
        final String fakeName = "test_name";
        DreamBackend mockBackend = mock(DreamBackend.class);
        Context mockContext = mock(Context.class);
        when(mockBackend.isEnabled()).thenReturn(true);
        when(mockBackend.getActiveDreamName()).thenReturn(fakeName);

        assertThat(DreamSettings.getSummaryTextFromBackend(mockBackend, mockContext))
                .isEqualTo(fakeName);
    }
}
