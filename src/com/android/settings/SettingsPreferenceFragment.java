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

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.search.actionbar.SearchMenuController;
import com.android.settings.support.actionbar.HelpMenuController;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settings.widget.HighlightablePreferenceGroupAdapter;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.CustomDialogPreferenceCompat;
import com.android.settingslib.CustomEditTextPreferenceCompat;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.widget.LayoutPreference;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.UUID;

/**
 * Base class for Settings fragments, with some helper functions and dialog management.
 */
public abstract class SettingsPreferenceFragment extends InstrumentedPreferenceFragment
        implements DialogCreatable, HelpResourceProvider, Indexable {

    private static final String TAG = "SettingsPreferenceFragment";

    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    private static final int ORDER_FIRST = -1;

    private SettingsDialogFragment mDialogFragment;
    // Cache the content resolver for async callbacks
    private ContentResolver mContentResolver;

    private RecyclerView.Adapter mCurrentRootAdapter;
    private boolean mIsDataSetObserverRegistered = false;
    private RecyclerView.AdapterDataObserver mDataSetObserver =
            new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    onDataSetChanged();
                }
            };

    @VisibleForTesting
    ViewGroup mPinnedHeaderFrameLayout;

    private LayoutPreference mHeader;

    private View mEmptyView;
    private LinearLayoutManager mLayoutManager;
    private ArrayMap<String, Preference> mPreferenceCache;
    private boolean mAnimationAllowed;

    @VisibleForTesting
    public HighlightablePreferenceGroupAdapter mAdapter;
    @VisibleForTesting
    public boolean mPreferenceHighlighted = false;

    @Override
    public void onAttach(Context context) {
        if (shouldSkipForInitialSUW() && !WizardManagerHelper.isDeviceProvisioned(getContext())) {
            Log.w(TAG, "Skip " + getClass().getSimpleName() + " before SUW completed.");
            finish();
        }
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SearchMenuController.init(this /* host */);
        HelpMenuController.init(this /* host */);

        if (icicle != null) {
            mPreferenceHighlighted = icicle.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }
        HighlightablePreferenceGroupAdapter.adjustInitialExpandedChildCount(this /* host */);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        mPinnedHeaderFrameLayout = root.findViewById(R.id.pinned_header);
        return root;
    }

    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        checkAvailablePrefs(getPreferenceScreen());
    }

    @VisibleForTesting
    void checkAvailablePrefs(PreferenceGroup preferenceGroup) {
        if (preferenceGroup == null) return;
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference pref = preferenceGroup.getPreference(i);
            if (pref instanceof SelfAvailablePreference
                    && !((SelfAvailablePreference) pref).isAvailable(getContext())) {
                pref.setVisible(false);
            } else if (pref instanceof PreferenceGroup) {
                checkAvailablePrefs((PreferenceGroup) pref);
            }
        }
    }

    public View setPinnedHeaderView(int layoutResId) {
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View pinnedHeader =
                inflater.inflate(layoutResId, mPinnedHeaderFrameLayout, false);
        setPinnedHeaderView(pinnedHeader);
        return pinnedHeader;
    }

    public void setPinnedHeaderView(View pinnedHeader) {
        mPinnedHeaderFrameLayout.addView(pinnedHeader);
        mPinnedHeaderFrameLayout.setVisibility(View.VISIBLE);
    }

    public void showPinnedHeader(boolean show) {
        mPinnedHeaderFrameLayout.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAdapter != null) {
            outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mAdapter.isHighlightRequested());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        highlightPreferenceIfNeeded();
    }

    @Override
    protected void onBindPreferences() {
        registerObserverIfNeeded();
    }

    @Override
    protected void onUnbindPreferences() {
        unregisterObserverIfNeeded();
    }

    public void setLoading(boolean loading, boolean animate) {
        View loadingContainer = getView().findViewById(R.id.loading_container);
        LoadingViewController.handleLoadingContainer(loadingContainer, getListView(),
                !loading /* done */,
                animate);
    }

    public void registerObserverIfNeeded() {
        if (!mIsDataSetObserverRegistered) {
            if (mCurrentRootAdapter != null) {
                mCurrentRootAdapter.unregisterAdapterDataObserver(mDataSetObserver);
            }
            mCurrentRootAdapter = getListView().getAdapter();
            mCurrentRootAdapter.registerAdapterDataObserver(mDataSetObserver);
            mIsDataSetObserverRegistered = true;
            onDataSetChanged();
        }
    }

    public void unregisterObserverIfNeeded() {
        if (mIsDataSetObserverRegistered) {
            if (mCurrentRootAdapter != null) {
                mCurrentRootAdapter.unregisterAdapterDataObserver(mDataSetObserver);
                mCurrentRootAdapter = null;
            }
            mIsDataSetObserverRegistered = false;
        }
    }

    public void highlightPreferenceIfNeeded() {
        if (!isAdded()) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.requestHighlight(getView(), getListView());
        }
    }

    /**
     * Returns initial expanded child count.
     * <p/>
     * Only override this method if the initial expanded child must be determined at run time.
     */
    public int getInitialExpandedChildCount() {
        return 0;
    }

    /**
     * Whether preference is allowing to be displayed to the user.
     *
     * @param preference to check if it can be displayed to the user (not hidding in expand area).
     * @return {@code true} when preference is allowing to be displayed to the user.
     * {@code false} when preference is hidden in expand area and not been displayed to the user.
     */
    protected boolean isPreferenceExpanded(Preference preference) {
        return ((mAdapter == null)
                || (mAdapter.getPreferenceAdapterPosition(preference) != RecyclerView.NO_POSITION));
    }

    /**
     * Whether UI should be skipped in the initial SUW flow.
     *
     * @return {@code true} when UI should be skipped in the initial SUW flow.
     * {@code false} when UI should not be skipped in the initial SUW flow.
     */
    protected boolean shouldSkipForInitialSUW() {
        return false;
    }

    protected void onDataSetChanged() {
        highlightPreferenceIfNeeded();
        updateEmptyView();
    }

    public LayoutPreference getHeaderView() {
        return mHeader;
    }

    protected void setHeaderView(int resource) {
        mHeader = new LayoutPreference(getPrefContext(), resource);
        mHeader.setSelectable(false);
        addPreferenceToTop(mHeader);
    }

    protected void setHeaderView(View view) {
        mHeader = new LayoutPreference(getPrefContext(), view);
        mHeader.setSelectable(false);
        addPreferenceToTop(mHeader);
    }

    private void addPreferenceToTop(LayoutPreference preference) {
        preference.setOrder(ORDER_FIRST);
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().addPreference(preference);
        }
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != null && !preferenceScreen.isAttached()) {
            // Without ids generated, the RecyclerView won't animate changes to the preferences.
            preferenceScreen.setShouldUseGeneratedIds(mAnimationAllowed);
        }
        super.setPreferenceScreen(preferenceScreen);
        if (preferenceScreen != null) {
            if (mHeader != null) {
                preferenceScreen.addPreference(mHeader);
            }
        }
    }

    @VisibleForTesting
    void updateEmptyView() {
        if (mEmptyView == null) return;
        if (getPreferenceScreen() != null) {
            final View listContainer = getActivity().findViewById(android.R.id.list_container);
            boolean show = (getPreferenceScreen().getPreferenceCount()
                    - (mHeader != null ? 1 : 0)) <= 0
                    || (listContainer != null && listContainer.getVisibility() != View.VISIBLE);
            mEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    public void setEmptyView(View v) {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
        mEmptyView = v;
        updateEmptyView();
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    @Override
    public RecyclerView.LayoutManager onCreateLayoutManager() {
        mLayoutManager = new LinearLayoutManager(getContext());
        return mLayoutManager;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        final Bundle arguments = getArguments();
        mAdapter = new HighlightablePreferenceGroupAdapter(preferenceScreen,
                arguments == null
                        ? null : arguments.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY),
                mPreferenceHighlighted);
        return mAdapter;
    }

    protected void setAnimationAllowed(boolean animationAllowed) {
        mAnimationAllowed = animationAllowed;
    }

    protected void cacheRemoveAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap<>();
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference p = group.getPreference(i);
            if (TextUtils.isEmpty(p.getKey())) {
                continue;
            }
            mPreferenceCache.put(p.getKey(), p);
        }
    }

    protected Preference getCachedPreference(String key) {
        return mPreferenceCache != null ? mPreferenceCache.remove(key) : null;
    }

    protected void removeCachedPrefs(PreferenceGroup group) {
        for (Preference p : mPreferenceCache.values()) {
            group.removePreference(p);
        }
        mPreferenceCache = null;
    }

    protected int getCachedCount() {
        return mPreferenceCache != null ? mPreferenceCache.size() : 0;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public boolean removePreference(String key) {
        return removePreference(getPreferenceScreen(), key);
    }

    @VisibleForTesting
    boolean removePreference(PreferenceGroup group, String key) {
        final int preferenceCount = group.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            final Preference preference = group.getPreference(i);
            final String curKey = preference.getKey();

            if (TextUtils.equals(curKey, key)) {
                return group.removePreference(preference);
            }

            if (preference instanceof PreferenceGroup) {
                if (removePreference((PreferenceGroup) preference, key)) {
                    return true;
                }
            }
        }
        return false;
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
        mDialogFragment = SettingsDialogFragment.newInstance(this, dialogId);
        mDialogFragment.show(getChildFragmentManager(), Integer.toString(dialogId));
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        return null;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        return 0;
    }

    protected void removeDialog(int dialogId) {
        // mDialogFragment may not be visible yet in parent fragment's onResume().
        // To be able to dismiss dialog at that time, don't check
        // mDialogFragment.isVisible().
        if (mDialogFragment != null && mDialogFragment.getDialogId() == dialogId) {
            mDialogFragment.dismissAllowingStateLoss();
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

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference.getKey() == null) {
            // Auto-key preferences that don't have a key, so the dialog can find them.
            preference.setKey(UUID.randomUUID().toString());
        }
        DialogFragment f = null;
        if (preference instanceof RestrictedListPreference) {
            f = RestrictedListPreference.RestrictedListPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else if (preference instanceof CustomListPreference) {
            f = CustomListPreference.CustomListPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else if (preference instanceof CustomDialogPreferenceCompat) {
            f = CustomDialogPreferenceCompat.CustomPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else if (preference instanceof CustomEditTextPreferenceCompat) {
            f = CustomEditTextPreferenceCompat.CustomPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        f.setTargetFragment(this, 0);
        f.show(getFragmentManager(), "dialog_preference");
        onDialogShowing();
    }

    public static class SettingsDialogFragment extends InstrumentedDialogFragment {
        private static final String KEY_DIALOG_ID = "key_dialog_id";
        private static final String KEY_PARENT_FRAGMENT_ID = "key_parent_fragment_id";

        private Fragment mParentFragment;

        private DialogInterface.OnCancelListener mOnCancelListener;
        private DialogInterface.OnDismissListener mOnDismissListener;

        public static SettingsDialogFragment newInstance(DialogCreatable fragment, int dialogId) {
            if (!(fragment instanceof Fragment)) {
                throw new IllegalArgumentException("fragment argument must be an instance of "
                        + Fragment.class.getName());
            }

            final SettingsDialogFragment settingsDialogFragment = new SettingsDialogFragment();
            settingsDialogFragment.setParentFragment(fragment);
            settingsDialogFragment.setDialogId(dialogId);

            return settingsDialogFragment;
        }

        @Override
        public int getMetricsCategory() {
            if (mParentFragment == null) {
                return Instrumentable.METRICS_CATEGORY_UNKNOWN;
            }
            final int metricsCategory =
                    ((DialogCreatable) mParentFragment).getDialogMetricsCategory(mDialogId);
            if (metricsCategory <= 0) {
                throw new IllegalStateException("Dialog must provide a metrics category");
            }
            return metricsCategory;
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
                if (mParentFragment == null) {
                    mParentFragment = getFragmentManager().findFragmentById(mParentFragmentId);
                }
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

        private void setParentFragment(DialogCreatable fragment) {
            mParentFragment = (Fragment) fragment;
        }

        private void setDialogId(int dialogId) {
            mDialogId = dialogId;
        }
    }

    protected boolean hasNextButton() {
        return ((ButtonBarHandler) getActivity()).hasNextButton();
    }

    protected Button getNextButton() {
        return ((ButtonBarHandler) getActivity()).getNextButton();
    }

    public void finish() {
        Activity activity = getActivity();
        if (activity == null) return;
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            activity.finish();
        }
    }

    protected Intent getIntent() {
        if (getActivity() == null) {
            return null;
        }
        return getActivity().getIntent();
    }

    protected void setResult(int result, Intent intent) {
        if (getActivity() == null) {
            return;
        }
        getActivity().setResult(result, intent);
    }

    protected void setResult(int result) {
        if (getActivity() == null) {
            return;
        }
        getActivity().setResult(result);
    }

    protected boolean isFinishingOrDestroyed() {
        final Activity activity = getActivity();
        return activity == null || activity.isFinishing() || activity.isDestroyed();
    }
}
