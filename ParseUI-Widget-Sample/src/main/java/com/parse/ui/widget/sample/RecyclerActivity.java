package com.parse.ui.widget.sample;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.widget.util.ParseQueryPager;

import java.util.List;

import bolts.CancellationTokenSource;
import bolts.Continuation;
import bolts.Task;

public class RecyclerActivity extends AppCompatActivity {

  private SwipeRefreshLayout refreshLayout;

  private MyAdapter<ParseObject> adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_recycler);

    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler);

    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);

    adapter = new MyAdapter<>(createPager());
    recyclerView.setAdapter(adapter);

    refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
    refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        final ParseQueryPager<ParseObject> pager = createPager();
        pager.loadNextPage().continueWith(new Continuation<List<ParseObject>, Void>() {
          @Override
          public Void then(Task<List<ParseObject>> task) throws Exception {
            refreshLayout.setRefreshing(false);

            if (task.isCancelled()) {
              return null;
            }

            if (task.isFaulted()) {
              return null;
            }

            adapter.swap(pager);
            adapter.notifyDataSetChanged();
            return null;
          }
        }, Task.UI_THREAD_EXECUTOR);
      }
    });
  }

  private ParseQueryPager<ParseObject> createPager() {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("TestObject");
    query.orderByAscending("name");
    return new ParseQueryPager<>(query, 25);
  }

  public static class MyAdapter<T extends ParseObject> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_NEXT = 1;

    private static class ItemViewHolder extends RecyclerView.ViewHolder {
      TextView textView;

      public ItemViewHolder(View itemView) {
        super(itemView);
        textView = (TextView) itemView;
      }
    }

    private static class NextViewHolder extends RecyclerView.ViewHolder {
      TextView textView;
      ProgressBar progressBar;

      public NextViewHolder(View itemView) {
        super(itemView);
        textView = (TextView) itemView.findViewById(android.R.id.text1);
        progressBar = (ProgressBar) itemView.findViewById(android.R.id.progress);
      }

      public void setLoading(boolean loading) {
        if (loading) {
          textView.setVisibility(View.INVISIBLE);
          progressBar.setVisibility(View.VISIBLE);
          progressBar.setIndeterminate(true);
        } else {
          textView.setVisibility(View.VISIBLE);
          progressBar.setVisibility(View.INVISIBLE);
          progressBar.setIndeterminate(false);
        }
      }
    }

    private final Object lock = new Object();
    private ParseQueryPager<T> pager;
    private CancellationTokenSource cts;

    public MyAdapter(ParseQueryPager<T> pager) {
      swap(pager);
    }

    public ParseQueryPager<T> getPager() {
      synchronized (lock) {
        return pager;
      }
    }

    public void swap(ParseQueryPager<T> pager) {
      synchronized (lock) {
        if (cts != null) {
          cts.cancel();
        }
        this.pager = pager;
        this.cts = new CancellationTokenSource();
      }
    }

    private void loadNextPage() {
      final ParseQueryPager<T> pager;
      final CancellationTokenSource cts;

      synchronized (lock) {
        pager = this.pager;
        cts = this.cts;
      }

      final int oldSize = pager.getObjects().size();

      // Uses Tasks, so it does not support CACHE_THEN_NETWORK. See ListActivity for a sample
      // with callbacks.
      pager.loadNextPage(cts.getToken()).continueWith(new Continuation<List<T>, Task<Void>>() {
        @Override
        public Task<Void> then(Task<List<T>> task) throws Exception {
          if (task.isCancelled()) {
            return null;
          }

          if (task.isFaulted()) {
            notifyDataSetChanged();
            return null;
          }

          // Remove "Load more..."
          notifyItemRemoved(oldSize);

          // Insert results
          List<T> results = task.getResult();
          if (results.size() > 0) {
            notifyItemRangeInserted(oldSize, results.size());
          }

          if (pager.hasNextPage()) {
            // Add "Load more..."
            notifyItemInserted(pager.getObjects().size());
          }
          return null;
        }
      });
      notifyItemChanged(oldSize);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      switch (viewType) {
        case TYPE_ITEM: {
          View v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
          return new ItemViewHolder(v);
        }
        case TYPE_NEXT: {
          View v = inflater.inflate(R.layout.load_more_list_item, parent, false);
          v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (!getPager().isLoadingNextPage()) {
                loadNextPage();
              }
            }
          });
          NextViewHolder vh = new NextViewHolder(v);
          vh.textView.setText(R.string.load_more);
          return vh;
        }
        default:
          throw new IllegalStateException("Invalid view type: " + viewType);
      }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      switch (getItemViewType(position)) {
        case TYPE_ITEM: {
          ParseObject item = getPager().getObjects().get(position);

          ItemViewHolder vh = (ItemViewHolder) holder;
          vh.textView.setText(item.getString("name"));
        }
        break;
        case TYPE_NEXT: {
          NextViewHolder vh = (NextViewHolder) holder;
          vh.setLoading(getPager().isLoadingNextPage());
        }
        break;
      }
    }

    @Override
    public int getItemCount() {
      ParseQueryPager<T> pager = getPager();
      return pager.getObjects().size() + (pager.hasNextPage() ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
      return position < getPager().getObjects().size() ? TYPE_ITEM : TYPE_NEXT;
    }

    @Override
    public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
      super.registerAdapterDataObserver(observer);
      // We use this method as a notification that the RecyclerView is bound to the adapter.
      loadNextPage();
    }
  }
}
