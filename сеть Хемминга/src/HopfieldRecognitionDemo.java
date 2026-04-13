import java.util.Arrays;
import java.util.Random;

public class HopfieldRecognitionDemo {

    public static void main(String[] args) {
        final int SIZE = 100;
        final int DIGITS = 10;

        // 1. Эталонные цифры
        int[][] digitPatterns = new int[DIGITS][];
        for (int d = 0; d < DIGITS; d++) {
            digitPatterns[d] = DigitPatterns.getDigitVector(d);
        }

        // 2. Обучаем сеть Хопфилда
        HopfieldNetwork hopfield = new HopfieldNetwork(SIZE);
        hopfield.train(digitPatterns);

        // 3. Обучаем распознающую сеть
        PerceptronClassifier classifier = new PerceptronClassifier(SIZE, DIGITS, 0.1);
        classifier.train(digitPatterns, 1000);

        // 4. Тестирование
        for (int digit = 0; digit < DIGITS; digit++) {
            System.out.println("============================================");
            System.out.println("Эталон цифры: " + digit);
            DigitPatterns.print(digitPatterns[digit]);

            int[] damaged = Arrays.copyOf(digitPatterns[digit], SIZE);
            addNoise(damaged, 18); // портим 18 пикселей

            System.out.println("\nПоврежденный образ:");
            DigitPatterns.print(damaged);

            int[] restored = hopfield.recall(damaged, 30);

            System.out.println("\nВосстановленный сетью Хопфилда образ:");
            DigitPatterns.print(restored);

            int recognized = classifier.predict(restored);
            System.out.println("\nРаспознано как цифра: " + recognized);
        }
    }

    private static void addNoise(int[] pattern, int noiseCount) {
        Random random = new Random();
        for (int i = 0; i < noiseCount; i++) {
            int idx = random.nextInt(pattern.length);
            pattern[idx] = -pattern[idx];
        }
    }
}

class HopfieldNetwork {
    private final int size;
    private final int[][] weights;

    public HopfieldNetwork(int size) {
        this.size = size;
        this.weights = new int[size][size];
    }

    public void train(int[][] patterns) {
        for (int[] pattern : patterns) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (i != j) {
                        weights[i][j] += pattern[i] * pattern[j];
                    }
                }
            }
        }
    }

    public int[] recall(int[] input, int maxIterations) {
        int[] state = Arrays.copyOf(input, input.length);

        for (int iter = 0; iter < maxIterations; iter++) {
            boolean changed = false;

            for (int i = 0; i < size; i++) {
                int sum = 0;
                for (int j = 0; j < size; j++) {
                    sum += weights[i][j] * state[j];
                }

                int newState = (sum >= 0) ? 1 : -1;
                if (newState != state[i]) {
                    state[i] = newState;
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }
        }

        return state;
    }
}

class PerceptronClassifier {
    private final int inputSize;
    private final int outputSize;
    private final double learningRate;
    private final double[][] weights;
    private final double[] bias;

    public PerceptronClassifier(int inputSize, int outputSize, double learningRate) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.learningRate = learningRate;
        this.weights = new double[outputSize][inputSize];
        this.bias = new double[outputSize];
    }

    public void train(int[][] patterns, int epochs) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            int errors = 0;

            for (int digit = 0; digit < patterns.length; digit++) {
                int[] input = patterns[digit];

                for (int out = 0; out < outputSize; out++) {
                    int target = (out == digit) ? 1 : -1;
                    int prediction = activate(net(input, out));
                    int error = target - prediction;

                    if (error != 0) {
                        errors++;
                        for (int i = 0; i < inputSize; i++) {
                            weights[out][i] += learningRate * error * input[i];
                        }
                        bias[out] += learningRate * error;
                    }
                }
            }

            if (errors == 0) {
                break;
            }
        }
    }

    public int predict(int[] input) {
        int bestClass = 0;
        double bestValue = net(input, 0);

        for (int out = 1; out < outputSize; out++) {
            double value = net(input, out);
            if (value > bestValue) {
                bestValue = value;
                bestClass = out;
            }
        }
        return bestClass;
    }

    private double net(int[] input, int neuron) {
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

    public static void print(int[] vector) {
        for (int i = 0; i < vector.length; i++) {
            System.out.print(vector[i] == 1 ? "##" : "..");
            if ((i + 1) % 10 == 0) {
                System.out.println();
            }
        }
    }
}