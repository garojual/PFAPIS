package com.uq.service;

import com.uq.dto.ProgramaDTO;
import com.uq.dto.ProgramaExecutionResultDTO;
import com.uq.exception.ProgramExecutionException;
import com.uq.exception.ProgramNotFoundException;
import com.uq.exception.UnauthorizedException;
import com.uq.exception.UserNotFoundException;
import com.uq.mapper.ProgramaMapper;
import com.uq.model.Estudiante;
import com.uq.model.Programa;
import com.uq.repository.EstudianteRepository;
import com.uq.repository.ProgramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


@ApplicationScoped
public class ProgramaService {

    private static final Logger LOGGER = Logger.getLogger(ProgramaService.class.getName()); // Logger para el servicio

    @Inject
    ProgramaRepository programaRepository;

    @Inject
    EstudianteRepository estudianteRepository;

    @Inject
    ProgramaMapper programaMapper;

    private static final int VERIFICATION_CODE_VALIDITY_MINUTES = 15;
    private static final int EXECUTION_TIMEOUT_SECONDS = 10; // Tiempo máximo de ejecución para evitar loops infinitos
    private static final String TEMP_DIR_PREFIX = "java_exec_"; // Prefijo para directorios temporales

    @Transactional
    public ProgramaDTO createProgram(Long estudianteId, ProgramaDTO programaDTO)
            throws UserNotFoundException {

        Estudiante estudiante = estudianteRepository.findById(estudianteId);
        if (estudiante == null) {
            throw new UserNotFoundException("Estudiante no encontrado con ID: " + estudianteId);
        }

        Programa programa = programaMapper.toEntity(programaDTO);
        programa.setEstudiante(estudiante);
        programa.setResuelto(false);

        programaRepository.persist(programa);
        LOGGER.log(Level.INFO, "Programa creado con ID {0} para estudiante {1}", new Object[]{programa.getId(), estudianteId});

        return programaMapper.toDTO(programa);
    }

    public List<ProgramaDTO> getProgramsByEstudianteId(Long estudianteId)
            throws UserNotFoundException {

        Estudiante estudiante = estudianteRepository.findById(estudianteId);
        if (estudiante == null) {
            throw new UserNotFoundException("Estudiante no encontrado con ID: " + estudianteId);
        }

        List<Programa> programas = programaRepository.findByEstudianteId(estudianteId);
        LOGGER.log(Level.INFO, "Obtenidos {0} programas para estudiante {1}", new Object[]{programas.size(), estudianteId});

        return programaMapper.toDTOList(programas);
    }

    // Este método ahora verifica la propiedad del programa
    public ProgramaDTO getProgramById(Long programaId, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa programa = programaRepository.findById(programaId);

        if (programa == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar si el usuario autenticado es el dueño del programa
        // Implementar lógica para permitir a profesores ver cualquier programa si es necesario
        if (programa.getEstudiante() == null || !programa.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de acceso no autorizado al programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para ver este programa.");
        }
        LOGGER.log(Level.INFO, "Acceso autorizado al programa {0} para estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});


        return programaMapper.toDTO(programa);
    }

    // Metodo para actualizar un programa completo
    @Transactional
    public ProgramaDTO updateProgram(Long programaId, ProgramaDTO updatedProgramaDTO, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar propiedad
        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de actualización no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para actualizar este programa.");
        }

        // Usar el mapper para actualizar la entidad existente desde el DTO
        programaMapper.updateEntityFromDto(updatedProgramaDTO, existingPrograma);

        LOGGER.log(Level.INFO, "Programa actualizado con ID {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
        return programaMapper.toDTO(existingPrograma);
    }

    // Metodo para actualizar un programa parcialmente
    @Transactional
    public ProgramaDTO partialUpdateProgram(Long programaId, ProgramaDTO partialProgramaDTO, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar propiedad
        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de actualización parcial no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para actualizar este programa.");
        }

        // Actualizar solo los campos no nulos del DTO
        if (partialProgramaDTO.getTitulo() != null) existingPrograma.setTitulo(partialProgramaDTO.getTitulo());
        if (partialProgramaDTO.getDescripcion() != null) existingPrograma.setDescripcion(partialProgramaDTO.getDescripcion());
        if (partialProgramaDTO.getCodigoFuente() != null) existingPrograma.setCodigoFuente(partialProgramaDTO.getCodigoFuente());
        //if (partialProgramaDTO.IsResuelto() != null) existingPrograma.setResuelto(partialProgramaDTO.IsResuelto());
        existingPrograma.setResuelto(partialProgramaDTO.isResuelto());

        LOGGER.log(Level.INFO, "Programa parcialmente actualizado con ID {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
        return programaMapper.toDTO(existingPrograma); // Mapear la entidad actualizada a DTO
    }


    // Metodo para eliminar un programa
    @Transactional
    public void deleteProgram(Long programaId, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar propiedad
        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de eliminación no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para eliminar este programa.");
        }

        // Eliminar el programa. Panache.delete(entity) retorna void si no encuentra, boolean si usa deleteById.
        // Con findById previo, es seguro usar delete(entity).
        programaRepository.delete(existingPrograma);
        LOGGER.log(Level.INFO, "Programa eliminado con ID {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});

        // No retorna nada si la eliminación fue exitosa y autorizada.
    }

    @Transactional
    public ProgramaDTO updateSharingStatus(Long programaId, boolean sharedStatus, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar propiedad
        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de modificar estado de compartir no autorizado del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para modificar el estado de compartir de este programa.");
        }

        // Actualizar el estado de compartir
        existingPrograma.setShared(sharedStatus);

        LOGGER.log(Level.INFO, "Estado de compartir del programa {0} actualizado a {1} por estudiante {2}", new Object[]{programaId, sharedStatus, authenticatedEstudianteId});
        return programaMapper.toDTO(existingPrograma); // Devolver el programa actualizado
    }

    public ProgramaExecutionResultDTO executeProgram(Long programaId, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException, ProgramExecutionException {

        Programa programa = programaRepository.findById(programaId);

        if (programa == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Solo el dueño puede ejecutar su programa
        if (programa.getEstudiante() == null || !programa.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de ejecución no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para ejecutar este programa.");
        }
        LOGGER.log(Level.INFO, "Ejecución autorizada del programa {0} para estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});


        String codigoFuente = programa.getCodigoFuente();
        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            throw new ProgramExecutionException("El código fuente del programa está vacío.");
        }

        // --- Proceso de Compilación y Ejecución ---
        Path tempDir = null;
        ProgramaExecutionResultDTO result = new ProgramaExecutionResultDTO();
        long startTime = System.currentTimeMillis();

        try {
            // 1. Crear un directorio temporal único
            tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX + UUID.randomUUID().toString().substring(0, 8) + "_");
            File sourceFile = tempDir.resolve("Main.java").toFile();

            // 2. Escribir el código fuente al archivo temporal
            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(codigoFuente);
            }

            // 3. Compilar el código Java
            ProcessBuilder compilePb = new ProcessBuilder("javac", sourceFile.getName());
            compilePb.directory(tempDir.toFile()); // Ejecutar en el directorio temporal
            Process compileProcess = compilePb.start();

            // Capturar salida de error de la compilación
            String compileStderr = readProcessStream(compileProcess.getErrorStream());
            LOGGER.log(Level.FINE, "Compilación STDERR:\n{0}", compileStderr);


            // Esperar a que termine la compilación
            boolean compilationCompleted = compileProcess.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!compilationCompleted) {
                compileProcess.destroyForcibly(); // Terminar el proceso si se excedió el tiempo
                throw new ProgramExecutionException("La compilación excedió el tiempo límite de " + EXECUTION_TIMEOUT_SECONDS + " segundos.");
            }

            int compileExitCode = compileProcess.exitValue();
            LOGGER.log(Level.INFO, "Compilación finalizada con código de salida: {0}", compileExitCode);

            if (compileExitCode != 0) {
                // Error de compilación
                result.setExitCode(compileExitCode); // Usamos el código de salida de javac
                result.setStderr(compileStderr); // Incluimos los errores del compilador
                result.setErrorMessage("Error de compilación: " + compileStderr);
                throw new ProgramExecutionException("Error de compilación.", compileStderr);
            }

            // 4. Ejecutar el código compilado (asumiendo que el archivo .class se llama Main.class)
            ProcessBuilder runPb = new ProcessBuilder("java", "Main");
            runPb.directory(tempDir.toFile()); // Ejecutar en el directorio temporal
            Process runProcess = runPb.start();

            // Capturar salida estándar y de error de la ejecución (usando hilos para evitar deadlocks)
            StreamGobbler stdoutGobbler = new StreamGobbler(runProcess.getInputStream());
            StreamGobbler stderrGobbler = new StreamGobbler(runProcess.getErrorStream());
            new Thread(stdoutGobbler).start();
            new Thread(stderrGobbler).start();

            // Esperar a que termine la ejecución
            boolean executionCompleted = runProcess.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!executionCompleted) {
                runProcess.destroyForcibly(); // Terminar el proceso
                result.setStderr("La ejecución excedió el tiempo límite de " + EXECUTION_TIMEOUT_SECONDS + " segundos.");
                result.setExitCode(-1); // Código de salida indicando timeout o error
                result.setErrorMessage("Tiempo de ejecución excedido.");
                // Decidimos lanzar excepción para indicar que la ejecución no pudo completarse
                throw new ProgramExecutionException("Tiempo de ejecución excedido.");
            }

            int runExitCode = runProcess.exitValue();
            LOGGER.log(Level.INFO, "Ejecución finalizada con código de salida: {0}", runExitCode);


            // Obtener las salidas capturadas por los hilos
            result.setStdout(stdoutGobbler.getOutput());
            result.setStderr(stderrGobbler.getOutput());
            result.setExitCode(runExitCode);
            result.setErrorMessage(null); // Si llegó aquí, no hubo error de plataforma

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error de I/O durante la ejecución del programa.", e);
            throw new ProgramExecutionException("Error interno al ejecutar el programa: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Hilo interrumpido durante la espera del proceso.", e);
            Thread.currentThread().interrupt();
            throw new ProgramExecutionException("La ejecución del programa fue interrumpida.", e);
        } finally {
            // 5. Limpiar los archivos temporales y el directorio
            if (tempDir != null && Files.exists(tempDir)) {
                try (Stream<Path> walk = Files.walk(tempDir)) {
                    walk.sorted(Comparator.reverseOrder()) // Borra archivos antes que directorios
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "No se pudo limpiar el directorio temporal: " + tempDir, e);
                }
            }
        }

        // Calcular duración
        long endTime = System.currentTimeMillis();
        result.setDurationMillis(endTime - startTime);

        return result;
    }

    // Clase auxiliar para leer streams de proceso en un hilo separado
    private static class StreamGobbler implements Runnable {
        private final InputStream is;
        private final StringBuilder output = new StringBuilder();

        StreamGobbler(InputStream is) {
            this.is = is;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error leyendo stream de proceso.", e);
            }
        }

        public String getOutput() {
            return output.toString();
        }
    }

    private String readProcessStream(InputStream is) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        return output.toString();
    }

}