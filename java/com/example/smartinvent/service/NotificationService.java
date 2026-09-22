package com.example.smartinvent.service;

import com.example.smartinvent.entity.Product;
import com.example.smartinvent.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import com.example.smartinvent.util.DateFilterUtil;

// Rather than storing every notification permanently in the database, we
// GENERATE the current list live from the actual product data every time
// notifications.html asks for it - this guarantees notifications are always
// accurate and never go stale.
@Service
public class NotificationService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ExpiryService expiryService;

    public List<Map<String, String>> getNotifications() {
        return getNotifications(null, null, null);
    }

    public List<Map<String, String>> getNotifications(String period, String from, String to) {
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        List<Product> products = productRepository.findAll();
        List<Map<String, String>> notifications = new ArrayList<>();

        for (Product p : products) {
            String stockStatus = expiryService.stockStatus(p);
            String expiryStatus = expiryService.expiryStatus(p);

            if ("LOW_STOCK".equals(stockStatus)) {
                notifications.add(note("LOW_STOCK", p.getName() + " stock is below reorder level."));
            }
            if ("EXPIRED".equals(expiryStatus)) {
                notifications.add(note("EXPIRED", p.getName() + " has expired."));
            } else if ("NEAR_EXPIRY".equals(expiryStatus)) {
                long days = expiryService.remainingDays(p);
                notifications.add(note("NEAR_EXPIRY", p.getName() + " expires in " + days + " day(s)."));
            }
        }

        products.stream()
                .sorted(Comparator.comparingInt(Product::getTotalSold).reversed())
                .limit(3)
                .forEach(p -> notifications.add(note("HIGH_DEMAND",
                        p.getName() + " is one of the highest-selling products.")));

        return notifications.stream()
                .filter(note -> matchesDate(note.get("date"), period, fromDate, toDate))
                .toList();
    }

    public List<Map<String, String>> getNotifications(String period, String from, String to, String action) {
        return getNotifications(period, from, to).stream()
                .filter(note -> action == null || action.isBlank() || "ALL".equalsIgnoreCase(action)
                        || ("STOCK".equalsIgnoreCase(action) && "LOW_STOCK".equals(note.get("type")))
                        || ("EXPIRY".equalsIgnoreCase(action)
                                && ("EXPIRED".equals(note.get("type")) || "NEAR_EXPIRY".equals(note.get("type"))))
                        || ("SALES".equalsIgnoreCase(action) && "HIGH_DEMAND".equals(note.get("type"))))
                .toList();
    }

    private boolean matchesDate(String value, String period, LocalDate from, LocalDate to) {
        LocalDate date = LocalDate.parse(value.substring(0, 10));
        return DateFilterUtil.matches(date, period)
                && (from == null || !date.isBefore(from))
                && (to == null || !date.isAfter(to));
    }

    private Map<String, String> note(String type, String message) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("message", message);
        m.put("date", LocalDateTime.now().toString());
        return m;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new RuntimeException("Date filters must use YYYY-MM-DD format");
        }
    }
}
