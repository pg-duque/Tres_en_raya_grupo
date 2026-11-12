import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Cada jugador tiene su propio hilo, como en los ejercicios de extends Thread.
public class Jugador extends Thread {
    private final Socket socket;
    private final Servidor servidor;
    private final BufferedReader entrada;
    private final PrintWriter salida;
    final int numero;
    final char marca;
    String nombre;
    boolean revancha = false;

    public Jugador(Socket socket, Servidor servidor, int numero) throws IOException {
        this.socket = socket;
        this.servidor = servidor;
        this.numero = numero;
        marca = numero == 0 ? 'X' : 'O';
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        salida = new PrintWriter(socket.getOutputStream(), true, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public void run() {
        try {
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                if (mensaje.startsWith("PLAY ")) {
                    servidor.registrar(this, mensaje.substring(5));
                } else if (mensaje.startsWith("MOVE ")) {
                    servidor.mover(this, mensaje.substring(5));
                } else if (mensaje.equals("REMATCH")) {
                    servidor.pedirRevancha(this);
                } else if (mensaje.equals("BYE")) {
                    break;
                } else {
                    enviar("DENIED Comando desconocido");
                }
            }
        } catch (IOException e) {
            System.out.println("Conexion cerrada con el jugador " + marca);
        } finally {
            servidor.desconectar(this);
        }
    }

    public void enviar(String mensaje) {
        salida.println(mensaje);
    }

    public void cerrar() {
        try {
            // Cerrar el socket cierra tambien sus flujos y desbloquea readLine().
            socket.close();
        } catch (IOException e) {
            System.out.println("Error al cerrar el jugador " + marca);
        }
    }
}
