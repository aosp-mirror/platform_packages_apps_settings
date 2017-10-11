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

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.testutils.shadow.ShadowDynamicIndexableContentMonitor;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.SettingsShadowResources.SettingsShadowTheme;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;
import com.android.settingslib.applications.ApplicationsState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
                ShadowDynamicIndexableContentMonitor.class,
                ShadowEventLogWriter.class
        })
public class ManageApplicationsTest {

    @Mock private ApplicationsState mState;
    @Mock private ApplicationsState.Session mSession;

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
        ReflectionHelpers.setField(mFragment, "mLifecycle", new Lifecycle());
    }

    @Test
    public void launchFragment() {
        SettingsRobolectricTestRunner.startSettingsFragment(
                mFragment, Settings.ManageApplicationsActivity.class);
    }

    @Test
    public void updateDisableView_appDisabledUntilUsed_shouldSetDisabled() {
        final TextView view = mock(TextView.class);
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;
        info.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
        ManageApplications fragment = mock(ManageApplications.class);
        when(fragment.getActivity()).thenReturn(mock(Activity.class));
        final ManageApplications.ApplicationsAdapter adapter =
            new ManageApplications.ApplicationsAdapter(mState, fragment, 0);

        adapter.updateDisableView(view, info);

        verify(view).setText(R.string.disabled);
    }
}
