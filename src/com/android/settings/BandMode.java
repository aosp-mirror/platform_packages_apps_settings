package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.os.AsyncResult;
import android.util.Log;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager;
import android.view.Window;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

/**
 * Radio Band Mode Selection Class
 * This will query the device for all available band modes
 * and display the options on the screen. If however it fails it will then
 * display all the available band modes that are in the BAND_NAMES array.
 * After the user selects a band, it will attempt to set the band mode 
 * regardless of the outcome. However if the bandmode will not work RIL.Java
 * will catch it and throw a GENERIC_FAILURE or RADIO_NOT_AVAILABLE error
 */
public class BandMode extends Activity {
    private static final String LOG_TAG = "phone";
    private static final boolean DBG = false;

    private static final int EVENT_BAND_SCAN_COMPLETED = 100;
    private static final int EVENT_BAND_SELECTION_DONE = 200;
/*
* pulled from hardware/ril/include/telephony/ril.h and cleaned up a little
* there ought to be a better way to do this...
* make queryAvailableBandMode return something other than just an int array?
*/
    private static final String[] BAND_NAMES = new String[] {
            "Automatic",
            "EURO Band     (GSM-900/DCS-1800/WCDMA-IMT-2000)",
            "USA Band      (GSM-850/PCS-1900/WCDMA-850/WCDMA-PCS-1900)",
            "JAPAN Band    (WCDMA-800/WCDMA-IMT-2000)",
            "AUS Band      (GSM-900/DCS-1800/WCDMA-850/WCDMA-IMT-2000)",
            "AUS2 Band     (GSM-900/DCS-1800/WCDMA-850)",
            "Cellular      (800-MHz)",
            "PCS           (1900-MHz)",
            "Band Class 3  (JTACS Band)",
            "Band Class 4  (Korean PCS Band)",
            "Band Class 5  (450-MHz Band)",
            "Band Class 6  (2-GMHz IMT2000 Band)",
            "Band Class 7  (Upper 700-MHz Band)",
            "Band Class 8  (1800-MHz Band)",
            "Band Class 9  (900-MHz Band)",
            "Band Class 10 (Secondary 800-MHz Band)",
            "Band Class 11 (400-MHz European PAMR Band)",
            "Band Class 15 (AWS Band)",
            "Band Class 16 (US 2.5-GHz Band)"
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
            //Always show Band 0, ie Automatic
            item = new BandListItem(0);
            mBandListAdapter.add(item);
            if (DBG) log("Add " + item.toString());
            for (int i=0; i<bands.length; i++) {
                item = new BandListItem(bands[i]);
                mBandListAdapter.add(item);
                if (DBG) log("Add " + item.toString());
            }
            addBandSuccess = true;
        }

        if (addBandSuccess == false) {
            if (DBG) log("Error in query, add default list");
            for (int i=0; i<BAND_NAMES.length; i++) {
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
