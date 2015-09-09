package com.parse;

import com.parse.ui.TestActivity;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by lukask on 9/9/15.
 */
public class ParseQueryRecyclerViewAdapterTest extends BaseActivityInstrumentationTestCase2<TestActivity> {

  @ParseClassName("Thing")
  public static class Thing extends ParseObject {
    public Thing() {
    }
  }

  public ParseQueryRecyclerViewAdapterTest() {
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
        if (state.cachePolicy() == ParseQuery.CachePolicy.CACHE_ONLY) {
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
}
