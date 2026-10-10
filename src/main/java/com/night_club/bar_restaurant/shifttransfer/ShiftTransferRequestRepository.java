package com.night_club.bar_restaurant.shifttransfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftTransferRequestRepository extends JpaRepository<ShiftTransferRequest, Long> {

    // "Requests waiting for my response" - the recipient's inbox.
    List<ShiftTransferRequest> findByRecipientIdAndStatusOrderByCreatedAtDesc(
            Long recipientId, ShiftTransferStatus status
    );

    // "Requests I've sent" - any status, so the requester can see how a
    // past request was resolved, not just the pending ones.
    List<ShiftTransferRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    // Used to block sending a second request while one is still pending -
    // one outstanding handover attempt at a time per requester.
    Optional<ShiftTransferRequest> findFirstByRequesterIdAndStatus(
            Long requesterId, ShiftTransferStatus status
    );

    // Manager oversight - every request, any status.
    List<ShiftTransferRequest> findAllByOrderByCreatedAtDesc();
}