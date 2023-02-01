/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.widget.CardPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ManageStoragePreferenceControllerTest {

    private ManageStoragePreferenceController mController;
    private CardPreference mPreference;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mPreference = new CardPreference(context);
        mController = new ManageStoragePreferenceController(context, "free_up_space");
    }

    @Test
    public void handPreferenceTreeClick_keyMatched_consumeClickEvent() {
        mPreference.setKey(mController.getPreferenceKey());

        assertTrue(mController.handlePreferenceTreeClick(mPreference));
    }

    @Test
    public void handPreferenceTreeClick_keyNotMatched_notConsumeClickEvent() {
        mPreference.setKey("not_matched_key");

        assertFalse(mController.handlePreferenceTreeClick(mPreference));
    }
}
