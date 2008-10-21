/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

public class DateTimeSettingsSetupWizard extends DateTimeSettings implements OnClickListener {
    private View mNextButton;
    
    @Override
    protected void onCreate(Bundle icicle) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        super.onCreate(icicle);
        setContentView(R.layout.date_time_settings_setupwizard);
        mNextButton = findViewById(R.id.next_button);
        mNextButton.setOnClickListener(this);
    }

    public void onClick(View v) {
        setResult(RESULT_OK);
        finish();
    }
}
