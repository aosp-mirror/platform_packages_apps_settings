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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.DeviceConfig;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settingslib.location.RecentLocationAccesses;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class RecentLocationAccessPreferenceControllerTest {
    @Mock
    private LayoutPreference mLayoutPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private RecentLocationAccesses mRecentLocationApps;

    private Context mContext;
    private RecentLocationAccessPreferenceController mController;
    private View mAppEntitiesHeaderView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = spy(
                new RecentLocationAccessPreferenceController(mContext, mRecentLocationApps));
        final String key = mController.getPreferenceKey();
        mAppEntitiesHeaderView = LayoutInflater.from(mContext).inflate(
                R.layout.app_entities_header, null /* root */);
        when(mScreen.findPreference(key)).thenReturn(mLayoutPreference);
        when(mLayoutPreference.getKey()).thenReturn(key);
        when(mLayoutPreference.getContext()).thenReturn(mContext);
        when(mLayoutPreference.findViewById(R.id.app_entities_header)).thenReturn(
                mAppEntitiesHeaderView);
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    @Test
    public void isAvailable_permissionHubNotSet_shouldReturnFalse() {
        // We have not yet set the property to show the Permissions Hub.
        assertThat(mController.isAvailable()).isEqualTo(false);
    }

    @Test
    public void isAvailable_permissionHubEnabled_shouldReturnTrue() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_PERMISSIONS_HUB_ENABLED, "true", true);

        assertThat(mController.isAvailable()).isEqualTo(true);
    }

    /** Verifies the title text, details text are correct, and the click listener is set. */
    @Test
    @Ignore
    public void updateState_whenAppListIsEmpty_shouldDisplayTitleTextAndDetailsText() {
        doReturn(new ArrayList<>()).when(mRecentLocationApps).getAppListSorted();
        mController.displayPreference(mScreen);
        mController.updateState(mLayoutPreference);

        final TextView title = mAppEntitiesHeaderView.findViewById(R.id.header_title);
        assertThat(title.getText()).isEqualTo(
                mContext.getText(R.string.location_category_recent_location_access));
        final TextView details = mAppEntitiesHeaderView.findViewById(R.id.header_details);
        assertThat(details.getText()).isEqualTo(
                mContext.getText(R.string.location_recent_location_access_view_details));
        assertThat(details.hasOnClickListeners()).isTrue();
    }

    @Test
    public void updateState_whenAppListMoreThanThree_shouldDisplayTopThreeApps() {
        final List<RecentLocationAccesses.Access> accesses = createMockAccesses(6);
        doReturn(accesses).when(mRecentLocationApps).getAppListSorted();
        mController.displayPreference(mScreen);
        mController.updateState(mLayoutPreference);

        // The widget can display the top 3 apps from the list when there're more than 3.
        final View app1View = mAppEntitiesHeaderView.findViewById(R.id.app1_view);
        final ImageView appIconView1 = app1View.findViewById(R.id.app_icon);
        final TextView appTitle1 = app1View.findViewById(R.id.app_title);

        assertThat(app1View.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(appIconView1.getDrawable()).isNotNull();
        assertThat(appTitle1.getText()).isEqualTo("appTitle0");

        final View app2View = mAppEntitiesHeaderView.findViewById(R.id.app2_view);
        final ImageView appIconView2 = app2View.findViewById(R.id.app_icon);
        final TextView appTitle2 = app2View.findViewById(R.id.app_title);

        assertThat(app2View.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(appIconView2.getDrawable()).isNotNull();
        assertThat(appTitle2.getText()).isEqualTo("appTitle1");

        final View app3View = mAppEntitiesHeaderView.findViewById(R.id.app3_view);
        final ImageView appIconView3 = app3View.findViewById(R.id.app_icon);
        final TextView appTitle3 = app3View.findViewById(R.id.app_title);

        assertThat(app3View.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(appIconView3.getDrawable()).isNotNull();
        assertThat(appTitle3.getText()).isEqualTo("appTitle2");
    }

    private List<RecentLocationAccesses.Access> createMockAccesses(int count) {
        final List<RecentLocationAccesses.Access> accesses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final Drawable icon = mock(Drawable.class);
            // Add mock accesses
            final RecentLocationAccesses.Access access = new RecentLocationAccesses.Access(
                    "packageName", android.os.Process.myUserHandle(), icon,
                    "appTitle" + i, "appSummary" + i, 1000 - i);
            accesses.add(access);
        }
        return accesses;
    }
}
