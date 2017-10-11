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

package com.android.settings.users;

import android.app.Activity;
import android.content.pm.UserInfo;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UserSettingsTest {

    @Mock private UserManager mUserManager;
    @Mock private SummaryLoader mSummaryLoader;
    private Activity mActivity;
    private SummaryLoader.SummaryProvider mSummaryProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(Robolectric.buildActivity(Activity.class).get());
        when((Object) mActivity.getSystemService(UserManager.class)).thenReturn(mUserManager);

        mSummaryProvider = UserSettings.SUMMARY_PROVIDER_FACTORY.createSummaryProvider(
            mActivity, mSummaryLoader);
    }

    @Test
    public void setListening_shouldSetSummaryWithUserName() {
        final String name = "John";
        final UserInfo userInfo = new UserInfo();
        userInfo.name = name;
        when(mUserManager.getUserInfo(anyInt())).thenReturn(userInfo);

        mSummaryProvider.setListening(true);

        verify(mSummaryLoader).setSummary(mSummaryProvider,
            mActivity.getString(R.string.users_summary, name));
    }

}
