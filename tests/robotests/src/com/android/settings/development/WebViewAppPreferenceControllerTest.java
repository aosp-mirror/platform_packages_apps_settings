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

package com.android.settings.development;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.pm.PackageManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.webview.WebViewUpdateServiceWrapper;
import com.android.settingslib.applications.DefaultAppInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class WebViewAppPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;
    @Mock
    private Preference mPreference;
    @Mock
    private DefaultAppInfo mAppInfo;

    private WebViewAppPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mController = spy(new WebViewAppPreferenceController(RuntimeEnvironment.application));
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);
        ReflectionHelpers
            .setField(mController, "mWebViewUpdateServiceWrapper", mWebViewUpdateServiceWrapper);
        doReturn(mAppInfo).when(mController).getDefaultAppInfo();
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void updateState_hasAppLabel_shouldSetAppLabelAndIcon() {
        when(mAppInfo.loadLabel()).thenReturn("SomeRandomAppLabel!!!");

        mController.updateState(mPreference);

        verify(mPreference).setSummary("SomeRandomAppLabel!!!");
    }

    @Test
    public void updateState_noAppLabel_shouldSetAppDefaultLabelAndNullIcon() {
        when(mAppInfo.loadLabel()).thenReturn(null);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.app_list_preference_none);
    }
}
