/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.virtual;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class})
public class ForgetDeviceDialogFragmentTest {

    private static final int ASSOCIATION_ID = 42;

    @Mock
    private AssociationInfo mAssociationInfo;
    @Mock
    private CompanionDeviceManager mCompanionDeviceManager;
    private AlertDialog mDialog;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mAssociationInfo.getId()).thenReturn(ASSOCIATION_ID);
        VirtualDeviceWrapper device = new VirtualDeviceWrapper(
                mAssociationInfo, "PersistentDeviceId", Context.DEVICE_ID_INVALID);
        ForgetDeviceDialogFragment fragment = ForgetDeviceDialogFragment.newInstance(device);
        FragmentController.setupFragment(fragment, FragmentActivity.class,
                0 /* containerViewId */, null /* bundle */);
        fragment.mDevice = device;
        fragment.mCompanionDeviceManager = mCompanionDeviceManager;
        mDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
    }

    @Test
    public void cancelDialog() {
        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();
        verify(mCompanionDeviceManager, never()).disassociate(anyInt());
    }

    @Test
    public void confirmDialog() {
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        verify(mCompanionDeviceManager).disassociate(ASSOCIATION_ID);
    }
}
