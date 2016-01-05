package com.SoftwareOverflow.CookingScheduler;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Created by Alex on 03/01/16.
 */
public class UpcomingRemindersAdapter extends ArrayAdapter<String> {
    public UpcomingRemindersAdapter(Context context, String[] adapterStrings) {
        super(context, R.layout.reminders_dialog_list_item, adapterStrings);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View customView = inflater.inflate(R.layout.reminders_dialog_list_item, null);

        TextView dateTV = (TextView) customView.findViewById(R.id.upcomingReminderDateTV);
        TextView nameTV = (TextView) customView.findViewById(R.id.upcomingReminderItemTV);
        TextView timeTV = (TextView) customView.findViewById(R.id.upcomingReminderTimeTV);

        String[] info = getItem(position).split("\\|");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Long.parseLong(info[1]));

        nameTV.setText(info[0]);
        dateTV.setText(cal.get(Calendar.DAY_OF_MONTH) + " - " + cal.get(Calendar.MONTH)+1 + " - " + cal.get(Calendar.YEAR));
        timeTV.setText(String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));

        return customView;
    }
}
