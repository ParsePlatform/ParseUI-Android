package com.parse.ui.widget.sample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.sample_recycler).setOnClickListener(this);
    findViewById(R.id.sample_list).setOnClickListener(this);
  }

  //region OnClickListener

  @Override
  public void onClick(View v) {
    int id = v.getId();
    switch (id) {
      case R.id.sample_recycler: {
        Intent intent = new Intent(this, RecyclerActivity.class);
        startActivity(intent);
        break;
      }
      case R.id.sample_list: {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
        break;
      }
    }
  }

  //endregion
}
