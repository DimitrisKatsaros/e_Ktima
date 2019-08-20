package com.gmail.katsaros.s.dimitris.e_ktima;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class CustomListAdapter extends ArrayAdapter<Card> {

    private List<Card> cardsList;
    private Context context;

    public CustomListAdapter(List<Card> cardsList, Context context) {
        super(context, R.layout.location_list_item, cardsList);
        this.cardsList = cardsList;
        this.context = context;
    }

    private static class CardsHolder {
        public TextView title;
        public CheckBox checkBox;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        CardsHolder holder;

        if (convertView == null) {

            holder = new CardsHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.location_list_item, parent, false);

            holder.title = (TextView) convertView.findViewById(R.id.location_name);
            holder.title.setOnClickListener((View.OnClickListener) context);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.location_checkbox);
            holder.checkBox.setOnCheckedChangeListener((MainActivity) context);
            convertView.setTag(holder);
        } else {

            holder = (CardsHolder) convertView.getTag();
        }

        holder.title.setText(cardsList.get(position).getTitle());
        holder.checkBox.setChecked(cardsList.get(position).isCheckbox());
        holder.checkBox.setTag(cardsList.get(position));

        return convertView;
    }
}
