import java.util.Arrays;
import java.util.Random;

public class MLPLogicDemo {

    public static void main(String[] args) {
        // XOR
        double[][] xorInputs = {
                {0, 0},
                {0, 1},
                {1, 0},
                {1, 1}
        };
        double[][] xorTargets = {
                {0},
                {1},
                {1},
                {0}
        };

        // AND
        double[][] andTargets = {
                {0},
                {0},
                {0},
                {1}
        };

        // OR
        double[][] orTargets = {
                {0},
                {1},
                {1},
                {1}
        };

        // NOT
        double[][] notInputs = {
                {0},
                {1}
        };
        double[][] notTargets = {
                {1},
                {0}
        };

        System.out.println("=== Обучение XOR ===");
        MLP xorNet = new MLP(2, 4, 1, 0.5);
        xorNet.train(xorInputs, xorTargets, 10000);
        testNetwork(xorNet, xorInputs, xorTargets);

        System.out.println("\n=== Обучение AND ===");
        MLP andNet = new MLP(2, 3, 1, 0.5);
        andNet.train(xorInputs, andTargets, 5000);
        testNetwork(andNet, xorInputs, andTargets);

        System.out.println("\n=== Обучение OR ===");
        MLP orNet = new MLP(2, 3, 1, 0.5);
        orNet.train(xorInputs, orTargets, 5000);
        testNetwork(orNet, xorInputs, orTargets);

        System.out.println("\n=== Обучение NOT ===");
        MLP notNet = new MLP(1, 2, 1, 0.5);
        notNet.train(notInputs, notTargets, 5000);
        testNetwork(notNet, notInputs, notTargets);
    }

    private static void testNetwork(MLP net, double[][] inputs, double[][] targets) {
        for (int i = 0; i < inputs.length; i++) {
            double[] output = net.predict(inputs[i]);
            int predicted = output[0] >= 0.5 ? 1 : 0;
            int expected = (int) targets[i][0];

            System.out.printf(
                    "Вход: %s -> выход: %.5f -> класс: %d, ожидается: %d%n",
                    Arrays.toString(inputs[i]),
                    output[0],
                    predicted,
                    expected
            );
        }
    }
}

class MLP {
    private final int inputSize;
    private final int hiddenSize;
    private final int outputSize;
    private final double learningRate;

    private final double[][] weightsInputHidden;
    private final double[] biasHidden;

    private final double[][] weightsHiddenOutput;
    private final double[] biasOutput;

    private final Random random = new Random(42);

    public MLP(int inputSize, int hiddenSize, int outputSize, double learningRate) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;
        this.learningRate = learningRate;

        this.weightsInputHidden = new double[inputSize][hiddenSize];
        this.biasHidden = new double[hiddenSize];

        this.weightsHiddenOutput = new double[hiddenSize][outputSize];
        this.biasOutput = new double[outputSize];

        initWeights();
    }

    private void initWeights() {
        for (int i = 0; i < inputSize; i++) {
            for (int h = 0; h < hiddenSize; h++) {
                weightsInputHidden[i][h] = random.nextDouble() * 2 - 1;
            }
        }

        for (int h = 0; h < hiddenSize; h++) {
            biasHidden[h] = random.nextDouble() * 2 - 1;
        }

        for (int h = 0; h < hiddenSize; h++) {
            for (int o = 0; o < outputSize; o++) {
                weightsHiddenOutput[h][o] = random.nextDouble() * 2 - 1;
            }
        }

        for (int o = 0; o < outputSize; o++) {
            biasOutput[o] = random.nextDouble() * 2 - 1;
        }
    }

    public void train(double[][] inputs, double[][] targets, int epochs) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalError = 0.0;

            for (int sample = 0; sample < inputs.length; sample++) {
                double[] x = inputs[sample];
                double[] y = targets[sample];

                // ===== Прямой проход =====
                double[] hidden = new double[hiddenSize];
                for (int h = 0; h < hiddenSize; h++) {
                    double sum = biasHidden[h];
                    for (int i = 0; i < inputSize; i++) {
                        sum += x[i] * weightsInputHidden[i][h];
                    }
                    hidden[h] = sigmoid(sum);
                }

                double[] output = new double[outputSize];
                for (int o = 0; o < outputSize; o++) {
                    double sum = biasOutput[o];
                    for (int h = 0; h < hiddenSize; h++) {
                        sum += hidden[h] * weightsHiddenOutput[h][o];
                    }
                    output[o] = sigmoid(sum);
                }

                // ===== Ошибка выходного слоя =====
                double[] outputDelta = new double[outputSize];
                for (int o = 0; o < outputSize; o++) {
                    double error = y[o] - output[o];
                    totalError += 0.5 * error * error;
                    outputDelta[o] = error * sigmoidDerivative(output[o]);
                }

                // ===== Ошибка скрытого слоя =====
                double[] hiddenDelta = new double[hiddenSize];
                for (int h = 0; h < hiddenSize; h++) {
                    double error = 0.0;
                    for (int o = 0; o < outputSize; o++) {
                        error += outputDelta[o] * weightsHiddenOutput[h][o];
                    }
                    hiddenDelta[h] = error * sigmoidDerivative(hidden[h]);
                }

                // ===== Обновление весов hidden -> output =====
                for (int h = 0; h < hiddenSize; h++) {
                    for (int o = 0; o < outputSize; o++) {
                        weightsHiddenOutput[h][o] += learningRate * outputDelta[o] * hidden[h];
                    }
                }

                for (int o = 0; o < outputSize; o++) {
                    biasOutput[o] += learningRate * outputDelta[o];
                }

                // ===== Обновление весов input -> hidden =====
                for (int i = 0; i < inputSize; i++) {
                    for (int h = 0; h < hiddenSize; h++) {
                        weightsInputHidden[i][h] += learningRate * hiddenDelta[h] * x[i];
                    }
                }

                for (int h = 0; h < hiddenSize; h++) {
                    biasHidden[h] += learningRate * hiddenDelta[h];
                }
            }

            if ((epoch + 1) % 1000 == 0 || totalError < 0.01) {
                System.out.printf("Эпоха %d, ошибка = %.6f%n", epoch + 1, totalError);
            }

            if (totalError < 0.01) {
                break;
            }
        }
    }

    public double[] predict(double[] input) {
        double[] hidden = new double[hiddenSize];
        for (int h = 0; h < hiddenSize; h++) {
            double sum = biasHidden[h];
            for (int i = 0; i < inputSize; i++) {
                sum += input[i] * weightsInputHidden[i][h];
            }
            hidden[h] = sigmoid(sum);
        }

        double[] output = new double[outputSize];
        for (int o = 0; o < outputSize; o++) {
            double sum = biasOutput[o];
            for (int h = 0; h < hiddenSize; h++) {
                sum += hidden[h] * weightsHiddenOutput[h][o];
            }
            output[o] = sigmoid(sum);
        }
        return output;
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private double sigmoidDerivative(double y) {
        return y * (1.0 - y);
    }
}
