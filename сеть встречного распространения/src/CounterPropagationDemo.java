import java.util.Arrays;
import java.util.Random;

public class CounterPropagationDemo {

    public static void main(String[] args) {
        int[][] digitPatterns = new int[10][];
        for (int d = 0; d < 10; d++) {
            digitPatterns[d] = DigitPatterns.getDigitVector(d);
        }

        // Подготовка обучающих данных
        double[][] inputs = new double[10][100];
        int[] labels = new int[10];

        for (int i = 0; i < 10; i++) {
            labels[i] = i;
            for (int j = 0; j < 100; j++) {
                inputs[i][j] = digitPatterns[i][j];
            }
        }

        // -----------------------------------------
        // 1. Однослойный перцептрон
        // -----------------------------------------
        System.out.println("===== Обучение однослойного перцептрона =====");
        SingleLayerPerceptron perceptron = new SingleLayerPerceptron(100, 10, 0.1);
        perceptron.train(inputs, labels, 1000);

        // -----------------------------------------
        // 2. Сеть встречного распространения
        // -----------------------------------------
        System.out.println("\n===== Обучение сети встречного распространения =====");
        CounterPropagationNetwork cpn = new CounterPropagationNetwork(100, 20, 10, 0.2, 0.3);
        cpn.train(inputs, labels, 500);

        // -----------------------------------------
        // 3. Сравнение на тестах с шумом
        // -----------------------------------------
        System.out.println("\n===== Сравнение на зашумленных цифрах =====");
        Random random = new Random(42);

        int perceptronCorrect = 0;
        int cpnCorrect = 0;
        int total = 0;

        for (int digit = 0; digit < 10; digit++) {
            for (int test = 0; test < 10; test++) {
                double[] noisy = addNoise(inputs[digit], 10, random);

                int pPred = perceptron.predict(noisy);
                int cPred = cpn.predict(noisy);

                if (pPred == digit) perceptronCorrect++;
                if (cPred == digit) cpnCorrect++;
                total++;

                System.out.printf(
                        "Цифра %d | Перцептрон: %d | Встречное распространение: %d%n",
                        digit, pPred, cPred
                );
            }
        }

        System.out.println("\n===== Итоги =====");
        System.out.printf("Перцептрон: %d/%d = %.2f%%%n",
                perceptronCorrect, total, 100.0 * perceptronCorrect / total);
        System.out.printf("Сеть встречного распространения: %d/%d = %.2f%%%n",
                cpnCorrect, total, 100.0 * cpnCorrect / total);
    }

    private static double[] addNoise(double[] input, int flips, Random random) {
        double[] noisy = Arrays.copyOf(input, input.length);
        for (int i = 0; i < flips; i++) {
            int idx = random.nextInt(noisy.length);
            noisy[idx] = noisy[idx] > 0 ? -1.0 : 1.0;
        }
        return noisy;
    }
}

// --------------------------------------------------
// Однослойный перцептрон
// --------------------------------------------------
class SingleLayerPerceptron {
    private final int inputSize;
    private final int outputSize;
    private final double learningRate;
    private final double[][] weights;
    private final double[] bias;

    public SingleLayerPerceptron(int inputSize, int outputSize, double learningRate) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.learningRate = learningRate;
        this.weights = new double[outputSize][inputSize];
        this.bias = new double[outputSize];
    }

    public void train(double[][] inputs, int[] labels, int epochs) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            int errors = 0;

            for (int sample = 0; sample < inputs.length; sample++) {
                double[] x = inputs[sample];
                int label = labels[sample];

                for (int out = 0; out < outputSize; out++) {
                    int target = (out == label) ? 1 : -1;
                    int prediction = activate(net(x, out));
                    int error = target - prediction;

                    if (error != 0) {
                        errors++;
                        for (int i = 0; i < inputSize; i++) {
                            weights[out][i] += learningRate * error * x[i];
                        }
                        bias[out] += learningRate * error;
                    }
                }
            }

            if ((epoch + 1) % 100 == 0 || errors == 0) {
                System.out.printf("Эпоха %d, ошибок: %d%n", epoch + 1, errors);
            }

            if (errors == 0) {
                break;
            }
        }
    }

    public int predict(double[] input) {
        int bestClass = 0;
        double bestScore = net(input, 0);

        for (int out = 1; out < outputSize; out++) {
            double score = net(input, out);
            if (score > bestScore) {
                bestScore = score;
                bestClass = out;
            }
        }
        return bestClass;
    }

    private double net(double[] input, int neuron) {
        double sum = bias[neuron];
        for (int i = 0; i < inputSize; i++) {
            sum += weights[neuron][i] * input[i];
        }
        return sum;
    }

    private int activate(double x) {
        return x >= 0 ? 1 : -1;
    }
}

// --------------------------------------------------
// Сеть встречного распространения
// --------------------------------------------------
class CounterPropagationNetwork {
    private final int inputSize;
    private final int kohonenSize;
    private final int outputSize;

    private final double kohonenLearningRate;
    private final double grossbergLearningRate;

    private final double[][] kohonenWeights;   // [kohonen][input]
    private final double[][] grossbergWeights; // [kohonen][output]

    private final Random random = new Random(1);

    public CounterPropagationNetwork(int inputSize, int kohonenSize, int outputSize,
                                     double kohonenLearningRate, double grossbergLearningRate) {
        this.inputSize = inputSize;
        this.kohonenSize = kohonenSize;
        this.outputSize = outputSize;
        this.kohonenLearningRate = kohonenLearningRate;
        this.grossbergLearningRate = grossbergLearningRate;

        this.kohonenWeights = new double[kohonenSize][inputSize];
        this.grossbergWeights = new double[kohonenSize][outputSize];

        initWeights();
    }

    private void initWeights() {
        for (int k = 0; k < kohonenSize; k++) {
            double norm = 0;
            for (int i = 0; i < inputSize; i++) {
                kohonenWeights[k][i] = random.nextDouble() * 2 - 1;
                norm += kohonenWeights[k][i] * kohonenWeights[k][i];
            }
            norm = Math.sqrt(norm);
            for (int i = 0; i < inputSize; i++) {
                kohonenWeights[k][i] /= norm;
            }

            for (int o = 0; o < outputSize; o++) {
                grossbergWeights[k][o] = random.nextDouble() * 0.1;
            }
        }
    }

    public void train(double[][] inputs, int[] labels, int epochs) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            for (int sample = 0; sample < inputs.length; sample++) {
                double[] x = normalize(inputs[sample]);
                int label = labels[sample];

                int winner = findWinner(x);

                // Обучение Кохонена
                for (int i = 0; i < inputSize; i++) {
                    kohonenWeights[winner][i] += kohonenLearningRate * (x[i] - kohonenWeights[winner][i]);
                }
                normalizeInPlace(kohonenWeights[winner]);

                // Обучение Гроссберга
                for (int o = 0; o < outputSize; o++) {
                    double target = (o == label) ? 1.0 : 0.0;
                    grossbergWeights[winner][o] += grossbergLearningRate * (target - grossbergWeights[winner][o]);
                }
            }

            if ((epoch + 1) % 100 == 0 || epoch == epochs - 1) {
                System.out.printf("Эпоха %d завершена%n", epoch + 1);
            }
        }
    }

    public int predict(double[] input) {
        double[] x = normalize(input);
        int winner = findWinner(x);

        int bestClass = 0;
        double bestValue = grossbergWeights[winner][0];

        for (int o = 1; o < outputSize; o++) {
            if (grossbergWeights[winner][o] > bestValue) {
                bestValue = grossbergWeights[winner][o];
                bestClass = o;
            }
        }

        return bestClass;
    }

    private int findWinner(double[] input) {
        int winner = 0;
        double bestScore = dot(input, kohonenWeights[0]);

        for (int k = 1; k < kohonenSize; k++) {
            double score = dot(input, kohonenWeights[k]);
            if (score > bestScore) {
                bestScore = score;
                winner = k;
            }
        }
        return winner;
    }

    private double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private double[] normalize(double[] x) {
        double[] result = Arrays.copyOf(x, x.length);
        normalizeInPlace(result);
        return result;
    }

    private void normalizeInPlace(double[] x) {
        double norm = 0;
        for (double v : x) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) return;
        for (int i = 0; i < x.length; i++) {
            x[i] /= norm;
        }
    }
}

// --------------------------------------------------
// Эталонные цифры 10x10
// --------------------------------------------------
class DigitPatterns {

    private static final String[][] DIGITS = {
        { // 0
            "0011111100",
            "0110000110",
            "1100000011",
            "1100000011",
            "1100000011",
            "1100000011",
            "1100000011",
            "1100000011",
            "0110000110",
            "0011111100"
        },
        { // 1
            "0001100000",
            "0011100000",
            "0111100000",
            "0001100000",
            "0001100000",
            "0001100000",
            "0001100000",
            "0001100000",
            "0001100000",
            "1111111111"
        },
        { // 2
            "0011111100",
            "0110000110",
            "1100000011",
            "0000000011",
            "0000000110",
            "0000011100",
            "0001110000",
            "0111000000",
            "1100000000",
            "1111111111"
        },
        { // 3
            "0011111100",
            "0110000110",
            "1100000011",
            "0000000011",
            "0001111110",
            "0001111110",
            "0000000011",
            "1100000011",
            "0110000110",
            "0011111100"
        },
        { // 4
            "0000011100",
            "0000111100",
            "0001101100",
            "0011001100",
            "0110001100",
            "1100001100",
            "1111111111",
            "0000001100",
            "0000001100",
            "0000001100"
        },
        { // 5
            "1111111111",
            "1100000000",
            "1100000000",
            "1111111100",
            "0000000110",
            "0000000011",
            "0000000011",
            "1100000011",
            "0110000110",
            "0011111100"
        },
        { // 6
            "0011111100",
            "0110000110",
            "1100000000",
            "1100000000",
            "1111111100",
            "1100000110",
            "1100000011",
            "1100000011",
            "0110000110",
            "0011111100"
        },
        { // 7
            "1111111111",
            "0000000011",
            "0000000110",
            "0000001100",
            "0000011000",
            "0000110000",
            "0001100000",
            "0011000000",
            "0011000000",
            "0011000000"
        },
        { // 8
            "0011111100",
            "0110000110",
            "1100000011",
            "0110000110",
            "0011111100",
            "0110000110",
            "1100000011",
            "1100000011",
            "0110000110",
            "0011111100"
        },
        { // 9
            "0011111100",
            "0110000110",
            "1100000011",
            "1100000011",
            "0110001111",
            "0011111111",
            "0000000011",
            "0000000011",
            "0110000110",
            "0011111100"
        }
    };

    public static int[] getDigitVector(int digit) {
        String[] pattern = DIGITS[digit];
        int[] vector = new int[100];
        int k = 0;

        for (String row : pattern) {
            for (char c : row.toCharArray()) {
                vector[k++] = (c == '1') ? 1 : -1;
            }
        }
        return vector;
    }
}