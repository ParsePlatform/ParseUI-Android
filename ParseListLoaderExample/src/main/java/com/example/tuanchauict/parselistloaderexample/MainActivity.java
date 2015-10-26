package com.example.tuanchauict.parselistloaderexample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.tuanchauict.parselistloaderexample.listview.ListViewDemoActivity;
import com.example.tuanchauict.parselistloaderexample.recycler.RecyclerViewDemoActivity;
import com.example.tuanchauict.parselistloaderexample.viewpager.ViewPagerDemoActivity;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.create_1000_objects).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                create1000Objects();
            }
        });


        findViewById(R.id.goto_list_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ListViewDemoActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.goto_viewpager).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ViewPagerDemoActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.goto_recycler_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecyclerViewDemoActivity.class);
                startActivity(intent);
            }
        });
    }

    public void create1000Objects() {

        for (int i = 0; i < 1000; i++) {
            ParseDemoObject object = ParseDemoObject.getObject(i);
            object.saveEventually();
        }
        Toast.makeText(this, "Check on your app's data", Toast.LENGTH_LONG).show();
    }
}
