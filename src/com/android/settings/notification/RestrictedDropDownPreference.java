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

package com.android.settings.notification;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreferenceHelper;

import java.util.ArrayList;
import java.util.List;

public class RestrictedDropDownPreference extends DropDownPreference {
    private final RestrictedPreferenceHelper mHelper;
    private ReselectionSpinner mSpinner;
    private List<RestrictedItem> mRestrictedItems = new ArrayList<>();
    private boolean mUserClicked = false;
    private OnPreferenceClickListener mPreClickListener;

    public RestrictedDropDownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.restricted_preference_dropdown);
        setWidgetLayoutResource(R.layout.restricted_icon);
        mHelper = new RestrictedPreferenceHelper(context, this, attrs);
    }

    @Override
    protected ArrayAdapter createAdapter() {
        return new RestrictedArrayItemAdapter(getContext());
    }

    @Override
    public void setValue(String value) {
        if (getRestrictedItemForEntryValue(value) != null) {
            return;
        }
        super.setValue(value);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        mSpinner = (ReselectionSpinner) view.itemView.findViewById(R.id.spinner);
        mSpinner.setPreference(this);
        super.onBindViewHolder(view);
        mHelper.onBindViewHolder(view);
        mSpinner.setOnItemSelectedListener(mItemSelectedListener);
        final View restrictedIcon = view.findViewById(R.id.restricted_icon);
        if (restrictedIcon != null) {
            restrictedIcon.setVisibility(isDisabledByAdmin() ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isRestrictedForEntry(CharSequence entry) {
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

    private RestrictedItem getRestrictedItemForPosition(int position) {
        if (position < 0 || position >= getEntryValues().length) {
            return null;
        }
        CharSequence entryValue = getEntryValues()[position];
        return getRestrictedItemForEntryValue(entryValue);
    }

    public void addRestrictedItem(RestrictedItem item) {
        mRestrictedItems.add(item);
    }

    public void clearRestrictedItems() {
        mRestrictedItems.clear();
    }

    @Override
    public void performClick() {
        if (mPreClickListener != null && mPreClickListener.onPreferenceClick(this)) {
            return;
        }
        if (!mHelper.performClick()) {
            mUserClicked = true;
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

    /**
     * Similar to {@link #setOnPreferenceClickListener(OnPreferenceClickListener)}, but can
     * preempt {@link #onClick()}.
     */
    public void setOnPreClickListener(OnPreferenceClickListener l) {
        mPreClickListener = l;
    }

    public boolean isDisabledByAdmin() {
        return mHelper.isDisabledByAdmin();
    }

    private void setUserClicked(boolean userClicked) {
        mUserClicked = userClicked;
    }

    private boolean isUserClicked() {
        return mUserClicked;
    }

    private final OnItemSelectedListener mItemSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
            if (mUserClicked) {
                mUserClicked = false;
            } else {
                return;
            }
            if (position >= 0 && position < getEntryValues().length) {
                String value = getEntryValues()[position].toString();
                RestrictedItem item = getRestrictedItemForEntryValue(value);
                if (item != null) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                            item.enforcedAdmin);
                    mSpinner.setSelection(findIndexOfValue(getValue()));
                } else if (!value.equals(getValue()) && callChangeListener(value)) {
                    setValue(value);
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // noop
        }
    };

    /**
     * Extension of {@link ArrayAdapter} which updates the state of the dropdown item
     * depending on whether it is restricted by the admin.
     */
    private class RestrictedArrayItemAdapter extends ArrayAdapter<String> {
        private static final int TEXT_RES_ID = android.R.id.text1;
        public RestrictedArrayItemAdapter(Context context) {
            super(context, R.layout.spinner_dropdown_restricted_item, TEXT_RES_ID);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rootView = super.getView(position, convertView, parent);
            CharSequence entry = getItem(position);
            boolean isEntryRestricted = isRestrictedForEntry(entry);
            TextView text = (TextView) rootView.findViewById(TEXT_RES_ID);
            if (text != null) {
                text.setEnabled(!isEntryRestricted);
            }
            View restrictedIcon = rootView.findViewById(R.id.restricted_icon);
            if (restrictedIcon != null) {
                restrictedIcon.setVisibility(isEntryRestricted ? View.VISIBLE : View.GONE);
            }
            return rootView;
        }
    }

    /**
     * Extension of {@link Spinner} which triggers the admin support dialog on user clicking a
     * restricted item even if was already selected.
     */
    public static class ReselectionSpinner extends Spinner {
        private RestrictedDropDownPreference pref;

        public ReselectionSpinner(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setPreference(RestrictedDropDownPreference pref) {
            this.pref = pref;
        }

        @Override
        public void setSelection(int position) {
            int previousSelectedPosition = getSelectedItemPosition();
            super.setSelection(position);
            if (position == previousSelectedPosition && pref.isUserClicked()) {
                pref.setUserClicked(false);
                RestrictedItem item = pref.getRestrictedItemForPosition(position);
                if (item != null) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                            item.enforcedAdmin);
                }
            }
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