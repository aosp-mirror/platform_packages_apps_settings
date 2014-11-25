/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.android.setupwizard.navigationbar.SetupWizardNavBar;


import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class SetupChooseLockGeneric extends ChooseLockGeneric
        implements SetupWizardNavBar.NavigationBarListener {

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SetupChooseLockGenericFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends PreferenceFragment> getFragmentClass() {
        return SetupChooseLockGenericFragment.class;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent(), resid);
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    public void onNavigationBarCreated(SetupWizardNavBar bar) {
        SetupWizardUtils.setImmersiveMode(this, bar);
        bar.getNextButton().setEnabled(false);
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
    }

    public static class SetupChooseLockGenericFragment extends ChooseLockGenericFragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.setup_preference, container, false);
            ListView list = (ListView) view.findViewById(android.R.id.list);
            View title = view.findViewById(R.id.title);
            if (title == null) {
                final View header = inflater.inflate(R.layout.setup_wizard_header, list, false);
                list.addHeaderView(header, null, false);
            }
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setHeaderText(getActivity(), getActivity().getTitle());
        }
    }
}
