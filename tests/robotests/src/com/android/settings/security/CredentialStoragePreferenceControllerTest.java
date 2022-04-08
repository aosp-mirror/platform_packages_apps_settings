/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowKeyStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowKeyStore.class)
public class CredentialStoragePreferenceControllerTest {

    private Context mContext;
    private CredentialStoragePreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new CredentialStoragePreferenceController(mContext);
        mPreference = new Preference(mContext);
    }

    @Test
    public void updateState_hardwareBacked_showHardwareSummary() {
        ShadowKeyStore.setHardwareBacked(true);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.credential_storage_type_hardware));
    }

    @Test
    public void updateState_hardwareBacked_showSoftwareSummary() {
        ShadowKeyStore.setHardwareBacked(false);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getText(R.string.credential_storage_type_software));
    }
}
