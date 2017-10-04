package com.github.shk0da.controller;

import com.github.shk0da.domain.Tick;
import com.github.shk0da.predictor.NeuralNetworkStockPredictor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private NeuralNetworkStockPredictor neuralNetworkStockPredictor;

    @PostMapping("/add-tick")
    @Transactional
    public void addTick(@RequestBody Tick tick) {
        neuralNetworkStockPredictor.addTick(tick);
    }

    @GetMapping(value = "/prediction", params = {"symbol"})
    @Transactional(readOnly = true)
    public ResponseEntity<Double> prediction(String symbol) {
        return new ResponseEntity<>(neuralNetworkStockPredictor.getPrediction(symbol), HttpStatus.OK);
    }
}
