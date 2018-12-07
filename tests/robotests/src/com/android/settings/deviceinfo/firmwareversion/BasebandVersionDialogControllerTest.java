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

package com.android.settings.deviceinfo.firmwareversion;

import static com.android.settings.deviceinfo.firmwareversion.BasebandVersionDialogController
        .BASEBAND_PROPERTY;
import static com.android.settings.deviceinfo.firmwareversion.BasebandVersionDialogController
        .BASEBAND_VERSION_LABEL_ID;
import static com.android.settings.deviceinfo.firmwareversion.BasebandVersionDialogController
        .BASEBAND_VERSION_VALUE_ID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.shadow.api.Shadow.extract;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.SystemProperties;

import com.android.settings.testutils.shadow.ShadowConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowConnectivityManager.class)
public class BasebandVersionDialogControllerTest {

    @Mock
    private FirmwareVersionDialogFragment mDialog;

    private Context mContext;
    private BasebandVersionDialogController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mDialog.getContext()).thenReturn(mContext);
        mController = new BasebandVersionDialogController(mDialog);
    }

    @Test
    public void initialize_wifiOnly_shouldRemoveSettingFromDialog() {
        ShadowConnectivityManager connectivityManager =
                extract(mContext.getSystemService(ConnectivityManager.class));
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, false);

        mController.initialize();

        verify(mDialog).removeSettingFromScreen(BASEBAND_VERSION_LABEL_ID);
        verify(mDialog).removeSettingFromScreen(BASEBAND_VERSION_VALUE_ID);
    }

    @Test
    public void initialize_hasMobile_shouldSetDialogTextToBasebandVersion() {
        final String text = "test";
        SystemProperties.set(BASEBAND_PROPERTY, text);
        ShadowConnectivityManager connectivityManager =
                extract(mContext.getSystemService(ConnectivityManager.class));
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, true);

        mController.initialize();

        verify(mDialog).setText(BASEBAND_VERSION_VALUE_ID, text);
    }
}
