/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settingslib.applications.RecentAppOpsAccess;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class RecentLocationAccessPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "test_preference_key";
    @Mock
    private PreferenceCategory mLayoutPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private RecentAppOpsAccess mRecentLocationApps;

    private Context mContext;
    private RecentLocationAccessPreferenceController mController;
    private View mAppEntitiesHeaderView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = spy(
                new RecentLocationAccessPreferenceController(mContext, PREFERENCE_KEY,
                        mRecentLocationApps));
        mController.init(mDashboardFragment);
        final String key = mController.getPreferenceKey();
        mAppEntitiesHeaderView = LayoutInflater.from(mContext).inflate(
                com.android.settingslib.widget.entityheader.R.layout.app_entities_header, null /* root */);
        when(mScreen.findPreference(key)).thenReturn(mLayoutPreference);
        when(mLayoutPreference.getKey()).thenReturn(key);
        when(mLayoutPreference.getContext()).thenReturn(mContext);
        when(mDashboardFragment.getContext()).thenReturn(mContext);
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    @Test
    public void isAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isEqualTo(true);
    }

    /** Verifies the title text, details text are correct, and the click listener is set. */
    @Test
    @Ignore
    public void updateState_whenAppListIsEmpty_shouldDisplayTitleTextAndDetailsText() {
        doReturn(new ArrayList<>()).when(mRecentLocationApps).getAppListSorted(false);
        mController.displayPreference(mScreen);
        mController.updateState(mLayoutPreference);

        final TextView title = mAppEntitiesHeaderView.findViewById(R.id.header_title);
        assertThat(title.getText()).isEqualTo(
                mContext.getText(R.string.location_category_recent_location_access));
        final TextView details = mAppEntitiesHeaderView
                .findViewById(com.android.settingslib.widget.entityheader.R.id.header_details);
        assertThat(details.getText()).isEqualTo(
                mContext.getText(R.string.location_recent_location_access_view_details));
        assertThat(details.hasOnClickListeners()).isTrue();
    }

    /** Verifies the title text, details text are correct, and the click listener is set. */
    @Test
    public void updateState_showSystemAccess() {
        doReturn(ImmutableList.of(
                new RecentAppOpsAccess.Access("app", UserHandle.CURRENT, null, "app", "", 0)))
                .when(mRecentLocationApps).getAppListSorted(false);
        doReturn(new ArrayList<>()).when(mRecentLocationApps).getAppListSorted(true);
        mController.displayPreference(mScreen);
        mController.updateState(mLayoutPreference);
        verify(mLayoutPreference).addPreference(Mockito.any());

        Settings.Secure.putInt(
                mContext.getContentResolver(), Settings.Secure.LOCATION_SHOW_SYSTEM_OPS, 1);
        verify(mLayoutPreference, Mockito.times(1)).addPreference(Mockito.any());
    }
}
