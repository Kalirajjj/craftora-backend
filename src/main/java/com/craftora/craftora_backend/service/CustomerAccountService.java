package com.craftora.craftora_backend.service;

import com.craftora.craftora_backend.model.CustomerAccount;
import com.craftora.craftora_backend.model.CustomerActionToken;
import com.craftora.craftora_backend.model.CustomerSession;
import com.craftora.craftora_backend.repository.CustomerAccountRepository;
import com.craftora.craftora_backend.repository.CustomerActionTokenRepository;
import com.craftora.craftora_backend.repository.CustomerSessionRepository;
import com.craftora.craftora_backend.repository.CustomQuoteRequestRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerAccountService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final CustomerAccountRepository accounts;
    private final CustomerSessionRepository sessions;
    private final CustomerActionTokenRepository actionTokens;
    private final CustomQuoteRequestRepository quotes;
    private final PasswordEncoder passwords;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String frontendUrl;
    private final String fromAddress;
    private final String smtpHost;

    public CustomerAccountService(CustomerAccountRepository accounts, CustomerSessionRepository sessions,
            CustomerActionTokenRepository actionTokens, CustomQuoteRequestRepository quotes, PasswordEncoder passwords,
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.accounts = accounts; this.sessions = sessions; this.actionTokens = actionTokens;
        this.quotes = quotes;
        this.passwords = passwords; this.mailSenders = mailSenders;
        this.frontendUrl = frontendUrl.replaceAll("/$", ""); this.fromAddress = fromAddress; this.smtpHost = smtpHost;
    }

    @Transactional
    public void register(String emailValue, String password, String fullName, String phone) {
        String email = normalizeEmail(emailValue);
        if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw badRequest("Enter a valid email address.");
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 120) throw badRequest("Enter your name (up to 120 characters).");
        if (password == null || password.length() < 12 || password.length() > 72) throw badRequest("Password must be between 12 and 72 characters.");
        if (phone != null && phone.length() > 40) throw badRequest("Phone number is too long.");
        if (accounts.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");

        CustomerAccount customer = accounts.save(new CustomerAccount(email, passwords.encode(password), fullName.trim(), clean(phone)));
        sendActionLink(customer, "VERIFY_EMAIL", "Verify your Craftora 3D account", "account/verify");
    }

    @Transactional
    public void verifyEmail(String token) {
        CustomerActionToken action = findActionToken(token, "VERIFY_EMAIL");
        CustomerAccount customer = action.getCustomer();
        customer.setVerified(true);
        quotes.findAllByEmailIgnoreCaseAndCustomerAccountIsNull(customer.getEmail()).forEach(quote -> quote.setCustomerAccount(customer));
        actionTokens.deleteByTokenHash(hash(token));
    }

    @Transactional
    public LoginResult login(String emailValue, String password) {
        CustomerAccount customer = accounts.findByEmailIgnoreCase(normalizeEmail(emailValue))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));
        if (!passwords.matches(password == null ? "" : password, customer.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        if (!customer.isVerified()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verify your email before signing in.");

        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new CustomerSession(customer, hash(token), LocalDateTime.now().plusDays(7)));
        return new LoginResult(token, customer);
    }

    @Transactional
    public void logout(String bearerToken) { if (bearerToken != null && !bearerToken.isBlank()) sessions.deleteByTokenHash(hash(bearerToken)); }

    @Transactional
    public void resendVerification(String emailValue) {
        accounts.findByEmailIgnoreCase(normalizeEmail(emailValue)).filter(account -> !account.isVerified())
                .ifPresent(account -> sendActionLink(account, "VERIFY_EMAIL", "Verify your Craftora 3D account", "account/verify"));
    }

    @Transactional
    public void startPasswordReset(String emailValue) {
        accounts.findByEmailIgnoreCase(normalizeEmail(emailValue)).ifPresent(account ->
                sendActionLink(account, "RESET_PASSWORD", "Reset your Craftora 3D password", "account/reset-password"));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 12 || newPassword.length() > 72) throw badRequest("Password must be between 12 and 72 characters.");
        CustomerActionToken action = findActionToken(token, "RESET_PASSWORD");
        CustomerAccount customer = action.getCustomer();
        customer.changePasswordHash(passwords.encode(newPassword));
        actionTokens.deleteByTokenHash(hash(token));
        sessions.deleteByCustomerId(customer.getId());
    }

    private CustomerActionToken findActionToken(String token, String purpose) {
        if (token == null || token.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This link is invalid or expired.");
        return actionTokens.findByTokenHashAndPurposeAndExpiresAtAfter(hash(token), purpose, LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This link is invalid or expired."));
    }

    private void sendActionLink(CustomerAccount customer, String purpose, String subject, String route) {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null || fromAddress.isBlank() || smtpHost.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Account email is not configured yet. Configure the transactional email settings and try again.");
        actionTokens.deleteByCustomerIdAndPurpose(customer.getId(), purpose);
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        actionTokens.save(new CustomerActionToken(customer, hash(token), purpose, LocalDateTime.now().plusHours(24)));
        String actionUrl = frontendUrl + "/" + route + "?token=" + token;
        boolean verification = "VERIFY_EMAIL".equals(purpose);
        String actionLabel = verification ? "Verify email address" : "Reset password";
        String heading = verification ? "Welcome to Craftora 3D" : "Reset your password";
        String intro = verification
                ? "Thanks for creating an account. Confirm your email address to get started."
                : "We received a request to reset the password for your Craftora 3D account.";
        String expiryNote = verification
                ? "This verification link expires in 24 hours."
                : "This password reset link expires in 24 hours.";
        String safeName = escapeHtml(customer.getFullName());
        String safeUrl = escapeHtml(actionUrl);
        String plainText = "Hello " + customer.getFullName() + ",\n\n" + intro + "\n\n" + actionLabel + ":\n" + actionUrl
                + "\n\n" + expiryNote + " If you did not request this, you can ignore this email.";
        String html = """
                <!doctype html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>%s</title></head>
                <body style="margin:0;padding:0;background:#f2f6f7;font-family:Arial,Helvetica,sans-serif;color:#172033;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f2f6f7;padding:36px 14px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #e2e8ee;border-radius:18px;overflow:hidden;">
                        <tr><td style="padding:25px 34px;border-bottom:1px solid #edf1f4;">
                          <div style="font-size:22px;font-weight:800;letter-spacing:-.5px;color:#172033;">Craftora <span style="color:#00a896;">3D</span></div>
                          <div style="margin-top:5px;font-size:12px;letter-spacing:1.3px;text-transform:uppercase;color:#718096;">Where Ideas Take Shape</div>
                        </td></tr>
                        <tr><td style="padding:34px;">
                          <div style="display:inline-block;padding:7px 11px;border-radius:999px;background:#e8f8f5;color:#008f80;font-size:11px;font-weight:700;letter-spacing:1px;text-transform:uppercase;">Craftora 3D account</div>
                          <h1 style="margin:20px 0 10px;font-size:27px;line-height:1.25;letter-spacing:-.5px;color:#172033;">%s</h1>
                          <p style="margin:0 0 12px;font-size:15px;line-height:1.7;color:#526174;">Hello %s,</p>
                          <p style="margin:0 0 25px;font-size:15px;line-height:1.7;color:#526174;">%s</p>
                          <table role="presentation" cellspacing="0" cellpadding="0"><tr><td style="border-radius:10px;background:#00a896;">
                            <a href="%s" style="display:inline-block;padding:14px 22px;color:#ffffff;text-decoration:none;font-size:15px;font-weight:700;">%s</a>
                          </td></tr></table>
                          <p style="margin:23px 0 0;font-size:13px;line-height:1.65;color:#718096;">%s</p>
                          <p style="margin:18px 0 0;font-size:12px;line-height:1.6;color:#8a96a5;">If the button doesn’t work, copy and paste this link into your browser:<br><a href="%s" style="color:#008f80;overflow-wrap:anywhere;">%s</a></p>
                          <p style="margin:22px 0 0;font-size:13px;line-height:1.6;color:#718096;">If you didn’t request this, you can safely ignore this email.</p>
                        </td></tr>
                        <tr><td style="padding:18px 34px;background:#f8fafb;border-top:1px solid #edf1f4;font-size:12px;line-height:1.6;color:#8a96a5;">Craftora 3D · Chennai, Tamil Nadu, India</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(subject), heading, safeName, intro, safeUrl, actionLabel, expiryNote, safeUrl, safeUrl);
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress); helper.setTo(customer.getEmail()); helper.setSubject(subject);
            helper.setText(plainText, html);
            sender.send(message);
        } catch (MessagingException error) {
            throw new IllegalStateException("Could not create the account email message.", error);
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String normalizeEmail(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception error) { throw new IllegalStateException("SHA-256 is unavailable", error); }
    }
    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    public record LoginResult(String token, CustomerAccount customer) {}
}
