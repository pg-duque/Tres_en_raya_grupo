import java.util.ArrayList;

// Guarda el tablero inicial y una copia tras cada jugada aceptada.
// Solo lo utiliza el hilo de JavaFX: no necesita synchronized.
public class Historial {
    final ArrayList<String> tableros = new ArrayList<>();
    String resultado = "";

    public void iniciar() {
        tableros.clear();
        resultado = "";
    }

    public void guardar(String tablero) {
        tableros.add(tablero);
    }

    public String texto() {
        StringBuilder texto = new StringBuilder("HISTORIAL DE TRES EN RAYA\n");
        for (int paso = 0; paso < tableros.size(); paso++) {
            String actual = tableros.get(paso);
            if (paso == 0) {
                texto.append("\nTablero inicial\n");
            } else {
                String anterior = tableros.get(paso - 1);
                for (int i = 0; i < 9; i++) {
                    if (actual.charAt(i) != anterior.charAt(i)) {
                        texto.append("\nJugada ").append(paso)
                                .append(": ").append(actual.charAt(i))
                                .append(" en fila ").append(i / 3 + 1)
                                .append(", columna ").append(i % 3 + 1).append('\n');
                    }
                }
            }
            for (int fila = 0; fila < 3; fila++) {
                texto.append(actual, fila * 3, fila * 3 + 3).append('\n');
            }
        }
        return texto.append("\nResultado: ").append(resultado).append('\n').toString();
    }
}
