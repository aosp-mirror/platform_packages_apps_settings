/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.password;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.FragmentHostCallback;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings.Global;

import com.android.settings.TestConfig;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = {
        SettingsShadowResources.class,
        SettingsShadowResources.SettingsShadowTheme.class
    })
public class ChooseLockGenericTest {

    @Mock
    private ChooseLockGeneric mActivity;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mActivity.getContentResolver()).thenReturn(mContext.getContentResolver());
        when(mActivity.getTheme()).thenReturn(mContext.getTheme());
        when(mActivity.getResources()).thenReturn(mContext.getResources());
    }

    @After
    public void tearDown() {
        Global.putInt(mContext.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
    }

    @Test
    public void onCreate_deviceNotProvisioned_shouldFinishActivity() {
        Global.putInt(mContext.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        final ChooseLockGenericFragment fragment = spy(new ChooseLockGenericFragment());
        when(fragment.getActivity()).thenReturn(mActivity);
        ReflectionHelpers.setField(fragment, "mHost", new TestHostCallbacks());

        fragment.onCreate(Bundle.EMPTY);

        verify(mActivity).finish();
    }

    public class TestHostCallbacks extends FragmentHostCallback<Activity> {

        public TestHostCallbacks() {
            super(mActivity, null /* handler */, 0 /* windowAnimations */);
        }

        @Override
        public Activity onGetHost() {
          return mActivity;
        }
    }

}
