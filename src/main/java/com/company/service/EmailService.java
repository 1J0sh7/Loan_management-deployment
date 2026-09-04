package com.company.service;

import com.company.model.LoanApplication;
import com.company.model.Repayment;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final SendGrid sendGrid;
    private final String fromEmail;

    // Template IDs (hardcoded)
    private static final String TEMPLATE_WELCOME = "d-75a2fd64eab94866bb76bc28f1872edd";
    private static final String TEMPLATE_LOAN_CREATED = "d-6baf9c2677b149448fda1e886c3baaa9";
    private static final String TEMPLATE_LOAN_APPROVED = "d-50801dfa2535498b849e8060672a14e7";
    private static final String TEMPLATE_LOAN_REJECTED = "d-bcfe21e718a842e3a477e85db090bd5b";
    private static final String TEMPLATE_LOAN_DISBURSED = "d-335e389cd2e44066a865cc364fad8670";
    private static final String TEMPLATE_REPAYMENT = "d-1c0d7df118924c2aa98744b99ad77468";

    public EmailService(SendGrid sendGrid,
                        @Value("${sendgrid.from-email}") String fromEmail) {
        this.sendGrid = sendGrid;
        this.fromEmail = fromEmail;
    }

    // ===== TEMPLATE EMAILS =====

    public void sendWelcomeEmail(String to, String name) {
        log.info("Sending welcome email to: {}", to);
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(to));
        personalization.addDynamicTemplateData("name", name);
        sendDynamicEmail(TEMPLATE_WELCOME, personalization);
    }

    public void sendLoanCreationEmail(String to, LoanApplication loanApplication) {
        log.info("Sending loan creation email to: {}", to);
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(to));
        personalization.addDynamicTemplateData("name", getSafeName(loanApplication));
        personalization.addDynamicTemplateData("applicationId", getSafeApplicationId(loanApplication));
        personalization.addDynamicTemplateData("amount", getSafeAmount(loanApplication));
        personalization.addDynamicTemplateData("productName", getSafeProductName(loanApplication));
        sendDynamicEmail(TEMPLATE_LOAN_CREATED, personalization);
    }

    public void sendLoanApprovalEmail(String to, LoanApplication loanApplication) {
        log.info("Sending loan approval email to: {}", to);
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(to));
        personalization.addDynamicTemplateData("name", getSafeName(loanApplication));
        personalization.addDynamicTemplateData("applicationId", getSafeApplicationId(loanApplication));
        personalization.addDynamicTemplateData("amount", getSafeAmount(loanApplication));
        personalization.addDynamicTemplateData("productName", getSafeProductName(loanApplication));
        personalization.addDynamicTemplateData("interestRate", getSafeInterestRate(loanApplication));
        personalization.addDynamicTemplateData("termMonths", getSafeTermMonths(loanApplication));
        sendDynamicEmail(TEMPLATE_LOAN_APPROVED, personalization);
    }

    public void sendLoanRejectionEmail(String to, LoanApplication loanApplication, String reason) {
        log.info("Sending loan rejection email to: {}", to);
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(to));
        personalization.addDynamicTemplateData("name", getSafeName(loanApplication));
        personalization.addDynamicTemplateData("applicationId", getSafeApplicationId(loanApplication));
        personalization.addDynamicTemplateData("amount", getSafeAmount(loanApplication));
        personalization.addDynamicTemplateData("reason", reason != null ? reason : "No reason provided");
        sendDynamicEmail(TEMPLATE_LOAN_REJECTED, personalization);
    }

    // 5. Loan Disbursed (HTML — Loaded from file)
    public void sendLoanDisbursementEmail(String to, LoanApplication loanApplication) {
        log.info("Sending loan disbursement email to: {}", to);

        String subject = " Your Loan Has Been Disbursed!";

        // Get safe values
        String name = getSafeName(loanApplication);
        String applicationId = getSafeApplicationId(loanApplication);
        String amount = getSafeAmount(loanApplication);
        String remainingBalance = getSafeRemainingBalance(loanApplication);
        String termMonths = getSafeTermMonths(loanApplication);

        // Load HTML template from file and replace placeholders
        String htmlBody = loadHtmlTemplate("templates/loan-disbursed-email.html")
                .replace("{name}", name)
                .replace("{applicationId}", applicationId)
                .replace("{amount}", amount)
                .replace("{remainingBalance}", remainingBalance)
                .replace("{termMonths}", termMonths);

        sendHtmlEmail(to, subject, htmlBody);
    }

    public void sendRepaymentConfirmationEmail(String to, LoanApplication loanApplication, Repayment repayment) {
        log.info("Sending repayment confirmation email to: {}", to);
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(to));
        personalization.addDynamicTemplateData("name", getSafeName(loanApplication));
        personalization.addDynamicTemplateData("applicationId", getSafeApplicationId(loanApplication));
        personalization.addDynamicTemplateData("paymentAmount", repayment.getAmount().toString());
        personalization.addDynamicTemplateData("remainingBalance", getSafeRemainingBalance(loanApplication));
        personalization.addDynamicTemplateData("paymentDate", repayment.getPaidDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        sendDynamicEmail(TEMPLATE_REPAYMENT, personalization);
    }

    // ===== HTML EMAIL SENDER =====
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        Email from = new Email(fromEmail, "Loan Management System");
        Email recipient = new Email(to);

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.addContent(new Content("text/html", htmlBody));

        Personalization personalization = new Personalization();
        personalization.addTo(recipient);
        mail.addPersonalization(personalization);

        sendEmail(mail);
    }

    // ===== LOAD HTML TEMPLATE FROM FILE =====
    private String loadHtmlTemplate(String filePath) {
        try {
            // Try to read from the filesystem (Docker container)
            String[] pathsToTry = {
                    "/app/templates/loan-disbursed-email.html",      // Docker container
                    "src/main/resources/templates/loan-disbursed-email.html",  // Local dev
                    filePath
            };

            for (String path : pathsToTry) {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    log.info("Loading template from: {}", file.getAbsolutePath());
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                        return new String(fis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }

            // Fallback to classpath
            ClassPathResource resource = new ClassPathResource(filePath);
            if (resource.exists()) {
                log.info("Loading template from classpath: {}", resource.getURL());
                try (java.io.InputStream inputStream = resource.getInputStream()) {
                    return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }

            log.error("Template not found in any location");
            return getFallbackTemplate();
        } catch (Exception e) {
            log.error("Failed to load HTML template: {}", e.getMessage());
            return getFallbackTemplate();
        }
    }

    private String getFallbackTemplate() {
        return "Dear {name},\n\nYour loan has been disbursed!\n\n" +
                "Application ID: {applicationId}\n" +
                "Amount: ${amount}\n" +
                "Remaining Balance: ${remainingBalance}\n" +
                "Term: {termMonths} months\n\n" +
                "Best regards,\nOptimasys Solutions";
    }


    // ===== TEMPLATE EMAIL SENDER =====
    private void sendDynamicEmail(String templateId, Personalization personalization) {
        Mail mail = new Mail();
        mail.setFrom(new Email(fromEmail, "Loan Management System"));
        mail.setTemplateId(templateId);
        mail.addPersonalization(personalization);
        sendEmail(mail);
    }

    // ===== GENERIC EMAIL SENDER (With Retry) =====
    @Retryable(
            value = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    private void sendEmail(Mail mail) {
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);

            log.info("Email sent. Status code: {}", response.getStatusCode());
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent successfully");
            } else {
                log.error("SendGrid returned error: {}", response.getBody());
                throw new RuntimeException("SendGrid returned error: " + response.getStatusCode());
            }
        } catch (IOException ex) {
            log.error("Failed to send email: {}", ex.getMessage());
            throw new RuntimeException("Failed to send email", ex);
        }
    }

    // ===== HELPER METHODS (Null-Safe) =====

    private String getSafeName(LoanApplication application) {
        return application.getCustomer() != null ? application.getCustomer().getName() : "Customer";
    }

    private String getSafeApplicationId(LoanApplication application) {
        return application.getId() != null ? application.getId().toString() : "N/A";
    }

    private String getSafeAmount(LoanApplication application) {
        return application.getAmount() != null ? application.getAmount().toString() : "0.00";
    }

    private String getSafeProductName(LoanApplication application) {
        return application.getProduct() != null ? application.getProduct().getName() : "Loan Product";
    }

    private String getSafeInterestRate(LoanApplication application) {
        if (application.getProduct() != null && application.getProduct().getInterestRate() != null) {
            return application.getProduct().getInterestRate().toString();
        }
        return "0.0";
    }

    private String getSafeTermMonths(LoanApplication application) {
        if (application.getProduct() != null && application.getProduct().getTermMonths() != null) {
            return application.getProduct().getTermMonths().toString();
        }
        return "0";
    }

    private String getSafeRemainingBalance(LoanApplication application) {
        return application.getRemainingBalance() != null
                ? application.getRemainingBalance().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                : "0.00";
    }
}