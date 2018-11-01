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

import static com.android.settings.deviceinfo.firmwareversion.SecurityPatchLevelDialogController
        .SECURITY_PATCH_LABEL_ID;
import static com.android.settings.deviceinfo.firmwareversion.SecurityPatchLevelDialogController
        .SECURITY_PATCH_VALUE_ID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;

@RunWith(SettingsRobolectricTestRunner.class)
public class SecurityPatchLevelDialogControllerTest {

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private FirmwareVersionDialogFragment mDialog;
    @Mock
    private View mView;

    private Context mContext;
    private SecurityPatchLevelDialogController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(Robolectric.setupActivity(Activity.class));
        when(mDialog.getContext()).thenReturn(mContext);
    }

    @Test
    public void initialize_noPatchInfo_shouldRemoveSettingFromDialog() {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SECURITY_PATCH", "");
        mController = new SecurityPatchLevelDialogController(mDialog);

        mController.initialize();

        verify(mDialog).removeSettingFromScreen(SECURITY_PATCH_VALUE_ID);
        verify(mDialog).removeSettingFromScreen(SECURITY_PATCH_LABEL_ID);
    }

    @Test
    public void initialize_patchInfoAvailable_shouldRegisterListeners() {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SECURITY_PATCH", "foobar");
        mController = new SecurityPatchLevelDialogController(mDialog);

        mController.initialize();

        verify(mDialog).registerClickListener(eq(SECURITY_PATCH_LABEL_ID), any());
        verify(mDialog).registerClickListener(eq(SECURITY_PATCH_VALUE_ID), any());
    }

    @Test
    public void onClick_noActivityIntent_shouldDoNothing() {
        when(mPackageManager.queryIntentActivities(any(), anyInt()))
                .thenReturn(Collections.emptyList());
        mController = new SecurityPatchLevelDialogController(mDialog);
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);

        mController.onClick(mView);

        verify(mContext, never()).startActivity(any());
    }

    @Test
    public void onClick_activityIntentFound_shouldStartActivity() {
        when(mPackageManager.queryIntentActivities(any(), anyInt()))
                .thenReturn(Collections.singletonList(null));
        mController = new SecurityPatchLevelDialogController(mDialog);
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);

        mController.onClick(mView);

        verify(mContext).startActivity(any());
    }
}
