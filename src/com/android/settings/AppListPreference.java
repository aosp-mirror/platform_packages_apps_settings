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

import android.app.AlertDialog;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
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
public class AppListPreference extends CustomListPreference {

    public static final String ITEM_NONE_VALUE = "";

    protected final boolean mForWork;
    protected final int mUserId;

    private Drawable[] mEntryDrawables;
    private boolean mShowItemNone = false;
    private CharSequence[] mSummaries;
    private int mSystemAppIndex = -1;

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
        public boolean isEnabled(int position) {
            return mSummaries == null || mSummaries[position] == null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.app_preference_item, parent, false);
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            textView.setText(getItem(position));
            if (position == mSelectedIndex && position == mSystemAppIndex) {
                view.findViewById(R.id.system_default_label).setVisibility(View.VISIBLE);
            } else if (position == mSelectedIndex) {
                view.findViewById(R.id.default_label).setVisibility(View.VISIBLE);
            } else if (position == mSystemAppIndex) {
                view.findViewById(R.id.system_label).setVisibility(View.VISIBLE);
            }
            ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
            imageView.setImageDrawable(mImageDrawables[position]);
            // Summaries are describing why a item is disabled, so anything with a summary
            // is not enabled.
            boolean enabled = mSummaries == null || mSummaries[position] == null;
            view.setEnabled(enabled);
            if (!enabled) {
                TextView summary = (TextView) view.findViewById(android.R.id.summary);
                summary.setText(mSummaries[position]);
                summary.setVisibility(View.VISIBLE);
            }
            return view;
        }
    }

    public AppListPreference(Context context, AttributeSet attrs, int defStyle, int defAttrs) {
        super(context, attrs, defStyle, defAttrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WorkPreference, 0, 0);
        mForWork = a.getBoolean(R.styleable.WorkPreference_forWork, false);
        final UserHandle managedProfile = Utils.getManagedProfile(UserManager.get(context));
        mUserId = mForWork && managedProfile != null ? managedProfile.getIdentifier()
                : UserHandle.myUserId();
    }

    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WorkPreference, 0, 0);
        mForWork = a.getBoolean(R.styleable.WorkPreference_forWork, false);
        final UserHandle managedProfile = Utils.getManagedProfile(UserManager.get(context));
        mUserId = mForWork && managedProfile != null ? managedProfile.getIdentifier()
                : UserHandle.myUserId();
    }

    public void setShowItemNone(boolean showItemNone) {
        mShowItemNone = showItemNone;
    }

    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName) {
        setPackageNames(packageNames, defaultPackageName, null);
    }

    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName,
            CharSequence systemPackageName) {
        // Look up all package names in PackageManager. Skip ones we can't find.
        PackageManager pm = getContext().getPackageManager();
        final int entryCount = packageNames.length + (mShowItemNone ? 1 : 0);
        List<CharSequence> applicationNames = new ArrayList<>(entryCount);
        List<CharSequence> validatedPackageNames = new ArrayList<>(entryCount);
        List<Drawable> entryDrawables = new ArrayList<>(entryCount);
        int selectedIndex = -1;
        mSystemAppIndex = -1;
        for (int i = 0; i < packageNames.length; i++) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfoAsUser(packageNames[i].toString(), 0,
                        mUserId);
                applicationNames.add(appInfo.loadLabel(pm));
                validatedPackageNames.add(appInfo.packageName);
                entryDrawables.add(appInfo.loadIcon(pm));
                if (defaultPackageName != null &&
                        appInfo.packageName.contentEquals(defaultPackageName)) {
                    selectedIndex = i;
                }
                if (appInfo.packageName != null && systemPackageName != null &&
                        appInfo.packageName.contentEquals(systemPackageName)) {
                    mSystemAppIndex = i;
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

    public void setComponentNames(ComponentName[] componentNames, ComponentName defaultCN) {
        setComponentNames(componentNames, defaultCN, null);
    }

    public void setComponentNames(ComponentName[] componentNames, ComponentName defaultCN,
            CharSequence[] summaries) {
        mSummaries = summaries;
        // Look up all package names in PackageManager. Skip ones we can't find.
        PackageManager pm = getContext().getPackageManager();
        final int entryCount = componentNames.length + (mShowItemNone ? 1 : 0);
        List<CharSequence> applicationNames = new ArrayList<>(entryCount);
        List<CharSequence> validatedComponentNames = new ArrayList<>(entryCount);
        List<Drawable> entryDrawables = new ArrayList<>(entryCount);
        int selectedIndex = -1;
        for (int i = 0; i < componentNames.length; i++) {
            try {
                ActivityInfo activityInfo = AppGlobals.getPackageManager().getActivityInfo(
                        componentNames[i], 0, mUserId);
                if (activityInfo == null) continue;
                applicationNames.add(activityInfo.loadLabel(pm));
                validatedComponentNames.add(componentNames[i].flattenToString());
                entryDrawables.add(activityInfo.loadIcon(pm));
                if (defaultCN != null && componentNames[i].equals(defaultCN)) {
                    selectedIndex = i;
                }
            } catch (RemoteException e) {
                // Skip unknown packages.
            }
        }

        if (mShowItemNone) {
            applicationNames.add(
                    getContext().getResources().getText(R.string.app_list_preference_none));
            validatedComponentNames.add(ITEM_NONE_VALUE);
            entryDrawables.add(getContext().getDrawable(R.drawable.ic_remove_circle));
        }

        setEntries(applicationNames.toArray(new CharSequence[applicationNames.size()]));
        setEntryValues(
                validatedComponentNames.toArray(new CharSequence[validatedComponentNames.size()]));
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
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        builder.setAdapter(createListAdapter(), listener);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(getEntryValues(), getValue(), mSummaries, mShowItemNone, superState);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mShowItemNone = savedState.showItemNone;
            setPackageNames(savedState.entryValues, savedState.value);
            mSummaries = savedState.summaries;
            super.onRestoreInstanceState(savedState.superState);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    /**
     * Sets app label as summary if there is only 1 app applicable to this preference.
     */
    protected void setSoleAppLabelAsSummary() {
        final CharSequence soleLauncherLabel = getSoleAppLabel();
        if (!TextUtils.isEmpty(soleLauncherLabel)) {
            setSummary(soleLauncherLabel);
        }
    }

    /**
     * Returns app label if there is only 1 app applicable to this preference.
     */
    protected CharSequence getSoleAppLabel() {
        // Intentionally left empty so subclasses can override with necessary logic.
        return null;
    }

    private static class SavedState implements Parcelable {

        public final CharSequence[] entryValues;
        public final CharSequence value;
        public final boolean showItemNone;
        public final Parcelable superState;
        public final CharSequence[] summaries;

        public SavedState(CharSequence[] entryValues, CharSequence value, CharSequence[] summaries,
                boolean showItemNone, Parcelable superState) {
            this.entryValues = entryValues;
            this.value = value;
            this.showItemNone = showItemNone;
            this.superState = superState;
            this.summaries = summaries;
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
            dest.writeCharSequenceArray(summaries);
        }

        public static Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                CharSequence[] entryValues = source.readCharSequenceArray();
                CharSequence value = source.readCharSequence();
                boolean showItemNone = source.readInt() != 0;
                Parcelable superState = source.readParcelable(getClass().getClassLoader());
                CharSequence[] summaries = source.readCharSequenceArray();
                return new SavedState(entryValues, value, summaries, showItemNone, superState);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
