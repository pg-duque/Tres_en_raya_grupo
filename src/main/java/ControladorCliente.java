import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

// Los componentes se declaran en cliente.fxml; FXMLLoader los inyecta aqui.
public class ControladorCliente {
    @FXML private TextField nombre;
    @FXML private TextField direccion;
    @FXML private TextField puerto;
    @FXML private Label estado;
    @FXML private Button conectar;
    @FXML private Button revancha;
    @FXML private Button revisar;
    @FXML private Button casilla0;
    @FXML private Button casilla1;
    @FXML private Button casilla2;
    @FXML private Button casilla3;
    @FXML private Button casilla4;
    @FXML private Button casilla5;
    @FXML private Button casilla6;
    @FXML private Button casilla7;
    @FXML private Button casilla8;
    private Button[] casillas;
    private final Historial historial = new Historial();
    private volatile Socket socket;
    private volatile PrintWriter salida;
    private volatile boolean cerrado = false;
    private boolean conectado = false;
    private boolean miTurno = false;
    private boolean terminada = false;
    private char marca;
    private String tablero = ".........";
    private Stage ventana;

    // Se llama automaticamente cuando se ha cargado el FXML.
    @FXML
    private void initialize() {
        casillas = new Button[] {casilla0, casilla1, casilla2, casilla3,
                casilla4, casilla5, casilla6, casilla7, casilla8};
        actualizarTablero();
    }

    public void setVentana(Stage ventana) {
        this.ventana = ventana;
    }

    @FXML
    private void jugar(ActionEvent evento) {
        Button boton = (Button) evento.getSource();
        int posicion = Integer.parseInt(boton.getUserData().toString());
        enviar("MOVE " + (posicion / 3 + 1) + "," + (posicion % 3 + 1));
    }

    @FXML
    private void pedirRevancha() {
        enviar("REMATCH");
        revancha.setDisable(true);
    }

    @FXML
    private void salir() {
        Platform.exit();
    }

    @FXML
    private void conectar() {
        String ip = direccion.getText().trim();
        String jugador = nombre.getText().trim();
        int numeroPuerto;
        try {
            numeroPuerto = Integer.parseInt(puerto.getText().trim());
            if (numeroPuerto < 1 || numeroPuerto > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            estado.setText("El puerto debe estar entre 1 y 65535.");
            return;
        }
        if (ip.isEmpty() || jugador.isEmpty()) {
            estado.setText("Escribe tu nombre y la IP del servidor.");
            return;
        }
        conectar.setDisable(true);
        nombre.setDisable(true);
        direccion.setDisable(true);
        puerto.setDisable(true);
        estado.setText("Conectando...");
        new RecibirMensajes(ip, numeroPuerto, jugador).start();
    }

    // Todos los cambios de interfaz y del historial ocurren en el hilo de JavaFX.
    private void recibir(String mensaje) {
        if (cerrado) return;
        if (mensaje.startsWith("START ")) {
            conectado = true;
            marca = mensaje.charAt(6);
            ventana.setTitle("Tres en raya - " + nombre.getText() + " - " + marca);
            terminada = false;
            miTurno = false;
            historial.iniciar();
            revisar.setDisable(true);
            revancha.setDisable(true);
        } else if (mensaje.startsWith("OK ")) {
            tablero = mensaje.substring(3);
            historial.guardar(tablero);
            miTurno = false;
        } else if (mensaje.startsWith("TURN ")) {
            miTurno = mensaje.charAt(5) == marca;
            estado.setText(miTurno ? "Eres " + marca + ". Tu turno."
                    : "Eres " + marca + ". Turno del rival.");
        } else if (mensaje.startsWith("END ")) {
            terminada = true;
            miTurno = false;
            String resultado;
            if (mensaje.equals("END DRAW")) {
                resultado = "Empate";
            } else {
                resultado = "Gana " + mensaje.split(" ")[2];
                if (mensaje.endsWith("ABANDONO")) resultado += " por abandono";
            }
            historial.resultado = resultado;
            estado.setText(resultado);
            revisar.setDisable(false);
            revancha.setDisable(mensaje.endsWith("ABANDONO"));
        } else if (mensaje.equals("WAIT")) {
            conectado = true;
            estado.setText("Esperando al segundo jugador...");
        } else if (mensaje.equals("FULL")) {
            estado.setText("Partida llena. Ya hay dos jugadores.");
        } else if (mensaje.startsWith("INFO ")) {
            if (!terminada) estado.setText(mensaje.substring(5));
        } else if (mensaje.startsWith("DENIED ")) {
            estado.setText(mensaje.substring(7));
        }
        actualizarTablero();
    }

    private void actualizarTablero() {
        for (int i = 0; i < 9; i++) {
            char ficha = tablero.charAt(i);
            casillas[i].setText(ficha == '.' ? "" : String.valueOf(ficha));
            casillas[i].setDisable(!conectado || !miTurno || terminada || ficha != '.');
        }
    }

    private void enviar(String mensaje) {
        if (salida != null) salida.println(mensaje);
    }

    private void desconectado(String motivo) {
        if (cerrado) return;
        conectado = false;
        miTurno = false;
        revancha.setDisable(true);
        if (!terminada && !estado.getText().startsWith("Partida llena")) estado.setText(motivo);
        actualizarTablero();
        // La ventana sigue abierta para revisar tras un abandono.
    }

    public void cerrar() {
        cerrado = true;
        enviar("BYE");
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // La aplicacion puede cerrarse aunque la conexion ya este cortada.
        }
    }

    @FXML
    private void mostrarHistorial() {
        try {
            FXMLLoader cargador = new FXMLLoader(getClass().getResource("/vistas/historial.fxml"));
            Parent vista = cargador.load();
            ControladorHistorial controlador = cargador.getController();
            controlador.cargar(historial);
            Stage revision = new Stage();
            revision.initOwner(ventana);
            revision.setTitle("Historial - " + historial.resultado);
            revision.setScene(new Scene(vista));
            revision.show();
        } catch (IOException e) {
            estado.setText("No se ha podido abrir la vista del historial.");
        }
    }

    private class RecibirMensajes extends Thread {
        private final String direccion;
        private final int puerto;
        private final String nombre;

        public RecibirMensajes(String direccion, int puerto, String nombre) {
            this.direccion = direccion;
            this.puerto = puerto;
            this.nombre = nombre;
        }

        @Override
        public void run() {
            // connect() y readLine() esperan aqui sin bloquear la interfaz.
            try (Socket conexion = new Socket()) {
                socket = conexion;
                if (cerrado) return;
                conexion.connect(new InetSocketAddress(direccion, puerto), 5000);
                salida = new PrintWriter(conexion.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader entrada = new BufferedReader(new InputStreamReader(
                        conexion.getInputStream(), StandardCharsets.UTF_8));
                salida.println("PLAY " + nombre);
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    String recibido = mensaje;
                    Platform.runLater(() -> recibir(recibido));
                }
                Platform.runLater(() -> desconectado("Conexion terminada. Reinicia el cliente para conectar de nuevo."));
            } catch (IOException e) {
                Platform.runLater(() -> desconectado("Sin conexion. Reinicia el cliente para volver a intentarlo."));
            }
        }
    }
}
