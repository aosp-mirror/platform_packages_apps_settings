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

package com.android.settings.accounts;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserManager;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.users.UserDialogs;

public class RemoveUserFragment extends InstrumentedDialogFragment {
    private static final String ARG_USER_ID = "userId";

    static RemoveUserFragment newInstance(int userId) {
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        RemoveUserFragment fragment = new RemoveUserFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int userId = getArguments().getInt(ARG_USER_ID);
        return UserDialogs.createRemoveDialog(getActivity(), userId,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    UserManager um = (UserManager)
                        getActivity().getSystemService(Context.USER_SERVICE);
                    um.removeUser(userId);
                }
            });
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_REMOVE_USER;
    }
}
