package com.example.tuanchauict.parselistloaderexample.viewpager;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;

import com.example.tuanchauict.parselistloaderexample.ParseDemoObject;
import com.example.tuanchauict.parselistloaderexample.R;
import com.parse.ParseException;
import com.parse.ParseListLoader;
import com.parse.ParseQuery;

import java.util.List;

/**
 * Created by tuanchauict on 10/25/15.
 */
public class ViewPagerDemoActivity extends FragmentActivity {
    ViewPager mViewPager;
    ViewPagerAdapter mAdapter;
    ParseListLoader<ParseDemoObject> mLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mAdapter = new ViewPagerAdapter(this);
        mLoader = new ParseListLoader<>(mAdapter, new ParseListLoader.QueryFactory<ParseDemoObject>() {
            @Override
            public ParseQuery<ParseDemoObject> create() {
                ParseQuery<ParseDemoObject> query = ParseQuery.getQuery(ParseDemoObject.class);
                query.addAscendingOrder(ParseDemoObject.ATTR_SSD);
                return query;
            }
        });
        mViewPager.setAdapter(mAdapter);
        mLoader.loadObjects();


        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position + 5 > mAdapter.getCount() && mAdapter.hasNextPage()){
                    mLoader.loadNextPage();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mLoader.addOnQueryLoadListener(new ParseListLoader.OnQueryLoadListener<ParseDemoObject>() {
            @Override
            public void onLoading() {

            }

            @Override
            public void onLoaded(List<ParseDemoObject> list, boolean hasNextPage, ParseException e) {
                mAdapter.setHasNextPage(hasNextPage);
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
