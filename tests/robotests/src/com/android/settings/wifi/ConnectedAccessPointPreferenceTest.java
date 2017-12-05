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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
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
                null, 0 /* iconResId */, false /* forSavedNetworks */);
        mConnectedAccessPointPreference.setOnGearClickListener(mOnGearClickListener);
    }

    @Test
    public void testOnClick_gearClicked_listenerInvoked() {
        doReturn(R.id.settings_button).when(mView).getId();

        mConnectedAccessPointPreference.onClick(mView);

        verify(mOnGearClickListener).onGearClick(mConnectedAccessPointPreference);
    }

    @Test
    public void testOnClick_gearNotClicked_listenerNotInvoked() {
        mConnectedAccessPointPreference.onClick(mView);

        verify(mOnGearClickListener, never()).onGearClick(mConnectedAccessPointPreference);
    }

}
