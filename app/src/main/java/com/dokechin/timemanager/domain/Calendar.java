package com.dokechin.timemanager.domain;

import android.database.Cursor;
import android.provider.CalendarContract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by dokechin on 2016/07/30.
 */
public class Calendar {

    private String title;
    private long id;

    public Calendar(String title, long id){
        this.title = title;
        this.id = id;
    }

    @Override public String toString(){
        return this.title;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

