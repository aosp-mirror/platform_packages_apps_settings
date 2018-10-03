/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.phone;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.OperatorInfo;
import com.android.phone.NetworkScanHelper.NetworkScanCallback;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * "Networks" preference in "Mobile network" settings UI for the Phone app.
 * It's used to manually search and choose mobile network. Enabled only when
 * autoSelect preference is turned off.
 */
public class NetworkSelectListPreference extends ListPreference
        implements DialogInterface.OnCancelListener,
        Preference.OnPreferenceChangeListener{

    private static final String LOG_TAG = "networkSelect";
    private static final boolean DBG = true;

    private static final int EVENT_MANUALLY_NETWORK_SELECTION_DONE = 1;
    private static final int EVENT_NETWORK_SCAN_RESULTS = 2;
    private static final int EVENT_NETWORK_SCAN_COMPLETED = 3;
    private static final int EVENT_NETWORK_SCAN_ERROR = 4;

    //dialog ids
    private static final int DIALOG_NETWORK_SELECTION = 100;
    private static final int DIALOG_NETWORK_LIST_LOAD = 200;

    private final ExecutorService mNetworkScanExecutor = Executors.newFixedThreadPool(1);

    private List<CellInfo> mCellInfoList;
    private CellInfo mCellInfo;

    private int mSubId;
    private TelephonyManager mTelephonyManager;
    private NetworkScanHelper mNetworkScanHelper;
    private NetworkOperators mNetworkOperators;
    private List<String> mForbiddenPlmns;

    private ProgressDialog mProgressDialog;
    public NetworkSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NetworkSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onClick() {
        showProgressDialog(DIALOG_NETWORK_LIST_LOAD);
        TelephonyManager telephonyManager = (TelephonyManager)
                getContext().getSystemService(Context.TELEPHONY_SERVICE);
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... voids) {
                String[] forbiddenPlmns = telephonyManager.getForbiddenPlmns();
                return forbiddenPlmns != null ? Arrays.asList(forbiddenPlmns) : null;
            }

            @Override
            protected void onPostExecute(List<String> result) {
                mForbiddenPlmns = result;
                loadNetworksList();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_MANUALLY_NETWORK_SELECTION_DONE:
                    if (DBG) logd("hideProgressPanel");
                    dismissProgressDialog();

                    boolean isSuccessed = (boolean) msg.obj;
                    if (isSuccessed) {
                        if (DBG) {
                            logd("manual network selection: succeeded! "
                                    + getNetworkTitle(mCellInfo));
                        }
                        mNetworkOperators.displayNetworkSelectionSucceeded();
                    } else {
                        if (DBG) logd("manual network selection: failed!");
                        mNetworkOperators.displayNetworkSelectionFailed();
                    }
                    mNetworkOperators.getNetworkSelectionMode();
                    break;

                case EVENT_NETWORK_SCAN_RESULTS:
                    List<CellInfo> results = (List<CellInfo>) msg.obj;
                    results.removeIf(cellInfo -> cellInfo == null);
                    mCellInfoList = new ArrayList<>(results);
                    if (DBG) logd("CALLBACK_SCAN_RESULTS" + mCellInfoList.toString());
                    break;

                case EVENT_NETWORK_SCAN_COMPLETED:
                    if (DBG) logd("scan complete, load the cellInfosList");
                    dismissProgressDialog();
                    networksListLoaded();
                    break;
                case EVENT_NETWORK_SCAN_ERROR:
                    dismissProgressDialog();
                    displayNetworkQueryFailed();
                    mNetworkOperators.getNetworkSelectionMode();
                    break;
            }
            return;
        }
    };

    private final NetworkScanHelper.NetworkScanCallback mCallback = new NetworkScanCallback() {
        public void onResults(List<CellInfo> results) {
            if (DBG) logd("get scan results: " + results.toString());
            Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_RESULTS, results);
            msg.sendToTarget();
        }

        public void onComplete() {
            if (DBG) logd("network scan completed.");
            Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED);
            msg.sendToTarget();
        }

        public void onError(int error) {
            if (DBG) logd("network scan error.");
            Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_ERROR);
            msg.sendToTarget();
        }
    };

    @Override
    //implemented for DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
        if (DBG) logd("user manually close the dialog");
        mNetworkScanHelper.stopNetworkQuery();

        // If cancelled, we query NetworkSelectMode and update states of AutoSelect button.
        mNetworkOperators.getNetworkSelectionMode();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        // If dismissed, we query NetworkSelectMode and update states of AutoSelect button.
        if (!positiveResult) {
            mNetworkOperators.getNetworkSelectionMode();
        }
    }

    // This initialize method needs to be called for this preference to work properly.
    protected void initialize(int subId, NetworkOperators networkOperators,
            ProgressDialog progressDialog) {
        mSubId = subId;
        mNetworkOperators = networkOperators;
        // This preference should share the same progressDialog with networkOperators category.
        mProgressDialog = progressDialog;

        mTelephonyManager = TelephonyManager.from(getContext()).createForSubscriptionId(mSubId);
        mNetworkScanHelper = new NetworkScanHelper(
                mTelephonyManager, mCallback, mNetworkScanExecutor);

        setSummary(mTelephonyManager.getNetworkOperatorName());

        setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onPrepareForRemoval() {
        destroy();
        super.onPrepareForRemoval();
    }

    private void destroy() {
        dismissProgressDialog();

        if (mNetworkScanHelper != null) {
            mNetworkScanHelper.stopNetworkQuery();
        }

        mNetworkScanExecutor.shutdown();
    }

    private void displayEmptyNetworkList() {
        Toast.makeText(getContext(), R.string.empty_networks_list, Toast.LENGTH_LONG).show();
    }

    private void displayNetworkQueryFailed() {
        Toast.makeText(getContext(), R.string.network_query_error, Toast.LENGTH_LONG).show();
    }

    private void loadNetworksList() {
        if (DBG) logd("load networks list...");
        mNetworkScanHelper.startNetworkScan(
                NetworkScanHelper.NETWORK_SCAN_TYPE_WAIT_FOR_ALL_RESULTS);
    }

    private void networksListLoaded() {
        if (DBG) logd("networks list loaded");

        mNetworkOperators.getNetworkSelectionMode();
        if (mCellInfoList != null) {
            // create a preference for each item in the list.
            // just use the operator name instead of the mildly
            // confusing mcc/mnc.
            List<CharSequence> networkEntriesList = new ArrayList<>();
            List<CharSequence> networkEntryValuesList = new ArrayList<>();
            for (CellInfo cellInfo: mCellInfoList) {
                // Display each operator name only once.
                String networkTitle = getNetworkTitle(cellInfo);
                if (CellInfoUtil.isForbidden(cellInfo, mForbiddenPlmns)) {
                    networkTitle += " "
                            + getContext().getResources().getString(R.string.forbidden_network);
                }
                networkEntriesList.add(networkTitle);
                networkEntryValuesList.add(getOperatorNumeric(cellInfo));
            }
            setEntries(networkEntriesList.toArray(new CharSequence[networkEntriesList.size()]));
            setEntryValues(networkEntryValuesList.toArray(
                    new CharSequence[networkEntryValuesList.size()]));

            super.onClick();
        } else {
            displayEmptyNetworkList();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            try {
                mProgressDialog.dismiss();
            } catch (IllegalArgumentException ex) {
                loge("Can't close the progress dialog " + ex);
            }
        }
    }

    private void showProgressDialog(int id) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getContext());
        } else {
            // Dismiss progress bar if it's showing now.
            dismissProgressDialog();
        }

        switch (id) {
            case DIALOG_NETWORK_SELECTION:
                final String networkSelectMsg = getContext().getResources()
                        .getString(R.string.register_on_network,
                                getNetworkTitle(mCellInfo));
                mProgressDialog.setMessage(networkSelectMsg);
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setIndeterminate(true);
                break;
            case DIALOG_NETWORK_LIST_LOAD:
                mProgressDialog.setMessage(
                        getContext().getResources().getString(R.string.load_networks_progress));
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setCancelable(true);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setOnCancelListener(this);
                break;
            default:
        }
        mProgressDialog.show();
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on this button.
     *
     * @param preference is the preference to be changed, should be network select button.
     * @param newValue should be the value of the selection as index of operators.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int operatorIndex = findIndexOfValue((String) newValue);
        mCellInfo = mCellInfoList.get(operatorIndex);
        if (DBG) logd("selected network: " + mCellInfo.toString());

        MetricsLogger.action(getContext(),
                MetricsEvent.ACTION_MOBILE_NETWORK_MANUAL_SELECT_NETWORK);

        if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
            ThreadUtils.postOnBackgroundThread(() -> {
                final OperatorInfo operatorInfo = getOperatorInfoFromCellInfo(mCellInfo);
                if (DBG) logd("manually selected network: " + operatorInfo.toString());
                boolean isSuccessed = mTelephonyManager.setNetworkSelectionModeManual(
                        operatorInfo, true /* persistSelection */);
                Message msg = mHandler.obtainMessage(EVENT_MANUALLY_NETWORK_SELECTION_DONE);
                msg.obj = isSuccessed;
                msg.sendToTarget();
            });
        } else {
            loge("Error selecting network, subscription Id is invalid " + mSubId);
        }

        return true;
    }

    /**
     * Returns the title of the network obtained in the manual search.
     *
     * @param cellInfo contains the information of the network.
     * @return Long Name if not null/empty, otherwise Short Name if not null/empty,
     * else MCCMNC string.
     */
    private String getNetworkTitle(CellInfo cellInfo) {
        OperatorInfo ni = getOperatorInfoFromCellInfo(cellInfo);

        if (!TextUtils.isEmpty(ni.getOperatorAlphaLong())) {
            return ni.getOperatorAlphaLong();
        } else if (!TextUtils.isEmpty(ni.getOperatorAlphaShort())) {
            return ni.getOperatorAlphaShort();
        } else {
            BidiFormatter bidiFormatter = BidiFormatter.getInstance();
            return bidiFormatter.unicodeWrap(ni.getOperatorNumeric(), TextDirectionHeuristics.LTR);
        }
    }

    /**
     * Returns the operator numeric (MCCMNC) obtained in the manual search.
     *
     * @param cellInfo contains the information of the network.
     * @return MCCMNC string.
     */
    private String getOperatorNumeric(CellInfo cellInfo) {
        return getOperatorInfoFromCellInfo(cellInfo).getOperatorNumeric();
    }

    /**
     * Wrap a cell info into an operator info.
     */
    private OperatorInfo getOperatorInfoFromCellInfo(CellInfo cellInfo) {
        OperatorInfo oi;
        if (cellInfo instanceof CellInfoLte) {
            CellInfoLte lte = (CellInfoLte) cellInfo;
            oi = new OperatorInfo(
                    (String) lte.getCellIdentity().getOperatorAlphaLong(),
                    (String) lte.getCellIdentity().getOperatorAlphaShort(),
                    lte.getCellIdentity().getMobileNetworkOperator());
        } else if (cellInfo instanceof CellInfoWcdma) {
            CellInfoWcdma wcdma = (CellInfoWcdma) cellInfo;
            oi = new OperatorInfo(
                    (String) wcdma.getCellIdentity().getOperatorAlphaLong(),
                    (String) wcdma.getCellIdentity().getOperatorAlphaShort(),
                    wcdma.getCellIdentity().getMobileNetworkOperator());
        } else if (cellInfo instanceof CellInfoGsm) {
            CellInfoGsm gsm = (CellInfoGsm) cellInfo;
            oi = new OperatorInfo(
                    (String) gsm.getCellIdentity().getOperatorAlphaLong(),
                    (String) gsm.getCellIdentity().getOperatorAlphaShort(),
                    gsm.getCellIdentity().getMobileNetworkOperator());
        } else if (cellInfo instanceof CellInfoCdma) {
            CellInfoCdma cdma = (CellInfoCdma) cellInfo;
            oi = new OperatorInfo(
                    (String) cdma.getCellIdentity().getOperatorAlphaLong(),
                    (String) cdma.getCellIdentity().getOperatorAlphaShort(),
                    "" /* operator numeric */);
        } else {
            oi = new OperatorInfo("", "", "");
        }
        return oi;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.mDialogListEntries = getEntries();
        myState.mDialogListEntryValues = getEntryValues();
        myState.mCellInfoList = mCellInfoList;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;

        if (getEntries() == null && myState.mDialogListEntries != null) {
            setEntries(myState.mDialogListEntries);
        }
        if (getEntryValues() == null && myState.mDialogListEntryValues != null) {
            setEntryValues(myState.mDialogListEntryValues);
        }
        if (mCellInfoList == null && myState.mCellInfoList != null) {
            mCellInfoList = myState.mCellInfoList;
        }

        super.onRestoreInstanceState(myState.getSuperState());
    }

    /**
     *  We save entries, entryValues and operatorInfoList into bundle.
     *  At onCreate of fragment, dialog will be restored if it was open. In this case,
     *  we need to restore entries, entryValues and operatorInfoList. Without those information,
     *  onPreferenceChange will fail if user select network from the dialog.
     */
    private static class SavedState extends BaseSavedState {
        CharSequence[] mDialogListEntries;
        CharSequence[] mDialogListEntryValues;
        List<CellInfo> mCellInfoList;

        SavedState(Parcel source) {
            super(source);
            final ClassLoader boot = Object.class.getClassLoader();
            mDialogListEntries = source.readCharSequenceArray();
            mDialogListEntryValues = source.readCharSequenceArray();
            mCellInfoList = source.readParcelableList(mCellInfoList, boot);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeCharSequenceArray(mDialogListEntries);
            dest.writeCharSequenceArray(mDialogListEntryValues);
            dest.writeParcelableList(mCellInfoList, flags);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    private void logd(String msg) {
        Log.d(LOG_TAG, "[NetworksList] " + msg);
    }

    private void loge(String msg) {
        Log.e(LOG_TAG, "[NetworksList] " + msg);
    }
}