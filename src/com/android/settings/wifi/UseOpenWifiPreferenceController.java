package com.android.settings.wifi;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.network.NetworkScoreManagerWrapper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

/**
 * {@link AbstractPreferenceController} that controls whether a user wants to enable the "use open
 * networks automatically" feature provider by the current network recommendation provider.
 */
public class UseOpenWifiPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {
    private static final String KEY_USE_OPEN_WIFI_AUTOMATICALLY = "use_open_wifi_automatically";
    @VisibleForTesting static final int REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY = 400;

    private final ContentResolver mContentResolver;
    private final Fragment mFragment;
    private final NetworkScoreManagerWrapper mNetworkScoreManagerWrapper;
    private final SettingObserver mSettingObserver;

    private Preference mPreference;
    private ComponentName mEnableUseWifiComponentName;
    private boolean mDoFeatureSupportedScorersExist;

    public UseOpenWifiPreferenceController(Context context, Fragment fragment,
            NetworkScoreManagerWrapper networkScoreManagerWrapper, Lifecycle lifecycle) {
        super(context);
        mContentResolver = context.getContentResolver();
        mFragment = fragment;
        mNetworkScoreManagerWrapper = networkScoreManagerWrapper;
        mSettingObserver = new SettingObserver();
        updateEnableUseWifiComponentName();
        checkForFeatureSupportedScorers();
        lifecycle.addObserver(this);
    }

    private void updateEnableUseWifiComponentName() {
        NetworkScorerAppData appData = mNetworkScoreManagerWrapper.getActiveScorer();
        mEnableUseWifiComponentName =
                appData == null ? null : appData.getEnableUseOpenWifiActivity();
    }

    private void checkForFeatureSupportedScorers() {
        if (mEnableUseWifiComponentName != null) {
            mDoFeatureSupportedScorersExist = true;
            return;
        }
        List<NetworkScorerAppData> scorers = mNetworkScoreManagerWrapper.getAllValidScorers();
        for (NetworkScorerAppData scorer : scorers) {
            if (scorer.getEnableUseOpenWifiActivity() != null) {
                mDoFeatureSupportedScorersExist = true;
                return;
            }
        }
        mDoFeatureSupportedScorersExist = false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_USE_OPEN_WIFI_AUTOMATICALLY);
    }

    @Override
    public void onResume() {
        mSettingObserver.register(mContentResolver);
    }

    @Override
    public void onPause() {
        mSettingObserver.unregister(mContentResolver);
    }

    @Override
    public boolean isAvailable() {
        return mDoFeatureSupportedScorersExist;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_USE_OPEN_WIFI_AUTOMATICALLY;
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        final SwitchPreference useOpenWifiPreference = (SwitchPreference) preference;

        boolean isScorerSet = mNetworkScoreManagerWrapper.getActiveScorerPackage() != null;
        boolean doesActiveScorerSupportFeature = mEnableUseWifiComponentName != null;

        useOpenWifiPreference.setChecked(isSettingEnabled());
        useOpenWifiPreference.setVisible(isAvailable());
        useOpenWifiPreference.setEnabled(isScorerSet && doesActiveScorerSupportFeature);

        if (!isScorerSet) {
            useOpenWifiPreference.setSummary(
                    R.string.use_open_wifi_automatically_summary_scoring_disabled);
        } else if (!doesActiveScorerSupportFeature) {
            useOpenWifiPreference.setSummary(
                    R.string.use_open_wifi_automatically_summary_scorer_unsupported_disabled);
        } else {
            useOpenWifiPreference.setSummary(R.string.use_open_wifi_automatically_summary);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!TextUtils.equals(preference.getKey(), KEY_USE_OPEN_WIFI_AUTOMATICALLY)
                || !isAvailable()) {
            return false;
        }

        if (isSettingEnabled()) {
            Settings.Global.putString(mContentResolver,
                    Settings.Global.USE_OPEN_WIFI_PACKAGE, "");
            return true;
        }

        Intent intent = new Intent(NetworkScoreManager.ACTION_CUSTOM_ENABLE);
        intent.setComponent(mEnableUseWifiComponentName);
        mFragment.startActivityForResult(intent, REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY);
        return false; // Updating state is done in onActivityResult.
    }

    private boolean isSettingEnabled() {
        String enabledUseOpenWifiPackage = Settings.Global.getString(mContentResolver,
                Settings.Global.USE_OPEN_WIFI_PACKAGE);
        String currentUseOpenWifiPackage = mEnableUseWifiComponentName == null
                ? null : mEnableUseWifiComponentName.getPackageName();
        return TextUtils.equals(enabledUseOpenWifiPackage, currentUseOpenWifiPackage);
    }

    public boolean onActivityResult(int requestCode, int resultCode) {
        if (requestCode != REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY) {
            return false;
        }

        if (resultCode == Activity.RESULT_OK) {
            Settings.Global.putString(mContentResolver, Settings.Global.USE_OPEN_WIFI_PACKAGE,
                    mEnableUseWifiComponentName.getPackageName());
        }
        return true;
    }

    class SettingObserver extends ContentObserver {
        private final Uri NETWORK_RECOMMENDATIONS_ENABLED_URI =
                Settings.Global.getUriFor(Settings.Global.NETWORK_RECOMMENDATIONS_ENABLED);

        public SettingObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(NETWORK_RECOMMENDATIONS_ENABLED_URI, false, this);
            onChange(true /* selfChange */, NETWORK_RECOMMENDATIONS_ENABLED_URI);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (NETWORK_RECOMMENDATIONS_ENABLED_URI.equals(uri)) {
                updateEnableUseWifiComponentName();
                updateState(mPreference);
            }
        }
    }
}
