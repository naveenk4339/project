package com.pos.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockRepository extends JpaRepository<StockItem, String> {

    @Query("select s from StockItem s where s.onHand <= s.reorderPoint order by s.onHand")
    List<StockItem> findLowStock();
}
