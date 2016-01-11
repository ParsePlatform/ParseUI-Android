package com.parse;

import android.app.Fragment;
import android.database.DataSetObserver;
import android.view.View;
import android.widget.TextView;

import com.parse.ParseQuery.CachePolicy;
import com.parse.ParseQueryPagerAdapter.OnQueryLoadListener;
import com.parse.ParseQueryPagerAdapter.QueryFactory;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import bolts.Capture;
import bolts.Task;

import com.parse.widget.test.R;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Modified by Pablo Baxter (Github: soaboz)
 */
public class ParseQueryPagerAdapterTest extends BaseActivityInstrumentationTestCase2<TestActivity>{

    @ParseClassName("Thing")
    public static class Thing extends ParseObject{
        public Thing(){
        }
    }

    public ParseQueryPagerAdapterTest(){
        super(TestActivity.class);
    }

    private int TOTAL_THINGS = 10;
    private List<ParseObject> savedThings = new ArrayList<ParseObject>();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Register a mock cachedQueryController, the controller maintain a cache list and return
        // results based on query state's CachePolicy
        ParseQueryController queryController = mock(ParseQueryController.class);
        Answer<Task<List<ParseObject>>> queryAnswer = new Answer<Task<List<ParseObject>>>() {
            private List<ParseObject> cachedThings = new ArrayList<>();

            @Override
            public Task<List<ParseObject>> answer(InvocationOnMock invocation) throws Throwable {
                ParseQuery.State state = (ParseQuery.State) invocation.getArguments()[0];
                int start = state.skip();
                // The default value of limit in ParseQuery is -1.
                int end = state.limit() > 0 ?
                        Math.min(state.skip() + state.limit(), TOTAL_THINGS) : TOTAL_THINGS;
                List<ParseObject> things;
                if (state.cachePolicy() == CachePolicy.CACHE_ONLY) {
                    try {
                        things = new ArrayList<>(cachedThings.subList(start, end));
                    } catch (IndexOutOfBoundsException e) {
                        // Cache miss, throw exception
                        return Task.forError(
                                new ParseException(ParseException.CACHE_MISS, "results not cached"));
                    }
                } else {
                    things = new ArrayList<>(savedThings.subList(start, end));
                    // Update cache
                    for (int i = start; i < end; i++) {
                        if (i < cachedThings.size()) {
                            cachedThings.set(i, savedThings.get(i));
                        } else {
                            cachedThings.add(i, savedThings.get(i));
                        }
                    }
                }
                return Task.forResult(things);
            }
        };
        when(queryController.findAsync(any(ParseQuery.State.class), any(ParseUser.class), any(Task.class)))
                .thenAnswer(queryAnswer);
        ParseCorePlugins.getInstance().registerQueryController(queryController);

        // Register a mock currentUserController to make getSessionToken work
        ParseCurrentUserController currentUserController = mock(ParseCurrentUserController.class);
        when(currentUserController.getAsync()).thenReturn(Task.forResult(mock(ParseUser.class)));
        when(currentUserController.getCurrentSessionTokenAsync())
                .thenReturn(Task.<String>forResult(null));
        ParseCorePlugins.getInstance().registerCurrentUserController(currentUserController);

        ParseObject.registerSubclass(Thing.class);
        // Make test data set
        for (int i = 0; i < TOTAL_THINGS; i++) {
            ParseObject thing = ParseObject.create("Thing");
            thing.put("aValue", i * 10);
            thing.put("name", "Thing " + i);
            thing.setObjectId(String.valueOf(i));
            savedThings.add(thing);
        }
    }

    @Override
    public void tearDown() throws Exception {
        savedThings = null;
        ParseCorePlugins.getInstance().reset();
        ParseObject.unregisterSubclass("Thing");
        super.tearDown();
    }

    public void testLoadObjects() throws Exception {
        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        final Semaphore done = new Semaphore(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                assertNull(e);
                assertEquals(TOTAL_THINGS, objects.size());
                done.release();
            }
        });

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testLoadObjectsWithGenericParseObjects() throws Exception {
        final ParseQueryPagerAdapter<ParseObject> adapter =
                new ParseQueryPagerAdapter<>(activity, Thing.class);
        final Semaphore done = new Semaphore(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<ParseObject>() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(List<ParseObject> objects, Exception e) {
                assertNull(e);
                assertEquals(TOTAL_THINGS, objects.size());
                done.release();
            }
        });

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testGetItemViewWithTextKey() {
        ParseQueryPagerAdapter<ParseObject> adapter =
                new ParseQueryPagerAdapter<>(activity, Thing.class);
        adapter.setTextKey("name");

        Fragment fragment = adapter.getFragment(savedThings.get(0));
        View view = fragment.onCreateView(null, null, null);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);

        assertEquals("Thing 0", textView.getText());
    }

    public void testGetItemViewWithCustomLayout() {
        ParseQueryPagerAdapter<ParseObject> adapter =
                new ParseQueryPagerAdapter<>(activity, Thing.class, R.layout.view_item);
        adapter.setTextKey("name");

        Fragment fragment = adapter.getFragment(savedThings.get(0));
        //Direcly call onCreateView for fragment, to mock regular implementation.
        //LayoutInflator, ViewGroup, and Bundle are not used for defaultView
        View view = fragment.onCreateView(null, null, null);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        assertEquals("Thing 0", textView.getText());

        // We should have inflated our own layout for the items.
        assertNotNull(view.findViewById(android.R.id.message));
    }

    public void testGetItemViewWithNoTextKey() throws ParseException {
        ParseQueryPagerAdapter<ParseObject> adapter =
                new ParseQueryPagerAdapter<>(activity, Thing.class);

        Fragment fragment = adapter.getFragment(savedThings.get(0));
        View view = fragment.onCreateView(null, null, null);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);

        // Since we do not set the textKey, we should display objectId
        assertEquals(savedThings.get(0).getObjectId(), textView.getText());
    }

    public void testLoadObjectsWithLimitsObjectsPerPage() throws Exception {
        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        final int pageSize = 4;
        adapter.setObjectsPerPage(pageSize);
        final Capture<Integer> timesThrough = new Capture<>(0);
        final Semaphore done = new Semaphore(0);
        final OnQueryLoadListener<Thing> listener = new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                if (e != null) {
                    return;
                }

                switch (timesThrough.get()) {
                    case 0:
                        // first time through, should have one page of results + "Load more"
                        assertEquals(pageSize, objects.size());
                        assertEquals(pageSize + 1, adapter.getCount());
                        adapter.loadNextPage();
                        break;
                    case 1:
                        // second time through, should have two pages of results + "Load more"
                        assertEquals(pageSize, objects.size());
                        assertEquals(2 * pageSize + 1, adapter.getCount());
                        adapter.loadNextPage();
                        break;
                    case 2:
                        // last time through, no "Load more" necessary.
                        assertEquals(TOTAL_THINGS - 2 * pageSize, objects.size());
                        assertEquals(TOTAL_THINGS, adapter.getCount());
                        done.release();
                }
                timesThrough.set(timesThrough.get() + 1);
            }
        };
        adapter.addOnQueryLoadListener(listener);

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testLoadObjectsWithLimitsObjectsPerPageAndNoRemainder() throws Exception {
        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        final int pageSize = 5;
        adapter.setObjectsPerPage(pageSize);
        final Capture<Integer> timesThrough = new Capture<>(0);
        final Semaphore done = new Semaphore(0);
        final OnQueryLoadListener<Thing> listener = new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                if (e != null) {
                    return;
                }

                switch (timesThrough.get()) {
                    case 0:
                        // first time through, should have one page of results + "Load more" fragment
                        assertEquals(pageSize, objects.size());
                        assertEquals(pageSize + 1, adapter.getCount());
                        adapter.loadNextPage();
                        break;
                    case 1:
                        // second time through, should have two pages' worth of results. It should realize that an
                        // additional "Load more" link isn't necessary, since this second page covers all of the
                        // results.
                        assertEquals(TOTAL_THINGS - pageSize, objects.size());
                        assertEquals(TOTAL_THINGS, adapter.getCount());
                        done.release();
                }
                timesThrough.set(timesThrough.get() + 1);
            }
        };
        adapter.addOnQueryLoadListener(listener);

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testLoadObjectsWithPaginationNextPageView() throws Exception {
        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        final int pageSize = 5;
        adapter.setObjectsPerPage(pageSize);
        final Capture<Integer> timesThrough = new Capture<>(0);
        final Semaphore done = new Semaphore(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                if (e != null) {
                    return;
                }

                switch (timesThrough.get()) {
                    case 0:
                        assertEquals(pageSize, objects.size());
                        assertEquals(pageSize + 1, adapter.getCount());

                        // Get Next Page fragment by passing in pageSize as the index
                        Fragment fragment = adapter.getItem(pageSize);
                        View view = fragment.onCreateView(null, null, null);
                        TextView textView = (TextView) view.findViewById(android.R.id.text1);
                        assertEquals("Load more...", textView.getText());
                        // Fragment's root view should have OnClickListener attached. In API level 15+, we could call
                        // view.hasOnClickListeners() instead.
                        assertTrue(view.performClick());
                        break;
                    case 1:
                        // Triggered by the performClick() call
                        done.release();
                }
                timesThrough.set(timesThrough.get() + 1);
            }
        });

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testLoadObjectsWithNoPagination() throws Exception {
        final int additional = 16;
        for (int i = 0; i < additional; i++) {
            ParseObject thing = ParseObject.create(Thing.class);
            thing.put("name", "Additional Thing " + i);
            savedThings.add(thing);
        }
        TOTAL_THINGS += additional;

        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        adapter.setPaginationEnabled(false);
        final Semaphore done = new Semaphore(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                assertNull(e);
                assertEquals(TOTAL_THINGS, objects.size());
                assertEquals(TOTAL_THINGS, adapter.getCount());
                done.release();
            }
        });

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testClear() throws Exception {
        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        final Semaphore done = new Semaphore(0);
        final Capture<Integer> counter = new Capture<>(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                if (e != null) {
                    return;
                }
                switch (counter.get()) {
                    case 0:
                        assertEquals(TOTAL_THINGS, objects.size());
                        assertEquals(TOTAL_THINGS, adapter.getCount());
                        adapter.clear();
                        assertEquals(0, adapter.getCount());
                        adapter.loadObjects();
                        break;
                    default:
                        assertEquals(TOTAL_THINGS, objects.size());
                        assertEquals(TOTAL_THINGS, adapter.getCount());
                        done.release();
                }
                counter.set(counter.get() + 1);
            }
        });

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testLoadObjectsWithCacheThenNetworkQueryAndPagination() throws Exception {
        QueryFactory<Thing> factory = new QueryFactory<Thing>() {
            @Override
            public ParseQuery<Thing> create() {
                ParseQuery<Thing> query = new ParseQuery<Thing>(Thing.class);
                query.setCachePolicy(CachePolicy.CACHE_THEN_NETWORK);
                return query;
            }
        };

        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, factory);
        final int pageSize = 5;
        adapter.setObjectsPerPage(pageSize);
        adapter.setPaginationEnabled(true);
        final Capture<Integer> timesThrough = new Capture<>(0);
        final Semaphore done = new Semaphore(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                if (e != null) {
                    return;
                }

                switch (timesThrough.get()) {
                    case 0:
                        // Network callback for first page
                        assertEquals(pageSize, objects.size());
                        assertEquals(pageSize + 1, adapter.getCount());
                        adapter.loadNextPage();
                        break;
                    case 1:
                        // Network callback for second page
                        assertEquals(TOTAL_THINGS - pageSize, objects.size());
                        assertEquals(TOTAL_THINGS, adapter.getCount());
                        adapter.loadObjects();
                        break;
                    case 2:
                        // Cache callback for first page
                        assertEquals(pageSize, objects.size());
                        assertEquals(pageSize + 1, adapter.getCount());
                        break;
                    case 3:
                        // Network callback for first page
                        assertEquals(pageSize, objects.size());
                        assertEquals(pageSize + 1, adapter.getCount());
                        adapter.loadNextPage();
                        break;
                    case 4:
                        // Cache callback for second page
                        assertEquals(TOTAL_THINGS - pageSize, objects.size());
                        assertEquals(TOTAL_THINGS, adapter.getCount());
                        break;
                    case 5:
                        // Network callback for second page
                        assertEquals(TOTAL_THINGS - pageSize, objects.size());
                        assertEquals(TOTAL_THINGS, adapter.getCount());
                        done.release();
                        break;
                }
                timesThrough.set(timesThrough.get() + 1);
            }
        });

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testLoadObjectsWithOnLoadingAndOnLoadedCallback() throws Exception {
        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        adapter.setObjectsPerPage(5);
        final Capture<Boolean> flag = new Capture<>(false);
        final Semaphore done = new Semaphore(0);

        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
                assertFalse(flag.get());
                flag.set(true);
                assertEquals(0, adapter.getCount());
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                assertTrue(flag.get());
                assertEquals(5, objects.size());
                done.release();
            }
        });

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testLoadNextPageBeforeLoadObjects() throws Exception {
        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        final Semaphore done = new Semaphore(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                assertNull(e);
                assertEquals(TOTAL_THINGS, objects.size());
                done.release();
            }
        });

        adapter.loadNextPage();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testIncomingQueryResultAfterClearing() throws Exception {
        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        final int pageSize = 4;
        adapter.setObjectsPerPage(pageSize);
        final Semaphore done = new Semaphore(0);
        final Capture<Integer> timesThrough = new Capture<>(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {}

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                switch (timesThrough.get()) {
                    case 0:
                        adapter.loadNextPage();
                        adapter.clear();
                    case 1:
                        done.release();
                }
                timesThrough.set(timesThrough.get() + 1);
            }
        });

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testLoadObjectsWithOverrideSetPageOnQuery() throws Exception {
        final int arbitraryLimit = 3;
        final ParseQueryPagerAdapter<Thing> adapter =
                new ParseQueryPagerAdapter<Thing>(activity, Thing.class) {
                    @Override
                    public void setPageOnQuery(int page, ParseQuery<Thing> query) {
                        // Make sure that this method is being used + respected.
                        query.setLimit(arbitraryLimit);
                    }
                };
        final Semaphore done = new Semaphore(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
            }

            ;

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                assertEquals(arbitraryLimit, objects.size());
                done.release();
            }
        });

        adapter.loadObjects();

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testLoadObjectsWithtAutoload() throws Exception {
        final ParseQueryPagerAdapter<Thing> adapter = new ParseQueryPagerAdapter<>(activity, Thing.class);
        final Capture<Boolean> flag = new Capture<>(false);
        // Make sure that the Adapter doesn't start trying to load objects until AFTER we set this flag
        // to true (= triggered by calling setAutoload, NOT registerDataSetObserver, if autoload is
        // false).
        adapter.setAutoload(false);
        final Semaphore done = new Semaphore(0);
        adapter.addOnQueryLoadListener(new OnQueryLoadListener<Thing>() {
            @Override
            public void onLoading() {
                assertEquals(0, adapter.getCount());
                assertTrue(flag.get());
            }

            @Override
            public void onLoaded(List<Thing> objects, Exception e) {
                assertEquals(TOTAL_THINGS, adapter.getCount());
                done.release();
            }
        });
        DataSetObserver observer = new DataSetObserver() { };
        adapter.registerDataSetObserver(observer);
        flag.set(true);
        adapter.setAutoload(true);

        // Make sure we assert in callback is executed
        assertTrue(done.tryAcquire(10, TimeUnit.SECONDS));
    }
}
