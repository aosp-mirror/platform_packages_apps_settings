/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.annotation.UserIdInt;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.SeekBarVolumizer;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.DefaultRingtonePreference;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import com.android.settingslib.drawer.CategoryKey;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SoundSettings extends DashboardFragment
        implements OnPreferenceChangeListener {
    private static final String TAG = "SoundSettings";

    private static final String KEY_PHONE_RINGTONE = "ringtone";
    private static final String KEY_NOTIFICATION_RINGTONE = "notification_ringtone";
    private static final String KEY_ALARM_RINGTONE = "alarm_ringtone";
    private static final String KEY_VIBRATE_WHEN_RINGING = "vibrate_when_ringing";
    private static final String KEY_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";

    private static final String KEY_WORK_CATEGORY = "sound_work_settings_section";
    private static final String KEY_WORK_USE_PERSONAL_SOUNDS = "work_use_personal_sounds";
    private static final String KEY_WORK_PHONE_RINGTONE = "work_ringtone";
    private static final String KEY_WORK_NOTIFICATION_RINGTONE = "work_notification_ringtone";
    private static final String KEY_WORK_ALARM_RINGTONE = "work_alarm_ringtone";

    private static final String SELECTED_PREFERENCE_KEY = "selected_preference";
    private static final int REQUEST_CODE = 200;

    private static final int SAMPLE_CUTOFF = 2000;  // manually cap sample playback at 2 seconds

    private final VolumePreferenceCallback mVolumeCallback = new VolumePreferenceCallback();
    private final H mHandler = new H();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    private Context mContext;
    private boolean mVoiceCapable;

    private Preference mPhoneRingtonePreference;
    private Preference mNotificationRingtonePreference;
    private Preference mAlarmRingtonePreference;
    private TwoStatePreference mVibrateWhenRinging;

    private PreferenceGroup mWorkPreferenceCategory;
    private TwoStatePreference mWorkUsePersonalSounds;
    private Preference mWorkPhoneRingtonePreference;
    private Preference mWorkNotificationRingtonePreference;
    private Preference mWorkAlarmRingtonePreference;

    private PackageManager mPm;
    private UserManager mUserManager;
    private RingtonePreference mRequestPreference;

    private @UserIdInt int mManagedProfileId;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SOUND;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mPm = getPackageManager();
        mUserManager = UserManager.get(getContext());
        mVoiceCapable = Utils.isVoiceCapable(mContext);

        // Enable link to CMAS app settings depending on the value in config.xml.
        boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        try {
            if (isCellBroadcastAppLinkEnabled) {
                if (mPm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                }
            }
        } catch (IllegalArgumentException ignored) {
            isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
        }
        if (!mUserManager.isAdminUser() || !isCellBroadcastAppLinkEnabled ||
                RestrictedLockUtils.hasBaseUserRestriction(mContext,
                        UserManager.DISALLOW_CONFIG_CELL_BROADCASTS, UserHandle.myUserId())) {
            removePreference(KEY_CELL_BROADCAST_SETTINGS);
        }
        initRingtones();
        initVibrateWhenRinging();

        if (savedInstanceState != null) {
            String selectedPreference = savedInstanceState.getString(SELECTED_PREFERENCE_KEY, null);
            if (!TextUtils.isEmpty(selectedPreference)) {
                mRequestPreference = (RingtonePreference) findPreference(selectedPreference);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        lookupRingtoneNames();
        mSettingsObserver.register(true);

        RestrictedPreference broadcastSettingsPref = (RestrictedPreference) findPreference(
                KEY_CELL_BROADCAST_SETTINGS);
        if (broadcastSettingsPref != null) {
            broadcastSettingsPref.checkRestrictionAndSetDisabled(
                    UserManager.DISALLOW_CONFIG_CELL_BROADCASTS);
        }

        mManagedProfileId = Utils.getManagedProfileId(mUserManager, UserHandle.myUserId());
        if (mManagedProfileId != UserHandle.USER_NULL && shouldShowRingtoneSettings()) {
            if ((mWorkPreferenceCategory == null)) {
                // Work preferences not yet set
                addPreferencesFromResource(R.xml.sound_work_settings);
                initWorkPreferences();
            }
            if (!mWorkUsePersonalSounds.isChecked()) {
                updateWorkRingtoneSummaries();
            }
        } else {
            maybeRemoveWorkPreferences();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mVolumeCallback.stopSample();
        mSettingsObserver.register(false);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RingtonePreference) {
            mRequestPreference = (RingtonePreference) preference;
            mRequestPreference.onPrepareRingtonePickerIntent(mRequestPreference.getIntent());
            startActivityForResult(preference.getIntent(), REQUEST_CODE);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_SOUND;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.sound_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        Lifecycle lifecycle = getLifecycle();
        controllers.add(new CastPreferenceController(context));
        controllers.add(new ZenModePreferenceController(context));
        // === Volumes ===
        controllers.add(new AlarmVolumePreferenceController(context, mVolumeCallback, lifecycle));
        controllers.add(new MediaVolumePreferenceController(context, mVolumeCallback, lifecycle));
        controllers.add(
            new NotificationVolumePreferenceController(context, mVolumeCallback, lifecycle));
        controllers.add(new RingVolumePreferenceController(context, mVolumeCallback, lifecycle));
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRequestPreference != null) {
            mRequestPreference.onActivityResult(requestCode, resultCode, data);
            mRequestPreference = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRequestPreference != null) {
            outState.putString(SELECTED_PREFERENCE_KEY, mRequestPreference.getKey());
        }
    }

    /**
     * Updates the summary of work preferences
     *
     * This fragment only listens to changes on the work ringtone preferences, identified by keys
     * "work_ringtone", "work_notification_ringtone" and "work_alarm_ringtone".
     *
     * Note: Changes to the personal ringtones aren't listened to this way because they were already
     * handled using a {@link #SettingsObserver} ContentObserver. This wouldn't be appropriate for
     * work settings since the Settings app runs on the personal user.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int ringtoneType;
        if (KEY_WORK_PHONE_RINGTONE.equals(preference.getKey())) {
            ringtoneType = RingtoneManager.TYPE_RINGTONE;
        } else if (KEY_WORK_NOTIFICATION_RINGTONE.equals(preference.getKey())) {
            ringtoneType = RingtoneManager.TYPE_NOTIFICATION;
        } else if (KEY_WORK_ALARM_RINGTONE.equals(preference.getKey())) {
            ringtoneType = RingtoneManager.TYPE_ALARM;
        } else {
            return true;
        }

        preference.setSummary(updateRingtoneName(getManagedProfileContext(), ringtoneType));
        return true;
    }

    // === Volumes ===

    final class VolumePreferenceCallback implements VolumeSeekBarPreference.Callback {
        private SeekBarVolumizer mCurrent;

        @Override
        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (mCurrent != null && mCurrent != sbv) {
                mCurrent.stopSample();
            }
            mCurrent = sbv;
            if (mCurrent != null) {
                mHandler.removeMessages(H.STOP_SAMPLE);
                mHandler.sendEmptyMessageDelayed(H.STOP_SAMPLE, SAMPLE_CUTOFF);
            }
        }

        @Override
        public void onStreamValueChanged(int stream, int progress) {
            // noop
        }

        public void stopSample() {
            if (mCurrent != null) {
                mCurrent.stopSample();
            }
        }
    };


    // === Phone & notification ringtone ===

    private boolean shouldShowRingtoneSettings() {
        return !AudioSystem.isSingleVolume(mContext);
    }

    private void initRingtones() {
        mPhoneRingtonePreference = getPreferenceScreen().findPreference(KEY_PHONE_RINGTONE);
        if (mPhoneRingtonePreference != null && !mVoiceCapable) {
            getPreferenceScreen().removePreference(mPhoneRingtonePreference);
            mPhoneRingtonePreference = null;
        }
        mNotificationRingtonePreference =
                getPreferenceScreen().findPreference(KEY_NOTIFICATION_RINGTONE);
        mAlarmRingtonePreference = getPreferenceScreen().findPreference(KEY_ALARM_RINGTONE);
    }

    private void lookupRingtoneNames() {
        AsyncTask.execute(mLookupRingtoneNames);
    }

    private final Runnable mLookupRingtoneNames = new Runnable() {
        @Override
        public void run() {
            if (mPhoneRingtonePreference != null) {
                final CharSequence summary = updateRingtoneName(
                        mContext, RingtoneManager.TYPE_RINGTONE);
                if (summary != null) {
                    mHandler.obtainMessage(H.UPDATE_PHONE_RINGTONE, summary).sendToTarget();
                }
            }
            if (mNotificationRingtonePreference != null) {
                final CharSequence summary = updateRingtoneName(
                        mContext, RingtoneManager.TYPE_NOTIFICATION);
                if (summary != null) {
                    mHandler.obtainMessage(H.UPDATE_NOTIFICATION_RINGTONE, summary).sendToTarget();
                }
            }
            if (mAlarmRingtonePreference != null) {
                final CharSequence summary =
                        updateRingtoneName(mContext, RingtoneManager.TYPE_ALARM);
                if (summary != null) {
                    mHandler.obtainMessage(H.UPDATE_ALARM_RINGTONE, summary).sendToTarget();
                }
            }
        }
    };

    private static CharSequence updateRingtoneName(Context context, int type) {
        if (context == null) {
            Log.e(TAG, "Unable to update ringtone name, no context provided");
            return null;
        }
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        return Ringtone.getTitle(context, ringtoneUri, false /* followSettingsUri */,
                true /* allowRemote */);
    }

    // === Vibrate when ringing ===

    private void initVibrateWhenRinging() {
        mVibrateWhenRinging =
                (TwoStatePreference) getPreferenceScreen().findPreference(KEY_VIBRATE_WHEN_RINGING);
        if (mVibrateWhenRinging == null) {
            Log.i(TAG, "Preference not found: " + KEY_VIBRATE_WHEN_RINGING);
            return;
        }
        if (!mVoiceCapable) {
            getPreferenceScreen().removePreference(mVibrateWhenRinging);
            mVibrateWhenRinging = null;
            return;
        }
        mVibrateWhenRinging.setPersistent(false);
        updateVibrateWhenRinging();
        mVibrateWhenRinging.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                return Settings.System.putInt(getContentResolver(),
                        Settings.System.VIBRATE_WHEN_RINGING,
                        val ? 1 : 0);
            }
        });
    }

    private void updateVibrateWhenRinging() {
        if (mVibrateWhenRinging == null) return;
        mVibrateWhenRinging.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 0) != 0);
    }

    // === Callbacks ===

    private final class SettingsObserver extends ContentObserver {
        private final Uri VIBRATE_WHEN_RINGING_URI =
                Settings.System.getUriFor(Settings.System.VIBRATE_WHEN_RINGING);

        public SettingsObserver() {
            super(mHandler);
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                cr.registerContentObserver(VIBRATE_WHEN_RINGING_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (VIBRATE_WHEN_RINGING_URI.equals(uri)) {
                updateVibrateWhenRinging();
            }
        }
    }

    private final class H extends Handler {
        private static final int UPDATE_PHONE_RINGTONE = 1;
        private static final int UPDATE_NOTIFICATION_RINGTONE = 2;
        private static final int STOP_SAMPLE = 3;
        private static final int UPDATE_EFFECTS_SUPPRESSOR = 4;
        private static final int UPDATE_RINGER_MODE = 5;
        private static final int UPDATE_ALARM_RINGTONE = 6;

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_PHONE_RINGTONE:
                    mPhoneRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
                case UPDATE_NOTIFICATION_RINGTONE:
                    mNotificationRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
                case STOP_SAMPLE:
                    mVolumeCallback.stopSample();
                    break;
                case UPDATE_ALARM_RINGTONE:
                    mAlarmRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
            }
        }
    }

    // === Summary ===

    private static class SummaryProvider extends BroadcastReceiver
            implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final AudioManager mAudioManager;
        private final SummaryLoader mSummaryLoader;
        private final int maxVolume;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
                filter.addAction(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
                filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
                filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                filter.addAction(AudioManager.STREAM_MUTE_CHANGED_ACTION);
                filter.addAction(NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED);
                mContext.registerReceiver(this, filter);
            } else {
                mContext.unregisterReceiver(this);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final int ringerMode = mAudioManager.getRingerMode();
            int resId;
            String percent = "";
            if (ringerMode == mAudioManager.RINGER_MODE_SILENT) {
                resId = R.string.sound_settings_summary_silent;
            } else if (ringerMode == mAudioManager.RINGER_MODE_VIBRATE){
                resId = R.string.sound_settings_summary_vibrate;
            }
            else {
                percent =  NumberFormat.getPercentInstance().format(
                        (double) mAudioManager.getStreamVolume(
                                AudioManager.STREAM_RING) / maxVolume);
                resId = R.string.sound_settings_summary;
            }
            mSummaryLoader.setSummary(this, mContext.getString(resId, percent));
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    // === Indexing ===

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.sound_settings;
            return Arrays.asList(sir);
        }

        public List<String> getNonIndexableKeys(Context context) {
            final ArrayList<String> rt = new ArrayList<String>();
            new NotificationVolumePreferenceController(
                context, null /* Callback */, null /* Lifecycle */).updateNonIndexableKeys(rt);
            new RingVolumePreferenceController(
                context, null /* Callback */, null /* Lifecycle */).updateNonIndexableKeys(rt);
            new CastPreferenceController(context).updateNonIndexableKeys(rt);
            if (!Utils.isVoiceCapable(context)) {
                rt.add(KEY_PHONE_RINGTONE);
                rt.add(KEY_VIBRATE_WHEN_RINGING);
            }

            final PackageManager pm = context.getPackageManager();
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);

            // Enable link to CMAS app settings depending on the value in config.xml.
            boolean isCellBroadcastAppLinkEnabled = context.getResources().getBoolean(
                    com.android.internal.R.bool.config_cellBroadcastAppLinks);
            try {
                if (isCellBroadcastAppLinkEnabled) {
                    if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                            == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                    }
                }
            } catch (IllegalArgumentException ignored) {
                isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
            }
            if (!um.isAdminUser() || !isCellBroadcastAppLinkEnabled) {
                rt.add(KEY_CELL_BROADCAST_SETTINGS);
            }

            return rt;
        }
    };

    // === Work Sound Settings ===

    private Context getManagedProfileContext() {
        if (mManagedProfileId == UserHandle.USER_NULL) {
            return null;
        }
        return Utils.createPackageContextAsUser(mContext, mManagedProfileId);
    }

    private DefaultRingtonePreference initWorkPreference(String key) {
        DefaultRingtonePreference pref =
                (DefaultRingtonePreference) getPreferenceScreen().findPreference(key);
        pref.setOnPreferenceChangeListener(this);

        // Required so that RingtonePickerActivity lists the work profile ringtones
        pref.setUserId(mManagedProfileId);
        return pref;
    }

    private void initWorkPreferences() {
        mWorkPreferenceCategory = (PreferenceGroup) getPreferenceScreen()
                .findPreference(KEY_WORK_CATEGORY);
        mWorkUsePersonalSounds = (TwoStatePreference) getPreferenceScreen()
                .findPreference(KEY_WORK_USE_PERSONAL_SOUNDS);
        mWorkPhoneRingtonePreference = initWorkPreference(KEY_WORK_PHONE_RINGTONE);
        mWorkNotificationRingtonePreference = initWorkPreference(KEY_WORK_NOTIFICATION_RINGTONE);
        mWorkAlarmRingtonePreference = initWorkPreference(KEY_WORK_ALARM_RINGTONE);

        if (!mVoiceCapable) {
            mWorkPreferenceCategory.removePreference(mWorkPhoneRingtonePreference);
            mWorkPhoneRingtonePreference = null;
        }

        Context managedProfileContext = getManagedProfileContext();
        if (Settings.Secure.getIntForUser(managedProfileContext.getContentResolver(),
                Settings.Secure.SYNC_PARENT_SOUNDS, 0, mManagedProfileId) == 1) {
            enableWorkSyncSettings();
        } else {
            disableWorkSyncSettings();
        }

        mWorkUsePersonalSounds.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    UnifyWorkDialogFragment.show(SoundSettings.this);
                    return false;
                } else {
                    disableWorkSync();
                    return true;
                }
            }
        });
    }

    private void enableWorkSync() {
        RingtoneManager.enableSyncFromParent(getManagedProfileContext());
        enableWorkSyncSettings();
    }

    private void enableWorkSyncSettings() {
        mWorkUsePersonalSounds.setChecked(true);

        if (mWorkPhoneRingtonePreference != null) {
            mWorkPhoneRingtonePreference.setSummary(R.string.work_sound_same_as_personal);
        }
        mWorkNotificationRingtonePreference.setSummary(R.string.work_sound_same_as_personal);
        mWorkAlarmRingtonePreference.setSummary(R.string.work_sound_same_as_personal);
    }

    private void disableWorkSync() {
        RingtoneManager.disableSyncFromParent(getManagedProfileContext());
        disableWorkSyncSettings();
    }

    private void disableWorkSyncSettings() {
        if (mWorkPhoneRingtonePreference != null) {
            mWorkPhoneRingtonePreference.setEnabled(true);
        }
        mWorkNotificationRingtonePreference.setEnabled(true);
        mWorkAlarmRingtonePreference.setEnabled(true);

        updateWorkRingtoneSummaries();
    }

    private void updateWorkRingtoneSummaries() {
        Context managedProfileContext = getManagedProfileContext();

        if (mWorkPhoneRingtonePreference != null) {
            mWorkPhoneRingtonePreference.setSummary(
                    updateRingtoneName(managedProfileContext, RingtoneManager.TYPE_RINGTONE));
        }
        mWorkNotificationRingtonePreference.setSummary(
                updateRingtoneName(managedProfileContext, RingtoneManager.TYPE_NOTIFICATION));
        mWorkAlarmRingtonePreference.setSummary(
                updateRingtoneName(managedProfileContext, RingtoneManager.TYPE_ALARM));
    }

    private void maybeRemoveWorkPreferences() {
        if (mWorkPreferenceCategory == null) {
            // No work preferences to remove
            return;
        }
        getPreferenceScreen().removePreference(mWorkPreferenceCategory);
        mWorkPreferenceCategory = null;
        mWorkPhoneRingtonePreference = null;
        mWorkNotificationRingtonePreference = null;
        mWorkAlarmRingtonePreference = null;
    }

    public static class UnifyWorkDialogFragment extends InstrumentedDialogFragment
            implements DialogInterface.OnClickListener {
        private static final String TAG = "UnifyWorkDialogFragment";
        private static final int REQUEST_CODE = 200;

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.DIALOG_UNIFY_SOUND_SETTINGS;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.work_sync_dialog_title)
                    .setMessage(R.string.work_sync_dialog_message)
                    .setPositiveButton(R.string.work_sync_dialog_yes, UnifyWorkDialogFragment.this)
                    .setNegativeButton(android.R.string.no, null)
                    .create();
        }

        public static void show(SoundSettings parent) {
            FragmentManager fm = parent.getFragmentManager();
            if (fm.findFragmentByTag(TAG) == null) {
                UnifyWorkDialogFragment fragment = new UnifyWorkDialogFragment();
                fragment.setTargetFragment(parent, REQUEST_CODE);
                fragment.show(fm, TAG);
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            SoundSettings soundSettings = (SoundSettings) getTargetFragment();
            if (soundSettings.isAdded()) {
                soundSettings.enableWorkSync();
            }
        }
    }
}
