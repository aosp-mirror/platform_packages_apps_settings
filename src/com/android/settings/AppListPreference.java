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

/**
 * Extends ListPreference to allow us to show the icons for a given list of applications. We do this
 * because the names of applications are very similar and the user may not be able to determine what
 * app they are selecting without an icon.
 */
public class AppListPreference extends ListPreference {
    private Drawable[] mEntryDrawables;

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

    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName) {
        // Look up all package names in PackageManager. Skip ones we can't find.
        int foundPackages = 0;
        PackageManager pm = getContext().getPackageManager();
        ApplicationInfo[] appInfos = new ApplicationInfo[packageNames.length];
        for (int i = 0; i < packageNames.length; i++) {
            try {
                appInfos[i] = pm.getApplicationInfo(packageNames[i].toString(), 0);
                foundPackages++;
            } catch (NameNotFoundException e) {
                // Leave appInfos[i] uninitialized; it will be skipped in the list.
            }
        }

        // Show the label and icon for each application package.
        CharSequence[] applicationNames = new CharSequence[foundPackages];
        mEntryDrawables = new Drawable[foundPackages];
        int index = 0;
        int selectedIndex = -1;
        for (ApplicationInfo appInfo : appInfos) {
            if (appInfo != null) {
                applicationNames[index] = appInfo.loadLabel(pm);
                mEntryDrawables[index] = appInfo.loadIcon(pm);
                if (defaultPackageName != null &&
                        appInfo.packageName.contentEquals(defaultPackageName)) {
                    selectedIndex = index;
                }
                index++;
            }
        }
        setEntries(applicationNames);
        setEntryValues(packageNames);
        if (selectedIndex != -1) {
            setValueIndex(selectedIndex);
        } else {
            setValue(null);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        int selectedIndex = findIndexOfValue(getValue());
        ListAdapter adapter = new AppArrayAdapter(getContext(),
            R.layout.app_preference_item, getEntries(), mEntryDrawables, selectedIndex);
        builder.setAdapter(adapter, this);
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(getEntryValues(), getValue(), superState);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            setPackageNames(savedState.entryValues, savedState.value);
            super.onRestoreInstanceState(savedState.superState);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState implements Parcelable {

        public final CharSequence[] entryValues;
        public final CharSequence value;
        public final Parcelable superState;

        public SavedState(CharSequence[] entryValues, CharSequence value, Parcelable superState) {
            this.entryValues = entryValues;
            this.value = value;
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
            dest.writeParcelable(superState, flags);
        }

        public static Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                CharSequence[] entryValues = source.readCharSequenceArray();
                CharSequence value = source.readCharSequence();
                Parcelable superState = source.readParcelable(getClass().getClassLoader());
                return new SavedState(entryValues, value, superState);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
