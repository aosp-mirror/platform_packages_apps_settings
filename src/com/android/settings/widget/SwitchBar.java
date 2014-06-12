/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.os.Parcel;
import android.os.Parcelable;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;

import java.util.ArrayList;

public class SwitchBar extends LinearLayout implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    private ToggleSwitch mSwitch;
    private TextView mTextView;

    private ArrayList<OnSwitchChangeListener> mSwitchChangeListeners =
            new ArrayList<OnSwitchChangeListener>();

    public static interface OnSwitchChangeListener {
        /**
         * Called when the checked state of the Switch has changed.
         *
         * @param switchView The Switch view whose state has changed.
         * @param isChecked  The new checked state of switchView.
         */
        void onSwitchChanged(Switch switchView, boolean isChecked);
    }

    public SwitchBar(Context context) {
        this(context, null);
    }

    public SwitchBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SwitchBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.switch_bar, this);

        mTextView = (TextView) findViewById(R.id.switch_text);
        mTextView.setText(R.string.switch_off_text);

        mSwitch = (ToggleSwitch) findViewById(R.id.switch_widget);

        addOnSwitchChangeListener(new OnSwitchChangeListener() {
            @Override
            public void onSwitchChanged(Switch switchView, boolean isChecked) {
                setTextViewLabel(isChecked);
            }
        });

        mSwitch.setTrackResource(R.drawable.switch_track);
        mSwitch.setThumbResource(R.drawable.switch_inner);

        setOnClickListener(this);

        // Default is hide
        setVisibility(View.GONE);
    }

    public void setTextViewLabel(boolean isChecked) {
        mTextView.setText(isChecked ? R.string.switch_on_text : R.string.switch_off_text);
    }

    public void setChecked(boolean checked) {
        setTextViewLabel(checked);
        mSwitch.setChecked(checked);
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mTextView.setEnabled(enabled);
        mSwitch.setEnabled(false);
    }

    public final ToggleSwitch getSwitch() {
        return mSwitch;
    }

    public void show() {
        if (!isShowing()) {
            setVisibility(View.VISIBLE);
            mSwitch.setOnCheckedChangeListener(this);
        }
    }

    public void hide() {
        if (isShowing()) {
            setVisibility(View.GONE);
            mSwitch.setOnCheckedChangeListener(null);
        }
    }

    public boolean isShowing() {
        return (getVisibility() == View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        final boolean isChecked = !mSwitch.isChecked();
        mSwitch.setChecked(isChecked);
    }

    public void propagateChecked(boolean isChecked) {
        final int count = mSwitchChangeListeners.size();
        for (int n = 0; n < count; n++) {
            mSwitchChangeListeners.get(n).onSwitchChanged(mSwitch, isChecked);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        propagateChecked(isChecked);
    }

    public void addOnSwitchChangeListener(OnSwitchChangeListener listener) {
        if (mSwitchChangeListeners.contains(listener)) {
            throw new IllegalStateException("Cannot add twice the same OnSwitchChangeListener");
        }
        mSwitchChangeListeners.add(listener);
    }

    public void removeOnSwitchChangeListener(OnSwitchChangeListener listener) {
        if (!mSwitchChangeListeners.contains(listener)) {
            throw new IllegalStateException("Cannot remove OnSwitchChangeListener");
        }
        mSwitchChangeListeners.remove(listener);
    }

    static class SavedState extends BaseSavedState {
        boolean checked;
        boolean visible;

        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            checked = (Boolean)in.readValue(null);
            visible = (Boolean)in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(checked);
            out.writeValue(visible);
        }

        @Override
        public String toString() {
            return "SwitchBar.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " checked=" + checked
                    + " visible=" + visible + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.checked = mSwitch.isChecked();
        ss.visible = isShowing();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());
        mSwitch.setChecked(ss.checked);
        setTextViewLabel(ss.checked);
        setVisibility(ss.visible ? View.VISIBLE : View.GONE);
        requestLayout();
    }
}
