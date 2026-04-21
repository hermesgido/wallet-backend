package com.mwangahakika.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mwangahakika.backend.entity.IdempotencyRecord;
import com.mwangahakika.backend.enums.IdempotencyStatus;
import com.mwangahakika.backend.repository.IdempotencyRecordRepository;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository, ObjectMapper objectMapper) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    public <T> IdempotencyStart<T> begin(String idempotencyKey, String endpoint, Long userId, Object request, Class<T> responseType) {
        String requestHash = requestHash(request);

        return idempotencyRecordRepository
                .findByIdempotencyKeyAndEndpointAndUserId(idempotencyKey, endpoint, userId)
                .map(record -> resolveExisting(record, requestHash, responseType))
                .orElseGet(() -> createRecord(idempotencyKey, endpoint, userId, requestHash, responseType));
    }

    public void complete(IdempotencyRecord record, int responseCode, Object response) {
        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setResponseCode(responseCode);
        record.setResponseBody(writeJson(response));
        record.setUpdatedAt(LocalDateTime.now());
        idempotencyRecordRepository.save(record);
    }

    private <T> IdempotencyStart<T> createRecord(
            String idempotencyKey,
            String endpoint,
            Long userId,
            String requestHash,
            Class<T> responseType
    ) {
        LocalDateTime now = LocalDateTime.now();
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .endpoint(endpoint)
                .userId(userId)
                .requestHash(requestHash)
                .status(IdempotencyStatus.IN_PROGRESS)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            IdempotencyRecord saved = idempotencyRecordRepository.saveAndFlush(record);
            return IdempotencyStart.proceed(saved);
        } catch (DataIntegrityViolationException ex) {
            IdempotencyRecord existing = idempotencyRecordRepository
                    .findByIdempotencyKeyAndEndpointAndUserId(idempotencyKey, endpoint, userId)
                    .orElseThrow(() -> ex);
            return resolveExisting(existing, requestHash, responseType);
        }
    }

    private <T> IdempotencyStart<T> resolveExisting(IdempotencyRecord record, String requestHash, Class<T> responseType) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IllegalStateException("Idempotency key has already been used with a different request.");
        }

        if (record.getStatus() != IdempotencyStatus.COMPLETED) {
            throw new IllegalStateException("Request with this idempotency key is already being processed.");
        }

        return IdempotencyStart.replay(readJson(record.getResponseBody(), responseType));
    }

    private String requestHash(Object request) {
        String body = writeJson(request);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to generate request hash", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize idempotency payload", ex);
        }
    }

    private <T> T readJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize idempotency payload", ex);
        }
    }

    public record IdempotencyStart<T>(IdempotencyRecord record, T response, boolean replay) {
        public static <T> IdempotencyStart<T> proceed(IdempotencyRecord record) {
            return new IdempotencyStart<>(record, null, false);
        }

        public static <T> IdempotencyStart<T> replay(T response) {
            return new IdempotencyStart<>(null, response, true);
        }
    }
}
