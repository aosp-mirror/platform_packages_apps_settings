/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RestrictedSettingsFragmentTest {

    @Mock
    private AlertDialog mAlertDialog;
    private RestrictedSettingsFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onActivityResult_dismissesDialogOnOk() {
        mFragment = new RestrictedSettingsFragment("fake_key") {
            @Override
            public int getMetricsCategory() {
                return 0;
            }
        };
        doReturn(true).when(mAlertDialog).isShowing();
        mFragment.mActionDisabledDialog = mAlertDialog;
        mFragment.onActivityResult(RestrictedSettingsFragment.REQUEST_PIN_CHALLENGE,
                Activity.RESULT_OK,
                null);

        // dialog should be gone
        verify(mAlertDialog, times(1)).setOnDismissListener(isNull());
        verify(mAlertDialog, times(1)).dismiss();
    }
}
