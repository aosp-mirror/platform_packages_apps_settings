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
import android.content.DialogInterface;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.applications.defaultapps.DefaultAppInfo;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for picking accessibility shortcut service
 */
public class ShortcutServicePickerFragment extends DefaultAppPickerFragment {

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY_TOGGLE_GLOBAL_GESTURE;
    }

    @Override
    protected List<? extends DefaultAppInfo> getCandidates() {
        final AccessibilityManager accessibilityManager = getContext()
                .getSystemService(AccessibilityManager.class);
        final List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        final int numInstalledServices = installedServices.size();

        List<DefaultAppInfo> candidates = new ArrayList<>(numInstalledServices);
        for (int i = 0; i < numInstalledServices; i++) {
            AccessibilityServiceInfo installedServiceInfo = installedServices.get(i);
            candidates.add(new DefaultAppInfo(mPm,
                    UserHandle.myUserId(),
                    installedServiceInfo.getComponentName(),
                    (String) installedServiceInfo.loadSummary(mPm.getPackageManager()),
                    true /* enabled */));
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

        final Activity activity = getActivity();
        if (TextUtils.isEmpty(selectedKey)) {
            super.onRadioButtonClicked(selected);
        } else if (activity != null) {
            final DialogFragment fragment = ConfirmationDialogFragment.newInstance(
                    this, selectedKey);
            fragment.show(activity.getFragmentManager(), ConfirmationDialogFragment.TAG);
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
            if ((which == BUTTON_POSITIVE) && (fragment instanceof DefaultAppPickerFragment)) {
                final Bundle bundle = getArguments();
                ((ShortcutServicePickerFragment) fragment).onServiceConfirmed(
                        bundle.getString(EXTRA_KEY));
            }
        }
    }
}
