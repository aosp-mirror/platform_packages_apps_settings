/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class KeyboardSettingsFeatureProviderImplTest {

    private Context mContext;
    private KeyboardSettingsFeatureProviderImpl mFeatureProvider;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureProvider = new KeyboardSettingsFeatureProviderImpl();
    }

    @Test
    public void supportsFirmwareUpdate_defaultValue_returnsFalse() {
        assertThat(mFeatureProvider.supportsFirmwareUpdate()).isFalse();
    }

    @Test
    public void getActionKeyIcon_defaultValue_returnsNull() {
        assertThat(mFeatureProvider.getActionKeyIcon(mContext)).isNull();
    }
}
