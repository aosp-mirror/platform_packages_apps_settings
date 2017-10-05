package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchResultAggregatorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private FakeFeatureFactory mFeatureFactory;

    private SearchResultAggregator mAggregator;

    @Mock
    private DatabaseResultLoader mStaticTask;
    @Mock
    private InstalledAppResultLoader mAppTask;
    @Mock
    private InputDeviceResultLoader mInputTask;
    @Mock
    private AccessibilityServiceResultLoader mMAccessibilityTask;
    @Mock
    private ExecutorService mService;


    private String[] DB_TITLES = {"static_one", "static_two"};
    private String[] INPUT_TITLES = {"input_one", "input_two"};
    private String[] ACCESS_TITLES = {"access_one", "access_two"};
    private String[] APP_TITLES = {"app_one", "app_two"};


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mAggregator = spy(SearchResultAggregator.getInstance());
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        // Return mock loaders from feature provider
        when(mFeatureFactory.searchFeatureProvider.getStaticSearchResultTask(any(Context.class),
                anyString())).thenReturn(mStaticTask);
        when(mFeatureFactory.searchFeatureProvider.getInstalledAppSearchTask(any(Context.class),
                anyString())).thenReturn(mAppTask);
        when(mFeatureFactory.searchFeatureProvider.getInputDeviceResultTask(any(Context.class),
                anyString())).thenReturn(mInputTask);
        when(mFeatureFactory.searchFeatureProvider.getAccessibilityServiceResultTask(
                any(Context.class),
                anyString())).thenReturn(mMAccessibilityTask);
        when(mFeatureFactory.searchFeatureProvider.getExecutorService()).thenReturn(mService);

        // Return fake data from the loaders
        List<? extends SearchResult> dbResults = getDummyDbResults();
        doReturn(dbResults).when(mStaticTask).get(anyLong(), any(TimeUnit.class));

        List<? extends SearchResult> appResults = getDummyAppResults();
        doReturn(appResults).when(mAppTask).get(anyLong(), any(TimeUnit.class));

        List<? extends SearchResult> inputResults = getDummyInputDeviceResults();
        doReturn(inputResults).when(mInputTask).get(anyLong(), any(TimeUnit.class));

        List<? extends SearchResult> accessResults = getDummyAccessibilityResults();
        doReturn(accessResults).when(mMAccessibilityTask).get(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testStaticResults_mergedProperly() {
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(mContext))
                .thenReturn(false);

        List<? extends SearchResult> results = mAggregator.fetchResults(mContext, "test");

        assertThat(results).hasSize(8);
        assertThat(results.get(0).title).isEqualTo(DB_TITLES[0]);
        assertThat(results.get(1).title).isEqualTo(DB_TITLES[1]);
        assertThat(results.get(2).title).isEqualTo(APP_TITLES[0]);
        assertThat(results.get(3).title).isEqualTo(ACCESS_TITLES[0]);
        assertThat(results.get(4).title).isEqualTo(INPUT_TITLES[0]);
        assertThat(results.get(5).title).isEqualTo(APP_TITLES[1]);
        assertThat(results.get(6).title).isEqualTo(ACCESS_TITLES[1]);
        assertThat(results.get(7).title).isEqualTo(INPUT_TITLES[1]);
    }

    @Test
    public void testStaticRanking_staticThrowsException_dbResultsAreMissing() throws Exception {
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(mContext))
                .thenReturn(false);
        when(mStaticTask.get(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        List<? extends SearchResult> results = mAggregator.fetchResults(mContext, "test");

        assertThat(results).hasSize(6);
        assertThat(results.get(0).title).isEqualTo(APP_TITLES[0]);
        assertThat(results.get(1).title).isEqualTo(ACCESS_TITLES[0]);
        assertThat(results.get(2).title).isEqualTo(INPUT_TITLES[0]);
        assertThat(results.get(3).title).isEqualTo(APP_TITLES[1]);
        assertThat(results.get(4).title).isEqualTo(ACCESS_TITLES[1]);
        assertThat(results.get(5).title).isEqualTo(INPUT_TITLES[1]);
    }

    @Test
    public void testStaticRanking_appsThrowException_appResultsAreMissing() throws Exception {
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(mContext))
                .thenReturn(false);
        when(mAppTask.get(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        List<? extends SearchResult> results = mAggregator.fetchResults(mContext, "test");

        assertThat(results).hasSize(6);
        assertThat(results.get(0).title).isEqualTo(DB_TITLES[0]);
        assertThat(results.get(1).title).isEqualTo(DB_TITLES[1]);
        assertThat(results.get(2).title).isEqualTo(ACCESS_TITLES[0]);
        assertThat(results.get(3).title).isEqualTo(INPUT_TITLES[0]);
        assertThat(results.get(4).title).isEqualTo(ACCESS_TITLES[1]);
        assertThat(results.get(5).title).isEqualTo(INPUT_TITLES[1]);
    }

    @Test
    public void testStaticRanking_inputThrowException_inputResultsAreMissing() throws Exception {
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(mContext))
                .thenReturn(false);
        when(mInputTask.get(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        List<? extends SearchResult> results = mAggregator.fetchResults(mContext, "test");

        assertThat(results).hasSize(6);
        assertThat(results.get(0).title).isEqualTo(DB_TITLES[0]);
        assertThat(results.get(1).title).isEqualTo(DB_TITLES[1]);
        assertThat(results.get(2).title).isEqualTo(APP_TITLES[0]);
        assertThat(results.get(3).title).isEqualTo(ACCESS_TITLES[0]);
        assertThat(results.get(4).title).isEqualTo(APP_TITLES[1]);
        assertThat(results.get(5).title).isEqualTo(ACCESS_TITLES[1]);
    }

    @Test
    public void testStaticRanking_accessThrowException_accessResultsAreMissing() throws Exception {
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(mContext))
                .thenReturn(false);
        when(mMAccessibilityTask.get(anyLong(), any(TimeUnit.class))).thenThrow(
                new InterruptedException());

        List<? extends SearchResult> results = mAggregator.fetchResults(mContext, "test");

        assertThat(results).hasSize(6);
        assertThat(results.get(0).title).isEqualTo(DB_TITLES[0]);
        assertThat(results.get(1).title).isEqualTo(DB_TITLES[1]);
        assertThat(results.get(2).title).isEqualTo(APP_TITLES[0]);
        assertThat(results.get(3).title).isEqualTo(INPUT_TITLES[0]);
        assertThat(results.get(4).title).isEqualTo(APP_TITLES[1]);
        assertThat(results.get(5).title).isEqualTo(INPUT_TITLES[1]);
    }

    @Test
    public void testDynamicRanking_sortsWithDynamicRanking() {
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(
                true);

        List<? extends SearchResult> results = mAggregator.fetchResults(mContext, "test");

        assertThat(results).hasSize(8);
        assertThat(results.get(0).title).isEqualTo(DB_TITLES[0]);
        assertThat(results.get(1).title).isEqualTo(DB_TITLES[1]);
        assertThat(results.get(2).title).isEqualTo(APP_TITLES[0]);
        assertThat(results.get(3).title).isEqualTo(ACCESS_TITLES[0]);
        assertThat(results.get(4).title).isEqualTo(INPUT_TITLES[0]);
        assertThat(results.get(5).title).isEqualTo(APP_TITLES[1]);
        assertThat(results.get(6).title).isEqualTo(ACCESS_TITLES[1]);
        assertThat(results.get(7).title).isEqualTo(INPUT_TITLES[1]);
    }

    private List<? extends SearchResult> getDummyDbResults() {
        List<SearchResult> results = new ArrayList<>();
        ResultPayload payload = new ResultPayload(new Intent());
        SearchResult.Builder builder = new SearchResult.Builder();
        builder.setPayload(payload)
                .setTitle(DB_TITLES[0])
                .setRank(1)
                .setStableId(Objects.hash(DB_TITLES[0], "db"));
        results.add(builder.build());

        builder.setTitle(DB_TITLES[1])
                .setRank(2)
                .setStableId(Objects.hash(DB_TITLES[1], "db"));
        results.add(builder.build());

        return results;
    }

    private List<? extends SearchResult> getDummyAppResults() {
        List<AppSearchResult> results = new ArrayList<>();
        ResultPayload payload = new ResultPayload(new Intent());
        AppSearchResult.Builder builder = new AppSearchResult.Builder();
        builder.setPayload(payload)
                .setTitle(APP_TITLES[0])
                .setRank(1)
                .setStableId(Objects.hash(APP_TITLES[0], "app"));
        results.add(builder.build());

        builder.setTitle(APP_TITLES[1])
                .setRank(2)
                .setStableId(Objects.hash(APP_TITLES[1], "app"));
        results.add(builder.build());

        return results;
    }

    public List<? extends SearchResult> getDummyInputDeviceResults() {
        List<SearchResult> results = new ArrayList<>();
        ResultPayload payload = new ResultPayload(new Intent());
        AppSearchResult.Builder builder = new AppSearchResult.Builder();
        builder.setPayload(payload)
                .setTitle(INPUT_TITLES[0])
                .setRank(1)
                .setStableId(Objects.hash(INPUT_TITLES[0], "app"));
        results.add(builder.build());

        builder.setTitle(INPUT_TITLES[1])
                .setRank(2)
                .setStableId(Objects.hash(INPUT_TITLES[1], "app"));
        results.add(builder.build());

        return results;
    }

    public List<? extends SearchResult> getDummyAccessibilityResults() {
        List<SearchResult> results = new ArrayList<>();
        ResultPayload payload = new ResultPayload(new Intent());
        AppSearchResult.Builder builder = new AppSearchResult.Builder();
        builder.setPayload(payload)
                .setTitle(ACCESS_TITLES[0])
                .setRank(1)
                .setStableId(Objects.hash(ACCESS_TITLES[0], "app"));
        results.add(builder.build());

        builder.setTitle(ACCESS_TITLES[1])
                .setRank(2)
                .setStableId(Objects.hash(ACCESS_TITLES[1], "app"));
        results.add(builder.build());

        return results;
    }
}
