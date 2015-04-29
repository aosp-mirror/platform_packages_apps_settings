/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import static com.android.settings.deviceinfo.StorageSettings.TAG;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.ManageApplications;
import com.android.settings.deviceinfo.StorageSettings.MountTask;
import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementDetails;
import com.android.settingslib.deviceinfo.StorageMeasurement.MeasurementReceiver;
import com.google.android.collect.Lists;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Panel showing summary and actions for a {@link VolumeInfo#TYPE_PRIVATE}
 * storage volume.
 */
public class PrivateVolumeSettings extends SettingsPreferenceFragment {
    // TODO: disable unmount when providing over MTP/PTP
    // TODO: warn when mounted read-only

    private static final String TAG_RENAME = "rename";
    private static final String TAG_CONFIRM_CLEAR_CACHE = "confirmClearCache";

    private StorageManager mStorageManager;
    private UserManager mUserManager;

    private String mVolumeId;
    private VolumeInfo mVolume;
    private VolumeInfo mSharedVolume;

    private StorageMeasurement mMeasure;

    private UserInfo mCurrentUser;

    private int mNextOrder = 0;

    private UsageBarPreference mGraph;
    private StorageItemPreference mTotal;
    private StorageItemPreference mAvailable;
    private StorageItemPreference mApps;
    private StorageItemPreference mDcim;
    private StorageItemPreference mMusic;
    private StorageItemPreference mDownloads;
    private StorageItemPreference mCache;
    private StorageItemPreference mMisc;
    private List<StorageItemPreference> mUsers = Lists.newArrayList();

    private long mTotalSize;
    private long mAvailSize;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO_STORAGE;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();

        mUserManager = context.getSystemService(UserManager.class);
        mStorageManager = context.getSystemService(StorageManager.class);

        mVolumeId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);
        mVolume = mStorageManager.findVolumeById(mVolumeId);

        Preconditions.checkNotNull(mVolume);
        Preconditions.checkState(mVolume.getType() == VolumeInfo.TYPE_PRIVATE);

        addPreferencesFromResource(R.xml.device_info_storage_volume);

        // Find the emulated shared storage layered above this private volume
        mSharedVolume = mStorageManager.findEmulatedForPrivate(mVolume);

        mMeasure = new StorageMeasurement(context, mVolume, mSharedVolume);
        mMeasure.setReceiver(mReceiver);

        mGraph = buildGraph();
        mTotal = buildItem(R.string.memory_size, 0);
        mAvailable = buildItem(R.string.memory_available, R.color.memory_avail);

        mApps = buildItem(R.string.memory_apps_usage, R.color.memory_apps_usage);
        mDcim = buildItem(R.string.memory_dcim_usage, R.color.memory_dcim);
        mMusic = buildItem(R.string.memory_music_usage, R.color.memory_music);
        mDownloads = buildItem(R.string.memory_downloads_usage, R.color.memory_downloads);
        mCache = buildItem(R.string.memory_media_cache_usage, R.color.memory_cache);
        mMisc = buildItem(R.string.memory_media_misc_usage, R.color.memory_misc);

        mCurrentUser = mUserManager.getUserInfo(UserHandle.myUserId());
        final List<UserInfo> otherUsers = getUsersExcluding(mCurrentUser);
        for (int i = 0; i < otherUsers.size(); i++) {
            final UserInfo user = otherUsers.get(i);
            final int colorRes = i % 2 == 0 ? R.color.memory_user_light
                    : R.color.memory_user_dark;
            final StorageItemPreference userPref = new StorageItemPreference(
                    context, user.name, colorRes, user.id);
            mUsers.add(userPref);
        }

        setHasOptionsMenu(true);
    }

    public void update() {
        getActivity().setTitle(mStorageManager.getBestVolumeDescription(mVolume));

        // Valid options may have changed
        getFragmentManager().invalidateOptionsMenu();

        final Context context = getActivity();
        final PreferenceScreen screen = getPreferenceScreen();

        screen.removeAll();

        if (!mVolume.isMountedReadable()) {
            return;
        }

        screen.addPreference(mGraph);
        screen.addPreference(mTotal);
        screen.addPreference(mAvailable);

        final boolean showUsers = !mUsers.isEmpty();
        if (showUsers) {
            screen.addPreference(new PreferenceHeader(context, mCurrentUser.name));
        }

        screen.addPreference(mApps);
        screen.addPreference(mDcim);
        screen.addPreference(mMusic);
        screen.addPreference(mDownloads);
        screen.addPreference(mCache);
        screen.addPreference(mMisc);

        if (showUsers) {
            screen.addPreference(new PreferenceHeader(context, R.string.storage_other_users));
            for (Preference pref : mUsers) {
                screen.addPreference(pref);
            }
        }

        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            final Preference pref = screen.getPreference(i);
            if (pref instanceof StorageItemPreference) {
                ((StorageItemPreference) pref).setLoading();
            }
        }

        final File file = mVolume.getPath();
        mTotalSize = file.getTotalSpace();
        mAvailSize = file.getFreeSpace();

        mTotal.setSummary(Formatter.formatFileSize(context, mTotalSize));
        mAvailable.setSummary(Formatter.formatFileSize(context, mAvailSize));

        mGraph.clear();
        mGraph.addEntry(0, (mTotalSize - mAvailSize) / (float) mTotalSize,
                android.graphics.Color.GRAY);
        mGraph.commit();

        mMeasure.forceMeasure();
    }

    private UsageBarPreference buildGraph() {
        final UsageBarPreference pref = new UsageBarPreference(getActivity());
        pref.setOrder(mNextOrder++);
        return pref;
    }

    private StorageItemPreference buildItem(int titleRes, int colorRes) {
        final StorageItemPreference pref = new StorageItemPreference(getActivity(), titleRes,
                colorRes);
        pref.setOrder(mNextOrder++);
        return pref;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh to verify that we haven't been formatted away
        mVolume = mStorageManager.findVolumeById(mVolumeId);
        if (mVolume == null) {
            getActivity().finish();
            return;
        }

        mStorageManager.registerListener(mStorageListener);
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMeasure.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.storage_volume, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final MenuItem rename = menu.findItem(R.id.storage_rename);
        final MenuItem mount = menu.findItem(R.id.storage_mount);
        final MenuItem unmount = menu.findItem(R.id.storage_unmount);
        final MenuItem format = menu.findItem(R.id.storage_format);
        final MenuItem usb = menu.findItem(R.id.storage_usb);

        // Actions live in menu for non-internal private volumes; they're shown
        // as preference items for public volumes.
        if (VolumeInfo.ID_PRIVATE_INTERNAL.equals(mVolume.getId())) {
            rename.setVisible(false);
            mount.setVisible(false);
            unmount.setVisible(false);
            format.setVisible(false);
        } else {
            rename.setVisible(mVolume.getType() == VolumeInfo.TYPE_PRIVATE);
            mount.setVisible(mVolume.getState() == VolumeInfo.STATE_UNMOUNTED);
            unmount.setVisible(mVolume.isMountedReadable());
            format.setVisible(true);
        }

        // TODO: show usb if we jumped past first screen
        usb.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Context context = getActivity();
        final Bundle args = new Bundle();
        switch (item.getItemId()) {
            case R.id.storage_rename:
                RenameFragment.show(this, mVolume);
                return true;
            case R.id.storage_mount:
                new MountTask(context, mVolume).execute();
                return true;
            case R.id.storage_unmount:
                args.putString(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
                startFragment(this, PrivateVolumeUnmount.class.getCanonicalName(),
                        R.string.storage_menu_unmount, 0, args);
                return true;
            case R.id.storage_format:
                args.putString(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
                startFragment(this, PrivateVolumeFormat.class.getCanonicalName(),
                        R.string.storage_menu_format, 0, args);
                return true;
            case R.id.storage_usb:
                startFragment(this, UsbSettings.class.getCanonicalName(),
                        R.string.storage_title_usb, 0, null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
        // TODO: launch better intents for specific volume

        Intent intent = null;
        if (pref == mApps) {
            Bundle args = new Bundle();
            args.putString(ManageApplications.EXTRA_CLASSNAME, StorageUseActivity.class.getName());
            args.putString(ManageApplications.EXTRA_VOLUME_UUID, mVolume.getFsUuid());
            args.putString(ManageApplications.EXTRA_VOLUME_NAME, mVolume.getDescription());
            intent = Utils.onBuildStartFragmentIntent(getActivity(),
                    ManageApplications.class.getName(), args, null, R.string.apps_storage, null,
                    false);
        } else if (pref == mDownloads) {
            intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).putExtra(
                    DownloadManager.INTENT_EXTRAS_SORT_BY_SIZE, true);

        } else if (pref == mMusic) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/mp3");

        } else if (pref == mDcim) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        } else if (pref == mCache) {
            ConfirmClearCacheFragment.show(this);
            return true;

        } else if (pref == mMisc) {
            intent = mSharedVolume.buildBrowseIntent();
        }

        if (intent != null) {
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "No activity found for " + intent);
            }
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, pref);
    }

    private final MeasurementReceiver mReceiver = new MeasurementReceiver() {
        @Override
        public void onDetailsChanged(MeasurementDetails details) {
            updateDetails(details);
        }
    };

    private void updateDetails(MeasurementDetails details) {
        mGraph.clear();

        updatePreference(mApps, details.appsSize);

        final long dcimSize = totalValues(details.mediaSize, Environment.DIRECTORY_DCIM,
                Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES);
        updatePreference(mDcim, dcimSize);

        final long musicSize = totalValues(details.mediaSize, Environment.DIRECTORY_MUSIC,
                Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS,
                Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS);
        updatePreference(mMusic, musicSize);

        final long downloadsSize = totalValues(details.mediaSize, Environment.DIRECTORY_DOWNLOADS);
        updatePreference(mDownloads, downloadsSize);

        updatePreference(mCache, details.cacheSize);
        updatePreference(mMisc, details.miscSize);

        for (StorageItemPreference userPref : mUsers) {
            final long userSize = details.usersSize.get(userPref.userHandle);
            updatePreference(userPref, userSize);
        }

        mGraph.commit();
    }

    private void updatePreference(StorageItemPreference pref, long size) {
        pref.setSummary(Formatter.formatFileSize(getActivity(), size));
        if (size > 0) {
            final int order = pref.getOrder();
            mGraph.addEntry(order, size / (float) mTotalSize, pref.color);
        }
    }

    /**
     * Return list of other users, excluding the current user.
     */
    private List<UserInfo> getUsersExcluding(UserInfo excluding) {
        final List<UserInfo> users = mUserManager.getUsers();
        final Iterator<UserInfo> i = users.iterator();
        while (i.hasNext()) {
            if (i.next().id == excluding.id) {
                i.remove();
            }
        }
        return users;
    }

    private static long totalValues(HashMap<String, Long> map, String... keys) {
        long total = 0;
        for (String key : keys) {
            if (map.containsKey(key)) {
                total += map.get(key);
            }
        }
        return total;
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (Objects.equals(mVolume.getId(), vol.getId())) {
                mVolume = vol;
                update();
            }
        }

        @Override
        public void onVolumeMetadataChanged(String fsUuid) {
            if (Objects.equals(mVolume.getFsUuid(), fsUuid)) {
                mVolume = mStorageManager.findVolumeById(mVolumeId);
                update();
            }
        }
    };

    /**
     * Dialog that allows editing of volume nickname.
     */
    public static class RenameFragment extends DialogFragment {
        public static void show(PrivateVolumeSettings parent, VolumeInfo vol) {
            if (!parent.isAdded()) return;

            final RenameFragment dialog = new RenameFragment();
            dialog.setTargetFragment(parent, 0);
            final Bundle args = new Bundle();
            args.putString(VolumeRecord.EXTRA_FS_UUID, vol.getFsUuid());
            dialog.setArguments(args);
            dialog.show(parent.getFragmentManager(), TAG_RENAME);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final StorageManager storageManager = context.getSystemService(StorageManager.class);

            final String fsUuid = getArguments().getString(VolumeRecord.EXTRA_FS_UUID);
            final VolumeInfo vol = storageManager.findVolumeByUuid(fsUuid);
            final VolumeRecord rec = storageManager.findRecordByUuid(fsUuid);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.dialog_edittext, null, false);
            final EditText nickname = (EditText) view.findViewById(R.id.edittext);
            nickname.setText(rec.getNickname());

            builder.setTitle(R.string.storage_rename_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.save,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO: move to background thread
                            storageManager.setVolumeNickname(fsUuid,
                                    nickname.getText().toString());
                        }
                    });
            builder.setNegativeButton(R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Dialog to request user confirmation before clearing all cache data.
     */
    public static class ConfirmClearCacheFragment extends DialogFragment {
        public static void show(PrivateVolumeSettings parent) {
            if (!parent.isAdded()) return;

            final ConfirmClearCacheFragment dialog = new ConfirmClearCacheFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_CLEAR_CACHE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.memory_clear_cache_title);
            builder.setMessage(getString(R.string.memory_clear_cache_message));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final PrivateVolumeSettings target = (PrivateVolumeSettings) getTargetFragment();
                    final PackageManager pm = context.getPackageManager();
                    final List<PackageInfo> infos = pm.getInstalledPackages(0);
                    final ClearCacheObserver observer = new ClearCacheObserver(
                            target, infos.size());
                    for (PackageInfo info : infos) {
                        pm.deleteApplicationCacheFiles(info.packageName, observer);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    private static class ClearCacheObserver extends IPackageDataObserver.Stub {
        private final PrivateVolumeSettings mTarget;
        private int mRemaining;

        public ClearCacheObserver(PrivateVolumeSettings target, int remaining) {
            mTarget = target;
            mRemaining = remaining;
        }

        @Override
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            synchronized (this) {
                if (--mRemaining == 0) {
                    mTarget.update();
                }
            }
        }
    }

    public static class PreferenceHeader extends Preference {
        public PreferenceHeader(Context context, int titleRes) {
            super(context, null, com.android.internal.R.attr.preferenceCategoryStyle);
            setTitle(titleRes);
        }

        public PreferenceHeader(Context context, CharSequence title) {
            super(context, null, com.android.internal.R.attr.preferenceCategoryStyle);
            setTitle(title);
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }
}
