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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.settings.R;

class LocaleDragCell extends RelativeLayout {
    // We need to keep the label and the checkbox "in sync"
    // The checkbox shows in remove mode, and the label shows in normal mode, in the same position.
    // So we need to set the same text to both of them, and coordinate the show / hide.
    private TextView mLabel;
    private CheckBox mCheckbox;
    private TextView mMiniLabel;
    private TextView mLocalized;
    private ImageView mDragHandle;

    public LocaleDragCell(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLabel = (TextView) findViewById(R.id.label);
        mLocalized = (TextView) findViewById(R.id.l10nWarn);
        mMiniLabel = (TextView) findViewById(R.id.miniLabel);
        mCheckbox = (CheckBox) findViewById(R.id.checkbox);
        mDragHandle = (ImageView) findViewById(R.id.dragHandle);
    }

    public void setShowHandle(boolean showHandle) {
        // We want invisible, not gone, so that everything else stays the same.
        // With GONE there is more space for the labels and the text wrapping change.
        // So the transition between "normal" mode (with numbers) and
        // "remove mode" (with checkboxes) is not that "smooth"
        mDragHandle.setVisibility(showHandle ? VISIBLE : INVISIBLE);
        invalidate();
        requestLayout();
    }

    public void setShowCheckbox(boolean showCheckbox) {
        // "Opposite" visibility for label / checkbox
        if (showCheckbox) {
            mCheckbox.setVisibility(VISIBLE);
            mLabel.setVisibility(INVISIBLE);
        } else {
            mCheckbox.setVisibility(INVISIBLE);
            mLabel.setVisibility(VISIBLE);
        }
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

    public void setLabelAndDescription(String labelText, String description) {
        mLabel.setText(labelText);
        mCheckbox.setText(labelText);
        mLabel.setContentDescription(description);
        mCheckbox.setContentDescription(description);
        invalidate();
    }

    public void setLocalized(boolean localized) {
        mLocalized.setVisibility(localized ? GONE : VISIBLE);
        invalidate();
    }

    public ImageView getDragHandle() {
        return mDragHandle;
    }

    public CheckBox getCheckbox() {
        return mCheckbox;
    }
}
