/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.RestrictedPreference;

/**
 * Based on android.preference.SeekBarPreference, but uses support preference as base.
 */
public class SeekBarPreference extends RestrictedPreference
        implements OnSeekBarChangeListener, View.OnKeyListener {

    private int mProgress;
    private int mMax;
    private int mMin;
    private boolean mTrackingTouch;

    private boolean mContinuousUpdates;
    private int mDefaultProgress = -1;

    private SeekBar mSeekBar;
    private boolean mShouldBlink;
    private int mAccessibilityRangeInfoType = AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT;
    private CharSequence mSeekBarContentDescription;
    private CharSequence mSeekBarStateDescription;

    public SeekBarPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.ProgressBar, defStyleAttr, defStyleRes);
        setMax(a.getInt(com.android.internal.R.styleable.ProgressBar_max, mMax));
        setMin(a.getInt(com.android.internal.R.styleable.ProgressBar_min, mMin));
        a.recycle();

        a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.SeekBarPreference, defStyleAttr, defStyleRes);
        final int layoutResId = a.getResourceId(
                com.android.internal.R.styleable.SeekBarPreference_layout,
                com.android.internal.R.layout.preference_widget_seekbar);
        a.recycle();

        a = context.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.Preference, defStyleAttr, defStyleRes);
        final boolean isSelectable = a.getBoolean(
                com.android.settings.R.styleable.Preference_android_selectable, false);
        setSelectable(isSelectable);
        a.recycle();

        setLayoutResource(layoutResId);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                        androidx.preference.R.attr.seekBarPreferenceStyle,
                        com.android.internal.R.attr.seekBarPreferenceStyle));
    }

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    public void setShouldBlink(boolean shouldBlink) {
        mShouldBlink = shouldBlink;
        notifyChanged();
    }

    @Override
    public boolean isSelectable() {
        if(isDisabledByAdmin()) {
            return true;
        } else {
            return super.isSelectable();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        view.itemView.setOnKeyListener(this);
        mSeekBar = (SeekBar) view.findViewById(
                com.android.internal.R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setMax(mMax);
        mSeekBar.setMin(mMin);
        mSeekBar.setProgress(mProgress);
        mSeekBar.setEnabled(isEnabled());
        final CharSequence title = getTitle();
        if (!TextUtils.isEmpty(mSeekBarContentDescription)) {
            mSeekBar.setContentDescription(mSeekBarContentDescription);
        } else if (!TextUtils.isEmpty(title)) {
            mSeekBar.setContentDescription(title);
        }
        if (!TextUtils.isEmpty(mSeekBarStateDescription)) {
            mSeekBar.setStateDescription(mSeekBarStateDescription);
        }
        if (mSeekBar instanceof DefaultIndicatorSeekBar) {
            ((DefaultIndicatorSeekBar) mSeekBar).setDefaultProgress(mDefaultProgress);
        }
        if (mShouldBlink) {
            View v = view.itemView;
            v.post(() -> {
                if (v.getBackground() != null) {
                    final int centerX = v.getWidth() / 2;
                    final int centerY = v.getHeight() / 2;
                    v.getBackground().setHotspot(centerX, centerY);
                }
                v.setPressed(true);
                v.setPressed(false);
                mShouldBlink = false;
            });
        }
        mSeekBar.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(view, info);
                // Update the range info with the correct type
                AccessibilityNodeInfo.RangeInfo rangeInfo = info.getRangeInfo();
                if (rangeInfo != null) {
                    info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                                    mAccessibilityRangeInfoType, rangeInfo.getMin(),
                                    rangeInfo.getMax(), rangeInfo.getCurrent()));
                }
            }
        });
    }

    @Override
    public CharSequence getSummary() {
        return null;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setProgress(restoreValue ? getPersistedInt(mProgress)
                : (Integer) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        SeekBar seekBar = (SeekBar) v.findViewById(com.android.internal.R.id.seekbar);
        if (seekBar == null) {
            return false;
        }
        return seekBar.onKeyDown(keyCode, event);
    }

    public void setMax(int max) {
        if (max != mMax) {
            mMax = max;
            notifyChanged();
        }
    }

    public void setMin(int min) {
        if (min != mMin) {
            mMin = min;
            notifyChanged();
        }
    }

    public int getMax() {
        return mMax;
    }

    public int getMin() {
        return mMin;
    }

    public void setProgress(int progress) {
        setProgress(progress, true);
    }

    /**
     * Sets the progress point to draw a single tick mark representing a default value.
     */
    public void setDefaultProgress(int defaultProgress) {
        if (mDefaultProgress != defaultProgress) {
            mDefaultProgress = defaultProgress;
            if (mSeekBar instanceof DefaultIndicatorSeekBar) {
                ((DefaultIndicatorSeekBar) mSeekBar).setDefaultProgress(mDefaultProgress);
            }
        }
    }

    /**
     * When {@code continuousUpdates} is true, update the persisted setting immediately as the thumb
     * is dragged along the SeekBar. Otherwise, only update the value of the setting when the thumb
     * is dropped.
     */
    public void setContinuousUpdates(boolean continuousUpdates) {
        mContinuousUpdates = continuousUpdates;
    }

    private void setProgress(int progress, boolean notifyChanged) {
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress < mMin) {
            progress = mMin;
        }
        if (progress != mProgress) {
            mProgress = progress;
            persistInt(progress);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    public int getProgress() {
        return mProgress;
    }

    /**
     * Persist the seekBar's progress value if callChangeListener
     * returns true, otherwise set the seekBar's progress to the stored value
     */
    void syncProgress(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (progress != mProgress) {
            if (callChangeListener(progress)) {
                setProgress(progress, false);
            } else {
                seekBar.setProgress(mProgress);
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && (mContinuousUpdates || !mTrackingTouch)) {
            syncProgress(seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = false;
        if (seekBar.getProgress() != mProgress) {
            syncProgress(seekBar);
        }
    }

    /**
     * Specify the type of range this seek bar represents.
     *
     * @param rangeInfoType The type of range to be shared with accessibility
     *
     * @see android.view.accessibility.AccessibilityNodeInfo.RangeInfo
     */
    public void setAccessibilityRangeInfoType(int rangeInfoType) {
        mAccessibilityRangeInfoType = rangeInfoType;
    }

    public void setSeekBarContentDescription(CharSequence contentDescription) {
        mSeekBarContentDescription = contentDescription;
        if (mSeekBar != null) {
            mSeekBar.setContentDescription(contentDescription);
        }
    }

    /**
     * Specify the state description for this seek bar represents.
     *
     * @param stateDescription the state description of seek bar
     */
    public void setSeekBarStateDescription(CharSequence stateDescription) {
        mSeekBarStateDescription = stateDescription;
        if (mSeekBar != null) {
            mSeekBar.setStateDescription(stateDescription);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /*
         * Suppose a client uses this preference type without persisting. We
         * must save the instance state so it is able to, for example, survive
         * orientation changes.
         */

        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.progress = mProgress;
        myState.max = mMax;
        myState.min = mMin;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mProgress = myState.progress;
        mMax = myState.max;
        mMin = myState.min;
        notifyChanged();
    }

    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state
     * of MyPreference, a subclass of Preference.
     * <p>
     * It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        int progress;
        int max;
        int min;

        public SavedState(Parcel source) {
            super(source);

            // Restore the click counter
            progress = source.readInt();
            max = source.readInt();
            min = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            // Save the click counter
            dest.writeInt(progress);
            dest.writeInt(max);
            dest.writeInt(min);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
