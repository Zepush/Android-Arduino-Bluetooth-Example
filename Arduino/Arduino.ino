/*
* Adafruit_Sensor ---> https://github.com/adafruit/Adafruit_Sensor
* DHT ---> https://github.com/adafruit/DHT-sensor-library
*
* If Arduino IDE can't find Adafruit_Sensor.h, uncomment the dafruit_Sensor.h include line.
*/
// #include <Adafruit_Sensor.h>
#include <DHT.h>

// DHT PIN:
#define DHT_PIN 2

// Sensor initialization:
DHT dht(DHT_PIN, DHT22);

int firstLoop = 0; // Variable for loop definition.

float temp, humid, MAXT, MINT, MAXH, MINH; // Temperature and humidity + MAX / MIN parameters.

char incomingByte; // Storage of incoming data.

String tempStr, humidStr, MAXTStr, MINTStr, MAXHStr, MINHStr; // Strings for sending data.

// Set the communication speed and start the sensor:
void setup() {
  Serial.begin(9600); // Speed.

  dht.begin(); // Sensor.
}

// Reset:
void(* resetFunc) (void) = 0;

void loop() {
  delay (2000);

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
  delay(100);
  Serial.println("h" + humidStr + "%");
  delay(100);
  Serial.println("m" + MAXTStr + " C");
  delay(100);
  Serial.println("i" + MINTStr + " C");
  delay(100);
  Serial.println("w" + MAXHStr + "%");
  delay(100);
  Serial.println("q" + MINHStr + "%");
  delay(100);

  if (Serial.available() > 0) {
  // read the incoming byte:
  incomingByte = Serial.read();

  // Reset:
  if(incomingByte == 'r'){
    resetFunc();
  }
  }
}
