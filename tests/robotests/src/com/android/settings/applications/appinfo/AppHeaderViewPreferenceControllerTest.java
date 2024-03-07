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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowSettingsLibUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSettingsLibUtils.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class AppHeaderViewPreferenceControllerTest {

    @Mock
    private AppInfoDashboardFragment mFragment;

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private LayoutPreference mPreference;

    private Context mContext;
    private FragmentActivity mActivity;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private View mHeader;
    private AppHeaderViewPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class).get());
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mHeader = LayoutInflater.from(mContext).inflate(
                com.android.settingslib.widget.preference.layout.R.layout.settings_entity_header, null);

        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        when(mPreference.findViewById(R.id.entity_header)).thenReturn(mHeader);

        mController =
            new AppHeaderViewPreferenceController(mContext, mFragment, "Package1", mLifecycle);
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


        final TextView title = mHeader.findViewById(R.id.entity_header_title);

        mController.displayPreference(mScreen);
        mController.refreshUi();

        assertThat(title).isNotNull();
        assertThat(title.getText()).isEqualTo(appLabel);
    }
}
