/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.utils.ZenServiceListing;

import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class ZenRuleSelectionDialog extends InstrumentedDialogFragment {
    private static final String TAG = "ZenRuleSelectionDialog";
    private static final boolean DEBUG = ZenModeSettings.DEBUG;

    private static ZenServiceListing mServiceListing;
    protected static PositiveClickListener mPositiveClickListener;

    private static Context mContext;
    private static PackageManager mPm;
    private static NotificationManager mNm;
    private LinearLayout mRuleContainer;

    /**
     * The interface we expect a listener to implement.
     */
    public interface PositiveClickListener {
        void onSystemRuleSelected(ZenRuleInfo ruleInfo, Fragment parent);
        void onExternalRuleSelected(ZenRuleInfo ruleInfo, Fragment parent);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ZEN_MODE_RULE_SELECTION_DIALOG;
    }

    public static void show(Context context, Fragment parent, PositiveClickListener
            listener, ZenServiceListing serviceListing) {
        mPositiveClickListener = listener;
        mContext = context;
        mPm = mContext.getPackageManager();
        mNm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mServiceListing = serviceListing;

        ZenRuleSelectionDialog dialog = new ZenRuleSelectionDialog();
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getFragmentManager(), TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View v = LayoutInflater.from(getContext()).inflate(R.layout.zen_rule_type_selection,
                null, false);

        mRuleContainer = (LinearLayout) v.findViewById(R.id.rule_container);
        if (mServiceListing != null) {
            bindType(defaultNewEvent());
            bindType(defaultNewSchedule());
            mServiceListing.addZenCallback(mServiceListingCallback);
            mServiceListing.reloadApprovedServices();
        }
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.zen_mode_choose_rule_type)
                .setView(v)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mServiceListing != null) {
            mServiceListing.removeZenCallback(mServiceListingCallback);
        }
    }

    private void bindType(final ZenRuleInfo ri) {
        try {
            ApplicationInfo info = mPm.getApplicationInfo(ri.packageName, 0);
            final LinearLayout v = (LinearLayout) LayoutInflater.from(mContext).inflate(
                    R.layout.zen_rule_type, null, false);

            ImageView iconView = v.findViewById(R.id.icon);
            ((TextView) v.findViewById(R.id.title)).setText(ri.title);
            if (!ri.isSystem) {
                LoadIconTask task = new LoadIconTask(iconView);
                task.execute(info);

                TextView subtitle = (TextView) v.findViewById(R.id.subtitle);
                subtitle.setText(info.loadLabel(mPm));
                subtitle.setVisibility(View.VISIBLE);
            } else {
                if (ZenModeConfig.isValidScheduleConditionId(ri.defaultConditionId)) {
                    iconView.setImageDrawable(mContext.getDrawable(R.drawable.ic_timelapse));
                } else if (ZenModeConfig.isValidEventConditionId(ri.defaultConditionId)) {
                    iconView.setImageDrawable(mContext.getDrawable(R.drawable.ic_event));
                }
            }
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    if (ri.isSystem) {
                        mPositiveClickListener.onSystemRuleSelected(ri, getTargetFragment());
                    } else {
                        mPositiveClickListener.onExternalRuleSelected(ri, getTargetFragment());
                    }
                }
            });
            mRuleContainer.addView(v);
        } catch (PackageManager.NameNotFoundException e) {
            // Omit rule.
        }
    }

    private ZenRuleInfo defaultNewSchedule() {
        final ZenModeConfig.ScheduleInfo schedule = new ZenModeConfig.ScheduleInfo();
        schedule.days = ZenModeConfig.ALL_DAYS;
        schedule.startHour = 22;
        schedule.endHour = 7;
        final ZenRuleInfo rt = new ZenRuleInfo();
        rt.settingsAction = ZenModeScheduleRuleSettings.ACTION;
        rt.title = mContext.getString(R.string.zen_schedule_rule_type_name);
        rt.packageName = ZenModeConfig.getEventConditionProvider().getPackageName();
        rt.defaultConditionId = ZenModeConfig.toScheduleConditionId(schedule);
        rt.serviceComponent = ZenModeConfig.getScheduleConditionProvider();
        rt.isSystem = true;
        return rt;
    }

    private ZenRuleInfo defaultNewEvent() {
        final ZenModeConfig.EventInfo event = new ZenModeConfig.EventInfo();
        event.calName = null; // any calendar
        event.calendarId = null;
        event.reply = ZenModeConfig.EventInfo.REPLY_ANY_EXCEPT_NO;
        final ZenRuleInfo rt = new ZenRuleInfo();
        rt.settingsAction = ZenModeEventRuleSettings.ACTION;
        rt.title = mContext.getString(R.string.zen_event_rule_type_name);
        rt.packageName = ZenModeConfig.getScheduleConditionProvider().getPackageName();
        rt.defaultConditionId = ZenModeConfig.toEventConditionId(event);
        rt.serviceComponent = ZenModeConfig.getEventConditionProvider();
        rt.isSystem = true;
        return rt;
    }

    private void bindExternalRules(Set<ZenRuleInfo> externalRuleTypes) {
        for (ZenRuleInfo ri : externalRuleTypes) {
            bindType(ri);
        }
    }

    private final ZenServiceListing.Callback mServiceListingCallback = new
            ZenServiceListing.Callback() {
        @Override
        public void onComponentsReloaded(Set<ComponentInfo> componentInfos) {
            if (DEBUG) Log.d(TAG, "Reloaded: count=" + componentInfos.size());

            Set<ZenRuleInfo> externalRuleTypes = new TreeSet<>(RULE_TYPE_COMPARATOR);
            for (ComponentInfo ci : componentInfos) {
                final ZenRuleInfo ri = AbstractZenModeAutomaticRulePreferenceController.
                        getRuleInfo(mPm, ci);
                if (ri != null && ri.configurationActivity != null
                        && mNm.isNotificationPolicyAccessGrantedForPackage(ri.packageName)
                        && (ri.ruleInstanceLimit <= 0 || ri.ruleInstanceLimit
                        >= (mNm.getRuleInstanceCount(ci.getComponentName()) + 1))) {
                    externalRuleTypes.add(ri);
                }
            }
            bindExternalRules(externalRuleTypes);
        }
    };

    private static final Comparator<ZenRuleInfo> RULE_TYPE_COMPARATOR =
            new Comparator<ZenRuleInfo>() {
                private final Collator mCollator = Collator.getInstance();

                @Override
                public int compare(ZenRuleInfo lhs, ZenRuleInfo rhs) {
                    int byAppName = mCollator.compare(lhs.packageLabel, rhs.packageLabel);
                    if (byAppName != 0) {
                        return byAppName;
                    } else {
                        return mCollator.compare(lhs.title, rhs.title);
                    }
                }
            };

    private class LoadIconTask extends AsyncTask<ApplicationInfo, Void, Drawable> {
        private final WeakReference<ImageView> viewReference;

        public LoadIconTask(ImageView view) {
            viewReference = new WeakReference<>(view);
        }

        @Override
        protected Drawable doInBackground(ApplicationInfo... params) {
            return params[0].loadIcon(mPm);
        }

        @Override
        protected void onPostExecute(Drawable icon) {
            if (icon != null) {
                final ImageView view = viewReference.get();
                if (view != null) {
                    view.setImageDrawable(icon);
                }
            }
        }
    }
}