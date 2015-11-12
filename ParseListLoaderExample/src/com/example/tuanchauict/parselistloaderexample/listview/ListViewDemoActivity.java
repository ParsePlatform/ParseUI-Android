package com.example.tuanchauict.parselistloaderexample.listview;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ListView;

import com.example.tuanchauict.parselistloaderexample.ParseDemoObject;
import com.example.tuanchauict.parselistloaderexample.R;
import com.parse.ParseException;
import com.parse.ParseListLoader;
import com.parse.ParseQuery;

import java.util.List;

/**
 * Created by tuanchauict on 10/25/15.
 */
public class ListViewDemoActivity extends FragmentActivity
        implements ParseListLoader.OnQueryLoadListener{


    private ListView mListView;
    private ListViewAdapter mAdapter;
    private ParseListLoader<ParseDemoObject> mLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mListView = (ListView) findViewById(R.id.list_view);
        mAdapter = new ListViewAdapter(this);
        mLoader = new ParseListLoader<>(mAdapter,
                new ParseListLoader.QueryFactory<ParseDemoObject>() {
            @Override
            public ParseQuery<ParseDemoObject> create() {
                ParseQuery<ParseDemoObject> query = ParseQuery.getQuery(ParseDemoObject.class);
                query.addAscendingOrder(ParseDemoObject.ATTR_SSD);
                return query;
            }
        });
        mLoader.loadObjects();

        mListView.setAdapter(mAdapter);

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
                if(firstVisibleItem + visibleItemCount > totalItemCount -5
                        && mAdapter.hasNextPage()){
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

    @Override
    public void onLoading() {

    }

    @Override
    public void onLoaded(List list, boolean hasNextPage, ParseException e) {
        mAdapter.setHasNextPage(hasNextPage);
    }
}
