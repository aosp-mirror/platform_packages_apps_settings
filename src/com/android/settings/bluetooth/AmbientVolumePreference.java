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

import static android.view.View.GONE;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static android.view.View.VISIBLE;

import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME_SLIDER;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.bluetooth.AmbientVolumeUi;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.Ints;

import java.util.Map;

/**
 * A preference group of ambient volume controls.
 *
 * <p> It consists of a header with an expand icon and volume sliders for unified control and
 * separated control for devices in the same set. Toggle the expand icon will make the UI switch
 * between unified and separated control.
 */
public class AmbientVolumePreference extends PreferenceGroup implements AmbientVolumeUi {

    private static final int ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED = 0;
    private static final int ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED = 1;

    @Nullable
    private AmbientVolumeUiListener mListener;
    @Nullable
    private View mExpandIcon;
    @Nullable
    private ImageView mVolumeIcon;
    private boolean mExpandable = true;
    private boolean mExpanded = false;
    private boolean mMutable = false;
    private boolean mMuted = false;
    private final BiMap<Integer, SeekBarPreference> mSideToSliderMap = HashBiMap.create();
    private int mVolumeLevel = AMBIENT_VOLUME_LEVEL_DEFAULT;

    private final OnPreferenceChangeListener mPreferenceChangeListener =
            (slider, v) -> {
                if (slider instanceof SeekBarPreference && v instanceof final Integer value) {
                    final Integer side = mSideToSliderMap.inverse().get(slider);
                    if (mListener != null && side != null) {
                        mListener.onSliderValueChange(side, value);
                    }
                    return true;
                }
                return false;
            };

    public AmbientVolumePreference(@NonNull Context context) {
        super(context, null);
        setLayoutResource(R.layout.preference_ambient_volume);
        setIcon(com.android.settingslib.R.drawable.ic_ambient_volume);
        setTitle(R.string.bluetooth_ambient_volume_control);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);

        mVolumeIcon = holder.itemView.requireViewById(com.android.internal.R.id.icon);
        mVolumeIcon.getDrawable().mutate().setTint(getContext().getColor(
                com.android.internal.R.color.materialColorOnPrimaryContainer));
        final View iconView = holder.itemView.requireViewById(R.id.icon_frame);
        iconView.setOnClickListener(v -> {
            if (!mMutable) {
                return;
            }
            setMuted(!mMuted);
            if (mListener != null) {
                mListener.onAmbientVolumeIconClick();
            }
        });
        updateVolumeIcon();

        mExpandIcon = holder.itemView.requireViewById(R.id.expand_icon);
        mExpandIcon.setOnClickListener(v -> {
            setExpanded(!mExpanded);
            if (mListener != null) {
                mListener.onExpandIconClick();
            }
        });
        updateExpandIcon();
    }

    @Override
    public void setExpandable(boolean expandable) {
        mExpandable = expandable;
        if (!mExpandable) {
            setExpanded(false);
        }
        updateExpandIcon();
    }

    @Override
    public boolean isExpandable() {
        return mExpandable;
    }

    @Override
    public void setExpanded(boolean expanded) {
        if (!mExpandable && expanded) {
            return;
        }
        mExpanded = expanded;
        updateExpandIcon();
        updateLayout();
    }

    @Override
    public boolean isExpanded() {
        return mExpanded;
    }

    @Override
    public void setMutable(boolean mutable) {
        mMutable = mutable;
        if (!mMutable) {
            mVolumeLevel = AMBIENT_VOLUME_LEVEL_DEFAULT;
            setMuted(false);
        }
        updateVolumeIcon();
    }

    @Override
    public boolean isMutable() {
        return mMutable;
    }

    @Override
    public void setMuted(boolean muted) {
        if (!mMutable && muted) {
            return;
        }
        mMuted = muted;
        if (mMutable && mMuted) {
            for (SeekBarPreference slider : mSideToSliderMap.values()) {
                slider.setProgress(slider.getMin());
            }
        }
        updateVolumeIcon();
    }

    @Override
    public boolean isMuted() {
        return mMuted;
    }

    @Override
    public void setListener(@Nullable AmbientVolumeUiListener listener) {
        mListener = listener;
    }

    @Override
    public void setupSliders(@NonNull Map<Integer, BluetoothDevice> sideToDeviceMap) {
        sideToDeviceMap.forEach((side, device) ->
                createSlider(side, ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED + side));
        createSlider(SIDE_UNIFIED, ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED);

        if (!mSideToSliderMap.isEmpty()) {
            for (int side : VALID_SIDES) {
                final SeekBarPreference slider = mSideToSliderMap.get(side);
                if (slider != null && findPreference(slider.getKey()) == null) {
                    addPreference(slider);
                }
            }
        }
        updateLayout();
    }

    @Override
    public void setSliderEnabled(int side, boolean enabled) {
        SeekBarPreference slider = mSideToSliderMap.get(side);
        if (slider != null && slider.isEnabled() != enabled) {
            slider.setEnabled(enabled);
            updateLayout();
        }
    }

    @Override
    public void setSliderValue(int side, int value) {
        SeekBarPreference slider = mSideToSliderMap.get(side);
        if (slider != null && slider.getProgress() != value) {
            slider.setProgress(value);
            updateVolumeLevel();
        }
    }

    @Override
    public void setSliderRange(int side, int min, int max) {
        SeekBarPreference slider = mSideToSliderMap.get(side);
        if (slider != null) {
            slider.setMin(min);
            slider.setMax(max);
        }
    }

    @Override
    public void updateLayout() {
        mSideToSliderMap.forEach((side, slider) -> {
            if (side == SIDE_UNIFIED) {
                slider.setVisible(!mExpanded);
            } else {
                slider.setVisible(mExpanded);
            }
            if (!slider.isEnabled()) {
                slider.setProgress(slider.getMin());
            }
        });
        updateVolumeLevel();
    }

    private void updateVolumeLevel() {
        int leftLevel, rightLevel;
        if (mExpanded) {
            leftLevel = getVolumeLevel(SIDE_LEFT);
            rightLevel = getVolumeLevel(SIDE_RIGHT);
        } else {
            final int unifiedLevel = getVolumeLevel(SIDE_UNIFIED);
            leftLevel = unifiedLevel;
            rightLevel = unifiedLevel;
        }
        mVolumeLevel = Ints.constrainToRange(leftLevel * 5 + rightLevel,
                AMBIENT_VOLUME_LEVEL_MIN, AMBIENT_VOLUME_LEVEL_MAX);
        updateVolumeIcon();
    }

    private int getVolumeLevel(int side) {
        SeekBarPreference slider = mSideToSliderMap.get(side);
        if (slider == null || !slider.isEnabled()) {
            return 0;
        }
        final double min = slider.getMin();
        final double max = slider.getMax();
        final double levelGap = (max - min) / 4.0;
        final int value = slider.getProgress();
        return (int) Math.ceil((value - min) / levelGap);
    }

    private void updateExpandIcon() {
        if (mExpandIcon == null) {
            return;
        }
        mExpandIcon.setVisibility(mExpandable ? VISIBLE : GONE);
        mExpandIcon.setRotation(mExpanded ? ROTATION_EXPANDED : ROTATION_COLLAPSED);
        if (mExpandable) {
            final int stringRes = mExpanded ? R.string.bluetooth_ambient_volume_control_collapse
                    : R.string.bluetooth_ambient_volume_control_expand;
            mExpandIcon.setContentDescription(getContext().getString(stringRes));
        } else {
            mExpandIcon.setContentDescription(null);
        }
    }

    private void updateVolumeIcon() {
        if (mVolumeIcon == null) {
            return;
        }
        mVolumeIcon.setImageLevel(mMuted ? 0 : mVolumeLevel);
        if (mMutable) {
            final int stringRes = mMuted ? R.string.bluetooth_ambient_volume_unmute
                    : R.string.bluetooth_ambient_volume_mute;
            mVolumeIcon.setContentDescription(getContext().getString(stringRes));
            mVolumeIcon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }  else {
            mVolumeIcon.setContentDescription(null);
            mVolumeIcon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    private void createSlider(int side, int order) {
        if (mSideToSliderMap.containsKey(side)) {
            return;
        }
        SeekBarPreference slider = new SeekBarPreference(getContext());
        slider.setKey(KEY_AMBIENT_VOLUME_SLIDER + "_" + side);
        slider.setOrder(order);
        slider.setOnPreferenceChangeListener(mPreferenceChangeListener);
        if (side == SIDE_LEFT) {
            slider.setTitle(
                    getContext().getString(R.string.bluetooth_ambient_volume_control_left));
        } else if (side == SIDE_RIGHT) {
            slider.setTitle(
                    getContext().getString(R.string.bluetooth_ambient_volume_control_right));
        }
        mSideToSliderMap.put(side, slider);
    }

    @VisibleForTesting
    Map<Integer, SeekBarPreference> getSliders() {
        return mSideToSliderMap;
    }
}
