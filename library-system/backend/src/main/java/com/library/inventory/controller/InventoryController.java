package com.library.inventory.controller;

import com.library.common.api.ApiResponse;
import com.library.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Inventory")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class InventoryController {

    private final InventoryService inventoryService;

    // Suppliers
    @GetMapping("/suppliers")
    public ApiResponse<List<Map<String, Object>>> listSuppliers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return page(inventoryService.listSuppliers(q, page, size));
    }

    @PostMapping("/suppliers")
    public ApiResponse<Map<String, Object>> createSupplier(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(inventoryService.createSupplier(body));
    }

    @PutMapping("/suppliers/{id}")
    public ApiResponse<Map<String, Object>> updateSupplier(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(inventoryService.updateSupplier(id, body));
    }

    @DeleteMapping("/suppliers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteSupplier(@PathVariable Long id) {
        inventoryService.deleteSupplier(id);
        return ApiResponse.ok(null, "Đã xóa NCC");
    }

    // Imports
    @GetMapping("/imports")
    public ApiResponse<List<Map<String, Object>>> listImports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return page(inventoryService.listImports(page, size));
    }

    @PostMapping("/imports")
    public ApiResponse<Map<String, Object>> createImport(@RequestBody ImportReq req) {
        return ApiResponse.ok(inventoryService.createImport(req.getSupplierId(), req.getSource(), req.getNote(), req.getItems()));
    }

    @PostMapping("/imports/{id}/submit")
    public ApiResponse<Map<String, Object>> submitImport(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.submitImport(id));
    }

    @PostMapping("/imports/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> approveImport(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.approveImport(id), "Đã duyệt & hoàn tất nhập");
    }

    @PostMapping("/imports/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> completeImport(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.completeImport(id));
    }

    // Audits
    @GetMapping("/inventory-audits")
    public ApiResponse<List<Map<String, Object>>> listAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return page(inventoryService.listAudits(page, size));
    }

    @PostMapping("/inventory-audits")
    public ApiResponse<Map<String, Object>> createAudit(@RequestBody AuditReq req) {
        return ApiResponse.ok(inventoryService.createAudit(req.getScopeType(), req.getScopeValue()));
    }

    @PostMapping("/inventory-audits/{id}/scan")
    public ApiResponse<Map<String, Object>> scan(@PathVariable Long id, @RequestBody ScanReq req) {
        return ApiResponse.ok(inventoryService.scanAudit(id, req.getBarcode()));
    }

    @PostMapping("/inventory-audits/{id}/close")
    public ApiResponse<Map<String, Object>> close(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.closeAudit(id));
    }

    @PostMapping("/inventory-audits/{id}/apply-adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> adjust(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.applyMissingAsLost(id));
    }

    // Disposals
    @GetMapping("/disposals")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> listDisposals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return page(inventoryService.listDisposals(page, size));
    }

    @PostMapping("/disposals")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> createDisposal(@RequestBody DisposalReq req) {
        return ApiResponse.ok(inventoryService.createDisposal(req.getReason(), req.getCopyIds(), req.getNote()));
    }

    @PostMapping("/disposals/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> approveDisposal(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.approveDisposal(id), "Đã thanh lý");
    }

    private ApiResponse<List<Map<String, Object>>> page(InventoryService.PageResult result) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", result.page());
        meta.put("limit", result.size());
        meta.put("total", result.total());
        ApiResponse<List<Map<String, Object>>> res = ApiResponse.ok(result.items());
        res.setMeta(meta);
        return res;
    }

    @Data
    public static class ImportReq {
        private Long supplierId;
        private String source;
        private String note;
        private List<Map<String, Object>> items;
    }

    @Data
    public static class AuditReq {
        private String scopeType;
        private String scopeValue;
    }

    @Data
    public static class ScanReq {
        private String barcode;
    }

    @Data
    public static class DisposalReq {
        private String reason;
        private List<Long> copyIds;
        private String note;
    }
}
