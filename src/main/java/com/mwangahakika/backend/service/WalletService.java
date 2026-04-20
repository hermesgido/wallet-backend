package com.mwangahakika.backend.service;

import com.mwangahakika.backend.dto.AdminTopUpRequest;
import com.mwangahakika.backend.dto.AdminTopUpResponse;

public interface WalletService {

    AdminTopUpResponse adminTopUp(Long adminUserId, Long walletId, AdminTopUpRequest request);
}