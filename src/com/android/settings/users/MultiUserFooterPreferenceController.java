/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.users;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixinCompat;

public class MultiUserFooterPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    final UserCapabilities mUserCaps;

    private FooterPreferenceMixinCompat mFooterMixin;

    public MultiUserFooterPreferenceController(Context context) {
        super(context, "dummy_key");
        mUserCaps = UserCapabilities.create(context);
    }

    public MultiUserFooterPreferenceController setFooterMixin(
            FooterPreferenceMixinCompat footerMixin) {
        mFooterMixin = footerMixin;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        return (mUserCaps.mEnabled && !mUserCaps.mUserSwitcherEnabled)
                ? AVAILABLE_UNSEARCHABLE
                : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        mUserCaps.updateAddUserCapabilities(mContext);
        final FooterPreference pref = mFooterMixin.createFooterPreference();
        pref.setTitle(R.string.user_settings_footer_text);
        pref.setVisible(isAvailable());
    }
}
