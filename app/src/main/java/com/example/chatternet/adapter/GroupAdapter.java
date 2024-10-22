package com.example.chatternet.adapter;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.chatternet.R;
import java.util.List;

public class GroupAdapter extends ArrayAdapter<Pair<String, String>> {

    public GroupAdapter(Context context, List<Pair<String, String>> groupList) {
        super(context, 0, groupList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_group, parent, false);
        }

        TextView groupNameTextView = convertView.findViewById(R.id.text_view_group_name);
        TextView groupEmojiTextView = convertView.findViewById(R.id.text_view_group_emoji);

        Pair<String, String> group = getItem(position);
        groupNameTextView.setText(group.first);
        groupEmojiTextView.setText(group.second);

        return convertView;
    }
}
