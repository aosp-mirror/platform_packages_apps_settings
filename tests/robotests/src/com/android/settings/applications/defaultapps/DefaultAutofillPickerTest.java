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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultAutofillPickerTest {

    private static final String TEST_APP_KEY = new ComponentName("foo.bar",
            "foo.bar.Baz").flattenToString();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManagerWrapper mPackageManager;
    private DefaultAutofillPicker mPicker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mActivity);
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mPicker = spy(new DefaultAutofillPicker());
        mPicker.onAttach((Context) mActivity);

        ReflectionHelpers.setField(mPicker, "mPm", mPackageManager);

        doReturn(RuntimeEnvironment.application).when(mPicker).getContext();
    }

    @Test
    public void setAndGetDefaultAppKey_shouldUpdateDefaultAutoFill() {
        assertThat(mPicker.setDefaultKey(TEST_APP_KEY)).isTrue();
        assertThat(mPicker.getDefaultKey()).isEqualTo(TEST_APP_KEY);
    }

    @Test
    public void getConfirmationMessage_shouldNotBeNull() {
        final DefaultAppInfo info = mock(DefaultAppInfo.class);
        when(info.loadLabel()).thenReturn("test_app_name");
        assertThat(mPicker.getConfirmationMessage(info)).isNotNull();
    }
}
