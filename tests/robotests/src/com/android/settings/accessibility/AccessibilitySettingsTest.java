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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AccessibilitySettingsTest {

    private Context mContext;
    private AccessibilitySettings mSettings;
    private ShadowAccessibilityManager mShadowAccessibilityManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSettings = spy(new AccessibilitySettings());
        mShadowAccessibilityManager = Shadow.extract(AccessibilityManager.getInstance(mContext));
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(new ArrayList<>());
        doReturn(mContext).when(mSettings).getContext();
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = AccessibilitySettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext, R.xml.accessibility_settings);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    @Config(shadows = {ShadowDeviceConfig.class})
    public void testIsRampingRingerEnabled_settingsFlagOn_Enabled() {
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, 1 /* ON */);
        assertThat(AccessibilitySettings.isRampingRingerEnabled(mContext)).isTrue();
    }

    @Test
    @Config(shadows = {ShadowDeviceConfig.class})
    public void testIsRampingRingerEnabled_settingsFlagOff_Disabled() {
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, 0 /* OFF */);
        assertThat(AccessibilitySettings.isRampingRingerEnabled(mContext)).isFalse();
    }
}
