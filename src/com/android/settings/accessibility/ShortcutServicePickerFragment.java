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

import static com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.accessibility.AccessibilityShortcutController.ToggleableFrameworkFeatureInfo;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fragment for picking accessibility shortcut service
 */
public class ShortcutServicePickerFragment extends RadioButtonPickerFragment {

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_GLOBAL_GESTURE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_shortcut_service_settings;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final Context context = getContext();
        final AccessibilityManager accessibilityManager = context
                .getSystemService(AccessibilityManager.class);
        final List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        final int numInstalledServices = installedServices.size();

        final List<CandidateInfo> candidates = new ArrayList<>(numInstalledServices);
        Map<ComponentName, ToggleableFrameworkFeatureInfo> frameworkFeatureInfoMap =
                AccessibilityShortcutController.getFrameworkShortcutFeaturesMap();
        for (ComponentName componentName : frameworkFeatureInfoMap.keySet()) {
            final int iconId;
            if (componentName.equals(COLOR_INVERSION_COMPONENT_NAME)) {
                iconId = R.drawable.ic_color_inversion;
            } else if (componentName.equals(DALTONIZER_COMPONENT_NAME)) {
                iconId = R.drawable.ic_daltonizer;
            } else {
                iconId = R.drawable.empty_icon;
            }
            candidates.add(new FrameworkCandidateInfo(frameworkFeatureInfoMap.get(componentName),
                    iconId, componentName.flattenToString()));
        }
        for (int i = 0; i < numInstalledServices; i++) {
            candidates.add(new ServiceCandidateInfo(installedServices.get(i)));
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
                final FragmentActivity activity = getActivity();
                if (activity != null) {
                    ConfirmationDialogFragment.newInstance(this, selectedKey)
                            .show(activity.getSupportFragmentManager(),
                                    ConfirmationDialogFragment.TAG);
                }
            }
        }
    }

    private void onServiceConfirmed(String serviceKey) {
        onRadioButtonConfirmed(serviceKey);
    }

    public static class ConfirmationDialogFragment extends InstrumentedDialogFragment
            implements View.OnClickListener {
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
            return SettingsEnums.ACCESSIBILITY_TOGGLE_GLOBAL_GESTURE;
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
        public void onClick(View view) {
            final Fragment fragment = getTargetFragment();
            if ((view.getId() == R.id.permission_enable_allow_button)
                && (fragment instanceof ShortcutServicePickerFragment)) {
                final Bundle bundle = getArguments();
                ((ShortcutServicePickerFragment) fragment).onServiceConfirmed(
                        bundle.getString(EXTRA_KEY));
            }
            dismiss();
        }
    }

    private class FrameworkCandidateInfo extends CandidateInfo {
        final ToggleableFrameworkFeatureInfo mToggleableFrameworkFeatureInfo;
        final int mIconResId;
        final String mKey;

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

    private class ServiceCandidateInfo extends CandidateInfo {
        final AccessibilityServiceInfo mServiceInfo;

        public ServiceCandidateInfo(AccessibilityServiceInfo serviceInfo) {
            super(true /* enabled */);
            mServiceInfo = serviceInfo;
        }

        @Override
        public CharSequence loadLabel() {
            final PackageManager pmw = getContext().getPackageManager();
            final CharSequence label =
                    mServiceInfo.getResolveInfo().serviceInfo.loadLabel(pmw);
            if (label != null) {
                return label;
            }

            final ComponentName componentName = mServiceInfo.getComponentName();
            if (componentName != null) {
                try {
                    final ApplicationInfo appInfo = pmw.getApplicationInfoAsUser(
                            componentName.getPackageName(), 0, UserHandle.myUserId());
                    return appInfo.loadLabel(pmw);
                } catch (PackageManager.NameNotFoundException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public Drawable loadIcon() {
            final ResolveInfo resolveInfo = mServiceInfo.getResolveInfo();
            return (resolveInfo.getIconResource() == 0)
                    ? getContext().getDrawable(R.drawable.ic_accessibility_generic)
                    : resolveInfo.loadIcon(getContext().getPackageManager());
        }

        @Override
        public String getKey() {
            return mServiceInfo.getComponentName().flattenToString();
        }
    }
}
