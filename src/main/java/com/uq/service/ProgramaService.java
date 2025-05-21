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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


@ApplicationScoped
public class ProgramaService {

    private static final Logger LOGGER = Logger.getLogger(ProgramaService.class.getName());

    @Inject
    ProgramaRepository programaRepository;

    @Inject
    EstudianteRepository estudianteRepository;

    @Inject
    ProgramaMapper programaMapper;

    private static final int VERIFICATION_CODE_VALIDITY_MINUTES = 15;
    private static final int EXECUTION_TIMEOUT_SECONDS = 10;
    private static final String TEMP_DIR_PREFIX = "java_exec_";


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
        programa.setShared(false);


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

    // Este metodo ahora verifica la propiedad del programa (solo el dueño puede verlo por este endpoint)
    public ProgramaDTO getProgramById(Long programaId, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa programa = programaRepository.findById(programaId);

        if (programa == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar si el usuario autenticado es el dueño del programa
        if (programa.getEstudiante() == null || !programa.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de acceso no autorizado al programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para ver este programa.");
        }
        LOGGER.log(Level.INFO, "Acceso autorizado al programa {0} para estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});


        return programaMapper.toDTO(programa);
    }

    @Transactional
    public ProgramaDTO updateProgram(Long programaId, ProgramaDTO updatedProgramaDTO, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de actualización no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para actualizar este programa.");
        }

        programaMapper.updateEntityFromDto(updatedProgramaDTO, existingPrograma);

        LOGGER.log(Level.INFO, "Programa actualizado (completo) con ID {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
        return programaMapper.toDTO(existingPrograma);
    }

    @Transactional
    public ProgramaDTO partialUpdateProgram(Long programaId, ProgramaDTO partialProgramaDTO, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de actualización parcial no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para actualizar este programa.");
        }

        if (partialProgramaDTO.getTitulo() != null) existingPrograma.setTitulo(partialProgramaDTO.getTitulo());
        if (partialProgramaDTO.getDescripcion() != null) existingPrograma.setDescripcion(partialProgramaDTO.getDescripcion());
        if (partialProgramaDTO.getCodigoFuente() != null) existingPrograma.setCodigoFuente(partialProgramaDTO.getCodigoFuente());

        LOGGER.log(Level.INFO, "Programa parcialmente actualizado con ID {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
        return programaMapper.toDTO(existingPrograma);
    }


    @Transactional
    public void deleteProgram(Long programaId, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de eliminación no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para eliminar este programa.");
        }

        programaRepository.delete(existingPrograma);
        LOGGER.log(Level.INFO, "Programa eliminado con ID {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
    }

    @Transactional
    public ProgramaDTO updateSharingStatus(Long programaId, boolean sharedStatus, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de modificar estado de compartir no autorizado del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para modificar el estado de compartir de este programa.");
        }

        existingPrograma.setShared(sharedStatus);

        LOGGER.log(Level.INFO, "Estado de compartir del programa {0} actualizado a {1} por estudiante {2}", new Object[]{programaId, sharedStatus, authenticatedEstudianteId});
        return programaMapper.toDTO(existingPrograma);
    }

    // Metodo para ejecutar un programa
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
            tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX + UUID.randomUUID().toString().substring(0, 8) + "_");
            File sourceFile = tempDir.resolve("Main.java").toFile();

            try (FileWriter writer = new FileWriter(sourceFile)) {
                writer.write(codigoFuente);
            }

            ProcessBuilder compilePb = new ProcessBuilder("javac", sourceFile.getName());
            compilePb.directory(tempDir.toFile());
            Process compileProcess = compilePb.start();

            String compileStderr = readProcessStream(compileProcess.getErrorStream());
            LOGGER.log(Level.FINE, "Compilación STDERR:\n{0}", compileStderr);

            boolean compilationCompleted = compileProcess.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!compilationCompleted) {
                compileProcess.destroyForcibly();
                throw new ProgramExecutionException("La compilación excedió el tiempo límite de " + EXECUTION_TIMEOUT_SECONDS + " segundos.");
            }

            int compileExitCode = compileProcess.exitValue();
            LOGGER.log(Level.INFO, "Compilación finalizada con código de salida: {0}", compileExitCode);

            if (compileExitCode != 0) {
                result.setExitCode(compileExitCode);
                result.setStderr(compileStderr);
                result.setErrorMessage("Error de compilación."); // Mensaje genérico para el usuario
                throw new ProgramExecutionException("Error de compilación.", compileStderr);
            }

            ProcessBuilder runPb = new ProcessBuilder("java", "Main");
            runPb.directory(tempDir.toFile());
            Process runProcess = runPb.start();

            StreamGobbler stdoutGobbler = new StreamGobbler(runProcess.getInputStream());
            StreamGobbler stderrGobbler = new StreamGobbler(runProcess.getErrorStream());
            new Thread(stdoutGobbler).start();
            new Thread(stderrGobbler).start();

            boolean executionCompleted = runProcess.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!executionCompleted) {
                runProcess.destroyForcibly();
                result.setStderr("La ejecución excedió el tiempo límite de " + EXECUTION_TIMEOUT_SECONDS + " segundos.");
                result.setExitCode(-1);
                result.setErrorMessage("Tiempo de ejecución excedido.");
                throw new ProgramExecutionException("Tiempo de ejecución excedido.");
            }

            int runExitCode = runProcess.exitValue();
            LOGGER.log(Level.INFO, "Ejecución finalizada con código de salida: {0}", runExitCode);


            result.setStdout(stdoutGobbler.getOutput());
            result.setStderr(stderrGobbler.getOutput());
            result.setExitCode(runExitCode);
            result.setErrorMessage(null);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error de I/O durante la ejecución del programa.", e);
            throw new ProgramExecutionException("Error interno al ejecutar el programa: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Hilo interrumpido durante la espera del proceso.", e);
            Thread.currentThread().interrupt();
            throw new ProgramExecutionException("La ejecución del programa fue interrumpida.", e);
        } finally {
            if (tempDir != null && Files.exists(tempDir)) {
                try (Stream<Path> walk = Files.walk(tempDir)) {
                    walk.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "No se pudo limpiar el directorio temporal: " + tempDir, e);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        result.setDurationMillis(endTime - startTime);

        return result;
    }

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

    // Metodo para marcar un programa como resuelto o no resuelto
    @Transactional
    public ProgramaDTO markProgramAsResolved(Long programaId, boolean resueltoStatus, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        // 1. Buscar el programa por ID
        Programa existingPrograma = programaRepository.findById(programaId);

        // 2. Verificar si el programa existe
        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // 3. Lógica de Autorización: Verificar si el usuario autenticado es el dueño del programa
        // Solo el dueño puede marcar su programa como resuelto.
        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de modificar estado 'resuelto' no autorizado del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para cambiar el estado 'resuelto' de este programa.");
        }

        // 4. Actualizar el estado 'resuelto'
        existingPrograma.setResuelto(resueltoStatus);

        LOGGER.log(Level.INFO, "Estado 'resuelto' del programa {0} actualizado a {1} por estudiante {2}", new Object[]{programaId, resueltoStatus, authenticatedEstudianteId});

        // 5. Mapear la entidad actualizada a DTO y retornar
        return programaMapper.toDTO(existingPrograma);
    }
}