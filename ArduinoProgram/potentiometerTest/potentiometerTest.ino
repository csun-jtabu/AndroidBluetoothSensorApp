#include <SoftwareSerial.h> // library used to make other pins on Arduino board TX and RX

// potentiometer variables
double potentOutput = 0; // this will store the output from the potentiometer
int potentPin = A0; // this will be the pin number for the potentiometer 

// bluetooth variables
SoftwareSerial BT(3, 2); // RX = 3, TX = 2 are the pins on the arduino board i'm using


void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600); // this will enable serial monitoring on ide
  BT.begin(9600); // this will enable us to send serial data 
}

void loop() {
  // put your main code here, to run repeatedly:
  potentOutput = analogRead(potentPin); // this reads from the potentiometer and stores in variable
  Serial.println(potentOutput); // prints stored value onto the serial monitor
  BT.println(potentOutput); // sends over data to bluetooth connected device
  delay(200); // delay to slow down displayed output
}
