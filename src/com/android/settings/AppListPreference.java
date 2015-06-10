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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends ListPreference to allow us to show the icons for a given list of applications. We do this
 * because the names of applications are very similar and the user may not be able to determine what
 * app they are selecting without an icon.
 */
public class AppListPreference extends ListPreference {

    public static final String ITEM_NONE_VALUE = "";

    private Drawable[] mEntryDrawables;
    private boolean mShowItemNone = false;

    public class AppArrayAdapter extends ArrayAdapter<CharSequence> {
        private Drawable[] mImageDrawables = null;
        private int mSelectedIndex = 0;

        public AppArrayAdapter(Context context, int textViewResourceId,
                CharSequence[] objects, Drawable[] imageDrawables, int selectedIndex) {
            super(context, textViewResourceId, objects);
            mSelectedIndex = selectedIndex;
            mImageDrawables = imageDrawables;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
            View view = inflater.inflate(R.layout.app_preference_item, parent, false);
            TextView textView = (TextView) view.findViewById(R.id.app_label);
            textView.setText(getItem(position));
            if (position == mSelectedIndex) {
                view.findViewById(R.id.default_label).setVisibility(View.VISIBLE);
            }
            ImageView imageView = (ImageView)view.findViewById(R.id.app_image);
            imageView.setImageDrawable(mImageDrawables[position]);
            return view;
        }
    }

    public AppListPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setShowItemNone(boolean showItemNone) {
        mShowItemNone = showItemNone;
    }

    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName) {
        // Look up all package names in PackageManager. Skip ones we can't find.
        PackageManager pm = getContext().getPackageManager();
        final int entryCount = packageNames.length + (mShowItemNone ? 1 : 0);
        List<CharSequence> applicationNames = new ArrayList<>(entryCount);
        List<CharSequence> validatedPackageNames = new ArrayList<>(entryCount);
        List<Drawable> entryDrawables = new ArrayList<>(entryCount);
        int selectedIndex = -1;
        for (int i = 0; i < packageNames.length; i++) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageNames[i].toString(), 0);
                applicationNames.add(appInfo.loadLabel(pm));
                validatedPackageNames.add(appInfo.packageName);
                entryDrawables.add(appInfo.loadIcon(pm));
                if (defaultPackageName != null &&
                        appInfo.packageName.contentEquals(defaultPackageName)) {
                    selectedIndex = i;
                }
            } catch (NameNotFoundException e) {
                // Skip unknown packages.
            }
        }

        if (mShowItemNone) {
            applicationNames.add(
                    getContext().getResources().getText(R.string.app_list_preference_none));
            validatedPackageNames.add(ITEM_NONE_VALUE);
            entryDrawables.add(getContext().getDrawable(R.drawable.ic_remove_circle));
        }

        setEntries(applicationNames.toArray(new CharSequence[applicationNames.size()]));
        setEntryValues(
                validatedPackageNames.toArray(new CharSequence[validatedPackageNames.size()]));
        mEntryDrawables = entryDrawables.toArray(new Drawable[entryDrawables.size()]);

        if (selectedIndex != -1) {
            setValueIndex(selectedIndex);
        } else {
            setValue(null);
        }
    }

    protected ListAdapter createListAdapter() {
        final String selectedValue = getValue();
        final boolean selectedNone = selectedValue == null ||
                (mShowItemNone && selectedValue.contentEquals(ITEM_NONE_VALUE));
        int selectedIndex = selectedNone ? -1 : findIndexOfValue(selectedValue);
        return new AppArrayAdapter(getContext(),
            R.layout.app_preference_item, getEntries(), mEntryDrawables, selectedIndex);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        builder.setAdapter(createListAdapter(), this);
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(getEntryValues(), getValue(), mShowItemNone, superState);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mShowItemNone = savedState.showItemNone;
            setPackageNames(savedState.entryValues, savedState.value);
            super.onRestoreInstanceState(savedState.superState);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState implements Parcelable {

        public final CharSequence[] entryValues;
        public final CharSequence value;
        public final boolean showItemNone;
        public final Parcelable superState;

        public SavedState(CharSequence[] entryValues, CharSequence value, boolean showItemNone,
                Parcelable superState) {
            this.entryValues = entryValues;
            this.value = value;
            this.showItemNone = showItemNone;
            this.superState = superState;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeCharSequenceArray(entryValues);
            dest.writeCharSequence(value);
            dest.writeInt(showItemNone ? 1 : 0);
            dest.writeParcelable(superState, flags);
        }

        public static Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                CharSequence[] entryValues = source.readCharSequenceArray();
                CharSequence value = source.readCharSequence();
                boolean showItemNone = source.readInt() != 0;
                Parcelable superState = source.readParcelable(getClass().getClassLoader());
                return new SavedState(entryValues, value, showItemNone, superState);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
