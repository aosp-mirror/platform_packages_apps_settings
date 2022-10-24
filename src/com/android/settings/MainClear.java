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

import static android.app.admin.DevicePolicyResources.Strings.Settings.PERSONAL_CATEGORY_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_CATEGORY_HEADER;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.ActionBar;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.sysprop.VoldProperties;
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

import androidx.annotation.VisibleForTesting;

import com.android.settings.core.InstrumentedFragment;
import com.android.settings.enterprise.ActionDisabledByAdminDialogHelper;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.ConfirmLockPattern;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.template.FooterButton.ButtonType;
import com.google.android.setupdesign.GlifLayout;

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
public class MainClear extends InstrumentedFragment implements OnGlobalLayoutListener {
    private static final String TAG = "MainClear";

    @VisibleForTesting
    static final int KEYGUARD_REQUEST = 55;
    @VisibleForTesting
    static final int CREDENTIAL_CONFIRM_REQUEST = 56;

    private static final String KEY_SHOW_ESIM_RESET_CHECKBOX =
            "masterclear.allow_retain_esim_profiles_after_fdr";

    static final String ERASE_EXTERNAL_EXTRA = "erase_sd";
    static final String ERASE_ESIMS_EXTRA = "erase_esim";

    private View mContentView;
    @VisibleForTesting
    FooterButton mInitiateButton;
    private View mExternalStorageContainer;
    @VisibleForTesting
    CheckBox mExternalStorage;
    @VisibleForTesting
    View mEsimStorageContainer;
    @VisibleForTesting
    CheckBox mEsimStorage;
    @VisibleForTesting
    ScrollView mScrollView;

    @Override
    public void onGlobalLayout() {
        mInitiateButton.setEnabled(hasReachedBottom(mScrollView));
    }

    private void setUpActionBarAndTitle() {
        final Activity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "No activity attached, skipping setUpActionBarAndTitle");
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (actionBar == null) {
            Log.e(TAG, "No actionbar, skipping setUpActionBarAndTitle");
            return;
        }
        actionBar.hide();
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    /**
     * Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     *
     * @param request the request code to be returned once confirmation finishes
     * @return true if confirmation launched
     */
    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        final ChooseLockSettingsHelper.Builder builder =
                new ChooseLockSettingsHelper.Builder(getActivity(), this);
        return builder.setRequestCode(request)
                .setTitle(res.getText(R.string.main_clear_short_title))
                .show();
    }

    @VisibleForTesting
    boolean isValidRequestCode(int requestCode) {
        return !((requestCode != KEYGUARD_REQUEST) && (requestCode != CREDENTIAL_CONFIRM_REQUEST));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        onActivityResultInternal(requestCode, resultCode, data);
    }

    /*
     * Internal method that allows easy testing without dealing with super references.
     */
    @VisibleForTesting
    void onActivityResultInternal(int requestCode, int resultCode, Intent data) {
        if (!isValidRequestCode(requestCode)) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            establishInitialState();
            return;
        }

        Intent intent = null;
        // If returning from a Keyguard request, try to show an account confirmation request if
        // applciable.
        if (CREDENTIAL_CONFIRM_REQUEST != requestCode
                && (intent = getAccountConfirmationIntent()) != null) {
            showAccountCredentialConfirmation(intent);
        } else {
            showFinalConfirmation();
        }
    }

    @VisibleForTesting
    void showFinalConfirmation() {
        final Bundle args = new Bundle();
        args.putBoolean(ERASE_EXTERNAL_EXTRA, mExternalStorage.isChecked());
        args.putBoolean(ERASE_ESIMS_EXTRA, mEsimStorage.isChecked());
        final Intent intent = new Intent();
        intent.setClass(getContext(),
                com.android.settings.Settings.FactoryResetConfirmActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, MainClearConfirm.class.getName());
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID,
                R.string.main_clear_confirm_title);
        intent.putExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, getMetricsCategory());
        getContext().startActivity(intent);
    }

    @VisibleForTesting
    void showAccountCredentialConfirmation(Intent intent) {
        startActivityForResult(intent, CREDENTIAL_CONFIRM_REQUEST);
    }

    @VisibleForTesting
    Intent getAccountConfirmationIntent() {
        final Context context = getActivity();
        final String accountType = context.getString(R.string.account_type);
        final String packageName = context.getString(R.string.account_confirmation_package);
        final String className = context.getString(R.string.account_confirmation_class);
        if (TextUtils.isEmpty(accountType)
                || TextUtils.isEmpty(packageName)
                || TextUtils.isEmpty(className)) {
            Log.i(TAG, "Resources not set for account confirmation.");
            return null;
        }
        final AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(accountType);
        if (accounts != null && accounts.length > 0) {
            final Intent requestAccountConfirmation = new Intent()
                    .setPackage(packageName)
                    .setComponent(new ComponentName(packageName, className));
            // Check to make sure that the intent is supported.
            final PackageManager pm = context.getPackageManager();
            final ResolveInfo resolution = pm.resolveActivity(requestAccountConfirmation, 0);
            if (resolution != null
                    && resolution.activityInfo != null
                    && packageName.equals(resolution.activityInfo.packageName)) {
                // Note that we need to check the packagename to make sure that an Activity resolver
                // wasn't returned.
                return requestAccountConfirmation;
            } else {
                Log.i(TAG, "Unable to resolve Activity: " + packageName + "/" + className);
            }
        } else {
            Log.d(TAG, "No " + accountType + " accounts installed!");
        }
        return null;
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
                return;
            }

            if (runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                return;
            }

            Intent intent = getAccountConfirmationIntent();
            if (intent != null) {
                showAccountCredentialConfirmation(intent);
            } else {
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
    @VisibleForTesting
    void establishInitialState() {
        setUpActionBarAndTitle();
        setUpInitiateButton();

        mExternalStorageContainer = mContentView.findViewById(R.id.erase_external_container);
        mExternalStorage = mContentView.findViewById(R.id.erase_external);
        mEsimStorageContainer = mContentView.findViewById(R.id.erase_esim_container);
        mEsimStorage = mContentView.findViewById(R.id.erase_esim);
        if (mScrollView != null) {
            mScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
        mScrollView = mContentView.findViewById(R.id.main_clear_scrollview);

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
            if (showWipeEuiccCheckbox()) {
                mEsimStorageContainer.setVisibility(View.VISIBLE);
                mEsimStorageContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mEsimStorage.toggle();
                    }
                });
            } else {
                final View esimAlsoErased = mContentView.findViewById(R.id.also_erases_esim);
                esimAlsoErased.setVisibility(View.VISIBLE);

                final View noCancelMobilePlan = mContentView.findViewById(
                        R.id.no_cancel_mobile_plan);
                noCancelMobilePlan.setVisibility(View.VISIBLE);
                mEsimStorage.setChecked(true /* checked */);
            }
        } else {
            mEsimStorage.setChecked(false /* checked */);
        }

        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        loadAccountList(um);
        final StringBuffer contentDescription = new StringBuffer();
        final View mainClearContainer = mContentView.findViewById(R.id.main_clear_container);
        getContentDescription(mainClearContainer, contentDescription);
        mainClearContainer.setContentDescription(contentDescription);

        // Set the status of initiateButton based on scrollview
        mScrollView.setOnScrollChangeListener(new OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX,
                    int oldScrollY) {
                if (v instanceof ScrollView && hasReachedBottom((ScrollView) v)) {
                    mInitiateButton.setEnabled(true);
                    mScrollView.setOnScrollChangeListener(null);
                }
            }
        });

        // Set the initial state of the initiateButton
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * Whether to show any UI which is SIM related.
     */
    @VisibleForTesting
    boolean showAnySubscriptionInfo(Context context) {
        return (context != null) && SubscriptionUtil.isSimHardwareVisible(context);
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
        if (!showAnySubscriptionInfo(context) || !isEuiccEnabled(context)) {
            return false;
        }
        ContentResolver cr = context.getContentResolver();
        return Settings.Global.getInt(cr, Settings.Global.EUICC_PROVISIONED, 0) != 0
                || DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context);
    }

    @VisibleForTesting
    boolean showWipeEuiccCheckbox() {
        return SystemProperties
                .getBoolean(KEY_SHOW_ESIM_RESET_CHECKBOX, false /* def */);
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

    private void setUpInitiateButton() {
        if (mInitiateButton != null) {
            return;
        }

        final GlifLayout layout = mContentView.findViewById(R.id.setup_wizard_layout);
        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getActivity())
                        .setText(R.string.main_clear_button_text)
                        .setListener(mInitiateListener)
                        .setButtonType(ButtonType.OTHER)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build()
        );
        mInitiateButton = mixin.getPrimaryButton();
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
        String state = VoldProperties.decrypt().orElse("");
        return !"".equals(state);
    }

    private void loadAccountList(final UserManager um) {
        View accountsLabel = mContentView.findViewById(R.id.accounts_label);
        LinearLayout contents = (LinearLayout) mContentView.findViewById(R.id.accounts);
        contents.removeAllViews();

        Context context = getActivity();
        final List<UserInfo> profiles = um.getProfiles(UserHandle.myUserId());
        final int profilesSize = profiles.size();

        AccountManager mgr = AccountManager.get(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        int accountsCount = 0;
        for (int profileIndex = 0; profileIndex < profilesSize; profileIndex++) {
            final UserInfo userInfo = profiles.get(profileIndex);
            final int profileId = userInfo.id;
            final UserHandle userHandle = new UserHandle(profileId);
            Account[] accounts = mgr.getAccountsAsUser(profileId);
            final int accountLength = accounts.length;
            if (accountLength == 0) {
                continue;
            }
            accountsCount += accountLength;

            AuthenticatorDescription[] descs = AccountManager.get(context)
                    .getAuthenticatorTypesAsUser(profileId);
            final int descLength = descs.length;

            if (profilesSize > 1) {
                View titleView = Utils.inflateCategoryHeader(inflater, contents);
                titleView.setPadding(0 /* left */, titleView.getPaddingTop(),
                        0 /* right */, titleView.getPaddingBottom());
                final TextView titleText = (TextView) titleView.findViewById(android.R.id.title);

                DevicePolicyManager devicePolicyManager =
                        context.getSystemService(DevicePolicyManager.class);

                if (userInfo.isManagedProfile()) {
                    titleText.setText(devicePolicyManager.getResources().getString(
                            WORK_CATEGORY_HEADER, () -> getString(R.string.category_work)));
                } else {
                    titleText.setText(devicePolicyManager.getResources().getString(
                            PERSONAL_CATEGORY_HEADER, () -> getString(R.string.category_personal)));
                }
                contents.addView(titleView);
            }

            for (int i = 0; i < accountLength; i++) {
                Account account = accounts[i];
                AuthenticatorDescription desc = null;
                for (int j = 0; j < descLength; j++) {
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

                View child = inflater.inflate(R.layout.main_clear_account, contents, false);
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
        final EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(context,
                UserManager.DISALLOW_FACTORY_RESET, UserHandle.myUserId());
        final UserManager um = UserManager.get(context);
        final boolean disallow = !um.isAdminUser() || RestrictedLockUtilsInternal
                .hasBaseUserRestriction(context, UserManager.DISALLOW_FACTORY_RESET,
                        UserHandle.myUserId());
        if (disallow && !Utils.isDemoUser(context)) {
            return inflater.inflate(R.layout.main_clear_disallowed_screen, null);
        } else if (admin != null) {
            new ActionDisabledByAdminDialogHelper(getActivity())
                    .prepareDialogBuilder(UserManager.DISALLOW_FACTORY_RESET, admin)
                    .setOnDismissListener(__ -> getActivity().finish())
                    .show();
            return new View(getContext());
        }

        mContentView = inflater.inflate(R.layout.main_clear, null);

        establishInitialState();
        return mContentView;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MASTER_CLEAR;
    }
}
