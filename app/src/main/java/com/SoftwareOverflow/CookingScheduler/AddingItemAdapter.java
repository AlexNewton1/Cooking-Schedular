package com.SoftwareOverflow.CookingScheduler;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class AddingItemAdapter extends ArrayAdapter<String>{


    public AddingItemAdapter(Context context, String[] items) {
        super(context, R.layout.item_list_view, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View customView = inflater.inflate(R.layout.item_list_view, null);

        String[] info = getItem(position).split("\\|");

        TextView name = (TextView) customView.findViewById(R.id.itemNameTV);
        TextView time = (TextView) customView.findViewById(R.id.itemTimeTV);
        name.setText(info[0]);
        time.setText(info[1]);

        return customView;
    }



}
