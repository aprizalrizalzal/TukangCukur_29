package com.bro.barbershop.utils.recyclerView;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewEmptyData extends RecyclerView.AdapterDataObserver {

    private final TextView textView;
    private final RecyclerView recyclerView;

    public RecyclerViewEmptyData(RecyclerView recyclerView, TextView textView) {
        this.recyclerView = recyclerView;
        this.textView = textView;
        checkIfEmpty();
    }

    private void checkIfEmpty() {
        if (textView != null && recyclerView.getAdapter() != null) {
            boolean emptyViewVisible = recyclerView.getAdapter().getItemCount() == 0;
            textView.setVisibility(emptyViewVisible ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(emptyViewVisible ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onChanged() {
        checkIfEmpty();
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        checkIfEmpty();
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        checkIfEmpty();
    }
}
