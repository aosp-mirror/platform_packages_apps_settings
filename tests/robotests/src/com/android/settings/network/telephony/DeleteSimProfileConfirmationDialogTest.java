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
 * limitations under the License
 */

package com.android.settings.network.telephony;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.telephony.SubscriptionInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class DeleteSimProfileConfirmationDialogTest {
    @Mock
    private SubscriptionInfo mSubscriptionInfo;

    private DeleteSimProfileConfirmationDialog mDialogFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mDialogFragment = spy(DeleteSimProfileConfirmationDialog.newInstance(mSubscriptionInfo));
        doNothing().when(mDialogFragment).beginDeletionWithProgress();
    }

    @Test
    public void showDialog_dialogCancelled_deleteNotCalled() {
        FragmentController.setupFragment(mDialogFragment, FragmentActivity.class,
                0 /* containerViewId */,
                null /* bundle */);
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        verify(mDialogFragment, never()).beginDeletionWithProgress();
    }

    @Test
    public void showDialog_dialogOk_deleteWasCalled() {
        FragmentController.setupFragment(mDialogFragment, FragmentActivity.class,
                0 /* containerViewId */,
                null /* bundle */);
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        verify(mDialogFragment).beginDeletionWithProgress();
    }
}
