package com.example.tuanchauict.parselistloaderexample.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.tuanchauict.parselistloaderexample.ParseDemoObject;
import com.example.tuanchauict.parselistloaderexample.R;
import com.parse.ParseListLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tuanchauict on 10/25/15.
 */
public class ListViewAdapter extends ArrayAdapter<ParseDemoObject>
        implements ParseListLoader.LoaderTarget<ParseDemoObject> {
    private List<ParseDemoObject> mObjects;
    private boolean mHasNextPage;

    private LayoutInflater mInflater;

    public ListViewAdapter(Context context) {
        super(context, 0);
        mInflater = LayoutInflater.from(context);
        mObjects = new ArrayList<>();
        mHasNextPage = true;
    }

    public boolean hasNextPage() {
        return mHasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        mHasNextPage = hasNextPage;
    }

    @Override
    public int getCount() {
        return mObjects.isEmpty() ? 0 : mHasNextPage ? mObjects.size() + 1 : mObjects.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(position >= mObjects.size()){
            return mInflater.inflate(R.layout.item_waiting, parent, false);
        }

        if (convertView == null || convertView.getTag() != Boolean.TRUE) {
            convertView = mInflater.inflate(R.layout.item_layout, parent, false);
            convertView.setTag(Boolean.TRUE);
        }
        ParseDemoObject item = mObjects.get(position);

        ((TextView) convertView.findViewById(R.id.txt_name))
                .setText(item.getFirstname() + " " + item.getLastName());

        return convertView;
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
