/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static android.view.flags.Flags.enableVectorCursorA11ySettings;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.SeekBar;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSystemSettings;
import com.android.settings.widget.LabeledSeekBarPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Tests for {@link PointerScaleSeekBarController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
})
public class PointerScaleSeekBarControllerTest {

    private static final String PREFERENCE_KEY = "pointer_scale";

    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private LifecycleOwner mLifecycleOwner;

    private Context mContext;
    private LabeledSeekBarPreference mPreference;
    private PointerScaleSeekBarController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPreference = new LabeledSeekBarPreference(mContext, null);
        mController = new PointerScaleSeekBarController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void getAvailabilityStatus_flagEnabled() {
        assumeTrue(enableVectorCursorA11ySettings());

        assertEquals(mController.getAvailabilityStatus(), AVAILABLE);
    }

    @Test
    public void onProgressChanged_changeListenerUpdatesSetting() {
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        SeekBar seekBar = mPreference.getSeekbar();
        int sliderValue = 1;

        mPreference.onProgressChanged(seekBar, sliderValue, false);

        float expectedScale = 1.5f;
        float currentScale = Settings.System.getFloatForUser(mContext.getContentResolver(),
                Settings.System.POINTER_SCALE, -1, UserHandle.USER_CURRENT);
        assertEquals(expectedScale, currentScale, /* delta= */ 0.001f);
    }

    @Test
    public void onPause_logCurrentScaleValue() {
        float scale = 1.5f;
        Settings.System.putFloatForUser(mContext.getContentResolver(),
                Settings.System.POINTER_SCALE, scale, UserHandle.USER_CURRENT);

        mController.onStateChanged(mLifecycleOwner, Lifecycle.Event.ON_PAUSE);

        verify(mFeatureFactory.metricsFeatureProvider).action(
                    any(), eq(SettingsEnums.ACTION_POINTER_ICON_SCALE_CHANGED),
                    eq(Float.toString(scale)));
    }
}
