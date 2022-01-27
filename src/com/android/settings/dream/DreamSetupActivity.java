/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.dream;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.dream.DreamBackend;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;
import com.google.android.setupdesign.util.ThemeResolver;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The setup activity for dreams which is displayed during setup wizard.
 */
public class DreamSetupActivity extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, DreamSetupFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return DreamSetupFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        setTheme(ThemeResolver.getDefault().resolve(getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(savedInstance);
    }

    @Override
    protected boolean isToolbarEnabled() {
        return false;
    }

    /**
     * Fragment used to control the active dream.
     */
    public static class DreamSetupFragment extends SettingsPreferenceFragment {
        private DreamBackend mBackend;
        private DreamBackend.DreamInfo mActiveDream;
        private FooterButton mFooterButton;

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DREAM;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.dream_setup_layout, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mBackend = DreamBackend.getInstance(getContext());
            final List<DreamBackend.DreamInfo> dreamInfos = mBackend.getDreamInfos();
            mActiveDream = dreamInfos.stream().filter(d -> d.isActive).findFirst().orElse(null);
            DreamAdapter dreamAdapter = new DreamAdapter(dreamInfos.stream()
                    .map(DreamItem::new)
                    .collect(Collectors.toList()));

            final RecyclerView recyclerView = view.findViewById(R.id.dream_setup_list);
            recyclerView.setLayoutManager(new AutoFitGridLayoutManager(getContext()));
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(dreamAdapter);

            final GlifLayout layout = view.findViewById(R.id.setup_wizard_layout);
            final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
            mFooterButton = new FooterButton.Builder(getContext())
                    .setListener(this::onPrimaryButtonClicked)
                    .setButtonType(FooterButton.ButtonType.NEXT)
                    .setTheme(R.style.SudGlifButton_Primary)
                    .build();
            updateFooterButtonText();
            mixin.setPrimaryButton(mFooterButton);
        }

        private void updateFooterButtonText() {
            final int res = canCustomizeDream() ? R.string.wizard_next : R.string.wizard_finish;
            mFooterButton.setText(getContext().getString(res));
        }

        private boolean canCustomizeDream() {
            return mActiveDream != null && mActiveDream.settingsComponentName != null;
        }

        private void onPrimaryButtonClicked(View view) {
            if (canCustomizeDream()) {
                final Intent intent = new Intent().setComponent(mActiveDream.settingsComponentName);
                WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
                startActivity(intent);
            }

            // Use RESULT_CANCELED here so that the user may go back and change this if they wish.
            setResult(RESULT_CANCELED);
            finish();
        }

        private class DreamItem implements IDreamItem {
            private final DreamBackend.DreamInfo mDreamInfo;

            private DreamItem(DreamBackend.DreamInfo dreamInfo) {
                mDreamInfo = dreamInfo;
            }

            @Override
            public CharSequence getTitle() {
                return mDreamInfo.caption;
            }

            @Override
            public Drawable getIcon() {
                return mDreamInfo.icon;
            }

            @Override
            public void onItemClicked() {
                mActiveDream = mDreamInfo;
                mBackend.setActiveDream(mDreamInfo.componentName);
                updateFooterButtonText();
            }

            @Override
            public Drawable getPreviewImage() {
                return mDreamInfo.previewImage;
            }

            @Override
            public boolean isActive() {
                if (mActiveDream == null) {
                    return false;
                }
                return mDreamInfo.componentName.equals(mActiveDream.componentName);
            }
        }
    }
}
