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
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.core.lifecycle.events.OnCreate;
import com.android.settings.core.lifecycle.events.OnSaveInstanceState;
import com.android.settings.overlay.FeatureFactory;

import java.util.ArrayList;
import java.util.List;

public class ProgressiveDisclosureMixin implements Preference.OnPreferenceClickListener,
        LifecycleObserver, OnCreate, OnSaveInstanceState {

    private static final String TAG = "ProgressiveDisclosure";
    private static final String STATE_USER_EXPANDED = "state_user_expanded";
    private static final int DEFAULT_TILE_LIMIT = 3;

    private int mTileLimit = DEFAULT_TILE_LIMIT;

    private final DashboardFeatureProvider mDashboardFeatureProvider;
    private final List<Preference> collapsedPrefs = new ArrayList<>();
    private final ExpandPreference mExpandButton;
    private final PreferenceFragment mFragment;

    private boolean mUserExpanded;

    public ProgressiveDisclosureMixin(Context context, PreferenceFragment fragment) {
        mFragment = fragment;
        mExpandButton = new ExpandPreference(context);
        mExpandButton.setOnPreferenceClickListener(this);
        mDashboardFeatureProvider = FeatureFactory.getFactory(context)
                .getDashboardFeatureProvider(context);
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
                for (Preference pref : collapsedPrefs) {
                    screen.addPreference(pref);
                }
                mUserExpanded = true;
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
        return !collapsedPrefs.isEmpty();
    }

    /**
     * Whether the screen should be collapsed.
     */
    public boolean shouldCollapse(PreferenceScreen screen) {
        return mDashboardFeatureProvider.isEnabled() && screen.getPreferenceCount() >= mTileLimit
                && !mUserExpanded;
    }

    /**
     * Collapse extra preferences and show a "More" button
     */
    public void collapse(PreferenceScreen screen) {
        final int itemCount = screen.getPreferenceCount();
        if (!shouldCollapse(screen)) {
            return;
        }
        if (!collapsedPrefs.isEmpty()) {
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
     * Add preference to collapsed list.
     */
    public void addToCollapsedList(Preference preference) {
        collapsedPrefs.add(preference);
    }

    /**
     * Remove preference from collapsed list. If the preference is not in list, do nothing.
     */
    public void removePreference(PreferenceScreen screen, String key) {
        if (!isCollapsed()) {
            return;
        }
        for (int i = 0; i < collapsedPrefs.size(); i++) {
            final Preference pref = collapsedPrefs.get(i);
            if (TextUtils.equals(key, pref.getKey())) {
                collapsedPrefs.remove(pref);
                if (collapsedPrefs.isEmpty()) {
                    // Removed last element, remove expand button too.
                    screen.removePreference(mExpandButton);
                }
                return;
            }
        }
    }

    /**
     * Find whether a preference is in collapsed list.
     */
    public Preference findPreference(CharSequence key) {
        for (int i = 0; i < collapsedPrefs.size(); i++) {
            final Preference pref = collapsedPrefs.get(i);
            if (TextUtils.equals(key, pref.getKey())) {
                return pref;
            }
        }
        return null;
    }

}
