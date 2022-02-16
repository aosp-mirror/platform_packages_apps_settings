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
 * limitations under the License
 */

package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.settings.password.ConfirmDeviceCredentialBaseFragment.LastTryDialog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ConfirmCredentialTest {
    @Test
    public void testLastTryDialogShownExactlyOnce() {
        FragmentManager fm = Robolectric.buildActivity(FragmentActivity.class).
                setup().get().getSupportFragmentManager();

        // Launch only one instance at a time.
        assertThat(LastTryDialog.show(
                fm, "title", android.R.string.yes, android.R.string.ok, false)).isTrue();
        assertThat(LastTryDialog.show(
                fm, "title", android.R.string.yes, android.R.string.ok, false)).isFalse();

        // After cancelling, the dialog should be re-shown when asked for.
        LastTryDialog.hide(fm);
        assertThat(LastTryDialog.show(
                fm, "title", android.R.string.yes, android.R.string.ok, false)).isTrue();
    }
}
