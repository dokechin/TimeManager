package com.dokechin.timemanager;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.zip.CRC32;

public class SpecialAdapter extends SimpleAdapter {
    private static String TAG = SpecialAdapter.class.getSimpleName();
    private int[] colors = new int[] { 0x30FF0000, 0x300000FF };

    public SpecialAdapter(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
        super(context, items, resource, from, to);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        HashMap map = (HashMap<String,String>)this.getItem(position);
        CRC32 crc = new CRC32();
        String title = (String)map.get("title");
        crc.update(title.getBytes());

        String colorString = String.format("#FF%s", String.format("%06X", crc.getValue()).substring(0,6));

        view.setBackgroundColor(Color.parseColor(colorString));
        return view;
    }
}
