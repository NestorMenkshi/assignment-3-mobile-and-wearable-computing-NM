package com.example.stepappv5.ui.Home;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.stepappv5.R;
import com.example.stepappv5.StepAppOpenHelper;
import com.example.stepappv5.databinding.FragmentHomeBinding;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private TextView stepCountsView;
    private CircularProgressIndicator progressBar;

    private MaterialButtonToggleGroup toggleButtonGroup;

    // TODO 1: Create an object from Sensor class, to be used for Acc. sensor
    private Sensor accSensor;

    // TODO 2: Create an object from SensorManager class
    private SensorManager sensorManager;

    private StepCounterListener sensorListener;

    // TODO 17.1 (YOUR TURN): Create an object from Sensor class,  to be used for STEP_DETECTOR sensor
    private Sensor stepDetectorSensor;

    public Integer progressMultiplier = 100000;
    public Button buttonStart;
    public Button buttonStop;
    public int stepDetectorCounter = 0;
    public int accStepCounter = 0;
    public View root;
    Date cDate = new Date();
    String current_time = new SimpleDateFormat("yyyy-MM-dd").format(cDate);

    public boolean firstVisit = true;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        stepCountsView = (TextView) root.findViewById(R.id.counter);
        if (firstVisit){
            stepCountsView.setText("0");
        }else{
            Map<Integer, Integer> stepsByHour = StepAppOpenHelper.loadStepsByHour(getContext(), current_time);
            // Initialize a variable to hold the sum
            Integer steps_taken_today = 0;
            // Iterate through the values and add them to the sum
            for (Integer value : stepsByHour.values()) {
                steps_taken_today += value;
            }
            stepCountsView.setText(String.valueOf(steps_taken_today));
        }
        firstVisit = false;


        progressBar = (CircularProgressIndicator) root.findViewById(R.id.progressBar);
        progressBar.setMax(50*progressMultiplier);
        progressBar.setProgress(0);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        StepAppOpenHelper databaseOpenHelper = new StepAppOpenHelper(this.getContext());
        SQLiteDatabase database = databaseOpenHelper.getWritableDatabase();


        buttonStop = (Button) root.findViewById(R.id.stop_button);
        buttonStart = (Button) root.findViewById(R.id.start_button);
        buttonStart.setEnabled(true);
        buttonStop.setEnabled(false);

        buttonStart.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (accSensor != null)
                {
                    sensorListener = new StepCounterListener(stepCountsView,progressBar, database);
                    sensorManager.registerListener(sensorListener, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
                else
                {
                    Toast.makeText(getContext(), R.string.acc_sensor_not_available, Toast.LENGTH_LONG).show();
                }
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sensorManager.unregisterListener(sensorListener);
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
            }
        });

        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public class ProgressBarAnimation extends Animation {
        private CircularProgressIndicator progressBar;
        private float from;
        private float to;



        public ProgressBarAnimation(CircularProgressIndicator progressBar, float from, float to) {
            super();
            this.progressBar = progressBar;
            this.from = from;
            this.to = to;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float value = from + (to - from) * interpolatedTime;
            progressBar.setProgress((int) value);
        }

        public void animateProgressBarStep(Integer step){
            ProgressBarAnimation anim = new ProgressBarAnimation(progressBar, step*progressMultiplier, 0);
            stepCountsView.setText(Integer.toString(step));
            anim.setDuration(200);
            progressBar.startAnimation(anim);
        }
    }



    class  StepCounterListener implements SensorEventListener{

        private long lastSensorUpdate = 0;
        public int accStepCounter = 0;
        ArrayList<Integer> accSeries = new ArrayList<Integer>();
        ArrayList<String> timestampsSeries = new ArrayList<String>();
        private double accMag = 0;
        private int lastAddedIndex = 1;
        int stepThreshold = 6;

        TextView stepCountsView;

        CircularProgressIndicator progressBar;
        private SQLiteDatabase database;

        private String timestamp;
        private String day;
        private String hour;


        public StepCounterListener(TextView stepCountsView, CircularProgressIndicator progressBar,  SQLiteDatabase databse)
        {
            this.stepCountsView = stepCountsView;
            this.database = databse;
            this.progressBar = progressBar;
        }


        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            switch (sensorEvent.sensor.getType())
            {
                case Sensor.TYPE_LINEAR_ACCELERATION:

                    float x = sensorEvent.values[0];
                    float y = sensorEvent.values[1];
                    float z = sensorEvent.values[2];

                    long currentTimeInMilliSecond = System.currentTimeMillis();

                    long timeInMillis = currentTimeInMilliSecond + (sensorEvent.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;

                    // Convert the timestamp to date
                    SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                    jdf.setTimeZone(TimeZone.getTimeZone("GMT+2"));
                    String sensorEventDate = jdf.format(timeInMillis);




                    if ((currentTimeInMilliSecond - lastSensorUpdate) > 1000)
                    {
                        lastSensorUpdate = currentTimeInMilliSecond;
                        String sensorRawValues = "  x = "+ String.valueOf(x) +"  y = "+ String.valueOf(y) +"  z = "+ String.valueOf(z);
                        Log.d("Acc. Event", "last sensor update at " + String.valueOf(sensorEventDate) + sensorRawValues);
                    }


                    accMag = Math.sqrt(x*x+y*y+z*z);


                    accSeries.add((int) accMag);

                    // Get the date, the day and the hour
                    timestamp = sensorEventDate;
                    day = sensorEventDate.substring(0,10);
                    hour = sensorEventDate.substring(11,13);

                    Log.d("SensorEventTimestampInMilliSecond", timestamp);


                    timestampsSeries.add(timestamp);
                    peakDetection();

                    break;

            }


        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        private void peakDetection() {

            int windowSize = 20;
            /* Peak detection algorithm derived from: A Step Counter Service for Java-Enabled Devices Using a Built-In Accelerometer Mladenov et al.
             */
            int currentSize = accSeries.size(); // get the length of the series
            if (currentSize - lastAddedIndex < windowSize) { // if the segment is smaller than the processing window size skip it
                return;
            }

            List<Integer> valuesInWindow = accSeries.subList(lastAddedIndex,currentSize);
            List<String> timePointList = timestampsSeries.subList(lastAddedIndex,currentSize);
            lastAddedIndex = currentSize;

            for (int i = 1; i < valuesInWindow.size()-1; i++) {
                int forwardSlope = valuesInWindow.get(i + 1) - valuesInWindow.get(i);
                int downwardSlope = valuesInWindow.get(i) - valuesInWindow.get(i - 1);

                if (forwardSlope < 0 && downwardSlope > 0 && valuesInWindow.get(i) > stepThreshold) {
                    accStepCounter += 1;
                    Log.d("ACC STEPS: ", String.valueOf(accStepCounter));
                    stepCountsView.setText(String.valueOf(accStepCounter));
                    progressBar.setProgress(accStepCounter);

                    ContentValues databaseEntry = new ContentValues();
                    databaseEntry.put(StepAppOpenHelper.KEY_TIMESTAMP, timePointList.get(i));

                    databaseEntry.put(StepAppOpenHelper.KEY_DAY, this.day);
                    databaseEntry.put(StepAppOpenHelper.KEY_HOUR, this.hour);

                    database.insert(StepAppOpenHelper.TABLE_NAME, null, databaseEntry);

                    stepCountsView.setText(Integer.toString(accStepCounter));
                    ProgressBarAnimation anim = new ProgressBarAnimation((CircularProgressIndicator) progressBar, (accStepCounter-1)*progressMultiplier, accStepCounter*progressMultiplier);
                    anim.setDuration(300);
                    progressBar.startAnimation(anim);

                }
            }
        }


    }
}

