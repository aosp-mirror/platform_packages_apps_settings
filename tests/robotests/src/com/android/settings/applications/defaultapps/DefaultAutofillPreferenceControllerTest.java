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

package com.android.settings.applications.defaultapps;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.provider.Settings;
import android.view.autofill.AutofillManager;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class DefaultAutofillPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PackageManager mPackageManager;
    @Mock
    private AutofillManager mAutofillManager;

    private DefaultAutofillPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mController = spy(new DefaultAutofillPreferenceController(mContext));
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);
        ReflectionHelpers.setField(mController, "mAutofillManager", mAutofillManager);
    }

    @Test
    public void isAvailableIfHasFeatureAndSupported() {
        when(mContext.getSystemService(AutofillManager.class)).thenReturn(null);
        assertThat(mController.isAvailable()).isFalse();

        when(mAutofillManager.hasAutofillFeature()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();

        when(mAutofillManager.hasAutofillFeature()).thenReturn(true);
        when(mAutofillManager.isAutofillSupported()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();

        when(mAutofillManager.hasAutofillFeature()).thenReturn(true);
        when(mAutofillManager.isAutofillSupported()).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_hasNoApp_shouldNotReturnLabel() {
        final Preference pref = mock(Preference.class);

        mController.updateState(pref);
        verify(pref).setSummary(R.string.app_list_preference_none);
    }

    @Test
    public void getDefaultAppInfo_shouldHaveSettingsProvider() {
        ReflectionHelpers.setField(mController, "mContext", RuntimeEnvironment.application);
        Settings.Secure.putString(RuntimeEnvironment.application.getContentResolver(),
                DefaultAutofillPicker.SETTING, "com.android.settings/SettingsActivity.class");

        assertThat(mController.getDefaultAppInfo()).isNotNull();
    }
}
