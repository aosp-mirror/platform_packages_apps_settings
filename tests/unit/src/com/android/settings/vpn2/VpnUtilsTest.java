/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.vpn2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.VpnManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class VpnUtilsTest {
    @Test
    public void testIsAlwaysOnVpnSet() {
        final VpnManager vm = mock(VpnManager.class);
        when(vm.getAlwaysOnVpnPackageForUser(0)).thenReturn("com.example.vpn");
        assertThat(VpnUtils.isAlwaysOnVpnSet(vm, 0)).isTrue();

        when(vm.getAlwaysOnVpnPackageForUser(0)).thenReturn(null);
        assertThat(VpnUtils.isAlwaysOnVpnSet(vm, 0)).isFalse();
    }
}
