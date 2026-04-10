package com.group09.ComicReader.model;

public class IapVerifyRequest {
    private final String store;
    private final String purchaseToken;
    private final Long packageId;
    private final String productId;
    private final String orderId;

    public IapVerifyRequest(String store, String purchaseToken, Long packageId, String productId, String orderId) {
        this.store = store;
        this.purchaseToken = purchaseToken;
        this.packageId = packageId;
        this.productId = productId;
        this.orderId = orderId;
    }

    public String getStore() {
        return store;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public Long getPackageId() {
        return packageId;
    }

    public String getProductId() {
        return productId;
    }

    public String getOrderId() {
        return orderId;
    }
}
