package com.farmatodo.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPaymentFailed(String toEmail, String orderId, String message) {
        try {
            var msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("Pago rechazado - Pedido " + orderId);
            msg.setText("Su pedido " + orderId + " no pudo ser procesado.\n\nMotivo: " + message + "\n\nPor favor intente nuevamente con otro método de pago.");
            mailSender.send(msg);
            log.info("Payment failure email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send payment failure email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendPaymentSuccess(String toEmail, String orderId, String totalAmount) {
        try {
            var msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("Pago exitoso - Pedido " + orderId);
            msg.setText("Su pedido " + orderId + " ha sido pagado exitosamente.\n\nTotal: " + totalAmount);
            mailSender.send(msg);
            log.info("Payment success email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send payment success email to {}: {}", toEmail, e.getMessage());
        }
    }
}
