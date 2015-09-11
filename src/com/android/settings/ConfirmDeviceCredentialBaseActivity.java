/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings;

import android.app.Fragment;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.WindowManager;

public abstract class ConfirmDeviceCredentialBaseActivity extends SettingsActivity {

    private boolean mRestoring;
    private boolean mDark;
    private boolean mEnterAnimationPending;
    private boolean mFirstTimeVisible = true;
    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedState) {
        if (getIntent().getBooleanExtra(ConfirmDeviceCredentialBaseFragment.DARK_THEME, false)) {
            setTheme(R.style.Theme_ConfirmDeviceCredentialsDark);
            mDark = true;
        }
        super.onCreate(savedState);
        boolean deviceLocked = getSystemService(KeyguardManager.class).isKeyguardLocked();
        if (deviceLocked && getIntent().getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        CharSequence msg = getIntent().getStringExtra(
                ConfirmDeviceCredentialBaseFragment.TITLE_TEXT);
        setTitle(msg);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        mRestoring = savedState != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isChangingConfigurations() && !mRestoring && mDark && mFirstTimeVisible) {
            mFirstTimeVisible = false;
            prepareEnterAnimation();
            mEnterAnimationPending = true;
            mHandler.postDelayed(mEnterAnimationCompleteTimeoutRunnable, 1000);
        }
    }

    private ConfirmDeviceCredentialBaseFragment getFragment() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_content);
        if (fragment != null && fragment instanceof ConfirmDeviceCredentialBaseFragment) {
            return (ConfirmDeviceCredentialBaseFragment) fragment;
        }
        return null;
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        if (mEnterAnimationPending) {
            mHandler.removeCallbacks(mEnterAnimationCompleteTimeoutRunnable);
            startEnterAnimation();
            mEnterAnimationPending = false;
        }
    }

    public void prepareEnterAnimation() {
        getFragment().prepareEnterAnimation();
    }

    public void startEnterAnimation() {
        getFragment().startEnterAnimation();
    }

    /**
     * Workaround for a bug in window manager which results that onEnterAnimationComplete doesn't
     * get called in all cases.
     */
    private final Runnable mEnterAnimationCompleteTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            onEnterAnimationComplete();
        }
    };
}
