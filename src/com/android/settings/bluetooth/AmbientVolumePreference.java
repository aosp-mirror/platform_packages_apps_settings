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
import static android.view.View.VISIBLE;

import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;

import android.content.Context;
import android.util.ArrayMap;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;

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
    };

    static final float ROTATION_COLLAPSED = 0f;
    static final float ROTATION_EXPANDED = 180f;
    static final int SIDE_UNIFIED = 999;
    static final List<Integer> VALID_SIDES = List.of(SIDE_UNIFIED, SIDE_LEFT, SIDE_RIGHT);

    @Nullable
    private OnIconClickListener mListener;
    @Nullable
    private View mExpandIcon;
    private boolean mExpandable = true;
    private boolean mExpanded = false;
    private Map<Integer, SeekBarPreference> mSideToSliderMap = new ArrayMap<>();

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
}
