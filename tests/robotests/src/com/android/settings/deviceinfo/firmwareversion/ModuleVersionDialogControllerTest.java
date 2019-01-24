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

package com.android.settings.deviceinfo.firmwareversion;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ModuleVersionDialogControllerTest {

    @Mock
    private FirmwareVersionDialogFragment mDialog;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private ModuleVersionDialogController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mDialog.getContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = new ModuleVersionDialogController(mDialog);
    }

    @Test
    public void initialize_noMainlineModuleProvider_shouldRemoveSettingFromDialog() {
        when(mContext.getString(
            com.android.internal.R.string.config_defaultModuleMetadataProvider)).thenReturn(null);

        mController.initialize();

        verify(mDialog).removeSettingFromScreen(mController.MODULE_VERSION_LABEL_ID);
        verify(mDialog).removeSettingFromScreen(mController.MODULE_VERSION_VALUE_ID);
    }

    @Test
    public void initialize_noMainlineModulePackageInfo_shouldRemoveSettingFromDialog()
            throws PackageManager.NameNotFoundException {
        final String provider = "test.provider";
        when(mContext.getString(
            com.android.internal.R.string.config_defaultModuleMetadataProvider))
            .thenReturn(provider);
        when(mPackageManager.getPackageInfo(eq(provider), anyInt()))
            .thenThrow(new PackageManager.NameNotFoundException());

        mController.initialize();

        verify(mDialog).removeSettingFromScreen(mController.MODULE_VERSION_LABEL_ID);
        verify(mDialog).removeSettingFromScreen(mController.MODULE_VERSION_VALUE_ID);
    }

    @Test
    public void initialize_hasMainlineModulePackageInfo_shouldshouldSetDialogTextToMainlineVersion()
            throws PackageManager.NameNotFoundException {
        final String provider = "test.provider";
        final String version = "test version 123";
        final PackageInfo info = new PackageInfo();
        info.versionName = version;
        when(mContext.getString(
            com.android.internal.R.string.config_defaultModuleMetadataProvider))
            .thenReturn(provider);
        when(mPackageManager.getPackageInfo(eq(provider), anyInt())).thenReturn(info);

        mController.initialize();

        verify(mDialog).setText(mController.MODULE_VERSION_VALUE_ID, version);
    }

}
