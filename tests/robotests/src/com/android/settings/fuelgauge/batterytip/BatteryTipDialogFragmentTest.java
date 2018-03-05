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
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.FragmentTestUtil;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class BatteryTipDialogFragmentTest {

    private static final String PACKAGE_NAME = "com.android.app";
    private static final String DISPLAY_NAME = "app";
    private static final long SCREEN_TIME_MS = DateUtils.HOUR_IN_MILLIS;

    private BatteryTipDialogFragment mDialogFragment;
    private Context mContext;
    private HighUsageTip mHighUsageTip;
    private RestrictAppTip mRestrictedOneAppTip;
    private RestrictAppTip mRestrictAppsTip;
    private UnrestrictAppTip mUnrestrictAppTip;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        FakeFeatureFactory.setupForTest();

        List<AppInfo> highUsageTips = new ArrayList<>();
        final AppInfo appInfo = new AppInfo.Builder()
                .setScreenOnTimeMs(SCREEN_TIME_MS)
                .setPackageName(PACKAGE_NAME)
                .build();
        highUsageTips.add(appInfo);
        mHighUsageTip = new HighUsageTip(SCREEN_TIME_MS, highUsageTips);

        final List<AppInfo> restrictApps = new ArrayList<>();
        restrictApps.add(appInfo);
        mRestrictedOneAppTip = new RestrictAppTip(BatteryTip.StateType.NEW,
                new ArrayList<>(restrictApps));
        restrictApps.add(appInfo);
        mRestrictAppsTip = new RestrictAppTip(BatteryTip.StateType.NEW,
                new ArrayList<>(restrictApps));

        mUnrestrictAppTip = new UnrestrictAppTip(BatteryTip.StateType.NEW, appInfo);
    }

    @Test
    public void testOnCreateDialog_highUsageTip_fireHighUsageDialog() {
        Robolectric.getForegroundThreadScheduler().pause();

        mDialogFragment = BatteryTipDialogFragment.newInstance(mHighUsageTip);

        FragmentTestUtil.startFragment(mDialogFragment);

        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getMessage())
            .isEqualTo(mContext.getString(R.string.battery_tip_dialog_message, "1h"));
    }

    @Test
    public void testOnCreateDialog_restrictOneAppTip_fireRestrictOneAppDialog() {
        mDialogFragment = BatteryTipDialogFragment.newInstance(mRestrictedOneAppTip);

        FragmentTestUtil.startFragment(mDialogFragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getTitle()).isEqualTo("Restrict app?");
        assertThat(shadowDialog.getMessage())
            .isEqualTo(mContext.getString(R.string.battery_tip_restrict_app_dialog_message));
    }

    @Test
    public void testOnCreateDialog_restrictAppsTip_fireRestrictAppsDialog() {
        Robolectric.getForegroundThreadScheduler().pause();

        mDialogFragment = BatteryTipDialogFragment.newInstance(mRestrictAppsTip);

        FragmentTestUtil.startFragment(mDialogFragment);

        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getTitle()).isEqualTo("Restrict 2 apps?");
        assertThat(shadowDialog.getMessage())
            .isEqualTo(mContext.getString(R.string.battery_tip_restrict_app_dialog_message));
        assertThat(shadowDialog.getView()).isNotNull();
    }

    @Test
    public void testOnCreateDialog_unRestrictAppTip_fireUnRestrictDialog() {
        mDialogFragment = BatteryTipDialogFragment.newInstance(mUnrestrictAppTip);
        ShadowUtils.setApplicationLabel(PACKAGE_NAME, DISPLAY_NAME);

        FragmentTestUtil.startFragment(mDialogFragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getTitle()).isEqualTo("Remove restriction for app?");
        assertThat(shadowDialog.getMessage())
            .isEqualTo(mContext.getString(R.string.battery_tip_unrestrict_app_dialog_message));
    }
}
