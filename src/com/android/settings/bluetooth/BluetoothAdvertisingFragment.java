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

package com.android.settings.bluetooth;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.R;

/**
 * Fragment to display and let the user change advertising preference.
 */
public class BluetoothAdvertisingFragment extends Fragment
        implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "BluetoothAdvertisingFragment";
    private View mView;
    private Switch mActionBarSwitch;
    private Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mActionBarSwitch = new Switch(mActivity);
        if (mActivity instanceof PreferenceActivity) {
            final int padding = mActivity.getResources().getDimensionPixelSize(
                    R.dimen.action_bar_switch_padding);
            mActionBarSwitch.setPaddingRelative(0, 0, padding, 0);
            mActivity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            mActivity.getActionBar().setCustomView(mActionBarSwitch, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.END));
            mActivity.getActionBar().setTitle(R.string.bluetooth_broadcasting);
        }
        mActionBarSwitch.setChecked(
                LocalBluetoothPreferences.isAdvertisingEnabled(mActivity.getApplicationContext()));

        mActionBarSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.bluetooth_advertising, container, false);
        initView(mView);
        return mView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().getActionBar().setCustomView(null);
    }

    private void initView(View view) {
        mActionBarSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean desiredState) {
        mActionBarSwitch.setChecked(desiredState);
        Context context = getActivity();
        LocalBluetoothPreferences.setAdvertisingEnabled(context, desiredState);
        if (!desiredState) {
            LocalBluetoothAdapter adapter =
                    LocalBluetoothManager.getInstance(context).getBluetoothAdapter();
            // Stop advertising if advertising is in process.
            if (adapter.isAdvertising()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_STOP_ADVERTISING);
                getActivity().startActivity(intent);
            }
        }
    }
}
