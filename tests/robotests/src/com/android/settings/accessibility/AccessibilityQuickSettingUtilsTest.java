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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityQuickSettingUtils}. */
@RunWith(RobolectricTestRunner.class)
public final class AccessibilityQuickSettingUtilsTest {
    private static final String DUMMY_PACKAGE_NAME = "com.mock.example";
    private static final String DUMMY_CLASS_NAME = DUMMY_PACKAGE_NAME + ".mock_a11y_service";
    private static final String DUMMY_CLASS_NAME2 = DUMMY_PACKAGE_NAME + ".mock_a11y_service2";
    private static final ComponentName DUMMY_COMPONENT_NAME = new ComponentName(DUMMY_PACKAGE_NAME,
            DUMMY_CLASS_NAME);
    private static final ComponentName DUMMY_COMPONENT_NAME2 = new ComponentName(DUMMY_PACKAGE_NAME,
            DUMMY_CLASS_NAME2);
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void optInValueToSharedPreferences_optInValue_haveMatchString() {
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(mContext,
                DUMMY_COMPONENT_NAME);

        assertThat(AccessibilityQuickSettingUtils.hasValueInSharedPreferences(mContext,
                DUMMY_COMPONENT_NAME)).isTrue();
    }

    @Test
    public void optInValueToSharedPreferences_optInTwoValues_haveMatchString() {
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(mContext,
                DUMMY_COMPONENT_NAME);
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(mContext,
                DUMMY_COMPONENT_NAME2);

        assertThat(AccessibilityQuickSettingUtils.hasValueInSharedPreferences(mContext,
                DUMMY_COMPONENT_NAME)).isTrue();
        assertThat(AccessibilityQuickSettingUtils.hasValueInSharedPreferences(mContext,
                DUMMY_COMPONENT_NAME2)).isTrue();
    }
}
