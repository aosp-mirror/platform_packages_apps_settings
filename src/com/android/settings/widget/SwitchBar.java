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

public class SwitchBar extends LinearLayout implements CompoundButton.OnCheckedChangeListener {

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
        mSwitch.setOnCheckedChangeListener(this);

        addOnSwitchChangeListener(new OnSwitchChangeListener() {
            @Override
            public void onSwitchChanged(Switch switchView, boolean isChecked) {
                mTextView.setText(isChecked ? R.string.switch_on_text : R.string.switch_off_text);
            }
        });

        mSwitch.setTrackResource(R.drawable.switch_track);
        mSwitch.setThumbResource(R.drawable.switch_inner);

        // Default is hide
        setVisibility(View.GONE);
    }

    public ToggleSwitch getSwitch() {
        return mSwitch;
    }

    public void show() {
        TransitionManager.beginDelayedTransition((ViewGroup) getParent());
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        TransitionManager.beginDelayedTransition((ViewGroup) getParent());
        setVisibility(View.GONE);
    }

    public boolean isShowing() {
        return (getVisibility() == View.VISIBLE);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final int count = mSwitchChangeListeners.size();
        for (int n = 0; n < count; n++) {
            mSwitchChangeListeners.get(n).onSwitchChanged(mSwitch,isChecked);
        }
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
}
