package com.android.settings.wifi;

import android.app.Activity;
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
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

/**
 * {@link TogglePreferenceController} that controls whether a user wants to enable the "use open
 * networks automatically" feature provided by the current network recommendation provider.
 */
public class UseOpenWifiPreferenceController extends TogglePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {
    public static final int REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY = 400;

    private static final String KEY_USE_OPEN_WIFI_AUTOMATICALLY = "use_open_wifi_automatically";

    private final ContentResolver mContentResolver;
    private Fragment mFragment;
    private final NetworkScoreManager mNetworkScoreManager;
    private final SettingObserver mSettingObserver;

    private Preference mPreference;
    private ComponentName mEnableUseWifiComponentName;
    private boolean mDoFeatureSupportedScorersExist;

    public UseOpenWifiPreferenceController(Context context) {
        super(context, KEY_USE_OPEN_WIFI_AUTOMATICALLY);
        mContentResolver = context.getContentResolver();
        mNetworkScoreManager =
                (NetworkScoreManager) context.getSystemService(Context.NETWORK_SCORE_SERVICE);
        mSettingObserver = new SettingObserver();
        updateEnableUseWifiComponentName();
        checkForFeatureSupportedScorers();
    }

    public void setFragment(Fragment hostFragment) {
        mFragment = hostFragment;
    }

    private void updateEnableUseWifiComponentName() {
        NetworkScorerAppData appData = mNetworkScoreManager.getActiveScorer();
        mEnableUseWifiComponentName =
                appData == null ? null : appData.getEnableUseOpenWifiActivity();
    }

    private void checkForFeatureSupportedScorers() {
        if (mEnableUseWifiComponentName != null) {
            mDoFeatureSupportedScorersExist = true;
            return;
        }
        List<NetworkScorerAppData> scorers = mNetworkScoreManager.getAllValidScorers();
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
        mPreference = screen.findPreference(getPreferenceKey());
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
    public int getAvailabilityStatus() {
        // It is possible that mEnableUseWifiComponentName is no longer enabled by
        // USE_OPEN_WIFI_PACKAGE. So update this component to reflect correct availability.
        updateEnableUseWifiComponentName();
        checkForFeatureSupportedScorers();
        return mDoFeatureSupportedScorersExist ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        final boolean isScorerSet = mNetworkScoreManager.getActiveScorerPackage() != null;
        final boolean doesActiveScorerSupportFeature = mEnableUseWifiComponentName != null;

        preference.setEnabled(isScorerSet && doesActiveScorerSupportFeature);
        if (!isScorerSet) {
            preference.setSummary(R.string.use_open_wifi_automatically_summary_scoring_disabled);
        } else if (!doesActiveScorerSupportFeature) {
            preference.setSummary(
                    R.string.use_open_wifi_automatically_summary_scorer_unsupported_disabled);
        } else {
            preference.setSummary(R.string.use_open_wifi_automatically_summary);
        }
    }

    @Override
    public boolean isChecked() {
        final String enabledUseOpenWifiPackage = Settings.Global.getString(mContentResolver,
                Settings.Global.USE_OPEN_WIFI_PACKAGE);
        final String currentUseOpenWifiPackage = mEnableUseWifiComponentName == null
                ? null : mEnableUseWifiComponentName.getPackageName();
        return TextUtils.equals(enabledUseOpenWifiPackage, currentUseOpenWifiPackage);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            if (mFragment == null) {
                throw new IllegalStateException("No fragment to start activity");
            }

            final Intent intent = new Intent(NetworkScoreManager.ACTION_CUSTOM_ENABLE);
            intent.setComponent(mEnableUseWifiComponentName);
            mFragment.startActivityForResult(intent, REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY);
            return false; // Updating state is done in onActivityResult.
        } else {
            Settings.Global.putString(mContentResolver, Settings.Global.USE_OPEN_WIFI_PACKAGE, "");
            return true;
        }
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
