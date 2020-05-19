package com.zyh.ddpunch;

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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        compositeDisposable = new CompositeDisposable();

        PermissionUtils.permission(Constant.PERMISSIONS_STORAGE).request();
        openAccessSettingOn(this);
        checkTime();

        tvBuildCode.setText("版本号:" + getLocalVersionName(getApplicationContext()));

        if (!isNotificationListenersEnabled(this)) {
            Toast.makeText(MainActivity.this,"请打开通知权限~", Toast.LENGTH_SHORT).show();
        }
        initDisplayData();
        initMediaProjectionManager();
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

    //初始化时获取截图需要一定时间，这里延迟500毫秒执行
    private void startCapture() {
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
                        sendEmail(emailBean.getTitle(), emailBean.getContent());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
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
                startService(new Intent(getApplicationContext(), MainAccessService.class));
            }
        } else {
            hasStart = false;
        }
        Log.d(TAG, "当前时间："+TimeUtils.millis2String(System.currentTimeMillis(), "HH:mm"));
    }

    private void sendEmail(final String title, final String content) {
        //耗时操作要起子线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
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
                    //sender.setMessage("你的163邮箱账号", "EmailS//ender", "Java Mail ！");这里面两个邮箱账号要一致
                    sender.sendEmail(Constant.HOST, Constant.sendEmailInfo, Constant.emailPassWord);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
                emailBean = (EmailBean) event.getContent();
                startCapture();
                break;
            default:
                break;
        }
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
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        EventBus.getDefault().unregister(this);
    }
}
