import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class Servidor {
    private final Jugador[] jugadores = new Jugador[2];
    private final char[] tablero = new char[9];
    private int conectados = 0;
    private int turno = 0;
    private boolean iniciada = false;
    private boolean terminada = false;
    private volatile boolean cerrado = false;
    private ServerSocket servidor;

    public static void main(String[] args) {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 5050;
        new Servidor().ejecutar(puerto);
    }

    public void ejecutar(int puerto) {
        try (ServerSocket escucha = new ServerSocket(puerto)) {
            servidor = escucha;
            System.out.println("Servidor escuchando en el puerto " + puerto);
            while (true) {
                Socket socket = servidor.accept();
                aceptar(socket);
            }
        } catch (IOException e) {
            if (!cerrado) System.out.println("Error del servidor: " + e.getMessage());
        }
    }

    private synchronized void aceptar(Socket socket) throws IOException {
        if (conectados == 2) {
            try (Socket rechazado = socket) {
                new PrintWriter(rechazado.getOutputStream(), true).println("FULL");
            }
        } else {
            Jugador jugador = new Jugador(socket, this, conectados);
            jugadores[conectados] = jugador;
            conectados++;
            jugador.start();
        }
    }

    // Todos los hilos usan el mismo Servidor como objeto compartido.
    // synchronized impide que dos jugadores modifiquen la partida a la vez.
    public synchronized void registrar(Jugador jugador, String nombre) {
        if (jugador.nombre != null) {
            jugador.enviar("DENIED Ya estas registrado");
            return;
        }
        if (nombre.trim().isEmpty()) {
            jugador.enviar("DENIED Escribe PLAY seguido de tu nombre");
            return;
        }
        jugador.nombre = nombre.trim();
        jugador.enviar("INFO Tu marca es " + jugador.marca);
        if (conectados == 2 && jugadores[0].nombre != null
                && jugadores[1].nombre != null) {
            iniciar();
        } else {
            jugador.enviar("WAIT");
        }
    }

    private void iniciar() {
        Arrays.fill(tablero, '.');
        turno = 0;
        iniciada = true;
        terminada = false;
        for (Jugador jugador : jugadores) {
            jugador.revancha = false;
            jugador.enviar("START " + jugador.marca);
        }
        avisar("OK " + new String(tablero));
        avisar("TURN X");
    }

    public synchronized void mover(Jugador jugador, String datos) {
        if (!iniciada || jugador.nombre == null) {
            jugador.enviar("DENIED La partida todavia no ha empezado");
            return;
        }
        if (terminada) {
            jugador.enviar("DENIED La partida ha terminado");
            return;
        }
        if (jugador.numero != turno) {
            jugador.enviar("DENIED No es tu turno");
            return;
        }
        int fila;
        int columna;
        try {
            String[] partes = datos.split(",", -1);
            if (partes.length != 2) throw new NumberFormatException();
            fila = Integer.parseInt(partes[0].trim()) - 1;
            columna = Integer.parseInt(partes[1].trim()) - 1;
        } catch (NumberFormatException e) {
            jugador.enviar("DENIED Usa MOVE fila,columna (de 1 a 3)");
            return;
        }
        if (fila < 0 || fila > 2 || columna < 0 || columna > 2) {
            jugador.enviar("DENIED Fila y columna deben estar entre 1 y 3");
            return;
        }
        int posicion = fila * 3 + columna;
        if (tablero[posicion] != '.') {
            jugador.enviar("DENIED Casilla ocupada");
            return;
        }
        tablero[posicion] = jugador.marca;
        avisar("OK " + new String(tablero));
        if (haGanado(jugador.marca)) {
            terminada = true;
            avisar("END WIN " + jugador.marca);
        } else if (new String(tablero).indexOf('.') == -1) {
            terminada = true;
            avisar("END DRAW");
        } else {
            turno = 1 - turno;
            avisar("TURN " + jugadores[turno].marca);
        }
    }

    private boolean haGanado(char marca) {
        for (int i = 0; i < 3; i++) {
            // Comprobar una fila y una columna en cada vuelta.
            if (tablero[i * 3] == marca && tablero[i * 3 + 1] == marca
                    && tablero[i * 3 + 2] == marca) return true;
            if (tablero[i] == marca && tablero[i + 3] == marca
                    && tablero[i + 6] == marca) return true;
        }
        return (tablero[0] == marca && tablero[4] == marca && tablero[8] == marca)
                || (tablero[2] == marca && tablero[4] == marca && tablero[6] == marca);
    }

    public synchronized void pedirRevancha(Jugador jugador) {
        if (!terminada) {
            jugador.enviar("DENIED La partida no ha terminado");
            return;
        }
        jugador.revancha = true;
        jugador.enviar("INFO Revancha solicitada; esperando al otro jugador");
        if (jugadores[0].revancha && jugadores[1].revancha) iniciar();
    }

    private void avisar(String mensaje) {
        for (Jugador jugador : jugadores) {
            if (jugador != null) jugador.enviar(mensaje);
        }
    }

    public synchronized void desconectar(Jugador jugador) {
        if (cerrado) return;
        cerrado = true;
        if (iniciada && !terminada) {
            avisar("END WIN " + jugadores[1 - jugador.numero].marca + " ABANDONO");
        }
        avisar("INFO Un jugador se ha desconectado. Se cierra el servidor");
        for (Jugador otro : jugadores) {
            if (otro != null) otro.cerrar();
        }
        try {
            servidor.close();
        } catch (IOException e) {
            System.out.println("Error al cerrar el servidor: " + e.getMessage());
        }
    }
}
