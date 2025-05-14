char data;

void setup() {
  pinMode(13, OUTPUT);
  Serial.begin(9600);
}

void loop() {
  if (Serial.available()) {
    data = Serial.read();
    if (data == '1') digitalWrite(13, HIGH);
    else if (data == '0') digitalWrite(13, LOW);
  }
}
