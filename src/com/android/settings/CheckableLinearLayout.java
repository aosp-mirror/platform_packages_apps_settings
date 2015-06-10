/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable {

    private boolean mChecked;
    private float mDisabledAlpha;

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedValue alpha = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, alpha, true);
        mDisabledAlpha = alpha.getFloat();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        final int N = getChildCount();
        for (int i = 0; i < N; i++) {
            getChildAt(i).setAlpha(enabled ? 1 : mDisabledAlpha);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        updateChecked();
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    private void updateChecked() {
        final int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View child = getChildAt(i);
            if (child instanceof Checkable) {
                ((Checkable) child).setChecked(mChecked);
            }
        }
    }

}
