package com.group09.ComicReader.wallet.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public class IapVerifyRequest {

    private String store;         // GOOGLE or APPLE
    @NotBlank(message = "purchaseToken is required")
    private String purchaseToken; // token from the store
    private Long packageId;       // top_up_packages.id
    private String productId;     // product identifier (e.g. "coins_500")
    private String orderId;       // order / transaction id from store

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public String getPurchaseToken() { return purchaseToken; }
    public void setPurchaseToken(String purchaseToken) { this.purchaseToken = purchaseToken; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @AssertTrue(message = "packageId or productId is required")
    public boolean isPackageHintProvided() {
        return packageId != null || (productId != null && !productId.isBlank());
    }
}
