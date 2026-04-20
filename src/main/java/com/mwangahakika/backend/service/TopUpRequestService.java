package com.mwangahakika.backend.service;

import java.util.List;

import com.mwangahakika.backend.dto.CreateTopUpRequestRequest;
import com.mwangahakika.backend.dto.ReviewTopUpRequestRequest;
import com.mwangahakika.backend.dto.TopUpRequestResponse;
import com.mwangahakika.backend.enums.TopUpRequestStatus;

public interface TopUpRequestService {

    TopUpRequestResponse create(Long authenticatedUserId, CreateTopUpRequestRequest request);

    List<TopUpRequestResponse> getMyRequests(Long authenticatedUserId);

    List<TopUpRequestResponse> getByStatus(TopUpRequestStatus status);

    TopUpRequestResponse approve(Long adminUserId, Long requestId, ReviewTopUpRequestRequest request);

    TopUpRequestResponse reject(Long adminUserId, Long requestId, ReviewTopUpRequestRequest request);
}