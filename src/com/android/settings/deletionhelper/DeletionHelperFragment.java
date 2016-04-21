package com.android.settings.deletionhelper;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.format.Formatter;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.deletionhelper.AppStateUsageStatsBridge;
import com.android.settings.deletionhelper.AppDeletionPreference;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import com.android.settingslib.applications.ApplicationsState.Session;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

/**
 * Settings screen for the deletion helper, which manually removes data which is not recently used.
 */
public class DeletionHelperFragment extends SettingsPreferenceFragment implements
        ApplicationsState.Callbacks, AppStateBaseBridge.Callback, Preference.OnPreferenceChangeListener {
    private static final String TAG = "DeletionHelperFragment";

    private static final String EXTRA_HAS_BRIDGE = "hasBridge";
    private static final String EXTRA_HAS_SIZES = "hasSizes";
    private static final String EXTRA_CHECKED_SET = "checkedSet";

    private static final String KEY_APPS_GROUP = "apps_group";

    private Button mCancel, mFree;
    private PreferenceGroup mApps;

    private ApplicationsState mState;
    private Session mSession;
    private HashSet<String> mUncheckedApplications;
    private AppStateUsageStatsBridge mDataUsageBridge;
    private ArrayList<AppEntry> mAppEntries;
    private boolean mHasReceivedAppEntries, mHasReceivedBridgeCallback, mFinishedLoading;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mState.newSession(this);
        mUncheckedApplications = new HashSet<>();
        mDataUsageBridge = new AppStateUsageStatsBridge(getActivity(), mState, this);

        addPreferencesFromResource(R.xml.deletion_helper_list);
        mApps = (PreferenceGroup) findPreference(KEY_APPS_GROUP);

        if (savedInstanceState != null) {
            mHasReceivedAppEntries =
                    savedInstanceState.getBoolean(EXTRA_HAS_SIZES, false);
            mHasReceivedBridgeCallback =
                    savedInstanceState.getBoolean(EXTRA_HAS_BRIDGE, false);
            mUncheckedApplications =
                    (HashSet<String>) savedInstanceState.getSerializable(EXTRA_CHECKED_SET);
        }
    }

    private void initializeButtons(View v) {
        mCancel = (Button) v.findViewById(R.id.back_button);
        mCancel.setText(R.string.cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishFragment();
            }
        });

        mFree = (Button) v.findViewById(R.id.next_button);
        mFree.setText(R.string.storage_menu_free);
        mFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArraySet<String> apps = new ArraySet<>();
                for (AppEntry entry : mAppEntries) {
                    if (!mUncheckedApplications.contains(entry.label)) {
                        synchronized (entry) {
                            apps.add(entry.info.packageName);
                        }
                    }
                }
                // TODO: If needed, add an action on the callback.
                PackageDeletionTask task = new PackageDeletionTask(
                        getActivity().getPackageManager(), apps,
                        new PackageDeletionTask.Callback() {
                            @Override
                            public void onSuccess() {
                            }

                            @Override
                            public void onError() {
                                Log.e(TAG, "An error occurred while uninstalling packages.");
                            }
                        });
                finishFragment();
                task.run();
            }
        });
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        initializeButtons(v);
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.resume();
        mDataUsageBridge.resume();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_HAS_SIZES, mHasReceivedAppEntries);
        outState.putBoolean(EXTRA_HAS_BRIDGE, mHasReceivedBridgeCallback);
        outState.putSerializable(EXTRA_CHECKED_SET, mUncheckedApplications);
    }


    @Override
    public void onPause() {
        super.onPause();
        mDataUsageBridge.pause();
        mSession.pause();
    }

    private void rebuild() {
        // Only rebuild if we have the packages and their usage stats.
        if (!mHasReceivedBridgeCallback || !mHasReceivedAppEntries) {
            return;
        }

        final ArrayList<AppEntry> apps =
                mSession.rebuild(AppStateUsageStatsBridge.FILTER_USAGE_STATS,
                        ApplicationsState.SIZE_COMPARATOR);
        mAppEntries = apps;
        cacheRemoveAllPrefs(mApps);
        int entryCount = apps.size();
        for (int i = 0; i < entryCount; i++) {
            AppEntry entry = apps.get(i);
            final String packageName = entry.label;
            AppDeletionPreference preference = (AppDeletionPreference) getCachedPreference(entry.label);
            if (preference == null) {
                preference = new AppDeletionPreference(getActivity(), entry,
                        mState);
                preference.setKey(packageName);
                preference.setChecked(!mUncheckedApplications.contains(packageName));
                preference.setOnPreferenceChangeListener(this);
                mApps.addPreference(preference);
            }
            preference.setOrder(i);
        }
        removeCachedPrefs(mApps);

        // All applications should be filled in if we've received the sizes.
        // setLoading being called multiple times causes flickering, so we only do it once.
        if (mHasReceivedAppEntries && !mFinishedLoading) {
            mFinishedLoading = true;
            setLoading(false, true);
            getButtonBar().setVisibility(View.VISIBLE);
        }
        updateFreeButtonText();
    }

    private void updateFreeButtonText() {
        mFree.setText(String.format(getActivity().getString(R.string.deletion_helper_free_button),
                Formatter.formatFileSize(getActivity(), getTotalFreeableSpace())));
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        // No-op.
    }

    @Override
    public void onPackageListChanged() {
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        rebuild();
    }

    @Override
    public void onAllSizesComputed() {
        rebuild();
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onLoadEntriesCompleted() {
        mHasReceivedAppEntries = true;
        rebuild();
    }

    @Override
    public void onExtraInfoUpdated() {
        mHasReceivedBridgeCallback = true;
        rebuild();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_STORAGE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean checked = (boolean) newValue;
        String packageName = ((AppDeletionPreference) preference).getPackageName();
        if (checked) {
            mUncheckedApplications.remove(packageName);
        } else {
            mUncheckedApplications.add(packageName);
        }
        updateFreeButtonText();
        return true;
    }

    private long getTotalFreeableSpace() {
        long freeableSpace = 0;
        for (int i = 0; i < mAppEntries.size(); i++) {
            final AppEntry entry = mAppEntries.get(i);
            if (!mUncheckedApplications.contains(entry.label)) {
                freeableSpace += mAppEntries.get(i).size;
            }
        }
        return freeableSpace;
    }
}