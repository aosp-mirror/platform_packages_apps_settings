/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.blacklist;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ImageView;

public class ToggleImageView extends ImageView implements Checkable {
    public static interface OnCheckedChangeListener {
        void onCheckedChanged(ToggleImageView view, boolean isChecked);
    }

    private static final int[] CHECKED_STATE_SET = {
        com.android.internal.R.attr.state_checked
    };

    private boolean mIsChecked = false;
    private OnCheckedChangeListener mOnCheckedChangeListener;

    public ToggleImageView(Context context) {
        super(context);
    }

    public ToggleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ToggleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.CompoundButton, defStyle, 0);
        boolean checked = a.getBoolean(
                com.android.internal.R.styleable.CompoundButton_checked, false);
        setChecked(checked);
        a.recycle();
    }

    @Override
    public boolean performClick() {
        /* When clicked, toggle the state */
        toggle();
        return super.performClick();
    }

    @Override
    public void setChecked(boolean checked) {
        setCheckedInternal(checked, true);
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mIsChecked);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    /* package */ void setCheckedInternal(boolean checked, boolean callListener) {
        if (mIsChecked != checked) {
            mIsChecked = checked;
            setImageState(checked ? CHECKED_STATE_SET : null, true);
            if (callListener && mOnCheckedChangeListener != null) {
                mOnCheckedChangeListener.onCheckedChanged(this, mIsChecked);
            }
        }
    }
}
