package com.mwangahakika.backend.service;

import com.mwangahakika.backend.dto.TransferRequest;
import com.mwangahakika.backend.dto.TransferResponse;

public interface TransferService {
    TransferResponse transfer(Long authenticatedUserId, String idempotencyKey, TransferRequest request);
}
