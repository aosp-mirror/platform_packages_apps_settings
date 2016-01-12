/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.localepicker;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

class LocaleDragCell extends LinearLayout {
    private TextView mLabel;
    private TextView mMiniLabel;
    private ImageView mLocalized;
    private CheckBox mCheckbox;
    private ImageView mDragHandle;

    public LocaleDragCell(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLabel = (TextView) findViewById(R.id.label);
        mLocalized = (ImageView) findViewById(R.id.l10nWarn);
        mMiniLabel = (TextView) findViewById(R.id.miniLabel);
        mCheckbox = (CheckBox) findViewById(R.id.checkbox);
        mDragHandle = (ImageView) findViewById(R.id.dragHandle);
    }

    public void setShowHandle(boolean showHandle) {
        mDragHandle.setVisibility(showHandle ? VISIBLE : GONE);
        invalidate();
        requestLayout();
    }

    public void setShowCheckbox(boolean showCheckbox) {
        mCheckbox.setVisibility(showCheckbox ? VISIBLE : GONE);
        invalidate();
        requestLayout();
    }

    public void setChecked(boolean checked) {
        mCheckbox.setChecked(checked);
    }

    public void setShowMiniLabel(boolean showMiniLabel) {
        mMiniLabel.setVisibility(showMiniLabel ? VISIBLE : GONE);
        invalidate();
        requestLayout();
    }

    public void setMiniLabel(String miniLabelText) {
        mMiniLabel.setText(miniLabelText);
        invalidate();
    }

    public void setLabel(String labelText) {
        mLabel.setText(labelText);
        invalidate();
    }

    public void setLocalized(boolean localized) {
        mLocalized.setVisibility(localized ? INVISIBLE : VISIBLE);
        invalidate();
    }

    public ImageView getDragHandle() {
        return mDragHandle;
    }

    public ImageView getTranslateableLabel() {
        return mLocalized;
    }

    public TextView getTextLabel() {
        return mLabel;
    }

    public CheckBox getCheckbox() {
        return mCheckbox;
    }
}
