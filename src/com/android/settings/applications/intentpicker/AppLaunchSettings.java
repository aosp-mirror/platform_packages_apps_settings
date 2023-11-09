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
 * limitations under the License.
 */

package com.android.settings.applications.intentpicker;

import static android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_NONE;
import static android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_SELECTED;
import static android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_VERIFIED;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.icu.text.MessageFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.ClearDefaultsPreference;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.MainSwitchPreference;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The page of the Open by default */
public class AppLaunchSettings extends AppInfoBase implements
        Preference.OnPreferenceChangeListener, OnCheckedChangeListener {
    private static final String TAG = "AppLaunchSettings";
    // Preference keys
    private static final String MAIN_SWITCH_PREF_KEY = "open_by_default_supported_links";
    private static final String VERIFIED_LINKS_PREF_KEY = "open_by_default_verified_links";
    private static final String ADD_LINK_PREF_KEY = "open_by_default_add_link";
    private static final String CLEAR_DEFAULTS_PREF_KEY = "app_launch_clear_defaults";
    private static final String FOOTER_PREF_KEY = "open_by_default_footer";

    private static final String MAIN_PREF_CATEGORY_KEY = "open_by_default_main_category";
    private static final String SELECTED_LINKS_CATEGORY_KEY =
            "open_by_default_selected_links_category";
    private static final String OTHER_DETAILS_PREF_CATEGORY_KEY = "app_launch_other_defaults";

    private static final String LEARN_MORE_URI =
            "https://developer.android.com/training/app-links/verify-site-associations";

    // Dialogs id
    private static final int DLG_VERIFIED_LINKS = DLG_BASE + 1;

    // Arguments key
    public static final String APP_PACKAGE_KEY = "app_package";

    private ClearDefaultsPreference mClearDefaultsPreference;
    private MainSwitchPreference mMainSwitchPreference;
    private Preference mAddLinkPreference;
    private PreferenceCategory mMainPreferenceCategory;
    private PreferenceCategory mSelectedLinksPreferenceCategory;
    private PreferenceCategory mOtherDefaultsPreferenceCategory;

    private boolean mActivityCreated;

    @VisibleForTesting
    Context mContext;
    @VisibleForTesting
    DomainVerificationManager mDomainVerificationManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mActivityCreated = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mAppEntry == null) {
            Log.w(TAG, "onCreate: mAppEntry is null, please check the reason!!!");
            getActivity().finish();
            return;
        }
        addPreferencesFromResource(R.xml.installed_app_launch_settings);
        mDomainVerificationManager = mContext.getSystemService(DomainVerificationManager.class);
        initUIComponents();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        createHeaderPreference();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPLICATIONS_APP_LAUNCH;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        if (id == DLG_VERIFIED_LINKS) {
            return createVerifiedLinksDialog();
        }
        return null;
    }

    @Override
    protected boolean refreshUi() {
        mClearDefaultsPreference.setPackageName(mPackageName);
        mClearDefaultsPreference.setAppEntry(mAppEntry);
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isChecked = (boolean) newValue;
        IntentPickerUtils.logd(
                "onPreferenceChange: " + preference.getTitle() + " isChecked: " + isChecked);
        if ((preference instanceof LeftSideCheckBoxPreference) && !isChecked) {
            final Set<String> domainSet = new ArraySet<>();
            domainSet.add(preference.getTitle().toString());
            removePreference(preference.getKey());
            final DomainVerificationUserState userState =
                    IntentPickerUtils.getDomainVerificationUserState(mDomainVerificationManager,
                            mPackageName);
            if (userState == null) {
                return false;
            }
            setDomainVerificationUserSelection(userState.getIdentifier(), domainSet, /* enabled= */
                    false);
            mAddLinkPreference.setEnabled(isAddLinksNotEmpty());
        }
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        IntentPickerUtils.logd("onSwitchChanged: isChecked=" + isChecked);
        if (mMainSwitchPreference != null) { //mMainSwitchPreference synced with Switch
            mMainSwitchPreference.setChecked(isChecked);
        }
        if (mMainPreferenceCategory != null) {
            mMainPreferenceCategory.setVisible(isChecked);
        }
        if (mDomainVerificationManager != null) {
            try {
                mDomainVerificationManager.setDomainVerificationLinkHandlingAllowed(mPackageName,
                        isChecked);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "onSwitchChanged: " + e.getMessage());
            }
        }
    }

    private void createHeaderPreference() {
        if (mActivityCreated) {
            Log.w(TAG, "onParentActivityCreated: ignoring duplicate call.");
            return;
        }
        mActivityCreated = true;
        if (mPackageInfo == null) {
            Log.w(TAG, "onParentActivityCreated: PakcageInfo is null.");
            return;
        }
        final Activity activity = getActivity();
        final String summary = activity.getString(R.string.app_launch_top_intro_message);
        final Preference pref = EntityHeaderController
                .newInstance(activity, this, null /* header */)
                .setIcon(Utils.getBadgedIcon(mContext, mPackageInfo.applicationInfo))
                .setLabel(mPackageInfo.applicationInfo.loadLabel(mPm))
                .setSummary(summary)  // add intro text
                .setIsInstantApp(AppUtils.isInstant(mPackageInfo.applicationInfo))
                .setPackageName(mPackageName)
                .setUid(mPackageInfo.applicationInfo.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE)
                .done(getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    private void initUIComponents() {
        initMainSwitchAndCategories();
        if (canUpdateMainSwitchAndCategories()) {
            initVerifiedLinksPreference();
            initAddLinkPreference();
            addSelectedLinksPreference();
            initFooter();
        }
    }

    private void initMainSwitchAndCategories() {
        mMainSwitchPreference = (MainSwitchPreference) findPreference(MAIN_SWITCH_PREF_KEY);
        mMainPreferenceCategory = findPreference(MAIN_PREF_CATEGORY_KEY);
        mSelectedLinksPreferenceCategory = findPreference(SELECTED_LINKS_CATEGORY_KEY);
        // Initialize the "Other Default Category" section
        initOtherDefaultsSection();
    }

    private boolean canUpdateMainSwitchAndCategories() {
        final DomainVerificationUserState userState =
                IntentPickerUtils.getDomainVerificationUserState(mDomainVerificationManager,
                        mPackageName);
        if (userState == null) {
            disabledPreference();
            return false;
        }

        IntentPickerUtils.logd("isLinkHandlingAllowed() : " + userState.isLinkHandlingAllowed());
        mMainSwitchPreference.updateStatus(userState.isLinkHandlingAllowed());
        mMainSwitchPreference.addOnSwitchChangeListener(this);
        mMainPreferenceCategory.setVisible(userState.isLinkHandlingAllowed());
        return true;
    }

    /** Initialize verified links preference */
    private void initVerifiedLinksPreference() {
        final Preference verifiedLinksPreference = mMainPreferenceCategory.findPreference(
                VERIFIED_LINKS_PREF_KEY);
        verifiedLinksPreference.setOnPreferenceClickListener(preference -> {
            showVerifiedLinksDialog();
            return true;
        });
        final int verifiedLinksNo = getLinksNumber(DOMAIN_STATE_VERIFIED);
        verifiedLinksPreference.setTitle(getVerifiedLinksTitle(verifiedLinksNo));
        verifiedLinksPreference.setEnabled(verifiedLinksNo > 0);
    }

    private void showVerifiedLinksDialog() {
        final int linksNo = getLinksNumber(DOMAIN_STATE_VERIFIED);
        if (linksNo == 0) {
            return;
        }
        showDialogInner(DLG_VERIFIED_LINKS, /* moveErrorCode= */ 0);
    }

    private AlertDialog createVerifiedLinksDialog() {
        final int linksNo = getLinksNumber(DOMAIN_STATE_VERIFIED);

        final View titleView = LayoutInflater.from(mContext).inflate(
                R.layout.app_launch_verified_links_title, /* root= */ null);
        ((TextView) titleView.findViewById(R.id.dialog_title)).setText(
                getVerifiedLinksTitle(linksNo));
        ((TextView) titleView.findViewById(R.id.dialog_message)).setText(
                getVerifiedLinksMessage(linksNo));

        final List<String> verifiedLinksList = IntentPickerUtils.getLinksList(
                mDomainVerificationManager, mPackageName, DOMAIN_STATE_VERIFIED);
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setCustomTitle(titleView)
                .setCancelable(true)
                .setItems(verifiedLinksList.toArray(new String[0]), /* listener= */ null)
                .setPositiveButton(R.string.app_launch_dialog_ok, /* listener= */ null)
                .create();
        if (dialog.getListView() != null) {
            dialog.getListView().setTextDirection(View.TEXT_DIRECTION_LOCALE);
        } else {
            Log.w(TAG, "createVerifiedLinksDialog: dialog.getListView() is null, please check it.");
        }
        return dialog;
    }

    @VisibleForTesting
    String getVerifiedLinksTitle(int linksNo) {
        MessageFormat msgFormat = new MessageFormat(
                getResources().getString(R.string.app_launch_verified_links_title),
                Locale.getDefault());
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("count", linksNo);
        return msgFormat.format(arguments);
    }

    private String getVerifiedLinksMessage(int linksNo) {
        MessageFormat msgFormat = new MessageFormat(
                getResources().getString(R.string.app_launch_verified_links_message),
                Locale.getDefault());
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("count", linksNo);
        return msgFormat.format(arguments);
    }

    /** Add selected links items */
    void addSelectedLinksPreference() {
        if (getLinksNumber(DOMAIN_STATE_SELECTED) == 0) {
            return;
        }
        mSelectedLinksPreferenceCategory.removeAll();
        final List<String> selectedLinks = IntentPickerUtils.getLinksList(
                mDomainVerificationManager, mPackageName, DOMAIN_STATE_SELECTED);
        for (String host : selectedLinks) {
            generateCheckBoxPreference(mSelectedLinksPreferenceCategory, host);
        }
        mAddLinkPreference.setEnabled(isAddLinksNotEmpty());
    }

    /** Initialize add link preference */
    private void initAddLinkPreference() {
        mAddLinkPreference = findPreference(ADD_LINK_PREF_KEY);
        mAddLinkPreference.setVisible(isAddLinksShown());
        mAddLinkPreference.setEnabled(isAddLinksNotEmpty());
        mAddLinkPreference.setOnPreferenceClickListener(preference -> {
            final int stateNoneLinksNo = getLinksNumber(DOMAIN_STATE_NONE);
            IntentPickerUtils.logd("The number of the state none links: " + stateNoneLinksNo);
            if (stateNoneLinksNo > 0) {
                showProgressDialogFragment();
            }
            return true;
        });
    }

    private boolean isAddLinksNotEmpty() {
        return getLinksNumber(DOMAIN_STATE_NONE) > 0;
    }

    private boolean isAddLinksShown() {
        return (isAddLinksNotEmpty() || getLinksNumber(DOMAIN_STATE_SELECTED) > 0);
    }

    private void showProgressDialogFragment() {
        final Bundle args = new Bundle();
        args.putString(APP_PACKAGE_KEY, mPackageName);
        final ProgressDialogFragment dialogFragment = new ProgressDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.showDialog(getActivity().getSupportFragmentManager());
    }

    private void disabledPreference() {
        mMainSwitchPreference.updateStatus(false);
        mMainSwitchPreference.setSelectable(false);
        mMainSwitchPreference.setEnabled(false);
        mMainPreferenceCategory.setVisible(false);
    }

    /** Init OTHER DEFAULTS category */
    private void initOtherDefaultsSection() {
        mOtherDefaultsPreferenceCategory = findPreference(OTHER_DETAILS_PREF_CATEGORY_KEY);
        mOtherDefaultsPreferenceCategory.setVisible(isClearDefaultsEnabled());
        mClearDefaultsPreference = (ClearDefaultsPreference) findPreference(
                CLEAR_DEFAULTS_PREF_KEY);
    }

    private void initFooter() {
        final CharSequence footerText = mContext.getText(R.string.app_launch_footer);
        final FooterPreference footerPreference = (FooterPreference) findPreference(
                FOOTER_PREF_KEY);
        footerPreference.setTitle(footerText);
        // learn more
        footerPreference.setLearnMoreAction(view -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(LEARN_MORE_URI));
            mContext.startActivity(intent);
        });
        final String learnMoreText = mContext.getString(
                R.string.footer_learn_more_content_description, getLabelName());
        footerPreference.setLearnMoreText(learnMoreText);
    }

    private String getLabelName() {
        return mContext.getString(R.string.launch_by_default);
    }

    private boolean isClearDefaultsEnabled() {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
        final boolean hasBindAppWidgetPermission =
                appWidgetManager.hasBindAppWidgetPermission(mAppEntry.info.packageName);

        final boolean isAutoLaunchEnabled = AppUtils.hasPreferredActivities(mPm, mPackageName)
                || AppUtils.isDefaultBrowser(mContext, mPackageName)
                || AppUtils.hasUsbDefaults(mUsbManager, mPackageName);

        IntentPickerUtils.logd("isClearDefaultsEnabled hasBindAppWidgetPermission : "
                + hasBindAppWidgetPermission);
        IntentPickerUtils.logd(
                "isClearDefaultsEnabled isAutoLaunchEnabled : " + isAutoLaunchEnabled);
        return (isAutoLaunchEnabled || hasBindAppWidgetPermission);
    }

    private void setDomainVerificationUserSelection(UUID identifier, Set<String> domainSet,
            boolean isEnabled) {
        try {
            mDomainVerificationManager.setDomainVerificationUserSelection(identifier, domainSet,
                    isEnabled);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "addSelectedItems : " + e.getMessage());
        }
    }

    private void generateCheckBoxPreference(PreferenceCategory parent, String title) {
        final LeftSideCheckBoxPreference checkBoxPreference = new LeftSideCheckBoxPreference(
                parent.getContext(), /* isChecked= */ true);
        checkBoxPreference.setTitle(title);
        checkBoxPreference.setOnPreferenceChangeListener(this);
        checkBoxPreference.setKey(UUID.randomUUID().toString());
        parent.addPreference(checkBoxPreference);
    }

    /** get the number of the specify links */
    private int getLinksNumber(@DomainVerificationUserState.DomainState int state) {
        final List<String> linkList = IntentPickerUtils.getLinksList(
                mDomainVerificationManager, mPackageName, state);
        if (linkList == null) {
            return 0;
        }
        return linkList.size();
    }
}
