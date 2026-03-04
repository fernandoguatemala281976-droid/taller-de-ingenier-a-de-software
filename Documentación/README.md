# 🦯 SmartWalk: Dispositivo de Asistencia Sensorial

## 📖 Resumen del Proyecto
SmartWalk es un prototipo de bastón inteligente desarrollado para el Taller de Ingeniería de Software. Su objetivo principal es brindar mayor autonomía y seguridad a personas con discapacidad visual mediante la detección de obstáculos en tiempo real utilizando sensores de proximidad.

## ⚙️ Lógica del Sistema (Funcionamiento)
El núcleo del sistema se basa en un microcontrolador ESP32 que procesa los datos de un sensor ultrasónico (HC-SR04). El algoritmo clasifica la distancia en tres zonas de alerta para proporcionar retroalimentación inmediata:

* **Zona Segura (> 100 cm):** El camino está libre. El sistema permanece en reposo para ahorrar energía.
* **Zona de Precaución (50 cm - 100 cm):** Se detecta un obstáculo. El sistema emite un pitido intermitente a través del buzzer.
* **Zona de Peligro (< 50 cm):** El obstáculo requiere acción inmediata. El sistema activa un tono constante en el buzzer y enciende el motor de vibración.

**Accesibilidad (Modo Silencio):**
El dispositivo cuenta con un interruptor físico que suprime las alertas sonoras en espacios que lo requieran, permitiendo al usuario navegar utilizando exclusivamente la retroalimentación háptica (vibración).

## 🔌 Esquema de Conexiones (Hardware Pinout)
El proyecto utiliza un ESP32 (DOIT DEVKIT V1). A continuación se detalla el cableado:

| Componente | Pin del Componente | Pin del ESP32 | Función / Descripción |
| :--- | :--- | :--- | :--- |
| **Sensor (HC-SR04)** | VCC | VIN (5V) | Alimentación principal |
| | GND | GND | Tierra común |
| | TRIG | **D5** | Emite el pulso ultrasónico |
| | ECHO | **D18** | Recibe el rebote del sonido |
| **Buzzer Activo** | I/O / S | **D26** | Señal de activación del tono |
| **Motor Vibrador** | IN / Positivo | **D21** | Señal de activación del motor |
| **Interruptor** | Terminal 1 | **D15** | Lectura del Modo Silencio (Pull-up) |

> *Nota: Todos los componentes comparten una línea de tierra común (GND) para cerrar los circuitos correctamente.*

## 💻 Tecnologías Utilizadas
* **Lenguaje:** C++ (Framework de Arduino)
* **Microcontrolador:** DOIT ESP32 DEVKIT V1
* **IDE:** Visual Studio Code 

## 👥 Equipo de Desarrollo
Proyecto estructurado y programado por:

* **Fernando Guatemala Arizmendi**
## 🚀 Cómo replicar este proyecto
Para cargar este código en tu propio ESP32:
1. Clona este repositorio en tu computadora local.
2. Abre el archivo `Baston.ino` en Visual Studio Code (con la extensión de Arduino) o en el Arduino IDE clásico.
3. Asegúrate de tener instaladas las tarjetas de la familia **ESP32** en tu gestor de placas.
4. Conecta el ESP32 por USB, selecciona el puerto COM correspondiente y presiona "Subir".
## 🎨 Diseño y Ensamblaje
Para garantizar la ergonomía y facilidad de uso del dispositivo, a continuación se muestran los bocetos de cómo se integra la electrónica (ESP32, sensor y actuadores) dentro de la estructura física del bastón:

![Primer boceto](boceto_1.jpg)
![Segundo boceto](boceto_2.jpg)