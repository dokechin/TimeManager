package com.dokechin.timemanager.domain;

/**
 * Created by dokechin on 2016/07/22.
 */
public class Counter {
    private long sec = 0;
    public Counter(long second) {
        this.sec = second;
    }
    public String clockFormat(){

        long hour = this.sec / 3600;
        long min = (this.sec  - hour * 3600) / 60;
        long sec = (this.sec  - hour * 3600 - min * 60);

        return String.format("%1$02d:%2$02d:%3$02d", hour, min , sec);
    }
}
