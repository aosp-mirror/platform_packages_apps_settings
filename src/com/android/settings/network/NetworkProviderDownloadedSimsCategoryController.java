package com.android.settings.network;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

public class NetworkProviderDownloadedSimsCategoryController extends
        PreferenceCategoryController implements LifecycleObserver {

    private static final String KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM =
            "provider_model_downloaded_sim_category";
    private PreferenceCategory mPreferenceCategory;
    private NetworkProviderDownloadedSimListController mNetworkProviderDownloadedSimListController;

    public NetworkProviderDownloadedSimsCategoryController(Context context, String key) {
        super(context, key);
    }

    public void init(Lifecycle lifecycle) {
        mNetworkProviderDownloadedSimListController = createDownloadedSimListController(lifecycle);
    }

    @VisibleForTesting
    protected NetworkProviderDownloadedSimListController createDownloadedSimListController(
            Lifecycle lifecycle) {
        return new NetworkProviderDownloadedSimListController(mContext, lifecycle);
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
        mPreferenceCategory = screen.findPreference(
                KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM);
        mPreferenceCategory.setVisible(isAvailable());
        mNetworkProviderDownloadedSimListController.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        int count = mPreferenceCategory.getPreferenceCount();
        String title = mContext.getString(count > 1
                ? R.string.downloaded_sims_category_title
                : R.string.downloaded_sim_category_title);
        mPreferenceCategory.setTitle(title);
    }
}
