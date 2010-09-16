/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Top-level settings activity to handle single pane and double pane UI layout.
 */
public class Settings extends Activity
        implements PreferenceFragment.OnPreferenceStartFragmentCallback,
        SettingsPreferenceFragment.OnStateListener,
        SettingsPreferenceFragment.FragmentStarter {

    private static final boolean DBG = false;

    private static final String TAG = "Settings";

    private static final String KEY_PARENT = "parent";
    private static final String KEY_CALL_SETTINGS = "call_settings";
    private static final String KEY_SYNC_SETTINGS = "sync_settings";
    private static final String KEY_SEARCH_SETTINGS = "search_settings";
    private static final String KEY_DOCK_SETTINGS = "dock_settings";

    private static final String KEY_OPERATOR_SETTINGS = "operator_settings";
    private static final String KEY_MANUFACTURER_SETTINGS = "manufacturer_settings";

    public static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";

    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";

    private static final String BACK_STACK_PREFS = ":settings:prefs";

    private View mPrefsPane;
    private View mMainPane;
    private boolean mSinglePane;

    private BreadCrumbs mBreadCrumbs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_top_level);
        mPrefsPane = findViewById(R.id.prefs);
        mMainPane = findViewById(R.id.top_level);
        mSinglePane = mMainPane == null;
        if (mSinglePane) mMainPane = mPrefsPane;

        final Intent intent = getIntent();
        String initialFragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);
        Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);

        createActionBar();

        if (mSinglePane) {
            if (initialFragment != null) {
                showFragment(initialFragment, initialArguments);
            } else {
                // Intent#getCompontent() lets us get Fragment name, even when the Intent is
                // given via <activity-alias>.
                //
                // e.g. When we reach here via "ChildSetting" activity-alias,
                // we should get the name here instead of targetActivity ("Settings").
                if (intent.getComponent().getClassName().equals(this.getClass().getName())) {
                    showFragment(TopLevelSettings.class.getName(), null);
                } else {
                    showFragment(intent.getComponent().getClassName(), intent.getExtras());
                }
            }
        } else {
            if (!intent.getComponent().getClassName().equals(this.getClass().getName())) {
                if (showFragment(intent.getComponent().getClassName(), intent.getExtras())) {
                    mMainPane.setVisibility(View.GONE);
                }
            } else {
                Fragment topLevel = getFragmentManager().findFragmentById(R.id.top_level);
                if (topLevel != null) {
                    ((TopLevelSettings) topLevel).selectFirst();
                }
            }
        }
    }

    private void createActionBar() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customNavBar = inflater.inflate(R.layout.settings_actionbar, null, false);
        getActionBar().setCustomNavigationMode(customNavBar);
        mBreadCrumbs = (BreadCrumbs) customNavBar.findViewById(R.id.bread_crumbs);
        mBreadCrumbs.setActivity(this);
    }

    boolean showFragment(Preference preference) {
        if (mSinglePane) {
            startWithFragment(preference.getFragment(), preference.getExtras());
            return false;
        } else {
            mBreadCrumbs.clear();
            return showFragment(preference.getFragment(), preference.getExtras());
        }
    }

    private void startWithFragment(String fragmentName, Bundle args) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(this, getClass());
        intent.putExtra(EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        startActivity(intent);
    }

    private boolean showFragment(String fragmentClass, Bundle extras) {
        if (DBG) Log.d(TAG, "showFragment");
       Fragment f = Fragment.instantiate(this, fragmentClass, extras);
        if (f instanceof SettingsPreferenceFragment) {
            SettingsPreferenceFragment spf = (SettingsPreferenceFragment) f;
            spf.setOnStateListener(this);
            spf.setFragmentStarter(this);
        }
        mBreadCrumbs.clear();
        getFragmentManager().popBackStack(BACK_STACK_PREFS, POP_BACK_STACK_INCLUSIVE);
        getFragmentManager().openTransaction().replace(R.id.prefs, f).commit();
        return true;
    }

    private void addToBreadCrumbs(Fragment fragment) {
        final CharSequence title = ((PreferenceFragment) fragment)
                .getPreferenceScreen().getTitle();
        if (mSinglePane) {
            mBreadCrumbs.clear();
        }
        mBreadCrumbs.push(title);
    }

    private void removeFromBreadCrumbs(Fragment fragment) {
        mBreadCrumbs.pop(((PreferenceFragment) fragment).getPreferenceScreen().getTitle());
        mBreadCrumbs.update();
    }

    public void onCreated(SettingsPreferenceFragment fragment) {
        if (DBG) Log.d(TAG, "Fragment created " + fragment);
        addToBreadCrumbs(fragment);
    }

    public void onDestroyed(SettingsPreferenceFragment fragment) {
        Log.d(TAG, "Fragment destroyed " + fragment + " (name: " + fragment.getClass() + ")");
    }

    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        if (DBG) Log.d(TAG, "onPreferenceStartFragment");
        return startFragment(caller, pref.getFragment(), -1, pref.getExtras());
    }

    public boolean startFragment(
            Fragment caller, String fragmentClass, int requestCode, Bundle extras) {
        Fragment f = Fragment.instantiate(this, fragmentClass, extras);
        caller.setTargetFragment(f, requestCode);
        if (f instanceof SettingsPreferenceFragment) {
            SettingsPreferenceFragment spf = (SettingsPreferenceFragment) f;
            spf.setOnStateListener(this);
            spf.setFragmentStarter(this);
        }
        getFragmentManager().openTransaction().replace(R.id.prefs, f)
                .addToBackStack(BACK_STACK_PREFS).commit();
        return true;
    }

    @Override
    public void onBackPressed() {
        mBreadCrumbs.pop();
        mBreadCrumbs.update();
        super.onBackPressed();
    }

    public static class TopLevelSettings extends PreferenceFragment {

        private IconPreferenceScreen mHighlightedPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings);

            updatePreferenceList();
        }

        @Override
        public void onResume() {
            super.onResume();

            updateCallSettings();
        }

        private void updateCallSettings() {
            Preference callSettings = findPreference(KEY_CALL_SETTINGS);
            // Might have been removed in non-voice-capable devices
            if (callSettings != null) {
                callSettings.setEnabled(!AirplaneModeEnabler.isAirplaneModeOn(getActivity()));
            }
        }

        private void updatePreferenceList() {
            final Activity activity = getActivity();
            PreferenceGroup parent = (PreferenceGroup) findPreference(KEY_PARENT);
            Preference dockSettings = parent.findPreference(KEY_DOCK_SETTINGS);
            if (activity.getResources().getBoolean(R.bool.has_dock_settings) == false
                    && dockSettings != null) {
                parent.removePreference(dockSettings);
            }

            Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(activity, parent,
                    KEY_OPERATOR_SETTINGS);
            Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(activity, parent,
                    KEY_MANUFACTURER_SETTINGS);

            Preference callSettings = parent.findPreference(KEY_CALL_SETTINGS);
            if (!Utils.isVoiceCapable(activity) && callSettings != null) {
                parent.removePreference(callSettings);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
            // If it is a fragment preference, replace the prefs pane in the 2 pane UI.
            final String fragmentClass = preference.getFragment();
            if (fragmentClass != null) {
                boolean showed = ((Settings) getActivity()).showFragment(preference);
                if (showed) {
                    highlight(preference);
                }
                return showed;
            }
            return false;
        }

        void highlight(Preference preference) {
            if (mHighlightedPreference != null) {
                mHighlightedPreference.setHighlighted(false);
            }
            mHighlightedPreference = (IconPreferenceScreen) preference;
            mHighlightedPreference.setHighlighted(true);
        }

        void selectFirst() {
            Preference first = getPreferenceScreen().getPreference(0);
            onPreferenceTreeClick(getPreferenceScreen(), first);
        }
    }

    public static class BreadCrumbs extends LinearLayout implements OnClickListener {

        private ArrayList<CharSequence> mTitles = new ArrayList<CharSequence>();
        private TextView mLevelUpTitle;
        private TextView mCurrentLevelTitle;
        private View mDivider;
        private Activity mActivity;

        public BreadCrumbs(Context context) {
            this(context, null);
        }

        public BreadCrumbs(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void push(CharSequence title) {
            if (mTitles.size() == 0
                    || !TextUtils.equals(title, mTitles.get(mTitles.size() - 1))) {
                mTitles.add(title);
                update();
            }
        }

        public void pop() {
            if (mTitles.size() > 0) {
                mTitles.remove(mTitles.size() - 1);
            }
        }

        public void pop(CharSequence title) {
            if (mTitles.size() > 1) {
                mTitles.remove(title);
            }
        }

        public void clear() {
            mTitles.clear();
        }

        private void initNavViews() {
            if (mLevelUpTitle == null) {
                mLevelUpTitle = (TextView) findViewById(R.id.level_up_title);
                mCurrentLevelTitle = (TextView) findViewById(R.id.level_current_title);
                mDivider = findViewById(R.id.level_divider);
                if (mLevelUpTitle != null) {
                    mLevelUpTitle.setOnClickListener(this);
                }
                if (mCurrentLevelTitle != null) {
                    mCurrentLevelTitle.setOnClickListener(this);
                }
            }
        }

        public void update() {
            initNavViews();
            if (mLevelUpTitle == null) return;

            final int titleCount = mTitles.size();
            if (titleCount > 1) {
                mLevelUpTitle.setText(mTitles.get(titleCount - 2));
                mLevelUpTitle.setVisibility(VISIBLE);
                mDivider.setVisibility(VISIBLE);
            } else {
                mLevelUpTitle.setVisibility(GONE);
                mDivider.setVisibility(GONE);
            }
            if (titleCount > 0) {
                mCurrentLevelTitle.setText(mTitles.get(titleCount - 1));
            } else {
                mCurrentLevelTitle.setText("");
            }
        }

        public void setActivity(Activity activity) {
            mActivity = activity;
        }

        public void onClick(View v) {
            if (mActivity == null)
                return;
            if (v == mLevelUpTitle) {
                mActivity.onBackPressed();
            }
        }
    }
}
