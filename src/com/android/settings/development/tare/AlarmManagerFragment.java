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
import android.app.tare.EconomyManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import androidx.annotation.Nullable;

import com.android.settings.R;

/**
 * Creates the AlarmManager fragment to display all the AlarmManager factors
 * when the AlarmManager policy is chosen in the dropdown TARE menu.
 */
public class AlarmManagerFragment extends Fragment implements
        TareFactorController.DataChangeListener {

    private TareFactorController mFactorController;

    private TareFactorExpandableListAdapter mExpandableListAdapter;

    private String[] mGroups;
    private String[][] mChildren;
    private String[][] mKeys;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFactorController = TareFactorController.getInstance(getContext());
        populateArrays();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tare_policy_fragment, null);
        ExpandableListView elv = (ExpandableListView) v.findViewById(R.id.factor_list);
        mExpandableListAdapter = new TareFactorExpandableListAdapter(
                mFactorController, LayoutInflater.from(getActivity()), mGroups, mChildren, mKeys);
        elv.setGroupIndicator(null);
        elv.setAdapter(mExpandableListAdapter);
        elv.setOnChildClickListener(new OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                final String key = mExpandableListAdapter.getKey(groupPosition, childPosition);
                mFactorController.createDialog(key).show(getFragmentManager(), key);
                return true;
            }
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        mFactorController.registerListener(this);
    }

    @Override
    public void onStop() {
        mFactorController.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onDataChanged() {
        mExpandableListAdapter.notifyDataSetChanged();
    }

    private void populateArrays() {
        final Resources resources = getResources();

        mGroups = new String[]{
                resources.getString(R.string.tare_consumption_limits),
                resources.getString(R.string.tare_balances),
                // resources.getString(R.string.tare_modifiers),
                resources.getString(R.string.tare_actions_ctp),
                resources.getString(R.string.tare_actions_base_price),
                resources.getString(R.string.tare_rewards_instantaneous),
                resources.getString(R.string.tare_rewards_ongoing),
                resources.getString(R.string.tare_rewards_max)
        };

        mChildren = new String[][]{
                resources.getStringArray(R.array.tare_consumption_limit_subfactors),
                resources.getStringArray(R.array.tare_app_balance_subfactors),
                // TODO: support
                // resources.getStringArray(R.array.tare_modifiers_subfactors),
                resources.getStringArray(R.array.tare_alarm_manager_actions),
                resources.getStringArray(R.array.tare_alarm_manager_actions),
                resources.getStringArray(R.array.tare_rewards_subfactors),
                {resources.getString(R.string.tare_top_activity)},
                resources.getStringArray(R.array.tare_rewards_subfactors)
        };

        mKeys = new String[][]{
                {
                        EconomyManager.KEY_AM_INITIAL_CONSUMPTION_LIMIT,
                        EconomyManager.KEY_AM_MIN_CONSUMPTION_LIMIT,
                        EconomyManager.KEY_AM_MAX_CONSUMPTION_LIMIT,
                },
                {
                        EconomyManager.KEY_AM_MAX_SATIATED_BALANCE,
                        EconomyManager.KEY_AM_MIN_SATIATED_BALANCE_EXEMPTED,
                        EconomyManager.KEY_AM_MIN_SATIATED_BALANCE_HEADLESS_SYSTEM_APP,
                        EconomyManager.KEY_AM_MIN_SATIATED_BALANCE_OTHER_APP
                },
                // {},
                {
                        EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_CTP,
                        EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_CTP,
                        EconomyManager.KEY_AM_ACTION_ALARM_EXACT_WAKEUP_CTP,
                        EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_WAKEUP_CTP,
                        EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_CTP,
                        EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_CTP,
                        EconomyManager.KEY_AM_ACTION_ALARM_EXACT_NONWAKEUP_CTP,
                        EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_NONWAKEUP_CTP,
                        EconomyManager.KEY_AM_ACTION_ALARM_ALARMCLOCK_CTP
                },
                {
                        EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_BASE_PRICE,
                        EconomyManager
                                .KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_BASE_PRICE,
                        EconomyManager.KEY_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE,
                        EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_WAKEUP_BASE_PRICE,
                        EconomyManager
                                .KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_BASE_PRICE,
                        EconomyManager
                                .KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE,
                        EconomyManager.KEY_AM_ACTION_ALARM_EXACT_NONWAKEUP_BASE_PRICE,
                        EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_NONWAKEUP_BASE_PRICE,
                        EconomyManager.KEY_AM_ACTION_ALARM_ALARMCLOCK_BASE_PRICE
                },
                {
                        EconomyManager.KEY_AM_REWARD_TOP_ACTIVITY_INSTANT,
                        EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_INSTANT,
                        EconomyManager.KEY_AM_REWARD_NOTIFICATION_INTERACTION_INSTANT,
                        EconomyManager.KEY_AM_REWARD_WIDGET_INTERACTION_INSTANT,
                        EconomyManager.KEY_AM_REWARD_OTHER_USER_INTERACTION_INSTANT,
                },
                {EconomyManager.KEY_AM_REWARD_TOP_ACTIVITY_ONGOING},
                {
                        EconomyManager.KEY_AM_REWARD_TOP_ACTIVITY_MAX,
                        EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_MAX,
                        EconomyManager.KEY_AM_REWARD_NOTIFICATION_INTERACTION_MAX,
                        EconomyManager.KEY_AM_REWARD_WIDGET_INTERACTION_MAX,
                        EconomyManager.KEY_AM_REWARD_OTHER_USER_INTERACTION_MAX,
                }
        };
    }
}
