package com.android.providers.calendar.permission;

import android.Manifest;

public class PermissionConfigure {

    /**
     * 1. 需要进行检测的权限数组
     * 2. 获取权限集中需要申请权限的列表
     */
    public static String[] CALENDAR_PERMISSIONS = {
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
}
