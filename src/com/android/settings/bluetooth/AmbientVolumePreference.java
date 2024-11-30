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

import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;

import android.content.Context;
import android.util.ArrayMap;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;

import com.google.common.primitives.Ints;

import java.util.List;
import java.util.Map;

/**
 * A preference group of ambient volume controls.
 *
 * <p> It consists of a header with an expand icon and volume sliders for unified control and
 * separated control for devices in the same set. Toggle the expand icon will make the UI switch
 * between unified and separated control.
 */
public class AmbientVolumePreference extends PreferenceGroup {

    /** Interface definition for a callback to be invoked when the icon is clicked. */
    public interface OnIconClickListener {
        /** Called when the expand icon is clicked. */
        void onExpandIconClick();

        /** Called when the ambient volume icon is clicked. */
        void onAmbientVolumeIconClick();
    };

    static final float ROTATION_COLLAPSED = 0f;
    static final float ROTATION_EXPANDED = 180f;
    static final int AMBIENT_VOLUME_LEVEL_MIN = 0;
    static final int AMBIENT_VOLUME_LEVEL_MAX = 24;
    static final int AMBIENT_VOLUME_LEVEL_DEFAULT = 24;
    static final int SIDE_UNIFIED = 999;
    static final List<Integer> VALID_SIDES = List.of(SIDE_UNIFIED, SIDE_LEFT, SIDE_RIGHT);

    @Nullable
    private OnIconClickListener mListener;
    @Nullable
    private View mExpandIcon;
    @Nullable
    private ImageView mVolumeIcon;
    private boolean mExpandable = true;
    private boolean mExpanded = false;
    private boolean mMutable = false;
    private boolean mMuted = false;
    private Map<Integer, SeekBarPreference> mSideToSliderMap = new ArrayMap<>();

    /**
     * Ambient volume level for hearing device ambient control icon
     * <p>
     * This icon visually represents the current ambient gain setting.
     * It displays separate levels for the left and right sides, each with 5 levels ranging from 0
     * to 4.
     * <p>
     * To represent the combined left/right levels with a single value, the following calculation
     * is used:
     *      finalLevel = (leftLevel * 5) + rightLevel
     * For example:
     * <ul>
     *    <li>If left level is 2 and right level is 3, the final level will be 13 (2 * 5 + 3)</li>
     *    <li>If both left and right levels are 0, the final level will be 0</li>
     *    <li>If both left and right levels are 4, the final level will be 24</li>
     * </ul>
     */
    private int mVolumeLevel = AMBIENT_VOLUME_LEVEL_DEFAULT;

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

    void setExpandable(boolean expandable) {
        mExpandable = expandable;
        if (!mExpandable) {
            setExpanded(false);
        }
        updateExpandIcon();
    }

    boolean isExpandable() {
        return mExpandable;
    }

    void setExpanded(boolean expanded) {
        if (!mExpandable && expanded) {
            return;
        }
        mExpanded = expanded;
        updateExpandIcon();
        updateLayout();
    }

    boolean isExpanded() {
        return mExpanded;
    }

    void setMutable(boolean mutable) {
        mMutable = mutable;
        if (!mMutable) {
            mVolumeLevel = AMBIENT_VOLUME_LEVEL_DEFAULT;
            setMuted(false);
        }
        updateVolumeIcon();
    }

    boolean isMutable() {
        return mMutable;
    }

    void setMuted(boolean muted) {
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

    boolean isMuted() {
        return mMuted;
    }

    void setOnIconClickListener(@Nullable OnIconClickListener listener) {
        mListener = listener;
    }

    void setSliders(Map<Integer, SeekBarPreference> sideToSliderMap) {
        mSideToSliderMap = sideToSliderMap;
        for (SeekBarPreference preference : sideToSliderMap.values()) {
            if (findPreference(preference.getKey()) == null) {
                addPreference(preference);
            }
        }
        updateLayout();
    }

    void setSliderEnabled(int side, boolean enabled) {
        SeekBarPreference slider = mSideToSliderMap.get(side);
        if (slider != null && slider.isEnabled() != enabled) {
            slider.setEnabled(enabled);
            updateLayout();
        }
    }

    void setSliderValue(int side, int value) {
        SeekBarPreference slider = mSideToSliderMap.get(side);
        if (slider != null && slider.getProgress() != value) {
            slider.setProgress(value);
            updateVolumeLevel();
        }
    }

    void setSliderRange(int side, int min, int max) {
        SeekBarPreference slider = mSideToSliderMap.get(side);
        if (slider != null) {
            slider.setMin(min);
            slider.setMax(max);
        }
    }

    void updateLayout() {
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
            final int stringRes = mExpanded
                    ? R.string.bluetooth_ambient_volume_control_collapse
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
            final int stringRes = mMuted
                    ? R.string.bluetooth_ambient_volume_unmute
                    : R.string.bluetooth_ambient_volume_mute;
            mVolumeIcon.setContentDescription(getContext().getString(stringRes));
            mVolumeIcon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }  else {
            mVolumeIcon.setContentDescription(null);
            mVolumeIcon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }
}
