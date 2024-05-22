/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.net.Uri;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;

import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ZenModesListPreferenceControllerTest {
    private static final String TEST_MODE_ID = "test_mode";
    private static final String TEST_MODE_NAME = "Test Mode";
    private static final ZenMode TEST_MODE = new ZenMode(
            TEST_MODE_ID,
            new AutomaticZenRule.Builder(TEST_MODE_NAME, Uri.parse("test_uri"))
                    .setType(AutomaticZenRule.TYPE_BEDTIME)
                    .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                    .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                    .build(),
            false);

    private static final ZenMode TEST_MANUAL_MODE = ZenMode.manualDndMode(
            new AutomaticZenRule.Builder("Do Not Disturb", Uri.EMPTY)
                    .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                    .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                    .build(),
            false);

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(
            SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT);

    private Context mContext;

    @Mock
    private ZenModesBackend mBackend;

    private ZenModesListPreferenceController mPrefController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mPrefController = new ZenModesListPreferenceController(mContext, null, mBackend);
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void testModesUiOff_notAvailableAndNoSearchData() {
        // There exist modes
        when(mBackend.getModes()).thenReturn(List.of(TEST_MANUAL_MODE, TEST_MODE));

        assertThat(mPrefController.isAvailable()).isFalse();
        List<SearchIndexableRaw> data = new ArrayList<>();
        mPrefController.updateDynamicRawDataToIndex(data);
        assertThat(data).isEmpty();  // despite existence of modes
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void testUpdateDynamicRawDataToIndex_empty() {
        // Case of no modes.
        when(mBackend.getModes()).thenReturn(new ArrayList<>());

        List<SearchIndexableRaw> data = new ArrayList<>();
        mPrefController.updateDynamicRawDataToIndex(data);
        assertThat(data).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void testUpdateDynamicRawDataToIndex_oneMode() {
        // One mode present, confirm it's the correct one
        when(mBackend.getModes()).thenReturn(List.of(TEST_MODE));

        List<SearchIndexableRaw> data = new ArrayList<>();
        mPrefController.updateDynamicRawDataToIndex(data);
        assertThat(data).hasSize(1);

        SearchIndexableRaw item = data.get(0);
        assertThat(item.key).isEqualTo(TEST_MODE_ID);
        assertThat(item.title).isEqualTo(TEST_MODE_NAME);

        // Changing mode data so there's a different one mode doesn't keep any previous data
        // (and setting that state up in the caller)
        when(mBackend.getModes()).thenReturn(List.of(TEST_MANUAL_MODE));
        List<SearchIndexableRaw> newData = new ArrayList<>();
        mPrefController.updateDynamicRawDataToIndex(newData);
        assertThat(newData).hasSize(1);

        SearchIndexableRaw newItem = newData.get(0);
        assertThat(newItem.key).isEqualTo(ZenMode.MANUAL_DND_MODE_ID);
        assertThat(newItem.title).isEqualTo("Do Not Disturb");  // set above
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void testUpdateDynamicRawDataToIndex_multipleModes() {
        when(mBackend.getModes()).thenReturn(List.of(TEST_MANUAL_MODE, TEST_MODE));

        List<SearchIndexableRaw> data = new ArrayList<>();
        mPrefController.updateDynamicRawDataToIndex(data);
        assertThat(data).hasSize(2);

        // Should keep the order presented by getModes()
        SearchIndexableRaw item0 = data.get(0);
        assertThat(item0.key).isEqualTo(ZenMode.MANUAL_DND_MODE_ID);
        assertThat(item0.title).isEqualTo("Do Not Disturb");  // set above

        SearchIndexableRaw item1 = data.get(1);
        assertThat(item1.key).isEqualTo(TEST_MODE_ID);
        assertThat(item1.title).isEqualTo(TEST_MODE_NAME);
    }
}
