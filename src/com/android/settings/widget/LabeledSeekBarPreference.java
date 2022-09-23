/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.Utils;

/**
 * A labeled {@link SeekBarPreference} with left and right text label, icon label, or both.
 *
 * <p>
 * The component provides the attribute usage below.
 * <attr name="textStart" format="reference" />
 * <attr name="textEnd" format="reference" />
 * <attr name="tickMark" format="reference" />
 * <attr name="iconStart" format="reference" />
 * <attr name="iconEnd" format="reference" />
 * <attr name="iconStartContentDescription" format="reference" />
 * <attr name="iconEndContentDescription" format="reference" />
 * </p>
 *
 * <p> If you set the attribute values {@code iconStartContentDescription} or {@code
 * iconEndContentDescription} from XML, you must also set the corresponding attributes {@code
 * iconStart} or {@code iconEnd}, otherwise throws an {@link IllegalArgumentException}.</p>
 */
public class LabeledSeekBarPreference extends SeekBarPreference {

    private final int mTextStartId;
    private final int mTextEndId;
    private final int mTickMarkId;
    private final int mIconStartId;
    private final int mIconEndId;
    private final int mIconStartContentDescriptionId;
    private final int mIconEndContentDescriptionId;
    private OnPreferenceChangeListener mStopListener;
    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener;

    public LabeledSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {

        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_labeled_slider);

        final TypedArray styledAttrs = context.obtainStyledAttributes(attrs,
                R.styleable.LabeledSeekBarPreference);
        mTextStartId = styledAttrs.getResourceId(
                R.styleable.LabeledSeekBarPreference_textStart, /* defValue= */ 0);
        mTextEndId = styledAttrs.getResourceId(
                R.styleable.LabeledSeekBarPreference_textEnd, /* defValue= */ 0);
        mTickMarkId = styledAttrs.getResourceId(
                R.styleable.LabeledSeekBarPreference_tickMark, /* defValue= */ 0);
        mIconStartId = styledAttrs.getResourceId(
                R.styleable.LabeledSeekBarPreference_iconStart, /* defValue= */ 0);
        mIconEndId = styledAttrs.getResourceId(
                R.styleable.LabeledSeekBarPreference_iconEnd, /* defValue= */ 0);

        mIconStartContentDescriptionId = styledAttrs.getResourceId(
                R.styleable.LabeledSeekBarPreference_iconStartContentDescription,
                /* defValue= */ 0);
        Preconditions.checkArgument(!(mIconStartContentDescriptionId != 0 && mIconStartId == 0),
                "The resource of the iconStart attribute may be invalid or not set, "
                        + "you should set the iconStart attribute and have the valid resource.");

        mIconEndContentDescriptionId = styledAttrs.getResourceId(
                R.styleable.LabeledSeekBarPreference_iconEndContentDescription,
                /* defValue= */ 0);
        Preconditions.checkArgument(!(mIconEndContentDescriptionId != 0 && mIconEndId == 0),
                "The resource of the iconEnd attribute may be invalid or not set, "
                        + "you should set the iconEnd attribute and have the valid resource.");
        styledAttrs.recycle();
    }

    public LabeledSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                androidx.preference.R.attr.seekBarPreferenceStyle,
                com.android.internal.R.attr.seekBarPreferenceStyle), 0);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
        boolean isSummaryVisible = false;
        if (summaryView != null) {
            isSummaryVisible = (summaryView.getVisibility() == View.VISIBLE);
        }
        final TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        if (titleView != null && !isSelectable() && isEnabled() && isSummaryVisible) {
            titleView.setTextColor(
                    Utils.getColorAttr(getContext(), android.R.attr.textColorPrimary));
        }

        final TextView startText = (TextView) holder.findViewById(android.R.id.text1);
        if (mTextStartId > 0) {
            startText.setText(mTextStartId);
        }

        final TextView endText = (TextView) holder.findViewById(android.R.id.text2);
        if (mTextEndId > 0) {
            endText.setText(mTextEndId);
        }

        final View labelFrame = holder.findViewById(R.id.label_frame);
        final boolean isValidTextResIdExist = mTextStartId > 0 || mTextEndId > 0;
        labelFrame.setVisibility(isValidTextResIdExist ? View.VISIBLE : View.GONE);

        final SeekBar seekBar = (SeekBar) holder.findViewById(com.android.internal.R.id.seekbar);
        if (mTickMarkId != 0) {
            final Drawable tickMark = getContext().getDrawable(mTickMarkId);
            seekBar.setTickMark(tickMark);
        }

        final ViewGroup iconStartFrame = (ViewGroup) holder.findViewById(R.id.icon_start_frame);
        final ImageView iconStartView = (ImageView) holder.findViewById(R.id.icon_start);
        updateIconStartIfNeeded(iconStartFrame, iconStartView, seekBar);

        final ViewGroup iconEndFrame = (ViewGroup) holder.findViewById(R.id.icon_end_frame);
        final ImageView iconEndView = (ImageView) holder.findViewById(R.id.icon_end);
        updateIconEndIfNeeded(iconEndFrame, iconEndView, seekBar);
    }

    public void setOnPreferenceChangeStopListener(OnPreferenceChangeListener listener) {
        mStopListener = listener;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);

        if (mSeekBarChangeListener != null) {
            mSeekBarChangeListener.onStartTrackingTouch(seekBar);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);

        if (mSeekBarChangeListener != null) {
            mSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);

        if (mSeekBarChangeListener != null) {
            mSeekBarChangeListener.onStopTrackingTouch(seekBar);
        }

        if (mStopListener != null) {
            mStopListener.onPreferenceChange(this, seekBar.getProgress());
        }

        // Need to update the icon enabled status
        notifyChanged();
    }

    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener seekBarChangeListener) {
        mSeekBarChangeListener = seekBarChangeListener;
    }

    private void updateIconStartIfNeeded(ViewGroup iconFrame, ImageView iconStart,
            SeekBar seekBar) {
        if (mIconStartId == 0) {
            return;
        }

        if (iconStart.getDrawable() == null) {
            iconStart.setImageResource(mIconStartId);
        }

        if (mIconStartContentDescriptionId != 0) {
            final String contentDescription =
                    iconFrame.getContext().getString(mIconStartContentDescriptionId);
            iconFrame.setContentDescription(contentDescription);
        }

        iconFrame.setOnClickListener((view) -> {
            final int progress = getProgress();
            if (progress > 0) {
                setProgress(progress - 1);
            }
        });

        iconFrame.setVisibility(View.VISIBLE);
        setIconViewAndFrameEnabled(iconStart, seekBar.getProgress() > 0);
    }

    private void updateIconEndIfNeeded(ViewGroup iconFrame, ImageView iconEnd, SeekBar seekBar) {
        if (mIconEndId == 0) {
            return;
        }

        if (iconEnd.getDrawable() == null) {
            iconEnd.setImageResource(mIconEndId);
        }

        if (mIconEndContentDescriptionId != 0) {
            final String contentDescription =
                    iconFrame.getContext().getString(mIconEndContentDescriptionId);
            iconFrame.setContentDescription(contentDescription);
        }

        iconFrame.setOnClickListener((view) -> {
            final int progress = getProgress();
            if (progress < getMax()) {
                setProgress(progress + 1);
            }
        });

        iconFrame.setVisibility(View.VISIBLE);
        setIconViewAndFrameEnabled(iconEnd, seekBar.getProgress() < seekBar.getMax());
    }

    private static void setIconViewAndFrameEnabled(View iconView, boolean enabled) {
        iconView.setEnabled(enabled);
        final ViewGroup iconFrame = (ViewGroup) iconView.getParent();
        iconFrame.setEnabled(enabled);
    }
}

