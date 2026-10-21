package com.paymentplatform.organization.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Charge utile QR commune aux documents metier. Le scanner de l'application
 * reconnait ce JSON et ouvre la page de detail du type concerne.
 */
public final class DocumentQrPayload {

    public static final String TYPE_ORDER = "ORDER";
    public static final String TYPE_INVOICE = "INVOICE";
    public static final String TYPE_QUOTE = "QUOTE";
    public static final String TYPE_DELIVERY = "DELIVERY";
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_DISPUTE = "DISPUTE";
    public static final String TYPE_WITHHOLDING_CERTIFICATE = "WITHHOLDING_CERTIFICATE";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DocumentQrPayload() {
    }

    public static String of(String type, String reference) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("v", 1);
        payload.put("type", type);
        payload.put("reference", reference != null ? reference : "");
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible de generer le QR du document", e);
        }
    }
}
