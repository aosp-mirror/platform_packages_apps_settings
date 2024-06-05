/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.widget.mainswitch.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SettingsMainSwitchBarTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final SettingsMainSwitchBar mMainSwitchBar = new SettingsMainSwitchBar(mContext);

    private final TextView mTitle = mMainSwitchBar.findViewById(R.id.switch_text);

    private final CompoundButton mSwitchWidget =
            mMainSwitchBar.findViewById(android.R.id.switch_widget);

    @Test
    public void disabledByAdmin_shouldBeDisabled() {
        mMainSwitchBar.setDisabledByAdmin(new RestrictedLockUtils.EnforcedAdmin());

        assertThat(mTitle.isEnabled()).isFalse();
        assertThat(mSwitchWidget.isEnabled()).isFalse();
    }

    @Test
    public void disabledByAdmin_setNull_shouldBeEnabled() {
        mMainSwitchBar.setDisabledByAdmin(null);

        assertThat(mTitle.isEnabled()).isTrue();
        assertThat(mSwitchWidget.isEnabled()).isTrue();
    }
}
