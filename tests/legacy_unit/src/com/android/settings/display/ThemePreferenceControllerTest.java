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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContextWrapper;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.preference.ListPreference;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ThemePreferenceControllerTest {

    private IOverlayManager mMockOverlayManager;
    private ContextWrapper mContext;
    private ThemePreferenceController mPreferenceController;
    private PackageManager mMockPackageManager;

    @Before
    public void setup() {
        mMockOverlayManager = mock(IOverlayManager.class);
        mMockPackageManager = mock(PackageManager.class);
        mContext = new ContextWrapper(InstrumentationRegistry.getTargetContext()) {
            @Override
            public PackageManager getPackageManager() {
                return mMockPackageManager;
            }
        };
        mPreferenceController = new ThemePreferenceController(mContext, mMockOverlayManager);
    }

    @Test
    public void testUpdateState() throws Exception {
        OverlayInfo info1 = new OverlayInfo("com.android.Theme1", "android", "",
                OverlayInfo.CATEGORY_THEME, "", OverlayInfo.STATE_ENABLED, 0, 0, true);
        OverlayInfo info2 = new OverlayInfo("com.android.Theme2", "android", "",
                OverlayInfo.CATEGORY_THEME, "", 0, 0, 0, true);
        when(mMockPackageManager.getApplicationInfo(any(), anyInt())).thenAnswer(inv -> {
            ApplicationInfo info = mock(ApplicationInfo.class);
            if ("com.android.Theme1".equals(inv.getArguments()[0])) {
                when(info.loadLabel(any())).thenReturn("Theme1");
            } else {
                when(info.loadLabel(any())).thenReturn("Theme2");
            }
            return info;
        });
        when(mMockPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(
                new PackageInfo());
        when(mMockOverlayManager.getOverlayInfosForTarget(any(), anyInt())).thenReturn(
                list(info1, info2));
        ListPreference pref = mock(ListPreference.class);
        mPreferenceController.updateState(pref);
        ArgumentCaptor<String[]> arg = ArgumentCaptor.forClass(String[].class);
        verify(pref).setEntries(arg.capture());


        CharSequence[] entries = arg.getValue();
        assertThat(entries).asList().containsExactly("Theme1", "Theme2");

        verify(pref).setEntryValues(arg.capture());
        CharSequence[] entryValues = arg.getValue();
        assertThat(entryValues).asList().containsExactly(
                "com.android.Theme1", "com.android.Theme2");

        verify(pref).setValue(eq("com.android.Theme1"));
    }

    @Test
    public void testUpdateState_withStaticOverlay() throws Exception {
        OverlayInfo info1 = new OverlayInfo("com.android.Theme1", "android", "",
                OverlayInfo.CATEGORY_THEME, "", OverlayInfo.STATE_ENABLED, 0, 0, true);
        OverlayInfo info2 = new OverlayInfo("com.android.Theme2", "android", "",
                OverlayInfo.CATEGORY_THEME, "", OverlayInfo.STATE_ENABLED, 0, 0, true);
        when(mMockPackageManager.getApplicationInfo(any(), anyInt())).thenAnswer(inv -> {
            ApplicationInfo info = mock(ApplicationInfo.class);
            if ("com.android.Theme1".equals(inv.getArguments()[0])) {
                when(info.loadLabel(any())).thenReturn("Theme1");
            } else {
                when(info.loadLabel(any())).thenReturn("Theme2");
            }
            return info;
        });
        PackageInfo pi = mock(PackageInfo.class);
        when(pi.isStaticOverlayPackage()).thenReturn(true);
        when(mMockPackageManager.getPackageInfo(eq("com.android.Theme1"), anyInt())).thenReturn(pi);
        when(mMockPackageManager.getPackageInfo(eq("com.android.Theme2"), anyInt())).thenReturn(
                new PackageInfo());
        when(mMockOverlayManager.getOverlayInfosForTarget(any(), anyInt())).thenReturn(
                list(info1, info2));
        ListPreference pref = mock(ListPreference.class);
        mPreferenceController.updateState(pref);
        ArgumentCaptor<String[]> arg = ArgumentCaptor.forClass(String[].class);
        verify(pref).setEntries(arg.capture());


        CharSequence[] entries = arg.getValue();
        assertThat(entries).asList().containsExactly("Theme2");

        verify(pref).setEntryValues(arg.capture());
        CharSequence[] entryValues = arg.getValue();
        assertThat(entryValues).asList().containsExactly("com.android.Theme2");

        verify(pref).setValue(eq("com.android.Theme2"));
    }

    private ArrayList<OverlayInfo> list(OverlayInfo... infos) {
        ArrayList<OverlayInfo> list = new ArrayList<>();
        for (OverlayInfo info : infos) {
            list.add(info);
        }
        return list;
    }
}
