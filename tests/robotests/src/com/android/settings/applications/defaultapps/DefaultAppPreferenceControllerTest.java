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

import static com.android.settingslib.TwoTargetPreference.ICON_SIZE_MEDIUM;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.TwoTargetPreference;
import com.android.settingslib.applications.DefaultAppInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
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

        when(mController.mAppInfo.loadLabel()).thenReturn(TEST_APP_NAME);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(TEST_APP_NAME);
    }

    @Test
    public void updateState_hasNoApp_shouldNotUpdateAppName() {
        mController = new TestPreferenceController(mContext);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.app_list_preference_none);
    }

    @Test
    public void updateState_twoTargetPref_shouldUseMediumIcon() {
        final TwoTargetPreference pref = mock(TwoTargetPreference.class);
        mController = new TestPreferenceController(mContext);

        mController.updateState(pref);

        verify(pref).setIconSize(ICON_SIZE_MEDIUM);
    }

    private static class TestPreferenceController extends DefaultAppPreferenceController {

        private DefaultAppInfo mAppInfo;

        private TestPreferenceController(Context context) {
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
