/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.development;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.SettingsActivity;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

public class DeveloperOptionsActivityLifecycle implements Application.ActivityLifecycleCallbacks {

    private FragmentManager.FragmentLifecycleCallbacks mFragmentCallback =
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                    if (!(f instanceof DeveloperOptionAwareMixin)) {
                        return;
                    }

                    Activity activity = f.getActivity();
                    if (activity == null) {
                        return;
                    }

                    if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(activity)) {
                        return;
                    }

                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                    } else {
                        activity.finish();
                    }
                }
            };

    public DeveloperOptionsActivityLifecycle() {}

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        if (!(activity instanceof SettingsActivity)) {
            return;
        }

        FragmentManager fm = ((SettingsActivity) activity).getSupportFragmentManager();
        fm.registerFragmentLifecycleCallbacks(mFragmentCallback, /* recursive= */ true);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {}

    @Override
    public void onActivityResumed(@NonNull Activity activity) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
}
