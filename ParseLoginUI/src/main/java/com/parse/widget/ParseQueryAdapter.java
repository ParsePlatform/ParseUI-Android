/*
 *  Copyright (c) 2015, Parse, LLC. All rights reserved.
 *
 *  You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 *  copy, modify, and distribute this software in source code or binary form for use
 *  in connection with the web services and APIs provided by Parse.
 *
 *  As with any software that integrates with the Parse platform, your use of
 *  this software is subject to the Parse Terms of Service
 *  [https://www.parse.com/about/terms]. This copyright notice shall be
 *  included in all copies or substantial portions of the software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.parse.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import bolts.Capture;

/**
 * A {@code ParseQueryAdapter} handles the fetching of objects by page, and displaying
 * objects as views in a {@link android.support.v7.widget.RecyclerView}.
 * <p/>
 * This class is highly configurable, but also intended to be easy to get started with. See below
 * for an example of using a {@code ParseQueryAdapter} inside an
 * {@link android.app.Activity}'s {@code onCreate}:
 * <pre>
 * final ParseQueryAdapter adapter
 *         = new ParseQueryAdapter(this, &quot;TestObject&quot;);
 * adapter.setTextKey(&quot;name&quot;);
 *
 * RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
 * recyclerView.setAdapter(adapter);
 * recyclerView.setLayoutManager(new LinearLayoutManager(this));
 *
 * adapter.loadObjects();
 * </pre>
 * <p/>
 * Below, an example showing off the level of configuration available with this class:
 * <pre>
 * // Instantiate a QueryFactory to define the ParseQuery to be used for fetching items in this
 * // Adapter.
 * ParseQueryAdapter.QueryFactory&lt;ParseObject&gt; factory =
 *     new ParseQueryAdapter.QueryFactory&lt;ParseObject&gt;() {
 *       public ParseQuery create() {
 *         ParseQuery query = new ParseQuery(&quot;Customer&quot;);
 *         query.whereEqualTo(&quot;activated&quot;, true);
 *         query.orderByDescending(&quot;moneySpent&quot;);
 *         return query;
 *       }
 *     };
 *
 * // Pass the factory into the ParseQueryAdapter's constructor.
 * ParseQueryAdapter&lt;ParseObject&gt; adapter
 *         = new ParseQueryAdapter&lt;ParseObject&gt;(this, factory);
 * adapter.setTextKey(&quot;name&quot;);
 *
 * // Perhaps set a callback to be fired upon successful loading of a new set of ParseObjects.
 * adapter.addOnQueryLoadListener(new OnQueryLoadListener&lt;ParseObject&gt;() {
 *   public void onLoading() {
 *     // Trigger any &quot;loading&quot; UI
 *   }
 *
 *   public void onLoaded(List&lt;ParseObject&gt; objects, ParseException e) {
 *     // Execute any post-loading logic, hide &quot;loading&quot; UI
 *   }
 * });
 *
 * </pre>
 */
public class ParseQueryAdapter<T extends ParseObject> extends RecyclerView.Adapter<ParseQueryAdapter.ViewHolder> {

  /**
   * Implement to construct your own custom {@link ParseQuery} for fetching objects.
   */
  public interface QueryFactory<T extends ParseObject> {
    ParseQuery<T> create();
  }

  /**
   * Implement with logic that is called before and after objects are fetched from Parse by the
   * adapter.
   */
  public interface OnQueryLoadListener<T extends ParseObject> {
    void onLoading();

    void onLoaded(List<T> objects, Exception e);
  }

  /**
   * OnClickListener.
   */
  public interface OnClickListener<T extends ParseObject> {
    void onClick(T item, int position);
  }

  /**
   * ViewHolder.
   */
  public class ViewHolder extends RecyclerView.ViewHolder {
    private TextView textView;
    private ParseImageView imageView;

    public ViewHolder(View itemView) {
      super(itemView);

      try {
        textView = (TextView) itemView.findViewById(android.R.id.text1);
      } catch (ClassCastException ex) {
        throw new IllegalStateException(
                "Your object views must have a TextView whose id attribute is 'android.R.id.text1'", ex);
      }
      try {
        imageView = (ParseImageView) itemView.findViewById(android.R.id.icon);
      } catch (ClassCastException ex) {
        throw new IllegalStateException(
                "Your object views must have a ParseImageView whose id attribute is 'android.R.id.icon'", ex);
      }
    }

    public void bind(final T object, final int position) {
      if (textKey == null) {
        textView.setText(object.getObjectId());
      } else if (object.get(textKey) != null) {
        textView.setText(object.get(textKey).toString());
      } else {
        textView.setText(null);
      }

      if (imageKey != null) {
        if (imageView == null) {
          throw new IllegalStateException(
                  "Your object views must have a ParseImageView whose id attribute is 'android.R.id.icon' if an imageKey is specified");
        }
        if (!imageViewSet.containsKey(imageView)) {
          imageViewSet.put(imageView, null);
        }
        imageView.setPlaceholder(placeholder);
        imageView.setParseFile((ParseFile) object.get(imageKey));
        imageView.loadInBackground();
      }
      textView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (onClickListener != null) {
            onClickListener.onClick(object, position);
          }
        }
      });
    }

    public void setNextView() {
      textView.setText("Load more...");
      if (imageView != null) {
        imageView.setVisibility(View.GONE);
      }
      textView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          loadNextPage();
        }
      });
    }

    public TextView getTextView() {
      return textView;
    }

    public ParseImageView getImageView() {
      return imageView;
    }
  }

  // The key to use to display on the cell text label.
  private String textKey;

  // The key to use to fetch an image for display in the cell's image view.
  private String imageKey;

  // The number of objects to show per page (default: 25)
  private int objectsPerPage = 25;

  // Whether the table should use the built-in pagination feature (default:
  // true)
  private boolean paginationEnabled = true;

  // A Drawable placeholder, to be set on ParseImageViews while images are loading. Can be null.
  private Drawable placeholder;

  // A WeakHashMap, holding references to ParseImageViews that have been configured.
  // Accessed and iterated over if setPlaceholder(Drawable) is called after some set of
  // ParseImageViews have already been instantiated and configured.
  private WeakHashMap<ParseImageView, Void> imageViewSet = new WeakHashMap<>();

  // A WeakHashMap, keeping track of the DataSetObservers on this class
  private WeakHashMap<RecyclerView.AdapterDataObserver, Void> dataSetObservers = new WeakHashMap<>();

  // Whether the adapter should trigger loadObjects() on registerDataSetObserver(); Defaults to
  // true.
  private boolean autoload = true;

  private Context context;

  private List<T> objects = new ArrayList<>();

  private Set<ParseQuery> runningQueries = Collections.newSetFromMap(new ConcurrentHashMap<ParseQuery, Boolean>());

  // Used to keep track of the pages of objects when using CACHE_THEN_NETWORK. When using this,
  // the data will be flattened and put into the objects list.
  private List<List<T>> objectPages = new ArrayList<>();

  private int currentPage = 0;

  private int itemResourceId;

  private boolean hasNextPage = true;

  private QueryFactory<T> queryFactory;

  private OnClickListener<T> onClickListener;

  private List<OnQueryLoadListener<T>> onQueryLoadListeners = new ArrayList<>();

  /**
   * Constructs a {@code ParseQueryAdapter}. Given a {@link ParseObject} subclass,
   * this adapter will fetch and display all {@link ParseObject}s of the specified class,
   * ordered by creation time.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param clazz
   *          The {@link ParseObject} subclass type to fetch and display.
   */
  public ParseQueryAdapter(Context context, Class<? extends ParseObject> clazz) {
    this(context, getClassName(clazz));
  }

  /**
   * Constructs a {@code ParseQueryAdapter}. Given a {@link ParseObject} subclass,
   * this adapter will fetch and display all {@link ParseObject}s of the specified class, ordered
   * by creation time.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param className
   *          The name of the Parse class of {@link ParseObject}s to display.
   */
  public ParseQueryAdapter(Context context, final String className) {
    this(context, new QueryFactory<T>() {
      @Override
      public ParseQuery<T> create() {
        ParseQuery<T> query = ParseQuery.getQuery(className);
        query.orderByDescending("createdAt");

        return query;
      }
    });

    if (className == null) {
      throw new RuntimeException("You need to specify a className for the ParseQueryAdapter");
    }
  }

  /**
   * Constructs a {@code ParseQueryAdapter}. Given a {@link ParseObject} subclass,
   * this adapter will fetch and display all {@link ParseObject}s of the specified class, ordered
   * by creation time.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param clazz
   *          The {@link ParseObject} subclass type to fetch and display.
   * @param itemViewResource
   *        A resource id that represents the layout for an item in the AdapterView.
   */
  public ParseQueryAdapter(
          Context context, Class<? extends ParseObject> clazz, int itemViewResource) {
    this(context, getClassName(clazz), itemViewResource);
  }

  /**
   * Constructs a {@code ParseQueryAdapter}. Given a {@link ParseObject} subclass,
   * this adapter will fetch and display all {@link ParseObject}s of the specified class, ordered
   * by creation time.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param className
   *          The name of the Parse class of {@link ParseObject}s to display.
   * @param itemViewResource
   *        A resource id that represents the layout for an item in the AdapterView.
   */
  public ParseQueryAdapter(
          Context context, final String className, int itemViewResource) {
    this(context, new QueryFactory<T>() {
      @Override
      public ParseQuery<T> create() {
        ParseQuery<T> query = ParseQuery.getQuery(className);
        query.orderByDescending("createdAt");

        return query;
      }
    }, itemViewResource);

    if (className == null) {
      throw new RuntimeException(
              "You need to specify a className for the ParseQueryAdapter");
    }
  }
  /**
   * Constructs a {@code ParseQueryAdapter}. Allows the caller to define further
   * constraints on the {@link ParseQuery} to be used when fetching items from Parse.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param queryFactory
   *          A {@link QueryFactory} to build a {@link ParseQuery} for fetching objects.
   */
  public ParseQueryAdapter(Context context, QueryFactory<T> queryFactory) {
    this(context, queryFactory, -1);
  }

  /**
   * Constructs a {@code ParseQueryAdapter}. Allows the caller to define further
   * constraints on the {@link ParseQuery} to be used when fetching items from Parse.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param queryFactory
   *          A {@link QueryFactory} to build a {@link ParseQuery} for fetching objects.
   * @param itemViewResource
   *          A resource id (>0) that represents the layout for an item in the AdapterView.
   */
  public ParseQueryAdapter(
          Context context, QueryFactory<T> queryFactory, int itemViewResource) {
    super();
    this.context = context;
    this.queryFactory = queryFactory;
    this.itemResourceId = itemViewResource;
  }

  /**
   * Return the context provided by the {@code Activity} utilizing this {@code ParseQueryAdapter}.
   *
   * @return The activity utilizing this adapter.
   */
  public Context getContext() {
    return this.context;
  }

  public void clear() {
    objectPages.clear();
    cancelAllQueries();
    syncObjectsWithPages();
    notifyDataSetChanged();
    currentPage = 0;
  }

  private void cancelAllQueries() {
    for (ParseQuery q : runningQueries) {
      q.cancel();
    }
    runningQueries.clear();
  }

  /**
   * Clears the table and loads the first page of objects asynchronously. This method is called
   * automatically when this {@code Adapter} is attached to an {@code AdapterView}.
   * <p/>
   * {@code loadObjects()} should only need to be called if {@link #setAutoload(boolean)} is set to
   * {@code false}.
   */
  public void loadObjects() {
    loadObjects(0, true);
  }

  private void loadObjects(final int page, final boolean shouldClear) {
    final ParseQuery<T> query = queryFactory.create();

    if (objectsPerPage > 0 && paginationEnabled) {
      setPageOnQuery(page, query);
    }

    notifyOnLoadingListeners();

    // Create a new page
    if (page >= objectPages.size()) {
      objectPages.add(page, new ArrayList<T>());
    }

    // In the case of CACHE_THEN_NETWORK, two callbacks will be called. Using this flag to keep track,
    final Capture<Boolean> firstCallBack = new Capture<>(true);

    runningQueries.add(query);

    // TODO convert to Tasks and CancellationTokens
    // (depends on https://github.com/ParsePlatform/Parse-SDK-Android/issues/6)
    query.findInBackground(new FindCallback<T>() {
      @Override
      public void done(List<T> foundObjects, ParseException e) {
        if (!runningQueries.contains(query)) {
          return;
        }
        // In the case of CACHE_THEN_NETWORK, two callbacks will be called. We can only remove the
        // query after the second callback.
        if (query.getCachePolicy() != ParseQuery.CachePolicy.CACHE_THEN_NETWORK ||
                (query.getCachePolicy() == ParseQuery.CachePolicy.CACHE_THEN_NETWORK && !firstCallBack.get())) {
          runningQueries.remove(query);
        }
        try {
          if (query.getCachePolicy() == ParseQuery.CachePolicy.CACHE_ONLY
                  && e != null && e.getCode() == ParseException.CACHE_MISS) {
            // no-op on cache miss
            return;
          }
        } catch (IllegalStateException ignore) {
          // LocaleDatastore disabled
        }

        if ((e != null) && ((e.getCode() == ParseException.CONNECTION_FAILED) || (e.getCode() != ParseException.CACHE_MISS))) {
          hasNextPage = true;
        } else if (foundObjects != null) {
          if (shouldClear && firstCallBack.get()) {
            runningQueries.remove(query);
            cancelAllQueries();
            runningQueries.add(query); // allow 2nd callback
            objectPages.clear();
            objectPages.add(new ArrayList<T>());
            currentPage = page;
            firstCallBack.set(false);
          }

          // Only advance the page, this prevents second call back from CACHE_THEN_NETWORK to
          // reset the page.
          if (page >= currentPage) {
            currentPage = page;

            // since we set limit == objectsPerPage + 1
            hasNextPage = (foundObjects.size() > objectsPerPage);
          }

          if (paginationEnabled && foundObjects.size() > objectsPerPage) {
            // Remove the last object, fetched in order to tell us whether there was a "next page"
            foundObjects.remove(objectsPerPage);
          }

          List<T> currentPage = objectPages.get(page);
          currentPage.clear();
          currentPage.addAll(foundObjects);

          syncObjectsWithPages();

          // executes on the UI thread
          notifyDataSetChanged();
        }

        notifyOnLoadedListeners(foundObjects, e);
      }
    });
  }

  /**
   * Loads the next page of objects, appends to table, and notifies the UI that the model has
   * changed.
   */
  public void loadNextPage() {
    if (objects.size() == 0 && runningQueries.size() == 0) {
      loadObjects(0, false);
    }
    else {
      loadObjects(currentPage + 1, false);
    }
  }

  private View getDefaultView() {
    if (this.itemResourceId != -1) {
      return View.inflate(context, itemResourceId, null);
    }
    LinearLayout view = new LinearLayout(context);
    view.setPadding(8, 4, 8, 4);

    ParseImageView imageView = new ParseImageView(context);
    imageView.setId(android.R.id.icon);
    imageView.setLayoutParams(new LinearLayout.LayoutParams(50, 50));
    view.addView(imageView);

    TextView textView = new TextView(context);
    textView.setId(android.R.id.text1);
    textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
    textView.setPadding(8, 0, 0, 0);
    view.addView(textView);

    return view;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
    return new ViewHolder(getDefaultView());
  }

  @Override
  public void onBindViewHolder(ParseQueryAdapter.ViewHolder viewHolder, final int position) {
    if (position < objects.size()) {
      final T object = objects.get(position);
      viewHolder.bind(object, position);
    }
    else if (position == objects.size()) {
      viewHolder.setNextView();
    }
    else {
      throw new RuntimeException();
    }
  }

  @Override
  public int getItemCount() {
    int count = objects.size();

    if (shouldShowPaginationCell()) {
      count++;
    }

    return count;
  }

  @Override
  public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
    super.registerAdapterDataObserver(observer);
    dataSetObservers.put(observer, null);
    if (autoload) {
      loadObjects();
    }
  }

  @Override
  public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
    super.unregisterAdapterDataObserver(observer);
    this.dataSetObservers.remove(observer);
  }


  /**
   * This is a helper function to sync the objects with objectPages. This is only used with the
   * CACHE_THEN_NETWORK option.
   */
  private void syncObjectsWithPages() {
    objects.clear();
    for (List<T> pageOfObjects : objectPages) {
      objects.addAll(pageOfObjects);
    }
  }

  private int getPaginationCellRow() {
    return objects.size();
  }

  private boolean shouldShowPaginationCell() {
    return paginationEnabled && objects.size() > 0 && hasNextPage;
  }

  private void notifyOnLoadingListeners() {
    for (OnQueryLoadListener<T> listener : onQueryLoadListeners) {
      listener.onLoading();
    }
  }

  private void notifyOnLoadedListeners(List<T> objects, Exception e) {
    for (OnQueryLoadListener<T> listener : onQueryLoadListeners) {
      listener.onLoaded(objects, e);
    }
  }

  /**
   * Override this method to manually paginate the provided {@code ParseQuery}. By default, this
   * method will set the {@code limit} value to {@link #getObjectsPerPage()} and the {@code skip}
   * value to {@link #getObjectsPerPage()} * {@code page}.
   * <p/>
   * Overriding this method will not be necessary, in most cases.
   *
   * @param page
   *          the page number of results to fetch from Parse.
   * @param query
   *          the {@link ParseQuery} used to fetch items from Parse. This query will be mutated and
   *          used in its mutated form.
   */
  protected void setPageOnQuery(int page, ParseQuery<T> query) {
    query.setLimit(objectsPerPage + 1);
    query.setSkip(page * objectsPerPage);
  }

  public void setTextKey(String textKey) {
    this.textKey = textKey;
  }

  public void setImageKey(String imageKey) {
    this.imageKey = imageKey;
  }

  public void setObjectsPerPage(int objectsPerPage) {
    this.objectsPerPage = objectsPerPage;
  }

  public int getObjectsPerPage() {
    return objectsPerPage;
  }

  /**
   * Enable or disable pagination of results. Defaults to true.
   *
   * @param paginationEnabled
   *          Defaults to true.
   */
  public void setPaginationEnabled(boolean paginationEnabled) {
    this.paginationEnabled = paginationEnabled;
  }

  /**
   * Sets a placeholder image to be used when fetching data for each item in the {@code AdapterView}
   * . Will not be used if {@link #setImageKey(String)} was not used to define which images to
   * display.
   *
   * @param placeholder
   *          A {@code Drawable} to be displayed while the remote image data is being fetched. This
   *          value can be null, and {@code ImageView}s in this AdapterView will simply be blank
   *          while data is being fetched.
   */
  public void setPlaceholder(Drawable placeholder) {
    if (this.placeholder == placeholder) {
      return;
    }
    this.placeholder = placeholder;
    Iterator<ParseImageView> iter = imageViewSet.keySet().iterator();
    ParseImageView imageView;
    while (iter.hasNext()) {
      imageView = iter.next();
      if (imageView != null) {
        imageView.setPlaceholder(this.placeholder);
      }
    }
  }

  public void setOnClickListener(OnClickListener onClickListener) {
    this.onClickListener = onClickListener;
  }

  /**
   * Enable or disable the automatic loading of results upon attachment to an {@code AdapterView}.
   * Defaults to true.
   *
   * @param autoload
   *          Defaults to true.
   */
  public void setAutoload(boolean autoload) {
    if (this.autoload == autoload) {
      // An extra precaution to prevent an overzealous setAutoload(true) after assignment to an
      // AdapterView from triggering an unnecessary additional loadObjects().
      return;
    }
    this.autoload = autoload;
    if (autoload && !dataSetObservers.isEmpty() && objects.isEmpty()) {
      loadObjects();
    }
  }

  public void addOnQueryLoadListener(OnQueryLoadListener<T> listener) {
    onQueryLoadListeners.add(listener);
  }

  public void removeOnQueryLoadListener(OnQueryLoadListener<T> listener) {
    onQueryLoadListeners.remove(listener);
  }

  /**
   * Gets the class name based on the {@link ParseClassName} annotation associated with a class.
   *
   * @param clazz
   *          The class to inspect.
   * @return The name of the Parse class, if one is provided. Otherwise, {@code null}.
   */
  private static String getClassName(Class<? extends ParseObject> clazz) {
    ParseClassName info = clazz.getAnnotation(ParseClassName.class);
    if (info == null) {
      return null;
    }
    return info.value();
  }
}
