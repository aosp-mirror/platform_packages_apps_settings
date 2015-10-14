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

package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public abstract class ZenRuleSelectionDialog {
    private static final String TAG = "ZenRuleSelectionDialog";
    private static final boolean DEBUG = ZenModeSettings.DEBUG;

    private final Context mContext;
    private final PackageManager mPm;
    private final AlertDialog mDialog;
    private final LinearLayout mRuleContainer;
    private final ServiceListing mServiceListing;

    public ZenRuleSelectionDialog(Context context, ServiceListing serviceListing) {
        mContext = context;
        mPm = context.getPackageManager();
        mServiceListing = serviceListing;
        final View v =
                LayoutInflater.from(context).inflate(R.layout.zen_rule_type_selection, null, false);

        mRuleContainer = (LinearLayout) v.findViewById(R.id.rule_container);
        if (mServiceListing != null) {
            bindType(defaultNewEvent());
            bindType(defaultNewSchedule());
            mServiceListing.addCallback(mServiceListingCallback);
            mServiceListing.reload();
        }
        mDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.zen_mode_choose_rule_type)
                .setView(v)
                .setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (mServiceListing != null) {
                            mServiceListing.removeCallback(mServiceListingCallback);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    public void show() {
        mDialog.show();
    }

    abstract public void onSystemRuleSelected(ZenRuleInfo ruleInfo);
    abstract public void onExternalRuleSelected(ZenRuleInfo ruleInfo);

    private void bindType(final ZenRuleInfo ri) {
        try {
            ApplicationInfo info = mPm.getApplicationInfo(ri.packageName, 0);
            final LinearLayout v = (LinearLayout) LayoutInflater.from(mContext).inflate(
                    R.layout.zen_rule_type, null, false);

            LoadIconTask task = new LoadIconTask((ImageView) v.findViewById(R.id.icon));
            task.execute(info);
            ((TextView) v.findViewById(R.id.title)).setText(ri.title);
            if (!ri.isSystem) {
                TextView subtitle = (TextView) v.findViewById(R.id.subtitle);
                subtitle.setText(info.loadLabel(mPm));
                subtitle.setVisibility(View.VISIBLE);
            }
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog.dismiss();
                    if (ri.isSystem) {
                        onSystemRuleSelected(ri);
                    } else {
                        onExternalRuleSelected(ri);
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
        event.calendar = null; // any calendar
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

    private void bindExternalRules(ZenRuleInfo[] externalRuleTypes) {
        Arrays.sort(externalRuleTypes, RULE_TYPE_COMPARATOR);
        for (ZenRuleInfo ri : externalRuleTypes) {
            bindType(ri);
        }
    }

    private final ServiceListing.Callback mServiceListingCallback = new ServiceListing.Callback() {
        @Override
        public void onServicesReloaded(List<ServiceInfo> services) {
            if (DEBUG) Log.d(TAG, "Services reloaded: count=" + services.size());
            ZenRuleInfo[] externalRuleTypes = new ZenRuleInfo[services.size()];
            for (int i = 0; i < services.size(); i++) {
                final ZenRuleInfo ri = ZenModeAutomationSettings.getRuleInfo(mPm, services.get(i));
                if (ri != null && ri.configurationActivity != null) {
                    externalRuleTypes[i] = ri;
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