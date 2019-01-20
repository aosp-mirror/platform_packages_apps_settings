/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.enterprise.ActionDisabledByAdminDialogHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;

/**
 * Base class for settings screens that should be pin protected when in restricted mode or
 * that will display an admin support message in case an admin has disabled the options.
 * The constructor for this class will take the restriction key that this screen should be
 * locked by.  If {@link RestrictionsManager.hasRestrictionsProvider()} and
 * {@link UserManager.hasUserRestriction()}, then the user will have to enter the restrictions
 * pin before seeing the Settings screen.
 *
 * If this settings screen should be pin protected whenever
 * {@link RestrictionsManager.hasRestrictionsProvider()} returns true, pass in
 * {@link RESTRICT_IF_OVERRIDABLE} to the constructor instead of a restrictions key.
 *
 * @deprecated Use {@link RestrictedDashboardFragment} instead
 */
@Deprecated
public abstract class RestrictedSettingsFragment extends SettingsPreferenceFragment {

    protected static final String RESTRICT_IF_OVERRIDABLE = "restrict_if_overridable";

    // No RestrictedSettingsFragment screens should use this number in startActivityForResult.
    @VisibleForTesting static final int REQUEST_PIN_CHALLENGE = 12309;

    private static final String KEY_CHALLENGE_SUCCEEDED = "chsc";
    private static final String KEY_CHALLENGE_REQUESTED = "chrq";

    // If the restriction PIN is entered correctly.
    private boolean mChallengeSucceeded;
    private boolean mChallengeRequested;

    private UserManager mUserManager;
    private RestrictionsManager mRestrictionsManager;

    private final String mRestrictionKey;
    private EnforcedAdmin mEnforcedAdmin;
    private TextView mEmptyTextView;

    private boolean mOnlyAvailableForAdmins = false;
    private boolean mIsAdminUser;

    // Receiver to clear pin status when the screen is turned off.
    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mChallengeRequested) {
                mChallengeSucceeded = false;
                mChallengeRequested = false;
            }
        }
    };

    @VisibleForTesting
    AlertDialog mActionDisabledDialog;

    /**
     * @param restrictionKey The restriction key to check before pin protecting
     *            this settings page. Pass in {@link RESTRICT_IF_OVERRIDABLE} if it should
     *            be protected whenever a restrictions provider is set. Pass in
     *            null if it should never be protected.
     */
    public RestrictedSettingsFragment(String restrictionKey) {
        mRestrictionKey = restrictionKey;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mRestrictionsManager = (RestrictionsManager) getSystemService(Context.RESTRICTIONS_SERVICE);
        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);
        mIsAdminUser = mUserManager.isAdminUser();

        if (icicle != null) {
            mChallengeSucceeded = icicle.getBoolean(KEY_CHALLENGE_SUCCEEDED, false);
            mChallengeRequested = icicle.getBoolean(KEY_CHALLENGE_REQUESTED, false);
        }

        IntentFilter offFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        offFilter.addAction(Intent.ACTION_USER_PRESENT);
        getActivity().registerReceiver(mScreenOffReceiver, offFilter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mEmptyTextView = initEmptyTextView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (getActivity().isChangingConfigurations()) {
            outState.putBoolean(KEY_CHALLENGE_REQUESTED, mChallengeRequested);
            outState.putBoolean(KEY_CHALLENGE_SUCCEEDED, mChallengeSucceeded);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (shouldBeProviderProtected(mRestrictionKey)) {
            ensurePin();
        }
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mScreenOffReceiver);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PIN_CHALLENGE) {
            if (resultCode == Activity.RESULT_OK) {
                mChallengeSucceeded = true;
                mChallengeRequested = false;
                if (mActionDisabledDialog != null && mActionDisabledDialog.isShowing()) {
                    mActionDisabledDialog.setOnDismissListener(null);
                    mActionDisabledDialog.dismiss();
                }
            } else {
                mChallengeSucceeded = false;
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void ensurePin() {
        if (!mChallengeSucceeded && !mChallengeRequested
                && mRestrictionsManager.hasRestrictionsProvider()) {
            Intent intent = mRestrictionsManager.createLocalApprovalIntent();
            if (intent != null) {
                mChallengeRequested = true;
                mChallengeSucceeded = false;
                PersistableBundle request = new PersistableBundle();
                request.putString(RestrictionsManager.REQUEST_KEY_MESSAGE,
                        getResources().getString(R.string.restr_pin_enter_admin_pin));
                intent.putExtra(RestrictionsManager.EXTRA_REQUEST_BUNDLE, request);
                startActivityForResult(intent, REQUEST_PIN_CHALLENGE);
            }
        }
    }

    /**
     * Returns true if this activity is restricted, but no restrictions provider has been set.
     * Used to determine if the settings UI should disable UI.
     */
    protected boolean isRestrictedAndNotProviderProtected() {
        if (mRestrictionKey == null || RESTRICT_IF_OVERRIDABLE.equals(mRestrictionKey)) {
            return false;
        }
        return mUserManager.hasUserRestriction(mRestrictionKey)
                && !mRestrictionsManager.hasRestrictionsProvider();
    }

    protected boolean hasChallengeSucceeded() {
        return (mChallengeRequested && mChallengeSucceeded) || !mChallengeRequested;
    }

    /**
     * Returns true if this restrictions key is locked down.
     */
    protected boolean shouldBeProviderProtected(String restrictionKey) {
        if (restrictionKey == null) {
            return false;
        }
        boolean restricted = RESTRICT_IF_OVERRIDABLE.equals(restrictionKey)
                || mUserManager.hasUserRestriction(mRestrictionKey);
        return restricted && mRestrictionsManager.hasRestrictionsProvider();
    }

    protected TextView initEmptyTextView() {
        TextView emptyView = (TextView) getActivity().findViewById(android.R.id.empty);
        return emptyView;
    }

    public EnforcedAdmin getRestrictionEnforcedAdmin() {
        mEnforcedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(getActivity(),
                mRestrictionKey, UserHandle.myUserId());
        if (mEnforcedAdmin != null && mEnforcedAdmin.user == null) {
            mEnforcedAdmin.user = UserHandle.of(UserHandle.myUserId());
        }
        return mEnforcedAdmin;
    }

    public TextView getEmptyTextView() {
        return mEmptyTextView;
    }

    @Override
    protected void onDataSetChanged() {
        highlightPreferenceIfNeeded();
        if (isUiRestrictedByOnlyAdmin()
                && (mActionDisabledDialog == null || !mActionDisabledDialog.isShowing())) {
            final EnforcedAdmin admin = getRestrictionEnforcedAdmin();
            mActionDisabledDialog = new ActionDisabledByAdminDialogHelper(getActivity())
                    .prepareDialogBuilder(mRestrictionKey, admin)
                    .setOnDismissListener(__ -> getActivity().finish())
                    .show();
            setEmptyView(new View(getContext()));
        } else if (mEmptyTextView != null) {
            setEmptyView(mEmptyTextView);
        }
        super.onDataSetChanged();
    }

    public void setIfOnlyAvailableForAdmins(boolean onlyForAdmins) {
        mOnlyAvailableForAdmins = onlyForAdmins;
    }

    /**
     * Returns whether restricted or actionable UI elements should be removed or disabled.
     */
    protected boolean isUiRestricted() {
        return isRestrictedAndNotProviderProtected() || !hasChallengeSucceeded()
                || (!mIsAdminUser && mOnlyAvailableForAdmins);
    }

    protected boolean isUiRestrictedByOnlyAdmin() {
        return isUiRestricted() && !mUserManager.hasBaseUserRestriction(mRestrictionKey,
                UserHandle.of(UserHandle.myUserId())) && (mIsAdminUser || !mOnlyAvailableForAdmins);
    }
}
