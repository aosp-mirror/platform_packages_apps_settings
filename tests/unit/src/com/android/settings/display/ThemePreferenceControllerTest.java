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

package com.android.settings.core;

import static junit.framework.TestCase.assertNotNull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiModeManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.preference.ListPreference;

import com.android.settings.R;
import com.android.settings.display.ThemePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ThemePreferenceControllerTest {

    private UiModeManager mMockUiModeManager;
    private ContextWrapper mContext;
    private ThemePreferenceController mPreferenceController;

    @Before
    public void setup() {
        mMockUiModeManager = mock(UiModeManager.class);
        mContext = new ContextWrapper(InstrumentationRegistry.getTargetContext()) {
            @Override
            public Object getSystemService(String name) {
                if (Context.UI_MODE_SERVICE.equals(name)) {
                    return mMockUiModeManager;
                }
                return super.getSystemService(name);
            }
        };
        mPreferenceController = new ThemePreferenceController(mContext);
    }

    @Test
    public void testUpdateState() {
        when(mMockUiModeManager.getAvailableThemes()).thenReturn(new String[] {
                null,
                "Theme1",
                "Theme2",
        });
        when(mMockUiModeManager.getTheme()).thenReturn("Theme1");
        ListPreference pref = mock(ListPreference.class);
        mPreferenceController.updateState(pref);
        ArgumentCaptor<String[]> arg = ArgumentCaptor.forClass(String[].class);
        verify(pref).setEntries(arg.capture());

        String[] entries = arg.getValue();
        assertEquals(3, entries.length);
        assertNotNull(entries[0]);
        assertEquals("Theme1", entries[1]);
        assertEquals("Theme2", entries[2]);

        verify(pref).setEntryValues(arg.capture());
        String[] entryValues = arg.getValue();
        assertEquals(3, entryValues.length);
        assertNotNull(entryValues[0]);
        assertEquals("Theme1", entryValues[1]);
        assertEquals("Theme2", entryValues[2]);

        verify(pref).setValue(eq("Theme1"));
    }

    @Test
    public void testAvailable_false() {
        when(mMockUiModeManager.getAvailableThemes()).thenReturn(new String[1]);
        assertFalse(mPreferenceController.isAvailable());
    }

    @Test
    public void testAvailable_true() {
        when(mMockUiModeManager.getAvailableThemes()).thenReturn(new String[2]);
        assertTrue(mPreferenceController.isAvailable());
    }
}
