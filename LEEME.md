# Tres en raya con JavaFX

Proyecto para el grupo de tres personas. La partida sigue siendo de dos jugadores. Se añade cliente gráfico e historial revisable.

## Contenido

- `src/main/java/Servidor.java`: conexiones, tablero, turnos y reglas.
- `src/main/java/Jugador.java`: un hilo por jugador, con `extends Thread`.
- `src/main/java/Cliente.java`: arranque de JavaFX y carga de la vista.
- `src/main/java/ControladorCliente.java`: eventos de la ventana y un hilo que recibe mensajes.
- `src/main/java/ControladorHistorial.java`: revisión y exportación del historial.
- `src/main/resources/vistas/cliente.fxml`: campos de conexión, tablero y botones.
- `src/main/resources/vistas/historial.fxml`: tablero de revisión y sus botones.
- `src/main/java/Historial.java`: estados del tablero y exportación a TXT.
- `pom.xml`: configuración Maven para Java 17 y JavaFX 17.0.10.
- `Memoria_tres_en_raya_JavaFX_Final.docx` y `.pdf`: memoria de cuatro páginas, con protocolo, diagrama y decisiones de diseño.

Las dos ventanas se definen en FXML y tienen sus propios controladores. Se pueden editar visualmente en Scene Builder. Los componentes no se construyen en las clases Java.

## Vistas FXML y controladores

- `cliente.fxml` utiliza `fx:controller="ControladorCliente"`.
- `historial.fxml` utiliza `fx:controller="ControladorHistorial"`.
- Los `fx:id` de las vistas coinciden con los campos `@FXML` de sus controladores.
- `onAction="#conectar"`, por ejemplo, llama al método `conectar()` del controlador.
- `initialize()` se ejecuta después de que FXMLLoader haya inyectado los componentes.
- `Cliente` carga `/vistas/cliente.fxml` con `FXMLLoader`, obtiene su controlador y muestra la escena.
- Para revisar una partida, el controlador carga `/vistas/historial.fxml` y entrega una copia del historial a su controlador.

Los nueve botones del tablero están declarados en FXML. Comparten el evento `#jugar` y su propiedad `userData` indica la casilla, de 0 a 8. En el controlador se agrupan en un array solo para actualizar sus textos y desactivarlos.

### Editar con Scene Builder

Abre `src/main/resources/vistas/cliente.fxml` o `historial.fxml` desde IntelliJ o directamente en Scene Builder. Puedes cambiar tamaños, distribución y textos. Conserva los `fx:id`, `fx:controller`, eventos `onAction` y el `userData` de las casillas para mantener su conexión con Java.

Maven copia automáticamente `src/main/resources` al compilar. Conserva esa carpeta al compartir el proyecto: copiar solo las clases Java dejaría las vistas fuera del programa. El `pom.xml` incluye `javafx-fxml` además de `javafx-controls`.

## Abrirlo en IntelliJ

1. Descomprime el ZIP y abre el archivo `pom.xml` como proyecto Maven.
2. Selecciona un JDK 17 o posterior para el proyecto y para Maven.
3. Espera a que Maven descargue las dependencias. La primera vez necesita Internet.
4. Ejecuta `Servidor.main()` con el botón verde.
5. En el panel Maven abre Plugins > javafx > javafx:run para iniciar el cliente.
6. Ejecuta el objetivo `javafx:run` una segunda vez en otro proceso. Si IntelliJ propone detener el primero, crea una configuración Maven con `javafx:run` y activa la opción para permitir varias instancias.
7. En cada ventana introduce un nombre y pulsa Conectar. Para probar en el mismo equipo deja `localhost` y `5050`.

Se recomienda arrancar el cliente mediante el objetivo Maven `javafx:run`. Ejecutar directamente `Cliente.main()` sin configurar JavaFX puede mostrar un error de componentes de JavaFX ausentes.

## Desde terminal

Dentro de la carpeta del proyecto, con el JDK y Maven instalados:

```bash
mvn compile
java -cp target/classes Servidor
```

En otras dos terminales dentro de la misma carpeta:

```bash
mvn javafx:run
```

Para cambiar el puerto del servidor:

```bash
java -cp target/classes Servidor 6000
```

En las ventanas de los clientes también hay que poner `6000`.

## Desde dos ordenadores

1. Ambos equipos deben tener conectividad con el servidor.
2. Ejecuta el servidor en un ordenador y permite el acceso a su puerto TCP.
3. Abre un cliente en cada ordenador.
4. En el campo de dirección escribe la IP local del ordenador servidor, por ejemplo `192.168.1.20`. `localhost` solo sirve si el servidor está en el mismo ordenador.
5. Usa el mismo puerto en el servidor y en los clientes.

## Jugar y revisar

- El primer cliente usa X y el segundo O. X empieza.
- Haz clic en una casilla vacía cuando sea tu turno.
- El servidor valida las jugadas aunque el cliente desactive botones.
- Tras el resultado se activa Revisar jugadas.
- En la ventana de revisión, Anterior y Siguiente recorren el tablero inicial y las jugadas aceptadas.
- Guardar TXT permite elegir un archivo para conservar la partida con sus coordenadas y resultado.
- Ambos jugadores deben pulsar Revancha para iniciar otra partida.
- El historial actual se reinicia con cada revancha. Una revisión que ya esté abierta mantiene su propia copia. Exporta el TXT antes de cerrar o iniciar otra partida si quieres conservarlo.
- Salir o cerrar la ventana envía BYE y cierra la conexión.

## Hilos y sincronización

`Jugador` hereda de `Thread`. Su `run()` espera mensajes con `readLine()` y llama al mismo objeto `Servidor` que comparte con el otro jugador.

Los métodos del servidor que registran jugadores o cambian la partida son `synchronized`. No pueden validar y modificar el tablero los dos hilos a la vez.

En `ControladorCliente`, `RecibirMensajes extends Thread` espera la red. `Platform.runLater()` entrega las respuestas al hilo de JavaFX, que actualiza los controles y el historial. La interfaz permanece disponible mientras la red espera.

## Protocolo

El protocolo completo se describe en la memoria. Cada mensaje ocupa una línea UTF-8. Las coordenadas van de 1 a 3 y el tablero tiene nueve caracteres por filas. `.` indica casilla vacía.

El cliente envía `PLAY nombre`, `MOVE fila,columna`, `REMATCH` o `BYE`. El servidor contesta con `WAIT`, `FULL`, `START`, `OK`, `TURN`, `DENIED`, `END` e `INFO`.

No hay comandos nuevos para el historial: se guardan las copias que el servidor confirma con `OK`.

## Cierre y limitaciones

Solo hay dos plazas; los demás clientes reciben FULL. Si se desconecta un jugador, se cierra esa sesión y el servidor. Durante una partida el rival gana por abandono y mantiene abierta la ventana para revisar el historial. Para empezar una sesión nueva se vuelve a ejecutar el servidor y se abren clientes nuevos.

Después de un error de conexión, reinicia el cliente para volver a intentarlo. El intento de conexión espera como máximo cinco segundos. No hay detección de inactividad ni mensajes de latido.

## Demo de cinco minutos para tres personas

Reparto propuesto, para que los tres expliquéis una parte similar:

1. Persona 1 (0:00 a 1:40): presentar el servidor, las dos conexiones y el hilo Jugador. Conectar dos clientes desde equipos distintos y explicar `synchronized`.
2. Persona 2 (1:40 a 3:20): mostrar la vista FXML y su controlador JavaFX y el intercambio MOVE / OK / TURN. Jugar X(1,1), O(2,1), X(1,2), O(2,2), X(1,3), que termina en victoria de X.
3. Persona 3 (3:20 a 5:00): abrir el historial, avanzar y retroceder, exportar el TXT y solicitar revancha desde ambos clientes. Salir de uno para mostrar el abandono.

La memoria no atribuye trabajo a personas concretas: completad los datos de autoría que os pida el profesor.

## Verificación

Se ha compilado el código con Java 17 y JavaFX 17.0.10. Se han probado las reglas mediante TCP y dos clientes JavaFX con pantalla virtual, incluida la carga de los dos FXML, los eventos de los botones, la revisión, la generación de texto y las revanchas. Queda ensayar la demo con vuestra configuración y en dos equipos del aula.

Configuración Maven basada en la documentación de OpenJFX:
https://openjfx.io/openjfx-docs/maven

Documentación de FXML utilizada:
https://openjfx.io/javadoc/17/javafx.fxml/javafx/fxml/doc-files/introduction_to_fxml.html
