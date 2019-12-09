/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.development;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;

/** Activity shows terms of service for a given DSU package */
public class DSUTermsOfServiceActivity extends Activity {
    public static final String KEY_TOS = "KEY_TOS";

    private void installDSU(Intent intent) {
        intent.setClassName("com.android.dynsystem", "com.android.dynsystem.VerificationActivity");
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.dsu_terms_of_service);
        TextView tv = findViewById(R.id.tos_content);
        Intent intent = getIntent();
        if (!intent.hasExtra(KEY_TOS)) {
            finish();
        }
        String tos = intent.getStringExtra(KEY_TOS);
        if (TextUtils.isEmpty(tos)) {
            installDSU(intent);
        } else {
            tv.setText(tos);
            Button accept = findViewById(R.id.accept);
            accept.setOnClickListener(
                    new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            installDSU(intent);
                        }
                    });
        }
    }
}
