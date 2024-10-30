package com.example.stepappv5.ui;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.example.stepappv5.R;
import com.yourpackage.database.StepDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class DayFragment extends Fragment {
    private AnyChartView anyChartView;
    private SQLiteDatabase stepDatabase;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day, container, false);

        anyChartView = view.findViewById(R.id.any_chart_view);
        StepDatabaseHelper dbHelper = new StepDatabaseHelper(getContext());
        stepDatabase = dbHelper.getReadableDatabase();

        loadStepData();
        return view;
    }

    private void loadStepData() {
        List<DataEntry> data = new ArrayList<>();

        // Retrieve step count by date for the last 7 days
        String query = "SELECT date, steps FROM stepapp WHERE date BETWEEN date('now', '-6 days') AND date('now')";
        Cursor cursor = stepDatabase.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                int steps = cursor.getInt(cursor.getColumnIndexOrThrow("steps"));
                data.add(new ValueDataEntry(date, steps));
            } while (cursor.moveToNext());
        }
        cursor.close();

        createBarChart(data);
    }

    private void createBarChart(List<DataEntry> data) {
        Cartesian column = AnyChart.column();

        column.data(data);
        column.title("Steps Over the Last Week");
        column.xAxis(0).title("Date");
        column.yAxis(0).title("Number of Steps");

        anyChartView.setChart(column);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (stepDatabase != null && stepDatabase.isOpen()) {
            stepDatabase.close();
        }
    }
}
