package com.siller.rohlik.store.order.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateNewOrderError {
    private String productId;
    private Code errorCode;

    public enum Code {
        INVALID_PRODUCT,
        MISSING_QUANTITY, UNFINISHED_PRODUCT, NOT_ENOUGH_PRODUCTS_ON_STOCK
    }

}
