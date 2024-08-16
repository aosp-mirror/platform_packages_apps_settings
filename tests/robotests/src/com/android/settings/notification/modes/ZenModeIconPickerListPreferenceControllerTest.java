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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.widget.LayoutPreference;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeIconPickerListPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ZenModeIconPickerListPreferenceController mController;
    @Mock private PreferenceScreen mPreferenceScreen;
    private LayoutPreference mLayoutPreference;
    private RecyclerView mRecyclerView;
    @Mock private ZenModeIconPickerListPreferenceController.IconPickerListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.getApplication();

        mController = new ZenModeIconPickerListPreferenceController(
                RuntimeEnvironment.getApplication(), "icon_list", mListener,
                new TestIconOptionsProvider());

        mRecyclerView = new RecyclerView(mContext);
        mRecyclerView.setId(R.id.icon_list);
        mLayoutPreference = new LayoutPreference(mContext, mRecyclerView);
        when(mPreferenceScreen.findPreference(eq("icon_list"))).thenReturn(mLayoutPreference);
    }

    @Test
    public void displayPreference_loadsIcons() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(mRecyclerView.getAdapter()).isNotNull();
        assertThat(mRecyclerView.getAdapter().getItemCount()).isEqualTo(3);
    }

    @Test
    public void updateState_highlightsCurrentIcon() {
        ZenMode mode = new TestModeBuilder().setIconResId(R.drawable.ic_hearing).build();
        mController.displayPreference(mPreferenceScreen);

        mController.updateZenMode(mLayoutPreference, mode);

        assertThat(getItemViewAt(0).isSelected()).isFalse();
        assertThat(getItemViewAt(1).isSelected()).isFalse();
        assertThat(getItemViewAt(2).isSelected()).isTrue();
    }

    @Test
    public void performClick_onIconItem_notifiesListener() {
        mController.displayPreference(mPreferenceScreen);

        getItemViewAt(1).performClick();

        verify(mListener).onIconSelected(R.drawable.ic_info);
    }

    private View getItemViewAt(int position) {
        ViewGroup fakeParent = new FrameLayout(mContext);
        RecyclerView.ViewHolder viewHolder = mRecyclerView.getAdapter().onCreateViewHolder(
                fakeParent, 0);
        mRecyclerView.getAdapter().bindViewHolder(viewHolder, position);
        return viewHolder.itemView;
    }

    private static class TestIconOptionsProvider implements IconOptionsProvider {

        @Override
        @NonNull
        public ImmutableList<IconInfo> getIcons() {
            return ImmutableList.of(
                    new IconInfo(R.drawable.ic_android, "android"),
                    new IconInfo(R.drawable.ic_info, "info"),
                    new IconInfo(R.drawable.ic_hearing, "hearing"));
        }
    }
}
