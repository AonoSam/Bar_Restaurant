package com.night_club.bar_restaurant.shifttransfer;

import jakarta.validation.constraints.NotNull;

public record CreateShiftTransferRequest(
        @NotNull(message = "Recipient is required") Long recipientId,
        @NotNull(message = "Reason is required") ShiftTransferReason reason,
        // Free text - optional for END_OF_SHIFT, but in practice EMERGENCY
        // should almost always come with one, even though it's not enforced
        // as a hard requirement here (see ShiftTransferReason's doc comment
        // on why this whole feature can't hard-enforce its own rules).
        String note
) {
}