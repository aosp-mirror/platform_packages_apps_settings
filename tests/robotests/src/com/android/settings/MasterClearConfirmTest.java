/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.view.LayoutInflater;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MasterClearConfirmTest {
    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
    }

    @Test
    public void setSubtitle_eraseEsim() {
        MasterClearConfirm masterClearConfirm = new MasterClearConfirm();
        masterClearConfirm.mEraseEsims = true;
        masterClearConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.master_clear_confirm, null);

        masterClearConfirm.setSubtitle();

        assertThat(((TextView) masterClearConfirm.mContentView
                .findViewById(R.id.sud_layout_description)).getText())
                .isEqualTo(mActivity.getString(R.string.master_clear_final_desc_esim));
    }

    @Test
    public void setSubtitle_notEraseEsim() {
        MasterClearConfirm masterClearConfirm = new MasterClearConfirm();
        masterClearConfirm.mEraseEsims = false;
        masterClearConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.master_clear_confirm, null);

        masterClearConfirm.setSubtitle();

        assertThat(((TextView) masterClearConfirm.mContentView
                .findViewById(R.id.sud_layout_description)).getText())
                .isEqualTo(mActivity.getString(R.string.master_clear_final_desc));
    }
}
