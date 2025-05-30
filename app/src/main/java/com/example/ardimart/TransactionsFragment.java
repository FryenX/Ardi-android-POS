package com.example.ardimart;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class TransactionsFragment extends Fragment {

    public TransactionsFragment() {
        // Required empty public constructor
    }

    public static TransactionsFragment newInstance(String param1, String param2) {
        TransactionsFragment fragment = new TransactionsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        LinearLayout btnInput = view.findViewById(R.id.btnInput);
        LinearLayout btnData = view.findViewById(R.id.btnData);

        btnInput.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), InputTransaction.class);
            startActivity(intent);
        });

        btnData.setOnClickListener(v -> {
            Fragment dataFragment = new DataTransactionsFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, dataFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;

    }
}