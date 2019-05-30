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
}
