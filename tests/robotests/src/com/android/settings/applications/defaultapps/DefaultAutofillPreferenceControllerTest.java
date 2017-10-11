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


import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.view.autofill.AutofillManager;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.AutofillManagerWrapper;
import com.android.settings.applications.PackageManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultAutofillPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PackageManagerWrapper mPackageManager;
    @Mock
    private AutofillManagerWrapper mAutofillManager;

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

        final DefaultAppInfo info = mController.getDefaultAppInfo();

        assertThat(info).isNotNull();

        mController.getSettingIntent(info);

        verify(mPackageManager.getPackageManager()).queryIntentServices(
                DefaultAutofillPicker.AUTOFILL_PROBE, PackageManager.GET_META_DATA);
    }
}
