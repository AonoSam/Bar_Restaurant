package com.night_club.bar_restaurant.supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public List<SupplierResponse> getAll() {
        return supplierRepository.findAll().stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }

    public SupplierResponse getById(Long id) {
        return SupplierResponse.fromEntity(findOrThrow(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .name(request.name())
                .contactPerson(request.contactPerson())
                .phone(request.phone())
                .email(request.email())
                .address(request.address())
                .notes(request.notes())
                .status(SupplierStatus.ACTIVE)
                .build();
        return SupplierResponse.fromEntity(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = findOrThrow(id);
        supplier.setName(request.name());
        supplier.setContactPerson(request.contactPerson());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setNotes(request.notes());
        return SupplierResponse.fromEntity(supplierRepository.save(supplier));
    }

    @Transactional
    public void deactivate(Long id) {
        Supplier supplier = findOrThrow(id);
        // Soft delete only - past StockMovement rows reference supplierId,
        // same reasoning as every other soft-delete pattern in this system
        // (Product, Employee): a hard delete would orphan historical records.
        supplier.setStatus(SupplierStatus.INACTIVE);
        supplierRepository.save(supplier);
    }

    @Transactional
    public SupplierResponse reactivate(Long id) {
        Supplier supplier = findOrThrow(id);
        supplier.setStatus(SupplierStatus.ACTIVE);
        return SupplierResponse.fromEntity(supplierRepository.save(supplier));
    }

    private Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found: " + id));
    }
}