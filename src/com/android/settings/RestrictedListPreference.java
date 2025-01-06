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

package com.android.settings;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.preference.ListPreferenceDialogFragmentCompat;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.RestrictedPreferenceHelperProvider;

import java.util.ArrayList;
import java.util.List;

public class RestrictedListPreference extends CustomListPreference implements
        RestrictedPreferenceHelperProvider {

    private final RestrictedPreferenceHelper mHelper;
    private final List<RestrictedItem> mRestrictedItems = new ArrayList<>();
    private boolean mRequiresActiveUnlockedProfile = false;
    private int mProfileUserId;

    public RestrictedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHelper = new RestrictedPreferenceHelper(context, this, attrs);
    }

    public RestrictedListPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mHelper = new RestrictedPreferenceHelper(context, this, attrs);
    }

    @Override
    public @NonNull RestrictedPreferenceHelper getRestrictedPreferenceHelper() {
        return mHelper;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mHelper.onBindViewHolder(holder);
    }

    @Override
    public void performClick() {
        if (mRequiresActiveUnlockedProfile) {
            // Check if the profile is started, first.
            if (Utils.startQuietModeDialogIfNecessary(getContext(), UserManager.get(getContext()),
                    mProfileUserId)) {
                return;
            }

            // Next, check if the profile is unlocked.
            KeyguardManager manager =
                    (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
            if (manager.isDeviceLocked(mProfileUserId)) {
                Intent intent = manager.createConfirmDeviceCredentialIntent(
                        null, null, mProfileUserId);
                getContext().startActivity(intent);
                return;
            }
        }

        if (!mHelper.performClick()) {
            super.performClick();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && isDisabledByAdmin()) {
            mHelper.setDisabledByAdmin(null);
            return;
        }
        super.setEnabled(enabled);
    }

    public void setDisabledByAdmin(EnforcedAdmin admin) {
        if (mHelper.setDisabledByAdmin(admin)) {
            notifyChanged();
        }
    }

    public boolean isDisabledByAdmin() {
        return mHelper.isDisabledByAdmin();
    }

    public void setRequiresActiveUnlockedProfile(boolean reqState) {
        mRequiresActiveUnlockedProfile = reqState;
    }

    public void setProfileUserId(int profileUserId) {
        mProfileUserId = profileUserId;
    }

    public boolean isRestrictedForEntry(CharSequence entry) {
        if (entry == null) {
            return false;
        }
        for (RestrictedItem item : mRestrictedItems) {
            if (entry.equals(item.entry)) {
                return true;
            }
        }
        return false;
    }

    public void addRestrictedItem(RestrictedItem item) {
        mRestrictedItems.add(item);
    }

    public void clearRestrictedItems() {
        mRestrictedItems.clear();
    }

    private RestrictedItem getRestrictedItemForEntryValue(CharSequence entryValue) {
        if (entryValue == null) {
            return null;
        }
        for (RestrictedItem item : mRestrictedItems) {
            if (entryValue.equals(item.entryValue)) {
                return item;
            }
        }
        return null;
    }

    protected ListAdapter createListAdapter(Context context) {
        return new RestrictedArrayAdapter(context, getEntries(),
                getSelectedValuePos());
    }

    public int getSelectedValuePos() {
        final String selectedValue = getValue();
        final int selectedIndex =
                (selectedValue == null) ? -1 : findIndexOfValue(selectedValue);
        return selectedIndex;
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        builder.setAdapter(createListAdapter(builder.getContext()), listener);
    }

    public class RestrictedArrayAdapter extends ArrayAdapter<CharSequence> {
        private final int mSelectedIndex;
        public RestrictedArrayAdapter(Context context, CharSequence[] objects, int selectedIndex) {
            super(context, R.layout.restricted_dialog_singlechoice, R.id.text1, objects);
            mSelectedIndex = selectedIndex;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View root = super.getView(position, convertView, parent);
            CharSequence entry = getItem(position);
            CheckedTextView text = (CheckedTextView) root.findViewById(R.id.text1);
            if (isRestrictedForEntry(entry)) {
                text.setEnabled(false);
                text.setChecked(false);
            } else {
                if (mSelectedIndex != -1) {
                    text.setChecked(position == mSelectedIndex);
                }
                if (!text.isEnabled()) {
                    text.setEnabled(true);
                }
            }
            return root;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    public static class RestrictedListPreferenceDialogFragment extends
            CustomListPreference.CustomListPreferenceDialogFragment {
        private int mLastCheckedPosition = AdapterView.INVALID_POSITION;

        public static ListPreferenceDialogFragmentCompat newInstance(String key) {
            final ListPreferenceDialogFragmentCompat fragment
                    = new RestrictedListPreferenceDialogFragment();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        private RestrictedListPreference getCustomizablePreference() {
            return (RestrictedListPreference) getPreference();
        }

        @Override
        protected DialogInterface.OnClickListener getOnItemClickListener() {
            return new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    final RestrictedListPreference preference = getCustomizablePreference();
                    if (which < 0 || which >= preference.getEntryValues().length) {
                        return;
                    }
                    String entryValue = preference.getEntryValues()[which].toString();
                    RestrictedItem item = preference.getRestrictedItemForEntryValue(entryValue);
                    if (item != null) {
                        ListView listView = ((AlertDialog) dialog).getListView();
                        listView.setItemChecked(getLastCheckedPosition(), true);
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                                item.enforcedAdmin);
                    } else {
                        setClickedDialogEntryIndex(which);
                    }

                    if (getCustomizablePreference().isAutoClosePreference()) {
                        /*
                         * Clicking on an item simulates the positive button
                         * click, and dismisses the dialog.
                         */
                        RestrictedListPreferenceDialogFragment.this.onClick(dialog,
                                DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                }
            };
        }

        private int getLastCheckedPosition() {
            if (mLastCheckedPosition == AdapterView.INVALID_POSITION) {
                mLastCheckedPosition = ((RestrictedListPreference) getCustomizablePreference())
                        .getSelectedValuePos();
            }
            return mLastCheckedPosition;
        }

        private void setCheckedPosition(int checkedPosition) {
            mLastCheckedPosition = checkedPosition;
        }

        @Override
        protected void setClickedDialogEntryIndex(int which) {
            super.setClickedDialogEntryIndex(which);
            mLastCheckedPosition = which;
        }
    }

    public static class RestrictedItem {
        public final CharSequence entry;
        public final CharSequence entryValue;
        public final EnforcedAdmin enforcedAdmin;

        public RestrictedItem(CharSequence entry, CharSequence entryValue,
                EnforcedAdmin enforcedAdmin) {
            this.entry = entry;
            this.entryValue = entryValue;
            this.enforcedAdmin = enforcedAdmin;
        }
    }
}
