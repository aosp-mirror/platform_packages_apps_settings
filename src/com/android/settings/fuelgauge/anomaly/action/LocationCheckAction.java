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

package com.android.settings.fuelgauge.anomaly.action;

import android.content.Context;
import android.content.pm.permission.RuntimePermissionPresenter;
import android.support.v4.content.PermissionChecker;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.fuelgauge.anomaly.Anomaly;

/**
 * Location action for anomaly app, which means to turn off location permission for this app
 */
public class LocationCheckAction extends AnomalyAction {

    private static final String TAG = "LocationCheckAction";
    private static final String LOCATION_PERMISSION = "android.permission-group.LOCATION";

    private final RuntimePermissionPresenter mRuntimePermissionPresenter;

    public LocationCheckAction(Context context) {
        super(context);
        mRuntimePermissionPresenter = RuntimePermissionPresenter.getInstance(context);
        mActionMetricKey = MetricsProto.MetricsEvent.ACTION_APP_LOCATION_CHECK;
    }

    @Override
    public void handlePositiveAction(Anomaly anomaly, int contextMetricsKey) {
        super.handlePositiveAction(anomaly, contextMetricsKey);
        mRuntimePermissionPresenter.revokeRuntimePermission(anomaly.packageName,
                LOCATION_PERMISSION);
    }

    @Override
    public boolean isActionActive(Anomaly anomaly) {
        return PermissionChecker.checkPermission(mContext, LOCATION_PERMISSION, -1, anomaly.uid,
                anomaly.packageName) == PermissionChecker.PERMISSION_GRANTED;
    }

    @Override
    public int getActionType() {
        return Anomaly.AnomalyActionType.LOCATION_CHECK;
    }
}
