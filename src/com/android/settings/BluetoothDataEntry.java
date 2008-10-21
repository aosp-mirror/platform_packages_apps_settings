/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BluetoothDataEntry extends Activity implements OnKeyListener {
    
    private Bundle extras;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.bluetooth_data_entry);

        mDataLabel = (TextView)findViewById(R.id.dataLabel);
        mDataEntry = (EditText)findViewById(R.id.dataEntry);
        mConfirmButton = (Button)findViewById(R.id.confirmButton);
        mCancelButton = (Button)findViewById(R.id.cancelButton);

        mDataEntry.setOnKeyListener(this);
        Intent intent = getIntent();
        String label = null;
        {
            String labelExtra = intent.getStringExtra("label");
            if (labelExtra != null) {
                label = labelExtra;
            }
        }
        extras = intent.getBundleExtra("extras");
        if (label != null && label.length() > 0) {
            mDataLabel.setText(label);
        }

        mConfirmButton.setOnClickListener(new ConfirmButtonListener());
        mCancelButton.setOnClickListener(new CancelButtonListener());
    }

    private class ConfirmButtonListener implements OnClickListener {
        public void onClick(View v) {
            activityResult(RESULT_OK, mDataEntry.getText().toString(), extras);
        }
    }

    private class CancelButtonListener implements OnClickListener {
        public void onClick(View v) {
            activityResult(RESULT_CANCELED, null, null);
        }
    }

    protected void activityResult(int result, String data, Bundle extras) {
        setResult(result, (new Intent()).setAction(data).putExtras(extras));
        finish();
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER 
                || keyCode == KeyEvent.KEYCODE_ENTER) {
            activityResult(RESULT_OK, mDataEntry.getText().toString(), extras);
            return true;
        }
        return false;
    }

    protected TextView mDataLabel;
    protected EditText mDataEntry;
    protected Button mConfirmButton;
    protected Button mCancelButton;
}
