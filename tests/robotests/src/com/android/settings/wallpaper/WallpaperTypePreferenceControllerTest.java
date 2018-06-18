/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wallpaper;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.Preference;

@RunWith(SettingsRobolectricTestRunner.class)
public class WallpaperTypePreferenceControllerTest {

    @Mock
    private Fragment mFragment;

    private Context mContext;
    private Activity mActivity;
    private WallpaperTypePreferenceController mController;
    private Preference mPreference;
    private Intent mIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mActivity = spy(Robolectric.buildActivity(Activity.class).get());
        mController = new WallpaperTypePreferenceController(mContext, "pref_key");
        mController.setParentFragment(mFragment);
        mIntent = new Intent();
        mPreference = new Preference(mContext);
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldBeShown() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void testhandlePreferenceTreeClick_intentNull_shouldDoNothing() {
        mPreference.setIntent(null);

        final boolean handled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(handled).isFalse();
    }

    @Test
    public void testhandlePreferenceTreeClick_shouldLaunchIntent() {
        mPreference.setIntent(mIntent);
        doNothing().when(mFragment).startActivity(any(Intent.class));
        when(mFragment.getActivity()).thenReturn(mActivity);
        doNothing().when(mActivity).finish();

        final boolean handled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(handled).isTrue();
    }
}
