/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.deviceinfo.storage;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.deviceinfo.StorageItemPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class StorageItemPreferenceControllerTest {
    private static final String KEY = "pref";
    private Context mContext;
    private StorageItemPreferenceController mController;
    private PreferenceViewHolder mHolder;
    private StorageItemPreferenceAlternate mPreference;

    @Before
    public void setUp() throws Exception {
        mContext = RuntimeEnvironment.application;
        mController = new StorageItemPreferenceController(mContext, KEY);
        mPreference = new StorageItemPreferenceAlternate(mContext);

        // Inflate the preference and the widget.
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(mContext), false);

        mHolder = new PreferenceViewHolder(view);
    }

    @Test
    public void testGetKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(KEY);
    }

    @Test
    public void testUpdateStateWithInitialState() {
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary().toString()).isEqualTo("Calculating…");
    }

    @Test
    public void testPreferenceShouldUpdateAfterPopulatingData() {
        mController.setStorageSize(1024L);
        mController.updateState(mPreference);
        assertThat(mPreference.getSummary().toString()).isEqualTo("1.00KB");

    }
}