/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.webview;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WebViewAppPreferenceControllerTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private Preference mPreference;

    private static final String DEFAULT_PACKAGE_NAME = "DEFAULT_PACKAGE_NAME";

    @Before public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mPreferenceScreen.findPreference(any())).thenReturn(mPreference);
    }

    @Test public void testOnActivityResultUpdatesStateOnSuccess() {
        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
        WebViewAppPreferenceController controller =
                spy(new WebViewAppPreferenceController(mContext, wvusWrapper));

        controller.displayPreference(mPreferenceScreen); // Makes sure Preference is non-null
        controller.onActivityResult(Activity.RESULT_OK, new Intent(DEFAULT_PACKAGE_NAME));
        verify(controller, times(1)).updateState(any());
    }

    @Test public void testOnActivityResultWithFailure() {
        WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);

        WebViewAppPreferenceController controller =
                spy(new WebViewAppPreferenceController(mContext, wvusWrapper));

        controller.displayPreference(mPreferenceScreen); // Makes sure Preference is non-null
        controller.onActivityResult(Activity.RESULT_CANCELED, new Intent(DEFAULT_PACKAGE_NAME));
        verify(controller, times(1)).updateState(any());
    }
}
