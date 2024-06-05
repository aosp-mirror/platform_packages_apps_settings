/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.notification;

import static com.android.internal.notification.NotificationAccessConfirmationActivityContract.EXTRA_COMPONENT_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.settings.R;

import com.google.common.base.Strings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class NotificationAccessConfirmationActivityTest {

    @Test
    public void start_showsDialog() {
        ComponentName cn = new ComponentName("com.example", "com.example.SomeService");
        installPackage(cn.getPackageName(), "X");

        NotificationAccessConfirmationActivity activity = startActivityWithIntent(cn);

        assertThat(activity.isFinishing()).isFalse();
        assertThat(getDialogText(activity)).isEqualTo(
                activity.getString(R.string.notification_listener_security_warning_summary, "X"));
    }

    @Test
    public void start_withMissingPackage_finishes() {
        ComponentName cn = new ComponentName("com.example", "com.example.SomeService");

        NotificationAccessConfirmationActivity activity = startActivityWithIntent(cn);

        assertThat(getDialogText(activity)).isNull();
        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void start_componentNameTooLong_finishes() {
        ComponentName longCn = new ComponentName("com.example", Strings.repeat("Blah", 150));
        installPackage(longCn.getPackageName(), "<Unused>");

        NotificationAccessConfirmationActivity activity = startActivityWithIntent(longCn);

        assertThat(getDialogText(activity)).isNull();
        assertThat(activity.isFinishing()).isTrue();
    }

    private static NotificationAccessConfirmationActivity startActivityWithIntent(
            ComponentName cn) {
        return Robolectric.buildActivity(
                        NotificationAccessConfirmationActivity.class,
                        new Intent().putExtra(EXTRA_COMPONENT_NAME, cn))
                .setup()
                .get();
    }

    private static void installPackage(String packageName, String appName) {
        PackageInfo pi = new PackageInfo();
        pi.packageName = packageName;
        pi.applicationInfo = new ApplicationInfo();
        pi.applicationInfo.packageName = packageName;
        pi.applicationInfo.name = appName;
        shadowOf(RuntimeEnvironment.application.getPackageManager()).installPackage(pi);
    }

    @Nullable
    private static String getDialogText(Activity activity) {
        TextView tv = activity.getWindow().findViewById(android.R.id.message);
        CharSequence text = (tv != null ? tv.getText() : null);
        return text != null ? text.toString() : null;
    }

}
