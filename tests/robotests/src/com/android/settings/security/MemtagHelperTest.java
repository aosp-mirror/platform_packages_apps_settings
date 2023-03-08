/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemProperties;

@RunWith(RobolectricTestRunner.class)
public class MemtagHelperTest {
    private final String mMemtagProperty = "arm64.memtag.bootctl";
    private final String mMemtagSupportedProperty = "ro.arm64.memtag.bootctl_settings_toggle";
    private final String mDeviceConfigOverride =
            "persist.device_config.memory_safety_native_boot.bootloader_override";

    @Test
    public void isChecked_empty_isFalse() {
        ShadowSystemProperties.override(mMemtagProperty, "");
        assertThat(MemtagHelper.isChecked()).isFalse();
    }

    @Test
    public void isChecked_memtag_isTrue() {
        ShadowSystemProperties.override(mMemtagProperty, "memtag");
        assertThat(MemtagHelper.isChecked()).isTrue();
    }

    @Test
    public void isChecked_memtagAndKernel_isTrue() {
        ShadowSystemProperties.override(mMemtagProperty, "memtag,memtag-kernel");
        assertThat(MemtagHelper.isChecked()).isTrue();
    }

    @Test
    public void isChecked_kernel_isFalse() {
        ShadowSystemProperties.override(mMemtagProperty, "memtag-kernel");
        assertThat(MemtagHelper.isChecked()).isFalse();
    }

    @Test
    public void isChecked_kernelAndMemtag_isTrue() {
        ShadowSystemProperties.override(mMemtagProperty, "memtag-kernel,memtag");
        assertThat(MemtagHelper.isChecked()).isTrue();
    }

    @Test
    public void SetChecked_true_isMemtag() {
        MemtagHelper.setChecked(true);
        assertThat(SystemProperties.get(mMemtagProperty)).isEqualTo("memtag");
    }

    @Test
    public void SetChecked_false_isNone() {
        MemtagHelper.setChecked(false);
        assertThat(SystemProperties.get(mMemtagProperty)).isEqualTo("none");
    }

    @Test
    public void getAvailabilityStatus_isForcedOff_isDISABLED_DEPENDENT_SETTING() {
        ShadowSystemProperties.override(mDeviceConfigOverride, "force_off");
        ShadowSystemProperties.override(mMemtagSupportedProperty, "true");
        assertThat(MemtagHelper.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_isForcedOn_isDISABLED_DEPENDENT_SETTING() {
        ShadowSystemProperties.override(mDeviceConfigOverride, "force_on");
        ShadowSystemProperties.override(mMemtagSupportedProperty, "true");
        assertThat(MemtagHelper.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_isUnsupported_isUNSUPPORTED_ON_DEVICE() {
        ShadowSystemProperties.override(mDeviceConfigOverride, "");
        ShadowSystemProperties.override(mMemtagSupportedProperty, "false");
        assertThat(MemtagHelper.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_isSupported_isAVAILABLE() {
        ShadowSystemProperties.override(mMemtagSupportedProperty, "true");
        assertThat(MemtagHelper.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @Config(shadows = {ZygoteShadow.class})
    public void IsOn_zygoteSupportsMemoryTagging_isTrue() {
        ZygoteShadow.setSupportsMemoryTagging(true);
        assertThat(MemtagHelper.isOn()).isTrue();
    }

    @Test
    @Config(shadows = {ZygoteShadow.class})
    public void IsOn_noZygoteSupportsMemoryTagging_isFalse() {
        ZygoteShadow.setSupportsMemoryTagging(false);
        assertThat(MemtagHelper.isOn()).isFalse();
    }

    @Test
    @Config(shadows = {ZygoteShadow.class})
    public void getSummary_memtagAndZygoteSupportsMemoryTagging_memtag_on() {
        ZygoteShadow.setSupportsMemoryTagging(true);
        ShadowSystemProperties.override(mDeviceConfigOverride, "");
        ShadowSystemProperties.override(mMemtagProperty, "memtag");
        assertThat(MemtagHelper.getSummary()).isEqualTo(R.string.memtag_on);
    }

    @Test
    @Config(shadows = {ZygoteShadow.class})
    public void getSummary_noMemtagAndZygoteSupportsMemoryTagging_memtag_off_pending() {
        ZygoteShadow.setSupportsMemoryTagging(true);
        ShadowSystemProperties.override(mDeviceConfigOverride, "");
        ShadowSystemProperties.override(mMemtagProperty, "");
        assertThat(MemtagHelper.getSummary()).isEqualTo(R.string.memtag_off_pending);
    }

    @Test
    @Config(shadows = {ZygoteShadow.class})
    public void getSummary_noMemtagAndNoZygoteSupportsMemoryTagging_memtag_off() {
        ZygoteShadow.setSupportsMemoryTagging(false);
        ShadowSystemProperties.override(mDeviceConfigOverride, "");
        ShadowSystemProperties.override(mMemtagProperty, "");
        assertThat(MemtagHelper.getSummary()).isEqualTo(R.string.memtag_off);
    }

    @Test
    @Config(shadows = {ZygoteShadow.class})
    public void getSummary_memtagAndNoZygoteSupportsMemoryTagging_memtag_on_pending() {
        ZygoteShadow.setSupportsMemoryTagging(false);
        ShadowSystemProperties.override(mDeviceConfigOverride, "");
        ShadowSystemProperties.override(mMemtagProperty, "memtag");
        assertThat(MemtagHelper.getSummary()).isEqualTo(R.string.memtag_on_pending);
    }

    @Test
    @Config(shadows = {ZygoteShadow.class})
    public void getSummary_forceOffOverride_memtag_force_off() {
        ZygoteShadow.setSupportsMemoryTagging(false);
        ShadowSystemProperties.override(mDeviceConfigOverride, "force_off");
        ShadowSystemProperties.override(mMemtagProperty, "memtag");
        assertThat(MemtagHelper.getSummary()).isEqualTo(R.string.memtag_force_off);
    }

    @Test
    @Config(shadows = {ZygoteShadow.class})
    public void getSummary_forceOffOverride_memtag_force_on() {
        ZygoteShadow.setSupportsMemoryTagging(false);
        ShadowSystemProperties.override(mDeviceConfigOverride, "force_on");
        ShadowSystemProperties.override(mMemtagProperty, "memtag");
        assertThat(MemtagHelper.getSummary()).isEqualTo(R.string.memtag_force_on);
    }

    @Test
    public void isForcedOn_forceOnOverride_isTrue() {
        ShadowSystemProperties.override(mDeviceConfigOverride, "force_on");
        assertThat(MemtagHelper.isForcedOn()).isTrue();
    }

    @Test
    public void isForcedOff_forceOffOverride_isTrue() {
        ShadowSystemProperties.override(mDeviceConfigOverride, "force_off");
        assertThat(MemtagHelper.isForcedOff()).isTrue();
    }
}
