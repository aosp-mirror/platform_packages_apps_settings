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

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.location.RecentLocationApps;
import com.android.settingslib.location.RecentLocationApps.Request;
import com.android.settingslib.widget.apppreference.AppPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class RecentLocationRequestPreferenceControllerTest {

    @Mock
    private LocationSettings mFragment;
    @Mock
    private PreferenceCategory mCategory;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private RecentLocationApps mRecentLocationApps;
    @Mock
    private Preference mSeeAllButton;

    private Context mContext;
    private RecentLocationRequestPreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(new RecentLocationRequestPreferenceController(
                mContext, mFragment, mLifecycle, mRecentLocationApps));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mCategory);
        when(mScreen.findPreference(mController.KEY_SEE_ALL_BUTTON)).thenReturn(mSeeAllButton);
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
        doReturn(new ArrayList<>()).when(mRecentLocationApps).getAppListSorted();
        mController.displayPreference(mScreen);

        mController.updateState(mCategory);

        verify(mCategory).removeAll();
        final String title = mContext.getString(R.string.location_no_recent_apps);
        verify(mCategory).addPreference(argThat(titleMatches(title)));
    }

    @Test
    public void updateState_hasRecentRequest_shouldRemoveAllAndAddInjectedSettings() {
        List<Request> requests = createMockRequests(2);
        doReturn(requests).when(mRecentLocationApps).getAppListSorted();

        mController.displayPreference(mScreen);
        mController.updateState(mCategory);

        verify(mCategory).removeAll();
        // Verifies two preferences are added in original order
        InOrder inOrder = Mockito.inOrder(mCategory);
        inOrder.verify(mCategory).addPreference(argThat(titleMatches("appTitle0")));
        inOrder.verify(mCategory).addPreference(argThat(titleMatches("appTitle1")));
    }

    @Test
    public void updateState_hasOverThreeRequests_shouldDisplaySeeAllButton() {
        List<Request> requests = createMockRequests(6);
        when(mRecentLocationApps.getAppListSorted()).thenReturn(requests);

        mController.displayPreference(mScreen);
        mController.updateState(mCategory);

        verify(mCategory).removeAll();
        // Verifies the first three preferences are added
        InOrder inOrder = Mockito.inOrder(mCategory);
        inOrder.verify(mCategory).addPreference(argThat(titleMatches("appTitle0")));
        inOrder.verify(mCategory).addPreference(argThat(titleMatches("appTitle1")));
        inOrder.verify(mCategory).addPreference(argThat(titleMatches("appTitle2")));
        verify(mCategory, never()).addPreference(argThat(titleMatches("appTitle3")));
        // Verifies the "See all" preference is visible
        verify(mSeeAllButton).setVisible(true);
    }

    @Test
    public void createAppPreference_shouldAddClickListener() {
        final Request request = mock(Request.class);
        final AppPreference preference = mock(AppPreference.class);
        doReturn(preference).when(mController).createAppPreference(any(Context.class));

        mController.createAppPreference(mContext, request);

        verify(preference).setOnPreferenceClickListener(
                any(RecentLocationRequestPreferenceController.PackageEntryClickedListener.class));
    }

    @Test
    public void onPreferenceClick_shouldLaunchAppDetails() {
        final Context context = mock(Context.class);
        when(mFragment.getContext()).thenReturn(context);

        final List<RecentLocationApps.Request> requests = new ArrayList<>();
        final Request request = mock(Request.class);
        requests.add(request);
        doReturn(requests).when(mRecentLocationApps).getAppListSorted();
        final AppPreference preference = new AppPreference(mContext);
        doReturn(preference).when(mController).createAppPreference(any(Context.class));
        mController.displayPreference(mScreen);
        mController.updateState(mCategory);

        final ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);

        preference.performClick();

        verify(context).startActivity(intent.capture());

        assertThat(intent.getValue().getStringExtra(EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AppInfoDashboardFragment.class.getName());
    }

    private static ArgumentMatcher<Preference> titleMatches(String expected) {
        return preference -> TextUtils.equals(expected, preference.getTitle());
    }

    private List<RecentLocationApps.Request> createMockRequests(int count) {
        List<RecentLocationApps.Request> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Add mock requests
            Request req = mock(Request.class, "request" + i);
            requests.add(req);
            // Map mock AppPreferences with mock requests
            String title = "appTitle" + i;
            AppPreference appPreference = mock(AppPreference.class, "AppPreference" + i);
            doReturn(title).when(appPreference).getTitle();
            doReturn(appPreference)
                    .when(mController).createAppPreference(any(Context.class), eq(req));
        }
        return requests;
    }
}