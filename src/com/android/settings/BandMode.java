package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;


/**
 * Radio Band Mode Selection Class
 *
 * It will query baseband about all available band modes and display them
 * in screen. It will display all six band modes if the query failed.
 *
 * After user select one band, it will send the selection to baseband.
 *
 * It will alter user the result of select operation and exit, no matter success
 * or not.
 *
 */
public class BandMode extends Activity {
    private static final String LOG_TAG = "phone";
    private static final boolean DBG = false;

    private static final int EVENT_BAND_SCAN_COMPLETED = 100;
    private static final int EVENT_BAND_SELECTION_DONE = 200;

    //Directly maps to RIL_RadioBandMode from ril.h
    private static final String[] BAND_NAMES = new String[] {
            "Automatic",
            "Europe",
            "United States",
            "Japan",
            "Australia",
            "Australia 2",
            "Cellular 800",
            "PCS",
            "Class 3 (JTACS)",
            "Class 4 (Korea-PCS)",
            "Class 5",
            "Class 6 (IMT2000)",
            "Class 7 (700Mhz-Upper)",
            "Class 8 (1800Mhz-Upper)",
            "Class 9 (900Mhz)",
            "Class 10 (800Mhz-Secondary)",
            "Class 11 (Europe PAMR 400Mhz)",
            "Class 15 (US-AWS)",
            "Class 16 (US-2500Mhz)"
    };

    private ListView mBandList;
    private ArrayAdapter mBandListAdapter;
    private BandListItem mTargetBand = null;
    private DialogInterface mProgressPanel;

    private Phone mPhone = null;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.band_mode);

        setTitle(getString(R.string.band_mode_title));
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                                    WindowManager.LayoutParams.WRAP_CONTENT);

        mPhone = PhoneFactory.getDefaultPhone();

        mBandList = (ListView) findViewById(R.id.band);
        mBandListAdapter = new ArrayAdapter<BandListItem>(this,
                android.R.layout.simple_list_item_1);
        mBandList.setAdapter(mBandListAdapter);
        mBandList.setOnItemClickListener(mBandSelectionHandler);

        loadBandList();
    }

    private AdapterView.OnItemClickListener mBandSelectionHandler =
            new AdapterView.OnItemClickListener () {
                public void onItemClick(AdapterView parent, View v,
                        int position, long id) {

                    getWindow().setFeatureInt(
                            Window.FEATURE_INDETERMINATE_PROGRESS,
                            Window.PROGRESS_VISIBILITY_ON);

                    mTargetBand = (BandListItem) parent.getAdapter().getItem(position);

                    if (DBG) log("Select band : " + mTargetBand.toString());

                    Message msg =
                            mHandler.obtainMessage(EVENT_BAND_SELECTION_DONE);
                    mPhone.setBandMode(mTargetBand.getBand(), msg);
                }
            };

    static private class BandListItem {
        private int mBandMode = Phone.BM_UNSPECIFIED;

        public BandListItem(int bm) {
            mBandMode = bm;
        }

        public int getBand() {
            return mBandMode;
        }

        public String toString() {
            if (mBandMode >= BAND_NAMES.length) return "Band mode " + mBandMode;
            return BAND_NAMES[mBandMode];
        }
    }

    private void loadBandList() {
        String str = getString(R.string.band_mode_loading);

        if (DBG) log(str);


        //ProgressDialog.show(this, null, str, true, true, null);
        mProgressPanel = new AlertDialog.Builder(this)
            .setMessage(str)
            .show();

        Message msg = mHandler.obtainMessage(EVENT_BAND_SCAN_COMPLETED);
        mPhone.queryAvailableBandMode(msg);

    }

    private void bandListLoaded(AsyncResult result) {
        if (DBG) log("network list loaded");

        if (mProgressPanel != null) mProgressPanel.dismiss();

        clearList();

        boolean addBandSuccess = false;
        BandListItem item;

        if (result.result != null) {
            int bands[] = (int[])result.result;

            if(bands.length == 0) {
                Log.wtf(LOG_TAG, "No Supported Band Modes");
                return;
            }

            int size = bands[0];

            if (size > 0) {
                mBandListAdapter.add(
                        new BandListItem(Phone.BM_UNSPECIFIED)); //Always include AUTOMATIC
                for (int i=1; i<=size; i++) {
                    if (bands[i] == Phone.BM_UNSPECIFIED) {
                        continue;
                    }
                    item = new BandListItem(bands[i]);
                    mBandListAdapter.add(item);
                    if (DBG) log("Add " + item.toString());
                }
                addBandSuccess = true;
            }
        }

        if (addBandSuccess == false) {
            if (DBG) log("Error in query, add default list");
            for (int i=0; i<Phone.BM_NUM_BAND_MODES; i++) {
                item = new BandListItem(i);
                mBandListAdapter.add(item);
                if (DBG) log("Add default " + item.toString());
            }
        }
        mBandList.requestFocus();
    }

    private void displayBandSelectionResult(Throwable ex) {
        String status = getString(R.string.band_mode_set)
                +" [" + mTargetBand.toString() + "] ";

        if (ex != null) {
            status = status + getString(R.string.band_mode_failed);
        } else {
            status = status + getString(R.string.band_mode_succeeded);
        }

        mProgressPanel = new AlertDialog.Builder(this)
            .setMessage(status)
            .setPositiveButton(android.R.string.ok, null).show();
    }

    private void clearList() {
        while(mBandListAdapter.getCount() > 0) {
            mBandListAdapter.remove(
                    mBandListAdapter.getItem(0));
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "[BandsList] " + msg);
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_BAND_SCAN_COMPLETED:
                    ar = (AsyncResult) msg.obj;

                    bandListLoaded(ar);
                    break;

                case EVENT_BAND_SELECTION_DONE:
                    ar = (AsyncResult) msg.obj;

                    getWindow().setFeatureInt(
                            Window.FEATURE_INDETERMINATE_PROGRESS,
                            Window.PROGRESS_VISIBILITY_OFF);

                    if (!isFinishing()) {
                        displayBandSelectionResult(ar.exception);
                    }
                    break;
            }
        }
    };


}
