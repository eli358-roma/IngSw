package com.hackhub.pattern.facade;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servizio per gestire i pagamenti dei premi agli hackathon
 * Simula l'integrazione con un servizio di pagamento esterno (Stripe, PayPal, etc.)
 */
@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${payment.service.enabled:true}")
    private boolean enabled;

    @Value("${payment.service.provider:mock}")
    private String provider;

    @Value("${payment.service.currency:EUR}")
    private String defaultCurrency;

    // Mappa per tracciare le transazioni (simula database esterno)
    private final Map<String, PaymentTransaction> transactions = new HashMap<>();

    /**
     * Processa il pagamento di un premio al team vincitore
     */
    public PaymentTransaction processPrizePayment(BigDecimal amount, String currency,
                                                  String teamName, String teamLeaderEmail,
                                                  String teamLeaderName, String hackathonName) {

        if (!enabled) {
            logger.warn("Payment service is disabled. Returning mock transaction.");
            return createMockTransaction(amount, teamName, hackathonName);
        }

        // Validazione
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("L'importo deve essere maggiore di zero");
        }

        if (currency == null || currency.trim().isEmpty()) {
            currency = defaultCurrency;
        }

        // Genera ID transazione
        String transactionId = generateTransactionId();

        // Crea la transazione
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(transactionId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setRecipientName(teamName);
        transaction.setRecipientEmail(teamLeaderEmail);
        transaction.setRecipientFullName(teamLeaderName);
        transaction.setDescription("Premio Hackathon: " + hackathonName);
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setHackathonName(hackathonName);

        // Simula chiamata API al servizio esterno
        if ("stripe".equalsIgnoreCase(provider)) {
            return processStripePayment(transaction);
        } else if ("paypal".equalsIgnoreCase(provider)) {
            return processPayPalPayment(transaction);
        } else {
            // Mock provider
            return processMockPayment(transaction);
        }
    }

    /**
     * Verifica lo stato di una transazione
     */
    public PaymentTransaction getTransactionStatus(String transactionId) {
        PaymentTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            throw new RuntimeException("Transaction not found: " + transactionId);
        }

        // Simula aggiornamento dello stato
        if ("PENDING".equals(transaction.getStatus())) {
            // Simula che il pagamento vada a buon fine dopo qualche secondo
            if (transaction.getCreatedAt().plusSeconds(5).isBefore(LocalDateTime.now())) {
                transaction.setStatus("COMPLETED");
                transaction.setCompletedAt(LocalDateTime.now());
                transactions.put(transactionId, transaction);
            }
        }

        return transaction;
    }

    /**
     * Richiedi il rimborso di un pagamento
     */
    public PaymentTransaction refundPayment(String transactionId, String reason) {
        PaymentTransaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            throw new RuntimeException("Transaction not found: " + transactionId);
        }

        if (!"COMPLETED".equals(transaction.getStatus())) {
            throw new RuntimeException("Only completed transactions can be refunded");
        }

        // Crea transazione di rimborso
        PaymentTransaction refund = new PaymentTransaction();
        refund.setId("refund_" + transactionId);
        refund.setAmount(transaction.getAmount());
        refund.setCurrency(transaction.getCurrency());
        refund.setRecipientName(transaction.getRecipientName());
        refund.setRecipientEmail(transaction.getRecipientEmail());
        refund.setDescription("Rimborso: " + reason);
        refund.setStatus("REFUNDED");
        refund.setCreatedAt(LocalDateTime.now());
        refund.setCompletedAt(LocalDateTime.now());
        refund.setParentTransactionId(transactionId);

        transactions.put(refund.getId(), refund);

        // Aggiorna transazione originale
        transaction.setRefundTransactionId(refund.getId());
        transactions.put(transactionId, transaction);

        logger.info("Refund processed: {} for transaction: {}", refund.getId(), transactionId);
        simulateNetworkDelay();

        return refund;
    }

    /**
     * Ottieni tutte le transazioni per un hackathon
     */
    public List<PaymentTransaction> getTransactionsForHackathon(String hackathonName) {
        return transactions.values().stream()
                .filter(t -> hackathonName.equals(t.getHackathonName()))
                .collect(Collectors.toList());
    }

    /**
     * Metodo per integrazione con Stripe (mock)
     */
    private PaymentTransaction processStripePayment(PaymentTransaction transaction) {
        logger.info("Processing Stripe payment for: {}", transaction.getRecipientEmail());

        // Simula chiamata API a Stripe
        String stripeChargeId = "ch_" + UUID.randomUUID().toString().substring(0, 10);
        transaction.setExternalId(stripeChargeId);
        transaction.setProvider("Stripe");

        // Simula pagamento in corso
        transaction.setStatus("PROCESSING");
        transactions.put(transaction.getId(), transaction);

        simulateNetworkDelay(2000);

        // Simula completamento
        transaction.setStatus("COMPLETED");
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setExternalStatus("succeeded");

        transactions.put(transaction.getId(), transaction);

        logger.info("Stripe payment completed successfully: {}", stripeChargeId);
        return transaction;
    }

    /**
     * Metodo per integrazione con PayPal (mock)
     */
    private PaymentTransaction processPayPalPayment(PaymentTransaction transaction) {
        logger.info("Processing PayPal payment for: {}", transaction.getRecipientEmail());

        // Simula chiamata API a PayPal
        String paypalPaymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        transaction.setExternalId(paypalPaymentId);
        transaction.setProvider("PayPal");

        transaction.setStatus("PROCESSING");
        transactions.put(transaction.getId(), transaction);

        simulateNetworkDelay(2500);

        transaction.setStatus("COMPLETED");
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setExternalStatus("approved");

        transactions.put(transaction.getId(), transaction);

        logger.info("PayPal payment completed successfully: {}", paypalPaymentId);
        return transaction;
    }

    /**
     * Processa pagamento mock (per testing)
     */
    private PaymentTransaction processMockPayment(PaymentTransaction transaction) {
        logger.info("Processing mock payment for: {}", transaction.getRecipientName());

        transaction.setExternalId("mock_" + UUID.randomUUID().toString());
        transaction.setProvider("Mock Payment Service");

        // Simula elaborazione
        transaction.setStatus("PROCESSING");
        transactions.put(transaction.getId(), transaction);

        simulateNetworkDelay(1000);

        // Completa il pagamento
        transaction.setStatus("COMPLETED");
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setExternalStatus("mock_success");

        transactions.put(transaction.getId(), transaction);

        logger.info("Mock payment completed: {}", transaction.getId());
        return transaction;
    }

    /**
     * Crea transazione mock (per quando il servizio è disabilitato)
     */
    private PaymentTransaction createMockTransaction(BigDecimal amount, String teamName,
                                                     String hackathonName) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId("mock_tx_" + UUID.randomUUID().toString());
        transaction.setAmount(amount);
        transaction.setCurrency(defaultCurrency);
        transaction.setRecipientName(teamName);
        transaction.setDescription("Mock prize for: " + hackathonName);
        transaction.setStatus("MOCK_COMPLETED");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setProvider("Mock Service");
        transaction.setHackathonName(hackathonName);

        transactions.put(transaction.getId(), transaction);
        return transaction;
    }

    // ========== METODI DI UTILITÀ ==========

    private String generateTransactionId() {
        return "tx_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 6);
    }

    private void simulateNetworkDelay() {
        simulateNetworkDelay(1000);
    }

    private void simulateNetworkDelay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Classe interna per rappresentare una transazione di pagamento
     */
    public static class PaymentTransaction {
        private String id;
        private String externalId;
        private BigDecimal amount;
        private String currency;
        private String recipientName;
        private String recipientEmail;
        private String recipientFullName;
        private String description;
        private String status; // PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
        private String externalStatus;
        private String provider;
        private String hackathonName;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private String parentTransactionId;
        private String refundTransactionId;

        // Getter e Setter
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getRecipientName() { return recipientName; }
        public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

        public String getRecipientEmail() { return recipientEmail; }
        public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

        public String getRecipientFullName() { return recipientFullName; }
        public void setRecipientFullName(String recipientFullName) { this.recipientFullName = recipientFullName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getExternalStatus() { return externalStatus; }
        public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getHackathonName() { return hackathonName; }
        public void setHackathonName(String hackathonName) { this.hackathonName = hackathonName; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

        public String getParentTransactionId() { return parentTransactionId; }
        public void setParentTransactionId(String parentTransactionId) { this.parentTransactionId = parentTransactionId; }

        public String getRefundTransactionId() { return refundTransactionId; }
        public void setRefundTransactionId(String refundTransactionId) { this.refundTransactionId = refundTransactionId; }

        public boolean isCompleted() {
            return "COMPLETED".equals(status);
        }

        public boolean isFailed() {
            return "FAILED".equals(status);
        }

        @Override
        public String toString() {
            return String.format("PaymentTransaction[id=%s, amount=%s %s, recipient=%s, status=%s]",
                    id, amount, currency, recipientName, status);
        }
    }
}