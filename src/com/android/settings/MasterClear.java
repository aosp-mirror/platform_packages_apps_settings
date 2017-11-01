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

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnScrollChangeListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.ConfirmLockPattern;
import com.android.settingslib.RestrictedLockUtils;

import java.util.List;

/**
 * Confirm and execute a reset of the device to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE PHONE" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the initial screen.
 */
public class MasterClear extends OptionsMenuFragment {
    private static final String TAG = "MasterClear";

    private static final int KEYGUARD_REQUEST = 55;

    static final String ERASE_EXTERNAL_EXTRA = "erase_sd";
    static final String ERASE_ESIMS_EXTRA = "erase_esim";

    private View mContentView;
    private Button mInitiateButton;
    private View mExternalStorageContainer;
    @VisibleForTesting CheckBox mExternalStorage;
    private ScrollView mScrollView;

    private final OnGlobalLayoutListener mOnGlobalLayoutListener = new OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            mScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);
            mInitiateButton.setEnabled(hasReachedBottom(mScrollView));
        }
    };

    /**
     * Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     * @param request the request code to be returned once confirmation finishes
     * @return true if confirmation launched
     */
    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(
                request, res.getText(R.string.master_clear_title));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != KEYGUARD_REQUEST) {
            return;
        }

        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (resultCode == Activity.RESULT_OK) {
            showFinalConfirmation();
        } else {
            establishInitialState();
        }
    }

    @VisibleForTesting
    void showFinalConfirmation() {
        Bundle args = new Bundle();
        args.putBoolean(ERASE_EXTERNAL_EXTRA, mExternalStorage.isChecked());
        // TODO: Offer the user a choice to wipe eSIMs when it is technically feasible to do so.
        args.putBoolean(ERASE_ESIMS_EXTRA, true);
        ((SettingsActivity) getActivity()).startPreferencePanel(
                this, MasterClearConfirm.class.getName(),
                args, R.string.master_clear_confirm_title, null, null, 0);
    }

    /**
     * If the user clicks to begin the reset sequence, we next require a
     * keyguard confirmation if the user has currently enabled one.  If there
     * is no keyguard available, we simply go to the final confirmation prompt.
     *
     * If the user is in demo mode, route to the demo mode app for confirmation.
     */
    @VisibleForTesting
    protected final Button.OnClickListener mInitiateListener = new Button.OnClickListener() {

        public void onClick(View view) {
            final Context context = view.getContext();
            if (Utils.isDemoUser(context)) {
                final ComponentName componentName = Utils.getDeviceOwnerComponent(context);
                if (componentName != null) {
                    final Intent requestFactoryReset = new Intent()
                            .setPackage(componentName.getPackageName())
                            .setAction(Intent.ACTION_FACTORY_RESET);
                    context.startActivity(requestFactoryReset);
                }
            } else if (!runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                showFinalConfirmation();
            }
        }
    };

    /**
     * In its initial state, the activity presents a button for the user to
     * click in order to initiate a confirmation sequence.  This method is
     * called from various other points in the code to reset the activity to
     * this base state.
     *
     * <p>Reinflating views from resources is expensive and prevents us from
     * caching widget pointers, so we use a single-inflate pattern:  we lazy-
     * inflate each view, caching all of the widget pointers we'll need at the
     * time, then simply reuse the inflated views directly whenever we need
     * to change contents.
     */
    private void establishInitialState() {
        mInitiateButton = (Button) mContentView.findViewById(R.id.initiate_master_clear);
        mInitiateButton.setOnClickListener(mInitiateListener);
        mExternalStorageContainer = mContentView.findViewById(R.id.erase_external_container);
        mExternalStorage = (CheckBox) mContentView.findViewById(R.id.erase_external);
        mScrollView = (ScrollView) mContentView.findViewById(R.id.master_clear_scrollview);

        /*
         * If the external storage is emulated, it will be erased with a factory
         * reset at any rate. There is no need to have a separate option until
         * we have a factory reset that only erases some directories and not
         * others. Likewise, if it's non-removable storage, it could potentially have been
         * encrypted, and will also need to be wiped.
         */
        boolean isExtStorageEmulated = Environment.isExternalStorageEmulated();
        if (isExtStorageEmulated
                || (!Environment.isExternalStorageRemovable() && isExtStorageEncrypted())) {
            mExternalStorageContainer.setVisibility(View.GONE);

            final View externalOption = mContentView.findViewById(R.id.erase_external_option_text);
            externalOption.setVisibility(View.GONE);

            final View externalAlsoErased = mContentView.findViewById(R.id.also_erases_external);
            externalAlsoErased.setVisibility(View.VISIBLE);

            // If it's not emulated, it is on a separate partition but it means we're doing
            // a force wipe due to encryption.
            mExternalStorage.setChecked(!isExtStorageEmulated);
        } else {
            mExternalStorageContainer.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mExternalStorage.toggle();
                }
            });
        }

        if (showWipeEuicc()) {
            final View esimAlsoErased = mContentView.findViewById(R.id.also_erases_esim);
            esimAlsoErased.setVisibility(View.VISIBLE);

            final View noCancelMobilePlan = mContentView.findViewById(R.id.no_cancel_mobile_plan);
            noCancelMobilePlan.setVisibility(View.VISIBLE);
        }

        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        loadAccountList(um);
        StringBuffer contentDescription = new StringBuffer();
        View masterClearContainer = mContentView.findViewById(R.id.master_clear_container);
        getContentDescription(masterClearContainer, contentDescription);
        masterClearContainer.setContentDescription(contentDescription);

        // Set the status of initiateButton based on scrollview
        mScrollView.setOnScrollChangeListener(new OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX,
                int oldScrollY) {
                if (v instanceof ScrollView && hasReachedBottom((ScrollView) v)) {
                    mInitiateButton.setEnabled(true);
                }
            }
        });

        // Set the initial state of the initiateButton
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
    }

    /**
     * Whether to show strings indicating that the eUICC will be wiped.
     *
     * <p>We show the strings on any device which supports eUICC as long as the eUICC was ever
     * provisioned (that is, at least one profile was ever downloaded onto it).
     */
    @VisibleForTesting
    boolean showWipeEuicc() {
        Context context = getContext();
        if (!isEuiccEnabled(context)) {
            return false;
        }
        ContentResolver cr = context.getContentResolver();
        return Settings.Global.getInt(cr, Settings.Global.EUICC_PROVISIONED, 0) != 0;
    }

    @VisibleForTesting
    protected boolean isEuiccEnabled(Context context) {
        EuiccManager euiccManager = (EuiccManager) context.getSystemService(Context.EUICC_SERVICE);
        return euiccManager.isEnabled();
    }

    @VisibleForTesting
    boolean hasReachedBottom(final ScrollView scrollView) {
        if (scrollView.getChildCount() < 1) {
            return true;
        }

        final View view = scrollView.getChildAt(0);
        final int diff = view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY());

        return diff <= 0;
    }

    private void getContentDescription(View v, StringBuffer description) {
       if (v.getVisibility() != View.VISIBLE) {
           return;
       }
       if (v instanceof ViewGroup) {
           ViewGroup vGroup = (ViewGroup) v;
           for (int i = 0; i < vGroup.getChildCount(); i++) {
               View nextChild = vGroup.getChildAt(i);
               getContentDescription(nextChild, description);
           }
       } else if (v instanceof TextView) {
           TextView vText = (TextView) v;
           description.append(vText.getText());
           description.append(","); // Allow Talkback to pause between sections.
       }
    }

    private boolean isExtStorageEncrypted() {
        String state = SystemProperties.get("vold.decrypt");
        return !"".equals(state);
    }

    private void loadAccountList(final UserManager um) {
        View accountsLabel = mContentView.findViewById(R.id.accounts_label);
        LinearLayout contents = (LinearLayout)mContentView.findViewById(R.id.accounts);
        contents.removeAllViews();

        Context context = getActivity();
        final List<UserInfo> profiles = um.getProfiles(UserHandle.myUserId());
        final int profilesSize = profiles.size();

        AccountManager mgr = AccountManager.get(context);

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        int accountsCount = 0;
        for (int profileIndex = 0; profileIndex < profilesSize; profileIndex++) {
            final UserInfo userInfo = profiles.get(profileIndex);
            final int profileId = userInfo.id;
            final UserHandle userHandle = new UserHandle(profileId);
            Account[] accounts = mgr.getAccountsAsUser(profileId);
            final int N = accounts.length;
            if (N == 0) {
                continue;
            }
            accountsCount += N;

            AuthenticatorDescription[] descs = AccountManager.get(context)
                    .getAuthenticatorTypesAsUser(profileId);
            final int M = descs.length;

            if (profilesSize > 1) {
                View titleView = Utils.inflateCategoryHeader(inflater, contents);
                final TextView titleText = (TextView) titleView.findViewById(android.R.id.title);
                titleText.setText(userInfo.isManagedProfile() ? R.string.category_work
                        : R.string.category_personal);
                contents.addView(titleView);
            }

            for (int i = 0; i < N; i++) {
                Account account = accounts[i];
                AuthenticatorDescription desc = null;
                for (int j = 0; j < M; j++) {
                    if (account.type.equals(descs[j].type)) {
                        desc = descs[j];
                        break;
                    }
                }
                if (desc == null) {
                    Log.w(TAG, "No descriptor for account name=" + account.name
                            + " type=" + account.type);
                    continue;
                }
                Drawable icon = null;
                try {
                    if (desc.iconId != 0) {
                        Context authContext = context.createPackageContextAsUser(desc.packageName,
                                0, userHandle);
                        icon = context.getPackageManager().getUserBadgedIcon(
                                authContext.getDrawable(desc.iconId), userHandle);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Bad package name for account type " + desc.type);
                } catch (Resources.NotFoundException e) {
                    Log.w(TAG, "Invalid icon id for account type " + desc.type, e);
                }
                if (icon == null) {
                    icon = context.getPackageManager().getDefaultActivityIcon();
                }

                View child = inflater.inflate(R.layout.master_clear_account, contents, false);
                ((ImageView) child.findViewById(android.R.id.icon)).setImageDrawable(icon);
                ((TextView) child.findViewById(android.R.id.title)).setText(account.name);
                contents.addView(child);
            }
        }

        if (accountsCount > 0) {
            accountsLabel.setVisibility(View.VISIBLE);
            contents.setVisibility(View.VISIBLE);
        }
        // Checking for all other users and their profiles if any.
        View otherUsers = mContentView.findViewById(R.id.other_users_present);
        final boolean hasOtherUsers = (um.getUserCount() - profilesSize) > 0;
        otherUsers.setVisibility(hasOtherUsers ? View.VISIBLE : View.GONE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final Context context = getContext();
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(context,
                UserManager.DISALLOW_FACTORY_RESET, UserHandle.myUserId());
        final UserManager um = UserManager.get(context);
        final boolean disallow = !um.isAdminUser() || RestrictedLockUtils.hasBaseUserRestriction(
                context, UserManager.DISALLOW_FACTORY_RESET, UserHandle.myUserId());
        if (disallow && !Utils.isDemoUser(context)) {
            return inflater.inflate(R.layout.master_clear_disallowed_screen, null);
        } else if (admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(View.VISIBLE);
            return view;
        }

        mContentView = inflater.inflate(R.layout.master_clear, null);

        establishInitialState();
        return mContentView;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.MASTER_CLEAR;
    }
}
