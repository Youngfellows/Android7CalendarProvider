package com.android.providers.calendar.permission;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class PermissionReq {

    private static String TAG = PermissionReq.class.getSimpleName();
    private static AtomicInteger sRequestCode = new AtomicInteger(0);
    private static SparseArray<Result> sResultArray = new SparseArray<>();
    private static Set<String> sManifestPermissionSet;//通过PMS查询AndroidManifest.xml中申明使用的权限列表

    public interface Result {

        void onGranted();

        void onDenied();
    }

    private Object mObject;
    private String[] mPermissions;//请求的权限数组
    private Result mResult;

    private PermissionReq(Object object) {
        mObject = object;
    }

    public static PermissionReq with(@NonNull Activity activity) {
        return new PermissionReq(activity);
    }

    public static PermissionReq with(@NonNull Fragment fragment) {
        return new PermissionReq(fragment);
    }

    public PermissionReq permissions(@NonNull String... permissions) {
        mPermissions = permissions;
        return this;
    }

    public PermissionReq result(@Nullable Result result) {
        mResult = result;
        return this;
    }

    public void request() {
        Activity activity = getActivity(mObject);
        if (activity == null) {
            throw new IllegalArgumentException(mObject.getClass().getName() + " is not supported");
        }

        initManifestPermission(activity);
        for (String permission : mPermissions) {
            if (!sManifestPermissionSet.contains(permission)) {
                if (mResult != null) {
                    mResult.onDenied();//请求的权限没有在AndroidManifest.xml中声明
                }
                return;
            }
        }

        // Android 6.0以下不需要申请权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (mResult != null) {
                mResult.onGranted();
            }
            return;
        }

        // 获取还未授权的权限列表
        List<String> deniedPermissionList = getDeniedPermissions(activity, mPermissions);
        if (deniedPermissionList.isEmpty()) {//全部都已经授权了
            if (mResult != null) {
                mResult.onGranted();
            }
            return;
        }

        int requestCode = genRequestCode();
        // 将list转化为数组
        String[] deniedPermissions = deniedPermissionList.toArray(new String[deniedPermissionList.size()]);
        requestPermissions(mObject, deniedPermissions, requestCode);
        sResultArray.put(requestCode, mResult);//将请求回调保存到map中
    }

    /**
     * 将在 Activity 或者 Fragment 的权限请求结构委托给PermissionReq处理
     *
     * @param requestCode
     * @param permissions
     * @param grantResults 权限是否授权成功的结果数组
     */
    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Result result = sResultArray.get(requestCode);//根据key获取Result回调对象

        if (result == null) {
            return;
        }

        sResultArray.remove(requestCode);

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                result.onDenied();//权限还是拒绝的
                return;
            }
        }
        result.onGranted();//请求权限成功
    }

    /**
     * Android 6.0以上请求权限
     *
     * @param object
     * @param permissions
     * @param requestCode
     */
    @TargetApi(Build.VERSION_CODES.M)
    private static void requestPermissions(Object object, String[] permissions, int requestCode) {
        if (object instanceof Activity) {
            ((Activity) object).requestPermissions(permissions, requestCode);
        } else if (object instanceof Fragment) {
            ((Fragment) object).requestPermissions(permissions, requestCode);
        }
    }

    /**
     * 获取还未授权的权限列表
     *
     * @param context
     * @param permissions 请求的权限数组
     * @return
     */
    private static List<String> getDeniedPermissions(Context context, String[] permissions) {
        List<String> deniedPermissionList = new ArrayList<>();
        for (String permission : permissions) {
            //检查该权限是否已经授权，未授权则添加到权限拒绝列表
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissionList.add(permission);
            }
        }
        return deniedPermissionList;
    }

    /**
     * 通过PMS查询AndroidManifest.xml中申明使用的权限列表
     *
     * @param context
     */
    private static synchronized void initManifestPermission(Context context) {
        if (sManifestPermissionSet == null) {
            sManifestPermissionSet = new HashSet<>();
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
                String[] permissions = packageInfo.requestedPermissions;
                Collections.addAll(sManifestPermissionSet, permissions);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static Activity getActivity(Object object) {
        if (object != null) {
            if (object instanceof Activity) {
                return (Activity) object;
            } else if (object instanceof Fragment) {
                return ((Fragment) object).getActivity();
            }
        }
        return null;
    }

    private static int genRequestCode() {
        return sRequestCode.incrementAndGet();
    }

    /**
     * 检查是否授权
     *
     * @param context
     * @return
     */
    public static boolean checkPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= 23 && context.getApplicationInfo().targetSdkVersion >= 23) {
            Log.d(TAG, "check permission ... ");
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "checkPermissions fail: " + perm);
                    return false;
                }
            }
        }
        return true;
    }
}
