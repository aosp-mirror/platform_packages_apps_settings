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

package com.android.settings.applications.specialaccess.premiumsms;

import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.telephony.SmsManager;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PremiumSmsAccessTest {

    private FakeFeatureFactory mFeatureFactory;
    private PremiumSmsAccess mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment = new PremiumSmsAccess();
        mFragment.onAttach(RuntimeEnvironment.application);
    }

    @Test
    public void logSpecialPermissionChange() {
        mFragment.logSpecialPermissionChange(SmsManager.PREMIUM_SMS_CONSENT_ASK_USER,
                "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_PREMIUM_SMS_ASK,
                mFragment.getMetricsCategory(),
                "app",
                SmsManager.PREMIUM_SMS_CONSENT_ASK_USER);

        mFragment.logSpecialPermissionChange(SmsManager.PREMIUM_SMS_CONSENT_NEVER_ALLOW,
                "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_PREMIUM_SMS_DENY,
                mFragment.getMetricsCategory(),
                "app",
                SmsManager.PREMIUM_SMS_CONSENT_NEVER_ALLOW);

        mFragment.logSpecialPermissionChange(SmsManager.PREMIUM_SMS_CONSENT_ALWAYS_ALLOW,
                "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_PREMIUM_SMS_ALWAYS_ALLOW,
                mFragment.getMetricsCategory(),
                "app",
                SmsManager.PREMIUM_SMS_CONSENT_ALWAYS_ALLOW);
    }
}
