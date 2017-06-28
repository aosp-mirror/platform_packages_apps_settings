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
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.XmlRes;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.instrumentation.Instrumentable;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.CustomDialogPreference;
import com.android.settingslib.CustomEditTextPreference;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreferenceMixin;

import java.util.UUID;

/**
 * Base class for Settings fragments, with some helper functions and dialog management.
 */
public abstract class SettingsPreferenceFragment extends InstrumentedPreferenceFragment
        implements DialogCreatable {

    /**
     * The Help Uri Resource key. This can be passed as an extra argument when creating the
     * Fragment.
     **/
    public static final String HELP_URI_RESOURCE_KEY = "help_uri_resource";

    private static final String TAG = "SettingsPreference";

    @VisibleForTesting
    static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;

    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    protected final FooterPreferenceMixin mFooterPreferenceMixin =
            new FooterPreferenceMixin(this, getLifecycle());

    private SettingsDialogFragment mDialogFragment;

    private String mHelpUri;

    private static final int ORDER_FIRST = -1;
    private static final int ORDER_LAST = Integer.MAX_VALUE -1;

    // Cache the content resolver for async callbacks
    private ContentResolver mContentResolver;

    private String mPreferenceKey;

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

    private ViewGroup mPinnedHeaderFrameLayout;
    private ViewGroup mButtonBar;

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
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mPreferenceHighlighted = icicle.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }

        // Prepare help url and enable menu if necessary
        Bundle arguments = getArguments();
        int helpResource;
        if (arguments != null && arguments.containsKey(HELP_URI_RESOURCE_KEY)) {
            helpResource = arguments.getInt(HELP_URI_RESOURCE_KEY);
        } else {
            helpResource = getHelpResource();
        }
        if (helpResource != 0) {
            mHelpUri = getResources().getString(helpResource);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        mPinnedHeaderFrameLayout = (ViewGroup) root.findViewById(R.id.pinned_header);
        mButtonBar = (ViewGroup) root.findViewById(R.id.button_bar);
        return root;
    }

    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        checkAvailablePrefs(getPreferenceScreen());
    }

    private void checkAvailablePrefs(PreferenceGroup preferenceGroup) {
        if (preferenceGroup == null) return;
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference pref = preferenceGroup.getPreference(i);
            if (pref instanceof SelfAvailablePreference
                    && !((SelfAvailablePreference) pref).isAvailable(getContext())) {
                preferenceGroup.removePreference(pref);
            } else if (pref instanceof PreferenceGroup) {
                checkAvailablePrefs((PreferenceGroup) pref);
            }
        }
    }

    public ViewGroup getButtonBar() {
        return mButtonBar;
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        final Bundle args = getArguments();
        if (args != null) {
            mPreferenceKey = args.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
            highlightPreferenceIfNeeded();
        }
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
        if (isAdded() && !mPreferenceHighlighted &&!TextUtils.isEmpty(mPreferenceKey)) {
            getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    highlightPreference(mPreferenceKey);
                }
            }, DELAY_HIGHLIGHT_DURATION_MILLIS);
        }
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
        addPreferenceToTop(mHeader);
    }

    protected void setHeaderView(View view) {
        mHeader = new LayoutPreference(getPrefContext(), view);
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
                    - (mHeader != null ? 1 : 0)
                    - (mFooterPreferenceMixin.hasFooter() ? 1 : 0)) <= 0
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

    /**
     * Return a valid ListView position or -1 if none is found
     */
    private int canUseListViewForHighLighting(String key) {
        if (getListView() == null) {
            return -1;
        }

        RecyclerView listView = getListView();
        RecyclerView.Adapter adapter = listView.getAdapter();

        if (adapter != null && adapter instanceof PreferenceGroupAdapter) {
            return findListPositionFromKey((PreferenceGroupAdapter) adapter, key);
        }

        return -1;
    }

    @Override
    public RecyclerView.LayoutManager onCreateLayoutManager() {
        mLayoutManager = new LinearLayoutManager(getContext());
        return mLayoutManager;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        mAdapter = new HighlightablePreferenceGroupAdapter(preferenceScreen);
        return mAdapter;
    }

    protected void setAnimationAllowed(boolean animationAllowed) {
        mAnimationAllowed = animationAllowed;
    }

    protected void cacheRemoveAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap<String, Preference>();
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

    private void highlightPreference(String key) {
        final int position = canUseListViewForHighLighting(key);
        if (position < 0) {
            return;
        }

        mPreferenceHighlighted = true;
        mLayoutManager.scrollToPosition(position);
        mAdapter.highlight(position);
    }

    private int findListPositionFromKey(PreferenceGroupAdapter adapter, String key) {
        final int count = adapter.getItemCount();
        for (int n = 0; n < count; n++) {
            final Preference preference = adapter.getItem(n);
            final String preferenceKey = preference.getKey();
            if (preferenceKey != null && preferenceKey.equals(key)) {
                return n;
            }
        }
        return -1;
    }

    protected boolean removePreference(String key) {
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

    /**
     * Override this if you want to show a help item in the menu, by returning the resource id.
     * @return the resource id for the help url
     */
    protected int getHelpResource() {
        return R.string.help_uri_default;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mHelpUri != null && getActivity() != null) {
            HelpUtils.prepareHelpMenuItem(getActivity(), menu, mHelpUri, getClass().getName());
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
        } else if (preference instanceof CustomDialogPreference) {
            f = CustomDialogPreference.CustomPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else if (preference instanceof CustomEditTextPreference) {
            f = CustomEditTextPreference.CustomPreferenceDialogFragment
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

        public SettingsDialogFragment() {
            /* do nothing */
        }

        public SettingsDialogFragment(DialogCreatable fragment, int dialogId) {
            super(fragment, dialogId);
            if (!(fragment instanceof Fragment)) {
                throw new IllegalArgumentException("fragment argument must be an instance of "
                        + Fragment.class.getName());
            }
            mParentFragment = (Fragment) fragment;
        }


        @Override
        public int getMetricsCategory() {
            if (mDialogCreatable == null) {
                return Instrumentable.METRICS_CATEGORY_UNKNOWN;
            }
            final int metricsCategory = mDialogCreatable.getDialogMetricsCategory(mDialogId);
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
    }

    protected boolean hasNextButton() {
        return ((ButtonBarHandler)getActivity()).hasNextButton();
    }

    protected Button getNextButton() {
        return ((ButtonBarHandler)getActivity()).getNextButton();
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

    public boolean startFragment(Fragment caller, String fragmentClass, int titleRes,
            int requestCode, Bundle extras) {
        final Activity activity = getActivity();
        if (activity instanceof SettingsActivity) {
            SettingsActivity sa = (SettingsActivity) activity;
            sa.startPreferencePanel(
                    caller, fragmentClass, extras, titleRes, null, caller, requestCode);
            return true;
        } else {
            Log.w(TAG,
                    "Parent isn't SettingsActivity nor PreferenceActivity, thus there's no way to "
                    + "launch the given Fragment (name: " + fragmentClass
                    + ", requestCode: " + requestCode + ")");
            return false;
        }
    }

    public static class HighlightablePreferenceGroupAdapter extends PreferenceGroupAdapter {

        @VisibleForTesting(otherwise=VisibleForTesting.NONE)
        int initialHighlightedPosition = -1;

        private int mHighlightPosition = -1;

        public HighlightablePreferenceGroupAdapter(PreferenceGroup preferenceGroup) {
            super(preferenceGroup);
        }

        public void highlight(int position) {
            mHighlightPosition = position;
            initialHighlightedPosition = position;
            notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            if (position == mHighlightPosition) {
                View v = holder.itemView;
                v.post(() -> {
                    if (v.getBackground() != null) {
                        final int centerX = v.getWidth() / 2;
                        final int centerY = v.getHeight() / 2;
                        v.getBackground().setHotspot(centerX, centerY);
                    }
                    v.setPressed(true);
                    v.setPressed(false);
                    mHighlightPosition = -1;
                });
            }
        }
    }
}
