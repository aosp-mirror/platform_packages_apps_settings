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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class OverlayCategoryPreferenceControllerTest {

    private static final OverlayInfo ONE_DISABLED = createFakeOverlay("overlay.one", false, 1);
    private static final OverlayInfo ONE_ENABLED = createFakeOverlay("overlay.one", true, 1);
    private static final OverlayInfo TWO_DISABLED = createFakeOverlay("overlay.two", false, 2);
    private static final OverlayInfo TWO_ENABLED = createFakeOverlay("overlay.two", true, 2);
    private static final String TEST_CATEGORY = "android.test.category";

    @Mock
    private IOverlayManager mOverlayManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ListPreference mPreference;
    private OverlayCategoryPreferenceController mController;

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
        try {
            return when(mOverlayManager.getOverlayInfosForTarget(eq("android"), anyInt()))
                .thenReturn(Arrays.asList(overlays));
        } catch (RemoteException re) {
            return new ArrayList<OverlayInfo>();
        }
    }

    @Test
    public void getKey_returnsCategory() {
        assertThat(createController().getPreferenceKey()).isEqualTo(TEST_CATEGORY);
    }

    @Test
    public void isAvailable_true() {
        mockCurrentOverlays(ONE_DISABLED, TWO_DISABLED);

        assertThat(createController().isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_false() {
        mockCurrentOverlays();

        assertThat(createController().isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceChange_enable() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_DISABLED);

        mController.onPreferenceChange(null, TWO_DISABLED.packageName);
        ShadowApplication.runBackgroundTasks();

        verify(mOverlayManager)
            .setEnabledExclusiveInCategory(eq(TWO_DISABLED.packageName), anyInt());
    }

    @Test
    public void onPreferenceChange_enable_fails() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_DISABLED);
        when(mOverlayManager.setEnabledExclusiveInCategory(eq(TWO_DISABLED.packageName), anyInt()))
                .thenReturn(false);

        mController.onPreferenceChange(null, TWO_DISABLED.packageName);
        ShadowApplication.runBackgroundTasks();

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.overlay_toast_failed_to_apply));
    }

    @Test
    public void onPreferenceChange_disable() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_ENABLED);

        mController.onPreferenceChange(
                null, OverlayCategoryPreferenceController.PACKAGE_DEVICE_DEFAULT);
        ShadowApplication.runBackgroundTasks();

        verify(mOverlayManager).setEnabled(eq(TWO_ENABLED.packageName), eq(false), anyInt());
    }

    @Test
    public void onPreferenceChange_disable_fails() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_ENABLED);
        when(mOverlayManager.setEnabled(eq(TWO_ENABLED.packageName), eq(false), anyInt()))
                .thenReturn(false);

        mController.onPreferenceChange(
                null, OverlayCategoryPreferenceController.PACKAGE_DEVICE_DEFAULT);
        ShadowApplication.runBackgroundTasks();

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.overlay_toast_failed_to_apply));
    }

    @Test
    public void onPreferenceChange_disable_throws() throws Exception {
        mockCurrentOverlays(ONE_DISABLED, TWO_ENABLED);
        when(mOverlayManager.setEnabled(eq(TWO_ENABLED.packageName), eq(false), anyInt()))
                .thenThrow(new RemoteException());

        mController.onPreferenceChange(
                null, OverlayCategoryPreferenceController.PACKAGE_DEVICE_DEFAULT);
        ShadowApplication.runBackgroundTasks();

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                RuntimeEnvironment.application.getString(R.string.overlay_toast_failed_to_apply));
    }

    @Test
    public void updateState_enabled() {
        mockCurrentOverlays(ONE_DISABLED, TWO_ENABLED);

        mController.updateState(null);

        verify(mPreference).setValue(TWO_ENABLED.packageName);
    }

    @Test
    public void updateState_disabled() {
        mockCurrentOverlays(ONE_DISABLED, TWO_DISABLED);

        mController.updateState(null);

        verify(mPreference).setValue(OverlayCategoryPreferenceController.PACKAGE_DEVICE_DEFAULT);
    }

    @Test
    public void ordered_by_priority() {
        mockCurrentOverlays(TWO_DISABLED, ONE_DISABLED);

        mController.updateState(null);

        verify(mPreference).setEntryValues(
                aryEq(new String[]{
                        OverlayCategoryPreferenceController.PACKAGE_DEVICE_DEFAULT,
                        ONE_DISABLED.packageName,
                        TWO_DISABLED.packageName}));
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

    private OverlayCategoryPreferenceController createController() {
        return new OverlayCategoryPreferenceController(RuntimeEnvironment.application,
                mPackageManager, mOverlayManager, TEST_CATEGORY);
    }

    private static OverlayInfo createFakeOverlay(String pkg, boolean enabled, int priority) {
        final int state = (enabled) ? OverlayInfo.STATE_ENABLED : OverlayInfo.STATE_DISABLED;

        return new OverlayInfo(pkg /* packageName */,
                "android" /* targetPackageName */,
                null /* targetOverlayableName */,
                TEST_CATEGORY/* category */,
                pkg + ".baseCodePath" /* baseCodePath */,
                state /* state */,
                0 /* userId */,
                priority,
                true /* isStatic */);
    }
}