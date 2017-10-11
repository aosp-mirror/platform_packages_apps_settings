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


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultAppPreferenceControllerTest {

    private static final String TEST_APP_NAME = "test";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock
    private Preference mPreference;

    private TestPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
    }

    @Test
    public void updateState_hasDefaultApp_shouldUpdateAppName() {
        mController = new TestPreferenceController(mContext);

        when(mController.mAppInfo.loadLabel())
                .thenReturn(TEST_APP_NAME);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(TEST_APP_NAME);
    }

    @Test
    public void updateState_hasNoApp_shouldNotUpdateAppName() {
        mController = new TestPreferenceController(mContext);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.app_list_preference_none);
    }

    private static class TestPreferenceController extends DefaultAppPreferenceController {

        private DefaultAppInfo mAppInfo;

        public TestPreferenceController(Context context) {
            super(context);
            mAppInfo = mock(DefaultAppInfo.class);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getPreferenceKey() {
            return "test";
        }

        @Override
        protected DefaultAppInfo getDefaultAppInfo() {
            return mAppInfo;
        }
    }
}
