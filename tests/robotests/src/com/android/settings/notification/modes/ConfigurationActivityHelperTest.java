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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.EXTRA_AUTOMATIC_RULE_ID;
import static android.service.notification.ConditionProviderService.EXTRA_RULE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ConditionProviderService;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ConfigurationActivityHelperTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ConfigurationActivityHelper mHelper;

    @Mock private PackageManager mPm;
    @Mock private Function<ComponentName, ComponentInfo> mApprovedServiceFinder;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.getApplication();
        mHelper = new ConfigurationActivityHelper(mPm);

        when(mPm.queryIntentActivities(any(), anyInt())).thenReturn(List.of(new ResolveInfo()));
    }

    @Test
    public void getConfigurationActivityIntentForMode_configActivity() throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setId("id")
                .setPackage(mContext.getPackageName())
                .setConfigurationActivity(new ComponentName(mContext.getPackageName(), "test"))
                .build();
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mHelper.getConfigurationActivityIntentForMode(mode, mApprovedServiceFinder);

        assertThat(res).isNotNull();
        assertThat(res.getStringExtra(EXTRA_RULE_ID)).isEqualTo("id");
        assertThat(res.getStringExtra(EXTRA_AUTOMATIC_RULE_ID)).isEqualTo("id");
        assertThat(res.getComponent()).isEqualTo(
                new ComponentName(mContext.getPackageName(), "test"));
    }

    @Test
    public void getConfigurationActivityIntentForMode_configActivityNotResolvable_returnsNull()
            throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setId("id")
                .setPackage(mContext.getPackageName())
                .setConfigurationActivity(new ComponentName(mContext.getPackageName(), "test"))
                .build();
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);
        when(mPm.queryIntentActivities(any(), anyInt())).thenReturn(new ArrayList<>());

        Intent res = mHelper.getConfigurationActivityIntentForMode(mode, mApprovedServiceFinder);

        assertThat(res).isNull();
    }

    @Test
    public void getConfigurationActivityIntentForMode_configActivityAndWrongPackage_returnsNull()
            throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setPackage(mContext.getPackageName())
                .setConfigurationActivity(new ComponentName("another", "test"))
                .build();
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mHelper.getConfigurationActivityIntentForMode(mode, mApprovedServiceFinder);

        assertThat(res).isNull();
    }

    @Test
    public void getConfigurationActivityIntentForMode_configActivityAndUnspecifiedOwner()
            throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setId("id")
                .setPackage(null)
                .setConfigurationActivity(new ComponentName("another", "test"))
                .build();
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mHelper.getConfigurationActivityIntentForMode(mode, mApprovedServiceFinder);

        assertThat(res).isNotNull();
        assertThat(res.getStringExtra(EXTRA_RULE_ID)).isEqualTo("id");
        assertThat(res.getStringExtra(EXTRA_AUTOMATIC_RULE_ID)).isEqualTo("id");
        assertThat(res.getComponent()).isEqualTo(new ComponentName("another", "test"));
    }

    @Test
    public void getConfigurationActivityIntentForMode_cps() throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setId("id")
                .setPackage(mContext.getPackageName())
                .setOwner(new ComponentName(mContext.getPackageName(), "service"))
                .build();
        ComponentInfo ci = new ComponentInfo();
        ci.packageName = mContext.getPackageName();
        ci.metaData = new Bundle();
        ci.metaData.putString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY,
                ComponentName.flattenToShortString(
                        new ComponentName(mContext.getPackageName(), "activity")));
        when(mApprovedServiceFinder.apply(new ComponentName(mContext.getPackageName(), "service")))
                .thenReturn(ci);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mHelper.getConfigurationActivityIntentForMode(mode, mApprovedServiceFinder);

        assertThat(res).isNotNull();
        assertThat(res.getStringExtra(EXTRA_RULE_ID)).isEqualTo("id");
        assertThat(res.getStringExtra(EXTRA_AUTOMATIC_RULE_ID)).isEqualTo("id");
        assertThat(res.getComponent()).isEqualTo(
                new ComponentName(mContext.getPackageName(), "activity"));
    }

    @Test
    public void getConfigurationActivityIntentForMode_cpsAndWrongPackage_returnsNull()
            throws Exception {
        ZenMode mode = new TestModeBuilder()
                .setPackage("other")
                .setOwner(new ComponentName(mContext.getPackageName(), "service"))
                .build();
        ComponentInfo ci = new ComponentInfo();
        ci.packageName = mContext.getPackageName();
        ci.metaData = new Bundle();
        ci.metaData.putString(ConditionProviderService.META_DATA_CONFIGURATION_ACTIVITY,
                ComponentName.flattenToShortString(
                        new ComponentName(mContext.getPackageName(), "activity")));
        when(mApprovedServiceFinder.apply(new ComponentName(mContext.getPackageName(), "service")))
                .thenReturn(ci);
        when(mPm.getPackageUid(mContext.getPackageName(), 0)).thenReturn(1);

        Intent res = mHelper.getConfigurationActivityIntentForMode(mode, mApprovedServiceFinder);

        assertThat(res).isNull();
    }
}
