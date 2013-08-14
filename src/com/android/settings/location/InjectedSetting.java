/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.location;

import android.content.Intent;

/**
 * Specifies a setting that is being injected into Settings > Location > Location services.
 *
 * @see android.location.SettingInjectorService
 */
class InjectedSetting {

    /**
     * Package for the subclass of {@link android.location.SettingInjectorService} and for the
     * settings activity.
     */
    public final String packageName;

    /**
     * Class name for the subclass of {@link android.location.SettingInjectorService} that
     * specifies dynamic values for the location setting.
     */
    public final String className;

    /**
     * The {@link android.preference.Preference#getTitle()} value.
     */
    public final String title;

    /**
     * The {@link android.preference.Preference#getIcon()} value.
     */
    public final int iconId;

    /**
     * The activity to launch to allow the user to modify the settings value. Assumed to be in the
     * {@link #packageName} package.
     */
    public final String settingsActivity;

    public InjectedSetting(String packageName, String className,
            String title, int iconId, String settingsActivity) {
        this.packageName = packageName;
        this.className = className;
        this.title = title;
        this.iconId = iconId;
        this.settingsActivity = settingsActivity;
    }

    @Override
    public String toString() {
        return "InjectedSetting{" +
                "mPackageName='" + packageName + '\'' +
                ", mClassName='" + className + '\'' +
                ", label=" + title +
                ", iconId=" + iconId +
                ", settingsActivity='" + settingsActivity + '\'' +
                '}';
    }

    /**
     * Returns the intent to start the {@link #className} service.
     */
    public Intent getServiceIntent() {
        Intent intent = new Intent();
        intent.setClassName(packageName, className);
        return intent;
    }
}
