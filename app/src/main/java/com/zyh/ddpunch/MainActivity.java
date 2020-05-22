package com.zyh.ddpunch;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.zyh.ddpunch.bean.EmailBean;
import com.zyh.ddpunch.bean.EventBusBean;
import com.zyh.ddpunch.constant.Constant;
import com.zyh.ddpunch.email.EmailSender;
import com.zyh.ddpunch.service.MainAccessService;
import com.zyh.ddpunch.util.CommonUtils;
import com.zyh.ddpunch.util.TimeUtils;
import com.zyh.ddpunch.util.log.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.zyh.ddpunch.util.CommonUtils.getLocalVersionName;
import static com.zyh.ddpunch.util.PermissionUtils.isNotificationListenersEnabled;
import static com.zyh.ddpunch.util.PermissionUtils.openAccessSettingOn;


public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.version_text)
    TextView tvBuildCode;
    @BindView(R.id.tv_cur_time)
    TextView tvCurTime;

    private MediaProjectionManager mediaProjectionManager;
    private static MediaProjection mediaProjection = null;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private CompositeDisposable compositeDisposable;
    private boolean hasStart = false;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private Image image;
    private EmailBean emailBean;
    private boolean isSend = false;//是否发送邮件（当打卡未收到通知消息，定时发送邮件）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        compositeDisposable = new CompositeDisposable();

        PermissionUtils.permission(Constant.PERMISSIONS_STORAGE).callback(new PermissionUtils.SimpleCallback() {
            @Override
            public void onGranted() {
                initMediaProjectionManager();
            }

            @Override
            public void onDenied() {

            }
        }).request();
        openAccessSettingOn(this);
        checkTime();

        tvBuildCode.setText(String.format("版本号:%s", getLocalVersionName(getApplicationContext())));

        if (!isNotificationListenersEnabled(this)) {
            Toast.makeText(MainActivity.this,"请打开通知权限~", Toast.LENGTH_SHORT).show();
        }
        initDisplayData();
    }

    private void initDisplayData() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metric);
        mScreenDensity = metric.densityDpi;
        mScreenWidth = metric.widthPixels; // 屏幕实际宽度（PX）
        mScreenHeight = metric.heightPixels; // 屏幕实际高度（PX）
    }

    //获取截取屏幕权限
    private void initMediaProjectionManager() {
        if (mediaProjectionManager != null) {
            return;
        }
        mediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                Constant.REQUEST_MEDIA_PROJECTION);
    }

    // 设置截屏的宽高
    private void createImageReader() {
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);
    }

    /**
     * 最终得到当前屏幕的内容，注意这里mImageReader.getSurface()被传入，
     * 屏幕的数据也将会在ImageReader中的Surface中
     */
    private void virtualDisplay() {
        if (mediaProjection != null) {
            mVirtualDisplay = mediaProjection.createVirtualDisplay("screen-mirror",
                    mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
        }
    }

    private void checkTime() {
        Disposable mTimerSubscribe = Observable
                .interval(10, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        int week = TimeUtils.getWeekIndex(System.currentTimeMillis());
                        if ( week == 7 || week == 1) {
                            return;
                        }
                        startPunch();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
        compositeDisposable.add(mTimerSubscribe);
    }

    //打卡成功后有loading图，延迟10s执行
    private void startCapture(String title, String content) {
        Disposable mImageSubscribe = Observable.timer(10, TimeUnit.SECONDS)
                .observeOn(Schedulers.io())
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        // 这个方法已经被调用过，在获取另外一个新的image之前，请先关闭原有有的image
                        if (image != null) {
                            image.close();
                        }
                        image = mImageReader.acquireNextImage();
                        Bitmap bitmap = getScreenShot(image);
                        CommonUtils.saveBitmap(bitmap);
                    }
                })
                .delay(3, TimeUnit.SECONDS)
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        EmailSender sender = new EmailSender();
                        //设置服务器地址和端口，可以查询网络
                        sender.setProperties(Constant.HOST, Constant.PORT);
                        //分别设置发件人，邮件标题和文本内容
                        sender.setMessage(Constant.sendEmailInfo, title, content);
                        //设置收件人
                        sender.setReceiver(new String[]{Constant.receiveEmailInfo});
                        //添加附件换成你手机里正确的路径
                        sender.addAttachment(Constant.sdCardDir + "tmplName" + ".jpg");
                        //发送邮件
                        sender.sendEmail(Constant.HOST, Constant.sendEmailInfo, Constant.emailPassWord);
                    }
                })
                .delay(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        EventBus.getDefault().post(new EventBusBean(Constant.EVENT_BACK, 0, null));
                        Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
        compositeDisposable.add(mImageSubscribe);
    }

    //image转换bitmap
    private Bitmap getScreenShot(Image image) {
        if (image !=null) {
            return CommonUtils.buffer2Bitmap(image);
        }
        Log.e(TAG, "image is null, continue:");
        return null;
    }

    //检测是否到打卡时间
    private void startPunch() {
        if (TextUtils.equals(TimeUtils.millis2String(System.currentTimeMillis(), "HH:mm"), Constant.upJobTime) ||
                TimeUtils.millis2String(System.currentTimeMillis(), "HH:mm").equals(Constant.downJobTime)) {
            Toast.makeText(MainActivity.this,"开启自动打卡", Toast.LENGTH_SHORT).show();
            if (!hasStart) {
                hasStart = true;
                isSend = false;
                startService(new Intent(getApplicationContext(), MainAccessService.class));
            }
        } else {
            hasStart = false;
        }
        //检测是否到发送邮件时间
        if (TextUtils.equals(TimeUtils.millis2String(System.currentTimeMillis(), "HH:mm"), Constant.sendUpEmailTime)) {
            if (!isSend) {
                isSend = true;
                startCapture(getString(R.string.up_job),getString(R.string.up_job));
            }
        }
        if (TimeUtils.millis2String(System.currentTimeMillis(), "HH:mm").equals(Constant.sendDownEmailTime)) {
            if (!isSend) {
                isSend = true;
                startCapture(getString(R.string.down_job), getString(R.string.down_job));
            }
        }
        Log.d(TAG, "当前时间："+TimeUtils.millis2String(System.currentTimeMillis(), "HH:mm"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.REQUEST_MEDIA_PROJECTION:
                if (resultCode == RESULT_OK && data != null) {
                    mediaProjection = this.mediaProjectionManager.getMediaProjection(resultCode, data);
                    createImageReader();
                    virtualDisplay();
                } else {
                    finish();
                }
                break;
            case Constant.REQUEST_WINDOW_GRANT:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        finish();
                    } else {
                        initMediaProjectionManager();
                    }
                }
                break;
        }
    }

    @Subscribe
    public void onMessageEvent(final EventBusBean event) {
        switch (event.getReceiveType()) {
            case Constant.EVENT_PIC:
                isSend = true;
                emailBean = (EmailBean) event.getContent();
                startCapture(emailBean.getTitle(), emailBean.getContent());
                break;
            default:
                break;
        }
    }

    @OnClick({R.id.relative})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.relative:
                startService(new Intent(getApplicationContext(), MainAccessService.class));
//                startCapture(getString(R.string.up_job),getString(R.string.up_job));
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        EventBus.getDefault().unregister(this);
    }
}
