/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.gestures;

import static android.os.UserHandle.USER_CURRENT;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import static com.android.settings.gestures.SystemNavigationGestureSettings.KEY_SYSTEM_NAV_2BUTTONS;
import static com.android.settings.gestures.SystemNavigationGestureSettings.KEY_SYSTEM_NAV_3BUTTONS;
import static com.android.settings.gestures.SystemNavigationGestureSettings.KEY_SYSTEM_NAV_GESTURAL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.os.ServiceManager;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;

import com.android.internal.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class SystemNavigationGestureSettingsTest {

    private Context mContext;

    private IOverlayManager mOverlayManager;

    private SystemNavigationGestureSettings mSettings;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mOverlayManager = mock(IOverlayManager.class);

        mSettings = new SystemNavigationGestureSettings();
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                SystemNavigationGestureSettings.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mSettings.getPreferenceScreenResId());
    }

    @Test
    public void testGetCurrentSystemNavigationMode() {
        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_GESTURAL);
        assertThat(TextUtils.equals(mSettings.getCurrentSystemNavigationMode(mContext),
                KEY_SYSTEM_NAV_GESTURAL)).isTrue();

        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_3BUTTON);
        assertThat(TextUtils.equals(mSettings.getCurrentSystemNavigationMode(mContext),
                KEY_SYSTEM_NAV_3BUTTONS)).isTrue();

        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_2BUTTON);
        assertThat(TextUtils.equals(mSettings.getCurrentSystemNavigationMode(mContext),
                KEY_SYSTEM_NAV_2BUTTONS)).isTrue();
    }

    @Test
    public void testSetCurrentSystemNavigationMode() throws Exception {
        mSettings.setCurrentSystemNavigationMode(mOverlayManager, KEY_SYSTEM_NAV_GESTURAL);
        verify(mOverlayManager, times(1)).setEnabledExclusiveInCategory(
                NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);

        mSettings.setCurrentSystemNavigationMode(mOverlayManager, KEY_SYSTEM_NAV_2BUTTONS);
        verify(mOverlayManager, times(1)).setEnabledExclusiveInCategory(
                NAV_BAR_MODE_2BUTTON_OVERLAY, USER_CURRENT);

        mSettings.setCurrentSystemNavigationMode(mOverlayManager, KEY_SYSTEM_NAV_3BUTTONS);
        verify(mOverlayManager, times(1)).setEnabledExclusiveInCategory(
                NAV_BAR_MODE_3BUTTON_OVERLAY, USER_CURRENT);
    }
}
