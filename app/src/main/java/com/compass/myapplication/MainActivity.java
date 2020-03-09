package com.compass.myapplication;

import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.compass.myapplication.gps.GPSLocationListener;
import com.compass.myapplication.gps.GPSLocationManager;
import com.compass.myapplication.gps.GPSProviderStatus;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int UPDATE_UI = 0;
    private TextView mTvOrientation;
    private TextView mTemperaturet;
    private RelativeLayout mRlImage;
    private TextView mTvLgt;
    private TextView mTvAltitude;
    private SensorManager sm=null;
    private Sensor aSensor=null;  //加速度
    private Sensor mSensor=null;  //重力
    private Sensor mTemperatureSensor=null; //温度
    private GPSLocationManager gpsLocationManager;
    private ArrayList<Double> mHeight = new ArrayList<>();
    float[] accelerometerValues=new float[3];
    float[] magneticFieldValues=new float[3];
    float[] values=new float[3];
    float[] R=new float[9];
    private long mLastTime = 0;
    private int fromDegress = 0;
    private Handler mHandle = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_UI){
                mTvOrientation.setText(getOrientation(msg.arg1));
                int value = -msg.arg1;
                ObjectAnimator mObjectAnimator = ObjectAnimator.ofFloat(mRlImage, "rotation", fromDegress, value);
                mObjectAnimator.setDuration(150);
                mObjectAnimator.start();
                fromDegress = value;
           }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.compass.myapplication.R.layout.activity_main);
        initView();
        initData();
    }
    private void initView(){
        mTvOrientation =(TextView)findViewById(com.compass.myapplication.R.id.tv_test);
        mTvLgt = findViewById(com.compass.myapplication.R.id.tv_lgt);
        mTvAltitude = findViewById(com.compass.myapplication.R.id.tv_altitude);
        mRlImage = findViewById(com.compass.myapplication.R.id.rl_image);
        mTemperaturet = findViewById(com.compass.myapplication.R.id.tv_temperature);
        sm=(SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = sm.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            Log.d("sennor", "onResume: "+sensor.getName());
        }
        aSensor=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor=sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mTemperatureSensor = sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(myListener, mTemperatureSensor, SensorManager.SENSOR_DELAY_UI);
    }
    private void initData(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (result != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                gpsLocationManager = GPSLocationManager.getInstances(MainActivity.this);
                //开启定位
                gpsLocationManager.start(new MyListener());
            }
        } else {
            gpsLocationManager = GPSLocationManager.getInstances(MainActivity.this);
            //开启定位
            gpsLocationManager.start(new MyListener());
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i : grantResults){
            if (i != PackageManager.PERMISSION_GRANTED){
                return;
            }
        }
        gpsLocationManager = GPSLocationManager.getInstances(MainActivity.this);
        //开启定位
        gpsLocationManager.start(new MyListener());
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在onPause()方法终止定位
        if (gpsLocationManager != null) {
            gpsLocationManager.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sm.unregisterListener(myListener);
    }

    private SensorEventListener myListener=new SensorEventListener(){

        @Override

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

            // TODO Auto-generated method stub
        }
        @Override

        public void onSensorChanged(SensorEvent event) {

            // TODO Auto-generated method stub
            if(event.sensor.getType()==Sensor.TYPE_AMBIENT_TEMPERATURE){
                float temperatureValue = event.values[0]; // 得到温度
                int temperature = (Math.round(temperatureValue * 10)) / 10;  ;// 转为摄氏温度
                mTemperaturet.setText("温度："+ temperature);
                return;
            }
            if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
                accelerometerValues=event.values;
            }
            if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
                magneticFieldValues=event.values;
            }
            //调用getRotaionMatrix获得变换矩阵R[]
            SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(R, values);
            //用（磁场+加速度）得到的数据范围是（-180～180）,也就是说，0表示正北，90表示正东，180/-180表示正南，
            // -90表示正西。而直接通过方向感应器数据范围是（0～359）360/0表示正北，90表示正东，180表示正南，
            // 270表示正西
            //经过SensorManager.getOrientation(R, values);得到的values值为弧度
            //转换为角度
            values[0]=(float)Math.toDegrees(values[0]);
            long time = System.currentTimeMillis();
            if (time - mLastTime > 200) {
                Message message = new Message();
                message.what = UPDATE_UI;
                message.arg1 = (int)values[0];
                mHandle.sendMessage(message);
                mLastTime = time;
            }
        }};
    class MyListener implements GPSLocationListener {

        @Override
        public void UpdateLocation(Location location) {
            if (location != null) {
                mTvLgt.setText("经度：" + String.format("%.2f",location.getLongitude()) + "   纬度：" + String.format("%.2f",location.getLatitude()));
                if (mHeight.size() > 10){
                    mHeight.remove(0);
                    mHeight.add(location.getAltitude());
                }else {
                    mHeight.add(location.getAltitude());
                }
                double height = 0;
                for (double d : mHeight){
                    height += d;
                }
                mTvAltitude.setText("海拔：" + (int)(height/mHeight.size()) +"米");
            }
        }

        @Override
        public void UpdateStatus(String provider, int status, Bundle extras) {
            if ("gps" == provider) {
                Toast.makeText(MainActivity.this, "定位类型：" + provider, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void UpdateGPSProviderStatus(int gpsStatus) {
            switch (gpsStatus) {
                case GPSProviderStatus.GPS_ENABLED:
                    Toast.makeText(MainActivity.this, "GPS开启", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_DISABLED:
                    Toast.makeText(MainActivity.this, "GPS关闭", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_OUT_OF_SERVICE:
                    Toast.makeText(MainActivity.this, "GPS不可用", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_TEMPORARILY_UNAVAILABLE:
                    Toast.makeText(MainActivity.this, "GPS暂时不可用", Toast.LENGTH_SHORT).show();
                    break;
                case GPSProviderStatus.GPS_AVAILABLE:
                    Toast.makeText(MainActivity.this, "GPS可用啦", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
    private String getOrientation(int angle){
        String orientation = getResources().getString(com.compass.myapplication.R.string.n);
        if (-22.5 < angle && angle <= 22.5){ //北
            orientation = getResources().getString(com.compass.myapplication.R.string.n);
        } else if (22.5 < angle && angle <= 67.5){ //东北
            orientation = getResources().getString(com.compass.myapplication.R.string.en);
        } else if (67.5 < angle && angle <= 112.5){ //东
            orientation = getResources().getString(com.compass.myapplication.R.string.e);
        } else if (112.5 < angle && angle <= 157.5){ //东南
            orientation = getResources().getString(com.compass.myapplication.R.string.es);
        } else if (157.5 < angle || angle <= -157.5){ //南
            orientation = getResources().getString(com.compass.myapplication.R.string.s);
        } else if (-157.5 < angle && angle <= -112.5){ //西南
            orientation = getResources().getString(com.compass.myapplication.R.string.ws);
        } else if (-112.5 < angle && angle <= -67.5){ //西
            orientation = getResources().getString(com.compass.myapplication.R.string.w);
        } else if (-67.5 < angle && angle <= -22.5){ //西北
            orientation = getResources().getString(com.compass.myapplication.R.string.wn);
        }
        return orientation;
    }
}
