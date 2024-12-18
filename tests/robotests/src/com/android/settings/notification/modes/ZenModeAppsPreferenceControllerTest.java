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

import static com.android.settings.notification.modes.ZenModeAppsPreferenceController.KEY_NONE;
import static com.android.settings.notification.modes.ZenModeAppsPreferenceController.KEY_PRIORITY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public final class ZenModeAppsPreferenceControllerTest {

    private Context mContext;
    @Mock
    private ZenModesBackend mBackend;
    private ZenModeAppsPreferenceController mPriorityController;
    private ZenModeAppsPreferenceController mNoneController;

    private SelectorWithWidgetPreference mPriorityPref;
    private SelectorWithWidgetPreference mNonePref;
    private PreferenceCategory mPrefCategory;
    private PreferenceScreen mPreferenceScreen;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mPriorityController = new ZenModeAppsPreferenceController(mContext, KEY_PRIORITY, mBackend);
        mNoneController = new ZenModeAppsPreferenceController(mContext, KEY_NONE, mBackend);

        mPriorityPref = makePreference(KEY_PRIORITY, mPriorityController);
        mNonePref = makePreference(KEY_NONE, mNoneController);

        mPrefCategory = new PreferenceCategory(mContext);
        mPrefCategory.setKey("zen_mode_apps_category");

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mContext);

        mPreferenceScreen.addPreference(mPrefCategory);
        mPrefCategory.addPreference(mPriorityPref);
        mPrefCategory.addPreference(mNonePref);

        mPriorityController.displayPreference(mPreferenceScreen);
        mNoneController.displayPreference(mPreferenceScreen);
    }

    private SelectorWithWidgetPreference makePreference(String key,
            ZenModeAppsPreferenceController controller) {
        final SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(mContext, false);
        pref.setKey(key);
        pref.setOnClickListener(controller.mSelectorClickListener);
        return pref;
    }

    @Test
    public void testUpdateState_None() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_NONE)
                        .build())
                .build();

        mNoneController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testUpdateState_None_Unchecked() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build();

        mNoneController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(false);
    }

    @Test
    public void testUpdateState_Priority() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build();

        mPriorityController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testUpdateState_Priority_Unchecked() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_NONE)
                        .build())
                .build();

        mPriorityController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(false);
    }

    @Test
    public void testPreferenceClick_passesCorrectCheckedState_None() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build();

        mNoneController.updateZenMode(mNonePref, zenMode);
        mPriorityController.updateZenMode(mPriorityPref, zenMode);

        assertThat(((SelectorWithWidgetPreference) mPrefCategory.findPreference(KEY_NONE))
                .isChecked()).isFalse();
        assertThat(((SelectorWithWidgetPreference) mPrefCategory.findPreference(KEY_PRIORITY))
                .isChecked()).isTrue();

        // Click on NONE
        mPrefCategory.findPreference(KEY_NONE).performClick();

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        // NONE is not actually propagated to the backend as an interruption filter;
        // the filter is set to priority, and sounds and visual effects are disallowed.
        // See AbstractZenModePreferenceController.
        assertThat(captor.getValue().getRule().getInterruptionFilter())
                .isEqualTo(INTERRUPTION_FILTER_PRIORITY);

        // After screen is refreshed, NONE is now checked; others are unchecked.
        mNoneController.updateZenMode(mNonePref, captor.getValue());
        mPriorityController.updateZenMode(mPriorityPref, captor.getValue());
        assertThat(((SelectorWithWidgetPreference) mPrefCategory.findPreference(KEY_NONE))
                .isChecked()).isTrue();
        assertThat(((SelectorWithWidgetPreference) mPrefCategory.findPreference(KEY_PRIORITY))
                .isChecked()).isFalse();
    }

    @Test
    public void testPreferenceClick_passesCorrectCheckedState_Priority() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_NONE)
                        .build())
                .build();

        mNoneController.updateZenMode(mNonePref, zenMode);
        mPriorityController.updateZenMode(mPriorityPref, zenMode);

        assertThat(((SelectorWithWidgetPreference) mPrefCategory.findPreference(KEY_NONE))
                .isChecked()).isTrue();
        assertThat(((SelectorWithWidgetPreference) mPrefCategory.findPreference(KEY_PRIORITY))
                .isChecked()).isFalse();

        // Click on PRIORITY
        mPrefCategory.findPreference(KEY_PRIORITY).performClick();

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        // Checks the policy value for PRIORITY is propagated to the backend.
        assertThat(captor.getValue().getRule().getInterruptionFilter())
                .isEqualTo(INTERRUPTION_FILTER_PRIORITY);

        // After screen is refreshed, PRIORITY is now checked; others are unchecked.
        mNoneController.updateZenMode(mNonePref, captor.getValue());
        mPriorityController.updateZenMode(mPriorityPref, captor.getValue());
        assertThat(((SelectorWithWidgetPreference) mPrefCategory.findPreference(KEY_PRIORITY))
                .isChecked()).isTrue();
        assertThat(((SelectorWithWidgetPreference) mPrefCategory.findPreference(KEY_NONE))
                .isChecked()).isFalse();
    }
}
