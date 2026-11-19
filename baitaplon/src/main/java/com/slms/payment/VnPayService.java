package com.slms.payment;

import com.slms.model.Fine;
import com.slms.model.enums.PaymentStatus;
import com.slms.service.FineService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tạo URL thanh toán VNPay và xác nhận kết quả return/IPN.
 */
public class VnPayService {

    private static final DateTimeFormatter VNP_DATE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ConcurrentHashMap<String, PendingVnPayOrder> PENDING = new ConcurrentHashMap<>();

    private final FineService fineService;
    private final int serverPort;

    public VnPayService(int serverPort) {
        this.fineService = new FineService();
        this.serverPort = serverPort;
    }

    public VnPayService(FineService fineService, int serverPort) {
        this.fineService = fineService;
        this.serverPort = serverPort;
    }

    /**
     * Tạo URL thanh toán cho một hoặc nhiều hóa đơn phạt chưa thanh toán.
     */
    public Map<String, Object> createPayment(String readerId, List<String> fineIds, String clientIp) {
        if (readerId == null || readerId.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã độc giả.");
        }
        if (fineIds == null || fineIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một hóa đơn phạt.");
        }
        clientIp = normalizeClientIp(clientIp);

        List<String> uniqueFineIds = fineIds.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        List<Fine> unpaid = new ArrayList<>();
        long totalVnd = 0;

        for (String fineId : uniqueFineIds) {
            Optional<Fine> fineOpt = fineService.getFineById(fineId);
            if (fineOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy hóa đơn phạt: " + fineId);
            }
            Fine fine = fineOpt.get();
            if (fine.getPaymentStatus() == PaymentStatus.Paid) {
                throw new IllegalStateException("Hóa đơn " + fineId + " đã được thanh toán.");
            }
            if (!fineService.isFineOwnedByReader(fineId, readerId)) {
                throw new IllegalArgumentException("Hóa đơn " + fineId + " không thuộc tài khoản của bạn.");
            }
            unpaid.add(fine);
            totalVnd += Math.round(fine.getFineAmount());
        }

        if (totalVnd <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ.");
        }

        String txnRef = buildTxnRef();
        PendingVnPayOrder order = new PendingVnPayOrder(txnRef, readerId, uniqueFineIds, totalVnd);
        PENDING.put(txnRef, order);

        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        String createDate = now.format(VNP_DATE);
        String expireDate = now.plusMinutes(VnPayConfig.EXPIRE_MINUTES).format(VNP_DATE);

        String orderInfo = sanitizeOrderInfo(
                "Thanh toan phat thu vien " + String.join(",", uniqueFineIds)
        );

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", VnPayConfig.VERSION);
        vnpParams.put("vnp_Command", VnPayConfig.COMMAND);
        vnpParams.put("vnp_TmnCode", VnPayConfig.tmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(totalVnd * 100));
        vnpParams.put("vnp_CurrCode", VnPayConfig.CURR_CODE);
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", VnPayConfig.ORDER_TYPE);
        vnpParams.put("vnp_Locale", VnPayConfig.LOCALE_VN);
        vnpParams.put("vnp_ReturnUrl", VnPayConfig.returnUrl(serverPort));
        vnpParams.put("vnp_IpAddr", clientIp);
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);

        String paymentUrl;
        String mode = VnPayConfig.mode();
        if (VnPayConfig.isLocalMode()) {
            // Giả lập cổng VNPay trên máy local (không phụ thuộc credential Sandbox thật)
            paymentUrl = VnPayConfig.localPayPage(serverPort)
                    + "?txnRef=" + VnPayUtil.urlEncode(txnRef)
                    + "&amount=" + totalVnd
                    + "&orderInfo=" + VnPayUtil.urlEncode(orderInfo);
        } else {
            paymentUrl = VnPayUtil.buildPaymentUrl(
                    VnPayConfig.payUrl(),
                    vnpParams,
                    VnPayConfig.hashSecret()
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentUrl", paymentUrl);
        result.put("txnRef", txnRef);
        result.put("amount", totalVnd);
        result.put("fineIds", uniqueFineIds);
        result.put("fineCount", unpaid.size());
        result.put("expireMinutes", VnPayConfig.EXPIRE_MINUTES);
        result.put("mode", mode);
        return result;
    }

    /**
     * Hoàn tất giao dịch ở chế độ local demo (ký tham số giống Return URL của VNPay).
     */
    public String buildLocalReturnRedirect(String txnRef, boolean success) {
        PendingVnPayOrder order = PENDING.get(txnRef);
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch đang chờ: " + txnRef);
        }

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", String.valueOf(order.getAmountVnd() * 100));
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_BankTranNo", "VNP" + System.currentTimeMillis());
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan phat thu vien");
        params.put("vnp_PayDate", ZonedDateTime.now(VN_ZONE).format(VNP_DATE));
        params.put("vnp_ResponseCode", success ? "00" : "24");
        params.put("vnp_TmnCode", VnPayConfig.tmnCode());
        params.put("vnp_TransactionNo", String.valueOf(System.currentTimeMillis() % 1_000_000_000L));
        params.put("vnp_TransactionStatus", success ? "00" : "02");
        params.put("vnp_TxnRef", txnRef);

        String query = VnPayUtil.buildQuery(params, VnPayConfig.hashSecret());
        return VnPayConfig.returnUrl(serverPort) + "?" + query;
    }

    public PendingVnPayOrder getPendingOrder(String txnRef) {
        return PENDING.get(txnRef);
    }

    private static String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "127.0.0.1";
        }
        String ip = clientIp.trim();
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.contains(":")) {
            return "127.0.0.1";
        }
        return ip;
    }

    /**
     * Xử lý callback Return URL từ VNPay.
     *
     * @return map chứa status, message, txnRef, amount, fineIds
     */
    public Map<String, Object> handleReturn(Map<String, String> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        String txnRef = params.getOrDefault("vnp_TxnRef", "");
        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "");
        String amountRaw = params.getOrDefault("vnp_Amount", "0");

        result.put("txnRef", txnRef);
        result.put("responseCode", responseCode);

        if (!VnPayUtil.validateSecureHash(params, VnPayConfig.hashSecret())) {
            result.put("success", false);
            result.put("status", "invalid_signature");
            result.put("message", "Chữ ký VNPay không hợp lệ. Giao dịch bị từ chối.");
            return result;
        }

        PendingVnPayOrder order = PENDING.get(txnRef);
        long paidAmountVnd;
        try {
            paidAmountVnd = Long.parseLong(amountRaw) / 100L;
        } catch (NumberFormatException e) {
            paidAmountVnd = 0;
        }
        result.put("amount", paidAmountVnd);

        if (order != null) {
            result.put("fineIds", order.getFineIds());
            result.put("readerId", order.getReaderId());
            if (order.getAmountVnd() != paidAmountVnd) {
                result.put("success", false);
                result.put("status", "amount_mismatch");
                result.put("message", "Số tiền trả về không khớp đơn thanh toán.");
                return result;
            }
        } else {
            result.put("fineIds", List.of());
        }

        boolean success = "00".equals(responseCode) && ("00".equals(transactionStatus) || transactionStatus.isEmpty());
        if (!success) {
            result.put("success", false);
            result.put("status", "failed");
            result.put("message", mapResponseMessage(responseCode));
            return result;
        }

        List<String> paidFineIds = new ArrayList<>();
        if (order != null) {
            for (String fineId : order.getFineIds()) {
                try {
                    Optional<Fine> fineOpt = fineService.getFineById(fineId);
                    if (fineOpt.isPresent() && fineOpt.get().getPaymentStatus() != PaymentStatus.Paid) {
                        fineService.payFine(fineId);
                        paidFineIds.add(fineId);
                    } else if (fineOpt.isPresent()) {
                        paidFineIds.add(fineId);
                    }
                } catch (IllegalStateException alreadyPaid) {
                    paidFineIds.add(fineId);
                }
            }
            PENDING.remove(txnRef);
        }

        result.put("success", true);
        result.put("status", "success");
        result.put("message", "Thanh toán VNPay thành công.");
        result.put("paidFineIds", paidFineIds);
        return result;
    }

    /**
     * IPN: xác nhận và trả mã phản hồi theo quy ước VNPay.
     */
    public Map<String, String> handleIpn(Map<String, String> params) {
        Map<String, String> response = new LinkedHashMap<>();
        if (!VnPayUtil.validateSecureHash(params, VnPayConfig.hashSecret())) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid Signature");
            return response;
        }

        Map<String, Object> handled = handleReturn(params);
        if (Boolean.TRUE.equals(handled.get("success"))) {
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
        } else if ("amount_mismatch".equals(handled.get("status"))) {
            response.put("RspCode", "04");
            response.put("Message", "Invalid Amount");
        } else {
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
        }
        return response;
    }

    private static String buildTxnRef() {
        // VNPay yêu cầu mã giao dịch duy nhất, tối đa 100 ký tự
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "SLMS" + System.currentTimeMillis() + uuid.substring(0, 6);
    }

    private static String sanitizeOrderInfo(String info) {
        String cleaned = info.replaceAll("[^a-zA-Z0-9 ,._:-]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 240) {
            cleaned = cleaned.substring(0, 240);
        }
        return cleaned.isEmpty() ? "Thanh toan phat thu vien" : cleaned;
    }

    private static String mapResponseMessage(String code) {
        return switch (code) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới gian lận).";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking.";
            case "10" -> "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11" -> "Đã hết hạn chờ thanh toán. Vui lòng thực hiện lại giao dịch.";
            case "12" -> "Thẻ/Tài khoản bị khóa.";
            case "13" -> "Sai OTP. Vui lòng thực hiện lại giao dịch.";
            case "24" -> "Khách hàng hủy giao dịch.";
            case "51" -> "Tài khoản không đủ số dư.";
            case "65" -> "Tài khoản đã vượt quá hạn mức giao dịch trong ngày.";
            case "75" -> "Ngân hàng thanh toán đang bảo trì.";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định.";
            case "99" -> "Lỗi không xác định / người dùng hủy.";
            default -> "Thanh toán không thành công (mã: " + code + ").";
        };
    }
}
