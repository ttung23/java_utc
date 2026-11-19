package com.slms.payment;

import java.util.Collections;
import java.util.List;

/**
 * Đơn thanh toán VNPay đang chờ kết quả (lưu tạm trong bộ nhớ).
 */
public class PendingVnPayOrder {

    private final String txnRef;
    private final String readerId;
    private final List<String> fineIds;
    private final long amountVnd;
    private final long createdAtMs;

    public PendingVnPayOrder(String txnRef, String readerId, List<String> fineIds, long amountVnd) {
        this.txnRef = txnRef;
        this.readerId = readerId;
        this.fineIds = List.copyOf(fineIds);
        this.amountVnd = amountVnd;
        this.createdAtMs = System.currentTimeMillis();
    }

    public String getTxnRef() {
        return txnRef;
    }

    public String getReaderId() {
        return readerId;
    }

    public List<String> getFineIds() {
        return Collections.unmodifiableList(fineIds);
    }

    public long getAmountVnd() {
        return amountVnd;
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }
}
