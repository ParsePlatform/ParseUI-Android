package com.example.tuanchauict.parselistloaderexample.viewpager;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.tuanchauict.parselistloaderexample.ParseDemoObject;
import com.example.tuanchauict.parselistloaderexample.R;
import com.parse.ParseListLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuanchauict on 10/25/15.
 */
public class ViewPagerAdapter extends PagerAdapter
        implements ParseListLoader.LoaderTarget<ParseDemoObject> {
    private List<ParseDemoObject> mObjects;
    private boolean mHasNextPage;

    private List<View> mPool;

    LayoutInflater mInflater;

    public ViewPagerAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mObjects = new ArrayList<>();
        mHasNextPage = true;

        mPool = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return mObjects.isEmpty() ? 0 : mHasNextPage ? mObjects.size() + 1 : mObjects.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (position >= mObjects.size()) {
            return mInflater.inflate(R.layout.item_waiting, container, true);
        }

        View view;
        if (mPool.isEmpty()) {
            view = mInflater.inflate(R.layout.item_layout_2, container, false);
        } else {
            view = mPool.remove(0);
        }

        ParseDemoObject object = mObjects.get(position);

        TextView txt = (TextView) view.findViewById(R.id.txt_name);
        txt.setText(object.getFirstname() + " " + object.getLastName());
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        container.removeView(view);
        if (position < mObjects.size())
            mPool.add((View) object);
    }

    public boolean hasNextPage() {
        return mHasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        mHasNextPage = hasNextPage;
    }

    @Override
    public void appendList(List<ParseDemoObject> sublist) {
        mObjects.addAll(sublist);
    }

    @Override
    public void clearList() {
        mObjects.clear();
    }

    @Override
    public void notifyDataChanged() {
        notifyDataSetChanged();
    }
}
