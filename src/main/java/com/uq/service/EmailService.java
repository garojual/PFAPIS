package com.uq.service;

import jakarta.enterprise.context.ApplicationScoped;

// Importaciones de Jakarta Mail
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

// Importaciones de MicroProfile Config
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    // --- Inyección de propiedades desde application.properties ---
    @ConfigProperty(name = "mail.smtp.host")
    String smtpHost;

    @ConfigProperty(name = "mail.smtp.port")
    int smtpPort;

    @ConfigProperty(name = "mail.smtp.auth", defaultValue = "false") // Por defecto deshabilitado si no se especifica
    boolean smtpAuth;

    @ConfigProperty(name = "mail.smtp.starttls.enable", defaultValue = "false") // Por defecto deshabilitado
    boolean smtpStartTlsEnable;

    @ConfigProperty(name = "mail.smtp.ssl.enable", defaultValue = "false") // Por defecto deshabilitado
    boolean smtpSslEnable;

    @ConfigProperty(name = "mail.smtp.username")
    String smtpUsername;

    @ConfigProperty(name = "mail.smtp.password")
    String smtpPassword;

    // Propiedad opcional para la dirección "From". Si no se especifica, usamos el username.
    @ConfigProperty(name = "mail.from", defaultValue = "${mail.smtp.username}")
    String mailFrom;
    // --- Fin Inyección de propiedades ---


    public void sendVerificationEmail(String recipientEmail, String verificationCode) {
        // --- Configuración de la Sesión de Mail usando las propiedades inyectadas ---
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort)); // El puerto a veces necesita ser String en Properties
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTlsEnable));
        props.put("mail.smtp.ssl.enable", String.valueOf(smtpSslEnable));

        // Obtener la sesión. Si smtpAuth es true, proporcionamos un Authenticator.
        Session session;
        if (smtpAuth) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    // Usamos las propiedades inyectadas en la clase exterior
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });
        } else {
            session = Session.getInstance(props);
        }


        try {
            // --- Creación del Mensaje ---
            MimeMessage message = new MimeMessage(session);

            // Remitente (Usamos la propiedad inyectada, que por defecto es el username)
            message.setFrom(new InternetAddress(mailFrom));

            // Destinatario
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));

            // Asunto
            message.setSubject("Verifica tu cuenta en la Plataforma de Programación UQ");

            // Cuerpo del mensaje (texto plano)
            String textBody = "Hola,\n\n" +
                    "Gracias por registrarte en nuestra plataforma. Para activar tu cuenta, usa el siguiente código de verificación:\n\n" +
                    verificationCode + "\n\n" +
                    "Este código expirará en 15 minutos.\n\n" +
                    "Si no te registraste en nuestra plataforma, por favor ignora este correo.\n\n" +
                    "Atentamente,\n" +
                    "El Equipo de la Plataforma de Programación UQ";
            message.setText(textBody);

            // --- Envío del Mensaje ---
            Transport.send(message);

            LOGGER.log(Level.INFO, "Correo de verificación enviado a: {0}", recipientEmail);

        } catch (MessagingException e) {
            // Jakarta Mail usa MessagingException para errores
            LOGGER.log(Level.SEVERE, "Error al enviar el correo de verificación a " + recipientEmail + " usando Jakarta Mail.", e);
            // La excepción MessagingException podría contener detalles sobre el error SMTP (autenticación, etc.)
            // Puedes examinar e.getCause() o e.getNextException() para más detalles si están disponibles.
        } catch (Exception e) {
            // Otros errores inesperados
            LOGGER.log(Level.SEVERE, "Error inesperado al preparar/enviar correo con Jakarta Mail a " + recipientEmail, e);
        }
    }

}