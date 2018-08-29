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

package com.android.settings.accessibility;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import androidx.appcompat.app.AlertDialog.Builder;

import com.android.settingslib.CustomDialogPreferenceCompat;

/**
 * Abstract dialog preference that displays a set of values and optional titles.
 */
public abstract class ListDialogPreference extends CustomDialogPreferenceCompat {
    private CharSequence[] mEntryTitles;
    private int[] mEntryValues;

    private OnValueChangedListener mOnValueChangedListener;

    /** The layout resource to use for grid items. */
    private int mListItemLayout;

    /** The current value of this preference. */
    private int mValue;

    /** The index within the value set of the current value. */
    private int mValueIndex;

    /** Whether the value had been set using {@link #setValue}. */
    private boolean mValueSet;

    public ListDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets a listened to invoke when the value of this preference changes.
     *
     * @param listener the listener to invoke
     */
    public void setOnValueChangedListener(OnValueChangedListener listener) {
        mOnValueChangedListener = listener;
    }

    /**
     * Sets the layout to use for grid items.
     *
     * @param layoutResId the layout to use for displaying grid items
     */
    public void setListItemLayoutResource(int layoutResId) {
        mListItemLayout = layoutResId;
    }

    /**
     * Sets the list of item values. Values must be distinct.
     *
     * @param values the list of item values
     */
    public void setValues(int[] values) {
        mEntryValues = values;

        if (mValueSet && mValueIndex == AbsListView.INVALID_POSITION) {
            mValueIndex = getIndexForValue(mValue);
        }
    }

    /**
     * Sets the list of item titles. May be null if no titles are specified, or
     * may be shorter than the list of values to leave some titles unspecified.
     *
     * @param titles the list of item titles
     */
    public void setTitles(CharSequence[] titles) {
        mEntryTitles = titles;
    }

    /**
     * Populates a list item view with data for the specified index.
     *
     * @param view the view to populate
     * @param index the index for which to populate the view
     * @see #setListItemLayoutResource(int)
     * @see #getValueAt(int)
     * @see #getTitleAt(int)
     */
    protected abstract void onBindListItem(View view, int index);

    /**
     * @return the title at the specified index, or null if none specified
     */
    protected CharSequence getTitleAt(int index) {
        if (mEntryTitles == null || mEntryTitles.length <= index) {
            return null;
        }

        return mEntryTitles[index];
    }

    /**
     * @return the value at the specified index
     */
    protected int getValueAt(int index) {
        return mEntryValues[index];
    }

    @Override
    public CharSequence getSummary() {
        if (mValueIndex >= 0) {
            return getTitleAt(mValueIndex);
        }

        return null;
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder,
            DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);

        final Context context = getContext();
        final int dialogLayout = getDialogLayoutResource();
        final View picker = LayoutInflater.from(context).inflate(dialogLayout, null);
        final ListPreferenceAdapter adapter = new ListPreferenceAdapter();
        final AbsListView list = (AbsListView) picker.findViewById(android.R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                if (callChangeListener((int) id)) {
                    setValue((int) id);
                }

                final Dialog dialog = getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Set initial selection.
        final int selectedPosition = getIndexForValue(mValue);
        if (selectedPosition != AbsListView.INVALID_POSITION) {
            list.setSelection(selectedPosition);
        }

        builder.setView(picker);
        builder.setPositiveButton(null, null);
    }

    /**
     * @return the index of the specified value within the list of entry values,
     *         or {@link AbsListView#INVALID_POSITION} if not found
     */
    protected int getIndexForValue(int value) {
        final int[] values = mEntryValues;
        if (values != null) {
            final int count = values.length;
            for (int i = 0; i < count; i++) {
                if (values[i] == value) {
                    return i;
                }
            }
        }

        return AbsListView.INVALID_POSITION;
    }

    /**
     * Sets the current value. If the value exists within the set of entry
     * values, updates the selection index.
     *
     * @param value the value to set
     */
    public void setValue(int value) {
        final boolean changed = mValue != value;
        if (changed || !mValueSet) {
            mValue = value;
            mValueIndex = getIndexForValue(value);
            mValueSet = true;
            persistInt(value);
            if (changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
            if (mOnValueChangedListener != null) {
                mOnValueChangedListener.onValueChanged(this, value);
            }
        }
    }

    /**
     * @return the current value
     */
    public int getValue() {
        return mValue;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = getValue();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.value);
    }

    private class ListPreferenceAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        @Override
        public int getCount() {
            return mEntryValues.length;
        }

        @Override
        public Integer getItem(int position) {
            return mEntryValues[position];
        }

        @Override
        public long getItemId(int position) {
            return mEntryValues[position];
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                if (mInflater == null) {
                    mInflater = LayoutInflater.from(parent.getContext());
                }
                convertView = mInflater.inflate(mListItemLayout, parent, false);
            }
            onBindListItem(convertView, position);
            return convertView;
        }
    }

    private static class SavedState extends BaseSavedState {
        public int value;

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings({ "hiding", "unused" })
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public interface OnValueChangedListener {
        public void onValueChanged(ListDialogPreference preference, int value);
    }
}
