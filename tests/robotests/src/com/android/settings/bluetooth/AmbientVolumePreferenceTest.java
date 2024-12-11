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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
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
import com.android.settingslib.bluetooth.AmbientVolumeUi;

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

    private static final int TEST_LEFT_VOLUME_LEVEL = 1;
    private static final int TEST_RIGHT_VOLUME_LEVEL = 2;
    private static final int TEST_UNIFIED_VOLUME_LEVEL = 3;
    private static final String KEY_UNIFIED_SLIDER = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_UNIFIED;
    private static final String KEY_LEFT_SLIDER = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_LEFT;
    private static final String KEY_RIGHT_SLIDER = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_RIGHT;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private AmbientVolumeUi.AmbientVolumeUiListener mListener;
    @Mock
    private View mItemView;

    private AmbientVolumePreference mPreference;
    private ImageView mExpandIcon;
    private ImageView mVolumeIcon;
    private final Map<Integer, BluetoothDevice> mSideToDeviceMap = new ArrayMap<>();

    @Before
    public void setUp() {
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new AmbientVolumePreference(mContext);
        mPreference.setKey(KEY_AMBIENT_VOLUME);
        mPreference.setListener(mListener);
        mPreference.setExpandable(true);
        mPreference.setMutable(true);
        preferenceScreen.addPreference(mPreference);

        prepareDevices();
        mPreference.setupSliders(mSideToDeviceMap);
        mPreference.getSliders().forEach((side, slider) -> {
            slider.setMin(0);
            slider.setMax(4);
            if (side == SIDE_LEFT) {
                slider.setKey(KEY_LEFT_SLIDER);
                slider.setProgress(TEST_LEFT_VOLUME_LEVEL);
            } else if (side == SIDE_RIGHT) {
                slider.setKey(KEY_RIGHT_SLIDER);
                slider.setProgress(TEST_RIGHT_VOLUME_LEVEL);
            } else {
                slider.setKey(KEY_UNIFIED_SLIDER);
                slider.setProgress(TEST_UNIFIED_VOLUME_LEVEL);
            }
        });

        mExpandIcon = new ImageView(mContext);
        mVolumeIcon = new ImageView(mContext);
        mVolumeIcon.setImageResource(com.android.settingslib.R.drawable.ic_ambient_volume);
        mVolumeIcon.setImageLevel(0);
        when(mItemView.requireViewById(R.id.expand_icon)).thenReturn(mExpandIcon);
        when(mItemView.requireViewById(com.android.internal.R.id.icon)).thenReturn(mVolumeIcon);
        when(mItemView.requireViewById(R.id.icon_frame)).thenReturn(mVolumeIcon);

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

    @Test
    public void setMutable_mutable_clickOnMuteIconChangeMuteState() {
        mPreference.setMutable(true);
        mPreference.setMuted(false);

        mVolumeIcon.callOnClick();

        assertThat(mPreference.isMuted()).isTrue();
    }

    @Test
    public void setMutable_notMutable_clickOnMuteIconWontChangeMuteState() {
        mPreference.setMutable(false);
        mPreference.setMuted(false);

        mVolumeIcon.callOnClick();

        assertThat(mPreference.isMuted()).isFalse();
    }

    @Test
    public void updateLayout_mute_volumeIconIsCorrect() {
        mPreference.setMuted(true);
        mPreference.updateLayout();

        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(0);
    }

    @Test
    public void updateLayout_unmuteAndExpanded_volumeIconIsCorrect() {
        mPreference.setMuted(false);
        mPreference.setExpanded(true);
        mPreference.updateLayout();

        int expectedLevel = calculateVolumeLevel(TEST_LEFT_VOLUME_LEVEL, TEST_RIGHT_VOLUME_LEVEL);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    public void updateLayout_unmuteAndNotExpanded_volumeIconIsCorrect() {
        mPreference.setMuted(false);
        mPreference.setExpanded(false);
        mPreference.updateLayout();

        int expectedLevel = calculateVolumeLevel(TEST_UNIFIED_VOLUME_LEVEL,
                TEST_UNIFIED_VOLUME_LEVEL);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    public void setSliderEnabled_expandedAndLeftIsDisabled_volumeIconIcCorrect() {
        mPreference.setExpanded(true);
        mPreference.setSliderEnabled(SIDE_LEFT, false);

        int expectedLevel = calculateVolumeLevel(0, TEST_RIGHT_VOLUME_LEVEL);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    public void setSliderValue_expandedAndLeftValueChanged_volumeIconIcCorrect() {
        mPreference.setExpanded(true);
        mPreference.setSliderValue(SIDE_LEFT, 4);

        int expectedLevel = calculateVolumeLevel(4, TEST_RIGHT_VOLUME_LEVEL);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    private int calculateVolumeLevel(int left, int right) {
        return left * 5 + right;
    }

    private void assertControlUiCorrect() {
        final boolean expanded = mPreference.isExpanded();
        Map<Integer, SeekBarPreference> sliders = mPreference.getSliders();
        assertThat(sliders.get(SIDE_UNIFIED).isVisible()).isEqualTo(!expanded);
        assertThat(sliders.get(SIDE_LEFT).isVisible()).isEqualTo(expanded);
        assertThat(sliders.get(SIDE_RIGHT).isVisible()).isEqualTo(expanded);
        final float rotation = expanded ? ROTATION_EXPANDED : ROTATION_COLLAPSED;
        assertThat(mExpandIcon.getRotation()).isEqualTo(rotation);
    }

    private void prepareDevices() {
        mSideToDeviceMap.put(SIDE_LEFT, mock(BluetoothDevice.class));
        mSideToDeviceMap.put(SIDE_RIGHT, mock(BluetoothDevice.class));
    }
}
