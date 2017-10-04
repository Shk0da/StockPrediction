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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Log
@Service
public class NeuralNetworkStockPredictor {

    @Autowired
    private TickRepository tickRepository;

    private static final long MIN_MLS = 600000;
    private static final long MONTH_MLS = 2629746000L;
    private static final long YEAR_MLS = 31556926000L;
    private static final int SLIDING_WINDOWS_SIZE = 5;

    private static Map<String, Map<String, Double>> minMax = new ConcurrentHashMap<>();
    private static Map<String, Long> locker = new ConcurrentHashMap<>();

    public void addTick(Tick tick) {
        Double normalize = normalizeValue(tick);
        if (normalize > 0 && normalize < Double.MAX_VALUE
                && normalize != Double.NaN
                && normalize != Double.POSITIVE_INFINITY && normalize != Double.NEGATIVE_INFINITY) {
            tick.setNormalization(normalize);
            tickRepository.save(tick);
        }
    }

    public Double getPrediction(String symbol) {
        trainNetwork(symbol);
        NeuralNetwork neuralNetwork = getNeuralNetwork(symbol);
        neuralNetwork.calculate();
        double[] networkOutput = neuralNetwork.getOutput();
        Map<String, Double> minmax = minMax.getOrDefault(symbol, null);
        return (minmax == null)
                ? 0.0
                : deNormalizeValue(networkOutput[0], minMax.get(symbol).get("min"), minMax.get(symbol).get("max"));
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
        return new Timestamp(System.currentTimeMillis() - 2 * YEAR_MLS);
    }

    private Timestamp getEnd() {
        return new Timestamp(System.currentTimeMillis());
    }

    private Double getOpenMax(String symbol) {
        Tick tick = tickRepository.findFirstBySymbolAndDatetimeBetweenOrderByOpenDesc(symbol, getStart(), getEnd());
        return tick == null ? 0D : tick.getOpen();
    }

    private Double getOpenMin(String symbol) {
        Tick tick = tickRepository.findFirstBySymbolAndDatetimeBetweenOrderByOpenAsc(symbol, getStart(), getEnd());
        return tick == null ? 0D : tick.getOpen();
    }

    private Double getCloseMax(String symbol) {
        Tick tick = tickRepository.findFirstBySymbolAndDatetimeBetweenOrderByCloseDesc(symbol, getStart(), getEnd());
        return tick == null ? 0D : tick.getClose();
    }

    private Double getCloseMin(String symbol) {
        Tick tick = tickRepository.findFirstBySymbolAndDatetimeBetweenOrderByCloseAsc(symbol, getStart(), getEnd());
        return tick == null ? 0D : tick.getClose();
    }

    private double normalizeValue(Tick tick) {
        Double min = getCloseMin(tick.getSymbol());
        Double max = getCloseMax(tick.getSymbol());
        minMax.put(tick.getSymbol(), new HashMap<String, Double>() {
            {
                put("min", (min > 0 && !max.equals(min)) ? min : tick.getMin());
                put("max", (max > 0 && !max.equals(min)) ? max : tick.getMax());
            }
        });
        return normalizeValue(tick.getClose(), minMax.get(tick.getSymbol()).get("min"),
                minMax.get(tick.getSymbol()).get("max"));
    }

    private double normalizeValue(double input, double min, double max) {
        return (input - min) / (max - min) * 0.8 + 0.1;
    }

    private double deNormalizeValue(double input, double min, double max) {
        return min + (input - 0.1) * (max - min) / 0.8;
    }

    @Async
    protected void trainNetwork(String symbol) {
        if (locker.containsKey(symbol)) {
            if (locker.get(symbol) < (System.currentTimeMillis() - MIN_MLS)) {
                locker.remove(symbol);
            }
            return;
        }
        locker.put(symbol, System.currentTimeMillis());

        int maxIterations = 1000;
        double learningRate = 0.5;
        double maxError = 0.00001;

        NeuralNetwork<BackPropagation> neuralNetwork =
                new MultiLayerPerceptron(SLIDING_WINDOWS_SIZE, 2 * SLIDING_WINDOWS_SIZE + 1, 1);
        SupervisedLearning learningRule = neuralNetwork.getLearningRule();
        learningRule.setMaxError(maxError);
        learningRule.setLearningRate(learningRate);
        learningRule.setMaxIterations(maxIterations);
        DataSet trainingSet = loadTrainingData(symbol);
        if (trainingSet == null) return;
        neuralNetwork.learn(trainingSet);
        neuralNetwork.save(getNeuralNetworkModelFilePath(symbol));
        locker.remove(symbol);
    }

    private DataSet loadTrainingData(String symbol) {
        DataSet trainingSet = new DataSet(SLIDING_WINDOWS_SIZE, 1);
        List<Tick> ticks = tickRepository.findTop10000BySymbolOrderByDatetimeAsc(symbol);
        int lineSize = SLIDING_WINDOWS_SIZE;
        int linesSize = ticks.size() / lineSize;
        Double[][] tickLines = new Double[linesSize + 1][lineSize];
        int x = 0;
        int y = 0;
        for (Tick tick : ticks) {
            tickLines[x][y++] = tick.getNormalization();
            if (y >= SLIDING_WINDOWS_SIZE) {
                x++;
                y = 0;
            }
        }

        for (int i = 0; i <= linesSize; i++) {
            Double[] tokens = tickLines[i];
            Double[] trainValues = new Double[SLIDING_WINDOWS_SIZE];
            int expect = 0;
            double tmp = 0D;
            for (int j = 0; j < SLIDING_WINDOWS_SIZE; j++) {
                if (tokens[j] == null) {
                    trainValues[j] = tmp;
                } else {
                    trainValues[j] = tokens[j];
                    expect = j;
                    tmp = trainValues[j];
                }
            }
            double[] set = Stream.of(trainValues).mapToDouble(Double::doubleValue).toArray();
            if (tokens[expect] == null) continue;
            double[] expected = new double[]{tokens[expect]};
            trainingSet.addRow(new DataSetRow(set, expected));
        }

        return trainingSet;
    }
}