#include <Arduino.h>
// --- CONFIGURACIÓN DE PINES ---
const int PIN_TRIG = 5;       
const int PIN_ECHO = 18;      
const int PIN_BUZZER = 26;  
const int PIN_MOTOR = 21;     
const int PIN_SWITCH = 15;    

// --- VARIABLES ---
long duracion;
int distancia;
bool modoSilencio = false;

void setup() {
  Serial.begin(115200); 
  
  pinMode(PIN_TRIG, OUTPUT);
  pinMode(PIN_ECHO, INPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_MOTOR, OUTPUT);
  pinMode(PIN_SWITCH, INPUT_PULLUP); 
  
  Serial.println("SmartWalk: Sistema Iniciado con Buzzer en D26");
}

void loop() {
  // 1. Verificar Interruptor (Modo Silencio)
  modoSilencio = (digitalRead(PIN_SWITCH) == LOW);

  // 2. Medir Distancia (Tu lógica que funciona)
  digitalWrite(PIN_TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(PIN_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_TRIG, LOW);
  
  duracion = pulseIn(PIN_ECHO, HIGH);
  distancia = duracion * 0.034 / 2; 

  // 3. Monitor Serie
  Serial.print("Distancia: ");
  Serial.print(distancia);
  Serial.print(" cm | Modo Silencio: ");
  Serial.println(modoSilencio ? "ACTIVADO" : "DESACTIVADO");

  // 4. LÓGICA DE ALERTAS ADAPTADA
  if (distancia > 0 && distancia <= 50) {
    // MUY CERCA: Vibración y Sonido Constante
    digitalWrite(PIN_MOTOR, HIGH);
    if (!modoSilencio) {
      tone(PIN_BUZZER, 1000); // Esto hace que el buzzer suene sí o sí
    } else {
      noTone(PIN_BUZZER);
    }
  } 
  else if (distancia > 50 && distancia <= 100) {
    // CERCA: Beep intermitente
    digitalWrite(PIN_MOTOR, LOW);
    if (!modoSilencio) {
      tone(PIN_BUZZER, 1000);
      delay(150);
      noTone(PIN_BUZZER);
      delay(150);
    }
  } 
  else {
    // LEJOS: Todo apagado
    digitalWrite(PIN_MOTOR, LOW);
    noTone(PIN_BUZZER);
  }

  delay(50); 
}