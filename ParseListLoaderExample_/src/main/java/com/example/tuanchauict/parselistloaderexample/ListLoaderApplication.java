package com.example.tuanchauict.parselistloaderexample;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by tuanchauict on 10/25/15.
 */
public class ListLoaderApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Required - Initialize the Parse SDK
        Parse.initialize(this);
        ParseObject.registerSubclass(ParseDemoObject.class);

    }
}
