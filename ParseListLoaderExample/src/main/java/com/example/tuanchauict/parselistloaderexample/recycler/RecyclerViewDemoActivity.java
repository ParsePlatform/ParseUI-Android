package com.example.tuanchauict.parselistloaderexample.recycler;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.example.tuanchauict.parselistloaderexample.ParseDemoObject;
import com.example.tuanchauict.parselistloaderexample.R;
import com.parse.ParseException;
import com.parse.ParseListLoader;
import com.parse.ParseQuery;

import java.util.List;

/**
 * Created by tuanchauict on 10/26/15.
 */
public class RecyclerViewDemoActivity extends FragmentActivity {

    RecyclerView mRecyclerView;
    RecyclerAdapter mAdapter;

    ParseListLoader<ParseDemoObject> mLoader;

    LinearLayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mAdapter = new RecyclerAdapter(this);
        mLoader = new ParseListLoader<>(mAdapter, new ParseListLoader.QueryFactory<ParseDemoObject>() {
            @Override
            public ParseQuery<ParseDemoObject> create() {
                ParseQuery<ParseDemoObject> query = ParseQuery.getQuery(ParseDemoObject.class);
                query.addAscendingOrder(ParseDemoObject.ATTR_SSD);
                return query;
            }
        });

        mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mLoader.loadObjects();

        mLoader.addOnQueryLoadListener(new ParseListLoader.OnQueryLoadListener<ParseDemoObject>() {
            @Override
            public void onLoading() {

            }

            @Override
            public void onLoaded(List<ParseDemoObject> list, boolean hasNextPage, ParseException e) {
                mAdapter.setHasNextPage(hasNextPage);
            }
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int pastVisibleItems, visibleItemCount, totalItemCount;

                visibleItemCount = mLayoutManager.getChildCount();
                totalItemCount = mLayoutManager.getItemCount();
                pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();

                if(mAdapter.hasNextPage() && visibleItemCount + pastVisibleItems > totalItemCount - 5){
                    mLoader.loadNextPage();
                }
            }
        });
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return super.onMenuItemSelected(featureId, item);
    }
}
