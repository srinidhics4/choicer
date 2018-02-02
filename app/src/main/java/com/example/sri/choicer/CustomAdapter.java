package com.example.sri.choicer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.sri.choicer.models.Choice;

import java.util.List;

/**
 * Created by Sri on 11/6/2017.
 * CustomAdapter class defines custom ListView
 */

public class CustomAdapter extends BaseAdapter{
        private Context context;
        List<Choice> choiceList;

        public CustomAdapter(List<Choice> listValue, Context context){
            this.context = context;
            this.choiceList = listValue;
        }

        @Override
        public int getCount(){
            return this.choiceList.size();
        }

        @Override
        public Object getItem(int position){
            return this.choiceList.get(position);
        }

        @Override
        public long getItemId(int position){
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewItem viewItem = null;
            if (convertView == null) {
                viewItem = new ViewItem();
                LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.list_view_items, null);
                viewItem.ChoiceNameTextView = (TextView) convertView.findViewById(R.id.textView);
                viewItem.EmailTextView = (TextView) convertView.findViewById(R.id.textView2);
                convertView.setTag(viewItem);
            } else {
                viewItem = (ViewItem) convertView.getTag();
            }
            viewItem.ChoiceNameTextView.setText(choiceList.get(position).getTitle());
            viewItem.EmailTextView.setText(choiceList.get(position).getEmail());
            return convertView;
        }
}

class ViewItem
{
    TextView ChoiceNameTextView;
    TextView EmailTextView;
}
