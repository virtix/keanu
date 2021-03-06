package io.improbable.keanu.e2e.regression;

import io.improbable.keanu.DeterministicRule;
import io.improbable.keanu.algorithms.variational.GradientOptimizer;
import io.improbable.keanu.network.BayesianNetwork;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.ConstantVertex;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.KeanuRandom;
import io.improbable.keanu.vertices.dbl.probabilistic.GaussianVertex;
import io.improbable.keanu.vertices.dbl.probabilistic.UniformVertex;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class LinearRegression {
    private final Logger log = LoggerFactory.getLogger(LinearRegression.class);

    @Rule
    public DeterministicRule deterministicRule = new DeterministicRule();

    private KeanuRandom random;

    @Before
    public void setup() {
        random = new KeanuRandom(1);
    }

    @Test
    public void linearRegression1FactorTensorVariationalMAP() {

        // Generate data
        int N = 100000;
        double expectedM = 3.0;
        double expectedB = 20.0;

        DoubleVertex xGenerator = new UniformVertex(new int[]{1, N}, 0, 10);
        DoubleVertex mu = xGenerator.multiply(expectedM).plus(expectedB);
        DoubleVertex yGenerator = new GaussianVertex(mu, 1.0);
        DoubleTensor xData = xGenerator.sample(random);
        xGenerator.setAndCascade(xData);
        DoubleTensor yData = yGenerator.sample(random);

        // Linear Regression
        DoubleVertex m = new GaussianVertex(0.0, 10.0);
        DoubleVertex b = new GaussianVertex(0.0, 10.0);
        DoubleVertex x = ConstantVertex.of(xData);
        DoubleVertex y = new GaussianVertex(x.multiply(m).plus(b), 5.0);
        y.observe(yData);

        BayesianNetwork bayesNet = new BayesianNetwork(m.getConnectedGraph());
        GradientOptimizer optimizer = new GradientOptimizer(bayesNet);

        optimizer.maxLikelihood(10000);

        log.info("M = " + m.getValue().scalar() + ", B = " + b.getValue().scalar());
        assertEquals(expectedM, m.getValue().scalar(), 0.05);
        assertEquals(expectedB, b.getValue().scalar(), 0.05);
    }

    @Test
    public void linearRegressionTwoFactorTensorVariationalMAP() {

        // Generate data
        int N = 100000;
        double expectedW1 = 3.0;
        double expectedW2 = 7.0;
        double expectedB = 20.0;

        DoubleVertex x1Generator = new UniformVertex(new int[]{1, N}, 0, 10);
        DoubleVertex x2Generator = new UniformVertex(new int[]{1, N}, 50, 100);
        DoubleVertex yGenerator = new GaussianVertex(
            x1Generator.multiply(expectedW1).plus(x2Generator.multiply(expectedW2)).plus(expectedB),
            1.0
        );
        DoubleTensor x1Data = x1Generator.sample(random);
        x1Generator.setAndCascade(x1Data);
        DoubleTensor x2Data = x1Generator.sample(random);
        x2Generator.setAndCascade(x2Data);
        DoubleTensor yData = yGenerator.sample(random);

        // Linear Regression
        DoubleVertex w1 = new GaussianVertex(0.0, 10.0);
        DoubleVertex w2 = new GaussianVertex(0.0, 10.0);
        DoubleVertex b = new GaussianVertex(0.0, 10.0);
        DoubleVertex x1 = ConstantVertex.of(x1Data);
        DoubleVertex x2 = ConstantVertex.of(x2Data);
        DoubleVertex y = new GaussianVertex(x1.multiply(w1).plus(x2.multiply(w2)).plus(b), 5.0);
        y.observe(yData);

        BayesianNetwork bayesNet = new BayesianNetwork(y.getConnectedGraph());
        GradientOptimizer optimizer = new GradientOptimizer(bayesNet);

        optimizer.maxLikelihood(10000);

        log.info("W1 = " + w1.getValue().scalar() + " W2 = " + w2.getValue().scalar() + ", B = " + b.getValue().scalar());
        assertEquals(expectedW1, w1.getValue().scalar(), 0.05);
        assertEquals(expectedW2, w2.getValue().scalar(), 0.05);
        assertEquals(expectedB, b.getValue().scalar(), 0.05);
    }

}