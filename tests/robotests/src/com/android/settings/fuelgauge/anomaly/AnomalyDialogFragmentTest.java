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

import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.FragmentTestUtil;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AnomalyDialogFragmentTest {
    @Anomaly.AnomalyType
    private static final int ANOMALY_TYPE = Anomaly.AnomalyType.WAKE_LOCK;
    private static final String PACKAGE_NAME = "com.android.app";
    private static final int UID = 111;
    private Anomaly mAnomaly;
    private AnomalyDialogFragment mAnomalyDialogFragment;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mAnomaly = new Anomaly.Builder()
                .setType(ANOMALY_TYPE)
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .build();

        mAnomalyDialogFragment = AnomalyDialogFragment.newInstance(mAnomaly);
    }

    @Test
    public void testOnCreateDialog_hasCorrectData() {
        FragmentTestUtil.startFragment(mAnomalyDialogFragment);

        assertThat(mAnomalyDialogFragment.mAnomaly).isEqualTo(mAnomaly);
    }

    @Test
    public void testOnCreateDialog_hasCorrectDialog() {
        FragmentTestUtil.startFragment(mAnomalyDialogFragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.force_stop_dlg_text));
        assertThat(shadowDialog.getTitle()).isEqualTo(
                mContext.getString(R.string.force_stop_dlg_title));
        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText()).isEqualTo(
                mContext.getString(R.string.dlg_ok));
        assertThat(dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText()).isEqualTo(
                mContext.getString(R.string.dlg_cancel));
    }
}
