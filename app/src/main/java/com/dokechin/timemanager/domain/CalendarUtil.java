package com.dokechin.timemanager.domain;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.util.Log;

import com.dokechin.timemanager.MainActivity;

import java.io.StringBufferInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by dokechin on 2016/07/30.
 */
public class CalendarUtil {

    private ContentResolver contentResolver;
    private static String TAG = CalendarUtil.class.getSimpleName();
    private static Pattern YM;
    private Calendar[] calendars;

    public CalendarUtil(ContentResolver contentResolver, String ym) {
        this.contentResolver = contentResolver;

        YM = Pattern.compile(ym);

        Cursor cursor = this.contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                new String [] {CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                        CalendarContract.Calendars._ID},
                null, null, null);
        HashSet<Calendar> set = new HashSet<Calendar>();
        while(cursor.moveToNext()){
            String title = cursor.getString(0);
            long id = cursor.getLong(1);
            Log.d(TAG, "title=" + title);
            Log.d(TAG, "id=" + id);
            set.add(new Calendar(title,id));
        }
        calendars = set.toArray(new Calendar[0]);


    }

    public Calendar[] getCalendars(){
        return calendars;
    }

    public String[] getAllEvents(Calendar cal){

        Log.d(TAG, "getAllEvents");
        Log.d(TAG, "id=" + cal.getId());

        Cursor cursor = this.contentResolver.query(CalendarContract.Events.CONTENT_URI,
                new String [] {
                        CalendarContract.Events.TITLE}
                , "(" + CalendarContract.Events.CALENDAR_ID + "=  ? )"
                , new String[]{
                        Long.toString(cal.getId())}, "");

        TreeSet<String> set = new TreeSet<String>();
        while(cursor.moveToNext()){
            String title = cursor.getString(0);
            set.add(title);
        }
        return set.toArray(new String[0]);

    }
    public TreeMap getEvents(Calendar cal, String date){

        Log.d(TAG, "date" + date);

        Matcher matcher = YM.matcher(date);
        boolean match = matcher.find();

        String year = matcher.group(1);
        String month = matcher.group(2);

        java.util.Calendar monthFirst = java.util.Calendar.getInstance();
        monthFirst.set(Integer.parseInt(year),Integer.parseInt(month)-1,1);
        java.util.Calendar monthEnd = (java.util.Calendar)monthFirst.clone();
        monthEnd.add(java.util.Calendar.MONTH, 1);

        Cursor cursor = this.contentResolver.query(CalendarContract.Events.CONTENT_URI,
                new String [] {
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND,
                        CalendarContract.Events.TITLE}
                , "(" + CalendarContract.Events.CALENDAR_ID + "=  ?  AND " +
                        CalendarContract.Events.DTSTART     + "<  ?  AND " +
                        CalendarContract.Events.DTEND       + "> ? )"
                , new String[]{
                        Long.toString(cal.getId()),
                        String.valueOf(monthEnd.getTimeInMillis()),
                        String.valueOf(monthFirst.getTimeInMillis())}, "");

        TreeMap<String, Long> map = new TreeMap<String,Long>();
        while(cursor.moveToNext()){
            long start = cursor.getLong(0);
            long end = cursor.getLong(1);
            if (start < monthFirst.getTimeInMillis()){
                start = monthFirst.getTimeInMillis();
            }
            if (end > monthEnd.getTimeInMillis()){
                end = monthEnd.getTimeInMillis();
            }

            long period = end - start;
            String title = cursor.getString(2);
            Long p_period = map.get(title);
            if (p_period != null){
                period += p_period;
            }
            map.put(title, period);
        }


        return map;
    }


    public int getNewEventId() {
        Cursor cursor = contentResolver.query(CalendarContract.Events.CONTENT_URI,
                new String [] {"MAX(_id) as max_id"}, null, null, "_id");
        cursor.moveToFirst();
        int max_val = cursor.getInt(cursor.getColumnIndex("max_id"));
        return max_val+1;
    }

}
