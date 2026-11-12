import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

// Los componentes de la revision estan en historial.fxml.
public class ControladorHistorial {
    @FXML private Label paso;
    @FXML private Label ficha0;
    @FXML private Label ficha1;
    @FXML private Label ficha2;
    @FXML private Label ficha3;
    @FXML private Label ficha4;
    @FXML private Label ficha5;
    @FXML private Label ficha6;
    @FXML private Label ficha7;
    @FXML private Label ficha8;
    @FXML private Button anterior;
    @FXML private Button siguiente;
    private Label[] fichas;
    private ArrayList<String> tableros;
    private String texto;
    private int indice = 0;

    @FXML
    private void initialize() {
        fichas = new Label[] {ficha0, ficha1, ficha2, ficha3, ficha4,
                ficha5, ficha6, ficha7, ficha8};
    }

    public void cargar(Historial historial) {
        // La copia no cambia al comenzar una revancha.
        tableros = new ArrayList<>(historial.tableros);
        texto = historial.texto();
        indice = 0;
        dibujar();
    }

    private void dibujar() {
        String tablero = tableros.get(indice);
        for (int i = 0; i < 9; i++) {
            char ficha = tablero.charAt(i);
            fichas[i].setText(ficha == '.' ? "" : String.valueOf(ficha));
        }
        paso.setText("Jugada " + indice + " de " + (tableros.size() - 1));
        anterior.setDisable(indice == 0);
        siguiente.setDisable(indice == tableros.size() - 1);
    }

    @FXML
    private void anterior() {
        if (indice > 0) {
            indice--;
            dibujar();
        }
    }

    @FXML
    private void siguiente() {
        if (indice < tableros.size() - 1) {
            indice++;
            dibujar();
        }
    }

    @FXML
    private void guardar() {
        FileChooser selector = new FileChooser();
        selector.setInitialFileName("historial.txt");
        File archivo = selector.showSaveDialog(paso.getScene().getWindow());
        if (archivo == null) return;
        try {
            Files.writeString(archivo.toPath(), texto, StandardCharsets.UTF_8);
            paso.setText("Historial guardado.");
        } catch (IOException e) {
            paso.setText("No se ha podido guardar el archivo.");
        }
    }
}
