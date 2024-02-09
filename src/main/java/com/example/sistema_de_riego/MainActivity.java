package com.example.sistema_de_riego;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private OutputStream outStream = null;
    private static final String TAG = "MainActivity";
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 3;
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 2;
    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice DispositivoSeleccionado;
    private ConnectedThread MyConexionBT;
    private ArrayList<String> mNameDevices = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;
    private static final int MESSAGE_READ = 2;
    private static final String TOAST = "toast_message";
    // Constante para el mensaje TOAST
    private static final int MESSAGE_TOAST = 5;  // Puedes ajustar el número según tus necesidades


    Button[] botonesDias = new Button[7];
    boolean[] diasDeLaSemana = new boolean[7];
    int horasProgramadas = 0;
    int minutosProgramados = 0;
    int segundosProgramados = 0;
    Button buscar, conectar, desconectar, programar_riego, encender, apagar;
    TextView fecha_hora;
    Spinner dispositivos;
    EditText horas, minutos, segundos;
    boolean presionado, presionado2, presionado3, presionado4, presionado5, presionado6, presionado7 = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestBluetoothConnectPermission();
        requestLocationPermission();

        buscar = findViewById(R.id.buscar);
        conectar = findViewById(R.id.conectar);
        desconectar = findViewById(R.id.desconectar);
        programar_riego = findViewById(R.id.programar_riego);
        encender = findViewById(R.id.encender);
        apagar = findViewById(R.id.apagar);
        dispositivos = findViewById(R.id.dispositivos);
        horas = findViewById(R.id.horas);
        minutos = findViewById(R.id.minutos);
        segundos = findViewById(R.id.segundos);
        fecha_hora = findViewById(R.id.fecha_hora);
        botonesDias[0] = findViewById(R.id.lunes);
        botonesDias[1] = findViewById(R.id.martes);
        botonesDias[2] = findViewById(R.id.miercoles);
        botonesDias[3] = findViewById(R.id.jueves);
        botonesDias[4] = findViewById(R.id.viernes);
        botonesDias[5] = findViewById(R.id.sabado);
        botonesDias[6] = findViewById(R.id.domingo);

        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mNameDevices);
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dispositivos.setAdapter(deviceAdapter);


        buscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DispositivosVinculados();
            }
        });


        conectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConectarDispBT();
            }
        });


        desconectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btSocket!=null)
                {
                    try {btSocket.close();}
                    catch (IOException e)
                    { Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();}
                }
                finish();
            }
        });

        programar_riego.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder diasSeleccionados = new StringBuilder("Dias seleccionados: ");

                for (int i = 0; i < diasDeLaSemana.length; i++){
                    if(diasDeLaSemana[i]){
                        switch (i){
                            case 0:
                                diasSeleccionados.append("Lunes, ");
                                break;
                            case 1:
                                diasSeleccionados.append("Martes, ");
                                break;
                            case 2:
                                diasSeleccionados.append("Miercoles, ");
                                break;
                            case 3:
                                diasSeleccionados.append("Jueves, ");
                                break;
                            case 4:
                                diasSeleccionados.append("Viernes, ");
                                break;
                            case 5:
                                diasSeleccionados.append("Sabado, ");
                                break;
                            case 6:
                                diasSeleccionados.append("Domingo, ");
                                break;
                        }
                    }
                }

                Log.d("Consultar dias", diasSeleccionados.toString());
                //enviarInformacion(diasDeLaSemana);
                enviarTiempo();
            }
        });

        encender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MyConexionBT != null) {
                    MyConexionBT.write('a');  // Enviar comando '1' para encender la bomba
                    showToast("Bomba encendida");
                } else {
                    showToast("Error: Conexión Bluetooth no establecida");
                }
            }
        });

        apagar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MyConexionBT != null) {
                    MyConexionBT.write('b');  // Enviar comando '0' para apagar la bomba
                    showToast("Bomba apagada");
                } else {
                    showToast("Error: Conexión Bluetooth no establecida");
                }
            }
        });

        for(int i = 0; i < botonesDias.length; i++){
            final int diaDeLaSemana = i;

            botonesDias[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    diasDeLaSemana[diaDeLaSemana] = !diasDeLaSemana[diaDeLaSemana];

                    Log.d("EstadoDias", "Dia: " + diaDeLaSemana + "seleccionado: " + diasDeLaSemana[diaDeLaSemana]);

                    actualizarColorBotones();
                }
            });
        }
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuffer = (byte[]) msg.obj;
                    int bytesRead = msg.arg1;
                    String receivedMessage = new String(readBuffer, 0, bytesRead);
                    updateDateTimeTextView(receivedMessage);
                    break;
                case MESSAGE_TOAST:
                    showToast(msg.getData().getString(TOAST));
                    break;
            }
            return true;
        }
    });


    private void updateDateTimeTextView(String dateTimeString) {
        Log.d(TAG, "Datos recibidos: " + dateTimeString);
        fecha_hora.setText("Fecha y Hora: " + dateTimeString);
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    if (result.getResultCode() == MainActivity.REQUEST_ENABLE_BT) {
                        Log.d(TAG, "ACTIVIDAD REGISTRADA");
                        //Toast.makeText(getBaseContext(), "ACTIVIDAD REGISTRADA", Toast.LENGTH_SHORT).show();
                    }
                }
            });


    private void actualizarColorBotones() {
        int colorPorDefecto = getResources().getColor(android.R.color.holo_purple); // Cambia a tu color por defecto
        int colorVerde = getResources().getColor(android.R.color.holo_green_light); // Cambia a tu color verde

        for (int i = 0; i < botonesDias.length; i++) {
            if (diasDeLaSemana[i]) {
                botonesDias[i].setBackgroundColor(colorVerde);
            } else {
                botonesDias[i].setBackgroundColor(colorPorDefecto);
            }
        }
    }


    // Método para enviar la información
    private void enviarInformacion(boolean[] diasDeLaSemana) {
        if (MyConexionBT != null) {
            // Crear una cadena que represente los días seleccionados
            StringBuilder info = new StringBuilder();

            for (boolean dia : diasDeLaSemana) {
                // Agregar '1' si el día está seleccionado, '0' si no lo está
                info.append(dia ? '1' : '0');
            }

            //separador para distinguir los dias del tiempo
            info.append(':');

            //agregar el tiempo programado
            info.append(String.format("%02d:%02d:%02d", horas, minutos, segundos));

            // Convertir la cadena a bytes y enviarla a través de la conexión Bluetooth
            MyConexionBT.write(info.toString().getBytes());
            showToast("Informacion enviada a Arduino: " + info.toString());
        } else {
            showToast("Error: Conexión Bluetooth no establecida");
        }
    }

    private void enviarTiempo(){
        String tiempoString = segundos.getText().toString();

        if (!tiempoString.isEmpty()){
            int tiempo = Integer.parseInt(tiempoString);

            try {
                outStream.write(String.valueOf(tiempo).getBytes());
            }catch (IOException e){
                showToast("Bluetooth no disponible en este dispositivo.");
            }
        }
    }


    public void DispositivosVinculados() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            showToast("Bluetooth no disponible en este dispositivo.");
            finish();
            return;
        }

        if (!mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            someActivityResultLauncher.launch(enableBtIntent);
        }

        dispositivos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                DispositivoSeleccionado = getBluetoothDeviceByName(mNameDevices.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                DispositivoSeleccionado = null;
            }
        });

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mNameDevices.add(device.getName());
            }
            deviceAdapter.notifyDataSetChanged();
        } else {
            showToast("No hay dispositivos Bluetooth emparejados.");
        }
    }

    // Agrega este método para solicitar el permiso
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION_PERMISSION);
    }

    // Agrega este método para solicitar el permiso
    private void requestBluetoothConnectPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_CONNECT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso concedido, ahora puedes utilizar funciones de Bluetooth que requieran BLUETOOTH_CONNECT");
            } else {
                Log.d(TAG, "Permiso denegado, debes manejar este caso según tus necesidades");
            }
        }
    }

    private BluetoothDevice getBluetoothDeviceByName(String name) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, " ----->>>>> ActivityCompat.checkSelfPermission");
        }
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(name)) {
                return device;
            }
        }
        return null;
    }
    private void ConectarDispBT() {
        if (DispositivoSeleccionado == null) {
            showToast("Selecciona un dispositivo Bluetooth.");
            return;
        }

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            btSocket = DispositivoSeleccionado.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
            btSocket.connect();
            MyConexionBT = new ConnectedThread(btSocket);
            MyConexionBT.start();
            showToast("Conexión exitosa.");
        } catch (IOException e) {
            showToast("Error al conectar con el dispositivo.");
        }
    }

    class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BufferedReader reader;
        ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                reader = new BufferedReader(new InputStreamReader(tmpIn));
            } catch (IOException e) {
                showToast("Error al crear el flujo de datos.");
                throw new RuntimeException(e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while (true){
                try {
                    bytes = mmInStream.read(buffer);
                    //Envia los datos al hilo principal mediante el manejador
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                }catch (IOException e){
                    showToast("Conexión perdida");
                    break;
                }
            }
        }
        public void write(char input) {
            //byte msgBuffer = (byte)input;
            try {
                mmOutStream.write((byte)input);
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            }catch (IOException e){
                showToast("Error al escribir en el socket");
            }
        }

        public void cancel(){
            try {
                mmInStream.close();
                mmOutStream.close();
            }catch (IOException e){
                showToast("Error al cerrar el socket");
            }
        }
    }
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}