package com.parse;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import bolts.Capture;

public class ParseListLoader<T extends ParseObject> {
    private List<List<T>> mObjectPages;
    private int mCurrentPage;
    private boolean mHasNextPage;
    private LoaderTarget<T> mTarget;
    private QueryFactory<T> mQueryFactory;
    private List<OnQueryLoadListener<T>> mOnQueryLoadListeners;
    private int mObjectsPerPage;
    private List<ParseQuery> mRunningQueries;


    public ParseListLoader(@NonNull LoaderTarget<T> target, @NonNull QueryFactory<T> factory) {
        mTarget = target;
        mQueryFactory = factory;
        mObjectPages = new ArrayList<>();
        mCurrentPage = -1; // init -1 so that we don't care about call loadObjects or loadNext first
        mHasNextPage = true;
        mOnQueryLoadListeners = new ArrayList<>();
        mObjectsPerPage = 25;
        mRunningQueries = new ArrayList<>();
    }

    public void setObjectsPerPage(int objectsPerPage) {
        mObjectsPerPage = objectsPerPage;
    }

    public int getObjectsPerPage() {
        return mObjectsPerPage;
    }

    public void addOnQueryLoadListener(OnQueryLoadListener<T> onQueryLoadListener) {
        mOnQueryLoadListeners.add(onQueryLoadListener);
    }

    public void clearOnQueryLoadListeners() {
        mOnQueryLoadListeners.clear();
    }

    public boolean hasOnQueryLoadListeners() {
        return !mOnQueryLoadListeners.isEmpty();
    }

    /**
     * support this function because sometime, we have to force the list reload
     *
     * @param hasNextPage
     */
    public void setHasNextPage(boolean hasNextPage) {
        mHasNextPage = hasNextPage;
    }

    public boolean hasNextPage() {
        return mHasNextPage;
    }

    public void loadObjects() {
        this.loadObjects(0, true);
    }

    public void loadNextPage() {
        loadObjects(mCurrentPage + 1, false);
    }

    private void loadObjects(final int page, final boolean shouldClear) {
        if (!mHasNextPage) {
            return;
        }
        if (mQueryFactory == null) {
            return;
        }

        final ParseQuery<T> query = mQueryFactory.create();
        if (query == null) {
            return;
        }

        mRunningQueries.add(query);

        setPageOnQuery(page, query);
        notifyOnLoadingListeners();
        if (page >= mObjectPages.size()) {
            for (int i = mObjectPages.size(); i <= page; i++) {
                mObjectPages.add(new ArrayList<T>());
            }
        }

        final int objectsPerPage = mObjectsPerPage;

        final Capture<Boolean> firstCallback = new Capture<>(Boolean.TRUE);
        query.findInBackground(new FindCallback<T>() {
            @Override
            public void done(List<T> foundObjects, ParseException e) {
                if (Parse.isLocalDatastoreEnabled()
                        || query.getCachePolicy() != ParseQuery.CachePolicy.CACHE_ONLY
                        || e == null
                        || e.getCode() != ParseException.CACHE_MISS) {
                    if (e != null && (e.getCode() == ParseException.CONNECTION_FAILED
                            || e.getCode() != ParseException.CACHE_MISS)) {
//                        mHasNextPage = false;
                    } else if (foundObjects != null) {
                        if (shouldClear && firstCallback.get()) {
                            mObjectPages.clear();
                            mObjectPages.add(new ArrayList<T>());
                            mCurrentPage = page;
                            firstCallback.set(Boolean.FALSE);
                        }

                        if (page >= mCurrentPage) {
                            mCurrentPage = page;
                            mHasNextPage = foundObjects.size() > objectsPerPage;
                        }

                        if (mHasNextPage) {
                            foundObjects.remove(objectsPerPage);
                        }

                        List<T> currentPage = mObjectPages.get(page);
                        currentPage.clear();
                        currentPage.addAll(foundObjects);
                        syncTargetWithPages();
                        mTarget.notifyDataChanged();
                    }
                }
                notifyOnLoadedListeners(foundObjects, mHasNextPage, e);
                mRunningQueries.remove(query);
            }
        });

    }


    private void notifyOnLoadingListeners() {
        for (OnQueryLoadListener listener : mOnQueryLoadListeners) {
            listener.onLoading();
        }
    }

    private void notifyOnLoadedListeners(List<T> list, boolean hasNextPage, ParseException e) {
        for (OnQueryLoadListener<T> listener : mOnQueryLoadListeners) {
            listener.onLoaded(list, hasNextPage, e);
        }
    }

    private void syncTargetWithPages() {
        mTarget.clearList();
        for (List<T> list : mObjectPages) {
            mTarget.appendList(list);
        }
    }

    private void setPageOnQuery(int page, ParseQuery<T> query) {
        query.setLimit(mObjectsPerPage + 1);
        query.setSkip(page * mObjectsPerPage);
    }

    public void cancelAllRunningQueries() {
        for (ParseQuery query : mRunningQueries) {
            query.cancel();
        }

        mRunningQueries.clear();
    }

    public boolean isLoading(){
        return !mRunningQueries.isEmpty();
    }

    public interface LoaderTarget<T> {
        void appendList(List<T> sublist);

        void clearList();

        void notifyDataChanged();
    }

    public interface QueryFactory<T extends ParseObject> {
        ParseQuery<T> create();
    }

    public interface OnQueryLoadListener<T> {
        void onLoading();

        void onLoaded(List<T> list, boolean hasNextPage, ParseException e);
    }
}
