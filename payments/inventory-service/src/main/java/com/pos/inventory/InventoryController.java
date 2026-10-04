package com.pos.inventory;

import com.pos.common.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final StockRepository stock;

    public InventoryController(StockRepository stock) {
        this.stock = stock;
    }

    @GetMapping
    public List<StockView> all() {
        return stock.findAll().stream().map(StockView::of).toList();
    }

    @GetMapping("/low-stock")
    public List<StockView> lowStock() {
        return stock.findLowStock().stream().map(StockView::of).toList();
    }

    @GetMapping("/{sku}")
    public ResponseEntity<?> one(@PathVariable String sku) {
        return stock.findById(sku).<ResponseEntity<?>>map(s -> ResponseEntity.ok(StockView.of(s)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("UNKNOWN_SKU", sku)));
    }

    /** Manual adjustment, e.g. receiving a delivery (+) or recording shrink (-). */
    @PostMapping("/{sku}/adjust")
    @Transactional
    public StockView adjust(@PathVariable String sku, @RequestBody Adjustment adjustment) {
        StockItem item = stock.findById(sku).orElseGet(() -> new StockItem(sku, 0, 0));
        item.adjust(adjustment.delta());
        return StockView.of(stock.save(item));
    }

    public record Adjustment(int delta, String reason) {
    }

    public record StockView(String sku, int onHand, int reorderPoint, boolean lowStock) {
        static StockView of(StockItem s) {
            return new StockView(s.getSku(), s.getOnHand(), s.getReorderPoint(), s.belowReorderPoint());
        }
    }
}
