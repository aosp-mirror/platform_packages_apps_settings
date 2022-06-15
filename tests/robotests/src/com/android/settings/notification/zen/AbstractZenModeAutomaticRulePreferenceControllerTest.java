/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.notification.zen;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.service.notification.ConditionProviderService;
import android.service.notification.ZenPolicy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AbstractZenModeAutomaticRulePreferenceControllerTest {

    @Mock
    private PackageManager mPm;
    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testGetSettingsActivity_configActivity() throws Exception {
        AutomaticZenRule rule = new AutomaticZenRule("name", null,
                new ComponentName(mContext.getPackageName(), "test"),  Uri.EMPTY,
                new ZenPolicy(), NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);
        rule.setPackageName(mContext.getPackageName());

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        ComponentName actual = AbstractZenModeAutomaticRulePreferenceController
                .getSettingsActivity(mPm, rule, null);

        assertThat(actual).isEqualTo(new ComponentName(mContext.getPackageName(), "test"));
    }

    @Test
    public void testGetSettingsActivity_configActivity_wrongPackage() throws Exception {
        AutomaticZenRule rule = new AutomaticZenRule("name", null,
                new ComponentName("another", "test"),  Uri.EMPTY,
                new ZenPolicy(), NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);
        rule.setPackageName(mContext.getPackageName());

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        ComponentName actual = AbstractZenModeAutomaticRulePreferenceController
                .getSettingsActivity(mPm, rule, null);

        assertThat(actual).isNull();
    }

    @Test
    public void testGetSettingsActivity_configActivity_unspecifiedOwner() throws Exception {
        AutomaticZenRule rule = new AutomaticZenRule("name", null,
                new ComponentName("another", "test"),  Uri.EMPTY,
                new ZenPolicy(), NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        ComponentName actual = AbstractZenModeAutomaticRulePreferenceController
                .getSettingsActivity(mPm, rule, null);

        assertThat(actual).isEqualTo(new ComponentName("another", "test"));
    }

    @Test
    public void testGetSettingsActivity_cps() throws Exception {
        AutomaticZenRule rule = new AutomaticZenRule("name",
                new ComponentName(mContext.getPackageName(), "service"), null, Uri.EMPTY,
                new ZenPolicy(), NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);
        rule.setPackageName(mContext.getPackageName());

        ComponentInfo ci = new ComponentInfo();
        ci.packageName = mContext.getPackageName();
        ci.metaData = new Bundle();
        ci.metaData.putString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY,
                ComponentName.flattenToShortString(
                        new ComponentName(mContext.getPackageName(), "activity")));

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        ComponentName actual = AbstractZenModeAutomaticRulePreferenceController
                .getSettingsActivity(mPm, rule, ci);

        assertThat(actual).isEqualTo(new ComponentName(mContext.getPackageName(), "activity"));
    }

    @Test
    public void testGetSettingsActivity_cps_wrongPackage() throws Exception {
        AutomaticZenRule rule = new AutomaticZenRule("name",
                new ComponentName(mContext.getPackageName(), "service"), null, Uri.EMPTY,
                new ZenPolicy(), NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);
        rule.setPackageName("other");

        ComponentInfo ci = new ComponentInfo();
        ci.packageName = mContext.getPackageName();
        ci.metaData = new Bundle();
        ci.metaData.putString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY,
                ComponentName.flattenToShortString(
                        new ComponentName(mContext.getPackageName(), "activity")));

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        ComponentName actual = AbstractZenModeAutomaticRulePreferenceController
                .getSettingsActivity(mPm, rule, ci);

        assertThat(actual).isNull();
    }

    @Test
    public void testGetSettingsActivity_cps_unspecifiedPackage() throws Exception {
        AutomaticZenRule rule = new AutomaticZenRule("name",
                new ComponentName(mContext.getPackageName(), "service"), null, Uri.EMPTY,
                new ZenPolicy(), NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);

        ComponentInfo ci = new ComponentInfo();
        ci.packageName = mContext.getPackageName();
        ci.metaData = new Bundle();
        ci.metaData.putString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY,
                ComponentName.flattenToShortString(
                        new ComponentName(mContext.getPackageName(), "activity")));

        when(mPm.getPackageUid(null, 0)).thenReturn(-1);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        ComponentName actual = AbstractZenModeAutomaticRulePreferenceController
                .getSettingsActivity(mPm, rule, ci);

        assertThat(actual).isEqualTo(new ComponentName(mContext.getPackageName(), "activity"));
    }
}