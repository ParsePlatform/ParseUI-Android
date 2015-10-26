package com.example.tuanchauict.parselistloaderexample.recycler;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
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
 * Created by tuanchauict on 10/26/15.
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.Holder> implements ParseListLoader.LoaderTarget<ParseDemoObject> {

    static class Holder extends RecyclerView.ViewHolder {
        View mView;
        public Holder(View itemView) {
            super(itemView);
            mView = itemView;
        }
    }

    private List<ParseDemoObject> mObjects;
    private boolean mHasNextPage;
    private LayoutInflater mInflater;

    public RecyclerAdapter(Context context) {
        mInflater = LayoutInflater.from(context);

        mObjects = new ArrayList<>();
        mHasNextPage = true;
    }

    @Override
    public int getItemViewType(int position) {
        return mObjects.size() <= position ? 1 : 0;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == 0){
            return new Holder(mInflater.inflate(R.layout.item_layout, parent, false));
        }
        else{
            return new Holder(mInflater.inflate(R.layout.item_waiting, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        if(getItemViewType(position) > 0){
            return;
        }

        ParseDemoObject object = mObjects.get(position);
        TextView txt = (TextView) holder.mView.findViewById(R.id.txt_name);
        txt.setText(object.getFirstname() + " " + object.getLastName());
    }

    @Override
    public int getItemCount() {
        return mObjects.isEmpty() ? 0 : mHasNextPage ? mObjects.size() + 1 : mObjects.size();
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

    public boolean hasNextPage() {
        return mHasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        mHasNextPage = hasNextPage;
    }
}
