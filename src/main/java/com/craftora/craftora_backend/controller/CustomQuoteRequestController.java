package com.craftora.craftora_backend.controller;

import com.craftora.craftora_backend.model.CustomQuoteRequest;
import com.craftora.craftora_backend.model.QuoteStatus;
import com.craftora.craftora_backend.model.CustomerAccount;
import com.craftora.craftora_backend.repository.CustomQuoteRequestRepository;
import com.craftora.craftora_backend.service.QuoteAttachmentStorageService;
import com.craftora.craftora_backend.service.QuoteStatusEmailService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class CustomQuoteRequestController {
    private final CustomQuoteRequestRepository requests;
    private final QuoteAttachmentStorageService attachmentStorage;
    private final QuoteStatusEmailService statusEmail;

    public CustomQuoteRequestController(CustomQuoteRequestRepository requests, QuoteAttachmentStorageService attachmentStorage,
            QuoteStatusEmailService statusEmail) {
        this.requests = requests;
        this.attachmentStorage = attachmentStorage;
        this.statusEmail = statusEmail;
    }

    @PostMapping(value = "/custom-requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuoteRequestResponse> create(
            @RequestPart("request") QuoteRequestPayload payload,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            Authentication authentication) throws IOException {
        CustomerAccount customer = authentication != null && authentication.getPrincipal() instanceof CustomerAccount account ? account : null;
        if (customer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in before requesting a quote.");
        if (payload.description() == null || payload.description().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project description is required.");
        }
        if (payload.quantity() < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least one.");

        String contactPhone = customer.getPhone();
        if (contactPhone == null || contactPhone.isBlank()) contactPhone = payload.phone();
        if (contactPhone == null || contactPhone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add a phone number to your account or this request.");
        }

        CustomQuoteRequest request = new CustomQuoteRequest();
        request.setName(customer.getFullName().trim());
        request.setEmail(customer.getEmail());
        request.setCustomerAccount(customer);
        request.setPhone(contactPhone.trim());
        request.setProductType(valueOrDefault(payload.productType(), "Other"));
        request.setDescription(payload.description().trim());
        request.setPreferredColor(payload.preferredColor());
        request.setQuantity(payload.quantity());
        request.setRequiredDate(parseDate(payload.requiredDate()));
        request.setAdditionalNotes(payload.additionalNotes());
        QuoteAttachmentStorageService.StoredAttachment stored = attachmentStorage.store(attachment);
        if (stored != null) {
            request.setAttachmentName(stored.originalName());
            request.setAttachmentStorageName(stored.storageName());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(requests.save(request)));
    }

    @GetMapping("/admin/custom-requests")
    public List<QuoteRequestResponse> list(@RequestParam(defaultValue = "false") boolean deleted) {
        List<CustomQuoteRequest> results = deleted
                ? requests.findAllByDeletedTrueOrderByDeletedAtDesc()
                : requests.findAllByDeletedFalseOrderByCreatedAtDesc();
        return results.stream().map(this::toResponse).toList();
    }

    @PatchMapping("/admin/custom-requests/{id}/delete")
    public QuoteRequestResponse delete(@PathVariable Long id) {
        CustomQuoteRequest request = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote request not found."));
        request.setDeleted(true);
        request.setDeletedAt(LocalDateTime.now());
        return toResponse(requests.save(request));
    }

    @PatchMapping("/admin/custom-requests/{id}/restore")
    public QuoteRequestResponse restore(@PathVariable Long id) {
        CustomQuoteRequest request = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote request not found."));
        request.setDeleted(false);
        request.setDeletedAt(null);
        return toResponse(requests.save(request));
    }

    @PatchMapping("/admin/custom-requests/{id}/status")
    public QuoteRequestResponse updateStatus(@PathVariable Long id, @RequestBody StatusUpdate update) {
        CustomQuoteRequest request = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote request not found."));
        QuoteStatus previousStatus = request.getStatus();
        try {
            request.setStatus(QuoteStatus.valueOf(update.status().trim().toUpperCase(Locale.ROOT)));
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be PENDING, IN_PROGRESS, or COMPLETED.");
        }
        CustomQuoteRequest saved = requests.save(request);
        if (!saved.isDeleted() && previousStatus != saved.getStatus()) {
            statusEmail.sendStatusChanged(saved, previousStatus, saved.getStatus());
        }
        return toResponse(saved);
    }

    @GetMapping("/admin/custom-requests/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        CustomQuoteRequest request = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote request not found."));
        if (request.getAttachmentStorageName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This request has no attachment.");
        }
        Resource resource = new FileSystemResource(attachmentStorage.resolve(request.getAttachmentStorageName()));
        if (!resource.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file was not found.");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(request.getAttachmentName()).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private QuoteRequestResponse toResponse(CustomQuoteRequest request) {
        String attachmentUrl = request.getAttachmentStorageName() == null ? null
                : "/api/admin/custom-requests/" + request.getId() + "/attachment";
        return new QuoteRequestResponse(request.getId(), request.getReferenceCode(), request.getName(), request.getEmail(),
                request.getPhone(), request.getProductType(), request.getDescription(), request.getPreferredColor(),
                request.getQuantity(), request.getRequiredDate(), request.getAdditionalNotes(), request.getAttachmentName(),
                attachmentUrl, request.getStatus(), request.getCreatedAt(), request.isDeleted(), request.getDeletedAt());
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        LocalDate requiredDate;
        try {
            requiredDate = LocalDate.parse(date);
        }
        catch (RuntimeException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required delivery date is invalid."); }
        if (requiredDate.isBefore(LocalDate.now().plusDays(3))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required delivery date must be at least 3 days from today.");
        }
        return requiredDate;
    }

    public record QuoteRequestPayload(String name, String email, String phone, String productType, String description,
            String preferredColor, int quantity, String requiredDate, String additionalNotes) {}
    public record StatusUpdate(String status) {}
    public record QuoteRequestResponse(Long id, String referenceCode, String name, String email, String phone,
            String productType, String description, String preferredColor, int quantity, LocalDate requiredDate,
            String additionalNotes, String attachmentName, String attachmentUrl, QuoteStatus status, LocalDateTime createdAt,
            boolean deleted, LocalDateTime deletedAt) {}
}
