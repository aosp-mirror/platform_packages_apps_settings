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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.wifi.WifiDialog2.WifiDialog2Listener;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowEntityHeaderController.class)
public class WifiDialog2Test {
    @Mock private WifiEntry mMockWifiEntry;

    private Context mContext = RuntimeEnvironment.application;

    private WifiDialog2Listener mListener = new WifiDialog2Listener() {};

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createModal_usesDefaultTheme() {
        WifiDialog2 modal = WifiDialog2
                .createModal(mContext, mListener, mMockWifiEntry, WifiConfigUiBase2.MODE_CONNECT);

        WifiDialog2 wifiDialog2 = new WifiDialog2(mContext, mListener, mMockWifiEntry,
                WifiConfigUiBase2.MODE_CONNECT, 0 /* style */, false /* hideSubmitButton */);
        assertThat(modal.getContext().getThemeResId())
                .isEqualTo(wifiDialog2.getContext().getThemeResId());
    }

    @Test
    public void createModal_whenSetTheme_shouldBeCustomizedTheme() {
        WifiDialog2 modal = WifiDialog2.createModal(mContext, mListener, mMockWifiEntry,
                WifiConfigUiBase2.MODE_CONNECT, R.style.SuwAlertDialogThemeCompat_Light);

        WifiDialog2 wifiDialog2 = new WifiDialog2(mContext, mListener, mMockWifiEntry,
                WifiConfigUiBase2.MODE_CONNECT, R.style.SuwAlertDialogThemeCompat_Light,
                        false /* hideSubmitButton */);
        assertThat(modal.getContext().getThemeResId())
                .isEqualTo(wifiDialog2.getContext().getThemeResId());
    }
}
