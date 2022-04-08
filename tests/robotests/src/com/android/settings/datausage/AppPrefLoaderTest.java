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

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppPrefLoaderTest {

    @Mock
    private PackageManager mPackageManager;

    private AppPrefLoader mLoader;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final ArraySet<String> pkgs = new ArraySet<>(2);
        pkgs.add("pkg0");
        pkgs.add("pkg1");
        mLoader = new AppPrefLoader(RuntimeEnvironment.application, pkgs, mPackageManager);
    }

    @Test
    public void loadInBackground_packageNotFound_shouldReturnEmptySet()
            throws NameNotFoundException {
        when(mPackageManager.getApplicationInfo(anyString(), anyInt()))
            .thenThrow(new NameNotFoundException());

        assertThat(mLoader.loadInBackground()).isEmpty();
    }

    @Test
    public void loadInBackground_shouldReturnPreference() throws NameNotFoundException {
        ApplicationInfo info = mock(ApplicationInfo.class);
        when(mPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(info);
        final Drawable drawable = mock(Drawable.class);
        final String label = "Label1";
        when(info.loadIcon(mPackageManager)).thenReturn(drawable);
        when(info.loadLabel(mPackageManager)).thenReturn(label);

        Preference preference = mLoader.loadInBackground().valueAt(0);
        assertThat(preference.getTitle()).isEqualTo(label);
        assertThat(preference.getIcon()).isEqualTo(drawable);
        assertThat(preference.isSelectable()).isFalse();
    }
}
