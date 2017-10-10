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

package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ShowOperatorNamePreferenceControllerTest {

    private static final String KEY_SHOW_OPERATOR_NAME = "show_operator_name";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private SwitchPreference mPreference;

    private ShowOperatorNamePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new ShowOperatorNamePreferenceController(mContext);
    }

    @Test
    public void testIsAvailable_configIsTrue_ReturnTrue() {
        when(mContext.getResources().getBoolean(R.bool.config_showOperatorNameInStatusBar))
                .thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_configIsFalse_ReturnFalse() {
        when(mContext.getResources().getBoolean(R.bool.config_showOperatorNameInStatusBar))
                .thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testOnPreferenceChange_TurnOn_ReturnOn() {
        mController.onPreferenceChange(mPreference, true);

        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                KEY_SHOW_OPERATOR_NAME, 0);
        assertThat(mode).isEqualTo(1);
    }

    @Test
    public void testOnPreferenceChange_TurnOff_ReturnOff() {
        mController.onPreferenceChange(mPreference, false);

        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                KEY_SHOW_OPERATOR_NAME, 1);
        assertThat(mode).isEqualTo(0);
    }
}
