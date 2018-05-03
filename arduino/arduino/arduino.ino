/*
* Adafruit_Sensor ---> https://github.com/adafruit/Adafruit_Sensor
* DHT ---> https://github.com/adafruit/DHT-sensor-library
*
* If Arduino IDE can't find Adafruit_Sensor.h, uncomment the dafruit_Sensor.h include line.
*/
// #include <Adafruit_Sensor.h>
#include <DHT.h>

#define DHT_PIN 2 // DHT PIN.
#define FIRST_LOOP_DELAY 2000
#define STANDART_DELAY 1400
#define ACTION_DELAY 100

// Sensor initialization:
DHT dht(DHT_PIN, DHT22);

String tempStr, humidStr, MAXTStr, MINTStr, MAXHStr, MINHStr; // Strings for sending data.
boolean firstLoop = true; // Variable for the first loop initialization.
int firstLoop = 0; // Variable for loop definition.
float temp, humid, MAXT, MINT, MAXH, MINH; // Temperature and humidity + MAX / MIN parameters.
char incomingByte; // Storage of incoming data.

// Set the communication speed and start the sensor:
void setup() {
  Serial.begin(9600); // Speed.
  dht.begin(); // Sensor.
}

// Reset:
void(* resetFunc) (void) = 0;

void loop() {
  if (firstLoop) {
    delay (FIRST_LOOP_DELAY)
    firstLoop = false;
  } else {
    delay(STANDART_DELAY);
  }

  temp = dht.readTemperature(); // Get the temperature.
  humid = dht.readHumidity(); // Get the humidity.

  // Calculation of the maximum and minimum values:
  if(firstLoop == 0) {
    MAXT = temp;
    MINT = temp;
    MAXH = humid;
    MINH = humid;

    firstLoop = 1;
  } else {
    if(temp >= MAXT) {
      MAXT = temp;
    } else if(temp <= MINT) {
      MINT = temp;
    }
    if(humid >= MAXH) {
      MAXH = humid;
    } else if(temp <= MINH) {
      MINH = humid;
    }
  }

  tempStr = (String) temp;
  humidStr = (String) humid;
  MAXTStr = (String) MAXT;
  MINTStr = (String) MINT;
  MAXHStr = (String) MAXH;
  MINHStr = (String) MINH;

  // Sending data to the application:
  Serial.println("t" + tempStr + " C");
  delay(ACTION_DELAY);
  Serial.println("h" + humidStr + "%");
  delay(ACTION_DELAY);
  Serial.println("m" + MAXTStr + " C");
  delay(ACTION_DELAY);
  Serial.println("i" + MINTStr + " C");
  delay(ACTION_DELAY);
  Serial.println("w" + MAXHStr + "%");
  delay(ACTION_DELAY0);
  Serial.println("q" + MINHStr + "%");
  delay(ACTION_DELAY);

  if (Serial.available() > 0) {
    // read the incoming byte:
    incomingByte = Serial.read();

    // Reset:
    if(incomingByte == 'r') {
      resetFunc();
    }
  }
}
