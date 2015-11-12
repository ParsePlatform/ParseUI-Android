package com.example.tuanchauict.parselistloaderexample;

import com.parse.ParseClassName;
import com.parse.ParseObject;

/**
 * Created by tuanchauict on 10/25/15.
 */
@ParseClassName("ListLoaderItem")
public class ParseDemoObject extends ParseObject {
    public static final String ATTR_SSD = "ssd";
    public static final String ATTR_FIRST_NAME = "firstName";
    public static final String ATTR_LAST_NAME = "lastName";


    public static ParseDemoObject getObject(int ssd){
        ParseDemoObject object = new ParseDemoObject();
        object.put(ATTR_SSD, ssd);
        object.put(ATTR_FIRST_NAME, "FirstName " + ssd);
        object.put(ATTR_LAST_NAME, "LastName " + ssd);
        return object;
    }


    public int getSSD(){
        return getInt(ATTR_SSD);
    }

    public String getFirstname(){
        return getString(ATTR_FIRST_NAME);
    }


    public String getLastName(){
        return getString(ATTR_LAST_NAME);
    }


}
