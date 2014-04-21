/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroupAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Base class for Settings fragments, with some helper functions and dialog management.
 */
public class SettingsPreferenceFragment extends PreferenceFragment implements DialogCreatable {

    private static final String TAG = "SettingsPreferenceFragment";

    private static final int MENU_HELP = Menu.FIRST + 100;
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 400;

    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    private SettingsDialogFragment mDialogFragment;

    private String mHelpUrl;

    // Cache the content resolver for async callbacks
    private ContentResolver mContentResolver;

    private String mPreferenceKey;
    private boolean mPreferenceHighlighted = false;

    private boolean mIsDataSetObserverRegistered = false;
    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            highlightPreferenceIfNeeded();
        }

        @Override
        public void onInvalidated() {
            highlightPreferenceIfNeeded();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mPreferenceHighlighted = icicle.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }

        // Prepare help url and enable menu if necessary
        int helpResource = getHelpResource();
        if (helpResource != 0) {
            mHelpUrl = getResources().getString(helpResource);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!TextUtils.isEmpty(mHelpUrl)) {
            setHasOptionsMenu(true);
        }

        final Bundle args = getArguments();
        if (args != null) {
            mPreferenceKey = args.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
            highlightPreferenceIfNeeded();
        }
    }

    @Override
    protected void onBindPreferences() {
        if (!mIsDataSetObserverRegistered) {
            getPreferenceScreen().getRootAdapter().registerDataSetObserver(mDataSetObserver);
            mIsDataSetObserverRegistered = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mIsDataSetObserverRegistered) {
            getPreferenceScreen().getRootAdapter().unregisterDataSetObserver(mDataSetObserver);
            mIsDataSetObserverRegistered = false;
        }
    }

    public void highlightPreferenceIfNeeded() {
        if (isAdded() && !mPreferenceHighlighted &&!TextUtils.isEmpty(mPreferenceKey)) {
            highlightPreference(mPreferenceKey);
        }
    }

    private Drawable getHighlightDrawable() {
        return getResources().getDrawable(R.drawable.preference_highlight);
    }

    /**
     * Return a valid ListView position or -1 if none is found
     */
    private int canUseListViewForHighLighting(String key) {
        if (!hasListView()) {
            return -1;
        }

        ListView listView = getListView();
        ListAdapter adapter = listView.getAdapter();

        if (adapter != null && adapter instanceof PreferenceGroupAdapter) {
            return findListPositionFromKey(adapter, key);
        }

        return -1;
    }

    private void highlightPreference(String key) {
        final Drawable highlight = getHighlightDrawable();

        final int position = canUseListViewForHighLighting(key);
        if (position >= 0) {
            final ListView listView = getListView();
            final ListAdapter adapter = listView.getAdapter();

            ((PreferenceGroupAdapter) adapter).setHighlightedDrawable(highlight);
            ((PreferenceGroupAdapter) adapter).setHighlighted(position);

            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.setSelection(position);
                    listView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            final int centerX = listView.getWidth() / 2;
                            final int centerY = listView.getChildAt(0).getHeight() / 2;
                            highlight.setHotspot(0, centerX, centerY);
                            highlight.clearHotspots();
                            ((PreferenceGroupAdapter) adapter).setHighlighted(-1);
                        }
                    }, DELAY_HIGHLIGHT_DURATION_MILLIS);

                    mPreferenceHighlighted = true;
                }
            });
        } else {
            // Try locating the Preference View thru its tag
            View preferenceView = findPreferenceViewForKey(getView(), key);
            if (preferenceView != null ) {
                preferenceView.setBackground(highlight);
                final int centerX = preferenceView.getWidth() / 2;
                final int centerY = preferenceView.getHeight() / 2;
                highlight.setHotspot(0, centerX, centerY);
                highlight.clearHotspots();
            }
        }
    }

    private int findListPositionFromKey(ListAdapter adapter, String key) {
        final int count = adapter.getCount();
        for (int n = 0; n < count; n++) {
            final Object item = adapter.getItem(n);
            if (item instanceof Preference) {
                Preference preference = (Preference) item;
                final String preferenceKey = preference.getKey();
                if (preferenceKey != null && preferenceKey.equals(key)) {
                    return n;
                }
            }
        }
        return -1;
    }

    private View findPreferenceViewForKey(View root, String key) {
        if (checkTag(root, key)) {
            return root;
        }
        if (root instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) root;
            final int count = group.getChildCount();
            for (int n = 0; n < count; n++) {
                final View child = group.getChildAt(n);
                final View view = findPreferenceViewForKey(child, key);
                if (view != null) {
                    return view;
                }
            }
        }
        return null;
    }

    private boolean checkTag(View view, String key) {
        final Object tag = view.getTag();
        if (tag == null || !(tag instanceof String)) {
            return false;
        }
        final String prefKey = (String) tag;
        return (!TextUtils.isEmpty(prefKey) && prefKey.equals(key));
    }

    protected void removePreference(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * Override this if you want to show a help item in the menu, by returning the resource id.
     * @return the resource id for the help url
     */
    protected int getHelpResource() {
        return 0;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mHelpUrl != null && getActivity() != null) {
            MenuItem helpItem = menu.add(0, MENU_HELP, 0, R.string.help_label);
            HelpUtils.prepareHelpMenuItem(getActivity(), helpItem, mHelpUrl);
        }
    }

    /*
     * The name is intentionally made different from Activity#finish(), so that
     * users won't misunderstand its meaning.
     */
    public final void finishFragment() {
        getActivity().onBackPressed();
    }

    // Some helpers for functions used by the settings fragments when they were activities

    /**
     * Returns the ContentResolver from the owning Activity.
     */
    protected ContentResolver getContentResolver() {
        Context context = getActivity();
        if (context != null) {
            mContentResolver = context.getContentResolver();
        }
        return mContentResolver;
    }

    /**
     * Returns the specified system service from the owning Activity.
     */
    protected Object getSystemService(final String name) {
        return getActivity().getSystemService(name);
    }

    /**
     * Returns the PackageManager from the owning Activity.
     */
    protected PackageManager getPackageManager() {
        return getActivity().getPackageManager();
    }

    @Override
    public void onDetach() {
        if (isRemoving()) {
            if (mDialogFragment != null) {
                mDialogFragment.dismiss();
                mDialogFragment = null;
            }
        }
        super.onDetach();
    }

    // Dialog management

    protected void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = new SettingsDialogFragment(this, dialogId);
        mDialogFragment.show(getChildFragmentManager(), Integer.toString(dialogId));
    }

    public Dialog onCreateDialog(int dialogId) {
        return null;
    }

    protected void removeDialog(int dialogId) {
        // mDialogFragment may not be visible yet in parent fragment's onResume().
        // To be able to dismiss dialog at that time, don't check
        // mDialogFragment.isVisible().
        if (mDialogFragment != null && mDialogFragment.getDialogId() == dialogId) {
            mDialogFragment.dismiss();
        }
        mDialogFragment = null;
    }

    /**
     * Sets the OnCancelListener of the dialog shown. This method can only be
     * called after showDialog(int) and before removeDialog(int). The method
     * does nothing otherwise.
     */
    protected void setOnCancelListener(DialogInterface.OnCancelListener listener) {
        if (mDialogFragment != null) {
            mDialogFragment.mOnCancelListener = listener;
        }
    }

    /**
     * Sets the OnDismissListener of the dialog shown. This method can only be
     * called after showDialog(int) and before removeDialog(int). The method
     * does nothing otherwise.
     */
    protected void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        if (mDialogFragment != null) {
            mDialogFragment.mOnDismissListener = listener;
        }
    }

    public void onDialogShowing() {
        // override in subclass to attach a dismiss listener, for instance
    }

    public static class SettingsDialogFragment extends DialogFragment {
        private static final String KEY_DIALOG_ID = "key_dialog_id";
        private static final String KEY_PARENT_FRAGMENT_ID = "key_parent_fragment_id";

        private int mDialogId;

        private Fragment mParentFragment;

        private DialogInterface.OnCancelListener mOnCancelListener;
        private DialogInterface.OnDismissListener mOnDismissListener;

        public SettingsDialogFragment() {
            /* do nothing */
        }

        public SettingsDialogFragment(DialogCreatable fragment, int dialogId) {
            mDialogId = dialogId;
            if (!(fragment instanceof Fragment)) {
                throw new IllegalArgumentException("fragment argument must be an instance of "
                        + Fragment.class.getName());
            }
            mParentFragment = (Fragment) fragment;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if (mParentFragment != null) {
                outState.putInt(KEY_DIALOG_ID, mDialogId);
                outState.putInt(KEY_PARENT_FRAGMENT_ID, mParentFragment.getId());
            }
        }

        @Override
        public void onStart() {
            super.onStart();

            if (mParentFragment != null && mParentFragment instanceof SettingsPreferenceFragment) {
                ((SettingsPreferenceFragment) mParentFragment).onDialogShowing();
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mDialogId = savedInstanceState.getInt(KEY_DIALOG_ID, 0);
                mParentFragment = getParentFragment();
                int mParentFragmentId = savedInstanceState.getInt(KEY_PARENT_FRAGMENT_ID, -1);
                if (!(mParentFragment instanceof DialogCreatable)) {
                    throw new IllegalArgumentException(
                            (mParentFragment != null
                                    ? mParentFragment.getClass().getName()
                                    : mParentFragmentId)
                                    + " must implement "
                                    + DialogCreatable.class.getName());
                }
                // This dialog fragment could be created from non-SettingsPreferenceFragment
                if (mParentFragment instanceof SettingsPreferenceFragment) {
                    // restore mDialogFragment in mParentFragment
                    ((SettingsPreferenceFragment) mParentFragment).mDialogFragment = this;
                }
            }
            return ((DialogCreatable) mParentFragment).onCreateDialog(mDialogId);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (mOnCancelListener != null) {
                mOnCancelListener.onCancel(dialog);
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss(dialog);
            }
        }

        public int getDialogId() {
            return mDialogId;
        }

        @Override
        public void onDetach() {
            super.onDetach();

            // This dialog fragment could be created from non-SettingsPreferenceFragment
            if (mParentFragment instanceof SettingsPreferenceFragment) {
                // in case the dialog is not explicitly removed by removeDialog()
                if (((SettingsPreferenceFragment) mParentFragment).mDialogFragment == this) {
                    ((SettingsPreferenceFragment) mParentFragment).mDialogFragment = null;
                }
            }
        }
    }

    protected boolean hasNextButton() {
        return ((ButtonBarHandler)getActivity()).hasNextButton();
    }

    protected Button getNextButton() {
        return ((ButtonBarHandler)getActivity()).getNextButton();
    }

    public void finish() {
        getActivity().onBackPressed();
    }

    public boolean startFragment(
            Fragment caller, String fragmentClass, int requestCode, Bundle extras) {
        if (getActivity() instanceof SettingsActivity) {
            SettingsActivity sa = (SettingsActivity) getActivity();
            sa.startPreferencePanel(fragmentClass, extras,
                    R.string.lock_settings_picker_title, null, caller, requestCode);
            return true;
        } else {
            Log.w(TAG, "Parent isn't Settings activity, thus there's no way to launch the "
                    + "given Fragment (name: " + fragmentClass + ", requestCode: " + requestCode
                    + ")");
            return false;
        }
    }

}
