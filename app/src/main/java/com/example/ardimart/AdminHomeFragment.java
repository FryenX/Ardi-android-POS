package com.example.ardimart;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.ardimart.config.DatabaseHelper;
import com.example.ardimart.config.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminHomeFragment extends Fragment {
    private TextView txtGreeting;
    private TextView txtTotalSales, txtTotalProducts, txtTotalTransaction;
    private BarChart salesChart;
    private RecyclerView recyclerRecentTransactions;
    private SQLiteDatabase db;

    public AdminHomeFragment() {

    }

    public static AdminHomeFragment newInstance() {
        AdminHomeFragment fragment = new AdminHomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_home, container, false);

        // Session
        SessionManager session = new SessionManager(requireContext());
        session.checkLogin();

        txtGreeting = view.findViewById(R.id.txtGreeting);
        txtGreeting.setText("Home " + session.getLevel() + " " + session.getName());

        txtTotalSales = view.findViewById(R.id.txtTotalSales);
        txtTotalProducts = view.findViewById(R.id.txtTotalProducts);
        txtTotalTransaction = view.findViewById(R.id.txtTotalTransaction);
        salesChart = view.findViewById(R.id.salesChart);
        recyclerRecentTransactions = view.findViewById(R.id.recyclerRecentTransactions);
        recyclerRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));

        loadSummary();
        loadChart();
        loadRecentTransactions();

        return view;
    }

    private void loadSummary() {
        txtTotalSales.setText(String.format("%.2f", getTotalSales()));
        txtTotalProducts.setText(String.valueOf(getTotalProducts()));
        txtTotalTransaction.setText(String.valueOf(getTotalTransactions()));
    }

    private void loadChart() {
        List<BarEntry> entries = getSalesLast7Days();

        BarDataSet dataSet = new BarDataSet(entries, "Sales Last 7 Days");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        salesChart.setData(barData);

        // Set x-axis labels to days of week
        final String[] days = new String[] {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        XAxis xAxis = salesChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(1f);  // minimum x value
        xAxis.setAxisMaximum(7f);  // maximum x value
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelCount(days.length, true);  // force exactly 7 labels

        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value - 1;  // convert 1-based to 0-based index
                if (index >= 0 && index < days.length) {
                    return days[index];
                }
                return "";
            }
        };
        xAxis.setValueFormatter(formatter);

        // Optional: improve label appearance
        xAxis.setDrawGridLines(false);
        salesChart.getAxisLeft().setDrawGridLines(false);
        salesChart.getAxisRight().setEnabled(false);

        salesChart.getDescription().setEnabled(false);
        salesChart.invalidate();
    }


    private void loadRecentTransactions() {
        Cursor cursor = getRecentTransactions();

        List<RecentTransaction> transactionList = new ArrayList<>();
        while (cursor.moveToNext()) {
            String invoice = cursor.getString(0);
            String date = cursor.getString(1);
            double total = cursor.getDouble(2);

            transactionList.add(new RecentTransaction(invoice, date, total));
        }
        cursor.close();

        RecentTransactionAdapter adapter = new RecentTransactionAdapter(transactionList);
        recyclerRecentTransactions.setAdapter(adapter);
    }

    public double getTotalSales() {
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        dbHelper.copyDatabaseIfNeeded();
        db = dbHelper.getConnection();
        Cursor cursor = db.rawQuery("SELECT SUM(net_total) FROM transactions", null);
        if (cursor.moveToFirst()) {
            return cursor.getDouble(0);
        }
        return 0.0;
    }

    public int getTotalProducts() {
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        dbHelper.copyDatabaseIfNeeded();
        db = dbHelper.getConnection();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM products", null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return 0;
    }

    // 4. Get total transactions
    public int getTotalTransactions() {
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        dbHelper.copyDatabaseIfNeeded();
        db = dbHelper.getConnection();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM transactions", null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return 0;
    }

    public List<BarEntry> getSalesLast7Days() {
        List<BarEntry> entries = new ArrayList<>();

        Map<Integer, Float> salesMap = new HashMap<>();
        for (int i = 1; i <= 7; i++) {
            salesMap.put(i, 0f);
        }

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        dbHelper.copyDatabaseIfNeeded();
        db = dbHelper.getConnection();

        // Query sales totals grouped by day of week (1=Sunday, 2=Monday, ..., 7=Saturday)
        // SQLite strftime('%w', date) returns day of week where 0=Sunday, 1=Monday,... 6=Saturday
        Cursor cursor = db.rawQuery(
                "SELECT strftime('%w', date_time) as weekday, SUM(net_total) as total " +
                        "FROM transactions " +
                        "WHERE date_time >= date('now', 'weekday 1', '-6 days') " + // last Monday to Sunday
                        "GROUP BY weekday", null);

        while (cursor.moveToNext()) {
            int weekdaySql = Integer.parseInt(cursor.getString(0)); // 0=Sun,1=Mon,...6=Sat
            float total = (float) cursor.getDouble(1);

            // Convert SQLite weekday to 1=Mon,...7=Sun
            int weekdayChart = weekdaySql == 0 ? 7 : weekdaySql;

            salesMap.put(weekdayChart, total);
        }
        cursor.close();

        // Now create BarEntry list for Monday=1 ... Sunday=7
        for (int day = 1; day <= 7; day++) {
            entries.add(new BarEntry(day, salesMap.get(day)));
        }

        return entries;
    }

    // 6. Get recent 5 transactions
    public Cursor getRecentTransactions() {
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        dbHelper.copyDatabaseIfNeeded();
        db = dbHelper.getConnection();
        return db.rawQuery(
                "SELECT invoice, date_time, net_total FROM transactions ORDER BY date_time DESC LIMIT 5", null);
    }
}