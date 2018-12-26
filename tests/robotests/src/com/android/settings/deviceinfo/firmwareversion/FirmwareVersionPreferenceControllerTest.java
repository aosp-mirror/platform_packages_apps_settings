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

package com.android.settings.deviceinfo.firmwareversion;

import static android.content.Context.CLIPBOARD_SERVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@RunWith(RobolectricTestRunner.class)
public class FirmwareVersionPreferenceControllerTest {

    private static final String KEY = "firmware_version";

    @Mock
    private Fragment mFragment;

    private Preference mPreference;
    private PreferenceScreen mScreen;
    private FirmwareVersionPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        final PreferenceManager preferenceManager = new PreferenceManager(context);
        mController = new FirmwareVersionPreferenceController(context, KEY);
        mController.setHost(mFragment);
        mPreference = new Preference(context);
        mPreference.setKey(KEY);
        mScreen = preferenceManager.createPreferenceScreen(context);
        mScreen.addPreference(mPreference);
    }

    @After
    public void tearDown() {
        ShadowFirmwareVersionDialogFragment.reset();
    }

    @Test
    public void firmwareVersion_shouldAlwaysBeShown() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void updatePreference_shouldSetSummaryToBuildNumber() {
        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(Build.VERSION.RELEASE);
    }

    @Test
    @Config(shadows = ShadowFirmwareVersionDialogFragment.class)
    public void handlePreferenceTreeClick_samePreferenceKey_shouldStartDialogFragment() {
        final boolean result = mController.handlePreferenceTreeClick(mPreference);

        assertThat(ShadowFirmwareVersionDialogFragment.isShowing).isTrue();
        assertThat(result).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_unknownPreferenceKey_shouldDoNothingAndReturnFalse() {
        mPreference.setKey("foobar");

        final boolean result = mController.handlePreferenceTreeClick(mPreference);

        assertThat(ShadowFirmwareVersionDialogFragment.isShowing).isFalse();
        assertThat(result).isFalse();
    }

    @Test
    public void isSliceable_shouldBeTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void copy_shouldCopyVersionNumberToClipboard() {
        mController.copy();

        final Context context = RuntimeEnvironment.application;
        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(
                CLIPBOARD_SERVICE);
        final CharSequence data = clipboard.getPrimaryClip().getItemAt(0).getText();
        assertThat(data.toString()).isEqualTo(Build.VERSION.RELEASE);
    }

    @Implements(FirmwareVersionDialogFragment.class)
    public static class ShadowFirmwareVersionDialogFragment {

        private static boolean isShowing = false;

        @Implementation
        public static void show(Fragment fragemnt) {
            isShowing = true;
        }

        @Resetter
        public static void reset() {
            isShowing = false;
        }
    }
}
