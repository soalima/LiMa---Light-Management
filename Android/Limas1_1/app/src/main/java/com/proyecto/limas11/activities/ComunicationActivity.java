package com.proyecto.limas11.activities;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.proyecto.limas11.bluetooth.BluetoothConnectionService;
import com.proyecto.limas11.R;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.proyecto.limas11.fragments.BluetoothFragment.MULTIPLE_PERMISSIONS;

/*********************************************************************************************************
 * Activity que tiene la logica de las luces
 **********************************************************************************************************/

//******************************************** Hilo principal del Activity*********************************
public class ComunicationActivity extends Activity {
    private static final int LUZMINIMA = 70;
    private static final int SEGUNDO = 1000;
    private static  final char ENCENDER1 = '1';
    private static  final char ENCENDER2 = '5';
    private static  final char APAGAR1 = '2';
    private static  final char APAGAR2 = '6';
    private static  final char SHAKE1 = '4';
    private static  final char SHAKE2 = '7';
    private static  final char RESET1 = '8';
    private static  final char RESET2 = '9';
    private static  final char ALARMA = '3';
    private static final int ACELERACION = 4;
    private static final int PROXIMIDAD = 3;

    Button btnApagar, btnEncender, btnEstadisticas, btnResetear;
    Switch switchL1, switchL2, swLinterna;
    boolean prendertorch, isflashon;
    static Camera cam = null;
    private String permissions = Manifest.permission.CAMERA;

    private BluetoothConnectionService bluetoothConnection;
    private long lastUpdate;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private static String address = null;
    private BluetoothDevice device;
    private SensorManager sensorManager;
    private String arduino1 = null;
    private String arduino2 = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comunication);

        //Se definen los componentes del layout
        btnApagar = (Button) findViewById(R.id.btnApagar);
        btnEncender = (Button) findViewById(R.id.btnEncender);

        btnEstadisticas = (Button) findViewById(R.id.btnEstadistica);
        btnResetear = (Button) findViewById(R.id.btnResetear);

        switchL1 = (Switch) findViewById(R.id.switchLuz1);
        switchL2 = (Switch) findViewById(R.id.switchLuz2);
        swLinterna = (Switch) findViewById(R.id.swLinterna);

        switchL1.setChecked(false);
        switchL2.setChecked(false);
        swLinterna.setChecked(false);

        //obtengo el adaptador del bluethoot
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //defino los handlers para los botones Apagar y encender
        btnEncender.setOnClickListener(btnEncenderListener);
        btnApagar.setOnClickListener(btnApagarListener);
        btnEstadisticas.setOnClickListener(btnEstadisticasListener);
        btnResetear.setOnClickListener(btnResetearListener);

        swLinterna.setOnCheckedChangeListener((new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    prendertorch = true;
                } else {
                    if (isflashon)
                        off(null);
                    prendertorch = false;
                }
            }
        }));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        /*
        sensorManager.registerListener(accelerometerSensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(proximitySensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(lightSensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
        */
    }

    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Acá se recibe el mensaje del arduino y se evalua
            String LED1 = "L1";
            String LED2 = "L2";
            String arduino= intent.getStringExtra("theMessage");
            String[] division = arduino.split("\\:");
            if (division[0].equals(LED1)) {
                //paso el valor a segundos
                /*String valor = Arrays.toSring(division[1]);
                String dato = valor.split("\r");*/
               // String dato = division[1].substring(0,division[1].indexOf("\r"));
                Long numero1 = Long.parseLong(division[1]);
                double L1 = (double) numero1 / SEGUNDO;
                arduino1=(Double.toString(L1));
            } else if (division[0].equals(LED2)) {
              //  Long numero3 = Long.parseLong(arduino);
               // String dato = division[1].substring(0,division[1].indexOf("\r"));
                Long numero2 = Long.parseLong(division[1]);
                double L2 = (double) numero2 / SEGUNDO;
                arduino2=(Double.toString(L2));
                if (division.length > 2 && division[2].equals(LED1)) {
                    //paso el valor a segundos
                /*String valor = Arrays.toSring(division[1]);
                String dato = valor.split("\r");*/
                  //  String dato2 = division[4].substring(0, division[4].indexOf("\r"));
                    Long numero1 = Long.parseLong(division[3]);
                    double L1 = (double) numero1 / SEGUNDO;
                    arduino1 = (Double.toString(L1));
                }
            }

            Log.d("ComandoRecepcion", arduino);
            //Log.d("Tiempo Led 1", arduino1);



        }
    };


    @Override
    public void onResume() {
        super.onResume();

        //Obtengo el parametro, aplicando un Bundle, que me indica la Mac Adress del HC05
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        address = extras.getString("Direccion_Bluethoot");
        device = btAdapter.getRemoteDevice(address);
        bluetoothConnection = new BluetoothConnectionService(getApplicationContext());
        //Se declara un receiver para obtener los datos que envíe el embebebido.
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter("IncomingMessage"));
        //Si tengo un disposito conectado comienzo la conexión.
        bluetoothConnection.startClient(device, bluetoothConnection.getDeviceUUID());

        sensorManager.registerListener(accelerometerSensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(proximitySensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(lightSensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    //Cuando se ejecuta el evento onPause se cierra el socket Bluethoot, para no recibiendo datos
    public void onPause() {
        super.onPause();
        /*
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            //btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
        */
        if (device != null) {
            Log.e("[onBACKPRESSED:Limas]", "CANCELANDO THREAD");
            try {
                bluetoothConnection.cancel();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "No hay nada que cerrar");
            }
        }
        sensorManager.unregisterListener(lightSensorEventListener);
        sensorManager.unregisterListener(proximitySensorEventListener);
        sensorManager.unregisterListener(accelerometerSensorEventListener);
    }

    //Listener del boton encender que envia  msj para enceder Led a Arduino atraves del Bluethoot
    private View.OnClickListener btnEncenderListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (switchL1.isChecked()) {
                enviarComando(ENCENDER1);
                //showToast("Encender el LED 1");
            }
            if (switchL2.isChecked()) {
                enviarComando(ENCENDER2);
                //showToast("Encender el LED 2");
            }
        }
    };

    //Listener del boton apagar que envia  msj para Apagar Led a Arduino atraves del Bluethoot
    private View.OnClickListener btnApagarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (switchL1.isChecked()) {
                enviarComando(APAGAR1);
                //showToast("Apagar el LED 1");
            }
            if (switchL2.isChecked()) {
                enviarComando(APAGAR2);
                //showToast("Apagar el LED 2");
            }
        }
    };

    private View.OnClickListener btnEstadisticasListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //enviarComando('8');
            //showToast("Presionado boton estadistica luz 1");

            Intent newIntent = new Intent(v.getContext(), EstadisticasActivity.class);
            newIntent.putExtra("LED1",arduino1);
            newIntent.putExtra("LED2",arduino2);
            //newIntent.putParcelableArrayListExtra("device.list", mDeviceList);

            startActivity(newIntent);

        }
    };

    private View.OnClickListener btnResetearListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (switchL1.isChecked()) {
                enviarComando(RESET1);
                arduino1= "0";
                showToast("Led 1 Reiniciado");
            }
            if (switchL2.isChecked()) {
                enviarComando(RESET2);
                arduino2= "0";
                showToast("Led 2 Reiniciado");
            }

        }
    };

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    SensorEventListener lightSensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_LIGHT && checkPermissions()) {
                long actualTime = event.timestamp;
                if (actualTime - lastUpdate < SEGUNDO) {
                    return;
                }
                lastUpdate = actualTime;

                float currentLux = event.values[0];
                if (currentLux < LUZMINIMA && prendertorch == true) {
                    on(null);
                } else {
                    off(null);
                }
            }
        }
    };

    SensorEventListener accelerometerSensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float[] values = event.values;

                float x = values[0];
                float y = values[1];

                float accelationSquareRoot = (x * x + y * y)
                        / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
                long actualTime = event.timestamp;
                if (accelationSquareRoot >= ACELERACION) //
                {
                    if (actualTime - lastUpdate < SEGUNDO) {
                        return;
                    }
                    lastUpdate = actualTime;
                    if (switchL1.isChecked()) {
                        enviarComando(SHAKE1);
                    }
                    if (switchL2.isChecked()) {
                        enviarComando(SHAKE2);
                    }
                }
            }
        }
    };

    SensorEventListener proximitySensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                if (event.values[0] == PROXIMIDAD)
                    enviarComando(ALARMA);
            }
        }
    };

    public void off(View v) {
        if (isflashon == true) {
            cam = Camera.open();
            Camera.Parameters p = cam.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            cam.setParameters(p);
            isflashon = false;
        }
    }

    public void on(View v) {
        if (isflashon == false) {
            cam = Camera.open();
            Camera.Parameters p = cam.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            cam.setParameters(p);
            isflashon = true;
        }
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        //Se chequea si la version de Android es menor a la 6
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }


        result = ContextCompat.checkSelfPermission(ComunicationActivity.this, permissions);
        if (result != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(permissions);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(ComunicationActivity.this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    //Método para envíar información al arduino.
    public void enviarComando(char comando) {
        if (device == null) {
            //Si no estoy conectado al bluetooth o se pierde la señal
            // Pido reconectarse
            //showToast("No estás conectado a ningún dispositivo. Conectate vía bluetooth.");
        } else {
            //showToast("Caracter a enviar: " + comando);
            byte[] commandInBytes = String.valueOf(comando).getBytes(Charset.defaultCharset());

            bluetoothConnection.write(commandInBytes);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(this.myReceiver);
        } catch (Exception ex) {
        }
        if (device != null) {
            Log.e("[onBACKPRESSED:Limas]", "CANCELANDO THREAD");
            try {
                bluetoothConnection.cancel();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "No hay nada que cerrar");
            }
        }
        sensorManager.unregisterListener(lightSensorEventListener);
        sensorManager.unregisterListener(proximitySensorEventListener);
        sensorManager.unregisterListener(accelerometerSensorEventListener);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions granted.
                    //enableComponent(); // Now you call here what ever you want :)
                } else {
                    String perStr = "";
                    for (String per : permissions) {
                        perStr += "\n" + per;
                    }
                    // permissions list of don't granted permission
                    Toast.makeText(ComunicationActivity.this, "ATENCION: La aplicacion no funcionara " + "correctamente debido a la falta de Permisos", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
}
