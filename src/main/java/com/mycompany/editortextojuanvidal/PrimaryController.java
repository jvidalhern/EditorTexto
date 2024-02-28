package com.mycompany.editortextojuanvidal;

import java.io.BufferedWriter;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;

public class PrimaryController {

    private Map<Tab, File> archivoPorPestana = new HashMap<>();
    // Mapa que relaciona cada pestaña con su archivo asociado
    private File archivoActual; // Variable para mantener una referencia al archivo actual
    @FXML
    private TabPane tabPane;

    private int numTabs = 1;
    @FXML
    private MenuItem nuevo;

    @FXML
    private Label longitudLabel;

    @FXML
    private Label numeroLineasLabel;

    @FXML
    private Label posicionLabel;

    @FXML
    private Label codificacionLabel;

    @FXML
    private TextArea textArea;

    @FXML
    private HBox barraEstado;

    @FXML
    private MenuItem ocultarVerBarraEstados;

    @FXML
    private Menu menuArchivo;

    @FXML
    private Button siguienteButton;

    @FXML
    private Menu menuArchivosRecientes;

    private File ultimoArchivoAbierto;

    private int contadorArchivosRecientes = 0;
    private final int MAX_ARCHIVOS_RECIENTES = 5;

    private final List<Integer> ocurrencias = new ArrayList<>();
    private int ocurrenciaActual = 0;
    private String palabraBuscar = "";

    /**
     * Método de inicialización, configura los listeners y actualiza la barra de
     * estado
     */
    public void initialize() {
        codificacionLabel.setText("Codificación:"); // Establece el texto predeterminado para la etiqueta de codificación
        barraEstado.setMargin(siguienteButton, new Insets(0, 0, 0, 10)); // Ajusta el margen del botón Siguiente

        // Listener para detectar el cambio de pestaña seleccionada
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                // Si se selecciona una nueva pestaña, actualizar la barra de estado
                TextArea areaTexto = (TextArea) newTab.getContent();
                actualizarBarraDeEstado(areaTexto);
                actualizarPosicionCursor(areaTexto);
            }
        });

        // Listener para detectar el cambio en el texto del área de texto
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            actualizarBarraDeEstado(textArea);
        });

        // Listener para detectar el cambio en la posición del cursor
        textArea.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
            actualizarPosicionCursor(textArea);
        });

        // Listener para detectar el cambio en el número de pestañas
        tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.getList().isEmpty()) {
                    System.out.println("No se puede cerrar la última pestaña.");
                    handleNuevo();
                }
            }
        });

        // Asignar un evento de cierre a cada pestaña
        for (Tab tab : tabPane.getTabs()) {
            tab.setOnCloseRequest(event -> {
                if (tabPane.getTabs().size() == 1) {
                    event.consume();
                }
            });
        }

        // Actualizar la barra de estado con la pestaña actual
        actualizarBarraDeEstado(textArea);
        actualizarPosicionCursor(textArea);

    }

    /**
     * Actualiza la barra de estado con información relevante sobre el texto en
     * el área de texto especificada.
     *
     * @param areaTexto El área de texto cuyo contenido se utilizará para
     * actualizar la barra de estado.
     */
    private void actualizarBarraDeEstado(TextArea areaTexto) {
        if (areaTexto != null) {
            // Longitud del texto
            int length = areaTexto.getText().length();
            longitudLabel.setText("Longitud: " + length + "   ");

            // Número de líneas
            int lines = areaTexto.getText().split("\n", -1).length;
            numeroLineasLabel.setText("Líneas: " + lines + "   ");
        } else {
            // Si no hay TextArea disponible, establecer los valores de la barra de estado en vacío
            longitudLabel.setText("Longitud: " + "   ");
            numeroLineasLabel.setText("Líneas: " + "   ");
        }
    }

    /**
     * Actualiza la etiqueta `posicionLabel` con la posición actual del cursor
     * en el área de texto especificada.
     *
     * @param areaTexto El área de texto cuyo cursor se utilizará para
     * actualizar la etiqueta de posición.
     */
    private void actualizarPosicionCursor(TextArea areaTexto) {
        int caretPosition = areaTexto.getCaretPosition();
        int line = areaTexto.getText(0, caretPosition).split("\n", -1).length;
        int column = caretPosition - areaTexto.getText(0, caretPosition).lastIndexOf('\n');
        posicionLabel.setText("Línea: " + line + ", Columna: " + column + "   ");
    }

    /**
     * Maneja la acción de crear una nueva pestaña en el editor. Crea una nueva
     * pestaña vacía y la agrega al TabPane.
     */
    @FXML
    private void handleNuevo() {
        if (tabPane.getTabs().size() < 10) {
            Tab nuevaPestana = new Tab("nuevo " + (numTabs + 1));
            TextArea areaTexto = new TextArea();
            // Hacer el área de texto editable
            areaTexto.setPrefSize(400, 390);
            areaTexto.setEditable(true);
            nuevaPestana.setContent(areaTexto);
            tabPane.getTabs().add(nuevaPestana);
            numTabs++;

            // Seleccionar la nueva pestaña
            tabPane.getSelectionModel().select(nuevaPestana);

            // Actualizar la barra de estado con la nueva pestaña
            actualizarBarraDeEstado(areaTexto);

            // Agregar listeners para actualizar la barra de estado al escribir y la posición del cursor
            areaTexto.textProperty().addListener((obs, oldText, newText) -> {
                actualizarBarraDeEstado(areaTexto);
            });
            areaTexto.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
                actualizarPosicionCursor(areaTexto);
            });
            handleArchivosRecientes(ultimoArchivoAbierto, menuArchivosRecientes);
        } else {
            // Mostrar un mensaje de advertencia si ya hay 10 pestañas activas
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Número máximo de pestañas alcanzado");
            alert.setHeaderText(null);
            alert.setContentText("Ya hay 10 pestañas activas. No se pueden agregar más.");
            alert.showAndWait();
        }
    }

    /**
     * Maneja la acción de abrir un archivo en una nueva pestaña. Abre un
     * diálogo de selección de archivo, lee el contenido del archivo
     * seleccionado y lo muestra en una nueva pestaña del editor.
     */
    @FXML
    private void handleAbrir() {
        // Verificar si ya hay 10 pestañas abiertas
        if (tabPane.getTabs().size() >= 10) {
            // Mostrar un mensaje de advertencia si ya hay 10 pestañas abiertas
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Número máximo de pestañas alcanzado");
            alert.setHeaderText(null);
            alert.setContentText("Ya hay 10 pestañas abiertas. No se pueden abrir más archivos.");
            alert.showAndWait();
            return; // Salir del método si se alcanza el límite de pestañas
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir Archivo");
        // Configurar filtros si es necesario
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            // Crear una nueva pestaña con el nombre del archivo
            Tab nuevaPestana = new Tab(file.getName());
            TextArea areaTexto = new TextArea();
            // Hacer el área de texto editable
            areaTexto.setEditable(true);
            nuevaPestana.setContent(areaTexto);

            // Asociar la nueva pestaña con el archivo en el mapa
            archivoPorPestana.put(nuevaPestana, file);

            // Leer el contenido del archivo y cargarlo en el TextArea
            try {
                List<String> lineas = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                StringBuilder contenido = new StringBuilder();
                for (String linea : lineas) {
                    contenido.append(linea).append("\n");
                }
                areaTexto.setText(contenido.toString());
                System.out.println("Archivo abierto: " + file.getAbsolutePath());

                // Obtener la codificación del archivo
                String codificacion = "UTF-8"; // Por defecto, usar UTF-8
                codificacionLabel.setText("Codificación: " + codificacion);
                // Probar si el archivo está en Latin-1
                if (Files.readAllLines(file.toPath(), Charset.forName("ISO-8859-1")).equals(lineas)) {
                    codificacion = "ISO-8859-1";
                    codificacionLabel.setText("Codificación: " + codificacion);
                }
                // Probar si el archivo está en ASCII
                if (Files.readAllLines(file.toPath(), StandardCharsets.US_ASCII).equals(lineas)) {
                    codificacion = "ASCII";
                    codificacionLabel.setText("Codificación: " + codificacion);
                }

                // Agregar un listener al evento textProperty del TextArea para actualizar la barra de estado al escribir
                areaTexto.textProperty().addListener((obs, oldText, newText) -> {
                    // Actualizar la barra de estado cada vez que el texto cambie
                    actualizarBarraDeEstado(areaTexto);
                });

                // Agregar un listener al evento caretPositionProperty del TextArea para actualizar la posición del cursor
                areaTexto.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
                    // Actualizar la posición del cursor cada vez que cambie
                    actualizarPosicionCursor(areaTexto);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Agregar la nueva pestaña al TabPane
            tabPane.getTabs().add(nuevaPestana);

            // Seleccionar la nueva pestaña
            tabPane.getSelectionModel().select(nuevaPestana);

            // Asignar el último archivo abierto
            ultimoArchivoAbierto = file;

            // Agregar el archivo abierto a la lista de archivos recientes
            handleArchivosRecientes(file, menuArchivosRecientes);
        }
    }

    /**
     * Maneja la acción de guardar el contenido de la pestaña actual en el
     * archivo asociado. Guarda el contenido del área de texto en el archivo
     * asociado a la pestaña actual. Si el archivo no tiene una ubicación
     * asociada, invoca el método handleGuardarComo().
     */
    @FXML
    private void handleGuardar() {
        // Obtener la pestaña actualmente seleccionada
        Tab pestañaActual = tabPane.getSelectionModel().getSelectedItem();

        if (pestañaActual == null) {
            // Si no hay pestaña seleccionada, mostrar un mensaje de advertencia
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Guardar");
            alert.setHeaderText(null);
            alert.setContentText("No hay ninguna pestaña abierta para guardar.");
            alert.showAndWait();
            return;
        }

        // Obtener el archivo asociado a la pestaña actual
        File archivo = archivoPorPestana.get(pestañaActual);

        if (archivo == null) {
            // Si no hay un archivo asociado, mostrar un mensaje de advertencia
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Guardar");
            alert.setHeaderText(null);
            alert.setContentText("No hay un archivo asociado a esta pestaña para guardar.");
            alert.showAndWait();
            return;
        }

        // Obtener el área de texto de la pestaña actual
        TextArea areaTexto = (TextArea) pestañaActual.getContent();

        if (areaTexto == null) {
            // Si no se puede obtener el área de texto, mostrar un mensaje de error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al guardar");
            alert.setHeaderText(null);
            alert.setContentText("No se puede acceder al contenido de la pestaña actual.");
            alert.showAndWait();
            return;
        }

        // Guardar el contenido del área de texto en el archivo
        try (PrintWriter writer = new PrintWriter(archivo)) {
            writer.write(areaTexto.getText());
            System.out.println("Contenido guardado en: " + archivo.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // Mostrar un mensaje de error si no se puede guardar el archivo
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al guardar");
            alert.setHeaderText(null);
            alert.setContentText("No se puede guardar el contenido en el archivo: " + archivo.getName());
            alert.showAndWait();
        }
    }

    /**
     * Maneja la acción de archivos recientes. Agrega el archivo recientemente
     * abierto al menú de archivos recientes.
     */
    private void handleArchivosRecientes(File ultimoArchivoAbierto, Menu menuArchivosRecientes) {
        if (ultimoArchivoAbierto != null && menuArchivosRecientes != null) {
            // Construir el nombre del archivo para mostrar en el menú de archivos recientes
            String nombreArchivo = (++contadorArchivosRecientes) + ": " + ultimoArchivoAbierto.getName();
            // Crear un nuevo elemento de menú para el archivo reciente
            MenuItem menuItem = new MenuItem(nombreArchivo);
            // Configurar la acción del elemento de menú para abrir el archivo reciente
            menuItem.setOnAction(event -> abrirArchivo(ultimoArchivoAbierto)); // Llama a un método para abrir el archivo

            // Obtener la lista de elementos de menú del menú de archivos recientes
            ObservableList<MenuItem> items = menuArchivosRecientes.getItems();

            // Si ya hay MAX_ARCHIVOS_RECIENTES elementos, eliminar el más antiguo para mantener el límite
            if (items.size() >= MAX_ARCHIVOS_RECIENTES) {
                items.remove(0);
            }

            // Agregar el nuevo elemento al final del menú
            items.add(menuItem);
        }
    }

    /**
     * Abre el archivo especificado en una nueva pestaña.
     *
     * @param archivo El archivo que se abrirá.
     */
    private void abrirArchivo(File archivo) {
        // Verificar si ya hay 10 pestañas abiertas
        if (tabPane.getTabs().size() >= 10) {
            // Mostrar una alerta si se alcanza el límite de pestañas
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Número máximo de pestañas alcanzado");
            alert.setHeaderText(null);
            alert.setContentText("Ya hay 10 pestañas abiertas. No se pueden abrir más archivos.");
            alert.showAndWait();
            return; // Salir del método si se alcanza el límite de pestañas
        }

        // Crear una nueva pestaña con el nombre del archivo
        Tab nuevaPestana = new Tab(archivo.getName());
        TextArea areaTexto = new TextArea();
        // Hacer el área de texto editable
        areaTexto.setEditable(true);
        nuevaPestana.setContent(areaTexto);

        // Asociar la nueva pestaña con el archivo en el mapa
        archivoPorPestana.put(nuevaPestana, archivo);

        // Leer el contenido del archivo y cargarlo en el TextArea
        try {
            // Leer todas las líneas del archivo
            List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
            StringBuilder contenido = new StringBuilder();
            for (String linea : lineas) {
                contenido.append(linea).append("\n");
            }
            // Establecer el texto del TextArea con el contenido del archivo
            areaTexto.setText(contenido.toString());

            // Obtener la codificación del archivo
            String codificacion = "UTF-8"; // Por defecto, usar UTF-8
            codificacionLabel.setText("Codificación: " + codificacion);
            // Probar si el archivo está en Latin-1
            if (Files.readAllLines(archivo.toPath(), Charset.forName("ISO-8859-1")).equals(lineas)) {
                codificacion = "ISO-8859-1";
                codificacionLabel.setText("Codificación: " + codificacion);
            }
            // Probar si el archivo está en ASCII
            if (Files.readAllLines(archivo.toPath(), StandardCharsets.US_ASCII).equals(lineas)) {
                codificacion = "ASCII";
                codificacionLabel.setText("Codificación: " + codificacion);
            }

            // Agregar listeners para actualizar la barra de estado y la posición del cursor al escribir o cambiar la selección
            areaTexto.textProperty().addListener((obs, oldText, newText) -> {
                actualizarBarraDeEstado(areaTexto);
            });
            areaTexto.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
                actualizarPosicionCursor(areaTexto);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Agregar la nueva pestaña al TabPane
        tabPane.getTabs().add(nuevaPestana);

        // Seleccionar la nueva pestaña
        tabPane.getSelectionModel().select(nuevaPestana);

        // Establecer el último archivo abierto y actualizar la lista de archivos recientes en el menú Archivo
        ultimoArchivoAbierto = archivo;
        handleArchivosRecientes(ultimoArchivoAbierto, menuArchivosRecientes); // Llamar a handleArchivosRecientes con el archivo abierto
    }

    /**
     * Maneja la acción de guardar el contenido de la pestaña actual en un
     * archivo.
     */
    @FXML
    private void handleGuardarComo() {
        // Obtener la pestaña actualmente seleccionada
        Tab pestañaActual = tabPane.getSelectionModel().getSelectedItem();

        if (pestañaActual == null) {
            // Si no hay pestaña seleccionada, mostrar un mensaje de advertencia
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Guardar");
            alert.setHeaderText(null);
            alert.setContentText("No hay ninguna pestaña abierta para guardar.");
            alert.showAndWait();
            return;
        }

        // Obtener el área de texto de la pestaña actual
        TextArea areaTexto = (TextArea) pestañaActual.getContent();

        if (areaTexto == null) {
            // Si no se puede obtener el área de texto, mostrar un mensaje de error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al guardar");
            alert.setHeaderText(null);
            alert.setContentText("No se puede acceder al contenido de la pestaña actual.");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Archivo Como");

        // Establecer el filtro de extensión si es necesario
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos de texto (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        // Mostrar el diálogo para seleccionar la ubicación y el nombre del archivo
        File archivo = fileChooser.showSaveDialog(null);

        if (archivo != null) {
            // Crear el ChoiceBox para que el usuario seleccione la codificación
            ChoiceBox<String> choiceBoxCodificacion = new ChoiceBox<>();
            choiceBoxCodificacion.getItems().addAll("UTF-8", "ISO-8859-1", "ASCII");
            choiceBoxCodificacion.setValue("UTF-8");

            // Crear el diálogo con el ChoiceBox
            Dialog<ButtonType> dialogCodificacion = new Dialog<>();
            dialogCodificacion.setTitle("Seleccionar Codificación");
            dialogCodificacion.setHeaderText(null);
            dialogCodificacion.getDialogPane().setContent(choiceBoxCodificacion);

            // Añadir botones de OK y Cancelar al diálogo
            dialogCodificacion.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Mostrar el diálogo y esperar a que el usuario seleccione una opción
            Optional<ButtonType> result = dialogCodificacion.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String codificacionSeleccionada = choiceBoxCodificacion.getValue();

                // Obtener el contenido del área de texto
                String contenido = areaTexto.getText();

                // Guardar el contenido del área de texto en el archivo seleccionado con la codificación especificada
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archivo), Charset.forName(codificacionSeleccionada)))) {
                    // Convertir caracteres fuera del rango de Latin-1 a su forma de escape Unicode si la codificación es ISO-8859-1
                    if (codificacionSeleccionada.equals("ISO-8859-1")) {
                        contenido = escapeUnicode(contenido);
                    }
                    writer.write(contenido);
                    System.out.println("Contenido guardado en: " + archivo.getAbsolutePath());

                    // Establecer el nombre del archivo como el texto de la pestaña
                    pestañaActual.setText(archivo.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                    // Mostrar un mensaje de error si no se puede guardar el archivo
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error al guardar");
                    alert.setHeaderText(null);
                    alert.setContentText("No se puede guardar el contenido en el archivo: " + archivo.getName());
                    alert.showAndWait();
                }
            }
        }
    }

    /**
     * Escapa los caracteres fuera del rango de Latin-1 a su forma de escape
     * Unicode. Recibe una cadena de entrada y devuelve una nueva cadena donde
     * los caracteres fuera del rango de Latin-1 están representados como su
     * forma de escape Unicode.
     *
     * @param input La cadena de entrada que se va a procesar.
     * @return Una nueva cadena donde los caracteres fuera del rango de Latin-1
     * están escapados en Unicode.
     */
    private String escapeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c > 255) {
                sb.append("\\u").append(String.format("%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Maneja el evento de salida de la aplicación. Muestra un diálogo de
     * confirmación para que el usuario confirme si desea cerrar la aplicación.
     * Si el usuario elige "Sí", la aplicación se cierra; de lo contrario, no
     * sucede nada.
     */
    @FXML
    private void handleSalir() {
        // Crear un diálogo de confirmación
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación");
        alert.setHeaderText(null);
        alert.setContentText("¿Está usted seguro de querer cerrar la aplicación?");

        // Configurar los botones de Sí y No
        ButtonType botonSi = new ButtonType("Sí");
        ButtonType botonNo = new ButtonType("No");
        alert.getButtonTypes().setAll(botonSi, botonNo);

        // Mostrar el diálogo y esperar a que el usuario seleccione una opción
        Optional<ButtonType> resultado = alert.showAndWait();
        if (resultado.isPresent() && resultado.get() == botonSi) {
            // Si el usuario elige "Sí", cerrar la aplicación
            System.exit(0);
        }
    }

    /**
     * Maneja el evento de búsqueda de texto en el área de texto de la pestaña
     * actual. Muestra un cuadro de diálogo para que el usuario ingrese la
     * palabra a buscar. Luego, busca la palabra en el texto del área de texto,
     * resaltando las ocurrencias y mostrando un mensaje si no se encuentra
     * ninguna coincidencia.
     */
    @FXML
    private void handleBuscar() {
        // Obtener la pestaña actualmente seleccionada
        Tab pestañaActual = tabPane.getSelectionModel().getSelectedItem();

        if (pestañaActual == null) {
            // Si no hay pestaña seleccionada, mostrar un mensaje de advertencia
            mostrarAlerta("Buscar", "No hay ninguna pestaña abierta para buscar.", Alert.AlertType.WARNING);
            return;
        }

        // Obtener el área de texto de la pestaña actual
        TextArea areaTexto = (TextArea) pestañaActual.getContent();

        if (areaTexto == null) {
            // Si no se puede obtener el área de texto, mostrar un mensaje de error
            mostrarAlerta("Error al buscar", "No se puede acceder al contenido de la pestaña actual.", Alert.AlertType.ERROR);
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Buscar");
        dialog.setHeaderText(null);
        dialog.setContentText("Introduce la palabra a buscar:");

        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isPresent()) {
            palabraBuscar = removerTildes(resultado.get().toLowerCase()); // Asignar la palabra buscada

            // Obtener el texto del área de texto y convertirlo a minúsculas y sin tildes para la búsqueda sin distinguir mayúsculas
            String texto = removerTildes(areaTexto.getText().toLowerCase());

            ocurrencias.clear();
            int indice = texto.indexOf(palabraBuscar);
            while (indice != -1) {
                ocurrencias.add(indice);
                indice = texto.indexOf(palabraBuscar, indice + 1);
            }

            if (!ocurrencias.isEmpty()) {
                // Se encontraron ocurrencias, resaltar la primera ocurrencia y configurar ocurrenciaActual a 0
                ocurrenciaActual = 0;
                resaltarSiguienteOcurrencia(areaTexto);
                // Mostrar el botón "Siguiente"
                siguienteButton.setVisible(true);
            } else {
                // Si no se encuentra ninguna coincidencia
                mostrarAlerta("Buscar", "No se ha encontrado ninguna coincidencia.", Alert.AlertType.INFORMATION);
                // Ocultar el botón "Siguiente"
                siguienteButton.setVisible(false);
            }
        } else {
            // El usuario canceló la búsqueda
            System.out.println("Búsqueda cancelada.");
        }
    }

    /**
     * Maneja el evento para resaltar la siguiente ocurrencia de la palabra
     * buscada en el texto del área de texto. Avanza al siguiente índice de
     * ocurrencia y resalta la palabra si hay más ocurrencias disponibles. Si no
     * hay más ocurrencias, el botón siguiente se oculta.
     */
    @FXML
    private void handleSiguiente() {
        if (!ocurrencias.isEmpty()) {
            // Avanzar a la siguiente ocurrencia (o volver a la primera si se llega al final)
            ocurrenciaActual = (ocurrenciaActual + 1) % ocurrencias.size();
            resaltarSiguienteOcurrencia((TextArea) tabPane.getSelectionModel().getSelectedItem().getContent());
        }
    }

    /**
     * Resalta la siguiente ocurrencia de la palabra buscada en el texto del
     * área de texto. Mueve el cursor al índice de la siguiente ocurrencia y
     * selecciona la palabra. Si no hay más ocurrencias disponibles después de
     * la actual, desactiva el botón "Siguiente".
     *
     * @param areaTexto El área de texto donde se busca y resalta la siguiente
     * ocurrencia.
     */
    private void resaltarSiguienteOcurrencia(TextArea areaTexto) {
        int indice = ocurrencias.get(ocurrenciaActual);
        areaTexto.positionCaret(indice);
        areaTexto.selectRange(indice, indice + palabraBuscar.length());
        areaTexto.requestFocus(); // Colocar el foco en el área de texto
    }

    /**
     * Muestra un cuadro de diálogo de alerta con el título, contenido y tipo
     * especificados.
     *
     * @param titulo El título del cuadro de diálogo de alerta.
     * @param contenido El contenido del cuadro de diálogo de alerta.
     * @param tipo El tipo de alerta (INFORMATION, WARNING, ERROR, etc.).
     */
    private void mostrarAlerta(String titulo, String contenido, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    /**
     * Remueve las tildes de un texto dado y lo retorna.
     *
     * @param texto El texto del cual se quieren remover las tildes.
     * @return El texto sin tildes.
     */
    private String removerTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Maneja la funcionalidad de reemplazar una palabra en el área de texto
     * actualmente seleccionada.
     */
    @FXML
    private void handleReemplazar() {
        // Obtener la pestaña actualmente seleccionada
        Tab pestañaActual = tabPane.getSelectionModel().getSelectedItem();

        if (pestañaActual == null) {
            // Si no hay pestaña seleccionada, mostrar un mensaje de advertencia
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Reemplazar");
            alert.setHeaderText(null);
            alert.setContentText("No hay ninguna pestaña abierta para reemplazar.");
            alert.showAndWait();
            return;
        }

        // Obtener el área de texto de la pestaña actual
        TextArea areaTexto = (TextArea) pestañaActual.getContent();

        if (areaTexto == null) {
            // Si no se puede obtener el área de texto, mostrar un mensaje de error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al reemplazar");
            alert.setHeaderText(null);
            alert.setContentText("No se puede acceder al contenido de la pestaña actual.");
            alert.showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reemplazar");
        dialog.setHeaderText(null);
        dialog.setContentText("Introduce la palabra a buscar:");

        Optional<String> resultadoBusqueda = dialog.showAndWait();
        if (resultadoBusqueda.isPresent()) {
            String palabraBuscar = resultadoBusqueda.get(); // Palabra a buscar

            dialog = new TextInputDialog();
            dialog.setTitle("Reemplazar");
            dialog.setHeaderText(null);
            dialog.setContentText("Introduce la palabra de reemplazo:");

            Optional<String> resultadoReemplazo = dialog.showAndWait();
            if (resultadoReemplazo.isPresent()) {
                String palabraReemplazo = resultadoReemplazo.get(); // Palabra de reemplazo

                // Realizar la búsqueda en el texto
                String texto = areaTexto.getText();
                if (!texto.contains(palabraBuscar)) {
                    // Si la palabra no se encuentra en el texto, mostrar un mensaje de advertencia
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Reemplazar");
                    alert.setHeaderText(null);
                    alert.setContentText("No se ha encontrado ninguna coincidencia.");
                    alert.showAndWait();
                    return;
                }

                // Realizar el reemplazo en el texto
                texto = texto.replaceAll(palabraBuscar, palabraReemplazo);
                areaTexto.setText(texto);
            }
        } else {
            // El usuario canceló la búsqueda
            System.out.println("Reemplazo cancelado.");
        }
    }

    /**
     * Maneja la funcionalidad de convertir a mayúsculas el texto seleccionado
     * en el área de texto.
     */
    @FXML
    private void handleConvertirAMayusculas() {
        // Obtener la pestaña actualmente seleccionada
        Tab pestañaActual = tabPane.getSelectionModel().getSelectedItem();

        if (pestañaActual == null) {
            // Si no hay pestaña seleccionada, mostrar un mensaje de advertencia
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Convertir a Mayúsculas");
            alert.setHeaderText(null);
            alert.setContentText("No hay ninguna pestaña abierta para convertir el texto.");
            alert.showAndWait();
            return;
        }

        // Obtener el área de texto de la pestaña actual
        TextArea areaTexto = (TextArea) pestañaActual.getContent();

        if (areaTexto == null) {
            // Si no se puede obtener el área de texto, mostrar un mensaje de error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al convertir a Mayúsculas");
            alert.setHeaderText(null);
            alert.setContentText("No se puede acceder al contenido de la pestaña actual.");
            alert.showAndWait();
            return;
        }

        // Obtener el texto seleccionado y convertirlo a mayúsculas
        String textoSeleccionado = areaTexto.getSelectedText();
        if (!textoSeleccionado.isEmpty()) {
            areaTexto.replaceSelection(textoSeleccionado.toUpperCase());
        }
    }

    /**
     * Maneja la funcionalidad de convertir a minúsculas el texto seleccionado
     * en el área de texto.
     */
    @FXML
    private void handleConvertirAMinusculas() {
        // Obtener la pestaña actualmente seleccionada
        Tab pestañaActual = tabPane.getSelectionModel().getSelectedItem();

        if (pestañaActual == null) {
            // Si no hay pestaña seleccionada, mostrar un mensaje de advertencia
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Convertir a Minúsculas");
            alert.setHeaderText(null);
            alert.setContentText("No hay ninguna pestaña abierta para convertir el texto.");
            alert.showAndWait();
            return;
        }

        // Obtener el área de texto de la pestaña actual
        TextArea areaTexto = (TextArea) pestañaActual.getContent();

        if (areaTexto == null) {
            // Si no se puede obtener el área de texto, mostrar un mensaje de error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al convertir a Minúsculas");
            alert.setHeaderText(null);
            alert.setContentText("No se puede acceder al contenido de la pestaña actual.");
            alert.showAndWait();
            return;
        }

        // Obtener el texto seleccionado y convertirlo a minúsculas
        String textoSeleccionado = areaTexto.getSelectedText();
        if (!textoSeleccionado.isEmpty()) {
            areaTexto.replaceSelection(textoSeleccionado.toLowerCase());
        }
    }

    /**
     * Maneja la funcionalidad de convertir la primera letra de cada oración a
     * mayúsculas en el texto seleccionado.
     *
     * @param event El evento que desencadena la acción.
     */
    @FXML
    private void handleOracionAMayusculas(ActionEvent event) {
        // Obtener el texto seleccionado
        String selectedText = textArea.getSelectedText();

        // Verificar si hay texto seleccionado
        if (selectedText != null && !selectedText.isEmpty()) {
            // Dividir el texto en oraciones utilizando el punto seguido de un espacio como delimitador
            String[] oraciones = selectedText.split("\\.\\s+");

            // Recorrer cada oración y convertir la primera letra a mayúscula
            StringBuilder newText = new StringBuilder();
            for (String oracion : oraciones) {
                if (!oracion.isEmpty()) {
                    // Convertir la primera letra de la oración a mayúscula
                    String primeraLetraMayuscula = oracion.substring(0, 1).toUpperCase();
                    // Concatenar el resto de la oración y agregarla al texto modificado
                    newText.append(primeraLetraMayuscula).append(oracion.substring(1)).append(". ");
                }
            }

            // Reemplazar el texto seleccionado con el texto modificado
            textArea.replaceSelection(newText.toString());
        }
    }

    /**
     * Maneja la funcionalidad para ocultar la barra de estado.
     */
    @FXML
    private void handleOcultarBarraDeEstado() {
        // Obtener el valor actual del texto del item de menú
        String menuItemText = ocultarVerBarraEstados.getText();

        // Verificar el texto actual del item de menú
        if (menuItemText.equals("Ocultar barra de estados")) {
            // Si el texto actual es "Ocultar barra de estados", ocultar la barra de estado
            barraEstado.setVisible(false);
            // Cambiar el texto del item de menú a "Ver barra de estados"
            ocultarVerBarraEstados.setText("Ver barra de estados");
        } else {
            // Si el texto actual no es "Ocultar barra de estados", mostrar la barra de estado
            barraEstado.setVisible(true);
            // Cambiar el texto del item de menú a "Ocultar barra de estados"
            ocultarVerBarraEstados.setText("Ocultar barra de estados");
        }
    }

    /**
     * Maneja la acción de mostrar la ventana "Acerca de".
     *
     * @param event El evento que desencadena esta acción.
     */
    @FXML
    private void handleAcercaDe(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de");
        alert.setHeaderText("Editor de texto sencillo de Juan José Vidal Hernández");

        // Crear un componente Text para mostrar el contenido
        Text contenido = new Text("Este es un editor de texto sencillo desarrollado por Juan José Vidal Hernández para la asignatura Desarrollo de Interfaces, a fecha 28/02/2024.");

        // Configurar el panel de diálogo personalizado
        GridPane gridPane = new GridPane();
        gridPane.setMaxWidth(Double.MAX_VALUE);
        gridPane.add(contenido, 0, 0);

        // Establecer el panel personalizado en el cuadro de diálogo
        alert.getDialogPane().setContent(gridPane);

        alert.showAndWait();
    }

}
