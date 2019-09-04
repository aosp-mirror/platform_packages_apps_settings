/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.defaultapps;

import android.Manifest;
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.autofill.AutofillService;
import android.service.autofill.AutofillServiceInfo;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;

public class DefaultAutofillPicker extends DefaultAppPickerFragment {

    private static final String TAG = "DefaultAutofillPicker";

    static final String SETTING = Settings.Secure.AUTOFILL_SERVICE;
    static final Intent AUTOFILL_PROBE = new Intent(AutofillService.SERVICE_INTERFACE);

    /**
     * Extra set when the fragment is implementing ACTION_REQUEST_SET_AUTOFILL_SERVICE.
     */
    public static final String EXTRA_PACKAGE_NAME = "package_name";

    /**
     * Set when the fragment is implementing ACTION_REQUEST_SET_AUTOFILL_SERVICE.
     */
    private DialogInterface.OnClickListener mCancelListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        if (activity != null && activity.getIntent().getStringExtra(EXTRA_PACKAGE_NAME) != null) {
            mCancelListener = (d, w) -> {
                activity.setResult(Activity.RESULT_CANCELED);
                activity.finish();
            };
            // If mCancelListener is not null, fragment is started from
            // ACTION_REQUEST_SET_AUTOFILL_SERVICE and we should always use the calling uid.
            mUserId = UserHandle.myUserId();
        }
        mSettingsPackageMonitor.register(activity, activity.getMainLooper(), false);
        update();
    }

    @Override
    protected ConfirmationDialogFragment newConfirmationDialogFragment(String selectedKey,
            CharSequence confirmationMessage) {
        final AutofillPickerConfirmationDialogFragment fragment =
                new AutofillPickerConfirmationDialogFragment();
        fragment.init(this, selectedKey, confirmationMessage);
        return fragment;
    }

    /**
     * Custom dialog fragment that has a cancel listener used to propagate the result back to
     * caller (for the cases where the picker is launched by
     * {@code android.settings.REQUEST_SET_AUTOFILL_SERVICE}.
     */
    public static class AutofillPickerConfirmationDialogFragment
            extends ConfirmationDialogFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            final DefaultAutofillPicker target = (DefaultAutofillPicker) getTargetFragment();
            setCancelListener(target.mCancelListener);
            super.onCreate(savedInstanceState);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.default_autofill_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEFAULT_AUTOFILL_PICKER;
    }

    @Override
    protected boolean shouldShowItemNone() {
        return true;
    }

    /**
     * Monitor coming and going auto fill services and calls {@link #update()} when necessary
     */
    private final PackageMonitor mSettingsPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            ThreadUtils.postOnMainThread(() -> update());
        }

        @Override
        public void onPackageModified(String packageName) {
            ThreadUtils.postOnMainThread(() -> update());
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            ThreadUtils.postOnMainThread(() -> update());
        }
    };

    /**
     * Update the data in this UI.
     */
    private void update() {
        updateCandidates();
        addAddServicePreference();
    }

    @Override
    public void onDestroy() {
        mSettingsPackageMonitor.unregister();
        super.onDestroy();
    }

    /**
     * Gets the preference that allows to add a new autofill service.
     *
     * @return The preference or {@code null} if no service can be added
     */
    private Preference newAddServicePreferenceOrNull() {
        final String searchUri = Settings.Secure.getStringForUser(
                getActivity().getContentResolver(),
                Settings.Secure.AUTOFILL_SERVICE_SEARCH_URI,
                mUserId);
        if (TextUtils.isEmpty(searchUri)) {
            return null;
        }

        final Intent addNewServiceIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUri));
        final Context context = getPrefContext();
        final Preference preference = new Preference(context);
        preference.setOnPreferenceClickListener(p -> {
                    context.startActivityAsUser(addNewServiceIntent, UserHandle.of(mUserId));
                    return true;
                });
        preference.setTitle(R.string.print_menu_item_add_service);
        preference.setIcon(R.drawable.ic_add_24dp);
        preference.setOrder(Integer.MAX_VALUE -1);
        preference.setPersistent(false);
        return preference;
    }

    /**
     * Add a preference that allows the user to add a service if the market link for that is
     * configured.
     */
    private void addAddServicePreference() {
        final Preference addNewServicePreference = newAddServicePreferenceOrNull();
        if (addNewServicePreference != null) {
            getPreferenceScreen().addPreference(addNewServicePreference);
        }
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        final List<ResolveInfo> resolveInfos = mPm.queryIntentServicesAsUser(
                AUTOFILL_PROBE, PackageManager.GET_META_DATA, mUserId);
        final Context context = getContext();
        for (ResolveInfo info : resolveInfos) {
            final String permission = info.serviceInfo.permission;
            if (Manifest.permission.BIND_AUTOFILL_SERVICE.equals(permission)) {
                candidates.add(new DefaultAppInfo(context, mPm, mUserId, new ComponentName(
                        info.serviceInfo.packageName, info.serviceInfo.name)));
            }
            if (Manifest.permission.BIND_AUTOFILL.equals(permission)) {
                // Let it go for now...
                Log.w(TAG, "AutofillService from '" + info.serviceInfo.packageName
                        + "' uses unsupported permission " + Manifest.permission.BIND_AUTOFILL
                        + ". It works for now, but might not be supported on future releases");
                candidates.add(new DefaultAppInfo(context, mPm, mUserId, new ComponentName(
                        info.serviceInfo.packageName, info.serviceInfo.name)));
            }
        }
        return candidates;
    }

    public static String getDefaultKey(Context context, int userId) {
        String setting = Settings.Secure.getStringForUser(
                context.getContentResolver(), SETTING, userId);
        if (setting != null) {
            ComponentName componentName = ComponentName.unflattenFromString(setting);
            if (componentName != null) {
                return componentName.flattenToString();
            }
        }
        return null;
    }

    @Override
    protected String getDefaultKey() {
        return getDefaultKey(getContext(), mUserId);
    }

    @Override
    protected CharSequence getConfirmationMessage(CandidateInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        final CharSequence appName = appInfo.loadLabel();
        final String message = getContext().getString(
                R.string.autofill_confirmation_message, appName);
        return Html.fromHtml(message);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        Settings.Secure.putStringForUser(getContext().getContentResolver(), SETTING, key, mUserId);

        // Check if activity was launched from Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE
        // intent, and set proper result if so...
        final Activity activity = getActivity();
        if (activity != null) {
            final String packageName = activity.getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
            if (packageName != null) {
                final int result = key != null && key.startsWith(packageName) ? Activity.RESULT_OK
                        : Activity.RESULT_CANCELED;
                activity.setResult(result);
                activity.finish();
            }
        }
        return true;
    }

    /**
     * Provides Intent to setting activity for the specified autofill service.
     */
    static final class AutofillSettingIntentProvider implements SettingIntentProvider {

        private final String mSelectedKey;
        private final Context mContext;
        private final int mUserId;

        public AutofillSettingIntentProvider(Context context, int userId, String key) {
            mSelectedKey = key;
            mContext = context;
            mUserId = userId;
        }

        @Override
        public Intent getIntent() {
            final List<ResolveInfo> resolveInfos = mContext.getPackageManager()
                    .queryIntentServicesAsUser(
                            AUTOFILL_PROBE, PackageManager.GET_META_DATA, mUserId);

            for (ResolveInfo resolveInfo : resolveInfos) {
                final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                final String flattenKey = new ComponentName(
                        serviceInfo.packageName, serviceInfo.name).flattenToString();
                if (TextUtils.equals(mSelectedKey, flattenKey)) {
                    final String settingsActivity;
                    try {
                        settingsActivity = new AutofillServiceInfo(mContext, serviceInfo)
                                .getSettingsActivity();
                    } catch (SecurityException e) {
                        // Service does not declare the proper permission, ignore it.
                        Log.w(TAG, "Error getting info for " + serviceInfo + ": " + e);
                        return null;
                    }
                    if (TextUtils.isEmpty(settingsActivity)) {
                        return null;
                    }
                    return new Intent(Intent.ACTION_MAIN).setComponent(
                            new ComponentName(serviceInfo.packageName, settingsActivity));
                }
            }

            return null;
        }
    }
}
