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

package com.android.settings.utils;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.app.Activity;
import com.android.settings.R;

import java.util.List;
import android.util.Log;

/**
 * Array adapter for selecting an item by voice interaction. Each row includes a visual
 * indication of the 1-indexed position of the item so that a user can easily say
 * "number 4" to select it.
 */
public class VoiceSelectionAdapter extends ArrayAdapter<VoiceSelection> {
    public VoiceSelectionAdapter(Context context, int resource, List<VoiceSelection> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VoiceSelection item = getItem(position);
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            row = inflater.inflate(R.layout.voice_item_row, parent, false);
        }

        TextView label = (TextView) row.findViewById(R.id.voice_item_label);
        if (label != null) {
            label.setText(item.getLabel());
        }

        TextView positionLabel = (TextView) row.findViewById(R.id.voice_item_position);
        if (positionLabel != null) {
            positionLabel.setText(Integer.toString(position + 1));
        }

        return row;
    }
};
