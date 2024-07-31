/*
 * Copyright 2024 The Android Open Source Project
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
import static com.android.settings.inputmethod.PointerStrokeStylePreferenceController.KEY_POINTER_STROKE_STYLE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSystemSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link PointerStrokeStylePreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
})
public class PointerStrokeStylePreferenceControllerTest {
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    PreferenceScreen mPreferenceScreen;
    @Mock
    LifecycleOwner mLifecycleOwner;

    private Context mContext;
    private PointerStrokeStylePreferenceController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new PointerStrokeStylePreferenceController(mContext);
    }

    @Test
    public void displayPreference_initializeDataStore() {
        Preference strokePreference = new Preference(mContext);
        strokePreference.setKey(KEY_POINTER_STROKE_STYLE);
        when(mPreferenceScreen.findPreference(eq(KEY_POINTER_STROKE_STYLE))).thenReturn(
                strokePreference);

        mController.displayPreference(mPreferenceScreen);

        assertNotNull(strokePreference.getPreferenceDataStore());
    }

    @Test
    public void getAvailabilityStatus_flagEnabled() {
        assumeTrue(enableVectorCursorA11ySettings());

        assertEquals(mController.getAvailabilityStatus(), AVAILABLE);
    }

    @Test
    public void onPause_logCurrentStrokeValue() {
        int strokeStyle = 1;
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.POINTER_STROKE_STYLE, strokeStyle, UserHandle.USER_CURRENT);

        mController.onStateChanged(mLifecycleOwner, Lifecycle.Event.ON_PAUSE);

        verify(mFeatureFactory.metricsFeatureProvider).action(
                    any(), eq(SettingsEnums.ACTION_POINTER_ICON_STROKE_STYLE_CHANGED),
                    eq(strokeStyle));
    }
}
