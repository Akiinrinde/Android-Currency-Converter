package com.nicoqueijo.android.currencyconverter.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nicoqueijo.android.currencyconverter.R;

public class ActiveExchangeRatesFragment extends Fragment {

    private static final String TAG = "ActiveExchangeRates";

    FloatingActionButton mFloatingActionButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View mView = inflater.inflate(R.layout.fragment_active_exchange_rates, container, false);
        mFloatingActionButton = mView.findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(mView.findViewById(R.id.fab), "FAB clicked",
                        Snackbar.LENGTH_SHORT).show();
            }
        });
        return mView;
    }
}