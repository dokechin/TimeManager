package com.dokechin.timemanager;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.dokechin.timemanager.domain.Calendar;
import com.dokechin.timemanager.domain.CalendarUtil;
import com.dokechin.timemanager.domain.Counter;

import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class TimerActivity extends AppCompatActivity {

    private String TAG = TimerActivity.class.getSimpleName();
    private AutoCompleteTextView mTextView;
    private TextView mCountTextView;
    private Calendar calendar;

    private boolean mIsRunning;
    private long mCount;
    private String mTitle;
    private Long mStartTimeMills;

    private View startButton;
    private View endButton;
    private View cancelButton;

    private Handler mHandler = new Handler();   //UI Threadへのpost用ハンドラ
    private Timer mainTimer;
    private MainTimerTask mainTimerTask;

    private CalendarUtil calendarUtil;

    @Override public void onStop(){
        super.onStop();
        if(mIsRunning && mainTimer != null){
            mainTimer.cancel();
            mainTimer = null;
        }
    }
    @Override public void onStart(){
        super.onStart();

        Log.d(TAG, "onStart");

        SharedPreferences sp = getSharedPreferences("TimeManager", Context.MODE_PRIVATE);

        mStartTimeMills = sp.getLong("start", 0);
        mTitle = sp.getString("title", "");
        mIsRunning = sp.getBoolean("is_running", false);

        Log.d(TAG, "mStartTimeMills" + mStartTimeMills);
        Log.d(TAG, "mLastEventTitle" + mTitle);
        Log.d(TAG, "isRunning" + mIsRunning);

        if (mIsRunning && mainTimer == null){

            // restart count
            mCount = (System.currentTimeMillis() - mStartTimeMills) / 1000;
            mTextView.setText(mTitle);

            //タイマーインスタンス生成
            this.mainTimer = new Timer();
            //タスククラスインスタンス生成
            this.mainTimerTask = new MainTimerTask();
            //タイマースケジュール設定＆開始
            this.mainTimer.schedule(mainTimerTask, 1000, 1000);

        }

        if (mIsRunning){
            startButton.setClickable(false);
            endButton.setClickable(true);
            cancelButton.setClickable(true);
        }
        else{
            startButton.setClickable(true);
            endButton.setClickable(false);
            cancelButton.setClickable(false);
        }


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        calendarUtil = new CalendarUtil(this.getContentResolver(), getString(R.string.YM));

        Intent i = getIntent();
        String title = i.getStringExtra("TITLE");
        String id = i.getStringExtra("ID");
        calendar = new Calendar(title, Long.parseLong(id));

        String[] TITLES = calendarUtil.getAllEvents(calendar);

        // In the onCreate method
        mTextView = (AutoCompleteTextView) this.findViewById(R.id.task_textview);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, TITLES);
        mTextView.setAdapter(adapter);

        //テキストビュー
        mCountTextView = (TextView)findViewById(R.id.timer_text);

        startButton = this.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (!mIsRunning && !mTextView.getText().toString().isEmpty()){

                    mStartTimeMills = System.currentTimeMillis();
                    mTitle = mTextView.getText().toString();
                    mIsRunning = true;

                    startButton.setClickable(false);
                    endButton.setClickable(true);
                    cancelButton.setClickable(true);

                    // reset count
                    mCount = 0;

                    //タイマーインスタンス生成
                    mainTimer = new Timer();
                    //タスククラスインスタンス生成
                    mainTimerTask = new MainTimerTask();
                    //タイマースケジュール設定＆開始
                    mainTimer.schedule(mainTimerTask, 1000, 1000);

                    // get EventID
                    SharedPreferences sp = getSharedPreferences("TimeManager", Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = sp.edit();
                    edit.putLong("start", mStartTimeMills);
                    edit.putString("title", mTitle);
                    edit.putBoolean("is_running", mIsRunning);
                    edit.commit();

                }
            }
        });
        endButton = this.findViewById(R.id.end_button);
        endButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (mIsRunning){

                    mIsRunning = false;
                    mainTimer.cancel();

                    startButton.setClickable(true);
                    endButton.setClickable(false);
                    cancelButton.setClickable(false);

                    long currentTimeMillis = System.currentTimeMillis();

                    ContentResolver cr = getContentResolver();
                    ContentValues values = new ContentValues();

                    values.put(CalendarContract.Events.DTSTART, mStartTimeMills);
                    values.put(CalendarContract.Events.CALENDAR_ID, calendar.getId());
                    values.put(CalendarContract.Events.DTEND, currentTimeMillis);
                    values.put(CalendarContract.Events.TITLE, mTitle);
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    TimeZone tz = cal.getTimeZone();
                    Log.d("Time zone","="+tz.getDisplayName());
                    values.put(CalendarContract.Events.EVENT_TIMEZONE, tz.getDisplayName());
                    cr.insert(CalendarContract.Events.CONTENT_URI, values);

                    // get EventID
                    SharedPreferences sp = getSharedPreferences("TimeManager", Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = sp.edit();
                    edit.putBoolean("is_running", mIsRunning);
                    edit.commit();
                }
            }
        });

        cancelButton = this.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (mIsRunning){

                    mIsRunning = false;
                    mainTimer.cancel();

                    startButton.setClickable(true);
                    endButton.setClickable(false);
                    cancelButton.setClickable(false);

                    // get EventID
                    SharedPreferences sp = getSharedPreferences("TimeManager", Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = sp.edit();
                    edit.putBoolean("is_running", mIsRunning);
                    edit.commit();
                }
            }
        });

    }

    public class MainTimerTask extends TimerTask {
        @Override
        public void run() {
            //ここに定周期で実行したい処理を記述します
            mHandler.post( new Runnable() {
                public void run() {

                    //実行間隔分を加算処理
                    mCount+= 1;
                    Counter counter = new Counter(mCount);
                    //画面にカウントを表示
                    mCountTextView.setText(counter.clockFormat());
                }
            });
        }
    }

}
