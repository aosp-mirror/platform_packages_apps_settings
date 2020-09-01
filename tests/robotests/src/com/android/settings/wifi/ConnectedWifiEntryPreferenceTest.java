/*
 * Copyright (C) 2020 The Android Open Source Project
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
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ConnectedWifiEntryPreferenceTest {

    @Mock
    private WifiEntry mWifiEntry;
    @Mock
    private View mView;
    @Mock
    private ConnectedWifiEntryPreference.OnGearClickListener mOnGearClickListener;
    private Context mContext;
    private ConnectedWifiEntryPreference mConnectedWifiEntryPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mConnectedWifiEntryPreference = new ConnectedWifiEntryPreference(mContext, mWifiEntry,
                null /* fragment */);
        mConnectedWifiEntryPreference.setOnGearClickListener(mOnGearClickListener);
    }

    @Test
    public void testOnClick_gearClicked_listenerInvoked() {
        when(mView.getId()).thenReturn(R.id.settings_button);

        mConnectedWifiEntryPreference.onClick(mView);

        verify(mOnGearClickListener).onGearClick(mConnectedWifiEntryPreference);
    }

    @Test
    public void testOnClick_gearNotClicked_listenerNotInvoked() {
        mConnectedWifiEntryPreference.onClick(mView);

        verify(mOnGearClickListener, never()).onGearClick(mConnectedWifiEntryPreference);
    }

    @Test
    public void testWidgetLayoutPreference() {
        assertThat(mConnectedWifiEntryPreference.getWidgetLayoutResource())
            .isEqualTo(R.layout.preference_widget_gear_optional_background);
    }
}
