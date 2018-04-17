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

package com.android.settings.fuelgauge.anomaly;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.fuelgauge.anomaly.action.AnomalyAction;

/**
 * Dialog Fragment to show action dialog for each anomaly
 */
public class AnomalyDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener {

    private static final String ARG_ANOMALY = "anomaly";
    private static final String ARG_METRICS_KEY = "metrics_key";

    @VisibleForTesting
    Anomaly mAnomaly;
    @VisibleForTesting
    AnomalyUtils mAnomalyUtils;

    /**
     * Listener to give the control back to target fragment
     */
    public interface AnomalyDialogListener {
        /**
         * This method is invoked once anomaly is handled, then target fragment could do
         * extra work. One example is that fragment could remove the anomaly preference
         * since it has been handled
         *
         * @param anomaly that has been handled
         */
        void onAnomalyHandled(Anomaly anomaly);
    }

    public static AnomalyDialogFragment newInstance(Anomaly anomaly, int metricsKey) {
        AnomalyDialogFragment dialogFragment = new AnomalyDialogFragment();

        Bundle args = new Bundle(2);
        args.putParcelable(ARG_ANOMALY, anomaly);
        args.putInt(ARG_METRICS_KEY, metricsKey);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAnomalyUtils();
    }

    @VisibleForTesting
    void initAnomalyUtils() {
        mAnomalyUtils = AnomalyUtils.getInstance(getContext());
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIALOG_HANDLE_ANOMALY;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final AnomalyDialogListener lsn = (AnomalyDialogListener) getTargetFragment();
        if (lsn == null) {
            return;
        }

        final AnomalyAction anomalyAction = mAnomalyUtils.getAnomalyAction(mAnomaly);
        final int metricsKey = getArguments().getInt(ARG_METRICS_KEY);

        anomalyAction.handlePositiveAction(mAnomaly, metricsKey);
        lsn.onAnomalyHandled(mAnomaly);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        final Context context = getContext();
        final AnomalyUtils anomalyUtils = AnomalyUtils.getInstance(context);

        mAnomaly = bundle.getParcelable(ARG_ANOMALY);
        anomalyUtils.logAnomaly(mMetricsFeatureProvider, mAnomaly,
                MetricsProto.MetricsEvent.DIALOG_HANDLE_ANOMALY);

        final AnomalyAction anomalyAction = mAnomalyUtils.getAnomalyAction(mAnomaly);
        switch (anomalyAction.getActionType()) {
            case Anomaly.AnomalyActionType.FORCE_STOP:
                return new AlertDialog.Builder(context)
                        .setTitle(R.string.dialog_stop_title)
                        .setMessage(getString(mAnomaly.type == Anomaly.AnomalyType.WAKE_LOCK
                                ? R.string.dialog_stop_message
                                : R.string.dialog_stop_message_wakeup_alarm, mAnomaly.displayName))
                        .setPositiveButton(R.string.dialog_stop_ok, this)
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
            case Anomaly.AnomalyActionType.STOP_AND_BACKGROUND_CHECK:
                return new AlertDialog.Builder(context)
                        .setTitle(R.string.dialog_background_check_title)
                        .setMessage(getString(R.string.dialog_background_check_message,
                                mAnomaly.displayName))
                        .setPositiveButton(R.string.dialog_background_check_ok, this)
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
            case Anomaly.AnomalyActionType.LOCATION_CHECK:
                return new AlertDialog.Builder(context)
                        .setTitle(R.string.dialog_location_title)
                        .setMessage(getString(R.string.dialog_location_message,
                                mAnomaly.displayName))
                        .setPositiveButton(R.string.dialog_location_ok, this)
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .create();
            default:
                throw new IllegalArgumentException("unknown type " + mAnomaly.type);
        }
    }

}
