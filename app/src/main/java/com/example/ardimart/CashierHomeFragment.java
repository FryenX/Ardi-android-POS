package com.example.ardimart;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.ardimart.config.SessionManager;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CashierHomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CashierHomeFragment extends Fragment {

    private TextView txtGreeting;

    public CashierHomeFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static CashierHomeFragment newInstance() {
        CashierHomeFragment fragment = new CashierHomeFragment();
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
        View view = inflater.inflate(R.layout.fragment_cashier_home, container, false);

        SessionManager session = new SessionManager(requireContext());
        session.checkLogin();

        txtGreeting = view.findViewById(R.id.txtGreeting);

        String name = session.getName();
        String level = session.getLevel();

        txtGreeting.setText("Home " + level + " " + name);

        return view;
    }
}