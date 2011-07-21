/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.nfc;

import android.app.Fragment;
import android.content.ContentResolver;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.util.Log;
import com.android.settings.R;

public class ZeroClick extends Fragment
        implements CompoundButton.OnCheckedChangeListener {
    private View mView;
    private CheckBox mCheckbox;
    private NfcAdapter mNfcAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.zeroclick, container, false);
        initView(mView);
        return mView;
    }

    private void initView(View view) {
        mCheckbox = (CheckBox) mView.findViewById(R.id.zeroclick_checkbox);
        mCheckbox.setOnCheckedChangeListener(this);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        mCheckbox.setChecked(mNfcAdapter.zeroClickEnabled());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean value) {
        Log.e("AAP", "onCheckedChanged!");
        final boolean desiredState = (Boolean) value;
        boolean success = false;
        mCheckbox.setEnabled(false);
        if (desiredState) {
            success = mNfcAdapter.enableZeroClick();
        } else {
            success = mNfcAdapter.disableZeroClick();
        }
        if (success) {
            mCheckbox.setChecked(desiredState);
        }
        mCheckbox.setEnabled(true);
    }
}
