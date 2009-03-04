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

import android.app.Activity;
import android.widget.EditText;
import android.widget.Button;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.text.TextUtils;
import android.text.Spannable;
import android.text.Selection;
import android.net.Uri;

/**
 * A simple activity that provides a UI for sending intents
 */
public class DebugIntentSender extends Activity {
    private EditText mIntentField;
    private EditText mDataField;
    private EditText mAccountField;
    private EditText mResourceField;
    private Button mSendBroadcastButton;
    private Button mStartActivityButton;
    private View.OnClickListener mClicked = new View.OnClickListener() {
        public void onClick(View v) {
            if ((v == mSendBroadcastButton) ||
                       (v == mStartActivityButton)) {
                String intentAction = mIntentField.getText().toString();
                String intentData = mDataField.getText().toString();
                String account = mAccountField.getText().toString();
                String resource = mResourceField.getText().toString();

                Intent intent = new Intent(intentAction);
                if (!TextUtils.isEmpty(intentData)) {
                    intent.setData(Uri.parse(intentData));
                }
                intent.putExtra("account", account);
                intent.putExtra("resource", resource);
                if (v == mSendBroadcastButton) {
                    sendBroadcast(intent);
                } else {
                    startActivity(intent);
                }

                setResult(RESULT_OK);
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.intent_sender);

        mIntentField = (EditText) findViewById(R.id.intent);
        mIntentField.setText(Intent.ACTION_SYNC);
        Selection.selectAll((Spannable) mIntentField.getText());

        mDataField = (EditText) findViewById(R.id.data);
        mDataField.setBackgroundResource(android.R.drawable.editbox_background);

        mAccountField = (EditText) findViewById(R.id.account);
        mResourceField = (EditText) findViewById(R.id.resource);

        mSendBroadcastButton = (Button) findViewById(R.id.sendbroadcast);
        mSendBroadcastButton.setOnClickListener(mClicked);

        mStartActivityButton = (Button) findViewById(R.id.startactivity);
        mStartActivityButton.setOnClickListener(mClicked);
    }
}
