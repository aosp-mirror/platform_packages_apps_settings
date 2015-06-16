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
import android.app.Fragment;
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
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
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
    private static final String TAG_OTHER_INFO = "otherInfo";
    private static final String TAG_USER_INFO = "userInfo";
    private static final String TAG_CONFIRM_CLEAR_CACHE = "confirmClearCache";

    private static final String AUTHORITY_MEDIA = "com.android.providers.media.documents";

    private StorageManager mStorageManager;
    private UserManager mUserManager;

    private String mVolumeId;
    private VolumeInfo mVolume;
    private VolumeInfo mSharedVolume;

    private StorageMeasurement mMeasure;

    private UserInfo mCurrentUser;

    private int mNextOrder = 0;

    private StorageSummaryPreference mSummary;
    private StorageItemPreference mApps;
    private StorageItemPreference mImages;
    private StorageItemPreference mVideos;
    private StorageItemPreference mAudio;
    private StorageItemPreference mOther;
    private StorageItemPreference mCache;
    private List<StorageItemPreference> mUsers = Lists.newArrayList();

    private Preference mExplore;

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

        mSummary = new StorageSummaryPreference(context);

        mApps = buildItem(R.string.storage_detail_apps);
        mImages = buildItem(R.string.storage_detail_images);
        mVideos = buildItem(R.string.storage_detail_videos);
        mAudio = buildItem(R.string.storage_detail_audio);
        mOther = buildItem(R.string.storage_detail_other);
        mCache = buildItem(R.string.storage_detail_cached);

        mCurrentUser = mUserManager.getUserInfo(UserHandle.myUserId());
        final List<UserInfo> otherUsers = getUsersExcluding(mCurrentUser);
        for (int i = 0; i < otherUsers.size(); i++) {
            final UserInfo user = otherUsers.get(i);
            final StorageItemPreference userPref = new StorageItemPreference(
                    context, user.name, user.id);
            mUsers.add(userPref);
        }

        mExplore = buildAction(R.string.storage_menu_explore);

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
            Log.d(TAG, "Leaving details fragment due to state " + mVolume.getState());
            finish();
            return;
        }

        screen.addPreference(mSummary);

        final boolean showUsers = !mUsers.isEmpty();
        final boolean showShared = (mSharedVolume != null) && mSharedVolume.isMountedReadable();

        if (showUsers) {
            screen.addPreference(new PreferenceHeader(context, mCurrentUser.name));
        }
        screen.addPreference(mApps);
        if (showShared) {
            screen.addPreference(mImages);
            screen.addPreference(mVideos);
            screen.addPreference(mAudio);
            screen.addPreference(mOther);
        }
        screen.addPreference(mCache);
        if (showShared) {
            screen.addPreference(mExplore);
        }
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
        final long totalBytes = file.getTotalSpace();
        final long freeBytes = file.getFreeSpace();
        final long usedBytes = totalBytes - freeBytes;

        final BytesResult result = Formatter.formatBytes(getResources(), usedBytes, 0);
        mSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large),
                result.value, result.units));
        mSummary.setSummary(getString(R.string.storage_volume_used,
                Formatter.formatFileSize(context, totalBytes)));
        mSummary.setPercent((int) ((usedBytes * 100) / totalBytes));

        mMeasure.forceMeasure();
    }

    private StorageItemPreference buildItem(int titleRes) {
        final StorageItemPreference pref = new StorageItemPreference(getActivity(), titleRes);
        pref.setOrder(mNextOrder++);
        return pref;
    }

    private Preference buildAction(int titleRes) {
        final Preference pref = new Preference(getActivity());
        pref.setTitle(titleRes);
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
        final MenuItem migrate = menu.findItem(R.id.storage_migrate);

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

        format.setTitle(R.string.storage_menu_format_public);

        // Only offer to migrate when not current storage
        final VolumeInfo privateVol = getActivity().getPackageManager()
                .getPrimaryStorageCurrentVolume();
        migrate.setVisible(!Objects.equals(mVolume, privateVol));
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
            case R.id.storage_migrate:
                final Intent intent = new Intent(context, StorageWizardMigrateConfirm.class);
                intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
                startActivity(intent);
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

        } else if (pref == mImages) {
            intent = new Intent(DocumentsContract.ACTION_BROWSE_DOCUMENT_ROOT);
            intent.setData(DocumentsContract.buildRootUri(AUTHORITY_MEDIA, "images_root"));
            intent.addCategory(Intent.CATEGORY_DEFAULT);

        } else if (pref == mVideos) {
            intent = new Intent(DocumentsContract.ACTION_BROWSE_DOCUMENT_ROOT);
            intent.setData(DocumentsContract.buildRootUri(AUTHORITY_MEDIA, "videos_root"));
            intent.addCategory(Intent.CATEGORY_DEFAULT);

        } else if (pref == mAudio) {
            intent = new Intent(DocumentsContract.ACTION_BROWSE_DOCUMENT_ROOT);
            intent.setData(DocumentsContract.buildRootUri(AUTHORITY_MEDIA, "audio_root"));
            intent.addCategory(Intent.CATEGORY_DEFAULT);

        } else if (pref == mOther) {
            OtherInfoFragment.show(this, mStorageManager.getBestVolumeDescription(mVolume),
                    mSharedVolume);
            return true;

        } else if (pref == mCache) {
            ConfirmClearCacheFragment.show(this);
            return true;

        } else if (pref == mExplore) {
            intent = mSharedVolume.buildBrowseIntent();
        }

        if (mUsers.contains(pref)) {
            UserInfoFragment.show(this, pref.getTitle(), pref.getSummary());
            return true;
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
        updatePreference(mApps, details.appsSize);

        final long imagesSize = totalValues(details.mediaSize, Environment.DIRECTORY_DCIM,
                Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES);
        updatePreference(mImages, imagesSize);

        final long videosSize = totalValues(details.mediaSize, Environment.DIRECTORY_MOVIES);
        updatePreference(mVideos, videosSize);

        final long audioSize = totalValues(details.mediaSize, Environment.DIRECTORY_MUSIC,
                Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS,
                Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS);
        updatePreference(mAudio, audioSize);

        updatePreference(mCache, details.cacheSize);
        updatePreference(mOther, details.miscSize);

        for (StorageItemPreference userPref : mUsers) {
            final long userSize = details.usersSize.get(userPref.userHandle);
            updatePreference(userPref, userSize);
        }
    }

    private void updatePreference(StorageItemPreference pref, long size) {
        pref.setSummary(Formatter.formatFileSize(getActivity(), size));
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
        public void onVolumeRecordChanged(VolumeRecord rec) {
            if (Objects.equals(mVolume.getFsUuid(), rec.getFsUuid())) {
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

    public static class OtherInfoFragment extends DialogFragment {
        public static void show(Fragment parent, String title, VolumeInfo sharedVol) {
            if (!parent.isAdded()) return;

            final OtherInfoFragment dialog = new OtherInfoFragment();
            dialog.setTargetFragment(parent, 0);
            final Bundle args = new Bundle();
            args.putString(Intent.EXTRA_TITLE, title);
            args.putParcelable(Intent.EXTRA_INTENT, sharedVol.buildBrowseIntent());
            dialog.setArguments(args);
            dialog.show(parent.getFragmentManager(), TAG_OTHER_INFO);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final String title = getArguments().getString(Intent.EXTRA_TITLE);
            final Intent intent = getArguments().getParcelable(Intent.EXTRA_INTENT);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(
                    TextUtils.expandTemplate(getText(R.string.storage_detail_dialog_other), title));

            builder.setPositiveButton(R.string.storage_menu_explore,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(intent);
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    public static class UserInfoFragment extends DialogFragment {
        public static void show(Fragment parent, CharSequence userLabel, CharSequence userSize) {
            if (!parent.isAdded()) return;

            final UserInfoFragment dialog = new UserInfoFragment();
            dialog.setTargetFragment(parent, 0);
            final Bundle args = new Bundle();
            args.putCharSequence(Intent.EXTRA_TITLE, userLabel);
            args.putCharSequence(Intent.EXTRA_SUBJECT, userSize);
            dialog.setArguments(args);
            dialog.show(parent.getFragmentManager(), TAG_USER_INFO);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final CharSequence userLabel = getArguments().getCharSequence(Intent.EXTRA_TITLE);
            final CharSequence userSize = getArguments().getCharSequence(Intent.EXTRA_SUBJECT);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(
                    getText(R.string.storage_detail_dialog_user), userLabel, userSize));

            builder.setPositiveButton(android.R.string.ok, null);

            return builder.create();
        }
    }

    /**
     * Dialog to request user confirmation before clearing all cache data.
     */
    public static class ConfirmClearCacheFragment extends DialogFragment {
        public static void show(Fragment parent) {
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
