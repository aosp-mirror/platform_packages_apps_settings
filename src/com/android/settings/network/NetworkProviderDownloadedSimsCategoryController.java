package com.android.settings.network;

import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

public class NetworkProviderDownloadedSimsCategoryController extends
        PreferenceCategoryController implements LifecycleObserver {

    private static final String LOG_TAG = "NetworkProviderDownloadedSimsCategoryController";
    private static final String KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM =
            "provider_model_downloaded_sim_category";
    private PreferenceCategory mPreferenceCategory;
    private NetworkProviderDownloadedSimListController mNetworkProviderDownloadedSimListController;

    public NetworkProviderDownloadedSimsCategoryController(Context context, String key,
            Lifecycle lifecycle) {
        super(context, key);
        mNetworkProviderDownloadedSimListController =
                new NetworkProviderDownloadedSimListController(mContext, lifecycle);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mNetworkProviderDownloadedSimListController == null
                || !mNetworkProviderDownloadedSimListController.isAvailable()) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mNetworkProviderDownloadedSimListController.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(
                KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM);
        if (mPreferenceCategory == null) {
            Log.d(LOG_TAG, "displayPreference(), Can not find the category.");
            return;
        }
        mPreferenceCategory.setVisible(isAvailable());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mPreferenceCategory == null) {
            Log.d(LOG_TAG, "updateState(), Can not find the category.");
            return;
        }
        int count = mPreferenceCategory.getPreferenceCount();
        String title = mContext.getString(count > 1
                ? R.string.downloaded_sims_category_title
                : R.string.downloaded_sim_category_title);
        mPreferenceCategory.setTitle(title);
    }
}
