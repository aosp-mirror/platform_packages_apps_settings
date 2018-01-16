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

package com.android.settings.fuelgauge.batterytip;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Context;
import android.text.format.DateUtils;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowRuntimePermissionPresenter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.FragmentTestUtil;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = ShadowRuntimePermissionPresenter.class)
public class BatteryTipDialogFragmentTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final long SCREEN_TIME_MS = DateUtils.HOUR_IN_MILLIS;

    private BatteryTipDialogFragment mDialogFragment;
    private Context mContext;
    private HighUsageTip mHighUsageTip;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest();

        List<AppInfo> highUsageTips = new ArrayList<>();
        highUsageTips.add(new AppInfo.Builder().setScreenOnTimeMs(SCREEN_TIME_MS).setPackageName(
                PACKAGE_NAME).build());
        mHighUsageTip = new HighUsageTip(SCREEN_TIME_MS, highUsageTips);
    }

    @Test
    public void testOnCreateDialog_highUsageTip_fireHighUsageDialog() {
        mDialogFragment = BatteryTipDialogFragment.newInstance(mHighUsageTip);

        FragmentTestUtil.startFragment(mDialogFragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.battery_tip_dialog_message, "1h"));
    }


}
