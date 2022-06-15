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
 * limitations under the License
 */

package com.android.settings.notification;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;

import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;

public class NotificationLockscreenPreference extends RestrictedListPreference {

    private boolean mAllowRemoteInput;
    private Listener mListener;
    private boolean mShowRemoteInput;
    private boolean mRemoteInputCheckBoxEnabled = true;
    private int mUserId = UserHandle.myUserId();
    private RestrictedLockUtils.EnforcedAdmin mAdminRestrictingRemoteInput;

    public NotificationLockscreenPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRemoteInputCheckBoxEnabled(boolean enabled) {
        mRemoteInputCheckBoxEnabled = enabled;
    }

    public void setRemoteInputRestricted(RestrictedLockUtils.EnforcedAdmin admin) {
        mAdminRestrictingRemoteInput = admin;
    }

    @Override
    protected void onClick() {
        final Context context = getContext();
        if (!Utils.startQuietModeDialogIfNecessary(context, UserManager.get(context), mUserId)) {
            // Call super to create preference dialog only when work mode is on
            // startQuietModeDialogIfNecessary will return false if mUserId is not a managed user
            super.onClick();
        }
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener innerListener) {

        mListener = new Listener(innerListener);
        builder.setSingleChoiceItems(createListAdapter(builder.getContext()), getSelectedValuePos(),
                mListener);
        mShowRemoteInput = getEntryValues().length == 3;
        mAllowRemoteInput = Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_REMOTE_INPUT, 0) != 0;
        builder.setView(R.layout.lockscreen_remote_input);
    }

    @Override
    protected void onDialogCreated(Dialog dialog) {
        super.onDialogCreated(dialog);
        dialog.create();
        CheckBox checkbox = (CheckBox) dialog.findViewById(R.id.lockscreen_remote_input);
        checkbox.setChecked(!mAllowRemoteInput);
        checkbox.setOnCheckedChangeListener(mListener);
        checkbox.setEnabled(mAdminRestrictingRemoteInput == null);

        View restricted = dialog.findViewById(R.id.restricted_lock_icon_remote_input);
        restricted.setVisibility(mAdminRestrictingRemoteInput == null ? View.GONE : View.VISIBLE);

        if (mAdminRestrictingRemoteInput != null) {
            checkbox.setClickable(false);
            dialog.findViewById(com.android.internal.R.id.customPanel)
                    .setOnClickListener(mListener);
        }
    }

    @Override
    protected void onDialogStateRestored(Dialog dialog, Bundle savedInstanceState) {
        super.onDialogStateRestored(dialog, savedInstanceState);
        ListView listView = ((AlertDialog) dialog).getListView();
        int selectedPosition = listView.getCheckedItemPosition();

        View panel = dialog.findViewById(com.android.internal.R.id.customPanel);
        panel.setVisibility(checkboxVisibilityForSelectedIndex(selectedPosition,
                mShowRemoteInput));
        mListener.setView(panel);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        Settings.Secure.putInt(getContext().getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_REMOTE_INPUT, mAllowRemoteInput ? 1 : 0);
    }

    @Override
    protected boolean isAutoClosePreference() {
        return false;
    }

    private int checkboxVisibilityForSelectedIndex(int selected,
            boolean showRemoteAtAll) {
        return selected == 1 && showRemoteAtAll && mRemoteInputCheckBoxEnabled ? View.VISIBLE
                : View.GONE;
    }

    private class Listener implements DialogInterface.OnClickListener,
            CompoundButton.OnCheckedChangeListener, View.OnClickListener {

        private final DialogInterface.OnClickListener mInner;
        private View mView;

        public Listener(DialogInterface.OnClickListener inner) {
            mInner = inner;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            mInner.onClick(dialog, which);
            ListView listView = ((AlertDialog) dialog).getListView();
            int selectedPosition = listView.getCheckedItemPosition();
            if (mView != null) {
                mView.setVisibility(
                        checkboxVisibilityForSelectedIndex(selectedPosition, mShowRemoteInput));
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mAllowRemoteInput = !isChecked;
        }

        public void setView(View view) {
            mView = view;
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == com.android.internal.R.id.customPanel) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                        mAdminRestrictingRemoteInput);
            }
        }
    }
}
