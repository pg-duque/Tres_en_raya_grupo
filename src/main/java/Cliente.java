import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

// Esta clase solo inicia la aplicacion y carga su vista.
public class Cliente extends Application {
    private ControladorCliente controlador;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage ventana) throws IOException {
        FXMLLoader cargador = new FXMLLoader(getClass().getResource("/vistas/cliente.fxml"));
        Parent vista = cargador.load();
        controlador = cargador.getController();
        controlador.setVentana(ventana);
        ventana.setTitle("Tres en raya con JavaFX y FXML");
        ventana.setScene(new Scene(vista));
        ventana.setOnCloseRequest(e -> Platform.exit());
        ventana.show();
    }

    @Override
    public void stop() {
        if (controlador != null) controlador.cerrar();
    }
}
