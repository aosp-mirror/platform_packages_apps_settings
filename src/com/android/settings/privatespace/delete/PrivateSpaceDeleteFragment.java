/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace.delete;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.privatespace.PrivateSpaceMaintainer;
import com.android.settingslib.accounts.AuthenticatorHelper;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/** Fragment to delete private space that lists the accounts logged in to the private profile. */
public class PrivateSpaceDeleteFragment extends InstrumentedFragment {
    private static final String TAG = "PrivateSpaceDeleteFrag";
    private View mContentView;
    private static final int CREDENTIAL_CONFIRM_REQUEST = 1;
    @Nullable private UserHandle mPrivateUserHandle;

    @Override
    public void onCreate(@Nullable Bundle icicle) {
        if (android.os.Flags.allowPrivateProfile()) {
            super.onCreate(icicle);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (PrivateSpaceMaintainer.getInstance(getContext()).isPrivateSpaceLocked()) {
            getActivity().finish();
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETTINGS;
    }

    private View.OnClickListener startAuthenticationForDelete() {
        return v -> {
            final ChooseLockSettingsHelper.Builder builder =
                    new ChooseLockSettingsHelper.Builder(getActivity(), this);
            if (mPrivateUserHandle != null) {
                builder.setRequestCode(CREDENTIAL_CONFIRM_REQUEST)
                        .setUserId(mPrivateUserHandle.getIdentifier())
                        .show();
            } else {
                Log.e(TAG, "Private space user handle cannot be null");
                getActivity().finish();
            }
        };
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mPrivateUserHandle =
                PrivateSpaceMaintainer.getInstance(getContext()).getPrivateProfileHandle();
        if (mPrivateUserHandle == null) {
            Log.e(TAG, "Private space user handle cannot be null");
            getActivity().finish();
        }
        mContentView = inflater.inflate(R.layout.private_space_delete, container, false);
        final GlifLayout layout = mContentView.findViewById(R.id.private_space_delete_layout);
        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
        final Activity activity = getActivity();
        mixin.setPrimaryButton(
                new FooterButton.Builder(activity)
                        .setText(R.string.private_space_delete_button_label)
                        .setListener(startAuthenticationForDelete())
                        .setButtonType(FooterButton.ButtonType.OTHER)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        mixin.setSecondaryButton(
                new FooterButton.Builder(activity)
                        .setText(android.R.string.cancel)
                        .setListener(view -> activity.onBackPressed())
                        .setButtonType(FooterButton.ButtonType.CANCEL)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build());

        loadPrivateProfileAccountList();
        return mContentView;
    }

    private void loadPrivateProfileAccountList() {
        View accountsLabel = mContentView.findViewById(R.id.accounts_label);
        LinearLayout contents = (LinearLayout) mContentView.findViewById(R.id.accounts);
        contents.removeAllViews();

        Context context = getActivity();

        AccountManager accountManager = AccountManager.get(context);

        LayoutInflater inflater = context.getSystemService(LayoutInflater.class);

        final AuthenticatorHelper helper =
                new AuthenticatorHelper(context, mPrivateUserHandle, null);
        final String[] accountTypes = helper.getEnabledAccountTypes();

        for (String type : accountTypes) {
            final String accountType = type;
            final Account[] accounts =
                    accountManager.getAccountsByTypeAsUser(accountType, mPrivateUserHandle);
            Drawable icon = helper.getDrawableForType(getContext(), accountType);
            if (icon == null) {
                icon = context.getPackageManager().getDefaultActivityIcon();
            }
            for (Account account : accounts) {
                View child = inflater.inflate(R.layout.main_clear_account, contents, false);
                child.<ImageView>findViewById(android.R.id.icon).setImageDrawable(icon);
                child.<TextView>findViewById(android.R.id.title).setText(account.name);
                contents.addView(child);
            }
        }

        if (contents.getChildCount() > 0) {
            accountsLabel.setVisibility(View.VISIBLE);
            contents.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREDENTIAL_CONFIRM_REQUEST && resultCode == Activity.RESULT_OK) {
            NavHostFragment.findNavController(PrivateSpaceDeleteFragment.this)
                    .navigate(R.id.action_authenticate_delete);
        }
    }
}
