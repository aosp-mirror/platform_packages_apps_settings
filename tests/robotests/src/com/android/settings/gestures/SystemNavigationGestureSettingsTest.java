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

import static junit.framework.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.provider.SearchIndexableResource;

import com.android.internal.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class SystemNavigationGestureSettingsTest {

    private Context mContext;
    private SystemNavigationGestureSettings mSettings;

    @Mock
    private IOverlayManager mOverlayManager;
    @Mock
    private OverlayInfo mOverlayInfoEnabled;
    @Mock
    private OverlayInfo mOverlayInfoDisabled;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mSettings = new SystemNavigationGestureSettings();

        when(mOverlayInfoDisabled.isEnabled()).thenReturn(false);
        when(mOverlayInfoEnabled.isEnabled()).thenReturn(true);
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(mOverlayInfoDisabled);
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
        SettingsShadowResources.overrideResource(
                R.integer.config_navBarInteractionMode, NAV_BAR_MODE_GESTURAL);
        assertEquals(KEY_SYSTEM_NAV_GESTURAL, mSettings.getCurrentSystemNavigationMode(mContext));

        SettingsShadowResources.overrideResource(
                R.integer.config_navBarInteractionMode, NAV_BAR_MODE_3BUTTON);
        assertEquals(KEY_SYSTEM_NAV_3BUTTONS, mSettings.getCurrentSystemNavigationMode(mContext));

        SettingsShadowResources.overrideResource(
                R.integer.config_navBarInteractionMode, NAV_BAR_MODE_2BUTTON);
        assertEquals(KEY_SYSTEM_NAV_2BUTTONS, mSettings.getCurrentSystemNavigationMode(mContext));
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
