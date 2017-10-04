package com.github.shk0da.predictor;

import com.github.shk0da.domain.Tick;
import com.github.shk0da.repository.TickRepository;
import lombok.extern.java.Log;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.learning.SupervisedLearning;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;

@Log
@Service
public class NeuralNetworkStockPredictor {

    @Autowired
    private TickRepository tickRepository;

    private static final long MONTH_MLS = 2629746000L;
    private static final int SLIDING_WINDOWS_SIZE = 5;

    public void addTick(Tick tick) {
        tick.setNormalization(normalizeValue(tick));
        tickRepository.save(tick);
        trainNetwork(tick.getSymbol());
    }

    public Double getPrediction(String symbol) {
        NeuralNetwork neuralNetwork = getNeuralNetwork(symbol);
        neuralNetwork.calculate();
        double[] networkOutput = neuralNetwork.getOutput();
        return deNormalizeValue(networkOutput[0], getCloseMin(symbol), getCloseMax(symbol));
    }

    private NeuralNetwork getNeuralNetwork(String symbol) {
        String path = getNeuralNetworkModelFilePath(symbol);
        File neuralNetworkFile = new File(path);
        if (neuralNetworkFile.exists() && !neuralNetworkFile.isDirectory()) {
            return NeuralNetwork.createFromFile(getNeuralNetworkModelFilePath(symbol));
        }

        return new MultiLayerPerceptron(SLIDING_WINDOWS_SIZE, 2 * SLIDING_WINDOWS_SIZE + 1, 1);
    }

    private String getNeuralNetworkModelFilePath(String symbol) {
        return "stockPredictor-" + symbol + ".nnet";
    }

    private Timestamp getStart() {
        return new Timestamp(System.currentTimeMillis() - MONTH_MLS);
    }

    private Timestamp getEnd() {
        return new Timestamp(System.currentTimeMillis());
    }

    private Double getOpenMax(String symbol) {
        Tick tick = tickRepository.findTickBySymbolAndDatetimeBetweenOrderByOpenDesc(symbol, getStart(), getEnd());
        return tick == null ? 0D : tick.getOpen();
    }

    private Double getOpenMin(String symbol) {
        Tick tick = tickRepository.findTickBySymbolAndDatetimeBetweenOrderByOpenAsc(symbol, getStart(), getEnd());
        return tick == null ? 0D : tick.getOpen();
    }

    private Double getCloseMax(String symbol) {
        Tick tick = tickRepository.findTickBySymbolAndDatetimeBetweenOrderByCloseDesc(symbol, getStart(), getEnd());
        return tick == null ? 0D : tick.getClose();
    }

    private Double getCloseMin(String symbol) {
        Tick tick = tickRepository.findTickBySymbolAndDatetimeBetweenOrderByCloseAsc(symbol, getStart(), getEnd());
        return tick == null ? 0D : tick.getClose();
    }

    private double normalizeValue(Tick tick) {
        return normalizeValue(tick.getClose(), getCloseMin(tick.getSymbol()), getCloseMax(tick.getSymbol()));
    }

    private double normalizeValue(double input, double min, double max) {
        return (input - min) / (max - min) * 0.8 + 0.1;
    }

    private double deNormalizeValue(Tick tick) {
        return deNormalizeValue(tick.getClose(), getCloseMin(tick.getSymbol()), getCloseMax(tick.getSymbol()));
    }

    private double deNormalizeValue(double input, double min, double max) {
        return min + (input - 0.1) * (max - min) / 0.8;
    }

    public void trainNetwork(String symbol) {
        log.info(symbol + ": Training starting");
        int maxIterations = 1000;
        double learningRate = 0.5;
        double maxError = 0.00001;

        NeuralNetwork<BackPropagation> neuralNetwork = getNeuralNetwork(symbol);
        SupervisedLearning learningRule = neuralNetwork.getLearningRule();
        learningRule.setMaxError(maxError);
        learningRule.setLearningRate(learningRate);
        learningRule.setMaxIterations(maxIterations);
        DataSet trainingSet = loadTrainingData(symbol);
        neuralNetwork.learn(trainingSet);
        neuralNetwork.save(getNeuralNetworkModelFilePath(symbol));
    }

    private DataSet loadTrainingData(String symbol) {
        DataSet trainingSet = new DataSet(SLIDING_WINDOWS_SIZE, 1);
        List<Tick> ticks = tickRepository
                .findAllBySymbolAndDatetimeIsBetweenOrderByDatetimeAsc(symbol, getStart(), getEnd());

        int lineSize = 5;
        int linesSize = ticks.size() / lineSize;
        Double[][] tickLines = new Double[linesSize][lineSize];
        int x = 1;
        int y = 1;
        for (Tick tick : ticks) {
            tickLines[x][y++] = tick.getClose();
            if (y > 5) {
                x++;
                y = 1;
            }
        }

        for (int i = 1; i <= linesSize; i++) {
            Double[] tokens = tickLines[i];
            double trainValues[] = new double[SLIDING_WINDOWS_SIZE];
            for (int j = 0; j < SLIDING_WINDOWS_SIZE; j++) {
                trainValues[j] = tokens[j];
            }
            double expectedValue[] = new double[]{tokens[SLIDING_WINDOWS_SIZE]};
            trainingSet.addRow(new DataSetRow(trainValues, expectedValue));
        }

        return trainingSet;
    }
}