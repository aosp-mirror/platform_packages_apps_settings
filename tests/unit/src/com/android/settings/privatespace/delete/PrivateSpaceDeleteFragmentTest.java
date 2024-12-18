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

package com.android.settings.privatespace.delete;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.app.settings.SettingsEnums;
import android.os.Flags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PrivateSpaceDeleteFragmentTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private PrivateSpaceDeleteFragment mFragment;

    @Test
    @UiThreadTest
    public void verifyMetricsConstant() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mFragment = spy(new PrivateSpaceDeleteFragment());
        assertThat(mFragment.getMetricsCategory()).isEqualTo(SettingsEnums.PRIVATE_SPACE_SETTINGS);
    }
}
