package com.night_club.bar_restaurant.shifttransfer;

import com.night_club.bar_restaurant.employee.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shift_transfer_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftTransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    // Snapshotted at request time - both parties are required to share this
    // role when the request is created (ShiftTransferService.create()), so
    // this is redundant with looking it up live, but keeping it on the
    // record means the request's own history stays meaningful even if
    // someone's role is later changed by a manager.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftTransferReason reason;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShiftTransferStatus status = ShiftTransferStatus.PENDING;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}