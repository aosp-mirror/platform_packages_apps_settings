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
 * limitations under the License.
 */

package com.android.settings.deviceinfo.firmwareversion;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController.MODULE_UPDATE_INTENT;
import static com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController.MODULE_UPDATE_V2_INTENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MainlineModuleVersionPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private Preference mPreference;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPreference = new Preference(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    public void getAvailabilityStatus_noMainlineModuleProvider_unavailable() {
        when(mContext.getString(
                com.android.internal.R.string.config_defaultModuleMetadataProvider)).thenReturn(
                null);

        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_noMainlineModulePackageInfo_unavailable() throws Exception {
        final String provider = "test.provider";
        when(mContext.getString(
                com.android.internal.R.string.config_defaultModuleMetadataProvider))
                .thenReturn(provider);
        when(mPackageManager.getPackageInfo(eq(provider), anyInt()))
                .thenThrow(new PackageManager.NameNotFoundException());

        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_hasMainlineModulePackageInfo_available() throws Exception {
        setupModulePackage("test version 123");

        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updateState_canHandleV2Intent_setIntentToPreference() throws Exception {
        setupModulePackage("test version 123");
        when(mPackageManager.resolveActivity(MODULE_UPDATE_V2_INTENT, 0))
                .thenReturn(new ResolveInfo());
        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        controller.updateState(mPreference);

        assertThat(mPreference.getIntent()).isEqualTo(MODULE_UPDATE_V2_INTENT);
    }

    @Test
    public void updateState_canHandleV2Intent_preferenceShouldBeSelectable() throws Exception {
        setupModulePackage("test version 123");
        when(mPackageManager.resolveActivity(MODULE_UPDATE_V2_INTENT, 0))
                .thenReturn(new ResolveInfo());
        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        controller.updateState(mPreference);

        assertThat(mPreference.isSelectable()).isTrue();
    }

    @Test
    public void updateState_canHandleIntent_setIntentToPreference() throws Exception {
        setupModulePackage("test version 123");
        when(mPackageManager.resolveActivity(MODULE_UPDATE_INTENT, 0))
                .thenReturn(new ResolveInfo());

        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        controller.updateState(mPreference);

        assertThat(mPreference.getIntent()).isEqualTo(MODULE_UPDATE_INTENT);
    }

    @Test
    public void updateState_canHandleIntent_preferenceShouldBeSelectable() throws Exception {
        setupModulePackage("test version 123");
        when(mPackageManager.resolveActivity(MODULE_UPDATE_INTENT, 0))
                .thenReturn(new ResolveInfo());

        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        controller.updateState(mPreference);

        assertThat(mPreference.isSelectable()).isTrue();
    }

    @Test
    public void updateState_cannotHandleIntent_setNullToPreference() throws Exception {
        setupModulePackage("test version 123");
        when(mPackageManager.resolveActivity(MODULE_UPDATE_INTENT, 0))
                .thenReturn(null);
        when(mPackageManager.resolveActivity(MODULE_UPDATE_V2_INTENT, 0))
                .thenReturn(null);

        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        controller.updateState(mPreference);

        assertThat(mPreference.getIntent()).isNull();
    }

    @Test
    public void getSummary_versionIsNull_returnNull() throws Exception {
        setupModulePackage(null);

        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        assertThat(controller.getSummary()).isNull();
    }

    @Test
    public void getSummary_versionIsMonth_returnMonth() throws Exception {
        setupModulePackage("2019-05");

        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        assertThat(controller.getSummary()).isEqualTo("May 01, 2019");
    }

    @Test
    public void getSummary_versionIsDate_returnDate() throws Exception {
        setupModulePackage("2019-05-13");

        final MainlineModuleVersionPreferenceController controller =
                new MainlineModuleVersionPreferenceController(mContext, "key");

        assertThat(controller.getSummary()).isEqualTo("May 13, 2019");
    }

    private void setupModulePackage(String version) throws Exception {
        final String provider = "test.provider";
        final PackageInfo info = new PackageInfo();
        info.versionName = version;
        when(mContext.getString(
                com.android.internal.R.string.config_defaultModuleMetadataProvider))
                .thenReturn(provider);
        when(mPackageManager.getPackageInfo(eq(provider), anyInt())).thenReturn(info);
    }
}
