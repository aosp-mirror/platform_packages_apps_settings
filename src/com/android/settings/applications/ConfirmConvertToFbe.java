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
package com.android.settings.applications;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ConfirmConvertToFbe extends SettingsPreferenceFragment {
    static final String TAG = "ConfirmConvertToFBE";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.confirm_convert_fbe, null);

        final Button button = (Button) rootView.findViewById(R.id.button_confirm_convert_fbe);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_FACTORY_RESET);
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                intent.setPackage("android");
                intent.putExtra(Intent.EXTRA_REASON, "convert_fbe");
                getActivity().sendBroadcast(intent);
            }
        });

        return rootView;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONVERT_FBE_CONFIRM;
    }
}
