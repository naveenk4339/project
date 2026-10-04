package com.pos.ai.forecast;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final DemandForecaster forecaster;

    public ForecastController(DemandForecaster forecaster) {
        this.forecaster = forecaster;
    }

    @GetMapping("/{sku}")
    public DemandForecaster.Forecast forecast(@PathVariable String sku, @RequestParam(defaultValue = "7") int days) {
        return forecaster.forecast(sku, Math.max(1, Math.min(days, 90)));
    }
}
