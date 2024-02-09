#include <SoftwareSerial.h>
#include <Wire.h>
#include <RTClib.h>

RTC_DS3231 rtc;

SoftwareSerial bluetooth(10, 11); // RX, TX
int relayPin = 8;  // Pin conectado al relé
const char* diasSemana[] = {"Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};


void setup() {
  pinMode(relayPin, OUTPUT);
  digitalWrite(relayPin, HIGH);  // Apagar la bomba inicialmente

  Serial.begin(9600);  // Inicializar la comunicación serial por USB
  bluetooth.begin(9600);  // Inicializar la comunicación serial para el módulo Bluetooth

  if(!rtc.begin()){
    Serial.println("No se encontro el modulo RTC");
    while(1);
  }

  if(rtc.lostPower()){
    Serial.println("RTC perdio energia, reajustando fecha y hora");
    rtc.adjust(DateTime(F(__DATE__), F(__TIME__)));
  }
}


void loop() {

  DateTime now = rtc.now();


  // Construir una cadena de fecha y hora en formato legible
  String dateTimeString = String(diasSemana[now.dayOfTheWeek()]) + " " + 
                          String(now.day()) + "/" +
                          String(now.month()) + "/" +
                          String(now.year()) + " " +
                          String(now.hour()) + ":" +
                          String(now.minute()) + ":" +
                          String(now.second());
  // Enviar la cadena a través del módulo Bluetooth
  bluetooth.println(dateTimeString);
  Serial.println(dateTimeString);
  delay(1000);

  //Metodo para encender y apagar la bomba por medio de los botones.
  if (bluetooth.available() > 0) {
    char command = bluetooth.read();
    if (command == 'a') {
      digitalWrite(relayPin, LOW);  // Encender la bomba
    } else if (command == 'b') {
      digitalWrite(relayPin, HIGH);   // Apagar la bomba
    }
  }

  //metodo para programar tiempo de encendido del rele por medio de la consola
  if(Serial.available() > 0){
    int tiempoEncendido = Serial.parseInt();//lee el numero ingresado en la consola

    int tiempo = tiempoEncendido * 1000; 


    if(tiempo > 0){
      digitalWrite(relayPin, LOW);
      delay(tiempo);
      digitalWrite(relayPin, HIGH);
    }else{
      //Serial.println("Por favor ingresa un tiempo valido");
    }
  }
}

  // Otros códigos o funciones pueden ir aquí

  


