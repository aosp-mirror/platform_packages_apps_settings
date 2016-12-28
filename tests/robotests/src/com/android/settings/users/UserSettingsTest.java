/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.users;

import android.content.Context;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UserSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private FakeFeatureFactory mFeatureFactory;
    private UserSettings mSetting;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mSetting = new UserSettings();
    }

    @Test
    public void testShowEmergencyInfoAndAddUsers_IAEnabled_shouldReturnFalse() {
        when(mFeatureFactory.dashboardFeatureProvider.isEnabled()).thenReturn(true);
        assertThat(mSetting.showEmergencyInfoAndAddUsersWhenLock(mContext)).isFalse();
    }

    @Test
    public void testShowEmergencyInfoAndAddUsers_IADisabled_shouldReturnTrue() {
        when(mFeatureFactory.dashboardFeatureProvider.isEnabled()).thenReturn(false);
        assertThat(mSetting.showEmergencyInfoAndAddUsersWhenLock(mContext)).isTrue();
    }

}
