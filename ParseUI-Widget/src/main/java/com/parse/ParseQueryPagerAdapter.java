package com.parse;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import bolts.Capture;

/**
 * A {@code ParseQueryPagerAdapter} handles the fetching of objects by page, and displaying objects as
 * fragments in a {@link android.app.Fragment}.
 * <p/>
 * This class is highly configurable, but also intended to be easy to get started with. See below
 * for an example of using a {@code ParseQueryPagerAdapter} inside an {@link android.app.Activity}'s
 * {@code onCreate}:
 * <pre>
 * final ParseQueryPagerAdapter adapter = new ParseQueryPagerAdapter(this, &quot;TestObject&quot;);
 * adapter.setTextKey(&quot;name&quot;);
 *
 * ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
 * viewPager.setAdapter(adapter);
 * </pre>
 * <p/>
 * Below, an example showing off the level of configuration available with this class:
 * <pre>
 * // Instantiate a QueryFactory to define the ParseQuery to be used for fetching items in this
 * // Adapter.
 * ParseQueryPagerAdapter.QueryFactory&lt;ParseObject&gt; factory =
 *     new ParseQueryPagerAdapter.QueryFactory&lt;ParseObject&gt;() {
 *       public ParseQuery create() {
 *         ParseQuery query = new ParseQuery(&quot;Customer&quot;);
 *         query.whereEqualTo(&quot;activated&quot;, true);
 *         query.orderByDescending(&quot;moneySpent&quot;);
 *         return query;
 *       }
 *     };
 *
 * // Pass the factory into the ParseQueryPagerAdapter's constructor.
 * ParseQueryPagerAdapter&lt;ParseObject&gt; adapter = new ParseQueryPagerAdapter&lt;ParseObject&gt;(this, factory);
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
 * // Attach it to your ViewPager, as in the example above
 * ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
 * viewpager.setAdapter(adapter);
 * </pre>
 *
 * Modification by Pablo Baxter (Github: pablobaxter)
 *
 */
public class ParseQueryPagerAdapter<T extends ParseObject> extends FragmentStatePagerAdapter {

    /**
     * Implement to construct your own custom {@link ParseQuery} for fetching objects.
     */
    public static interface QueryFactory<T extends ParseObject> {
        public ParseQuery<T> create();
    }

    /**
     * Implement with logic that is called before and after objects are fetched from Parse by the
     * adapter.
     */
    public static interface OnQueryLoadListener<T extends ParseObject> {
        public void onLoading();

        public void onLoaded(List<T> objects, Exception e);
    }

    // The key to use to display on the fragment text label.
    private String textKey;

    // The key to use to fetch an image for display in the viewpager's image view.
    private String imageKey;

    // The number of objects to retrieve per page (default: 25)
    private int objectsPerPage = 25;

    // Whether the viewpager should use the built-in pagination feature (default:
    // true)
    private boolean paginationEnabled = true;

    // A Drawable placeholder, to be set on ParseImageViews while images are loading. Can be null.
    private Drawable placeholder;

    // A WeakHashMap, holding references to ParseImageViews that have been configured by this PQA.
    // Accessed and iterated over if setPlaceholder(Drawable) is called after some set of
    // ParseImageViews have already been instantiated and configured.
    private WeakHashMap<ParseImageView, Void> imageViewSet = new WeakHashMap<>();

    // A WeakHashMap, keeping track of the DataSetObservers on this class
    private WeakHashMap<DataSetObserver, Void> dataSetObservers = new WeakHashMap<>();

    // Whether the adapter should trigger loadObjects() on registerDataSetObserver(); Defaults to
    // true.
    private boolean autoload = true;

    //The activity that will provide the necessary FragmentManager
    private Activity activity;

    private List<T> objects = new ArrayList<>();

    private Set<ParseQuery> runningQueries =
            Collections.newSetFromMap(new ConcurrentHashMap<ParseQuery, Boolean>());


    // Used to keep track of the pages of objects when using CACHE_THEN_NETWORK. When using this,
    // the data will be flattened and put into the objects list.
    private List<List<T>> objectPages = new ArrayList<>();

    private int currentPage = 0;

    private Integer itemResourceId;

    private boolean hasNextPage = true;

    //Holds the 'next page' fragment, in order to refresh it after next page has loaded.
    private Fragment nextPageFragment;

    private QueryFactory<T> queryFactory;

    private List<OnQueryLoadListener<T>> onQueryLoadListeners =
            new ArrayList<>();

    /**
     * Constructs a {@code ParseQueryPagerAdapter}. Given a {@link ParseObject} subclass, this adapter will
     * fetch and display all {@link ParseObject}s of the specified class, ordered by creation time.
     *
     * @param activity
     *          The activity utilizing this adapter.
     * @param clazz
     *          The {@link ParseObject} subclass type to fetch and display.
     */
    public ParseQueryPagerAdapter(Activity activity, Class<? extends ParseObject> clazz) {
        this(activity, ParseObject.getClassName(clazz));
    }

    /**
     * Constructs a {@code ParseQueryPagerAdapter}. Given a {@link ParseObject} subclass, this adapter will
     * fetch and display all {@link ParseObject}s of the specified class, ordered by creation time.
     *
     * @param activity
     *          The activity utilizing this adapter.
     * @param className
     *          The name of the Parse class of {@link ParseObject}s to display.
     */
    public ParseQueryPagerAdapter(Activity activity, final String className) {
        this(activity, new QueryFactory<T>() {
            @Override
            public ParseQuery<T> create() {
                ParseQuery<T> query = ParseQuery.getQuery(className);
                query.orderByDescending("createdAt");

                return query;
            }
        });

        if (className == null) {
            throw new RuntimeException("You need to specify a className for the ParseQueryPagerAdapter");
        }
    }

    /**
     * Constructs a {@code ParseQueryPagerAdapter}. Given a {@link ParseObject} subclass, this adapter will
     * fetch and display all {@link ParseObject}s of the specified class, ordered by creation time.
     *
     * @param activity
     *          The activity utilizing this adapter.
     * @param clazz
     *          The {@link ParseObject} subclass type to fetch and display.
     * @param itemViewResource
     *        A resource id that represents the layout for an item in the AdapterView.
     */
    public ParseQueryPagerAdapter(Activity activity, Class<? extends ParseObject> clazz,
                             int itemViewResource) {
        this(activity, ParseObject.getClassName(clazz), itemViewResource);
    }

    /**
     * Constructs a {@code ParseQueryPagerAdapter}. Given a {@link ParseObject} subclass, this adapter will
     * fetch and display all {@link ParseObject}s of the specified class, ordered by creation time.
     *
     * @param activity
     *          The activity utilizing this adapter.
     * @param className
     *          The name of the Parse class of {@link ParseObject}s to display.
     * @param itemViewResource
     *        A resource id that represents the layout for an item in the AdapterView.
     */
    public ParseQueryPagerAdapter(Activity activity, final String className, int itemViewResource) {
        this(activity, new QueryFactory<T>() {
            @Override
            public ParseQuery<T> create() {
                ParseQuery<T> query = ParseQuery.getQuery(className);
                query.orderByDescending("createdAt");

                return query;
            }
        }, itemViewResource);

        if (className == null) {
            throw new RuntimeException("You need to specify a className for the ParseQueryPagerAdapter");
        }
    }

    /**
     * Constructs a {@code ParseQueryPagerAdapter}. Allows the caller to define further constraints on the
     * {@link ParseQuery} to be used when fetching items from Parse.
     *
     * @param activity
     *          The activity utilizing this adapter.
     * @param queryFactory
     *          A {@link QueryFactory} to build a {@link ParseQuery} for fetching objects.
     */
    public ParseQueryPagerAdapter(Activity activity, QueryFactory<T> queryFactory) {
        this(activity, queryFactory, null);
    }

    /**
     * Constructs a {@code ParseQueryPagerAdapter}. Allows the caller to define further constraints on the
     * {@link ParseQuery} to be used when fetching items from Parse.
     *
     * @param activity
     *          The activity utilizing this adapter.
     * @param queryFactory
     *          A {@link QueryFactory} to build a {@link ParseQuery} for fetching objects.
     * @param itemViewResource
     *          A resource id that represents the layout for an item in the AdapterView.
     */
    public ParseQueryPagerAdapter(Activity activity, QueryFactory<T> queryFactory, int itemViewResource) {
        this(activity, queryFactory, Integer.valueOf(itemViewResource));
    }

    private ParseQueryPagerAdapter(Activity activity, QueryFactory<T> queryFactory, Integer itemViewResource) {
        super(activity.getFragmentManager());
        this.activity = activity;
        this.queryFactory = queryFactory;
        this.itemResourceId = itemViewResource;
    }

    /**
     * Return the context provided by the {@code Activity} utilizing this {@code ParseQueryPagerAdapter}.
     *
     * @return The activity utilizing this adapter.
     */
    public Activity getActivity() {
        return this.activity;
    }

    /** {@inheritDoc} **/
    @Override
    public Fragment getItem(int index) {
        if (index == this.getPaginationFragment()) {
            nextPageFragment = getNextPageFragment();
            return nextPageFragment;
        }
        return getFragment(this.objects.get(index));
    }

    /** {@inheritDoc} **/
    @Override
    public int getItemPosition(Object object){
        Fragment fragment = object instanceof Fragment ? (Fragment)object : null;
        if(fragment == nextPageFragment){
            nextPageFragment = null;
            return POSITION_NONE;
        }
        return POSITION_UNCHANGED;
    }

    /**
     * Overrides {@link android.support.v4.view.PagerAdapter#getCount()} method to return the number of fragments to
     * display. If pagination is turned on, this count will include an extra +1 count for the
     * pagination fragment.
     *
     * @return The number of fragments to be displayed by the {@link android.support.v4.view.ViewPager}.
     */
    @Override
    public int getCount() {
        int count = this.objects.size();

        if (this.shouldShowPaginationFragment()) {
            count++;
        }

        return count;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        this.dataSetObservers.put(observer, null);
        if (this.autoload) {
            this.loadObjects();
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
        this.dataSetObservers.remove(observer);
    }

    /**
     * Remove all elements from the list.
     */
    public void clear() {
        this.objectPages.clear();
        cancelAllQueries();
        syncObjectsWithPages();
        this.notifyDataSetChanged();
        this.currentPage = 0;
    }

    private void cancelAllQueries() {
        for (ParseQuery q : runningQueries) {
            q.cancel();
        }
        runningQueries.clear();
    }

    /**
     * Clears the viewpager and loads the first page of objects asynchronously. This method is called
     * automatically when this {@code PagerAdapter} is attached to a {@code ViewPager}.
     * <p/>
     * {@code loadObjects()} should only need to be called if {@link #setAutoload(boolean)} is set to
     * {@code false}.
     */
    public void loadObjects() {
        this.loadObjects(0, true);
    }

    private void loadObjects(final int page, final boolean shouldClear) {
        final ParseQuery<T> query = this.queryFactory.create();

        if (this.objectsPerPage > 0 && this.paginationEnabled) {
            this.setPageOnQuery(page, query);
        }

        this.notifyOnLoadingListeners();

        // Create a new page
        if (page >= objectPages.size()) {
            objectPages.add(page, new ArrayList<T>());
        }

        // In the case of CACHE_THEN_NETWORK, two callbacks will be called. Using this flag to keep
        // track of the callbacks.
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
                if (Parse.isLocalDatastoreEnabled() ||
                        (query.getCachePolicy() != ParseQuery.CachePolicy.CACHE_THEN_NETWORK) ||
                        (query.getCachePolicy() == ParseQuery.CachePolicy.CACHE_THEN_NETWORK && !firstCallBack.get())) {
                    runningQueries.remove(query);
                }

                if ((!Parse.isLocalDatastoreEnabled() &&
                        query.getCachePolicy() == ParseQuery.CachePolicy.CACHE_ONLY) &&
                        (e != null) && e.getCode() == ParseException.CACHE_MISS) {
                    // no-op on cache miss
                    return;
                }

                if ((e != null) &&
                        ((e.getCode() == ParseException.CONNECTION_FAILED) ||
                                (e.getCode() != ParseException.CACHE_MISS))) {
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
     * This is a helper function to sync the objects with objectPages. This is only used with the
     * CACHE_THEN_NETWORK option.
     */
    private void syncObjectsWithPages() {
        objects.clear();
        for (List<T> pageOfObjects : objectPages) {
            objects.addAll(pageOfObjects);
        }
    }

    /**
     * Loads the next page of objects, appends to viewpager, and notifies the UI that the model has
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

    /**
     * Override this method to customize each fragment given a {@link ParseObject}.
     * <p/>
     * If a fragment is not provided, a default fragment will be created
     * <p/>
     * This method expects a {@code TextView} with id {@code android.R.id.text1} in your object views.
     * If {@link #setImageKey(String)} was used, this method also expects an {@code ImageView} with id
     * {@code android.R.id.icon}.
     * <p/>
     * This method displays the text value specified by the text key (set via
     * {@link #setTextKey(String)}) and an image (described by a {@link ParseFile}, under the key set
     * via {@link #setImageKey(String)}) if applicable. If the text key is not set, the value for
     * {@link ParseObject#getObjectId()} will be displayed instead.
     *
     * @param object
     *          The {@link ParseObject} associated with this item.
     * @return The customized fragment displaying the {@link ParseObject}'s information.
     */
    public Fragment getFragment(final T object){

        return new Fragment(){
            @Nullable
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View view = getDefaultView(activity);
                TextView textView;
                try {
                    textView = (TextView) view.findViewById(android.R.id.text1);
                } catch (ClassCastException ex) {
                    throw new IllegalStateException(
                            "Your object views must have a TextView whose id attribute " +
                                    "is 'android.R.id.text1'", ex);
                }

                if (textView != null) {
                    if (textKey == null) {
                        textView.setText(object.getObjectId());
                    } else if (object.get(textKey) != null) {
                        textView.setText(object.get(textKey).toString());
                    } else {
                        textView.setText(null);
                    }
                }

                if (imageKey != null) {
                    ParseImageView imageView;
                    try {
                        imageView = (ParseImageView) view.findViewById(android.R.id.icon);
                    } catch (ClassCastException ex) {
                        throw new IllegalStateException(
                                "Your object views must have a ParseImageView whose id attribute" +
                                        " is 'android.R.id.icon'",
                                ex);
                    }
                    if (imageView == null) {
                        throw new IllegalStateException(
                                "Your object views must have a ParseImageView whose id attribute" +
                                        " is 'android.R.id.icon' if an imageKey is specified");
                    }
                    if (!imageViewSet.containsKey(imageView)) {
                        imageViewSet.put(imageView, null);
                    }
                    imageView.setPlaceholder(placeholder);
                    imageView.setParseFile((ParseFile) object.get(imageKey));
                    imageView.loadInBackground();
                }
                return view;
            }
        };
    }

    /**
     * Override this method to customize the "Load Next Page" fragment, visible when pagination is turned
     * on and there may be more results to display.
     * <p/>
     * This method expects a {@code TextView} with id {@code android.R.id.text1}.
     *
     * @return The fragment that allows the user to paginate.
     */
    public Fragment getNextPageFragment() {
        return new Fragment(){
            @Nullable
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View view = getDefaultView(activity);
				view.findViewById(android.R.id.icon).setVisibility(View.GONE);
                TextView textView = (TextView)view.findViewById(android.R.id.text1);
                textView.setText("Load more...");
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadNextPage();
                    }
                });
                return view;
            }
        };
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
        query.setLimit(this.objectsPerPage + 1);
        query.setSkip(page * this.objectsPerPage);
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
        return this.objectsPerPage;
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
     * Sets a placeholder image to be used when fetching data for each item in the {@code ViewPager}
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
        Iterator<ParseImageView> iter = this.imageViewSet.keySet().iterator();
        ParseImageView imageView;
        while (iter.hasNext()) {
            imageView = iter.next();
            if (imageView != null) {
                imageView.setPlaceholder(this.placeholder);
            }
        }
    }

    /**
     * Enable or disable the automatic loading of results upon attachment to an {@code ViewPager}.
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
        if (this.autoload && !this.dataSetObservers.isEmpty() && this.objects.isEmpty()) {
            this.loadObjects();
        }
    }

    public void addOnQueryLoadListener(OnQueryLoadListener<T> listener) {
        this.onQueryLoadListeners.add(listener);
    }

    public void removeOnQueryLoadListener(OnQueryLoadListener<T> listener) {
        this.onQueryLoadListeners.remove(listener);
    }

    private View getDefaultView(Context context) {
        if (this.itemResourceId != null) {
            return View.inflate(context, this.itemResourceId, null);
        }
		RelativeLayout view = new RelativeLayout(context);
		RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		view.setLayoutParams(relativeParams);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams linearParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
		linearParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        linearLayout.setLayoutParams(linearParams);
		view.addView(linearLayout);

		TextView textView = new TextView(context);
		textView.setId(android.R.id.text1);
		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		params1.gravity = Gravity.CENTER_HORIZONTAL;
		textView.setLayoutParams(params1);
		textView.setGravity(Gravity.CENTER);
		linearLayout.addView(textView);

        ParseImageView imageView = new ParseImageView(context);
        imageView.setId(android.R.id.icon);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(500, 500);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        imageView.setLayoutParams(params);
		linearLayout.addView(imageView);

        return view;
    }

    private int getPaginationFragment() {
        return this.objects.size();
    }

    private boolean shouldShowPaginationFragment() {
        return this.paginationEnabled && this.objects.size() > 0 && this.hasNextPage;
    }

    private void notifyOnLoadingListeners() {
        for (OnQueryLoadListener<T> listener : this.onQueryLoadListeners) {
            listener.onLoading();
        }
    }

    private void notifyOnLoadedListeners(List<T> objects, Exception e) {
        for (OnQueryLoadListener<T> listener : this.onQueryLoadListeners) {
            listener.onLoaded(objects, e);
        }
    }
}
