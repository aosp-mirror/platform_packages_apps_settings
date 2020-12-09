/*
 * Copyright (C) 2020 The Android Open Source Project
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
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link PreferredShortcuts} */
@RunWith(AndroidJUnit4.class)
public class PreferredShortcutsTest {

    private static final String PACKAGE_NAME_1 = "com.test1.example";
    private static final String CLASS_NAME_1 = PACKAGE_NAME_1 + ".test1";
    private static final ComponentName COMPONENT_NAME_1 = new ComponentName(PACKAGE_NAME_1,
            CLASS_NAME_1);
    private static final String PACKAGE_NAME_2 = "com.test2.example";
    private static final String CLASS_NAME_2 = PACKAGE_NAME_2 + ".test2";
    private static final ComponentName COMPONENT_NAME_2 = new ComponentName(PACKAGE_NAME_2,
            CLASS_NAME_2);

    private Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void retrieveUserShortcutType_fromSingleData_matchSavedType() {
        final int type = 1;
        final PreferredShortcut shortcut = new PreferredShortcut(COMPONENT_NAME_1.flattenToString(),
                type);

        PreferredShortcuts.saveUserShortcutType(mContext, shortcut);
        final int retrieveType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                COMPONENT_NAME_1.flattenToString(), 0);

        assertThat(retrieveType).isEqualTo(type);
    }

    @Test
    public void retrieveUserShortcutType_fromMultiData_matchSavedType() {
        final int type1 = 1;
        final int type2 = 2;
        final PreferredShortcut shortcut1 = new PreferredShortcut(
                COMPONENT_NAME_1.flattenToString(), type1);
        final PreferredShortcut shortcut2 = new PreferredShortcut(
                COMPONENT_NAME_2.flattenToString(), type2);

        PreferredShortcuts.saveUserShortcutType(mContext, shortcut1);
        PreferredShortcuts.saveUserShortcutType(mContext, shortcut2);
        final int retrieveType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                COMPONENT_NAME_1.flattenToString(), 0);

        assertThat(retrieveType).isEqualTo(type1);
    }
}
