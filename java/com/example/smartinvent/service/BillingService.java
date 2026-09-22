package com.example.smartinvent.service;

import com.example.smartinvent.dto.SaleItemRequest;
import com.example.smartinvent.dto.SaleRequest;
import com.example.smartinvent.entity.Product;
import com.example.smartinvent.entity.Sale;
import com.example.smartinvent.repository.ProductRepository;
import com.example.smartinvent.repository.SaleRepository;
import com.example.smartinvent.util.DateFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingService {
    private static final double GST_RATE = 18D;
    @Autowired
    private SaleRepository saleRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ActivityLogService activityLogService;

    public List<Sale> getAll() {
        return getAll(null, null, null, null, null);
    }

    public List<Sale> getAll(String period, String productType, String paymentMethod, String from, String to) {
        LocalDate fromDate = parseDate(from), toDate = parseDate(to);
        return saleRepository.findAllByOrderByDateDesc().stream()
                .filter(s -> DateFilterUtil.matches(s.getDate(), period))
                .filter(s -> s.getSalesChannel() == null || "COUNTER".equalsIgnoreCase(s.getSalesChannel()))
                .filter(s -> productType == null || productType.isBlank() || "ALL".equalsIgnoreCase(productType)
                        || (s.getProduct() != null && s.getProduct().getProductType().equalsIgnoreCase(productType)))
                .filter(s -> paymentMethod == null || paymentMethod.isBlank() || "ALL".equalsIgnoreCase(paymentMethod)
                        || paymentMethod.equalsIgnoreCase(s.getPaymentMethod()))
                .filter(s -> fromDate == null || (s.getDate() != null && !s.getDate().isBefore(fromDate)))
                .filter(s -> toDate == null || (s.getDate() != null && !s.getDate().isAfter(toDate)))
                .collect(Collectors.toList());
    }

    public List<Sale> getCustomerOrders(String username) {
        return saleRepository.findAllByOrderByDateDesc().stream().filter(s -> username.equals(s.getSoldBy()))
                .collect(Collectors.toList());
    }

    public List<Sale> getOnlineOrders(String period, String paymentMethod) {
        return saleRepository.findAllByOrderByDateDesc().stream()
                .filter(s -> "ONLINE".equalsIgnoreCase(s.getSalesChannel()))
                .filter(s -> DateFilterUtil.matches(s.getDate(), period))
                .filter(s -> paymentMethod == null || paymentMethod.isBlank() || "ALL".equalsIgnoreCase(paymentMethod)
                        || paymentMethod.equalsIgnoreCase(s.getPaymentMethod()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Sale> cancelCustomerOrder(String invoice, String username, String reason) {
        if (reason == null || reason.isBlank())
            throw new RuntimeException("Please provide a cancellation reason");
        List<Sale> rows = saleRepository.findAll().stream().filter(s -> invoice.equals(s.getInvoiceNumber())
                && username.equals(s.getSoldBy()) && "ONLINE".equalsIgnoreCase(s.getSalesChannel()))
                .collect(Collectors.toList());
        if (rows.isEmpty())
            throw new RuntimeException("Order not found");
        if (!LocalDate.now().equals(rows.get(0).getDate()) || "DELIVERED".equalsIgnoreCase(rows.get(0).getOrderStatus())
                || "CANCELLED".equalsIgnoreCase(rows.get(0).getOrderStatus()))
            throw new RuntimeException("This order can no longer be cancelled");
        rows.forEach(s -> {
            s.setOrderStatus("CANCELLED");
            s.setCancellationReason(reason.trim());
        });
        return saleRepository.saveAll(rows);
    }

    @Transactional
    public List<Sale> updateOrderStatus(String invoice, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ORDER_PLACED", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED").contains(normalizedStatus))
            throw new RuntimeException("Invalid order status");
        List<Sale> rows = saleRepository.findAll().stream().filter(s -> invoice.equals(s.getInvoiceNumber()))
                .collect(Collectors.toList());
        if (rows.isEmpty())
            throw new RuntimeException("Order not found");
        rows.forEach(s -> {
            s.setOrderStatus(normalizedStatus);
            if ("DELIVERED".equals(normalizedStatus) && s.getDeliveredDate() == null) {
                s.setDeliveredDate(LocalDate.now());
            }
        });
        return saleRepository.saveAll(rows);
    }

    @Transactional
    public List<Sale> recordDeliveredPayment(String invoice, Double amountReceived, String reference) {
        if (amountReceived == null || amountReceived < 0)
            throw new RuntimeException("Enter the amount received");
        List<Sale> rows = saleRepository.findAll().stream()
                .filter(s -> invoice.equals(s.getInvoiceNumber()))
                .collect(Collectors.toList());
        if (rows.isEmpty())
            throw new RuntimeException("Order not found");
        double total = rows.stream().mapToDouble(s -> s.getGrandTotal() == null ? 0 : s.getGrandTotal()).sum();
        if (!"DELIVERED".equalsIgnoreCase(rows.get(0).getOrderStatus())) {
            throw new RuntimeException("Payment can be recorded only after the order is delivered");
        }
        if (amountReceived + 0.009 < total)
            throw new RuntimeException("Amount received cannot be less than " + money(total));
        String cleanReference = blankToNull(reference);
        rows.forEach(s -> {
            s.setAmountReceived(amountReceived);
            s.setPaymentStatus("PAYMENT_SUCCESSFUL");
            if (cleanReference != null)
                s.setPaymentReference(cleanReference);
        });
        return saleRepository.saveAll(rows);
    }

    public List<Sale> getAll(String period, String productType) {
        return getAll(period, productType, null, null, null);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Date filters must use YYYY-MM-DD format");
        }
    }

    @Transactional
    public List<Sale> createBill(SaleRequest request, String username) {
        return createBill(request, username, "COUNTER");
    }

    @Transactional
    public List<Sale> createBill(SaleRequest request, String username, String salesChannel) {
        if (request == null)
            throw new RuntimeException("Bill details are required");
        String method = normalizePaymentMethod(request.getPaymentMethod());
        if (!"ONLINE".equalsIgnoreCase(salesChannel))
            validatePayment(request, method);
        double gstRate = GST_RATE;
        List<SaleItemRequest> items = request.getItems() == null ? new ArrayList<>()
                : new ArrayList<>(request.getItems());
        if (items.isEmpty() && request.getProductId() != null) {
            SaleItemRequest item = new SaleItemRequest();
            item.setProductId(request.getProductId());
            item.setQuantity(request.getQuantity());
            items.add(item);
        }
        if (items.isEmpty())
            throw new RuntimeException("Add at least one selling product");
        String invoice = "INV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        List<Sale> result = new ArrayList<>();
        for (SaleItemRequest item : items) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0)
                throw new RuntimeException("Every bill item needs a valid product and quantity");
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (!"SELLING_PRODUCT".equalsIgnoreCase(product.getProductType()))
                throw new RuntimeException("Only selling products can be billed");
            if (product.getQuantity() < item.getQuantity())
                throw new RuntimeException("Not enough stock for " + product.getName());
            BigDecimal taxable = money(product.getSellingPrice() * item.getQuantity());
            BigDecimal gst = money(taxable.doubleValue() * gstRate / 100D);
            BigDecimal total = money(taxable.doubleValue() + gst.doubleValue());
            int old = product.getQuantity();
            product.setQuantity(old - item.getQuantity());
            product.setTotalSold(product.getTotalSold() + item.getQuantity());
            productRepository.save(product);
            Sale sale = new Sale();
            sale.setProduct(product);
            sale.setQuantity(item.getQuantity());
            sale.setSellingPrice(money(product.getSellingPrice()).doubleValue());
            sale.setTotalAmount(taxable.doubleValue());
            sale.setTaxableAmount(taxable.doubleValue());
            sale.setGstRate(gstRate);
            sale.setGstAmount(gst.doubleValue());
            sale.setGrandTotal(total.doubleValue());
            sale.setSoldBy(username);
            sale.setSalesChannel(salesChannel);
            sale.setDate(LocalDate.now());
            sale.setInvoiceNumber(invoice);
            sale.setCustomerName(blankToNull(request.getCustomerName()));
            sale.setCustomerPhone(blankToNull(request.getCustomerPhone()));
            sale.setPaymentMethod(method);
            sale.setPaymentStatus("ONLINE".equalsIgnoreCase(salesChannel) ? "PAYMENT_DUE_AFTER_DELIVERY"
                    : ("UPI_QR".equals(method) ? "PAYMENT_CONFIRMED_BY_STAFF" : "PAYMENT_SUCCESSFUL"));
            sale.setPaymentReference(paymentReference(request, method));
            sale.setAmountReceived("ONLINE".equalsIgnoreCase(salesChannel) ? null
                    : (request.getAmountReceived() == null ? total.doubleValue() : request.getAmountReceived()));
            sale.setOrderStatus("ORDER_PLACED");
            sale.setDeliveryAddress(blankToNull(request.getDeliveryAddress()));
            sale.setDeliveryCity(blankToNull(request.getDeliveryCity()));
            sale.setDeliveryState(blankToNull(request.getDeliveryState()));
            sale.setDeliveryPincode(blankToNull(request.getDeliveryPincode()));
            sale.setExpectedDeliveryDate(LocalDate.now().plusDays(1));
            saleRepository.save(sale);
            result.add(sale);
            activityLogService.log(username, "ONLINE".equalsIgnoreCase(salesChannel) ? "ONLINE_ORDER" : "BILL_CREATED",
                    invoice + " / " + product.getName() + " / ₹" + total + " via " + method + "; stock " + old + " -> "
                            + product.getQuantity());
        }
        return result;
    }

    private String paymentReference(SaleRequest r, String method) {
        if ("CARD".equals(method)) {
            String c = r.getCardNumber() == null ? "" : r.getCardNumber().replaceAll("\\D", "");
            return c.length() >= 4 ? "CARD-" + c.substring(c.length() - 4) : "CARD-DUE-AFTER-DELIVERY";
        }
        return blankToNull(r.getPaymentReference()) != null ? r.getPaymentReference().trim()
                : method + "-DUE-AFTER-DELIVERY";
    }

    private String normalizePaymentMethod(String method) {
        if (method == null || method.isBlank())
            throw new RuntimeException("Select a payment method");
        String n = method.trim().toUpperCase(Locale.ROOT);
        if (!List.of("CASH", "UPI_QR", "CARD").contains(n))
            throw new RuntimeException("Choose Cash, UPI QR, or Credit/Debit Card");
        return n;
    }

    private void validatePayment(SaleRequest r, String method) {
        if ("CARD".equals(method)) {
            String c = r.getCardNumber() == null ? "" : r.getCardNumber().replaceAll("\\D", "");
            if (c.length() < 12 || c.length() > 19 || !passesLuhn(c))
                throw new RuntimeException("Enter a valid card number");
            if (r.getCardCvv() == null || !r.getCardCvv().matches("\\d{3,4}"))
                throw new RuntimeException("Enter a valid card CVV");
            if (r.getCardExpiry() == null || !validExpiry(r.getCardExpiry()))
                throw new RuntimeException("Enter a valid future card expiry (MM/YY)");
        }
        if ("UPI_QR".equals(method) && r.getPaymentReference() != null && r.getPaymentReference().length() > 80)
            throw new RuntimeException("UPI reference is too long");
    }

    private boolean validExpiry(String e) {
        if (!e.matches("(0[1-9]|1[0-2])/\\d{2}"))
            return false;
        try {
            return !YearMonth.parse(e, DateTimeFormatter.ofPattern("MM/yy")).isBefore(YearMonth.now());
        } catch (Exception x) {
            return false;
        }
    }

    private boolean passesLuhn(String v) {
        int sum = 0;
        boolean a = false;
        for (int i = v.length() - 1; i >= 0; i--) {
            int d = v.charAt(i) - '0';
            if (a && (d *= 2) > 9)
                d -= 9;
            sum += d;
            a = !a;
        }
        return sum % 10 == 0;
    }

    private BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
