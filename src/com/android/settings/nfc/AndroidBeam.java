/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.nfc;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.enterprise.ActionDisabledByAdminDialogHelper;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

public class AndroidBeam extends InstrumentedFragment
        implements SwitchBar.OnSwitchChangeListener {
    private View mView;
    private NfcAdapter mNfcAdapter;
    private SwitchBar mSwitchBar;
    private CharSequence mOldActivityTitle;
    private boolean mBeamDisallowedByBase;
    private boolean mBeamDisallowedByOnlyAdmin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        final PackageManager pm = context.getPackageManager();
        if (mNfcAdapter == null || !pm.hasSystemFeature(PackageManager.FEATURE_NFC_BEAM))
            getActivity().finish();
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_beam,
                getClass().getName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId());
        final UserManager um = UserManager.get(getActivity());
        mBeamDisallowedByBase = RestrictedLockUtilsInternal.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId());
        if (!mBeamDisallowedByBase && admin != null) {
            new ActionDisabledByAdminDialogHelper(getActivity())
                    .prepareDialogBuilder(UserManager.DISALLOW_OUTGOING_BEAM, admin).show();
            mBeamDisallowedByOnlyAdmin = true;
            return new View(getContext());
        }
        mView = inflater.inflate(R.layout.preference_footer, container, false);

        ImageView iconInfo = mView.findViewById(android.R.id.icon);
        iconInfo.setImageResource(R.drawable.ic_info_outline_24dp);
        TextView textInfo = mView.findViewById(android.R.id.title);
        textInfo.setText(R.string.android_beam_explained);

        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();

        mOldActivityTitle = activity.getActionBar().getTitle();

        mSwitchBar = activity.getSwitchBar();
        if (mBeamDisallowedByOnlyAdmin) {
            mSwitchBar.hide();
        } else {
            mSwitchBar.setChecked(!mBeamDisallowedByBase && mNfcAdapter.isNdefPushEnabled());
            mSwitchBar.addOnSwitchChangeListener(this);
            mSwitchBar.setEnabled(!mBeamDisallowedByBase);
            mSwitchBar.show();
        }

        activity.setTitle(R.string.android_beam_settings_title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mOldActivityTitle != null) {
            getActivity().getActionBar().setTitle(mOldActivityTitle);
        }
        if (!mBeamDisallowedByOnlyAdmin) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mSwitchBar.hide();
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean desiredState) {
        boolean success = false;
        mSwitchBar.setEnabled(false);
        if (desiredState) {
            success = mNfcAdapter.enableNdefPush();
        } else {
            success = mNfcAdapter.disableNdefPush();
        }
        if (success) {
            mSwitchBar.setChecked(desiredState);
        }
        mSwitchBar.setEnabled(true);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NFC_BEAM;
    }
}
