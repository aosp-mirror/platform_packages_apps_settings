/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

/**
 * Creates the JobScheduler fragment to display all the JobScheduler factors
 * when the JobScheduler policy is chosen in the dropdown TARE menu.
 */
public class JobSchedulerFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tare_policy_fragment, null);
        ExpandableListView elv = (ExpandableListView) v.findViewById(R.id.factor_list);
        final SavedTabsListAdapter expListAdapter = new SavedTabsListAdapter();
        elv.setGroupIndicator(null);
        elv.setAdapter(expListAdapter);
        elv.setOnChildClickListener(new OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                final String selected =
                        (String) expListAdapter.getChild(groupPosition, childPosition);
                Toast.makeText(getActivity(), selected, Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
        });
        return v;
    }

    /**
     * Creates the expandable list containing all JobScheduler factors within the
     * JobScheduler fragment.
     */
    public class SavedTabsListAdapter extends BaseExpandableListAdapter {

        private final LayoutInflater mInflater;
        private Resources mResources = getActivity().getResources();

        private String[] mGroups = {
                mResources.getString(R.string.tare_max_circulation),
                mResources.getString(R.string.tare_max_satiated_balance),
                mResources.getString(R.string.tare_min_satiated_balance),
                mResources.getString(R.string.tare_modifiers),
                mResources.getString(R.string.tare_actions),
                mResources.getString(R.string.tare_rewards)
        };

        /*
         * First two are empty arrays because the first two factors have no subfactors (no
         * children).
         */
        private String[][] mChildren = {
                {},
                {},
                mResources.getStringArray(R.array.tare_min_satiated_balance_subfactors),
                mResources.getStringArray(R.array.tare_modifiers_subfactors),
                mResources.getStringArray(R.array.tare_job_scheduler_actions),
                mResources.getStringArray(R.array.tare_rewards_subfactors)
        };

        public SavedTabsListAdapter() {
            mInflater = LayoutInflater.from(getActivity());
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

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView factor = (TextView) convertView.findViewById(android.R.id.text1);
            factor.setText(getGroup(groupPosition).toString());
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            // Here a custom child item is used instead of android.R.simple_list_item_2 because it
            // is more customizable for this specific UI
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.tare_child_item, null);
            }
            TextView factor = (TextView) convertView.findViewById(R.id.factor);
            TextView value = (TextView) convertView.findViewById(R.id.factor_number);

            // TODO: Replace these hardcoded values with either default or user inputted TARE values
            factor.setText(getChild(groupPosition, childPosition).toString());
            value.setText("500");

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
