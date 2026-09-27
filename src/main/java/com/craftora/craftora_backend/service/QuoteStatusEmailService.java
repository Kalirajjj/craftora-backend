package com.craftora.craftora_backend.service;

import com.craftora.craftora_backend.model.CustomQuoteRequest;
import com.craftora.craftora_backend.model.QuoteStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class QuoteStatusEmailService {
    private static final Logger log = LoggerFactory.getLogger(QuoteStatusEmailService.class);
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String frontendUrl;
    private final String fromAddress;
    private final String smtpHost;

    public QuoteStatusEmailService(ObjectProvider<JavaMailSender> mailSenders,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSenders = mailSenders;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
        this.fromAddress = fromAddress;
        this.smtpHost = smtpHost;
    }

    public void sendStatusChanged(CustomQuoteRequest request, QuoteStatus previous, QuoteStatus current) {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null || fromAddress.isBlank() || smtpHost.isBlank()) {
            log.warn("Quote status email was not sent for {} because mail is not configured.", request.getReferenceCode());
            return;
        }

        String oldLabel = label(previous);
        String newLabel = label(current);
        String trackUrl = frontendUrl + "/track-quote";
        String subject = "Update on your Craftora 3D quote " + request.getReferenceCode();
        String plainText = "Hello " + request.getName() + ",\n\nThe status of your custom 3D printing quote "
                + request.getReferenceCode() + " has changed from " + oldLabel + " to " + newLabel + ".\n\n"
                + "Project: " + request.getProductType() + "\n\nSign in to track your request: " + trackUrl
                + "\n\nThank you,\nCraftora 3D";
        String html = """
                <!doctype html>
                <html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Quote status update</title></head>
                <body style="margin:0;padding:0;background:#f2f6f7;font-family:Arial,Helvetica,sans-serif;color:#172033;">
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#f2f6f7;padding:36px 14px;"><tr><td align="center">
                    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#fff;border:1px solid #e2e8ee;border-radius:18px;overflow:hidden;">
                      <tr><td style="padding:25px 34px;border-bottom:1px solid #edf1f4;">
                        <div style="font-size:22px;font-weight:800;letter-spacing:-.5px;color:#172033;">Craftora <span style="color:#00a896;">3D</span></div>
                        <div style="margin-top:5px;font-size:12px;letter-spacing:1.3px;text-transform:uppercase;color:#718096;">Where Ideas Take Shape</div>
                      </td></tr>
                      <tr><td style="padding:34px;">
                        <div style="display:inline-block;padding:7px 11px;border-radius:999px;background:#e8f8f5;color:#008f80;font-size:11px;font-weight:700;letter-spacing:1px;text-transform:uppercase;">Custom 3D printing</div>
                        <h1 style="margin:20px 0 10px;font-size:26px;line-height:1.25;letter-spacing:-.5px;color:#172033;">Your quote status has been updated</h1>
                        <p style="margin:0 0 20px;font-size:15px;line-height:1.7;color:#526174;">Hello {name}, we’ve moved your request to a new stage.</p>
                        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin:0 0 24px;background:#f7fafb;border:1px solid #e5ecef;border-radius:12px;">
                          <tr><td style="padding:17px 18px;font-size:12px;letter-spacing:.7px;text-transform:uppercase;color:#718096;">Quote reference</td></tr>
                          <tr><td style="padding:0 18px 16px;font-size:17px;font-weight:700;color:#008f80;">{reference}</td></tr>
                          <tr><td style="padding:0 18px 16px;font-size:14px;color:#526174;">{project}</td></tr>
                          <tr><td style="padding:14px 18px;border-top:1px solid #e5ecef;font-size:14px;line-height:1.7;color:#526174;"><strong>{oldStatus}</strong> <span style="color:#8a96a5;">&nbsp;→&nbsp;</span> <strong style="color:#008f80;">{newStatus}</strong></td></tr>
                        </table>
                        <p style="margin:0 0 22px;font-size:14px;line-height:1.65;color:#526174;">You can sign in to view the latest details and track your request.</p>
                        <table role="presentation" cellspacing="0" cellpadding="0"><tr><td style="border-radius:10px;background:#00a896;"><a href="{trackUrl}" style="display:inline-block;padding:14px 22px;color:#fff;text-decoration:none;font-size:15px;font-weight:700;">Track your quote</a></td></tr></table>
                        <p style="margin:24px 0 0;font-size:13px;line-height:1.65;color:#718096;">If you have questions, reply to this email and our team will be happy to help.</p>
                      </td></tr>
                      <tr><td style="padding:18px 34px;background:#f8fafb;border-top:1px solid #edf1f4;font-size:12px;line-height:1.6;color:#8a96a5;">Craftora 3D · Chennai, Tamil Nadu, India</td></tr>
                    </table>
                  </td></tr></table>
                </body></html>
                """.replace("{name}", escapeHtml(request.getName()))
                .replace("{reference}", escapeHtml(request.getReferenceCode()))
                .replace("{project}", escapeHtml(request.getProductType()))
                .replace("{oldStatus}", escapeHtml(oldLabel))
                .replace("{newStatus}", escapeHtml(newLabel))
                .replace("{trackUrl}", escapeHtml(trackUrl));

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(request.getEmail());
            helper.setSubject(subject);
            helper.setText(plainText, html);
            sender.send(message);
        } catch (MessagingException | RuntimeException error) {
            log.warn("Could not send quote status email for {} to {}.", request.getReferenceCode(), request.getEmail(), error);
        }
    }

    private static String label(QuoteStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case IN_PROGRESS -> "In progress";
            case COMPLETED -> "Completed";
        };
    }

    private static String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
