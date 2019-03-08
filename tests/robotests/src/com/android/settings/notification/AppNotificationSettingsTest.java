package com.android.settings.notification;

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
public class AppNotificationSettingsTest {

    private WindowManager.LayoutParams mLayoutParams;
    private AppNotificationSettings mFragment;
    private FragmentActivity mActivity;
    @Mock
    private Window mWindow;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLayoutParams = new WindowManager.LayoutParams();
        mActivity = spy(Robolectric.setupActivity(FragmentActivity.class));
        mFragment = spy(new AppNotificationSettings());
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mFragment.getFragmentManager()).thenReturn(mock(FragmentManager.class));
        when(mActivity.getWindow()).thenReturn(mWindow);
        when(mWindow.getAttributes()).thenReturn(mLayoutParams);
    }

    @Test
    @Config(shadows = {ShadowNotificationSettingsBase.class})
    public void onResume_shouldHideSystemOverlay() {
        mFragment.onResume();

        verify(mWindow).addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
    }

    @Test
    @Config(shadows = {ShadowNotificationSettingsBase.class})
    public void onPause_shouldRemoveHideSystemOverlay() {
        mFragment.onResume();

        verify(mWindow).addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);

        mFragment.onPause();

        // There's no Window.clearPrivateFlags() method, so the Window.attributes are updated.
        ArgumentCaptor<WindowManager.LayoutParams> paramCaptor = ArgumentCaptor.forClass(
                WindowManager.LayoutParams.class);
        verify(mWindow).setAttributes(paramCaptor.capture());
        assertEquals(0,
                paramCaptor.getValue().privateFlags
                        & SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
    }

    @Implements(NotificationSettingsBase.class)
    public static class ShadowNotificationSettingsBase {

        protected void __constructor__() {
            // Do nothing
        }

        @Implementation
        protected void onResume() {
            // No-op.
        }

        @Implementation
        protected void onPause() {
            // No-op.
        }
    }
}
