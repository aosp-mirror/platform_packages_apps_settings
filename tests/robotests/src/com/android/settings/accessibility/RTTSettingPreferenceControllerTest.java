/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.PersistableBundle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.ResolveInfoBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

/** Tests for {@link RTTSettingPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class RTTSettingPreferenceControllerTest {

    private ShadowPackageManager mShadowPackageManager;
    private RTTSettingPreferenceController mController;
    private TelephonyManager mTelephonyManagerFromSubId;

    @Mock
    private PersistableBundle mPersistableBundle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final Context context = spy(ApplicationProvider.getApplicationContext());
        final int subId = SubscriptionManager.getDefaultVoiceSubscriptionId();
        final String rttSettingsPackageName =
                context.getString(R.string.config_rtt_setting_package_name);
        final CarrierConfigManager configManager = spy(new CarrierConfigManager(context));
        final TelephonyManager telephonyManager = spy(new TelephonyManager(context));
        final TelecomManager telecomManager = spy(new TelecomManager(context));
        mTelephonyManagerFromSubId = spy(new TelephonyManager(context, subId));
        doReturn(telephonyManager).when(context).getSystemService(TelephonyManager.class);
        doReturn(mTelephonyManagerFromSubId).when(telephonyManager).createForSubscriptionId(subId);
        doReturn(telecomManager).when(context).getSystemService(TelecomManager.class);
        doReturn(configManager).when(context).getSystemService(CarrierConfigManager.class);
        doReturn(mPersistableBundle).when(configManager).getConfigForSubId(subId);
        doReturn(rttSettingsPackageName).when(telecomManager).getDefaultDialerPackage();

        mShadowPackageManager = shadowOf(context.getPackageManager());
        mController = spy(new RTTSettingPreferenceController(context, "rtt_setting"));
        mController.mRTTIntent = new Intent("com.android.test.action.example");
    }

    @Test
    public void getStatus_carrierAndRttSupported_settingsIntent_available() {
        doReturn(true).when(mTelephonyManagerFromSubId).isRttSupported();
        doReturn(true).when(mPersistableBundle).getBoolean(
                CarrierConfigManager.KEY_IGNORE_RTT_MODE_SETTING_BOOL);
        setupTestIntent();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getStatus_rttSupported_settingsIntent_unsupported() {
        doReturn(true).when(mTelephonyManagerFromSubId).isRttSupported();
        doReturn(false).when(mPersistableBundle).getBoolean(
                CarrierConfigManager.KEY_IGNORE_RTT_MODE_SETTING_BOOL);
        setupTestIntent();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getStatus_settingsIntent_unsupported() {
        doReturn(false).when(mTelephonyManagerFromSubId).isRttSupported();
        setupTestIntent();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getStatus_unsupported() {
        doReturn(false).when(mTelephonyManagerFromSubId).isRttSupported();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    private void setupTestIntent() {
        final ResolveInfo info = new ResolveInfoBuilder("pkg")
                .setActivity("pkg", "class").build();
        final Intent intent = new Intent("com.android.test.action.example");
        mShadowPackageManager.addResolveInfoForIntent(intent, info);
    }
}
