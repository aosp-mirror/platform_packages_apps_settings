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

package com.android.settings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.android.internal.app.IBatteryStats;

import android.app.Activity;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.widget.TextView;

public class BatteryHistory extends Activity {
    private static final String TAG = "BatteryHistory";
    TextView mTextView;
    IBatteryStats mBatteryInfo;

    private String getDump(BatteryStats stats) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(out, true);
            stats.dumpLocked(null, pw, null);
            pw.flush();
            pw.close();
            out.close();
            return new String(out.toByteArray(), 0);
        } catch (IOException e) {
            return "IOException";
        }
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.battery_history);
        mTextView = (TextView) findViewById(R.id.text);
        mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager.getService("batteryinfo"));
        try {
            BatteryStats stats = mBatteryInfo.getStatistics();
            String s = getDump(stats);
            mTextView.setText(s);
        } catch (RemoteException e) {
            mTextView.setText("Got RemoteException");
            Log.e(TAG, "RemoteException:", e);
        }
    }
}
