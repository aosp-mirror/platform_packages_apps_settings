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

package com.android.settings.applications.appinfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppHeaderViewPreferenceControllerTest {

    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private Activity mActivity;

    private Context mContext;
    private AppHeaderViewPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getApplicationContext()).thenReturn(mContext);
        mController = new AppHeaderViewPreferenceController(mContext, mFragment, "Package1", null);
    }

    @Test
    public void refreshUi_shouldRefreshButton() {
        final PackageInfo packageInfo = mock(PackageInfo.class);
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        final String appLabel = "App1";
        appEntry.label = appLabel;
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;
        packageInfo.applicationInfo = info;
        appEntry.info = info;
        when(mFragment.getAppEntry()).thenReturn(appEntry);
        when(mFragment.getPackageInfo()).thenReturn(packageInfo);

        final PreferenceScreen screen = mock(PreferenceScreen.class);
        final LayoutPreference preference = mock(LayoutPreference.class);
        when(screen.findPreference(mController.getPreferenceKey())).thenReturn(preference);
        final View header = mock(View.class);
        when(preference.findViewById(R.id.entity_header)).thenReturn(header);
        final TextView title = mock(TextView.class);
        when(header.findViewById(R.id.entity_header_title)).thenReturn(title);
        final TextView summary = mock(TextView.class);
        when(header.findViewById(R.id.entity_header_summary)).thenReturn(summary);
        mController.displayPreference(screen);

        mController.refreshUi();

        verify(title).setText(appLabel);
        verify(summary).setText(mContext.getString(R.string.installed));
    }

}
