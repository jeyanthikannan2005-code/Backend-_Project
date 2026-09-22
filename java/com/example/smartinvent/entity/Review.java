package com.example.smartinvent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productName;
    private String invoiceNumber;
    private String reviewer;
    private Long customerId;
    private String customerUsername;
    private String customerDisplayName;
    private Integer rating;

    @Column(length = 1000)
    private String comment;

    // JSON array of up to five compressed data URLs. Kept in the review row so
    // the public product page and staff review page show the same submission.
    @Lob
    @Column(columnDefinition = "TEXT")
    private String photosJson;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long value) {
        productId = value;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String value) {
        productName = value;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String value) {
        invoiceNumber = value;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String value) {
        reviewer = value;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long value) {
        customerId = value;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String value) {
        customerUsername = value;
    }

    public String getCustomerDisplayName() {
        return customerDisplayName;
    }

    public void setCustomerDisplayName(String value) {
        customerDisplayName = value;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer value) {
        rating = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        comment = value;
    }

    public String getPhotosJson() {
        return photosJson;
    }

    public void setPhotosJson(String value) {
        photosJson = value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
