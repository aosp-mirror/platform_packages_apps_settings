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
package com.android.settings.accessibility;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.accessibility.AccessibilityShortcutController.ToggleableFrameworkFeatureInfo;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppInfo;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settings.wrapper.IPackageManagerWrapper;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.wrapper.PackageManagerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fragment for picking accessibility shortcut service
 */
public class ShortcutServicePickerFragment extends RadioButtonPickerFragment {

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY_TOGGLE_GLOBAL_GESTURE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_shortcut_service_settings;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final Context context = getContext();
        final PackageManager pm = context.getPackageManager();
        final AccessibilityManager accessibilityManager = context
                .getSystemService(AccessibilityManager.class);
        final List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        final int numInstalledServices = installedServices.size();
        final PackageManagerWrapper pmw = new PackageManagerWrapper(context.getPackageManager());

        final List<CandidateInfo> candidates = new ArrayList<>(numInstalledServices);
        Map<ComponentName, ToggleableFrameworkFeatureInfo> frameworkFeatureInfoMap =
                AccessibilityShortcutController.getFrameworkShortcutFeaturesMap();
        for (ComponentName componentName : frameworkFeatureInfoMap.keySet()) {
            // Lookup icon
            candidates.add(new FrameworkCandidateInfo(frameworkFeatureInfoMap.get(componentName),
                    R.drawable.empty_icon, componentName.flattenToString()));
        }
        for (int i = 0; i < numInstalledServices; i++) {
            final AccessibilityServiceInfo installedServiceInfo = installedServices.get(i);
            candidates.add(new DefaultAppInfo(context, pmw, UserHandle.myUserId(),
                    installedServiceInfo.getComponentName(),
                    (String) installedServiceInfo.loadSummary(pm), true /* enabled */));
        }

        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        String shortcutServiceString = AccessibilityUtils
                .getShortcutTargetServiceComponentNameString(getContext(), UserHandle.myUserId());
        if (shortcutServiceString != null) {
            ComponentName shortcutName = ComponentName.unflattenFromString(shortcutServiceString);
            if (shortcutName != null) {
                return shortcutName.flattenToString();
            }
        }
        return null;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE, key);
        return true;
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference selected) {
        final String selectedKey = selected.getKey();

        if (TextUtils.isEmpty(selectedKey)) {
            super.onRadioButtonClicked(selected);
        } else {
            final ComponentName selectedComponent = ComponentName.unflattenFromString(selectedKey);
            if (AccessibilityShortcutController.getFrameworkShortcutFeaturesMap()
                    .containsKey(selectedComponent)) {
                // This is a framework feature. It doesn't need to be confirmed.
                onRadioButtonConfirmed(selectedKey);
            } else {
                final Activity activity = getActivity();
                if (activity != null) {
                    ConfirmationDialogFragment.newInstance(this, selectedKey)
                            .show(activity.getFragmentManager(), ConfirmationDialogFragment.TAG);
                }
            }
        }
    }

    private void onServiceConfirmed(String serviceKey) {
        onRadioButtonConfirmed(serviceKey);
    }

    public static class ConfirmationDialogFragment extends InstrumentedDialogFragment
            implements DialogInterface.OnClickListener {
        private static final String EXTRA_KEY = "extra_key";
        private static final String TAG = "ConfirmationDialogFragment";
        private IBinder mToken;

        public static ConfirmationDialogFragment newInstance(ShortcutServicePickerFragment parent,
                String key) {
            final ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            final Bundle argument = new Bundle();
            argument.putString(EXTRA_KEY, key);
            fragment.setArguments(argument);
            fragment.setTargetFragment(parent, 0);
            fragment.mToken = new Binder();
            return fragment;
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.ACCESSIBILITY_TOGGLE_GLOBAL_GESTURE;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle bundle = getArguments();
            final String key = bundle.getString(EXTRA_KEY);
            final ComponentName serviceComponentName = ComponentName.unflattenFromString(key);
            final AccessibilityManager accessibilityManager = getActivity()
                    .getSystemService(AccessibilityManager.class);
            AccessibilityServiceInfo info = accessibilityManager
                    .getInstalledServiceInfoWithComponentName(serviceComponentName);
            return AccessibilityServiceWarning.createCapabilitiesDialog(getActivity(), info, this);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final Fragment fragment = getTargetFragment();
            if ((which == BUTTON_POSITIVE) && (fragment instanceof ShortcutServicePickerFragment)) {
                final Bundle bundle = getArguments();
                ((ShortcutServicePickerFragment) fragment).onServiceConfirmed(
                        bundle.getString(EXTRA_KEY));
            }
        }
    }

    private class FrameworkCandidateInfo extends CandidateInfo {
        ToggleableFrameworkFeatureInfo mToggleableFrameworkFeatureInfo;
        int mIconResId;
        String mKey;

        public FrameworkCandidateInfo(
                ToggleableFrameworkFeatureInfo frameworkFeatureInfo, int iconResId, String key) {
            super(true /* enabled */);
            mToggleableFrameworkFeatureInfo = frameworkFeatureInfo;
            mIconResId = iconResId;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mToggleableFrameworkFeatureInfo.getLabel(getContext());
        }

        @Override
        public Drawable loadIcon() {
            return getContext().getDrawable(mIconResId);
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }
}
