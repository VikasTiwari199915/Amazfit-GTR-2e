package com.vikas.gtr2e.interfaces;

public interface FirmwareUpdateListener {

    void onProgress(int progress);

    void onEnablingFw();

    void onFinish();

    void onError(String message);
}