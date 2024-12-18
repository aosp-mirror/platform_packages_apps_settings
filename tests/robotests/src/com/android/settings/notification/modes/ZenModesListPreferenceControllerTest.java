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

import static com.android.settingslib.notification.modes.TestModeBuilder.MANUAL_DND_INACTIVE;

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

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.search.SearchIndexableRaw;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ZenModesListPreferenceControllerTest {
    private static final String TEST_MODE_ID = "test_mode";
    private static final String TEST_MODE_NAME = "Test Mode";

    private static final ZenMode TEST_MODE = new TestModeBuilder()
            .setId(TEST_MODE_ID)
            .setAzr(new AutomaticZenRule.Builder(TEST_MODE_NAME, Uri.parse("test_uri"))
                    .setType(AutomaticZenRule.TYPE_BEDTIME)
                    .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                    .setZenPolicy(new ZenPolicy.Builder().allowAllSounds().build())
                    .build())
            .build();

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(
            SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT);

    private Context mContext;

    @Mock
    private ZenModesBackend mBackend;

    private ZenModesListPreferenceController mPrefController;
    private PreferenceCategory mPreference;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mPreference = new PreferenceCategory(mContext);
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        preferenceScreen.addPreference(mPreference);

        mPrefController = new ZenModesListPreferenceController(mContext, mBackend,
                new ZenIconLoader(MoreExecutors.newDirectExecutorService()));
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void updateState_addsPreferences() {
        ImmutableList<ZenMode> modes = ImmutableList.of(newMode("One"), newMode("Two"),
                newMode("Three"), newMode("Four"), newMode("Five"));
        when(mBackend.getModes()).thenReturn(modes);

        mPrefController.updateState(mPreference);

        assertThat(mPreference.getPreferenceCount()).isEqualTo(5);
        List<ZenModesListItemPreference> itemPreferences = getModeListItems(mPreference);
        assertThat(itemPreferences.stream().map(ZenModesListItemPreference::getZenMode).toList())
                .containsExactlyElementsIn(modes)
                .inOrder();
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void updateState_secondTime_updatesPreferences() {
        ImmutableList<ZenMode> modes = ImmutableList.of(newMode("One"), newMode("Two"),
                newMode("Three"), newMode("Four"), newMode("Five"));
        when(mBackend.getModes()).thenReturn(modes);
        mPrefController.updateState(mPreference);

        assertThat(mPreference.getPreferenceCount()).isEqualTo(5);
        List<ZenModesListItemPreference> oldPreferences = getModeListItems(mPreference);

        ImmutableList<ZenMode> updatedModes = ImmutableList.of(modes.get(0), modes.get(1),
                newMode("Two.1"), newMode("Two.2"), modes.get(2), /* deleted "Four" */
                modes.get(4));
        when(mBackend.getModes()).thenReturn(updatedModes);
        mPrefController.updateState(mPreference);

        List<ZenModesListItemPreference> newPreferences = getModeListItems(mPreference);
        assertThat(newPreferences.stream().map(ZenModesListItemPreference::getZenMode).toList())
                .containsExactlyElementsIn(updatedModes)
                .inOrder();

        // Verify that the old preference controllers were reused instead of creating new ones.
        assertThat(newPreferences.get(0)).isSameInstanceAs(oldPreferences.get(0));
        assertThat(newPreferences.get(1)).isSameInstanceAs(oldPreferences.get(1));
        assertThat(newPreferences.get(4)).isSameInstanceAs(oldPreferences.get(2));
        assertThat(newPreferences.get(5)).isSameInstanceAs(oldPreferences.get(4));
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void testModesUiOff_notAvailableAndNoSearchData() {
        // There exist modes
        when(mBackend.getModes()).thenReturn(List.of(MANUAL_DND_INACTIVE, TEST_MODE));

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
        when(mBackend.getModes()).thenReturn(List.of(MANUAL_DND_INACTIVE));
        List<SearchIndexableRaw> newData = new ArrayList<>();
        mPrefController.updateDynamicRawDataToIndex(newData);
        assertThat(newData).hasSize(1);

        SearchIndexableRaw newItem = newData.get(0);
        assertThat(newItem.key).isEqualTo(MANUAL_DND_INACTIVE.getId());
        assertThat(newItem.title).isEqualTo("Do Not Disturb");  // set above
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void testUpdateDynamicRawDataToIndex_multipleModes() {
        when(mBackend.getModes()).thenReturn(List.of(MANUAL_DND_INACTIVE, TEST_MODE));

        List<SearchIndexableRaw> data = new ArrayList<>();
        mPrefController.updateDynamicRawDataToIndex(data);
        assertThat(data).hasSize(2);

        // Should keep the order presented by getModes()
        SearchIndexableRaw item0 = data.get(0);
        assertThat(item0.key).isEqualTo(MANUAL_DND_INACTIVE.getId());
        assertThat(item0.title).isEqualTo("Do Not Disturb");  // set above

        SearchIndexableRaw item1 = data.get(1);
        assertThat(item1.key).isEqualTo(TEST_MODE_ID);
        assertThat(item1.title).isEqualTo(TEST_MODE_NAME);
    }

    private static ZenMode newMode(String id) {
        return new TestModeBuilder().setId(id).setName("Mode " + id).build();
    }

    /**
     * Returns the child preferences of the {@code group}, sorted by their
     * {@link Preference#getOrder} value (which is the order they will be sorted by and displayed
     * in the UI).
     */
    private List<ZenModesListItemPreference> getModeListItems(PreferenceGroup group) {
        ArrayList<ZenModesListItemPreference> items = new ArrayList<>();
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            items.add((ZenModesListItemPreference) group.getPreference(i));
        }
        items.sort(Comparator.comparing(Preference::getOrder));
        return items;
    }
}
