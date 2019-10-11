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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ConnectedAccessPointPreferenceTest {

    @Mock
    private AccessPoint mAccessPoint;
    @Mock
    private View mView;
    @Mock
    private ConnectedAccessPointPreference.OnGearClickListener mOnGearClickListener;
    private Context mContext;
    private ConnectedAccessPointPreference mConnectedAccessPointPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mConnectedAccessPointPreference = new ConnectedAccessPointPreference(mAccessPoint, mContext,
                null, 0 /* iconResId */, false /* forSavedNetworks */, null /* fragment */);
        mConnectedAccessPointPreference.setOnGearClickListener(mOnGearClickListener);
    }

    @Test
    public void testOnClick_gearClicked_listenerInvoked() {
        when(mView.getId()).thenReturn(R.id.settings_button);

        mConnectedAccessPointPreference.onClick(mView);

        verify(mOnGearClickListener).onGearClick(mConnectedAccessPointPreference);
    }

    @Test
    public void testOnClick_gearNotClicked_listenerNotInvoked() {
        mConnectedAccessPointPreference.onClick(mView);

        verify(mOnGearClickListener, never()).onGearClick(mConnectedAccessPointPreference);
    }

    @Test
    public void testCaptivePortalStatus_isCaptivePortal_dividerDrawn() {
        mConnectedAccessPointPreference.setCaptivePortal(true);
        assertThat(mConnectedAccessPointPreference.shouldShowDivider()).isTrue();
    }

    @Test
    public void testCaptivePortalStatus_isNotCaptivePortal_dividerNotDrawn() {
        mConnectedAccessPointPreference.setCaptivePortal(false);
        assertThat(mConnectedAccessPointPreference.shouldShowDivider()).isFalse();
    }

    @Test
    public void testWidgetLayoutPreference() {
        assertThat(mConnectedAccessPointPreference.getWidgetLayoutResource())
            .isEqualTo(R.layout.preference_widget_gear_optional_background);
    }
}
