package com.vikas.gtr2e.interfaces;

public interface RenewTokenCallback {
    void onTokenRenewSuccess(String token);
    void onTokenRenewFailure(String error);
}
