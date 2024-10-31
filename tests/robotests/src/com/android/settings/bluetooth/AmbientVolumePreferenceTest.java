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

package com.android.settings.bluetooth;

import static com.android.settings.bluetooth.AmbientVolumePreference.ROTATION_COLLAPSED;
import static com.android.settings.bluetooth.AmbientVolumePreference.ROTATION_EXPANDED;
import static com.android.settings.bluetooth.AmbientVolumePreference.SIDE_UNIFIED;
import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME;
import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME_SLIDER;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.util.ArrayMap;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

/** Tests for {@link AmbientVolumePreference}. */
@RunWith(RobolectricTestRunner.class)
public class AmbientVolumePreferenceTest {

    private static final String KEY_UNIFIED_SLIDER = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_UNIFIED;
    private static final String KEY_LEFT_SLIDER = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_LEFT;
    private static final String KEY_RIGHT_SLIDER = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_RIGHT;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private AmbientVolumePreference.OnIconClickListener mListener;
    @Mock
    private View mItemView;

    private AmbientVolumePreference mPreference;
    private ImageView mExpandIcon;
    private final Map<Integer, SeekBarPreference> mSideToSlidersMap = new ArrayMap<>();

    @Before
    public void setUp() {
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new AmbientVolumePreference(mContext);
        mPreference.setKey(KEY_AMBIENT_VOLUME);
        mPreference.setOnIconClickListener(mListener);
        mPreference.setExpandable(true);
        preferenceScreen.addPreference(mPreference);

        prepareSliders();
        mPreference.setSliders(mSideToSlidersMap);

        mExpandIcon = new ImageView(mContext);
        when(mItemView.requireViewById(R.id.expand_icon)).thenReturn(mExpandIcon);

        PreferenceViewHolder preferenceViewHolder = PreferenceViewHolder.createInstanceForTests(
                mItemView);
        mPreference.onBindViewHolder(preferenceViewHolder);
    }

    @Test
    public void setExpandable_expandable_expandIconVisible() {
        mPreference.setExpandable(true);

        assertThat(mExpandIcon.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void setExpandable_notExpandable_expandIconGone() {
        mPreference.setExpandable(false);

        assertThat(mExpandIcon.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void setExpanded_expanded_assertControlUiCorrect() {
        mPreference.setExpanded(true);

        assertControlUiCorrect();
    }

    @Test
    public void setExpanded_notExpanded_assertControlUiCorrect() {
        mPreference.setExpanded(false);

        assertControlUiCorrect();
    }

    private void assertControlUiCorrect() {
        final boolean expanded = mPreference.isExpanded();
        assertThat(mSideToSlidersMap.get(SIDE_UNIFIED).isVisible()).isEqualTo(!expanded);
        assertThat(mSideToSlidersMap.get(SIDE_LEFT).isVisible()).isEqualTo(expanded);
        assertThat(mSideToSlidersMap.get(SIDE_RIGHT).isVisible()).isEqualTo(expanded);
        final float rotation = expanded ? ROTATION_EXPANDED : ROTATION_COLLAPSED;
        assertThat(mExpandIcon.getRotation()).isEqualTo(rotation);
    }

    private void prepareSliders() {
        prepareSlider(SIDE_UNIFIED);
        prepareSlider(SIDE_LEFT);
        prepareSlider(SIDE_RIGHT);
    }

    private void prepareSlider(int side) {
        SeekBarPreference slider = new SeekBarPreference(mContext);
        if (side == SIDE_LEFT) {
            slider.setKey(KEY_LEFT_SLIDER);
        } else if (side == SIDE_RIGHT) {
            slider.setKey(KEY_RIGHT_SLIDER);
        } else {
            slider.setKey(KEY_UNIFIED_SLIDER);
        }
        mSideToSlidersMap.put(side, slider);
    }
}
