package com.hd;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reader.usbdevice.DeviceLib;
import com.reader.usbdevice.DeviceStatusCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;

public class RNHDModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;

    private static final int VID = 1839;    //IDR VID
    private static final int PID = 37077;     //IDR PID


    private CountDownLatch countdownLatch = null;

    private UsbManager mUsbManager = null;

    private DeviceLib mdev = null;

    private final String ACTION_USB_PERMISSION = "com.hd.USB_PERMISSION";

    public RNHDModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        reactContext.addLifecycleEventListener(this);
    }

    /**
     * 广播
     */
    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            log("监听到了广播");

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        OpenDevice();
                        Toast.makeText(reactContext, "OpenDevice", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(reactContext, "USB未授权", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    private void OpenDevice() {
        log("连接设备");

        startIDCardReader();

        if (mdev == null || !mdev.isOpen()) {
            log("设备未连接成功");
            return;
        }
        log("设备连接成功");
    }

    private void startIDCardReader() {
        log("startIDCardReader");
        try {
            log("DeviceLib");
            mdev = new DeviceLib(reactContext.getApplicationContext(), new DeviceStatusCallback() {
                @Override
                public void UsbAttach() {
                    log("UsbAttach");
                }

                @Override
                public void UsbDeAttach() {
                    log("UsbDeAttach");
                }
            });
            log("openDevice");
            mdev.openDevice(100);
            log("ICC_PosBeep");
            mdev.ICC_PosBeep((byte) 10);
        } catch (Exception e) {
            log("出错了");
            log(e.getMessage() + e);
        }

    }

    /**
     * 安卓输出日志
     *
     * @param msg 日志
     */
    private void showToast(String msg) {
        Toast.makeText(getReactApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 打印日志
     *
     * @param msg 日志
     */
    private void log(String msg) {
        WritableMap params = new WritableNativeMap();
        // 事件子类型
        params.putString("type", "nativeLog");
        // 日志内容
        params.putString("value", msg);
        sendEvent(reactContext, "console", params);
    }

    private void callback(boolean success, String msg, WritableMap data) {
        WritableMap params = new WritableNativeMap();
        params.putBoolean("success", success);
        params.putMap("data", data);
        params.putString("msg", msg);
        sendEvent(reactContext, "callback", params);
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public void onHostResume() {
        // Activity `onResume`
    }

    @Override
    public void onHostPause() {
        // Activity `onPause`
    }

    @Override
    public void onHostDestroy() {

    }

    @ReactMethod
    private void readIDCard() {
        if (mdev == null || !mdev.isOpen()) {
            log("设备未连接");
            return;
        }
        String pkName = reactContext.getPackageName();
        String show = "";
        int nRt = mdev.PICC_ReadIDCardMsg(pkName);
        if (nRt != 0) {
            log("身份证读取失败，ret=" + nRt);
            return;
        }
        WritableMap map = new WritableNativeMap();
        map.putInt("身份证类型", mdev.GetCardType());
        map.putString("中文姓名", mdev.getName());
        map.putString("英文姓名", mdev.getEnName());
        map.putString("国籍代码", mdev.getNationalityCode());
        map.putString("性别", mdev.getSex());
        map.putString("民族", mdev.getNation());
        map.putString("出生日期", mdev.getBirth());
        map.putString("住址", mdev.getAddress());
        map.putString("身份证号码", mdev.getIDNo());
        map.putString("签发机关", mdev.getDepartment());
        map.putString("有效日期", mdev.getEffectDate() + "至" + mdev.getExpireDate());
        map.putString("永久居留证号码", mdev.getIDNo());
        map.putString("通行证号码", mdev.getTXZHM());
        map.putString("通行证签发次数", mdev.getTXZQFCS());
//        Bitmap bm1 = mdev.getBmpfile();
//        String header = bitmapToBase64(bm1);
//        map.putString("头像", header);
        callback(true, "身份证读取成功", map);
    }

    @ReactMethod
    private void readSiCard() {
        if (mdev == null || !mdev.isOpen()) {
            log("设备未连接");
            return;
        }
        int nRt = -99;
        byte[] cardInfo = new byte[128];
        nRt = mdev.iReadSiCard((byte) 0x11, cardInfo);
        if (nRt != 0) {
            log("读卡失败:" + nRt);
            return;
        }
        WritableMap map = new WritableNativeMap();
        try {
            map.putString("社保卡数据", new String(cardInfo, "gbk"));
        } catch (UnsupportedEncodingException e) {
            log(e.getMessage());
        }

        callback(true, "社保卡读取成功", map);
    }


    @ReactMethod
    public void start() {
        log("开始连接设备");
        if (mdev != null && mdev.isOpen()) {
            callback(true, "设备连接成功", null);
            return;
        }

        //生成usb
        mUsbManager = (UsbManager) reactContext.getSystemService(Context.USB_SERVICE);

        //查看设备列表
        log("mUsbManager 列表长度 " + mUsbManager.getDeviceList().values().size());

        log("开始发送广播");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        reactContext.registerReceiver(mUsbReceiver, filter);
        log("广播发送完毕");

        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            log("设备列表mName " + device.getDeviceName());
            log("设备列表mVendorId " + device.getVendorId());
            log("设备列表mProductId " + device.getProductId());
            if (device.getVendorId() == VID && device.getProductId() == PID) {
                log("匹配到了读卡器");
                Intent intent = new Intent(ACTION_USB_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(reactContext, 0, intent, 0);
                mUsbManager.requestPermission(device, pendingIntent);
                return;
            }
        }
    }

    @ReactMethod
    public void stop() {
        mdev.closeDevice();

    }

    private void sendEvent(ReactApplicationContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @Override
    public String getName() {
        return "RNHDIdCard";
    }


}
