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
import android.text.TextUtils;
import android.util.Log;
import android.os.UserHandle;
import com.android.internal.annotations.Immutable;
import com.android.internal.util.Preconditions;

/**
 * Specifies a setting that is being injected into Settings &gt; Location &gt; Location services.
 *
 * @see android.location.SettingInjectorService
 */
@Immutable
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
     * The user/profile associated with this setting (e.g. managed profile)
     */
    public final UserHandle mUserHandle;

    /**
     * The activity to launch to allow the user to modify the settings value. Assumed to be in the
     * {@link #packageName} package.
     */
    public final String settingsActivity;

    private InjectedSetting(String packageName, String className,
            String title, int iconId, UserHandle userHandle, String settingsActivity) {
        this.packageName = Preconditions.checkNotNull(packageName, "packageName");
        this.className = Preconditions.checkNotNull(className, "className");
        this.title = Preconditions.checkNotNull(title, "title");
        this.iconId = iconId;
        this.mUserHandle = userHandle;
        this.settingsActivity = Preconditions.checkNotNull(settingsActivity);
    }

    /**
     * Returns a new instance, or null.
     */
    public static InjectedSetting newInstance(String packageName, String className,
            String title, int iconId, UserHandle userHandle, String settingsActivity) {
        if (packageName == null || className == null ||
                TextUtils.isEmpty(title) || TextUtils.isEmpty(settingsActivity)) {
            if (Log.isLoggable(SettingsInjector.TAG, Log.WARN)) {
                Log.w(SettingsInjector.TAG, "Illegal setting specification: package="
                        + packageName + ", class=" + className
                        + ", title=" + title + ", settingsActivity=" + settingsActivity);
            }
            return null;
        }
        return new InjectedSetting(packageName, className, title, iconId, userHandle,
                settingsActivity);
    }

    @Override
    public String toString() {
        return "InjectedSetting{" +
                "mPackageName='" + packageName + '\'' +
                ", mClassName='" + className + '\'' +
                ", label=" + title +
                ", iconId=" + iconId +
                ", userId=" + mUserHandle.getIdentifier() +
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InjectedSetting)) return false;

        InjectedSetting that = (InjectedSetting) o;

        return packageName.equals(that.packageName) && className.equals(that.className)
                && title.equals(that.title) && iconId == that.iconId
                && mUserHandle.equals(that.mUserHandle)
                && settingsActivity.equals(that.settingsActivity);
    }

    @Override
    public int hashCode() {
        int result = packageName.hashCode();
        result = 31 * result + className.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + iconId;
        result = 31 * result + mUserHandle.hashCode();
        result = 31 * result + settingsActivity.hashCode();
        return result;
    }
}
