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

import com.android.settings.R;
import com.android.settings.RestrictedListPreference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;

public class NotificationLockscreenPreference extends RestrictedListPreference {

    private boolean mAllowRemoteInput;
    private int mInitialIndex;
    private Listener mListener;
    private boolean mShowRemoteInput;

    public NotificationLockscreenPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener innerListener) {

        final String selectedValue = getValue();
        mInitialIndex = (selectedValue == null) ? -1 : findIndexOfValue(selectedValue);
        mListener = new Listener(innerListener);
        builder.setSingleChoiceItems(createListAdapter(), mInitialIndex, mListener);
        mShowRemoteInput = getEntryValues().length == 3;
        mAllowRemoteInput = Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_REMOTE_INPUT, 0) != 0;
        builder.setView(R.layout.lockscreen_remote_input);
    }

    @Override
    protected void onDialogCreated(Dialog dialog) {
        super.onDialogCreated(dialog);
        dialog.create();
        CheckBox view = (CheckBox) dialog.findViewById(R.id.lockscreen_remote_input);
        view.setChecked(!mAllowRemoteInput);
        view.setOnCheckedChangeListener(mListener);
        View panel = dialog.findViewById(com.android.internal.R.id.customPanel);
        panel.setVisibility(checkboxVisibilityForSelectedIndex(mInitialIndex, mShowRemoteInput));
        mListener.setView(panel);
    }

    @Override
    protected ListAdapter createListAdapter() {
        return new RestrictedArrayAdapter(getContext(), getEntries(), -1);
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

    private static int checkboxVisibilityForSelectedIndex(int selected, boolean showRemoteAtAll) {
        return selected == 1 && showRemoteAtAll ? View.VISIBLE : View.GONE;
    }

    private class Listener implements DialogInterface.OnClickListener,
            CompoundButton.OnCheckedChangeListener {

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
    }
}
