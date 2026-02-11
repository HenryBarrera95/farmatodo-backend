package com.farmatodo.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService(mailSender);
    }

    @Test
    @DisplayName("sendPaymentFailed envía email con subject y texto correctos")
    void sendPaymentFailed() {
        service.sendPaymentFailed("a@a.com", "order-123", "Card declined");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("a@a.com");
        assertThat(msg.getSubject()).isEqualTo("Pago rechazado - Pedido order-123");
        assertThat(msg.getText()).contains("order-123").contains("Card declined");
    }

    @Test
    @DisplayName("sendPaymentSuccess envía email con total")
    void sendPaymentSuccess() {
        service.sendPaymentSuccess("b@b.com", "order-456", "150.00");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("b@b.com");
        assertThat(msg.getSubject()).isEqualTo("Pago exitoso - Pedido order-456");
        assertThat(msg.getText()).contains("150.00");
    }
}
