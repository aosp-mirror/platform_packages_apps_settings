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

import android.app.ActionBar;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settingslib.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.ShowAdminSupportDetailsDialog;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

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
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
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
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId());
        final UserManager um = UserManager.get(getActivity());
        mBeamDisallowedByBase = RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId());
        if (!mBeamDisallowedByBase && admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(View.VISIBLE);
            mBeamDisallowedByOnlyAdmin = true;
            return view;
        }
        mView = inflater.inflate(R.layout.android_beam, container, false);
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
    protected int getMetricsCategory() {
        return MetricsEvent.NFC_BEAM;
    }
}
