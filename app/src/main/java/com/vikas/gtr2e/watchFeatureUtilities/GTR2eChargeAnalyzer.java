package com.vikas.gtr2e.watchFeatureUtilities;

import com.vikas.gtr2e.beans.BatterySample;
import com.vikas.gtr2e.enums.ChargingPhase;

import java.util.LinkedList;
import java.util.Queue;

public class GTR2eChargeAnalyzer {

    public static class Result {
        public long etaMillis;
        public float rate; // % per minute
        public ChargingPhase phase;
        public float confidence; // 0.0 to 1.0

        public Result(long etaMillis, float rate, ChargingPhase phase, float confidence) {
            this.etaMillis = etaMillis;
            this.rate = rate;
            this.phase = phase;
            this.confidence = confidence;
        }
    }

    private final Queue<BatterySample> BatterySamples = new LinkedList<>();
    private final int MAX_BatterySampleS = 10;

    private boolean isCharging = false;

    // For rate comparison
    private float lastComputedRate = -1f;

    public void onChargingStateChanged(boolean charging) {
        if (this.isCharging != charging) {
            this.isCharging = charging;
            reset();
        }
    }

    public void addBatterySample(int batteryPercent) {
        if (!isCharging) return;

        long now = System.currentTimeMillis();

        if (!BatterySamples.isEmpty()) {
            BatterySample last = ((LinkedList<BatterySample>) BatterySamples).getLast();

            // Ignore invalid or duplicate data
            if (batteryPercent <= last.getBattery()) return;
        }

        BatterySamples.add(new BatterySample(now, batteryPercent));

        if (BatterySamples.size() > MAX_BatterySampleS) {
            BatterySamples.poll();
        }
    }

    public Result analyze() {
        if (BatterySamples.size() < 2) {
            return new Result(-1, 0, ChargingPhase.UNKNOWN, 0);
        }

        float rate = calculateRate();
        if (rate <= 0) {
            return new Result(-1, 0, ChargingPhase.UNKNOWN, 0);
        }

        int currentBattery = ((LinkedList<BatterySample>) BatterySamples).getLast().getBattery();
        int remaining = 100 - currentBattery;

        long etaMillis = (long) ((remaining / rate) * 60 * 1000);

        ChargingPhase phase = detectPhase(currentBattery, rate);
        float confidence = calculateConfidence();

        lastComputedRate = rate;

        return new Result(etaMillis, rate, phase, confidence);
    }

    private float calculateRate() {
        BatterySample first = ((LinkedList<BatterySample>) BatterySamples).getFirst();
        BatterySample last = ((LinkedList<BatterySample>) BatterySamples).getLast();

        float deltaBattery = last.getBattery() - first.getBattery();
        float deltaTimeMinutes = (last.getTime() - first.getTime()) / 60000f;

        if (deltaTimeMinutes <= 0) return -1;

        return deltaBattery / deltaTimeMinutes;
    }

    private ChargingPhase detectPhase(int battery, float rate) {
        if (battery >= 90) return ChargingPhase.TRICKLE;
        if (battery >= 80) return ChargingPhase.SLOW;

        if (lastComputedRate > 0 && rate < lastComputedRate * 0.7f) {
            return ChargingPhase.SLOW;
        }

        if (rate > 0.8f) return ChargingPhase.FAST;

        return ChargingPhase.NORMAL;
    }

    private float calculateConfidence() {
        if (BatterySamples.size() < 3) return 0.3f;
        if (BatterySamples.size() < 5) return 0.6f;

        // Stable if battery change is consistent
        BatterySample first = ((LinkedList<BatterySample>) BatterySamples).getFirst();
        BatterySample last = ((LinkedList<BatterySample>) BatterySamples).getLast();

        float deltaBattery = last.getBattery() - first.getBattery();

        if (deltaBattery < 3) return 0.5f;

        return 0.9f;
    }

    public void reset() {
        BatterySamples.clear();
        lastComputedRate = -1f;
    }
}