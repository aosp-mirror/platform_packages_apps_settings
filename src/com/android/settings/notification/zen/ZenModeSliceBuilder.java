/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.notification.zen;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import android.annotation.ColorInt;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

public class ZenModeSliceBuilder {

    private static final String TAG = "ZenModeSliceBuilder";

    private static final String ZEN_MODE_SLICE_KEY = ZenModeButtonPreferenceController.KEY;

    /**
     * Action notifying a change on the Zen Mode Slice.
     */
    public static final String ACTION_ZEN_MODE_SLICE_CHANGED =
            "com.android.settings.notification.ZEN_MODE_CHANGED";

    public static final IntentFilter INTENT_FILTER = new IntentFilter();

    static {
        INTENT_FILTER.addAction(NotificationManager.ACTION_NOTIFICATION_POLICY_CHANGED);
        INTENT_FILTER.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
        INTENT_FILTER.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED_INTERNAL);
    }

    private ZenModeSliceBuilder() {
    }

    /**
     * Return a ZenMode Slice bound to {@link CustomSliceRegistry#ZEN_MODE_URI}.
     * <p>
     * Note that you should register a listener for {@link #INTENT_FILTER} to get changes for
     * ZenMode.
     */
    public static Slice getSlice(Context context) {
        final boolean isZenModeEnabled = isZenModeEnabled(context);
        final CharSequence title = context.getText(R.string.zen_mode_settings_title);
        final CharSequence subtitle = context.getText(R.string.zen_mode_slice_subtitle);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(context);
        final PendingIntent toggleAction = getBroadcastIntent(context);
        final PendingIntent primaryAction = getPrimaryAction(context);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryAction,
                (IconCompat) null /* icon */, ListBuilder.ICON_IMAGE, title);
        final SliceAction toggleSliceAction = SliceAction.createToggle(toggleAction,
                null /* actionTitle */,
                isZenModeEnabled);
        final RowBuilder rowBuilder = new RowBuilder()
                .setTitle(title)
                .setPrimaryAction(primarySliceAction);
        if (!isManagedByAdmin(context)) {
            rowBuilder.addEndItem(toggleSliceAction);
        }

        if (!Utils.isSettingsIntelligence(context)) {
            rowBuilder.setSubtitle(subtitle);
        }

        return new ListBuilder(context, CustomSliceRegistry.ZEN_MODE_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(rowBuilder)
                .build();
    }

    /**
     * Update the current ZenMode status to the boolean value keyed by
     * {@link android.app.slice.Slice#EXTRA_TOGGLE_STATE} on {@param intent}.
     */
    public static void handleUriChange(Context context, Intent intent) {
        final boolean zenModeOn = intent.getBooleanExtra(EXTRA_TOGGLE_STATE, false);
        final int zenMode;
        if (zenModeOn) {
            zenMode = Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
        } else {
            zenMode = Settings.Global.ZEN_MODE_OFF;
        }
        NotificationManager.from(context).setZenMode(zenMode, null /* conditionId */, TAG);
        // Do not notifyChange on Uri. The service takes longer to update the current value than it
        // does for the Slice to check the current value again. Let {@link SliceBroadcastRelay}
        // handle it.
    }

    public static Intent getIntent(Context context) {
        final Uri contentUri = new Uri.Builder().appendPath(ZEN_MODE_SLICE_KEY).build();
        final String screenTitle = context.getText(R.string.zen_mode_settings_title).toString();
        return SliceBuilderUtils.buildSearchResultPageIntent(context,
                ZenModeSettings.class.getName(), ZEN_MODE_SLICE_KEY, screenTitle,
                SettingsEnums.NOTIFICATION_ZEN_MODE, R.string.menu_key_notifications)
                .setClassName(context.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);
    }

    private static boolean isZenModeEnabled(Context context) {
        final NotificationManager manager = context.getSystemService(NotificationManager.class);
        final int zenMode = manager.getZenMode();

        switch (zenMode) {
            case Settings.Global.ZEN_MODE_ALARMS:
            case Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                return true;
            case Settings.Global.ZEN_MODE_OFF:
            default:
                return false;
        }
    }

    private static PendingIntent getPrimaryAction(Context context) {
        final Intent intent = getIntent(context);
        return PendingIntent.getActivity(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent getBroadcastIntent(Context context) {
        final Intent intent = new Intent(ACTION_ZEN_MODE_SLICE_CHANGED)
                .setClass(context, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    private static boolean isManagedByAdmin(Context context) {
        EnforcedAdmin enforcedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                context, UserManager.DISALLOW_ADJUST_VOLUME, UserHandle.myUserId());
        return enforcedAdmin != null;
    }
}
