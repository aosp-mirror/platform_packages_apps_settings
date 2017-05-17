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

package com.android.settings.dashboard;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.Instrumentable;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProgressiveDisclosureMixin implements Preference.OnPreferenceClickListener,
        LifecycleObserver, OnCreate, OnSaveInstanceState {

    private static final String TAG = "ProgressiveDisclosure";
    private static final String STATE_USER_EXPANDED = "state_user_expanded";
    private static final int DEFAULT_TILE_LIMIT = 300;

    private final Context mContext;
    // Collapsed preference sorted by order.
    private final List<Preference> mCollapsedPrefs = new ArrayList<>();
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final PreferenceFragment mFragment;
    private /* final */ ExpandPreference mExpandButton;

    private int mTileLimit = DEFAULT_TILE_LIMIT;
    private boolean mUserExpanded;

    public ProgressiveDisclosureMixin(Context context,
            PreferenceFragment fragment, boolean keepExpanded) {
        mContext = context;
        mFragment = fragment;
        mExpandButton = new ExpandPreference(context);
        mExpandButton.setOnPreferenceClickListener(this);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mUserExpanded = keepExpanded;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mUserExpanded = savedInstanceState.getBoolean(STATE_USER_EXPANDED, false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_USER_EXPANDED, mUserExpanded);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof ExpandPreference) {
            final PreferenceScreen screen = mFragment.getPreferenceScreen();
            if (screen != null) {
                screen.removePreference(preference);
                for (Preference pref : mCollapsedPrefs) {
                    screen.addPreference(pref);
                }
                mCollapsedPrefs.clear();
                mUserExpanded = true;
                final int metricsCategory;
                if (mFragment instanceof Instrumentable) {
                    metricsCategory = ((Instrumentable) mFragment).getMetricsCategory();
                } else {
                    metricsCategory = MetricsProto.MetricsEvent.VIEW_UNKNOWN;
                }
                mMetricsFeatureProvider.actionWithSource(mContext, metricsCategory,
                        MetricsProto.MetricsEvent.ACTION_SETTINGS_ADVANCED_BUTTON_EXPAND);
            }
        }
        return false;
    }

    /**
     * Sets the threshold to start collapsing preferences when there are too many.
     */
    public void setTileLimit(int limit) {
        mTileLimit = limit;
    }

    /**
     * Whether the controller is in collapsed state.
     */
    public boolean isCollapsed() {
        return !mCollapsedPrefs.isEmpty();
    }

    /**
     * Whether the screen should be collapsed.
     */
    public boolean shouldCollapse(PreferenceScreen screen) {
        return !mUserExpanded && screen.getPreferenceCount() > mTileLimit;
    }

    /**
     * Collapse extra preferences and show a "More" button
     */
    public void collapse(PreferenceScreen screen) {
        final int itemCount = screen.getPreferenceCount();
        if (!shouldCollapse(screen)) {
            return;
        }
        if (!mCollapsedPrefs.isEmpty()) {
            Log.w(TAG, "collapsed list should ALWAYS BE EMPTY before collapsing!");
        }

        for (int i = itemCount - 1; i >= mTileLimit; i--) {
            final Preference preference = screen.getPreference(i);
            addToCollapsedList(preference);
            screen.removePreference(preference);
        }
        screen.addPreference(mExpandButton);
    }

    /**
     * Adds preference to screen. If there are too many preference on screen, adds it to
     * collapsed list instead.
     */
    public void addPreference(PreferenceScreen screen, Preference pref) {
        // Either add to screen, or to collapsed list.
        if (isCollapsed()) {
            // insert the preference to right position.
            final int lastPreferenceIndex = screen.getPreferenceCount() - 2;
            if (lastPreferenceIndex >= 0) {
                final Preference lastPreference = screen.getPreference(lastPreferenceIndex);
                if (lastPreference.getOrder() > pref.getOrder()) {
                    // insert to screen and move the last pref to collapsed list.
                    screen.removePreference(lastPreference);
                    screen.addPreference(pref);
                    addToCollapsedList(lastPreference);
                } else {
                    // Insert to collapsed list.
                    addToCollapsedList(pref);
                }
            } else {
                // Couldn't find last preference on screen, just add to collapsed list.
                addToCollapsedList(pref);
            }
        } else if (shouldCollapse(screen)) {
            // About to have too many tiles on scree, collapse and add pref to collapsed list.
            screen.addPreference(pref);
            collapse(screen);
        } else {
            // No need to collapse, add to screen directly.
            screen.addPreference(pref);
        }
    }

    /**
     * Removes preference. If the preference is on screen, remove it from screen. If the
     * preference is in collapsed list, remove it from list.
     */
    public void removePreference(PreferenceScreen screen, String key) {
        // Try removing from screen.
        final Preference preference = screen.findPreference(key);
        if (preference != null) {
            screen.removePreference(preference);
            return;
        }
        // Didn't find on screen, try removing from collapsed list.
        for (int i = 0; i < mCollapsedPrefs.size(); i++) {
            final Preference pref = mCollapsedPrefs.get(i);
            if (TextUtils.equals(key, pref.getKey())) {
                mCollapsedPrefs.remove(pref);
                if (mCollapsedPrefs.isEmpty()) {
                    // Removed last element, remove expand button too.
                    screen.removePreference(mExpandButton);
                } else {
                    updateExpandButtonSummary();
                }
                return;
            }
        }
    }

    /**
     * Finds preference by key, either from screen or from collapsed list.
     */
    public Preference findPreference(PreferenceScreen screen, CharSequence key) {
        Preference preference = screen.findPreference(key);
        if (preference != null) {
            return preference;
        }
        for (int i = 0; i < mCollapsedPrefs.size(); i++) {
            final Preference pref = mCollapsedPrefs.get(i);
            if (TextUtils.equals(key, pref.getKey())) {
                return pref;
            }
            if (pref instanceof PreferenceGroup) {
                final Preference returnedPreference = ((PreferenceGroup) pref).findPreference(key);
                if (returnedPreference != null) {
                    return returnedPreference;
                }
            }
        }
        Log.d(TAG, "Cannot find preference with key " + key);
        return null;
    }

    /**
     * Add preference to collapsed list.
     */
    @VisibleForTesting
    void addToCollapsedList(Preference preference) {
        // Insert preference based on it's order.
        int insertionIndex = Collections.binarySearch(mCollapsedPrefs, preference);
        if (insertionIndex < 0) {
            insertionIndex = insertionIndex * -1 - 1;
        }
        mCollapsedPrefs.add(insertionIndex, preference);
        updateExpandButtonSummary();
    }

    @VisibleForTesting
    List<Preference> getCollapsedPrefs() {
        return mCollapsedPrefs;
    }

    @VisibleForTesting
    void updateExpandButtonSummary() {
        final int size = mCollapsedPrefs.size();
        if (size == 0) {
            mExpandButton.setSummary(null);
        } else if (size == 1) {
            mExpandButton.setSummary(mCollapsedPrefs.get(0).getTitle());
        } else {
            CharSequence summary = mCollapsedPrefs.get(0).getTitle();
            for (int i = 1; i < size; i++) {
                final CharSequence nextSummary = mCollapsedPrefs.get(i).getTitle();
                if (!TextUtils.isEmpty(nextSummary)) {
                    summary = mContext.getString(R.string.join_many_items_middle, summary,
                            nextSummary);
                }
            }
            mExpandButton.setSummary(summary);
        }
    }
}
