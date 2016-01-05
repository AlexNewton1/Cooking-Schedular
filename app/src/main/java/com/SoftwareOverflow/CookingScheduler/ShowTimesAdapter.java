package com.SoftwareOverflow.CookingScheduler;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Calendar;


public class ShowTimesAdapter extends ArrayAdapter<String> {
    public ShowTimesAdapter(Context context, String[] info) {
        super(context, R.layout.show_times_list_view_layout, info);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View customView = inflater.inflate(R.layout.show_times_list_view_layout, null);

        String[] info = getItem(position).split("\\|");
        int cookingTime = Integer.parseInt(info[1]);

        TextView startTime = (TextView) customView.findViewById(R.id.startTimeTV);
        TextView startItem = (TextView) customView.findViewById(R.id.startItemTV);

        Calendar readyTimeCal = Calendar.getInstance();
        readyTimeCal.setTime(ShowTimes.getReadyTimeCal().getTime());
        Log.d("cal","" + readyTimeCal.getTime());
        readyTimeCal.add(Calendar.MINUTE, -cookingTime );

        startItem.setText(info[0] + " - " + info[1] + "mins");
        startTime.setText(String.format("%02d:%02d", readyTimeCal.get(Calendar.HOUR_OF_DAY), readyTimeCal.get(Calendar.MINUTE)));

        return customView;
    }
}
