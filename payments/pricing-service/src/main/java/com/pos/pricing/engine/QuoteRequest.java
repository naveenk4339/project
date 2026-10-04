package com.pos.pricing.engine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuoteRequest(@NotBlank String storeId, @NotEmpty List<@Valid Item> items) {

    public record Item(@NotBlank String sku, @Min(1) int quantity) {
    }
}
