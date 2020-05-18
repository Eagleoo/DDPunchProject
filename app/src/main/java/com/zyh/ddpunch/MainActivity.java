package com.zyh.ddpunch;

import android.content.Intent;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.zyh.ddpunch.service.MainAccessService;
import com.zyh.ddpunch.util.LogUtil;
import com.zyh.ddpunch.util.TimeUtils;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.zyh.ddpunch.util.CommonUtils.getLocalVersionName;
import static com.zyh.ddpunch.util.PermissionUtils.isNotificationListenersEnabled;
import static com.zyh.ddpunch.util.PermissionUtils.openAccessSettingOn;


public class MainActivity extends AppCompatActivity {
    @BindView(R.id.version_text)
    TextView tvBuildCode;
    @BindView(R.id.tv_cur_time)
    TextView tvCurTime;

    private CompositeDisposable compositeDisposable;
    private boolean hasStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        compositeDisposable = new CompositeDisposable();

        openAccessSettingOn(this);
        checkTime();

        tvBuildCode.setText("版本号:" + getLocalVersionName(getApplicationContext()));

        if (!isNotificationListenersEnabled(this)) {
            Toast.makeText(MainActivity.this,"请打开通知权限~", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkTime() {
        Disposable mTimerSubscribe = Observable
                .interval(10, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (TextUtils.equals(TimeUtils.millis2String(System.currentTimeMillis(), "HH:mm"), "07:49") ||
                                TimeUtils.millis2String(System.currentTimeMillis(), "HH:mm").contains("7:49")) {
                            Toast.makeText(MainActivity.this,"开启自动打卡", Toast.LENGTH_SHORT).show();
                            if (!hasStart) {
                                hasStart = true;
                                startService(new Intent(getApplicationContext(), MainAccessService.class));
                            }
                        }
                        LogUtil.E(TimeUtils.millis2String(System.currentTimeMillis(), "HH:mm"));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
        compositeDisposable.add(mTimerSubscribe);
    }

    @OnClick({R.id.relative})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.relative:

                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}
