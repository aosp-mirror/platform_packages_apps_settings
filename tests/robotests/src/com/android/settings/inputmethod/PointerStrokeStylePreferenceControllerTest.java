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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link PointerStrokeStylePreferenceController} */
@RunWith(RobolectricTestRunner.class)
public class PointerStrokeStylePreferenceControllerTest {
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private PointerStrokeStylePreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
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
}
