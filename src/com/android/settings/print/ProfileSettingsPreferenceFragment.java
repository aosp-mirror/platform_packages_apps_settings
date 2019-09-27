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

package com.android.settings.print;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.dashboard.profileselector.UserAdapter;

/**
 * Base fragment class for per profile settings.
 */
public abstract class ProfileSettingsPreferenceFragment extends SettingsPreferenceFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        final UserAdapter profileSpinnerAdapter =
                UserAdapter.createUserSpinnerAdapter(um, getActivity());
        if (profileSpinnerAdapter != null) {
            final Spinner spinner = (Spinner) setPinnedHeaderView(R.layout.spinner_view);
            spinner.setAdapter(profileSpinnerAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                        long id) {
                    final UserHandle selectedUser = profileSpinnerAdapter.getUserHandle(position);
                    if (selectedUser.getIdentifier() != UserHandle.myUserId()) {
                        final Activity activity = getActivity();
                        Intent intent = new Intent(getIntentActionString());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivityAsUser(intent, selectedUser);
                        // Go back to default selection, which is the first one
                        spinner.setSelection(0);
                        activity.finish();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Nothing to do
                }
            });
        }
    }

    /**
     * @return intent action string that will bring user to this fragment.
     */
    protected abstract String getIntentActionString();

}
