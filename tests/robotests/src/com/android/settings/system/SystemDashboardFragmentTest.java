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

package com.android.settings.system;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import com.android.settings.aware.AwareFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class, ShadowUserManager.class})
public class SystemDashboardFragmentTest {

    private Context mContext;
    private SystemDashboardFragment mFragment;

    @Before
    public void setup() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_supportSystemNavigationKeys, true);
        ShadowUserManager.getShadow().setIsAdminUser(true);
        mContext = RuntimeEnvironment.application;
        mFragment = spy(new SystemDashboardFragment());
        when(mFragment.getContext()).thenReturn(mContext);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = SystemDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final int xmlId = (new SystemDashboardFragment()).getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void showRestrictionDialog_hasValidExtra_shouldShowDialog() {
        final AwareFeatureProvider mProvider =
                FakeFeatureFactory.setupForTest().mAwareFeatureProvider;
        final Bundle bundle = new Bundle();
        bundle.putBoolean(SystemDashboardFragment.EXTRA_SHOW_AWARE_DISABLED, true);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.showRestrictionDialog();

        verify(mProvider).showRestrictionDialog(any());
    }

    @Test
    public void showRestrictionDialog_hasInvalidExtra_shouldNotShowDialog() {
        final AwareFeatureProvider mProvider =
                FakeFeatureFactory.setupForTest().mAwareFeatureProvider;

        mFragment.showRestrictionDialog();

        verify(mProvider, never()).showRestrictionDialog(any());
    }
}
