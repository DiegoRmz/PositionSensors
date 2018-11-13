package com.halo.positionsensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

    private ImageView mPointer;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;


    private float[] mLastAccelerometer = new float[3];  //Buffer para lecturas de sensor acelerometro
    private float[] mLastMagnetometer = new float[3];  //Buffer para lecturas de magnetometro

    //Esto es para decir que se acaba de updatear la lectura
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;


    private float[] mR = new float[9];  //Matriz de orientacion
    private float[] mOrientation = new float[3];    //Matriz con los 3 angulos de rotación

    private float mCurrentDegree = 0f;

    private TextView tvAzimuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Obtiene el manejador de sensores del sistema operativo
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        //Sensores de acelerómetro y de magnetismo
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //Imagen
        mPointer = (ImageView) findViewById(R.id.pointer);
        tvAzimuth = (TextView) findViewById(R.id.tvAzimuth);

        flashLightCameraOn();
    }

    protected void onResume() {
        super.onResume();

        //Cuando la app vuelve, se registran de nuevo los manejadores de los sensores
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    //Esta función corre cuando otra actividad "saca" a la app actual de la pantalla principal
    protected void onPause() {
        super.onPause();

        //Así, se deja de andar "polleando" los sensores para guardar batería
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);

        //Turns off the flashlight when there is no battery
        flashLightCameraOff();
    }


    //Evento que se ejecuta cuando un sensor obtiene lecturas
    @Override
    public void onSensorChanged(SensorEvent event) {
        //Copia la lectura del sensor de aceleración
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        }
        //Copia la lectura del sensor magnético
        else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }


        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];   //La orientacion en android
                                                        //Esta hecha por tres angulos
                                                        //Azimut: Rotación en -Z y es el ángulo entre
                                                        //el norte magnético y el teléfono
                                                        //Los otros dos ni los usamos alv

            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;

            Log.d("Azimuth", ""+azimuthInDegress);


            tvAzimuth.setText("Azimuth: "+azimuthInDegress);

            String azimuthDeg = ""+azimuthInDegress;

            //Only turn on flashlight when nearing north
            if(azimuthDeg.charAt(0) == '3'){
                flashLightCameraOn();
            }else{
                flashLightCameraOff();
            }

            //Esto rota la imagen de la brujula
            RotateAnimation ra = new RotateAnimation(
                    mCurrentDegree,
                    -azimuthInDegress,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);

            ra.setDuration(250);

            ra.setFillAfter(true);

            mPointer.startAnimation(ra);
            mCurrentDegree = -azimuthInDegress;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    //Esto enciende la flashlight solo para que se vea chido
    private void flashLightCameraOn(){
        CameraManager camera = (CameraManager)getSystemService(Context.CAMERA_SERVICE);


        try{
            String cid = camera.getCameraIdList()[0];
            camera.setTorchMode(cid,true);


        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void flashLightCameraOff(){
        CameraManager camera = (CameraManager)getSystemService(Context.CAMERA_SERVICE);

        try{
            String cid = camera.getCameraIdList()[0];
            camera.setTorchMode(cid,false);


        }catch (Exception e){
            e.printStackTrace();
        }
    }

}