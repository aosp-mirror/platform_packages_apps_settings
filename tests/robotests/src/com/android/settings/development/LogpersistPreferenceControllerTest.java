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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import android.support.v7.preference.ListPreference;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * deprecated in favor of {@link LogPersistPreferenceControllerV2}
 */
@Deprecated
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LogpersistPreferenceControllerTest {

    private Lifecycle mLifecycle;

    @Mock
    private ListPreference mListPreference;

    private LogpersistPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle(() -> mLifecycle);
        mController
                = new LogpersistPreferenceController(RuntimeEnvironment.application, mLifecycle);
    }

    @Test
    public void testIsConfirmationDialogShowing() {
        assertThat(mController.isConfirmationDialogShowing()).isFalse();
        mController.showConfirmationDialog(mListPreference);
        assertThat(mController.isConfirmationDialogShowing()).isTrue();
        mController.dismissConfirmationDialog();
        assertThat(mController.isConfirmationDialogShowing()).isFalse();
    }
}
