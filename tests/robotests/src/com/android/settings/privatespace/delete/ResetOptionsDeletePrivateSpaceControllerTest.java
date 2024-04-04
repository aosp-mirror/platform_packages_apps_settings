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

package com.android.settings.privatespace.delete;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.system.ResetDashboardFragment.PRIVATE_SPACE_DELETE_CREDENTIAL_REQUEST;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.platform.test.flag.junit.SetFlagsRule;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.system.ResetDashboardFragment;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = ShadowAlertDialogCompat.class)
public class ResetOptionsDeletePrivateSpaceControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String KEY = "reset_delete_private_space";
    private Context mContext;
    private ResetOptionsDeletePrivateSpaceController mController;
    private ResetOptionsDeletePrivateSpaceController.DeletePrivateSpaceDialogFragment
            mDialogFragment;
    @Mock FragmentTransaction mFragmentTransaction;
    @Mock ResetDashboardFragment mResetDashboardFragment;
    @Mock FragmentManager mFragmentManager;
    @Mock ResetOptionsDeletePrivateSpaceController.DeletePrivateSpaceDialogFragment
            mMockAlertDialog;
    @Mock Intent mIntent;
    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ResetOptionsDeletePrivateSpaceController(mContext, KEY);
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mDialogFragment =
                new ResetOptionsDeletePrivateSpaceController.DeletePrivateSpaceDialogFragment();
    }

    @Test
    public void getAvailabilityStatus_flagsDisabled_returnsUnsupported() {
        mSetFlagsRule.disableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mSetFlagsRule.disableFlags(android.multiuser.Flags.FLAG_DELETE_PRIVATE_SPACE_FROM_RESET);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_deleteFromResetFlagDisabled_returnsUnsupported() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mSetFlagsRule.disableFlags(android.multiuser.Flags.FLAG_DELETE_PRIVATE_SPACE_FROM_RESET);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_flagsEnabledCanAddProfile_returnsAvailable() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_DELETE_PRIVATE_SPACE_FROM_RESET);
        ResetOptionsDeletePrivateSpaceController spyController = spy(mController);
        doReturn(true).when(spyController).isPrivateSpaceEntryPointEnabled();

        assertThat(spyController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagsEnabledCannotAddProfile_returnsUnsupported() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_DELETE_PRIVATE_SPACE_FROM_RESET);
        ResetOptionsDeletePrivateSpaceController spyController = spy(mController);
        doReturn(false).when(spyController).isPrivateSpaceEntryPointEnabled();

        assertThat(spyController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void handleActivityResult_success_showsAlertDialog() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_DELETE_PRIVATE_SPACE_FROM_RESET);
        ResetOptionsDeletePrivateSpaceController controller = spy(mController);
        doReturn(mFragmentManager).when(controller).getFragmentManager();
        doReturn(mFragmentTransaction).when(mFragmentManager).beginTransaction();
        doReturn(mMockAlertDialog).when(controller).getDeleteDialogFragment();

        controller.setFragment(mResetDashboardFragment);
        boolean result =
                controller.handleActivityResult(
                        PRIVATE_SPACE_DELETE_CREDENTIAL_REQUEST, Activity.RESULT_OK, mIntent);

        assertThat(result).isTrue();
        verify(mMockAlertDialog).show((FragmentManager) any(), anyString());
    }

    @Test
    public void handleActivityResult_notSuccess_noDialogShown() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_DELETE_PRIVATE_SPACE_FROM_RESET);

        mController.setFragment(mResetDashboardFragment);
        boolean result =
                mController.handleActivityResult(
                        PRIVATE_SPACE_DELETE_CREDENTIAL_REQUEST, Activity.RESULT_CANCELED, mIntent);

        assertThat(result).isFalse();
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Test
    public void setAlertDialog_showsDialog_onPositiveButtonClickDialogRemoved() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_DELETE_PRIVATE_SPACE_FROM_RESET);

        mDialogFragment.show(mActivity.getSupportFragmentManager(), "className");
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isTrue();
        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);

        positiveButton.performClick();
        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Test
    public void setAlertDialog_showsDialog_onNegativeButtonClickDialogRemoved() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_DELETE_PRIVATE_SPACE_FROM_RESET);

        mDialogFragment.show(mActivity.getSupportFragmentManager(), "fragmentName");
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isTrue();
        Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        negativeButton.performClick();
        assertThat(alertDialog.isShowing()).isFalse();
    }
}
