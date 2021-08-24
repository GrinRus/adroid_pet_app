package ru.ryabov.pet.application;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lombok.Getter;
import ru.ryabov.pet.application.dao.Note;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private static final String TAG = "CustomAdapter";

    private List<Note> mDataSet;

    @Getter
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewFirst;
        private final TextView textViewSecond;
        private final TextView textViewThird;
        private final TextView textViewFourth;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");
                }
            });
            textViewFirst = (TextView) v.findViewById(R.id.textRowFirst);
            textViewSecond = (TextView) v.findViewById(R.id.textRowSecond);
            textViewThird = (TextView) v.findViewById(R.id.textRowThird);
            textViewFourth = (TextView) v.findViewById(R.id.textRowFourth);
        }
    }

    public CustomAdapter(List<Note> dataSet) {
        mDataSet = dataSet;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.text_row_item, viewGroup, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        Log.d(TAG, "Element " + position + " set.");
        viewHolder.getTextViewFirst().setText(mDataSet.get(position).getDateTime());
        viewHolder.getTextViewFourth().setText(mDataSet.get(position).getText());
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }
}
