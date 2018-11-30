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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;
import android.view.DisplayCutout;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.OverlayManagerWrapper;
import com.android.settings.wrapper.OverlayManagerWrapper.OverlayInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

@RunWith(SettingsRobolectricTestRunner.class)
public class EmulateDisplayCutoutPreferenceControllerTest {

    private static final OverlayInfo ONE_DISABLED = createFakeOverlay("emulation.one", false, 1);
    private static final OverlayInfo ONE_ENABLED = createFakeOverlay("emulation.one", true, 1);
    private static final OverlayInfo TWO_DISABLED = createFakeOverlay("emulation.two", false, 2);
    private static final OverlayInfo TWO_ENABLED = createFakeOverlay("emulation.two", true, 2);

    @Mock
    private Context mContext;
    @Mock
    private OverlayManagerWrapper mOverlayManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ListPreference mPreference;
    private EmulateDisplayCutoutPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockCurrentOverlays();
        when(mPackageManager.getApplicationInfo(any(), anyInt()))
            .thenThrow(PackageManager.NameNotFoundException.class);
        mController = createController();
        mController.setPreference(mPreference);
    }

    Object mockCurrentOverlays(OverlayInfo... overlays) {
        return when(mOverlayManager.getOverlayInfosForTarget(eq("android"), anyInt()))
            .thenReturn(Arrays.asList(overlays));
    }

    @Test
    public void isAvailable_true() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_DISABLED);

        assertThat(createController().isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_false() throws Exception {
        mockCurrentOverlays();

        assertThat(createController().isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceChange_enable() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_DISABLED);

        mController.onPreferenceChange(null, TWO_DISABLED.packageName);

        verify(mOverlayManager)
            .setEnabledExclusiveInCategory(eq(TWO_DISABLED.packageName), anyInt());
    }

    @Test
    public void onPreferenceChange_disable() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_ENABLED);

        mController.onPreferenceChange(null, "");

        verify(mOverlayManager).setEnabled(eq(TWO_ENABLED.packageName), eq(false), anyInt());
    }

    @Test
    public void updateState_enabled() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_ENABLED);

        mController.updateState(null);

        verify(mPreference).setValueIndex(2);
    }

    @Test
    public void updateState_disabled() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_DISABLED);

        mController.updateState(null);

        verify(mPreference).setValueIndex(0);
    }

    @Test
    public void ordered_by_priority() throws Exception {
        mockCurrentOverlays(TWO_DISABLED, ONE_DISABLED);

        mController.updateState(null);

        verify(mPreference).setEntryValues(
                aryEq(new String[]{"", ONE_DISABLED.packageName, TWO_DISABLED.packageName}));
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled() throws Exception {
        mockCurrentOverlays(ONE_ENABLED, TWO_DISABLED);
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(screen);

        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setEnabled(false);
        verify(mOverlayManager).setEnabled(eq(ONE_ENABLED.packageName), eq(false), anyInt());
    }

    private EmulateDisplayCutoutPreferenceController createController() {
        return new EmulateDisplayCutoutPreferenceController(mContext, mPackageManager,
                mOverlayManager);
    }

    private static OverlayInfo createFakeOverlay(String pkg, boolean enabled, int priority) {
        return new OverlayInfo(pkg, DisplayCutout.EMULATION_OVERLAY_CATEGORY, enabled,
                priority);
    }
}