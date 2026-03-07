package com.group09.ComicReader.wallet.dto;

public class IapVerifyRequest {

    private String store;         // GOOGLE or APPLE
    private String purchaseToken; // token from the store
    private String productId;     // product identifier (e.g. "coins_500")
    private String orderId;       // order / transaction id from store

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public String getPurchaseToken() { return purchaseToken; }
    public void setPurchaseToken(String purchaseToken) { this.purchaseToken = purchaseToken; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
