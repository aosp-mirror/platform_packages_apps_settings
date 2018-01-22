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
package com.android.settings;

import android.app.Dialog;
import android.app.Fragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingsDialogFragmentTest {

    private static final int DIALOG_ID = 15;

    @Mock
    private DialogCreatableFragment mDialogCreatable;
    private SettingsPreferenceFragment.SettingsDialogFragment mDialogFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetMetrics_shouldGetMetricFromDialogCreatable() {
        when(mDialogCreatable.getDialogMetricsCategory(DIALOG_ID)).thenReturn(1);

        mDialogFragment =
                new SettingsPreferenceFragment.SettingsDialogFragment(mDialogCreatable, DIALOG_ID);
        mDialogFragment.getMetricsCategory();

        // getDialogMetricsCategory called in constructor, and explicitly in test.
        verify(mDialogCreatable, times(2)).getDialogMetricsCategory(DIALOG_ID);
    }

    @Test
    public void testGetInvalidMetricsValue_shouldCrash() {
        when(mDialogCreatable.getDialogMetricsCategory(DIALOG_ID)).thenReturn(-1);

        try {
            mDialogFragment = new SettingsPreferenceFragment.SettingsDialogFragment(
                    mDialogCreatable, DIALOG_ID);
        } catch (IllegalStateException e) {
            // getDialogMetricsCategory called in constructor
            verify(mDialogCreatable).getDialogMetricsCategory(DIALOG_ID);
            return;
        }
        fail("Should fail with IllegalStateException");
    }

    public static class DialogCreatableFragment extends Fragment implements DialogCreatable {

        @Override
        public Dialog onCreateDialog(int dialogId) {
            return null;
        }

        @Override
        public int getDialogMetricsCategory(int dialogId) {
            return 0;
        }
    }
}
