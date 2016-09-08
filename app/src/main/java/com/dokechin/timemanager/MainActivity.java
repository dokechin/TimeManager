package com.dokechin.timemanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.dokechin.timemanager.domain.CalendarUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.zip.CRC32;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static String TAG = MainActivity.class.getSimpleName();
    private Spinner calenderSpinner;
    private Spinner dateSpinner;
    private com.dokechin.timemanager.domain.Calendar calendarSpinnerItems[] = {};
    private String dateSpinnerItems[] = {};
    private CalendarUtil calendarUtil;
    private TextView textView;
    private ListView listView;
    private WebView webView;
    private JSONArray dataSet;
    private com.dokechin.timemanager.domain.Calendar calendar;

    @Override
    protected void onStart(){
        super.onStart();
        Log.d(TAG, "onStart");

        this.render();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        calendarUtil = new CalendarUtil(this.getContentResolver(), getString(R.string.YM));
        calendarSpinnerItems = calendarUtil.getCalendars();

        calenderSpinner = (Spinner)findViewById(R.id.spinner1);

        // ArrayAdapter
        ArrayAdapter<com.dokechin.timemanager.domain.Calendar> adapter1
                = new ArrayAdapter<com.dokechin.timemanager.domain.Calendar>(this, R.layout.spinner_item, calendarSpinnerItems);
        adapter1.setDropDownViewResource(R.layout.spinner_dropdown_item);
        // spinner に adapter をセット
        calenderSpinner.setAdapter(adapter1);
        calenderSpinner.setOnItemSelectedListener(this);

        dateSpinner = (Spinner)findViewById(R.id.spinner2);
        // current year and month
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        Log.d(TAG, "year" + year);
        Log.d(TAG, "month" + month);
        DateFormat df = new SimpleDateFormat(getString(R.string.yyyymm_dateformat));
        ArrayList<String> list = new ArrayList<String>();
        end :
        {
            for (int i = 0; i < 11; i++) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year - 10 + i);
                for (int j = 0; j < 12; j++) {
                    cal.set(Calendar.MONTH, j);
                    list.add(df.format(cal.getTime()));
                    if ((year - 10 + i) == year && j == month) {
                        break end;
                    }
                }
            }
        }
        Collections.reverse(list);
        dateSpinnerItems = list.toArray(new String[list.size()]);
        // ArrayAdapter
        ArrayAdapter<String> adapter2
                = new ArrayAdapter<String>(this, R.layout.spinner_item, dateSpinnerItems);
        adapter2.setDropDownViewResource(R.layout.spinner_dropdown_item);
        // spinner に adapter をセット
        dateSpinner.setAdapter(adapter2);
        dateSpinner.setOnItemSelectedListener(this);

        textView = (TextView)findViewById(R.id.textview);
        listView = (ListView)findViewById(R.id.listview);

        SharedPreferences sp = getSharedPreferences("TimeManager", Context.MODE_PRIVATE);
        String defaultCalendar = sp.getString("calendar_title", null);
        Log.d(TAG, "defaultCalendar" + defaultCalendar);

        if (defaultCalendar != null) {
            // デフォルト選択する
            for(int i=0; i < adapter1.getCount(); i++) {
                if(defaultCalendar.equals(adapter1.getItem(i).toString())){
                    calenderSpinner.setSelection(i);
                    break;
                }
            }
        }

        webView = (WebView)findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);


    }

    private void render(){
        // カレンダーIDと日付からイベントごとの時間を集計して表示する
        calendar =
                (com.dokechin.timemanager.domain.Calendar)calenderSpinner.getSelectedItem();
        String date = (String)dateSpinner.getSelectedItem();

        if (calendar == null) {
            textView.setText(getString(R.string.calendar_notfound));
            return;
        }

        Log.d(TAG, "calendar=" + calendar.toString());
        Log.d(TAG, "date=" + date);

        TreeMap<String,Long> events = calendarUtil.getEvents(calendar,date);
        Iterator ite = events.keySet().iterator();

        long totalMills = 0;
        ArrayList list = new ArrayList();

        for (Map.Entry<String, Long> entry : events.entrySet()) {

            String name= entry.getKey();
            long mills = entry.getValue();
            totalMills += mills;
            long hour = mills / 3600000;
            long min = (mills - (hour * 3600000)) / 60000;
            HashMap map = new HashMap();
            map.put("title", name);
            if (hour > 0){
                map.put("time", String.format("%d%s%02d%s", hour, getString(R.string.hour), min, getString(R.string.min)));
            }
            else {
                map.put("time", String.format("%d%s", min, getString(R.string.min)));
            }
            list.add(map);
        }

        dataSet = new JSONArray();

        for (Map.Entry<String, Long> entry : events.entrySet()) {

            String name= entry.getKey();
            long mills = entry.getValue();

            Log.d(TAG, "name" + name);
            Log.d(TAG, "mills" + mills);

            // 1 min over and register
            if (mills >= 60000 ) {
                JSONObject json = new JSONObject();
                try {
                    json.put("label", name);
                    json.put("count", (int)((1.0 * mills / totalMills) * 100.0));
                    CRC32 crc = new CRC32();
                    crc.update(name.getBytes());
                    String color = String.format("#%06X", crc.getValue()).substring(0,7);
                    Log.d(TAG, "color" + color);
                    json.put("color", color);

                    dataSet.put(json);
                } catch (JSONException ex) {
                }
            }
        }

        long totalHour = totalMills / 3600000;
        long totalMin = (totalMills - (totalHour * 3600000)) / 60000;

        if (totalHour > 0) {
            textView.setText(String.format("%s : %d%s%02d%s", getString(R.string.total), totalHour, getString(R.string.hour), totalMin, getString(R.string.min)));
        }
        else{
            textView.setText(String.format("%s : %d%s", getString(R.string.total), totalMin, getString(R.string.min)));
        }

        // リストビューに渡すアダプタを生成
        SpecialAdapter adapter = new SpecialAdapter(
                        this,
                        list,
                        R.layout.list,
                new String[] { "title", "time" },
                new int[] {R.id.title, R.id.time });

        listView.setAdapter(adapter);

        Log.d(TAG, "dataSet" + dataSet.toString());

        webView.loadUrl("file:///android_asset/test.html");
        webView.setWebViewClient(new WebViewClient(){
            public void onPageFinished(WebView view, String url){
                webView.loadUrl("javascript:init('" + dataSet.toString() +  "')");
            }
        });


    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // get EventID
        SharedPreferences sp = getSharedPreferences("TimeManager", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();

        com.dokechin.timemanager.domain.Calendar calendar =
                (com.dokechin.timemanager.domain.Calendar)calenderSpinner.getSelectedItem();

        edit.putString("calendar_title", (calendar != null)? calendar.getTitle(): null);
        edit.commit();

        this.render();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    public void openTimerActivity(View view)
    {
        Log.d(TAG, "calender title" + calendar.getTitle());
        Log.d(TAG, "calender id " + calendar.getId());
        Intent intent = new Intent(this, TimerActivity.class);
        intent.putExtra("TITLE", calendar.getTitle());
        intent.putExtra("ID", String.valueOf(calendar.getId()));
        startActivity(intent);
    }
}
