package com.android.settings.support;

import android.accounts.Account;
import android.annotation.NonNull;
import android.annotation.StringRes;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.text.Spannable;
import android.text.style.URLSpan;
import android.widget.CheckBox;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.SupportFeatureProvider;
import com.android.settings.overlay.SupportFeatureProvider.SupportType;
import com.android.settings.support.SupportDisclaimerDialogFragmentTest.SupportDisclaimerShadowResources;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.FragmentTestUtil;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.internal.Shadow.directlyOn;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {SupportDisclaimerShadowResources.class})
public class SupportDisclaimerDialogFragmentTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Context mContext;
    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private SupportFeatureProvider mSupportFeatureProvider;

    private final Account mFakeAccount = new Account("user1", "fake_type");

    private static final int FAKE_RES_ID = -1000;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFakeFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        mSupportFeatureProvider = mFakeFeatureFactory.getSupportFeatureProvider(mContext);
        when(mSupportFeatureProvider.getDisclaimerStringResId())
                .thenReturn(FAKE_RES_ID);
    }

    @Test
    public void onClick_DoNotShowCheckedLogsAction() {
        SupportDisclaimerDialogFragment fragment =
                SupportDisclaimerDialogFragment.newInstance(mFakeAccount, SupportType.CHAT);
        FragmentTestUtil.startFragment(fragment);

        // pretend the user selected to skip the dialog in the future
        CheckBox doNotShow = (CheckBox) fragment.getDialog()
                .findViewById(R.id.support_disclaimer_do_not_show_again);
        doNotShow.setChecked(true);

        // verify we logged the action
        fragment.onClick(fragment.getDialog(), Dialog.BUTTON_POSITIVE);
        verify(mMetricsFeatureProvider, times(1)).action(any(),
                eq(MetricsProto.MetricsEvent.ACTION_SKIP_DISCLAIMER_SELECTED));
    }

    @Test
    public void onClick_DoNotShowUncheckedDoesNotLogAction() {
        SupportDisclaimerDialogFragment fragment =
                SupportDisclaimerDialogFragment.newInstance(mFakeAccount, SupportType.CHAT);
        FragmentTestUtil.startFragment(fragment);

        // pretend the user selected to skip the dialog in the future
        CheckBox doNotShow = (CheckBox) fragment.getDialog()
                .findViewById(R.id.support_disclaimer_do_not_show_again);
        doNotShow.setChecked(false);

        // verify we logged the action
        fragment.onClick(fragment.getDialog(), Dialog.BUTTON_POSITIVE);
        verify(mMetricsFeatureProvider, never()).action(any(),
                eq(MetricsProto.MetricsEvent.ACTION_SKIP_DISCLAIMER_SELECTED));
    }

    @Implements(Resources.class)
    public static class SupportDisclaimerShadowResources extends SettingsShadowResources {

        @Implementation
        @NonNull public CharSequence getText(@StringRes int id) throws NotFoundException {
            if (id == FAKE_RES_ID) {
                Spannable text = Spannable.Factory.getInstance()
                        .newSpannable("string with url");
                text.setSpan(new URLSpan("https://google.com"), 0, 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return text;
            }
            return directlyOn(realResources, Resources.class).getText(id);
        }
    }

}
