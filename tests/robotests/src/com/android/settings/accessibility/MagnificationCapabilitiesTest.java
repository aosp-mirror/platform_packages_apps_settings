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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.database.ContentObserver;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MagnificationCapabilities} */
@RunWith(RobolectricTestRunner.class)
public final class MagnificationCapabilitiesTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void getCapabilities_windowMode_expectedCapabilities() {
        MagnificationCapabilities.setCapabilities(mContext,
                MagnificationCapabilities.MagnificationMode.WINDOW);

        final int windowCapabilities = MagnificationCapabilities.getCapabilities(mContext);
        assertThat(windowCapabilities).isEqualTo(
                MagnificationCapabilities.MagnificationMode.WINDOW);
    }

    @Test
    public void getSummary_fullScreenMode_expectedSummary() {
        final int fullScreenCapabilities = MagnificationCapabilities.MagnificationMode.FULLSCREEN;

        final String actualString = MagnificationCapabilities.getSummary(mContext,
                fullScreenCapabilities);

        final String expectedString = mContext.getString(
                R.string.accessibility_magnification_area_settings_full_screen_summary);
        assertThat(actualString).isEqualTo(expectedString);
    }

    @Test
    public void getCapabilities_unset_defaultValue() {
        final int windowCapabilities = MagnificationCapabilities.getCapabilities(mContext);
        assertThat(windowCapabilities).isEqualTo(
                MagnificationCapabilities.MagnificationMode.FULLSCREEN);
    }

    @Test
    public void registerObserver_triggeredWhenCapabilitiesChanged() {
        MagnificationCapabilities.setCapabilities(mContext,
                MagnificationCapabilities.MagnificationMode.FULLSCREEN);

        ContentObserver contentObserver =
                spy(new ContentObserver(/* handler= */ null) {});

        MagnificationCapabilities.registerObserver(mContext, contentObserver);
        MagnificationCapabilities.setCapabilities(mContext,
                MagnificationCapabilities.MagnificationMode.WINDOW);

        verify(contentObserver).onChange(/* selfChange= */ anyBoolean(), /* uri= */ any());
    }

    @Test
    public void unregisterObserver_neverTriggeredWhenCapabilitiesChanged() {
        MagnificationCapabilities.setCapabilities(mContext,
                MagnificationCapabilities.MagnificationMode.FULLSCREEN);

        ContentObserver contentObserver =
                spy(new ContentObserver(/* handler= */ null) {});

        MagnificationCapabilities.registerObserver(mContext, contentObserver);
        MagnificationCapabilities.unregisterObserver(mContext, contentObserver);
        MagnificationCapabilities.setCapabilities(mContext,
                MagnificationCapabilities.MagnificationMode.WINDOW);

        verify(contentObserver, never()).onChange(/* selfChange= */ anyBoolean(), /* uri= */ any());
    }
}
