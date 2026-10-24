package br.com.denarco.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envia e-mails pelo servidor SMTP configurado (variável SPRING_MAIL_HOST).
 * Sem SMTP, em modo demonstração o e-mail é mostrado no console para a equipe testar.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean modoDemo;

    public EmailService(ObjectProvider<JavaMailSender> mailSender, @Value("${app.modo-demo}") boolean modoDemo) {
        this.mailSender = mailSender;
        this.modoDemo = modoDemo;
    }

    public void enviar(String para, String assunto, String texto) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            if (modoDemo) {
                log.info("\n--- E-mail (modo demonstração) ---\nPara: {}\nAssunto: {}\n{}\n----------------------------------", para, assunto, texto);
            } else {
                log.warn("SMTP não configurado: e-mail para {} não foi enviado.", para);
            }
            return;
        }

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(para);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        try {
            sender.send(mensagem);
        } catch (MailException e) {
            // Uma falha no e-mail não deve derrubar a operação que o usuário fez.
            log.error("Falha ao enviar e-mail para {}: {}", para, e.getMessage());
        }
    }
}
