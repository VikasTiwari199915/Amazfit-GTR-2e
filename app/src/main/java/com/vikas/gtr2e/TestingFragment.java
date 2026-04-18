package com.vikas.gtr2e;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel;
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType;
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptions;
import com.github.aachartmodel.aainfographics.aachartcreator.AAOptionsConstructor;
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement;
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAChartAxisType;
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AALabels;
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAMarker;
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle;
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAZonesElement;

import com.vikas.gtr2e.databinding.FragmentTestingBinding;
import com.vikas.gtr2e.db.AppDatabase;
import com.vikas.gtr2e.db.entities.BatterySampleEntity;
import com.vikas.gtr2e.utils.GTR2eManager;
import com.vikas.gtr2e.watchFeatureUtilities.GTR2eFirmwareUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestingFragment extends Fragment {

    public static final String TAG = "CHART";
    public static final String RGBA_GREEN = "rgba(76,175,80,0.3)";
    public static final String RGBA_BLUE = "rgba(33,150,243,0.3)";
    FragmentTestingBinding binding;

    public TestingFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTestingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        //get battery samples from db and chart it
        try(ExecutorService executor = Executors.newSingleThreadExecutor()){
            executor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<BatterySampleEntity> samples = db.batterySampleDao().getAll();
                requireActivity().runOnUiThread(()-> convertDbDataToAAChart(samples));
            });
        }

    }

    private void convertDbDataToAAChart(List<BatterySampleEntity> samples) {
        // Get the difference between UTC and your local time
        long localOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis());

        if (samples == null || samples.isEmpty()) return;

        samples.sort(Comparator.comparingLong(s -> s.timestamp));

        long now = System.currentTimeMillis();
        long start7Days = (now - (7 * 24 * 60 * 60 * 1000)) + localOffset;
        long currentLocalTime = now + localOffset;

        long gapThreshold = 30 * 60 * 1000; // 30 min

        String colorCharging = "#4CAF50";    // Green
        String colorDischarging = "#2196F3"; // Blue

        List<Object[]> dataList = new ArrayList<>();
        List<AAZonesElement> zonesList = new ArrayList<>();

        boolean lastState = false;
        boolean isFirstValid = true;
        long lastTimestamp = -1;
        int lastBatteryPercent = -1;

        for (BatterySampleEntity sample : samples) {
            long localTimestamp = sample.timestamp + localOffset;
            if (localTimestamp < start7Days) continue;

            Log.e(TAG, "Sample included: " + sample.batteryPercent + ", isCharging -> "+sample.isCharging + ", time : "+localTimestamp);

            // Init first valid state
            if (isFirstValid) {
                lastState = sample.isCharging;
                isFirstValid = false;
            }

            // GAP: break line completely
            if (lastTimestamp != -1
                    && lastBatteryPercent!=-1
                    && !(lastBatteryPercent == sample.batteryPercent || lastBatteryPercent+1 == sample.batteryPercent || lastBatteryPercent -1 == sample.batteryPercent)
                    && ((localTimestamp - lastTimestamp) > gapThreshold)) {
                dataList.add(new Object[]{(double) (localTimestamp - 1), null});
            }

            // Add actual point
            dataList.add(new Object[]{(double) localTimestamp, (double) sample.batteryPercent});

            // Zone transition (charging ↔ discharging)
            if (sample.isCharging != lastState) {
                Log.e(TAG, "Adding new zone as charging = "+sample.isCharging);
                zonesList.add(new AAZonesElement()
                        .value((double) localTimestamp)
                        .color(lastState ? colorCharging : colorDischarging)
                        .fillColor(lastState ? RGBA_GREEN : RGBA_BLUE)
                );
                lastState = sample.isCharging;
            }
            lastTimestamp = localTimestamp;
            lastBatteryPercent = sample.batteryPercent;
        }

        // Final zone
        Log.e(TAG, "Adding final zone, lastState of charging :" +lastState);
        zonesList.add(new AAZonesElement().color(lastState ? colorCharging : colorDischarging).fillColor(lastState ? RGBA_GREEN : RGBA_BLUE));

        Log.e(TAG, "Data size: " + dataList.size());
        Log.e(TAG, "Zones size: " + zonesList.size());

        if (dataList.size() < 2) {
            Log.e(TAG, "Not enough data to render chart");
            return;
        }

        AASeriesElement batterySeries = new AASeriesElement()
                .type(AAChartType.Area)
                .name("Battery")
                .data(dataList.toArray())
                .lineWidth(2f)
                .zoneAxis("x")
                .zones(zonesList.toArray(new AAZonesElement[]{}))
                .marker(new AAMarker().enabled(false))
                .threshold(null); // prevent fill dropping to 0

        String bgColor = resolveAttrColor(requireContext(), com.google.android.material.R.attr.colorTertiary);
        String textColor = resolveAttrColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant);

        AAChartModel aaChartModel = new AAChartModel()
                .chartType(AAChartType.Area)
                .backgroundColor(bgColor)
                .dataLabelsEnabled(false)
                .yAxisMax(100f)
                .yAxisMin(0f)
                .legendEnabled(false)
                .yAxisVisible(true)
                .yAxisTitle("% charge")
                .axesTextColor(textColor)
                .series(new AASeriesElement[]{batterySeries});

        AAOptions aaOptions = AAOptionsConstructor.INSTANCE.configureChartOptions(aaChartModel);

        AALabels xAxislabels = new AALabels().format("{value:%d %b}")
                .style(new AAStyle().color(textColor).fontSize(10f));

        AALabels yAxislabels = new AALabels().style(new AAStyle().color(textColor).fontSize(10f));

        // X Axis (time)
        if (aaOptions.getXAxis() != null) {
            aaOptions.getXAxis()
                    .type(AAChartAxisType.Datetime)
                    .min((double) start7Days)
                    .max((double) currentLocalTime)
                    .labels(xAxislabels)
                    .tickColor(textColor)
                    .lineColor(textColor)
                    .tickWidth(1)
                    .tickInterval((double) (24 * 60 * 60 * 1000));
        }

        if(aaOptions.getYAxis() != null) {
            aaOptions.getYAxis().labels(yAxislabels);
        }

        if (aaOptions.getPlotOptions() != null && aaOptions.getPlotOptions().getSeries() != null) {
            aaOptions.getPlotOptions().getSeries().connectNulls(false);
        }

        binding.aaChartView.post(() -> binding.aaChartView.aa_drawChartWithChartOptions(aaOptions));
    }

    private String resolveAttrColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);

        int color;
        if (typedValue.resourceId != 0) {
            color = ContextCompat.getColor(context, typedValue.resourceId);
        } else {
            color = typedValue.data;
        }

        return String.format("#%06X", (0xFFFFFF & color));
    }


}