package saqibr.fitnesstracking;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;

public class Progress extends AppCompatActivity {

    private static final String TAG = "FitnessTrackerApi";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;
    private static final String STATE_WEIGHT_DATA_SETS = "WeightDataSets";

    private LineChart weightChart;
    private DataReadResponse recentReadResponse;
    private List<Entry> weightSummaryEntries;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_dashboard:
                    return true;
                case R.id.navigation_notifications:
                    return true;
            }
            return false;
        }
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.i(TAG, "Saved weight summaries");

        ArrayList<Entry> temp = new ArrayList<>(this.weightSummaryEntries);
        savedInstanceState.putParcelableArrayList(STATE_WEIGHT_DATA_SETS, temp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        weightChart = (LineChart) findViewById(R.id.weightChart);

        weightChart.getDescription().setEnabled(false);
        weightChart.setTouchEnabled(true);
        weightChart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging

        weightChart.setDragEnabled(true);
        weightChart.setScaleEnabled(true);
        weightChart.setDrawGridBackground(false);
        weightChart.setHighlightPerDragEnabled(true);
        weightChart.setDragXEnabled(true);

        WeightMarkerView wmv = new WeightMarkerView(this, R.layout.custom_marker_view);

        wmv.setChartView(weightChart);
        weightChart.setMarker(wmv);

        weightChart.setBackgroundColor(Color.WHITE);
        weightChart.setViewPortOffsets(0f, 0f, 0f, 0f);

        Legend l = weightChart.getLegend();
        l.setEnabled(true);

        XAxis xAxis = weightChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.rgb(255, 192,56));
        xAxis.setLabelRotationAngle(50f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new DateAxisValueFormatter());
        xAxis.enableGridDashedLine(10f, 10f, 0f);

        YAxis leftAxis = weightChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));
        leftAxis.enableAxisLineDashedLine(10f, 10f, 0f);

        YAxis rightAxis = weightChart.getAxisRight();
        rightAxis.setEnabled(false);

        if (savedInstanceState != null) {
            Log.i(TAG, "Using restored state.");
            this.weightSummaryEntries = savedInstanceState.getParcelableArrayList(STATE_WEIGHT_DATA_SETS);
            this.addWeightSummaryLineData();
        } else {
            this.getWeightData();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                this.getWeightData();
            }
        }
    }

    protected void getWeightData() {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_WEIGHT_SUMMARY, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_BODY_FAT_PERCENTAGE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_CUMULATIVE, FitnessOptions.ACCESS_READ)
                .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions
            );
        } else {
            this.accessGoogleFit();
        }
    }

    public void printData(DataReadResponse dataReadResult) {
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(
                    TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size()
            );

            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());

            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }
        }

        List<DataSet> recentWeights = GetDataSetsFromResponse(this.recentReadResponse);
        for (DataSet dataSet : recentWeights) {
            dumpDataSet(dataSet);
        }
    }

    private static void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        Log.i(TAG, "Number of returned DataPoints is: " + dataSet.getDataPoints().size());
        DateFormat dateFormat = getDateInstance();

        for (com.google.android.gms.fitness.data.DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tField: " + field.getName() + " Value: " + (dp.getValue(field).asFloat() * 2.2046));
            }
        }
    }

    private static List<DataSet> GetDataSetsFromResponse(DataReadResponse readResponse) {
        List<Bucket> buckets = readResponse.getBuckets();

        if (buckets.size() > 0) {
            List<DataSet> allDataSets = new ArrayList<>();
            for (Bucket bucket : buckets) {
                allDataSets.addAll(bucket.getDataSets());
            }
            return allDataSets;
        }

        return readResponse.getDataSets();
    }

    private void addWeightSummaryLineData() {
        LineDataSet weightSet = new LineDataSet(this.weightSummaryEntries, "Average Weight");
        weightSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        weightSet.setColor(Color.DKGRAY);
        weightSet.setValueTextSize(9f);
        weightSet.setValueTextColor(Color.DKGRAY);
        weightSet.setLineWidth(3f);
        weightSet.setDrawCircles(true);
        weightSet.setDrawValues(true);
        weightSet.setHighLightColor(Color.rgb(244, 117, 117));
        weightSet.setDrawCircleHole(false);

        weightSet.enableDashedHighlightLine(10f, 5f, 0f);

        this.weightChart.getXAxis().setAxisMaximum(weightSet.getXMax() + (1000 * 60 * 60 * 24 * 3));
        this.weightChart.getXAxis().setAxisMinimum(weightSet.getXMin() - (1000 * 60 * 60 * 24 * 3));
        this.weightChart.getAxisLeft().setAxisMaximum(weightSet.getYMax() + 10);
        this.weightChart.getAxisLeft().setAxisMinimum(weightSet.getYMin() - 10);

        if (this.weightChart.getData() != null &&
                this.weightChart.getData().getDataSetCount() > 0) {
            LineDataSet existingDataSet = (LineDataSet) this.weightChart.getData().getDataSetByIndex(0);
            existingDataSet.setValues(weightSummaryEntries);
            existingDataSet.notifyDataSetChanged();
            this.weightChart.getData().notifyDataChanged();
            this.weightChart.notifyDataSetChanged();
        } else {
            LineData weightData = new LineData(weightSet, new LineDataSet(new ArrayList<Entry>(), "Temp"));
            this.weightChart.setData(weightData);
            this.weightChart.invalidate();
        }
    }

    private void addWeightLine(DataReadResponse readResponse) {
        List<DataSet> weightDataSets = GetDataSetsFromResponse(readResponse);
        List<Entry> weightEntries = this.getAverageWeightEntries(weightDataSets);
        LineDataSet weightSet = new LineDataSet(weightEntries, "Weight");
        weightSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        weightSet.setColor(ColorTemplate.getHoloBlue());
        weightSet.setLineWidth(1.5f);
        weightSet.setDrawCircles(true);
        weightSet.setDrawValues(false);
        weightSet.setFillAlpha(80);
        weightSet.setFillColor(ColorTemplate.getHoloBlue());
        weightSet.setDrawFilled(true);
        weightSet.setHighLightColor(Color.rgb(244, 117, 117));
        weightSet.setDrawCircleHole(false);

        this.weightChart.getXAxis().setAxisMaximum(weightSet.getXMax() + (1000 * 60 * 60 * 24 * 3));
        this.weightChart.getXAxis().setAxisMinimum(weightSet.getXMin() - (1000 * 60 * 60 * 24 * 3));
        this.weightChart.getAxisLeft().setAxisMaximum(weightSet.getYMax() + 3);
        this.weightChart.getAxisLeft().setAxisMinimum(weightSet.getYMin() - 3);

        if (this.weightChart.getData() != null &&
                this.weightChart.getData().getDataSetCount() > 0) {
            LineDataSet existingDataSet = (LineDataSet) this.weightChart.getData().getDataSetByIndex(1);
            existingDataSet.setValues(weightEntries);
            existingDataSet.notifyDataSetChanged();
            this.weightChart.getData().notifyDataChanged();
            this.weightChart.notifyDataSetChanged();
        } else {
            LineData weightData = new LineData(new LineDataSet(new ArrayList<Entry>(), "Temp"), weightSet);
            this.weightChart.setData(weightData);
        }
    }

    private List<Entry> getAverageWeightEntries(List<DataSet> dataSets) {
        List<Entry> results = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        float lastAverageWeight = 0f;

        for (DataSet dataSet : dataSets) {
            for (com.google.android.gms.fitness.data.DataPoint dp : dataSet.getDataPoints()) {
                long startTime = dp.getStartTime(TimeUnit.MILLISECONDS);
                long endTime = dp.getEndTime(TimeUnit.MILLISECONDS);
                long diffDays = (endTime - startTime) / (1000 * 60 * 60 * 24);
                int days = Math.round(diffDays / 2.0f);
                cal.setTimeInMillis(startTime);
                cal.add(Calendar.DATE, days);
                float averageWeight = dp.getValue(Field.FIELD_AVERAGE).asFloat() * 2.2046f;
                Entry weightEntry = lastAverageWeight > 0
                        ? new Entry(cal.getTimeInMillis(), averageWeight, averageWeight - lastAverageWeight)
                        : new Entry(cal.getTimeInMillis(), averageWeight);
                results.add(weightEntry);
                lastAverageWeight = averageWeight;
            }
        }

        return results;
    }

    private void accessGoogleFit() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -12);
        long startTime = cal.getTimeInMillis();
        cal.setTime(now);
        long recentEndTime = cal.getTimeInMillis();

        DateFormat dateFormat = getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        Log.i(TAG, "Recent Range Start: " + dateFormat.format(endTime));
        Log.i(TAG, "Recent Range End: " + dateFormat.format(recentEndTime));

        final DataReadRequest readRequest =
                new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(7, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadRequest recentAverageWeightRequest =
                new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(10, TimeUnit.DAYS)
                .setTimeRange(endTime, recentEndTime, TimeUnit.MILLISECONDS)
                .build();

        this.weightChart.removeAllViews();

        final HistoryClient historyClient = Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this));
        historyClient
            .readData(recentAverageWeightRequest)
            .addOnSuccessListener(
                    new OnSuccessListener<DataReadResponse>() {
                        @Override
                        public void onSuccess(DataReadResponse dataReadResponse) {
                            recentReadResponse = dataReadResponse;
                        }
                    }
            )
            .addOnFailureListener(
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "There was a problem reading the data.", e);
                        }
                    }
            ).addOnCompleteListener(
                    new OnCompleteListener<DataReadResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<DataReadResponse> task) {
                            historyClient
                                    .readData(readRequest)
                                    .addOnSuccessListener(
                                            new OnSuccessListener<DataReadResponse>() {
                                                @Override
                                                public void onSuccess(DataReadResponse dataReadResponse) {
                                                    List<DataSet> recentWeights = GetDataSetsFromResponse(recentReadResponse);
                                                    List<DataSet> weightDataSets = GetDataSetsFromResponse(dataReadResponse);
                                                    weightDataSets.addAll(recentWeights);
                                                    weightSummaryEntries = getAverageWeightEntries(weightDataSets);

                                                    addWeightSummaryLineData();
                                                    printData(dataReadResponse);
                                                }
                                            }
                                    )
                                    .addOnFailureListener(
                                            new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e(TAG, "There was a problem reading the data.", e);
                                                }
                                            }
                                    );
                        }
                    }
        );



        final DataReadRequest weightRequest =
            new DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, recentEndTime, TimeUnit.MILLISECONDS)
                    .build();

//        historyClient
//                .readData(weightRequest)
//                .addOnSuccessListener(
//                        new OnSuccessListener<DataReadResponse>() {
//                            @Override
//                            public void onSuccess(DataReadResponse readResponse) {
//                                addWeightLine(readResponse);
//                                printData(readResponse);
//                            }
//                        }
//                )
//                .addOnFailureListener(
//                        new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.e(TAG, "There was  a problem reading the data.", e);
//                            }
//                        }
//                );
    }

}
