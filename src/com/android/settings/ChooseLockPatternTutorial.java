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

import com.android.internal.widget.LockPatternUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ChooseLockPatternTutorial extends Activity implements View.OnClickListener {
    private static final int REQUESTCODE_EXAMPLE = 1;
    
    private View mNextButton;
    private View mSkipButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Don't show the tutorial if the user has seen it before.
        LockPatternUtils lockPatternUtils = new LockPatternUtils(getContentResolver());
        if (savedInstanceState == null && lockPatternUtils.isPatternEverChosen()) {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.ChooseLockPattern");
            startActivity(intent);
            finish();
        } else {
            initViews();
        }
    }
    
    private void initViews() {
        setContentView(R.layout.choose_lock_pattern_tutorial);
        mNextButton = findViewById(R.id.next_button);
        mNextButton.setOnClickListener(this);
        mSkipButton = findViewById(R.id.skip_button);
        mSkipButton.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (v == mSkipButton) {
            // Canceling, so finish all
            setResult(ChooseLockPattern.RESULT_FINISHED);
            finish();
        } else if (v == mNextButton) {
            startActivityForResult(new Intent(this, ChooseLockPatternExample.class),
                    REQUESTCODE_EXAMPLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUESTCODE_EXAMPLE && resultCode == ChooseLockPattern.RESULT_FINISHED) {
            setResult(resultCode);
            finish();
        }
    }
    
}

