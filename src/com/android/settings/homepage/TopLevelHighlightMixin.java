/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.homepage;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.SettingsActivity;
import com.android.settings.widget.HighlightableTopLevelPreferenceAdapter;

/** A highlight mixin for the top level settings fragment. */
public class TopLevelHighlightMixin implements Parcelable, DialogInterface.OnShowListener,
        DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

    private static final String TAG = "TopLevelHighlightMixin";

    private String mCurrentKey;
    // Stores the previous key for the profile select dialog cancel event
    private String mPreviousKey;
    // Stores the key hidden for the search page presence
    private String mHiddenKey;
    private DialogInterface mDialog;
    private HighlightableTopLevelPreferenceAdapter mTopLevelAdapter;
    private boolean mActivityEmbedded;

    public TopLevelHighlightMixin(boolean activityEmbedded) {
        mActivityEmbedded = activityEmbedded;
    }

    public TopLevelHighlightMixin(Parcel source) {
        mCurrentKey = source.readString();
        mPreviousKey = source.readString();
        mHiddenKey = source.readString();
        mActivityEmbedded = source.readBoolean();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCurrentKey);
        dest.writeString(mPreviousKey);
        dest.writeString(mHiddenKey);
        dest.writeBoolean(mActivityEmbedded);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TopLevelHighlightMixin> CREATOR = new Creator<>() {
        @Override
        public TopLevelHighlightMixin createFromParcel(Parcel source) {
            return new TopLevelHighlightMixin(source);
        }

        @Override
        public TopLevelHighlightMixin[] newArray(int size) {
            return new TopLevelHighlightMixin[size];
        }
    };

    @Override
    public void onShow(DialogInterface dialog) {
        mDialog = dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDialog = null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (mTopLevelAdapter != null) {
            mCurrentKey = mPreviousKey;
            mPreviousKey = null;
            mTopLevelAdapter.highlightPreference(mCurrentKey, /* scrollNeeded= */ false);
        }
    }

    void setActivityEmbedded(boolean activityEmbedded) {
        mActivityEmbedded = activityEmbedded;
    }

    boolean isActivityEmbedded() {
        return mActivityEmbedded;
    }

    RecyclerView.Adapter onCreateAdapter(TopLevelSettings topLevelSettings,
            PreferenceScreen preferenceScreen, boolean scrollNeeded) {
        if (TextUtils.isEmpty(mCurrentKey)) {
            mCurrentKey = getHighlightPrefKeyFromArguments(topLevelSettings.getArguments());
        }

        Log.d(TAG, "onCreateAdapter, pref key: " + mCurrentKey);

        // Remove the animator to avoid a RecyclerView crash.
        RecyclerView recyclerView = topLevelSettings.getListView();
        recyclerView.setItemAnimator(null);

        mTopLevelAdapter = new HighlightableTopLevelPreferenceAdapter(
                (SettingsHomepageActivity) topLevelSettings.getActivity(), preferenceScreen,
                recyclerView, mCurrentKey, scrollNeeded);
        return mTopLevelAdapter;
    }

    void reloadHighlightMenuKey(Bundle arguments) {
        if (mTopLevelAdapter == null) {
            return;
        }
        ensureDialogDismissed();

        mCurrentKey = getHighlightPrefKeyFromArguments(arguments);
        Log.d(TAG, "reloadHighlightMenuKey, pref key: " + mCurrentKey);
        mTopLevelAdapter.highlightPreference(mCurrentKey, /* scrollNeeded= */ true);
    }

    void setHighlightPreferenceKey(String prefKey) {
        if (mTopLevelAdapter != null) {
            ensureDialogDismissed();
            mPreviousKey = mCurrentKey;
            mCurrentKey = prefKey;
            mTopLevelAdapter.highlightPreference(prefKey, /* scrollNeeded= */ false);
        }
    }

    String getHighlightPreferenceKey() {
        return mCurrentKey;
    }

    void highlightPreferenceIfNeeded() {
        if (mTopLevelAdapter != null) {
            mTopLevelAdapter.requestHighlight();
        }
    }

    void setMenuHighlightShowed(boolean show) {
        if (mTopLevelAdapter == null) {
            return;
        }
        ensureDialogDismissed();

        if (show) {
            mCurrentKey = mHiddenKey;
            mHiddenKey = null;
        } else {
            if (mHiddenKey == null) {
                mHiddenKey = mCurrentKey;
            }
            mCurrentKey = null;
        }
        mTopLevelAdapter.highlightPreference(mCurrentKey, /* scrollNeeded= */ show);
    }

    void setHighlightMenuKey(String menuKey, boolean scrollNeeded) {
        if (mTopLevelAdapter == null) {
            return;
        }
        ensureDialogDismissed();

        final String prefKey = HighlightableMenu.lookupPreferenceKey(menuKey);
        if (TextUtils.isEmpty(prefKey)) {
            Log.e(TAG, "Invalid highlight menu key: " + menuKey);
        } else {
            Log.d(TAG, "Menu key: " + menuKey);
            mCurrentKey = prefKey;
            mTopLevelAdapter.highlightPreference(prefKey, scrollNeeded);
        }
    }

    private static String getHighlightPrefKeyFromArguments(Bundle arguments) {
        final String menuKey = arguments.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
        final String prefKey = HighlightableMenu.lookupPreferenceKey(menuKey);
        if (TextUtils.isEmpty(prefKey)) {
            Log.e(TAG, "Invalid highlight menu key: " + menuKey);
        } else {
            Log.d(TAG, "Menu key: " + menuKey);
        }
        return prefKey;
    }

    private void ensureDialogDismissed() {
        if (mDialog != null) {
            onCancel(mDialog);
            mDialog.dismiss();
        }
    }
}
