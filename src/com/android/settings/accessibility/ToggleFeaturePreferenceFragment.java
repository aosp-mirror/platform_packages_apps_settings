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

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.ToggleSwitch;

public abstract class ToggleFeaturePreferenceFragment
        extends SettingsPreferenceFragment {

    protected ToggleSwitch mToggleSwitch;

    protected String mPreferenceKey;
    protected Preference mSummaryPreference;

    protected CharSequence mSettingsTitle;
    protected Intent mSettingsIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                getActivity());
        setPreferenceScreen(preferenceScreen);
        mSummaryPreference = new Preference(getActivity()) {
                @Override
            protected void onBindView(View view) {
                super.onBindView(view);
                TextView summaryView = (TextView) view.findViewById(R.id.summary);
                summaryView.setText(getSummary());
                sendAccessibilityEvent(summaryView);
            }

            private void sendAccessibilityEvent(View view) {
                // Since the view is still not attached we create, populate,
                // and send the event directly since we do not know when it
                // will be attached and posting commands is not as clean.
                AccessibilityManager accessibilityManager =
                        AccessibilityManager.getInstance(getActivity());
                if (accessibilityManager.isEnabled()) {
                    AccessibilityEvent event = AccessibilityEvent.obtain();
                    event.setEventType(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    view.onInitializeAccessibilityEvent(event);
                    view.dispatchPopulateAccessibilityEvent(event);
                    accessibilityManager.sendAccessibilityEvent(event);
                }
            }
        };
        mSummaryPreference.setPersistent(false);
        mSummaryPreference.setLayoutResource(R.layout.text_description_preference);
        preferenceScreen.addPreference(mSummaryPreference);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getActivity().getActionBar().getThemedContext();
        final int padding = context.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mToggleSwitch = new ToggleSwitch(context);
        mToggleSwitch.setPaddingRelative(0, 0, padding, 0);
        mToggleSwitch.setLayoutParams(new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.END));

        onProcessArguments(getArguments());
    }

    @Override
    public void onResume() {
        super.onResume();

        installActionBarToggleSwitch();
    }

    @Override
    public void onPause() {
        removeActionBarToggleSwitch();

        super.onPause();
    }

    protected abstract void onPreferenceToggled(String preferenceKey, boolean enabled);

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem menuItem = menu.add(mSettingsTitle);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menuItem.setIntent(mSettingsIntent);
    }

    protected void onInstallActionBarToggleSwitch() {
        // Implement this to set a checked listener.
    }

    private void installActionBarToggleSwitch() {
        final ActionBar ab = getActivity().getActionBar();
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        ab.setCustomView(mToggleSwitch);

        onInstallActionBarToggleSwitch();
    }

    private void removeActionBarToggleSwitch() {
        mToggleSwitch.setOnBeforeCheckedChangeListener(null);

        final ActionBar ab = getActivity().getActionBar();
        ab.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        ab.setCustomView(null);
    }

    public void setTitle(String title) {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        getActivity().setTitle(title);
    }

    protected void onProcessArguments(Bundle arguments) {
        if (arguments == null) {
            getPreferenceScreen().removePreference(mSummaryPreference);
            return;
        }

        // Key.
        mPreferenceKey = arguments.getString(AccessibilitySettings.EXTRA_PREFERENCE_KEY);

        // Enabled.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_CHECKED)) {
            final boolean enabled = arguments.getBoolean(AccessibilitySettings.EXTRA_CHECKED);
            mToggleSwitch.setCheckedInternal(enabled);
        }

        // Title.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_TITLE)) {
            setTitle(arguments.getString(AccessibilitySettings.EXTRA_TITLE));
        }

        // Summary.
        if (arguments.containsKey(AccessibilitySettings.EXTRA_SUMMARY)) {
            final CharSequence summary = arguments.getCharSequence(
                    AccessibilitySettings.EXTRA_SUMMARY);
            mSummaryPreference.setSummary(summary);

            // Set a transparent drawable to prevent use of the default one.
            getListView().setSelector(new ColorDrawable(Color.TRANSPARENT));
            getListView().setDivider(null);
        } else {
            getPreferenceScreen().removePreference(mSummaryPreference);
        }
    }
}
