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

package com.android.settings.fuelgauge.anomaly;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;

import com.android.settings.R;
import com.android.settings.fuelgauge.anomaly.action.AnomalyAction;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.FragmentTestUtil;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AnomalyDialogFragmentTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String DISPLAY_NAME = "app";
    private static final int UID = 111;

    @Mock
    private AnomalyUtils mAnomalyUtils;
    @Mock
    private AnomalyAction mAnomalyAction;
    private Anomaly mWakeLockAnomaly;
    private Anomaly mWakeupAlarmAnomaly;
    private Anomaly mWakeupAlarmAnomaly2;
    private Anomaly mBluetoothAnomaly;
    private AnomalyDialogFragment mAnomalyDialogFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mWakeLockAnomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKE_LOCK)
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .setDisplayName(DISPLAY_NAME)
                .build();
        mWakeupAlarmAnomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .setDisplayName(DISPLAY_NAME)
                .build();
        mWakeupAlarmAnomaly2 = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .setDisplayName(DISPLAY_NAME)
                .setTargetSdkVersion(Build.VERSION_CODES.O)
                .build();
        mBluetoothAnomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.BLUETOOTH_SCAN)
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .setDisplayName(DISPLAY_NAME)
                .build();
        FakeFeatureFactory.setupForTest(mContext);
    }

    @Test
    public void testOnCreateDialog_hasCorrectData() {
        mAnomalyDialogFragment = AnomalyDialogFragment.newInstance(mWakeLockAnomaly,
                0 /* metricskey */);
        FragmentTestUtil.startFragment(mAnomalyDialogFragment);

        assertThat(mAnomalyDialogFragment.mAnomaly).isEqualTo(mWakeLockAnomaly);
    }

    @Test
    public void testOnCreateDialog_wakelockAnomaly_fireForceStopDialog() {
        mAnomalyDialogFragment = AnomalyDialogFragment.newInstance(mWakeLockAnomaly,
                0 /* metricskey */);

        FragmentTestUtil.startFragment(mAnomalyDialogFragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.dialog_stop_message, mWakeLockAnomaly.displayName));
        assertThat(shadowDialog.getTitle()).isEqualTo(
                mContext.getString(R.string.dialog_stop_title));
        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText()).isEqualTo(
                mContext.getString(R.string.dialog_stop_ok));
        assertThat(dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText()).isEqualTo(
                mContext.getString(R.string.dlg_cancel));
    }

    @Test
    public void testOnCreateDialog_wakeupAlarmAnomalyPriorO_fireStopAndBackgroundCheckDialog() {
        mAnomalyDialogFragment = AnomalyDialogFragment.newInstance(mWakeupAlarmAnomaly,
                0 /* metricskey */);

        FragmentTestUtil.startFragment(mAnomalyDialogFragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.dialog_background_check_message,
                        mWakeLockAnomaly.displayName));
        assertThat(shadowDialog.getTitle()).isEqualTo(
                mContext.getString(R.string.dialog_background_check_title));
        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText()).isEqualTo(
                mContext.getString(R.string.dialog_background_check_ok));
        assertThat(dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText()).isEqualTo(
                mContext.getString(R.string.dlg_cancel));
    }

    @Test
    public void testOnCreateDialog_wakeupAlarmAnomalyTargetingO_fireForceStopDialog() {
        mAnomalyDialogFragment = AnomalyDialogFragment.newInstance(mWakeupAlarmAnomaly2,
                0 /* metricskey */);

        FragmentTestUtil.startFragment(mAnomalyDialogFragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.dialog_stop_message_wakeup_alarm,
                        mWakeLockAnomaly.displayName));
        assertThat(shadowDialog.getTitle()).isEqualTo(
                mContext.getString(R.string.dialog_stop_title));
        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText()).isEqualTo(
                mContext.getString(R.string.dialog_stop_ok));
        assertThat(dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText()).isEqualTo(
                mContext.getString(R.string.dlg_cancel));
    }
}
