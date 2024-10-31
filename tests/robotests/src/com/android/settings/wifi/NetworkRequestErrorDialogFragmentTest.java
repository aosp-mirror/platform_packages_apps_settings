/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import android.content.DialogInterface;
import android.net.wifi.WifiManager.NetworkRequestUserSelectionCallback;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.wifi.NetworkRequestErrorDialogFragment.ERROR_DIALOG_TYPE;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class NetworkRequestErrorDialogFragmentTest {

    private NetworkRequestDialogActivity mActivity;
    private NetworkRequestErrorDialogFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        when(fakeFeatureFactory.wifiTrackerLibProvider.createWifiPickerTracker(
                any(), any(), any(), any(), any(), anyLong(), anyLong(), any()))
                .thenReturn(mock(WifiPickerTracker.class));

        mActivity = Robolectric.setupActivity(NetworkRequestDialogActivity.class);
        mFragment = spy(NetworkRequestErrorDialogFragment.newInstance());
        mFragment.show(mActivity.getSupportFragmentManager(), null);
    }

    @Ignore
    @Test
    public void getConstructor_shouldNotThrowNoSuchMethodException() {
        try {
            Object unused = NetworkRequestErrorDialogFragment.class.getConstructor();
        } catch (NoSuchMethodException e) {
            fail("No default constructor for configuration change!");
        }
    }

    @Ignore
    @Test
    public void display_shouldShowTimeoutDialog() {
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isTrue();

        ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(alertDialog);
        assertThat(RuntimeEnvironment.application
                .getString(R.string.network_connection_timeout_dialog_message))
                .isEqualTo(shadowAlertDialog.getMessage());
    }

    @Ignore
    @Test
    public void display_shouldShowAbortDialog() {
        mFragment = spy(NetworkRequestErrorDialogFragment.newInstance());
        Bundle bundle = new Bundle();
        bundle.putSerializable(NetworkRequestErrorDialogFragment.DIALOG_TYPE,
                ERROR_DIALOG_TYPE.ABORT);
        mFragment.setArguments(bundle);
        mFragment.show(mActivity.getSupportFragmentManager(), null);

        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isTrue();

        ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(alertDialog);
        assertThat(RuntimeEnvironment.application
                .getString(R.string.network_connection_errorstate_dialog_message))
                .isEqualTo(shadowAlertDialog.getMessage());
    }

    @Ignore
    @Test
    public void clickPositiveButton_shouldCallStartScanningDialog() {
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog.isShowing()).isTrue();

        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertThat(positiveButton).isNotNull();

        positiveButton.performClick();
        verify(mFragment, times(1)).onRescanClick();
    }

    @Ignore
    @Test
    public void clickNegativeButton_shouldCloseTheDialog() {
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog.isShowing()).isTrue();

        Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(negativeButton).isNotNull();

        negativeButton.performClick();
        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Ignore
    @Test
    public void clickNegativeButton_shouldCallReject() {
        final NetworkRequestUserSelectionCallback rejectCallback =
                mock(NetworkRequestUserSelectionCallback.class);
        mFragment.setRejectCallback(rejectCallback);

        final AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        final Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negativeButton.performClick();

        verify(rejectCallback, times(1)).reject();
    }
}
