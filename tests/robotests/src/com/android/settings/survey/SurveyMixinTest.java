package com.android.settings.survey;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SurveyMixinTest {

    private static final String FAKE_KEY = "fake_key";
    private static final String FAKE_SURVEY_ID = "fake_id";

    private FakeFeatureFactory mFactory;
    private Context mContext;
    private SurveyFeatureProvider mProvider;
    @Mock
    private BroadcastReceiver mReceiver;
    @Mock
    private InstrumentedPreferenceFragment mFragment;

    @Before
    public void setUp() {
        // set up the fakefeature factory to mock out the survey provider
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mProvider = mFactory.getSurveyFeatureProvider(mContext);
        when(mProvider.getSurveyId(any(), eq(FAKE_KEY))).thenReturn(FAKE_SURVEY_ID);
    }

    @Test
    public void onResume_triesRegisteringReceiverAndDownloadingWhenNoSurveyDetected() {
        // Pretend there is no survey in memory
        when(mProvider.getSurveyExpirationDate(any(), any())).thenReturn(-1L);

        // Pretend we are an activity that is starting up
        Activity temp = Robolectric.setupActivity(Activity.class);
        when(mFragment.getActivity()).thenReturn(temp);
        SurveyMixin mixin = new SurveyMixin(mFragment, FAKE_KEY);
        mixin.onResume();

        // Verify that a download was attempted
        verify(mProvider, times(1)).downloadSurvey(any(), any(), any());
        // Verify that we registered a receiver for download completion broadcasts
        verify(mProvider, times(1)).createAndRegisterReceiver(any());
        // Verify we did not try to show a survey
        verify(mProvider, never()).showSurveyIfAvailable(any(), any());
    }

    @Test
    public void onResume_triesShowingSurveyWhenOneIsPresent() {
        // Pretend there is a survey in memory
        when(mProvider.getSurveyExpirationDate(any(), any())).thenReturn(0L);

        // Pretend we are an activity that is starting up
        Activity temp = Robolectric.setupActivity(Activity.class);
        when(mFragment.getActivity()).thenReturn(temp);
        SurveyMixin mixin = new SurveyMixin(mFragment, FAKE_KEY);
        mixin.onResume();

        // Verify that a download was not attempted
        verify(mProvider, never()).downloadSurvey(any(), any(), any());
        // Verify that we did not register a receiver
        verify(mProvider, never()).createAndRegisterReceiver(any());
        // Verify we tried to show a survey
        verify(mProvider, times(1)).showSurveyIfAvailable(any(), any());
    }

    @Test
    public void onResume_doesNothingWhenActivityIsNull() {
        // Pretend the activity died somewhere in the process
        when(mFragment.getActivity()).thenReturn(null);
        SurveyMixin mixin = new SurveyMixin(mFragment, FAKE_KEY);
        mixin.onResume();

        // Verify we don't try showing or downloading a survey
        verify(mProvider, never()).showSurveyIfAvailable(any(), any());
        verify(mProvider, never()).downloadSurvey(any(), any(), any());
    }

    @Test
    public void onPause_removesReceiverIfPreviouslySet() {
        // Pretend there is a survey in memory
        when(mProvider.getSurveyExpirationDate(any(), any())).thenReturn(-1L);

        // Pretend we are an activity that starts and stops
        Activity temp = Robolectric.setupActivity(Activity.class);
        when(mFragment.getActivity()).thenReturn(temp);
        when(mProvider.createAndRegisterReceiver(any())).thenReturn(mReceiver);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(temp);
        SurveyMixin mixin = new SurveyMixin(mFragment, FAKE_KEY);
        mixin.onResume();
        manager.registerReceiver(mReceiver, new IntentFilter());
        mixin.onPause();

        // Verify we remove the receiver
        HashMap<BroadcastReceiver, ArrayList<IntentFilter>> map =
                ReflectionHelpers.getField(manager, "mReceivers");
        assertThat(map.containsKey(mReceiver)).isFalse();
    }

    @Test
    public void onPause_doesNothingWhenActivityOrReceiverNull() {
        // Pretend there is a survey in memory
        when(mProvider.getSurveyExpirationDate(any(), any())).thenReturn(-1L);

        // Pretend we are an activity that fails to create a receiver properly
        Activity temp = Robolectric.setupActivity(Activity.class);
        when(mFragment.getActivity()).thenReturn(temp);
        SurveyMixin mixin = new SurveyMixin(mFragment, FAKE_KEY);
        mixin.onPause();

        // Verify we do nothing;
        verify(mProvider, never()).showSurveyIfAvailable(any(), any());

        // pretend the activity died before onPause
        when(mFragment.getActivity()).thenReturn(null);
        mixin.onPause();

        // Verify we do nothing
        verify(mProvider, never()).showSurveyIfAvailable(any(), any());
    }

}
