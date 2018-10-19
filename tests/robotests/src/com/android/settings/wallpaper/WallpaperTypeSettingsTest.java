package com.android.settings.wallpaper;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.Activity;
import android.content.Intent;
import androidx.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;

@RunWith(SettingsRobolectricTestRunner.class)
public class WallpaperTypeSettingsTest {

    private Preference mPreference;

    private Intent mIntent;

    @Before
    public void setUp() {
        mIntent = new Intent();
        mPreference = new Preference(application);
    }

    @Test
    public void testOnPreferenceTreeClick_intentNull_shouldDoNothing() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        WallpaperTypeSettings fragment = spy(new WallpaperTypeSettings());
        doReturn(activity).when(fragment).getActivity();

        boolean handled = fragment.onPreferenceTreeClick(mPreference);

        assertThat(handled).isFalse();
    }

    @Test
    public void testOnPreferenceTreeClick_shouldLaunchIntentAndFinish() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        WallpaperTypeSettings fragment = spy(new WallpaperTypeSettings());
        doReturn(activity).when(fragment).getActivity();
        mPreference.setIntent(mIntent);
        doNothing().when(fragment).finish();
        ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(fragment).startActivity(intent.capture());

        boolean handled = fragment.onPreferenceTreeClick(mPreference);

        assertThat(handled).isTrue();
        verify(fragment, times(1)).finish();
        assertThat(intent.getValue()).isSameAs(mIntent);
    }
}
