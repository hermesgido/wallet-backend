package com.mwangahakika.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mwangahakika.backend.entity.Transfer;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByReference(String reference);
    List<Transfer> findBySenderWalletId(Long walletId);
    List<Transfer> findByReceiverWalletId(Long walletId);
}
