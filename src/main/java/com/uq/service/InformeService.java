package com.uq.service;

// Imports necesarios para PDFBox
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;


import com.uq.dto.ProgramaDTO;
import com.uq.dto.ComentarioDTO;
import com.uq.model.Estudiante;
import com.uq.repository.EstudianteRepository;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class InformeService {

    private static final Logger LOGGER = Logger.getLogger(InformeService.class.getName());
    private static final float MARGIN = 25f;
    private static final float START_Y = 750f;
    private static final float LINE_HEIGHT_NORMAL = 14.5f;
    private static final float LINE_HEIGHT_CODE = 10f;

    private static float CONTENT_WIDTH = 0;


    @Inject
    ProgramaService programaService;

    @Inject ComentarioService comentarioService;

    @Inject EstudianteRepository estudianteRepository;

    // Fuentes de PDFBox (cargadas una vez)
    private PDType1Font fontBold;
    private PDType1Font fontRegular;
    private PDType1Font fontItalic;
    private PDType1Font fontCode;

    @Inject
    public InformeService() {
        fontBold = new PDType1Font(FontName.HELVETICA_BOLD);
        fontRegular = new PDType1Font(FontName.HELVETICA);
        fontItalic = new PDType1Font(FontName.HELVETICA_OBLIQUE);
        fontCode = new PDType1Font(FontName.COURIER);
    }


    // Metodo para generar el informe PDF con más detalles
    public byte[] generateStudentProgressReport() throws IOException {

        // Verificar que las fuentes se cargaron correctamente
        if (fontBold == null || fontRegular == null || fontItalic == null || fontCode == null) {
            throw new IOException("Las fuentes de PDFBox no se cargaron correctamente. Consulta los logs de inicio.");
        }


        LOGGER.info("Generando informe de progreso de estudiantes detallado...");

        List<ProgramaDTO> programas;
        try {
            programas = programaService.listAllPrograms();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener programas para el informe.", e);
            throw new IOException("Error al obtener datos de programas para el informe.", e);
        }

        LOGGER.log(Level.INFO, "Obtenidos {0} programas para incluir en el informe.", programas.size());

        try (PDDocument document = new PDDocument()) {

            // Crear la primera página o una página de "no hay datos"
            if (programas.isEmpty()) {
                addEmptyReportPage(document, fontBold, fontRegular);
            } else {
                // Iterar sobre los programas y añadir una página por cada uno
                for (ProgramaDTO programa : programas) {
                    // Obtener información adicional del estudiante
                    Estudiante estudianteEntity = null; // Usar la entidad Estudiante
                    String studentName = "Desconocido";
                    String studentEmail = "N/A";

                    if (programa.getEstudianteId() != null) {
                        try {
                            // Usar el repositorio para encontrar la entidad Estudiante
                            estudianteEntity = estudianteRepository.findById(programa.getEstudianteId());

                            if (estudianteEntity != null) {
                                studentName = estudianteEntity.getNombre();
                                studentEmail = estudianteEntity.getEmail();
                            } else {
                                LOGGER.log(Level.WARNING, "Estudiante con ID {0} no encontrado para el programa {1}.", new Object[]{programa.getEstudianteId(), programa.getId()});
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error al obtener datos de estudiante para programa " + programa.getId(), e);
                            studentName = "Error";
                            studentEmail = "Error";
                        }
                    }

                    // Obtener comentarios del programa
                    List<ComentarioDTO> comentarios;
                    try {
                        comentarios = comentarioService.listCommentsForProgramNoAuth(programa.getId());
                    } catch (Exception e) { // Captura cualquier error al obtener comentarios
                        LOGGER.log(Level.WARNING, "Error al obtener comentarios para el programa {0}", programa.getId());
                        comentarios = java.util.Collections.emptyList(); // Retornar lista vacía en caso de error
                    }

                    // Añadir una nueva página para este programa
                    addProgramPage(document, programa, studentName, studentEmail, comentarios, fontBold, fontRegular, fontItalic, fontCode);
                }
            }

            // 3. Guardar el documento en un ByteArrayOutputStream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);

            LOGGER.info("Informe PDF generado exitosamente.");
            return baos.toByteArray();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error de I/O al generar el informe PDF.", e);
            throw e;
        } catch (Exception e) {
            // Captura cualquier otra excepción inesperada durante la generación del PDF
            LOGGER.log(Level.SEVERE, "Error inesperado al generar el informe PDF.", e);
            throw new IOException("Error interno al generar el informe: " + e.getMessage(), e);
        }
    }

    /**
     * Añade una página al documento indicando que no hay programas disponibles
     */
    private void addEmptyReportPage(PDDocument document, PDType1Font fontBold, PDType1Font fontRegular) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        float pageHeight = page.getMediaBox().getHeight();
        CONTENT_WIDTH = page.getMediaBox().getWidth() - 2 * MARGIN;


        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.setFont(fontBold, 12);
            contentStream.beginText();
            contentStream.setLeading(LINE_HEIGHT_NORMAL);
            contentStream.newLineAtOffset(MARGIN, pageHeight - MARGIN);
            contentStream.showText("Informe de Progreso de Estudiantes");
            contentStream.newLine();
            contentStream.newLine();

            contentStream.setFont(fontRegular, 10);
            contentStream.showText("No hay programas disponibles para el informe.");
            contentStream.endText();
        }
    }

    /**
     * Añade una página al documento con la información detallada de un programa
     */
    private void addProgramPage(PDDocument document, ProgramaDTO programa, String studentName, String studentEmail,
                                List<ComentarioDTO> comentarios, PDType1Font fontBold, PDType1Font fontRegular,
                                PDType1Font fontItalic, PDType1Font fontCode) throws IOException {

        PDPage page = new PDPage();
        document.addPage(page);
        float pageHeight = page.getMediaBox().getHeight();
        float pageActualWidth = page.getMediaBox().getWidth();
        CONTENT_WIDTH = pageActualWidth - 2 * MARGIN;

        float yPosition = pageHeight - MARGIN;


        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setLeading(LINE_HEIGHT_NORMAL);

            contentStream.setFont(fontBold, 8);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Informe de Progreso de Estudiantes - Programa ID: " + programa.getId());
            yPosition -= LINE_HEIGHT_NORMAL;
            contentStream.newLine();


            // Información del programa
            contentStream.setFont(fontBold, 11);
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Información del Programa");
            contentStream.setFont(fontRegular, 10);
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("ID: " + programa.getId());
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Título: " + programa.getTitulo());
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Estado: " + (programa.isResuelto() ? "Resuelto" : "No resuelto"));
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Compartido: " + (programa.isShared() ? "Sí" : "No"));
            yPosition -= LINE_HEIGHT_NORMAL * 2; contentStream.newLine(); contentStream.newLine();


            // Información del estudiante
            contentStream.setFont(fontBold, 11);
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Información del Estudiante");
            contentStream.setFont(fontRegular, 10);
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("ID: " + programa.getEstudianteId());
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Nombre: " + studentName);
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Email: " + studentEmail);
            yPosition -= LINE_HEIGHT_NORMAL * 2; contentStream.newLine(); contentStream.newLine();

            // Código del programa
            contentStream.setFont(fontBold, 11);
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Código del Programa");
            contentStream.setFont(fontCode, 9);
            contentStream.setLeading(LINE_HEIGHT_CODE);
            yPosition -= LINE_HEIGHT_CODE; contentStream.newLine();

            String codigoFuente = programa.getCodigoFuente();
            if (codigoFuente != null && !codigoFuente.isEmpty()) {
                yPosition = wrapAndShowText(contentStream, codigoFuente, yPosition, pageHeight, pageActualWidth, MARGIN, LINE_HEIGHT_CODE, fontCode);
            } else {
                contentStream.showText("No hay código disponible.");
                yPosition -= LINE_HEIGHT_CODE; contentStream.newLine();
            }
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine();
            contentStream.setFont(fontRegular, 10);
            contentStream.setLeading(LINE_HEIGHT_NORMAL);


            // Comentarios
            contentStream.setFont(fontBold, 11);
            yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Comentarios");

            if (comentarios.isEmpty()) {
                contentStream.setFont(fontItalic, 10);
                yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("No hay comentarios para este programa.");
            } else {
                contentStream.setFont(fontRegular, 10);
                yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine();

                for (int i = 0; i < comentarios.size(); i++) {
                    ComentarioDTO comentario = comentarios.get(i);

                    contentStream.setFont(fontBold, 10);
                    yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Comentario #" + (i+1) + " por " + comentario.getProfesorNombre());

                    contentStream.setFont(fontRegular, 10);
                    yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Fecha: " + comentario.getFecha()); // Puedes formatear la fecha

                    contentStream.setFont(fontItalic, 10);
                    yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine(); contentStream.showText("Comentario:");
                    yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine();


                    if (comentario.getTexto() != null && !comentario.getTexto().isEmpty()) {
                        // Usamos el método mejorado para mostrar el texto del comentario como párrafo continuo
                        yPosition = wrapAndShowTextAsFlowingParagraph(contentStream, comentario.getTexto(), yPosition, pageHeight, pageActualWidth, MARGIN + 10, LINE_HEIGHT_NORMAL, fontItalic);
                    } else {
                        contentStream.showText("(Comentario vacío)");
                        yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine();
                    }
                    yPosition -= LINE_HEIGHT_NORMAL; contentStream.newLine();
                }
            }

            contentStream.endText();

        }
    }

    /**
     * Método original para mostrar texto (utilizado para código fuente)
     */
    private float wrapAndShowText(PDPageContentStream contentStream, String text, float startY, float pageHeight, float pageWidth, float margin, float leading, PDType1Font font) throws IOException {
        float currentY = startY;
        int fontSize = (int) font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000;
        float contentWidth = pageWidth - 2 * margin;

        List<String> lines = new java.util.ArrayList<>();
        String remainingText = text;

        String[] words = remainingText.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (!currentLine.toString().isEmpty()) {
                float size = font.getStringWidth(currentLine.toString() + " " + word) / 1000 * font.getFontDescriptor().getCapHeight();
                if (size > contentWidth) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine.append(" ").append(word);
                }
            } else {
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }


        for (String line : lines) {
            contentStream.showText(line);
            currentY -= leading;
            contentStream.newLine();
        }

        return currentY;
    }

    /**
     * Metodo para mostrar texto como párrafo continuo (utilizado para comentarios)
     */
    private float wrapAndShowTextAsFlowingParagraph(PDPageContentStream contentStream, String text, float startY, float pageHeight, float pageWidth, float margin, float leading, PDType1Font font) throws IOException {
        float currentY = startY;
        float contentWidth = pageWidth - 2 * margin;

        // Primero procesamos el texto para reemplazar múltiples saltos de línea con espacios
        // y preservar solo los saltos de línea necesarios para párrafos
        String processedText = text.replaceAll("\\s*\\n\\s*", " ").trim();

        // Dividimos las palabras para procesamiento
        String[] words = processedText.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // Si la línea actual está vacía, comenzamos con esta palabra
            if (currentLine.length() == 0) {
                currentLine.append(word);
                continue;
            }

            // Verificamos si añadir esta palabra excedería el ancho de contenido
            String testLine = currentLine + " " + word;
            float lineWidth = font.getStringWidth(testLine) / 1000 * 10; // 10 es un factor aproximado para la conversión

            // Si excede el ancho disponible, mostramos la línea actual y comenzamos una nueva
            if (lineWidth > contentWidth) {
                contentStream.showText(currentLine.toString());
                currentY -= leading;
                contentStream.newLine();
                currentLine = new StringBuilder(word);
            } else {
                // Si no excede, añadimos la palabra a la línea actual
                currentLine.append(" ").append(word);
            }
        }

        // Mostramos la última línea si queda algo
        if (currentLine.length() > 0) {
            contentStream.showText(currentLine.toString());
            currentY -= leading;
            contentStream.newLine();
        }

        return currentY;
    }
}