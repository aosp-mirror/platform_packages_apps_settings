/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_GESTURE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.GESTURE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TWOFINGER_DOUBLETAP;

import android.annotation.SuppressLint;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.annotation.AnimRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Preconditions;
import androidx.core.widget.TextViewCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType;
import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for creating the dialog that shows tutorials on how to use the selected
 * accessibility shortcut types
 */
public final class AccessibilityShortcutsTutorial {
    private static final String TAG = "AccessibilityGestureNavigationTutorial";

    /** IntDef enum for dialog type. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_BUTTON,
            DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_GESTURE,
            DialogType.GESTURE_NAVIGATION_SETTINGS,
    })

    private @interface DialogType {
        int LAUNCH_SERVICE_BY_ACCESSIBILITY_BUTTON = 0;
        int LAUNCH_SERVICE_BY_ACCESSIBILITY_GESTURE = 1;
        int GESTURE_NAVIGATION_SETTINGS = 2;
    }

    private AccessibilityShortcutsTutorial() {}

    private static final DialogInterface.OnClickListener ON_CLICK_LISTENER =
            (DialogInterface dialog, int which) -> dialog.dismiss();

    /**
     * Displays a dialog that guides users to use accessibility features with accessibility
     * gestures under system gesture navigation mode.
     */
    public static AlertDialog showGestureNavigationTutorialDialog(Context context,
            DialogInterface.OnDismissListener onDismissListener) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(createTutorialDialogContentView(context,
                        DialogType.GESTURE_NAVIGATION_SETTINGS))
                .setPositiveButton(R.string.accessibility_tutorial_dialog_button, ON_CLICK_LISTENER)
                .setOnDismissListener(onDismissListener)
                .create();

        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        return alertDialog;
    }

    static AlertDialog showAccessibilityGestureTutorialDialog(Context context) {
        return createDialog(context, DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_GESTURE);
    }

    static AlertDialog createAccessibilityTutorialDialog(
            @NonNull Context context, int shortcutTypes, @NonNull CharSequence featureName) {
        return createAccessibilityTutorialDialog(
                context, shortcutTypes, ON_CLICK_LISTENER, featureName);
    }

    static AlertDialog createAccessibilityTutorialDialog(
            @NonNull Context context,
            int shortcutTypes,
            @Nullable DialogInterface.OnClickListener actionButtonListener,
            @NonNull CharSequence featureName) {

        final int category = SettingsEnums.SWITCH_SHORTCUT_DIALOG_ACCESSIBILITY_BUTTON_SETTINGS;
        final DialogInterface.OnClickListener linkButtonListener =
                (dialog, which) -> new SubSettingLauncher(context)
                        .setDestination(AccessibilityButtonFragment.class.getName())
                        .setSourceMetricsCategory(category)
                        .launch();

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.accessibility_tutorial_dialog_button,
                        actionButtonListener)
                .setNegativeButton(R.string.accessibility_tutorial_dialog_link_button,
                        linkButtonListener)
                .create();

        final List<TutorialPage> tutorialPages = createShortcutTutorialPages(
                context, shortcutTypes, featureName, /* isInSetupWizard= */ false);
        Preconditions.checkArgument(!tutorialPages.isEmpty(),
                /* errorMessage= */ "Unexpected tutorial pages size");

        final TutorialPageChangeListener.OnPageSelectedCallback callback =
                index -> updateTutorialNegativeButtonTextAndVisibility(
                        alertDialog, tutorialPages, index);

        alertDialog.setView(createShortcutNavigationContentView(context, tutorialPages, callback));

        // Showing first page won't invoke onPageSelectedCallback. Need to check the first tutorial
        // page type manually to set correct visibility of the link button.
        alertDialog.setOnShowListener(
                dialog -> updateTutorialNegativeButtonTextAndVisibility(
                        alertDialog, tutorialPages, /* selectedPageIndex= */ 0));

        return alertDialog;
    }

    private static void updateTutorialNegativeButtonTextAndVisibility(
            AlertDialog dialog, List<TutorialPage> pages, int selectedPageIndex) {
        final Button button = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        final int pageType = pages.get(selectedPageIndex).getType();
        final int buttonVisibility = (pageType == SOFTWARE) ? VISIBLE : GONE;
        button.setVisibility(buttonVisibility);
        if (buttonVisibility == VISIBLE) {
            final int textResId = AccessibilityUtil.isFloatingMenuEnabled(dialog.getContext())
                    ? R.string.accessibility_tutorial_dialog_link_button
                    : R.string.accessibility_tutorial_dialog_configure_software_shortcut_type;
            button.setText(textResId);
        }
    }

    static AlertDialog createAccessibilityTutorialDialogForSetupWizard(Context context,
            int shortcutTypes, CharSequence featureName) {
        return createAccessibilityTutorialDialogForSetupWizard(context, shortcutTypes,
                ON_CLICK_LISTENER, featureName);
    }

    static AlertDialog createAccessibilityTutorialDialogForSetupWizard(
            @NonNull Context context,
            int shortcutTypes,
            @Nullable DialogInterface.OnClickListener actionButtonListener,
            @NonNull CharSequence featureName) {

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.accessibility_tutorial_dialog_button,
                        actionButtonListener)
                .create();

        final List<TutorialPage> tutorialPages = createShortcutTutorialPages(
                context, shortcutTypes, featureName, /* inSetupWizard= */ true);
        Preconditions.checkArgument(!tutorialPages.isEmpty(),
                /* errorMessage= */ "Unexpected tutorial pages size");

        alertDialog.setView(createShortcutNavigationContentView(context, tutorialPages, null));

        return alertDialog;
    }

    /**
     * Gets a content View for a dialog to confirm that they want to enable a service.
     *
     * @param context    A valid context
     * @param dialogType The type of tutorial dialog
     * @return A content view suitable for viewing
     */
    private static View createTutorialDialogContentView(Context context, int dialogType) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View content = null;

        switch (dialogType) {
            case DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_BUTTON:
                content = inflater.inflate(
                        R.layout.tutorial_dialog_launch_service_by_accessibility_button, null);
                break;
            case DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_GESTURE:
                content = inflater.inflate(
                        R.layout.tutorial_dialog_launch_service_by_gesture_navigation, null);
                setupGestureNavigationTextWithImage(context, content);
                break;
            case DialogType.GESTURE_NAVIGATION_SETTINGS:
                content = inflater.inflate(
                        R.layout.tutorial_dialog_launch_by_gesture_navigation_settings, null);
                setupGestureNavigationTextWithImage(context, content);
                break;
        }

        return content;
    }

    private static void setupGestureNavigationTextWithImage(Context context, View view) {
        final boolean isTouchExploreEnabled = AccessibilityUtil.isTouchExploreEnabled(context);

        final ImageView imageView = view.findViewById(R.id.image);
        final int gestureSettingsImageResId =
                isTouchExploreEnabled
                        ? R.drawable.accessibility_shortcut_type_gesture_preview_touch_explore_on
                        : R.drawable.accessibility_shortcut_type_gesture_preview;
        imageView.setImageResource(gestureSettingsImageResId);

        final TextView textView = view.findViewById(R.id.gesture_tutorial_message);
        textView.setText(isTouchExploreEnabled
                ? R.string.accessibility_tutorial_dialog_message_gesture_settings_talkback
                : R.string.accessibility_tutorial_dialog_message_gesture_settings);
    }

    private static AlertDialog createDialog(Context context, int dialogType) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(createTutorialDialogContentView(context, dialogType))
                .setPositiveButton(R.string.accessibility_tutorial_dialog_button, ON_CLICK_LISTENER)
                .create();

        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        return alertDialog;
    }

    private static class TutorialPagerAdapter extends PagerAdapter {
        private final List<TutorialPage> mTutorialPages;
        private TutorialPagerAdapter(List<TutorialPage> tutorialPages) {
            this.mTutorialPages = tutorialPages;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            final View itemView = mTutorialPages.get(position).getIllustrationView();
            container.addView(itemView);
            return itemView;
        }

        @Override
        public int getCount() {
            return mTutorialPages.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view == o;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position,
                @NonNull Object object) {
            final View itemView = mTutorialPages.get(position).getIllustrationView();
            container.removeView(itemView);
        }
    }

    private static ImageView createImageView(Context context, int imageRes) {
        final ImageView imageView = new ImageView(context);
        imageView.setImageResource(imageRes);
        imageView.setAdjustViewBounds(true);

        return imageView;
    }

    private static View createIllustrationView(Context context, @DrawableRes int imageRes) {
        final View illustrationFrame = inflateAndInitIllustrationFrame(context);
        final LottieAnimationView lottieView = illustrationFrame.findViewById(R.id.image);
        lottieView.setImageResource(imageRes);

        return illustrationFrame;
    }

    private static View createIllustrationViewWithImageRawResource(Context context,
            @RawRes int imageRawRes) {
        final View illustrationFrame = inflateAndInitIllustrationFrame(context);
        final LottieAnimationView lottieView = illustrationFrame.findViewById(R.id.image);
        lottieView.setFailureListener(
                result -> Log.w(TAG, "Invalid image raw resource id: " + imageRawRes,
                        result));
        lottieView.setAnimation(imageRawRes);
        // Follow the Motion Stoppable requirement by using a finite animation.
        lottieView.setRepeatCount(0);
        LottieColorUtils.applyDynamicColors(context, lottieView);
        lottieView.playAnimation();

        return illustrationFrame;
    }

    private static View inflateAndInitIllustrationFrame(Context context) {
        final LayoutInflater inflater = context.getSystemService(LayoutInflater.class);

        return inflater.inflate(R.layout.accessibility_lottie_animation_view, /* root= */ null);
    }

    private static View createShortcutNavigationContentView(Context context,
            List<TutorialPage> tutorialPages,
            TutorialPageChangeListener.OnPageSelectedCallback onPageSelectedCallback) {

        final LayoutInflater inflater = context.getSystemService(LayoutInflater.class);
        final View contentView = inflater.inflate(
                R.layout.accessibility_shortcut_tutorial_dialog, /* root= */ null);

        final LinearLayout indicatorContainer = contentView.findViewById(R.id.indicator_container);
        indicatorContainer.setVisibility(tutorialPages.size() > 1 ? VISIBLE : GONE);
        for (TutorialPage page : tutorialPages) {
            indicatorContainer.addView(page.getIndicatorIcon());
        }
        tutorialPages.get(/* firstIndex */ 0).getIndicatorIcon().setEnabled(true);

        final TextSwitcher title = contentView.findViewById(R.id.title);
        title.setFactory(() -> makeTitleView(context));
        title.setText(tutorialPages.get(/* firstIndex */ 0).getTitle());

        final TextSwitcher instruction = contentView.findViewById(R.id.instruction);
        instruction.setFactory(() -> makeInstructionView(context));
        instruction.setText(tutorialPages.get(/* firstIndex */ 0).getInstruction());

        final ViewPager viewPager = contentView.findViewById(R.id.view_pager);
        viewPager.setAdapter(new TutorialPagerAdapter(tutorialPages));
        viewPager.setContentDescription(context.getString(R.string.accessibility_tutorial_pager,
                /* firstPage */ 1, tutorialPages.size()));
        viewPager.setImportantForAccessibility(tutorialPages.size() > 1
                ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        TutorialPageChangeListener listener = new TutorialPageChangeListener(context, viewPager,
                title, instruction, tutorialPages);
        listener.setOnPageSelectedCallback(onPageSelectedCallback);

        return contentView;
    }

    private static View makeTitleView(Context context) {
        final TextView textView = new TextView(context);
        // Sets the text color, size, style, hint color, and highlight color from the specified
        // TextAppearance resource.
        TextViewCompat.setTextAppearance(textView, R.style.AccessibilityDialogTitle);
        textView.setGravity(Gravity.CENTER);
        return textView;
    }

    private static View makeInstructionView(Context context) {
        final TextView textView = new TextView(context);
        TextViewCompat.setTextAppearance(textView, R.style.AccessibilityDialogDescription);
        return textView;
    }

    @SuppressLint("SwitchIntDef")
    private static CharSequence getShortcutTitle(
            @NonNull Context context, @UserShortcutType int shortcutType, int buttonMode) {
        return switch (shortcutType) {
            case HARDWARE -> context.getText(R.string.accessibility_tutorial_dialog_title_volume);
            case SOFTWARE -> getSoftwareTitle(context, buttonMode);
            case GESTURE -> context.getText(R.string.accessibility_tutorial_dialog_title_gesture);
            case TRIPLETAP -> context.getText(R.string.accessibility_tutorial_dialog_title_triple);
            case TWOFINGER_DOUBLETAP -> context.getString(
                    R.string.accessibility_tutorial_dialog_title_two_finger_double, 2);
            case QUICK_SETTINGS -> context.getText(
                    R.string.accessibility_tutorial_dialog_title_quick_setting);
            default -> "";
        };
    }

    @SuppressLint("SwitchIntDef")
    private static View getShortcutImage(
            @NonNull Context context, @UserShortcutType int shortcutType, int buttonMode) {
        return switch (shortcutType) {
            case HARDWARE -> createIllustrationView(
                    context, R.drawable.accessibility_shortcut_type_volume_keys);
            case SOFTWARE -> createSoftwareImage(context, buttonMode);
            case GESTURE -> createIllustrationView(context,
                    AccessibilityUtil.isTouchExploreEnabled(context)
                            ? R.drawable.accessibility_shortcut_type_gesture_touch_explore_on
                            : R.drawable.accessibility_shortcut_type_gesture);
            case TRIPLETAP -> createIllustrationViewWithImageRawResource(context,
                    R.raw.accessibility_shortcut_type_tripletap);
            case TWOFINGER_DOUBLETAP -> createIllustrationViewWithImageRawResource(context,
                    R.raw.accessibility_shortcut_type_2finger_doubletap);
            case QUICK_SETTINGS -> {
                View v = createIllustrationView(context,
                        R.drawable.accessibility_shortcut_type_quick_settings);
                View bg = v.findViewById(R.id.image_background);
                if (bg != null) {
                    bg.setVisibility(GONE);
                }
                yield v;
            }
            default -> new View(context);
        };
    }

    private static CharSequence getShortcutInstruction(
            @NonNull Context context, @UserShortcutType int shortcutType, int buttonMode,
            @NonNull CharSequence featureName, boolean inSetupWizard) {
        return switch (shortcutType) {
            case HARDWARE -> context.getText(R.string.accessibility_tutorial_dialog_message_volume);
            case SOFTWARE -> getSoftwareInstruction(context, buttonMode);
            case GESTURE -> StringUtil.getIcuPluralsString(
                    context,
                    AccessibilityUtil.isTouchExploreEnabled(context) ? 3 : 2,
                    R.string.accessibility_tutorial_dialog_gesture_shortcut_instruction);
            case TRIPLETAP -> context.getString(
                    R.string.accessibility_tutorial_dialog_tripletap_instruction, 3);
            case TWOFINGER_DOUBLETAP -> context.getString(
                    R.string.accessibility_tutorial_dialog_twofinger_doubletap_instruction, 2);
            case QUICK_SETTINGS -> getQuickSettingsInstruction(context, featureName, inSetupWizard);
            default -> "";
        };
    }

    @SuppressLint("SwitchIntDef")
    private static TutorialPage createShortcutTutorialPage(
            @NonNull Context context, @UserShortcutType int shortcutType, int buttonMode,
            @NonNull CharSequence featureName, boolean inSetupWizard) {

        final ImageView indicatorIcon =
                createImageView(context, R.drawable.ic_accessibility_page_indicator);
        indicatorIcon.setEnabled(false);

        return new TutorialPage(shortcutType,
                getShortcutTitle(context, shortcutType, buttonMode),
                getShortcutImage(context, shortcutType, buttonMode),
                createImageView(context, R.drawable.ic_accessibility_page_indicator),
                getShortcutInstruction(
                        context, shortcutType, buttonMode, featureName, inSetupWizard));
    }

    /**
     * Create the tutorial pages for selected shortcut types in the same order as shown in the
     * edit shortcut screen.
     */
    @VisibleForTesting
    static List<TutorialPage> createShortcutTutorialPages(
            @NonNull Context context, int shortcutTypes, @NonNull CharSequence featureName,
            boolean inSetupWizard) {
        final List<TutorialPage> tutorialPages = new ArrayList<>();
        int buttonMode = ShortcutUtils.getButtonMode(context, context.getUserId());

        for (int shortcutType: AccessibilityUtil.SHORTCUTS_ORDER_IN_UI) {
            if ((shortcutTypes & shortcutType) == 0) {
                continue;
            }
            tutorialPages.add(
                    createShortcutTutorialPage(
                            context, shortcutType, buttonMode, featureName, inSetupWizard));
        }

        return tutorialPages;
    }

    private static View createSoftwareImage(Context context, int buttonMode) {
        return switch(buttonMode) {
            case ACCESSIBILITY_BUTTON_MODE_GESTURE ->
                    createIllustrationView(context,
                            AccessibilityUtil.isTouchExploreEnabled(context)
                                    ? R.drawable
                                    .accessibility_shortcut_type_gesture_touch_explore_on
                                    : R.drawable.accessibility_shortcut_type_gesture);
            case ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU ->
                createIllustrationViewWithImageRawResource(
                        context, R.raw.accessibility_shortcut_type_fab);
            default -> createIllustrationView(
                    context, R.drawable.accessibility_shortcut_type_navbar);
        };
    }

    private static CharSequence getSoftwareTitle(Context context, int buttonMode) {
        return context.getText(buttonMode == ACCESSIBILITY_BUTTON_MODE_GESTURE
                ? R.string.accessibility_tutorial_dialog_title_gesture
                : R.string.accessibility_tutorial_dialog_title_button);
    }

    private static CharSequence getSoftwareInstruction(Context context, int buttonMode) {
        return switch(buttonMode) {
            case ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU -> context.getText(
                    R.string.accessibility_tutorial_dialog_message_floating_button);
            case ACCESSIBILITY_BUTTON_MODE_GESTURE -> StringUtil.getIcuPluralsString(
                    context,
                    AccessibilityUtil.isTouchExploreEnabled(context) ? 3 : 2,
                    R.string.accessibility_tutorial_dialog_gesture_shortcut_instruction);
            default -> getSoftwareInstructionWithIcon(context,
                    context.getText(R.string.accessibility_tutorial_dialog_message_button));
        };
    }

    private static CharSequence getSoftwareInstructionWithIcon(Context context, CharSequence text) {
        final String message = text.toString();
        final SpannableString spannableInstruction = SpannableString.valueOf(message);
        final int indexIconStart = message.indexOf("%s");
        final int indexIconEnd = indexIconStart + 2;
        final ImageView iconView = new ImageView(context);
        iconView.setImageDrawable(context.getDrawable(R.drawable.ic_accessibility_new));
        final Drawable icon = iconView.getDrawable().mutate();
        final ImageSpan imageSpan = new ImageSpan(icon);
        imageSpan.setContentDescription("");
        icon.setBounds(/* left= */ 0, /* top= */ 0,
                icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        spannableInstruction.setSpan(imageSpan, indexIconStart, indexIconEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableInstruction;
    }

    private static CharSequence getQuickSettingsInstruction(
            Context context, CharSequence featureName, boolean inSetupWizard) {
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count",
                AccessibilityUtil.isTouchExploreEnabled(context) ? 2 : 1);
        arguments.put("featureName", featureName);
        final CharSequence pluralsString = StringUtil.getIcuPluralsString(
                context, arguments,
                R.string.accessibility_tutorial_dialog_message_quick_setting);
        final SpannableStringBuilder tutorialText = new SpannableStringBuilder();
        if (inSetupWizard) {
            tutorialText.append(context.getText(R.string
                            .accessibility_tutorial_dialog_shortcut_unavailable_in_suw))
                    .append("\n\n");
        }
        return tutorialText.append(pluralsString);
    }

    private static class TutorialPage {
        private final int mType;
        private final CharSequence mTitle;
        private final View mIllustrationView;
        private final ImageView mIndicatorIcon;
        private final CharSequence mInstruction;

        TutorialPage(int type, CharSequence title, View illustrationView, ImageView indicatorIcon,
                CharSequence instruction) {
            this.mType = type;
            this.mTitle = title;
            this.mIllustrationView = illustrationView;
            this.mIndicatorIcon = indicatorIcon;
            this.mInstruction = instruction;

            setupIllustrationChildViewsGravity();
        }

        public int getType() {
            return mType;
        }

        public CharSequence getTitle() {
            return mTitle;
        }

        public View getIllustrationView() {
            return mIllustrationView;
        }

        public ImageView getIndicatorIcon() {
            return mIndicatorIcon;
        }

        public CharSequence getInstruction() {
            return mInstruction;
        }

        private void setupIllustrationChildViewsGravity() {
            final View backgroundView = mIllustrationView.findViewById(R.id.image_background);
            initViewGravity(backgroundView);

            final View lottieView = mIllustrationView.findViewById(R.id.image);
            initViewGravity(lottieView);
        }

        private void initViewGravity(@NonNull View view) {
            final FrameLayout.LayoutParams layoutParams =
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;

            view.setLayoutParams(layoutParams);
        }
    }

    private static class TutorialPageChangeListener implements ViewPager.OnPageChangeListener {
        private int mLastTutorialPagePosition = 0;
        private final Context mContext;
        private final TextSwitcher mTitle;
        private final TextSwitcher mInstruction;
        private final List<TutorialPage> mTutorialPages;
        private final ViewPager mViewPager;
        private OnPageSelectedCallback mOnPageSelectedCallback;

        TutorialPageChangeListener(Context context, ViewPager viewPager, ViewGroup title,
                ViewGroup instruction, List<TutorialPage> tutorialPages) {
            this.mContext = context;
            this.mViewPager = viewPager;
            this.mTitle = (TextSwitcher) title;
            this.mInstruction = (TextSwitcher) instruction;
            this.mTutorialPages = tutorialPages;
            this.mOnPageSelectedCallback = null;

            this.mViewPager.addOnPageChangeListener(this);
        }

        public void setOnPageSelectedCallback(
                OnPageSelectedCallback callback) {
            this.mOnPageSelectedCallback = callback;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
            // Do nothing.
        }

        @Override
        public void onPageSelected(int position) {
            final boolean isPreviousPosition =
                    mLastTutorialPagePosition > position;
            @AnimRes
            final int inAnimationResId = isPreviousPosition
                    ? android.R.anim.slide_in_left
                    : com.android.internal.R.anim.slide_in_right;

            @AnimRes
            final int outAnimationResId = isPreviousPosition
                    ? android.R.anim.slide_out_right
                    : com.android.internal.R.anim.slide_out_left;

            mTitle.setInAnimation(mContext, inAnimationResId);
            mTitle.setOutAnimation(mContext, outAnimationResId);
            mTitle.setText(mTutorialPages.get(position).getTitle());

            mInstruction.setInAnimation(mContext, inAnimationResId);
            mInstruction.setOutAnimation(mContext, outAnimationResId);
            mInstruction.setText(mTutorialPages.get(position).getInstruction());

            for (TutorialPage page : mTutorialPages) {
                page.getIndicatorIcon().setEnabled(false);
            }
            mTutorialPages.get(position).getIndicatorIcon().setEnabled(true);
            mLastTutorialPagePosition = position;

            final int currentPageNumber = position + 1;
            mViewPager.setContentDescription(
                    mContext.getString(R.string.accessibility_tutorial_pager,
                            currentPageNumber, mTutorialPages.size()));

            if (mOnPageSelectedCallback != null) {
                mOnPageSelectedCallback.onPageSelected(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Do nothing.
        }

        /** The interface that provides a callback method after tutorial page is selected. */
        private interface OnPageSelectedCallback {

            /** The callback method after tutorial page is selected. */
            void onPageSelected(int index);
        }
    }
}
