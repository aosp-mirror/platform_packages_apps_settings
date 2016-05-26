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
import android.os.UserManager;
import android.view.MenuItem;
import android.view.WindowManager;

public abstract class ConfirmDeviceCredentialBaseActivity extends SettingsActivity {

    private static final String STATE_IS_KEYGUARD_LOCKED = "STATE_IS_KEYGUARD_LOCKED";

    private boolean mRestoring;
    private boolean mDark;
    private boolean mEnterAnimationPending;
    private boolean mFirstTimeVisible = true;
    private boolean mIsKeyguardLocked = false;

    @Override
    protected void onCreate(Bundle savedState) {
        int credentialOwnerUserId = Utils.getCredentialOwnerUserId(this,
                Utils.getUserIdFromBundle(this, getIntent().getExtras()));
        if (Utils.isManagedProfile(UserManager.get(this), credentialOwnerUserId)) {
            setTheme(R.style.Theme_ConfirmDeviceCredentialsWork);
        } else if (getIntent().getBooleanExtra(
                ConfirmDeviceCredentialBaseFragment.DARK_THEME, false)) {
            setTheme(R.style.Theme_ConfirmDeviceCredentialsDark);
            mDark = true;
        }
        super.onCreate(savedState);
        mIsKeyguardLocked = savedState == null
                ? getSystemService(KeyguardManager.class).isKeyguardLocked()
                : savedState.getBoolean(STATE_IS_KEYGUARD_LOCKED, false);
        // If the activity is launched, not due to config change, when keyguard is locked and the
        // flag is set, assume it's launched on top of keyguard on purpose.
        // TODO: Don't abuse SHOW_WHEN_LOCKED and don't check isKeyguardLocked.
        // Set extra SHOW_WHEN_LOCKED and WindowManager FLAG_SHOW_WHEN_LOCKED only if it's
        // truly on top of keyguard on purpose
        if (mIsKeyguardLocked && getIntent().getBooleanExtra(
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_KEYGUARD_LOCKED, mIsKeyguardLocked);
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
}
