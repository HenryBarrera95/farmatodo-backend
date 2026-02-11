package com.farmatodo.product;

import org.springframework.context.ApplicationEvent;

public class ProductSearchEvent extends ApplicationEvent {

    private final int minStock;
    private final String txId;

    public ProductSearchEvent(Object source, int minStock, String txId) {
        super(source);
        this.minStock = minStock;
        this.txId = txId;
    }

    public int getMinStock() { return minStock; }
    public String getTxId() { return txId; }
}
