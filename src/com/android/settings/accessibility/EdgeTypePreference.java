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
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.widget.TextView;

import com.android.internal.widget.SubtitleView;
import com.android.settings.R;

/**
 * Grid preference that allows the user to pick a captioning edge type.
 */
public class EdgeTypePreference extends ListDialogPreference {
    private static final int DEFAULT_FOREGROUND_COLOR = Color.WHITE;
    private static final int DEFAULT_BACKGROUND_COLOR = Color.TRANSPARENT;
    private static final int DEFAULT_EDGE_COLOR = Color.BLACK;
    private static final float DEFAULT_FONT_SIZE = 32f;

    public EdgeTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources res = context.getResources();
        setValues(res.getIntArray(R.array.captioning_edge_type_selector_values));
        setTitles(res.getStringArray(R.array.captioning_edge_type_selector_titles));
        setDialogLayoutResource(R.layout.grid_picker_dialog);
        setListItemLayoutResource(R.layout.preset_picker_item);
    }

    @Override
    public boolean shouldDisableDependents() {
        return getValue() == CaptionStyle.EDGE_TYPE_NONE || super.shouldDisableDependents();
    }

    @Override
    protected void onBindListItem(View view, int index) {
        final SubtitleView preview = (SubtitleView) view.findViewById(R.id.preview);

        preview.setForegroundColor(DEFAULT_FOREGROUND_COLOR);
        preview.setBackgroundColor(DEFAULT_BACKGROUND_COLOR);

        final float density = getContext().getResources().getDisplayMetrics().density;
        preview.setTextSize(DEFAULT_FONT_SIZE * density);

        final int value = getValueAt(index);
        preview.setEdgeType(value);
        preview.setEdgeColor(DEFAULT_EDGE_COLOR);

        final CharSequence title = getTitleAt(index);
        if (title != null) {
            final TextView summary = (TextView) view.findViewById(R.id.summary);
            summary.setText(title);
        }
    }
}
