/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.content.Context;
import android.net.Uri;

import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.widget.LayoutPreference;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ZenModeIconPickerListPreferenceControllerTest {

    private static final ZenMode ZEN_MODE = new ZenMode(
            "mode_id",
            new AutomaticZenRule.Builder("mode name", Uri.parse("mode")).build(),
            /* isActive= */ false);

    private ZenModesBackend mBackend;
    private ZenModeIconPickerListPreferenceController mController;
    private PreferenceScreen mPreferenceScreen;
    private RecyclerView mRecyclerView;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        mBackend = mock(ZenModesBackend.class);

        DashboardFragment fragment = mock(DashboardFragment.class);
        mController = new ZenModeIconPickerListPreferenceController(
                RuntimeEnvironment.getApplication(), "icon_list", fragment,
                new TestIconOptionsProvider(), mBackend);

        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setId(R.id.icon_list);
        LayoutPreference layoutPreference = new LayoutPreference(context, mRecyclerView);
        mPreferenceScreen = mock(PreferenceScreen.class);
        when(mPreferenceScreen.findPreference(eq("icon_list"))).thenReturn(layoutPreference);
    }

    @Test
    public void displayPreference_loadsIcons() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mRecyclerView.getAdapter()).isNotNull();
        assertThat(mRecyclerView.getAdapter().getItemCount()).isEqualTo(3);
    }

    @Test
    public void selectIcon_updatesMode() {
        mController.setZenMode(ZEN_MODE);

        mController.onIconSelected(R.drawable.ic_android);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getRule().getIconResId()).isEqualTo(R.drawable.ic_android);
    }

    private static class TestIconOptionsProvider implements IconOptionsProvider {

        @Override
        public ImmutableList<IconInfo> getIcons() {
            return ImmutableList.of(
                    new IconInfo(R.drawable.ic_android, "android"),
                    new IconInfo(R.drawable.ic_info, "info"),
                    new IconInfo(R.drawable.ic_hearing, "hearing"));
        }
    }
}
