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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link CaptioningLocalePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningLocalePreferenceControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private PreferenceScreen mScreen;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CaptioningLocalePreferenceController mController;
    private LocalePreference mPreference;

    @Before
    public void setUp() {
        mController = new CaptioningLocalePreferenceController(mContext, "captioning_local_pref");
        mPreference = new LocalePreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void displayPreference_byDefault_shouldReturnDefault() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getEntry().toString()).isEqualTo(
                mContext.getResources().getString(R.string.locale_default));
    }

    @Test
    public void displayPreference_byArabicLocale_shouldReturnArabic() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE, "af_ZA");

        mController.displayPreference(mScreen);

        assertThat(mPreference.getEntry().toString()).isEqualTo("Afrikaans");
    }

    @Test
    public void onPreferenceChange_byArabicLocale_shouldReturnArabic() {
        mController.displayPreference(mScreen);

        mController.onPreferenceChange(mPreference, "af_ZA");

        assertThat(mPreference.getEntry().toString()).isEqualTo("Afrikaans");
    }
}
