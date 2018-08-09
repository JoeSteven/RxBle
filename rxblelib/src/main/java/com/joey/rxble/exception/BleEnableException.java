package com.joey.rxble.exception;

import com.polidea.rxandroidble2.exceptions.BleException;

/**
 * Description:
 * author:Joey
 * date:2018/8/9
 */
public class BleEnableException extends BleException{
    private int reason = -1;
    public static final int PERMISSION_DENIED = 0;
    public static final int ENABLE_FAILED = 1;

    public BleEnableException(int reason, String message) {
        super(message);
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }
}
