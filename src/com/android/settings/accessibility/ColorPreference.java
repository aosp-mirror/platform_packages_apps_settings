/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

/**
 * Grid preference that allows the user to pick a color from a predefined set of
 * colors. Optionally shows a preview in the preference item.
 */
public class ColorPreference extends ListDialogPreference {
    private ColorDrawable mPreviewColor;
    private boolean mPreviewEnabled;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.grid_picker_dialog);
        setListItemLayoutResource(R.layout.color_picker_item);
    }

    /**
     * @param enabled whether to show a preview in the preference item
     */
    public void setPreviewEnabled(boolean enabled) {
        if (mPreviewEnabled != enabled) {
            mPreviewEnabled = enabled;

            if (enabled) {
                setWidgetLayoutResource(R.layout.preference_color);
            } else {
                setWidgetLayoutResource(0);
            }
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        return Color.alpha(getValue()) == 0 || super.shouldDisableDependents();
    }

    @Override
    protected CharSequence getTitleAt(int index) {
        final CharSequence title = super.getTitleAt(index);
        if (title != null) {
            return title;
        }

        // If no title was supplied, format title using RGB values.
        final int value = getValueAt(index);
        final int r = Color.red(value);
        final int g = Color.green(value);
        final int b = Color.blue(value);
        return getContext().getString(R.string.color_custom, r, g, b);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if (mPreviewEnabled) {
            final ImageView previewImage = (ImageView) view.findViewById(R.id.color_preview);
            final int argb = getValue();
            if (Color.alpha(argb) < 255) {
                previewImage.setBackgroundResource(R.drawable.transparency_tileable);
            } else {
                previewImage.setBackground(null);
            }

            if (mPreviewColor == null) {
                mPreviewColor = new ColorDrawable(argb);
                previewImage.setImageDrawable(mPreviewColor);
            } else {
                mPreviewColor.setColor(argb);
            }

            final CharSequence summary = getSummary();
            if (!TextUtils.isEmpty(summary)) {
                previewImage.setContentDescription(summary);
            } else {
                previewImage.setContentDescription(null);
            }

            previewImage.setAlpha(isEnabled() ? 1f : 0.2f);
        }
    }

    @Override
    protected void onBindListItem(View view, int index) {
        final int argb = getValueAt(index);
        final int alpha = Color.alpha(argb);

        final ImageView swatch = (ImageView) view.findViewById(R.id.color_swatch);
        if (alpha < 255) {
            swatch.setBackgroundResource(R.drawable.transparency_tileable);
        } else {
            swatch.setBackground(null);
        }

        final Drawable foreground = swatch.getDrawable();
        if (foreground instanceof ColorDrawable) {
            ((ColorDrawable) foreground).setColor(argb);
        } else {
            swatch.setImageDrawable(new ColorDrawable(argb));
        }

        final CharSequence title = getTitleAt(index);
        if (title != null) {
            final TextView summary = (TextView) view.findViewById(R.id.summary);
            summary.setText(title);
        }
    }
}
