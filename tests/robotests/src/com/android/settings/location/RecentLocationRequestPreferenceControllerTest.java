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
package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.applications.AppInfoDashboardFragment;
import com.android.settings.core.FeatureFlags;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.AppPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.location.RecentLocationApps;
import com.android.settingslib.location.RecentLocationApps.Request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class RecentLocationRequestPreferenceControllerTest {

    @Mock
    private LocationSettings mFragment;
    @Mock
    private PreferenceCategory mCategory;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private RecentLocationApps mRecentLocationApps;

    private Context mContext;
    private RecentLocationRequestPreferenceController mController;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mController = spy(new RecentLocationRequestPreferenceController(
                mContext, mFragment, mLifecycle, mRecentLocationApps));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mCategory);
        final String key = mController.getPreferenceKey();
        when(mCategory.getKey()).thenReturn(key);
        when(mCategory.getContext()).thenReturn(mContext);
    }

    @Test
    public void onLocationModeChanged_LocationOn_shouldEnablePreference() {
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mCategory).setEnabled(true);
    }

    @Test
    public void onLocationModeChanged_LocationOff_shouldDisablePreference() {
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_OFF, false);

        verify(mCategory).setEnabled(false);
    }

    @Test
    public void updateState_noRecentRequest_shouldRemoveAllAndAddBanner() {
        doReturn(new ArrayList<>()).when(mRecentLocationApps).getAppList();
        mController.displayPreference(mScreen);

        mController.updateState(mCategory);

        verify(mCategory).removeAll();
        verify(mCategory).addPreference(
                argThat(titleMatches(mContext.getString(R.string.location_no_recent_apps))));
    }

    @Test
    public void updateState_hasRecentRequest_shouldRemoveAllAndAddInjectedSettings() {
        final List<RecentLocationApps.Request> requests = new ArrayList<>();
        final Request req1 = mock(Request.class);
        final Request req2 = mock(Request.class);
        requests.add(req1);
        requests.add(req2);
        doReturn(requests).when(mRecentLocationApps).getAppList();
        final String title = "testTitle";
        final AppPreference preference = mock(AppPreference.class);
        when(preference.getTitle()).thenReturn(title);
        doReturn(preference).when(mController)
                .createAppPreference(any(Context.class), any(Request.class));
        mController.displayPreference(mScreen);

        mController.updateState(mCategory);

        verify(mCategory).removeAll();
        verify(mCategory, times(2)).addPreference(argThat(titleMatches(title)));
    }

    @Test
    public void createAppPreference_shouldAddClickListener() {
        final Request request = mock(Request.class);
        final AppPreference preference = mock(AppPreference.class);
        doReturn(preference).when(mController)
                .createAppPreference(any(Context.class));

        mController.createAppPreference(mContext, request);

        verify(preference).setOnPreferenceClickListener(
                any(RecentLocationRequestPreferenceController.PackageEntryClickedListener.class));
    }

    @Test
    public void onPreferenceClick_shouldLaunchAppDetails() {
        final SettingsActivity activity = mock(SettingsActivity.class);
        when(mFragment.getActivity()).thenReturn(activity);
        final List<RecentLocationApps.Request> requests = new ArrayList<>();
        final Request request = mock(Request.class);
        requests.add(request);
        doReturn(requests).when(mRecentLocationApps).getAppList();
        final AppPreference preference = new AppPreference(mContext);
        doReturn(preference).when(mController).createAppPreference(any(Context.class));
        mController.displayPreference(mScreen);
        mController.updateState(mCategory);

        preference.performClick();

        if (FeatureFlagUtils.isEnabled(mContext, FeatureFlags.APP_INFO_V2)) {
            verify(activity).startPreferencePanelAsUser(any(),
                    eq(AppInfoDashboardFragment.class.getName()),
                    any(Bundle.class), anyInt(), any(), any());
        } else {
            verify(activity).startPreferencePanelAsUser(any(),
                    eq(InstalledAppDetails.class.getName()),
                    any(Bundle.class), anyInt(), any(), any());
        }
    }

    private static ArgumentMatcher<Preference> titleMatches(String expected) {
        return preference -> TextUtils.equals(expected, preference.getTitle());
    }

}
