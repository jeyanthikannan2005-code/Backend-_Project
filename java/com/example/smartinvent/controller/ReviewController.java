package com.example.smartinvent.controller;

import com.example.smartinvent.entity.Review;
import com.example.smartinvent.entity.Sale;
import com.example.smartinvent.entity.User;
import com.example.smartinvent.repository.ReviewRepository;
import com.example.smartinvent.repository.SaleRepository;
import com.example.smartinvent.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewRepository reviewRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReviewController(ReviewRepository reviewRepository, SaleRepository saleRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Review>> getAllReviews() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(reviewRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/product/{productId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Review>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId));
    }

    @GetMapping("/eligible")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEligibleProducts(@RequestParam(required = false) String invoice,
            Authentication authentication) {
        requireCustomer(authentication);
        String customer = authentication.getName();
        User currentUser = userRepository.findByUsername(customer)
                .orElseThrow(() -> new RuntimeException("Authenticated customer was not found"));
        // Match by the customer's numeric id (the durable identity), not just the
        // username string, so this stays correct even if a review row was ever
        // saved with a slightly different reviewer string.
        Set<String> reviewedOrders = reviewRepository.findAll().stream()
                .filter(review -> currentUser.getId().equals(review.getCustomerId())
                        || customer.equals(review.getReviewer()))
                .map(review -> review.getInvoiceNumber() + ":" + review.getProductId())
                .collect(Collectors.toSet());

        Map<String, Map<String, Object>> products = new LinkedHashMap<>();
        for (Sale sale : saleRepository.findAll()) {
            if (!customer.equals(sale.getSoldBy()) || (invoice != null && !invoice.equals(sale.getInvoiceNumber()))
                    || !"ONLINE".equalsIgnoreCase(sale.getSalesChannel())
                    || !"DELIVERED".equalsIgnoreCase(sale.getOrderStatus())
                    || sale.getProduct() == null)
                continue;
            String key = sale.getInvoiceNumber() + ":" + sale.getProduct().getId();
            if (reviewedOrders.contains(key))
                continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("invoiceNumber", sale.getInvoiceNumber());
            item.put("productId", sale.getProduct().getId());
            item.put("productName", sale.getProduct().getName());
            item.put("deliveredDate", sale.getDeliveredDate());
            products.put(key, item);
        }
        return new ArrayList<>(products.values());
    }

    @PostMapping
    @Transactional
    public Review addReview(@RequestBody Review review, Authentication authentication) {
        requireCustomer(authentication);
        if (review.getProductId() == null || review.getComment() == null || review.getComment().isBlank()) {
            throw new RuntimeException("Choose a product and write a review");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }
        validatePhotos(review.getPhotosJson());

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated customer was not found"));
        Sale deliveredSale = saleRepository.findAll().stream()
                .filter(sale -> username.equals(sale.getSoldBy()))
                .filter(sale -> "ONLINE".equalsIgnoreCase(sale.getSalesChannel()))
                .filter(sale -> "DELIVERED".equalsIgnoreCase(sale.getOrderStatus()))
                .filter(sale -> sale.getProduct() != null && review.getProductId().equals(sale.getProduct().getId()))
                .filter(sale -> review.getInvoiceNumber() != null
                        && review.getInvoiceNumber().equals(sale.getInvoiceNumber()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Reviews are available only for your delivered online order and matching product"));

        // Match on the customer's numeric id (durable identity) rather than only
        // the username string, so a duplicate review is always caught correctly.
        boolean alreadyReviewed = reviewRepository.findAll().stream()
                .anyMatch(oldReview -> (user.getId().equals(oldReview.getCustomerId())
                        || username.equals(oldReview.getReviewer()))
                        && Objects.equals(oldReview.getProductId(), deliveredSale.getProduct().getId())
                        && Objects.equals(oldReview.getInvoiceNumber(), deliveredSale.getInvoiceNumber()));
        if (alreadyReviewed)
            throw new RuntimeException("You already reviewed this product for this order");

        // Never trust the productId/invoiceNumber the browser sent for what actually
        // gets persisted - always re-derive them from the verified, delivered Sale
        // row so a review can never end up saved against the wrong product or order.
        review.setProductId(deliveredSale.getProduct().getId());
        review.setProductName(deliveredSale.getProduct().getName());
        review.setInvoiceNumber(deliveredSale.getInvoiceNumber());
        review.setReviewer(username);
        review.setCustomerId(user.getId());
        review.setCustomerUsername(user.getUsername());
        review.setCustomerDisplayName(
                user.getName() == null || user.getName().isBlank() ? user.getUsername() : user.getName());
        Review saved = reviewRepository.saveAndFlush(review);
        // Return the row exactly as it now sits in the database (not just the
        // in-memory object) so the frontend "review saved" response and the
        // subsequent GET calls can never disagree.
        return reviewRepository.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Review was not saved correctly"));
    }

    private void requireCustomer(Authentication authentication) {
        if (authentication == null
                || authentication.getAuthorities().stream().noneMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()))) {
            throw new RuntimeException("Only an authenticated customer can access customer reviews");
        }
    }

    private void validatePhotos(String photosJson) {
        if (photosJson == null || photosJson.isBlank())
            return;
        try {
            List<String> photos = objectMapper.readValue(photosJson, new TypeReference<List<String>>() {
            });
            if (photos.size() > 5)
                throw new RuntimeException("You can upload up to 5 photos per review");
            for (String photo : photos) {
                if (photo == null || !photo.startsWith("data:image/") || photo.length() > 700_000) {
                    throw new RuntimeException("Each review photo must be a valid image under 500 KB");
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Review photos could not be read");
        }
    }
}
