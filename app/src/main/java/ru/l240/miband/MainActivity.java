package ru.l240.miband;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import io.realm.Realm;
import ru.l240.miband.miband.Battery;
import ru.l240.miband.miband.MiBand;
import ru.l240.miband.models.Profile;
import ru.l240.miband.models.UserMeasurement;
import ru.l240.miband.realm.RealmHelper;
import ru.l240.miband.retrofit.RequestTaskAddMeasurement;
import ru.l240.miband.utils.DateUtils;
import ru.l240.miband.utils.MedUtils;
import ru.l240.miband.utils.NotificationUtils;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity {

    // Test HR
    public static final byte COMMAND_SET_HR_SLEEP = 0x0;
    public static final byte COMMAND_SET__HR_CONTINUOUS = 0x1;
    public static final byte COMMAND_SET_HR_MANUAL = 0x2;
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String ADDRESS = "C8:0F:10:32:11:17";
    static final byte[] startHeartMeasurementManual = new byte[]{0x15, COMMAND_SET_HR_MANUAL, 1};
    static final byte[] stopHeartMeasurementManual = new byte[]{0x15, COMMAND_SET_HR_MANUAL, 0};
    static final byte[] stopHeartMeasurementContinuous = new byte[]{0x15, COMMAND_SET__HR_CONTINUOUS, 0};
    static final byte[] stopHeartMeasurementSleep = new byte[]{0x15, COMMAND_SET_HR_SLEEP, 0};
    private static final UUID UUID_MILI_SERVICE = UUID
            .fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_pair = UUID
            .fromString("0000ff0f-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_CONTROL_POINT = UUID
            .fromString("0000ff05-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_USER_INFO = UUID
            .fromString("0000ff04-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_REALTIME_STEPS = UUID
            .fromString("0000ff06-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_ACTIVITY = UUID
            .fromString("0000ff07-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_LE_PARAMS = UUID
            .fromString("0000ff09-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_DEVICE_NAME = UUID
            .fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CHAR_BATTERY = UUID
            .fromString("0000ff0c-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_SENSOR_DATA = UUID
            .fromString("0000ff0e-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_TEST = UUID
            .fromString("0000ff05-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_ALERT = UUID
            .fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID hRService = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_NOTIF = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_HR_MES = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");
    private ProgressBar pb;
    private TextView tv;
    private MiBand miBand = new MiBand();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothMi;
    private BluetoothGatt mGatt;
    private TextView tvBattery;
    private TextView tvSteps;
    private RelativeLayout relativeLayout;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        int state = 0;

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                pair();
            }

        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (characteristic.getService().getUuid().equals(hRService)) {
//                requestHR(UUID_NOTIF);
//                requestHR(UUID_HR_MES);
            } else {
                request(UUID_CHAR_BATTERY);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            byte[] b = characteristic.getValue();
            UUID uuid = characteristic.getUuid();
            Log.i(uuid.toString(), "state: " + state
                    + " value:" + Arrays.toString(b));

            // handle value
            if (uuid.equals(UUID_CHAR_REALTIME_STEPS)) {
                miBand.setmSteps(0xff & b[0] | (0xff & b[1]) << 8);
                BluetoothGattCharacteristic characteristicNotif = mGatt.getService(hRService).getCharacteristic(UUID_NOTIF);
                boolean b1 = mGatt.setCharacteristicNotification(characteristicNotif, true);
                BluetoothGattDescriptor descriptor = mGatt.getService(hRService).getCharacteristic(UUID_NOTIF).getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                boolean b3 = mGatt.writeDescriptor(descriptor);

                update();
            } else if (uuid.equals(UUID_CHAR_BATTERY)) {
                Battery battery = Battery.fromByte(b);
                miBand.setmBattery(battery);
                update();
                request(UUID_CHAR_REALTIME_STEPS);
            } else if (UUID_NOTIF.equals(uuid)) {
                byte[] value = characteristic.getValue();
                if (value.length == 2 && value[0] == 6) {
                    int hrValue = (value[1] & 0xff);
                    Log.d(TAG, String.valueOf(hrValue));
                    miBand.setmName(String.valueOf(hrValue));
                    Snackbar snackbar = Snackbar.make(relativeLayout, "Pulse is" + hrValue, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    update();

                } else {
                    Log.d(TAG, "RECEIVED DATA WITH LENGTH: " + value.length);
                    for (byte bb : value) {
                        Log.d(TAG, "DATA: " + String.format("0x%2x", bb));
                    }
                }
            }
        }
/*
        BluetoothGattCharacteristic characteristicUserInfo = getMiliService().getCharacteristic(UUID_CHAR_USER_INFO);
        characteristicUserInfo.setValue("F8663A5F0126B45500040049676F7200000000DC");
        boolean b4 = mGatt.writeCharacteristic(characteristicUserInfo);
        System.out.println("characteristicUserInfo" + b4);*/

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            UUID characteristicUUID = characteristic.getUuid();
            if (UUID_NOTIF.equals(characteristicUUID)) {
                byte[] value = characteristic.getValue();
                if (value.length == 2 && value[0] == 6) {
                    int hrValue = (value[1] & 0xff);
                    Log.d(TAG, String.valueOf(hrValue));
                    miBand.setmName(String.valueOf(hrValue));
                    Snackbar snackbar = Snackbar.make(relativeLayout, "Pulse is" + hrValue, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    UserMeasurement measurement = new UserMeasurement();
                    measurement.setMeasurementId(3);
                    measurement.setMeasurementDate(new Date());
                    measurement.setStrValue(miBand.getmName());
                    if (MedUtils.isNetworkConnected(getApplicationContext())) {
                        RequestTaskAddMeasurement addMeasurement = new RequestTaskAddMeasurement(getApplicationContext(), false, Collections.singletonList(measurement));
                        addMeasurement.execute();
                    } else {
                        RealmHelper.save(Realm.getInstance(getApplicationContext()), measurement);
                    }
                    try {
                        updateAlarm(hrValue);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    update();
                } else {
                    Log.d(TAG, "RECEIVED DATA WITH LENGTH: " + value.length);
                    for (byte b : value) {
                        Log.d(TAG, "DATA: " + String.format("0x%2x", b));
                    }
                }
            } else {
                byte[] value = characteristic.getValue();
                Log.d(TAG, "Unhandled characteristic changed: " + characteristicUUID);
                Log.d(TAG, "RECEIVED DATA WITH LENGTH: " + value.length);
                for (byte b : value) {
                    Log.d(TAG, "DATA: " + String.format("0x%2x", b));
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                System.out.println("HAVE PROBLEMS IN AUTH");
            } else if (status == BluetoothGatt.GATT_SUCCESS) {

                BluetoothGattCharacteristic characteristicNotif2 = mGatt.getService(hRService).getCharacteristic(UUID_NOTIF);
                characteristicNotif2.setValue(new byte[]{0x1, 0x0});
                boolean b2 = mGatt.writeCharacteristic(characteristicNotif2);
                System.out.println(b2);
            }
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        relativeLayout = (RelativeLayout) findViewById(R.id.rlMain);
        if (RealmHelper.getAll(Realm.getInstance(this), Profile.class).isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return;
        }
        pb = (ProgressBar) findViewById(R.id.progressBar);
        tv = (TextView) findViewById(R.id.tvMainActivitySearch);
        tvBattery = (TextView) findViewById(R.id.tvBattery);
        tvSteps = (TextView) findViewById(R.id.tvSteps);
        /*mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeColors(R.color.s1, R.color.s2, R.color.s3, R.color.s4);*/
        miBand.setmBTAddress(ADDRESS);
        mBluetoothManager = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                tv.setText("Пожалуйста включите Bluetooth!");
                receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (mBluetoothAdapter.isEnabled()) {
                            mBluetoothMi = mBluetoothAdapter.getRemoteDevice(ADDRESS);
                            mGatt = mBluetoothMi.connectGatt(MainActivity.this, true, mGattCallback);
                            mGatt.connect();
                            tv.setText("ИЩУ MIBAND 1S...");
                        } else {
                            tv.setText("Пожалуйста включите Bluetooth!");
                        }
                    }
                };
                this.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            } else {
                mBluetoothMi = mBluetoothAdapter.getRemoteDevice(ADDRESS);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothMi != null) {
            mGatt = mBluetoothMi.connectGatt(this, true, mGattCallback);
            mGatt.connect();
        }
    }

    private BluetoothGattService getMiliService() {
        return mGatt.getService(UUID_MILI_SERVICE);

    }

    private void pair() {
        BluetoothGattCharacteristic chrt = getMiliService().getCharacteristic(
                UUID_CHAR_pair);

        chrt.setValue(new byte[]{2});

        mGatt.writeCharacteristic(chrt);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText("Браслет найден. Синхронизиоруюсь...");
            }
        });
    }

    private void request(UUID what) {
        mGatt.readCharacteristic(getMiliService().getCharacteristic(what));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGatt != null) {
            if (receiver != null)
                unregisterReceiver(receiver);
            mGatt.disconnect();
            mGatt.close();
            mGatt = null;
        }
    }

    private void requestHR(UUID what) {
        mGatt.readCharacteristic(mGatt.getService(hRService).getCharacteristic(what));
    }
/*
    @Override
    public void onRefresh() {
        Log.d(TAG, "refreshing");
        BluetoothGattCharacteristic characteristicCP = mGatt.getService(hRService).getCharacteristic(UUID_HR_MES);
        characteristicCP.setValue(startHeartMeasurementManual);
    }*/

    private void update() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (miBand.getmName() != null) {
                    tv.setText("Пульс: " + miBand.getmName());
                } else {
                    tv.setText("Пульс не измерян.");
                }
                tvBattery.setText("Заряд: " + String.valueOf(miBand.getmBattery().mBatteryLevel));
                tvSteps.setText("Шагов: " + miBand.getmSteps());
                pb.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void refresh(View view) {
        Log.d(TAG, "refreshing");
        BluetoothGattCharacteristic characteristicCP = mGatt.getService(hRService).getCharacteristic(UUID_HR_MES);
        for (BluetoothGattService service : mGatt.getServices()) {
            System.out.println(service.getUuid());
            for (BluetoothGattCharacteristic ch : service.getCharacteristics()) {
                System.out.println(ch.getUuid());
                System.out.println(ch.getValue());
                for (BluetoothGattDescriptor ds : ch.getDescriptors()) {
                    System.out.println(ds.getUuid());
                    System.out.println(ds.getValue());
                }
            }
        }
//        BluetoothGattCharacteristic characteristicNot = mGatt.getService(hRService).getCharacteristic(UUID_NOTIF);
//        characteristicNot.setValue(new byte[]{0x1, 0x0});
//        mGatt.writeCharacteristic(characteristicNot);
        characteristicCP.setValue(startHeartMeasurementManual);
        boolean b = mGatt.writeCharacteristic(characteristicCP);
        try {
            NotificationUtils.getInstance(this).cancelAllAlarmNotify();
            NotificationUtils.getInstance(this).createAlarmNotify(DateUtils.addMinutes(new Date(), 1), NotificationUtils.MIN_5);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        tv.setText("Измеряю пульс..." + b);
    }

    public void vibrate(View view) {
        BluetoothGattCharacteristic characteristic1 = mGatt.getService(UUID_ALERT).getCharacteristic(UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb"));
        characteristic1.setValue(new byte[]{0x01});
        mGatt.writeCharacteristic(characteristic1);

    }

    private void updateAlarm(Integer value) throws ParseException {
        if (value >= 60 && value <= 90) {
            NotificationUtils.getInstance(this).cancelAllAlarmNotify();
            NotificationUtils.getInstance(this).createAlarmNotify(DateUtils.addMinutes(new Date(), 1), NotificationUtils.MIN_5);
            return;
        }
        if ((value >= 91 && value <= 120) || (value >= 46 && value <= 59)) {
            NotificationUtils.getInstance(this).cancelAllAlarmNotify();
            NotificationUtils.getInstance(this).createAlarmNotify(DateUtils.addMinutes(new Date(), 1), NotificationUtils.MIN_2);
            return;
        }
        if (value >= 121 || value <= 45) {
            NotificationUtils.getInstance(this).cancelAllAlarmNotify();
            NotificationUtils.getInstance(this).createAlarmNotify(DateUtils.addMinutes(new Date(), 1), NotificationUtils.MIN_1);
            return;
        }
    }
}