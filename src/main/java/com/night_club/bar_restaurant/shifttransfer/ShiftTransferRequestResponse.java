package com.night_club.bar_restaurant.shifttransfer;

import java.time.LocalDateTime;

public record ShiftTransferRequestResponse(
        Long id,
        Long requesterId,
        String requesterName,
        Long recipientId,
        String recipientName,
        String role,
        ShiftTransferReason reason,
        String note,
        ShiftTransferStatus status,
        LocalDateTime createdAt,
        LocalDateTime respondedAt
) {
    public static ShiftTransferRequestResponse fromEntity(
            ShiftTransferRequest r, String requesterName, String recipientName
    ) {
        return new ShiftTransferRequestResponse(
                r.getId(),
                r.getRequesterId(),
                requesterName,
                r.getRecipientId(),
                recipientName,
                r.getRole().name(),
                r.getReason(),
                r.getNote(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getRespondedAt()
        );
    }
}