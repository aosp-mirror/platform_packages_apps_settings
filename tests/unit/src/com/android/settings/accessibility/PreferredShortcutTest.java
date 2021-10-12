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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link PreferredShortcut} */
@RunWith(AndroidJUnit4.class)
public class PreferredShortcutTest {

    private static final String STUB_COMPONENT_NAME = new ComponentName("com.example",
            "com.example.testActivity").flattenToString();
    private static final int STUB_TYPE = 3;

    @Test
    public void fromString_matchMemberObject() {
        final String preferredShortcutString = STUB_COMPONENT_NAME + ":" + STUB_TYPE;

        final PreferredShortcut shortcut = PreferredShortcut.fromString(preferredShortcutString);

        assertThat(shortcut.getComponentName()).isEqualTo(STUB_COMPONENT_NAME);
        assertThat(shortcut.getType()).isEqualTo(STUB_TYPE);
    }

    @Test
    public void toString_matchString() {
        final PreferredShortcut shortcut = new PreferredShortcut(STUB_COMPONENT_NAME, STUB_TYPE);

        final String preferredShortcutString = shortcut.toString();

        assertThat(preferredShortcutString).isEqualTo(STUB_COMPONENT_NAME + ":" + STUB_TYPE);
    }

    @Test
    public void assertSameObject() {
        final String preferredShortcutString = STUB_COMPONENT_NAME + ":" + STUB_TYPE;
        final PreferredShortcut targetShortcut = PreferredShortcut.fromString(
                preferredShortcutString);

        assertThat(targetShortcut).isEqualTo(new PreferredShortcut(STUB_COMPONENT_NAME, STUB_TYPE));
    }
}
