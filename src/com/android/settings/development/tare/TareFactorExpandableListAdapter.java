/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development.tare;

import static android.app.tare.EconomyManager.CAKE_IN_ARC;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.settings.R;

/**
 * Creates the expandable list that will allow modifying individual factors.
 */
public class TareFactorExpandableListAdapter extends BaseExpandableListAdapter {

    private final LayoutInflater mLayoutInflater;
    private final TareFactorController mFactorController;

    private final String[] mGroups;
    private final String[][] mChildren;
    private final String[][] mKeys;

    TareFactorExpandableListAdapter(TareFactorController factorController,
            LayoutInflater layoutInflater, String[] groups, String[][] children, String[][] keys) {
        mLayoutInflater = layoutInflater;
        mFactorController = factorController;

        mGroups = groups;
        mChildren = children;
        mKeys = keys;

        validateMappings();
    }

    private void validateMappings() {
        if (mGroups.length != mChildren.length) {
            throw new IllegalStateException("groups and children don't have the same length");
        }
        if (mChildren.length != mKeys.length) {
            throw new IllegalStateException("children and keys don't have the same length");
        }
        for (int i = 0; i < mChildren.length; ++i) {
            if (mChildren[i].length != mKeys[i].length) {
                throw new IllegalStateException(
                        "children and keys don't have the same length in row " + i);
            }
        }
    }

    @Override
    public int getGroupCount() {
        return mGroups.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildren[groupPosition].length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mGroups[groupPosition];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mChildren[groupPosition][childPosition];
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @NonNull
    String getKey(int groupPosition, int childPosition) {
        return mKeys[groupPosition][childPosition];
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(android.R.layout.simple_list_item_1, parent,
                    false);
        }
        TextView factor = convertView.findViewById(android.R.id.text1);
        factor.setText(getGroup(groupPosition).toString());
        return convertView;
    }

    @Override
    @SuppressLint("InflateParams") // AdapterView doesn't support addView
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
        // Here a custom child item is used instead of android.R.simple_list_item_2 because it
        // is more customizable for this specific UI
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.tare_child_item, null);
        }
        TextView factor = convertView.findViewById(R.id.factor);
        TextView value = convertView.findViewById(R.id.factor_number);

        factor.setText(getChild(groupPosition, childPosition).toString());
        value.setText(cakeToString(
                mFactorController.getValue(getKey(groupPosition, childPosition))));

        return convertView;
    }

    @NonNull
    private static String cakeToString(long cakes) {
        // Resources.getQuantityString doesn't handle floating point numbers, so doing this manually
        if (cakes == 0) {
            return "0";
        }
        final long sub = cakes % CAKE_IN_ARC;
        final long arcs = (int) (cakes / CAKE_IN_ARC);
        if (arcs == 0) {
            return sub + " c";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(arcs);
        if (sub > 0) {
            sb.append(".").append(String.format("%03d", sub / (CAKE_IN_ARC / 1000)));
        }
        sb.append(" A");
        return sb.toString();
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
