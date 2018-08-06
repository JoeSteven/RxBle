package com.joey.rxble.permission;

import android.support.annotation.NonNull;

/**
 * /**
 * Description: call back for permission grant
 * author:Joey
 * date:2018/7/30
 */

public interface PermissionListener {

    /**
     * 通过授权
     * @param permission
     */
    void permissionGranted(@NonNull String[] permission);

    /**
     * 拒绝授权
     * @param permission
     */
    void permissionDenied(@NonNull String[] permission);
}
