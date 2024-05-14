package com.android.providers.calendar.permission;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;

import com.android.providers.calendar.R;

public class PermissionActivity extends Activity {

    private String TAG = this.getClass().getSimpleName();

    public static void start(Context context) {
        Intent intent = new Intent(context, PermissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        if (PermissionReq.checkPermissions(this, PermissionConfigure.CALENDAR_PERMISSIONS)) {
            finish();
        } else {
            PermissionReq.with(this)
                    .permissions(PermissionConfigure.CALENDAR_PERMISSIONS)
                    .result(new PermissionReq.Result() {
                        @Override
                        public void onGranted() {
                            Log.i(TAG, "onGranted:: ");

                        }

                        @Override
                        public void onDenied() {
                            Log.i(TAG, "onDenied:: ");
                        }
                    })
                    .request();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionReq.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
