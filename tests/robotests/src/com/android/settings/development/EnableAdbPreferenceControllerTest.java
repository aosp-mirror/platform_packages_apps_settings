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

import android.support.v14.preference.SwitchPreference;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * @deprecated in favor of {@link AdbPreferenceController}
 */
@Deprecated
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class EnableAdbPreferenceControllerTest {

    @Mock
    private SwitchPreference mSwitchPreference;

    private EnableAdbPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController
                = new EnableAdbPreferenceController(RuntimeEnvironment.application);
    }

    @Test
    public void testIsConfirmationDialogShowing() {
        assertThat(mController.isConfirmationDialogShowing()).isFalse();
        mController.showConfirmationDialog(mSwitchPreference);
        assertThat(mController.isConfirmationDialogShowing()).isTrue();
        mController.dismissConfirmationDialog();
        assertThat(mController.isConfirmationDialogShowing()).isFalse();
    }
}
