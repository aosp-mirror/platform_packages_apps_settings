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
 * limitations under the License.
 */

package com.android.settings.applications;

import android.os.Looper;
import android.os.UserManager;
import com.android.settings.Settings;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowDynamicIndexableContentMonitor;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.SettingsShadowResources.SettingsShadowTheme;
import com.android.settingslib.applications.ApplicationsState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ManageApplications}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
// TODO: Consider making the shadow class set global using a robolectric.properties file.
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowTheme.class,
                ShadowDynamicIndexableContentMonitor.class
        })
public class ManageApplicationsTest {

    @Mock private ApplicationsState mState;
    @Mock private ApplicationsState.Session mSession;
    @Mock private UserManager mUserManager;

    private Looper mBgLooper;

    private ManageApplications mFragment;
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ReflectionHelpers.setStaticField(ApplicationsState.class, "sInstance", mState);
        when(mState.newSession(any())).thenReturn(mSession);
        mBgLooper = Looper.myLooper();
        when(mState.getBackgroundLooper()).thenReturn(mBgLooper);

        mFragment = new ManageApplications();
    }

    @Test
    public void launchFragment() {
        SettingsRobolectricTestRunner.startSettingsFragment(
                mFragment, Settings.ManageApplicationsActivity.class);
    }
}
