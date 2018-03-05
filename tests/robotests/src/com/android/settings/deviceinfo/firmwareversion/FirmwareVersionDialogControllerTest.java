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

import static com.android.settings.deviceinfo.firmwareversion.FirmwareVersionDialogController.FIRMWARE_VERSION_LABEL_ID;
import static com.android.settings.deviceinfo.firmwareversion.FirmwareVersionDialogController.FIRMWARE_VERSION_VALUE_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import android.view.View;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class FirmwareVersionDialogControllerTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private FirmwareVersionDialogFragment mDialog;
    @Mock
    private View mView;

    private Context mContext;
    private FirmwareVersionDialogController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mDialog.getContext()).thenReturn(mContext);
        mController = spy(new FirmwareVersionDialogController(mDialog));
        ReflectionHelpers.setField(mController, "mUserManager", mUserManager);
        doNothing().when(mController).arrayCopy();
        doNothing().when(mController).initializeAdminPermissions();
    }

    @Test
    public void initialize_shouldRegisterListenersAndSetBuildVersion() {
        mController.initialize();

        verify(mDialog).registerClickListener(eq(FIRMWARE_VERSION_VALUE_ID), any());
        verify(mDialog).registerClickListener(eq(FIRMWARE_VERSION_LABEL_ID), any());
        verify(mDialog).setText(FIRMWARE_VERSION_VALUE_ID, Build.VERSION.RELEASE);
    }

    @Test
    public void handleSettingClicked_userRestricted_shouldDoNothing() {
        final long[] hits = ReflectionHelpers.getField(mController, "mHits");
        hits[0] = Long.MAX_VALUE;
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)).thenReturn(true);

        mController.onClick(mView);

        verify(mContext, never()).startActivity(any());
    }

    @Test
    public void handleSettingClicked_userNotRestricted_shouldStartActivity() {
        final long[] hits = ReflectionHelpers.getField(mController, "mHits");
        hits[0] = Long.MAX_VALUE;
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)).thenReturn(false);

        mController.onClick(mView);

        verify(mContext).startActivity(any());
    }
}
