import java.util.Arrays;
import java.util.Random;

public class DigitPerceptronDemo {

    public static void main(String[] args) {
        int inputSize = 100;   // 10x10
        int outputSize = 10;   // цифры 0-9

        Perceptron perceptron = new Perceptron(inputSize, outputSize, 0.1);

        double[][] trainInputs = new double[10][];
        int[] trainLabels = new int[10];

        for (int digit = 0; digit < 10; digit++) {
            trainInputs[digit] = DigitPatterns.getDigitVector(digit);
            trainLabels[digit] = digit;
        }

        perceptron.train(trainInputs, trainLabels, 500);

        System.out.println("=== Проверка на эталонных цифрах ===");
        for (int digit = 0; digit < 10; digit++) {
            int predicted = perceptron.predict(trainInputs[digit]);
            System.out.printf("Цифра %d -> распознано как %d%n", digit, predicted);
        }

        System.out.println("\n=== Проверка на зашумленных цифрах ===");
        Random random = new Random(42);
        for (int digit = 0; digit < 10; digit++) {
            double[] noisy = addNoise(trainInputs[digit], 8, random); // перевернуть 8 пикселей
            int predicted = perceptron.predict(noisy);
            System.out.printf("Зашумленная цифра %d -> распознано как %d%n", digit, predicted);
        }
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

class Perceptron {
    private final int inputSize;
    private final int outputSize;
    private final double learningRate;
    private final double[][] weights;
    private final double[] biases;

    public Perceptron(int inputSize, int outputSize, double learningRate) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.learningRate = learningRate;
        this.weights = new double[outputSize][inputSize];
        this.biases = new double[outputSize];

        Random random = new Random(1);
        for (int o = 0; o < outputSize; o++) {
            for (int i = 0; i < inputSize; i++) {
                weights[o][i] = random.nextDouble() * 0.2 - 0.1;
            }
            biases[o] = random.nextDouble() * 0.2 - 0.1;
        }
    }

    public void train(double[][] inputs, int[] labels, int epochs) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            int errors = 0;

            for (int sample = 0; sample < inputs.length; sample++) {
                double[] x = inputs[sample];
                int targetClass = labels[sample];

                int predictedClass = predict(x);
                if (predictedClass != targetClass) {
                    errors++;
                }

                for (int o = 0; o < outputSize; o++) {
                    int target = (o == targetClass) ? 1 : -1;
                    int output = activation(net(o, x));

                    int error = target - output;
                    if (error != 0) {
                        for (int i = 0; i < inputSize; i++) {
                            weights[o][i] += learningRate * error * x[i];
                        }
                        biases[o] += learningRate * error;
                    }
                }
            }

            if ((epoch + 1) % 50 == 0 || errors == 0) {
                System.out.printf("Эпоха %d, ошибок классификации: %d%n", epoch + 1, errors);
            }

            if (errors == 0) {
                break;
            }
        }
    }

    public int predict(double[] input) {
        int bestClass = 0;
        double bestScore = net(0, input);

        for (int o = 1; o < outputSize; o++) {
            double score = net(o, input);
            if (score > bestScore) {
                bestScore = score;
                bestClass = o;
            }
        }
        return bestClass;
    }

    private double net(int outputNeuron, double[] input) {
        double sum = biases[outputNeuron];
        for (int i = 0; i < inputSize; i++) {
            sum += weights[outputNeuron][i] * input[i];
        }
        return sum;
    }

    private int activation(double x) {
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

    public static double[] getDigitVector(int digit) {
        String[] pattern = DIGITS[digit];
        double[] vector = new double[100];
        int k = 0;

        for (String row : pattern) {
            for (char c : row.toCharArray()) {
                vector[k++] = (c == '1') ? 1.0 : -1.0;
            }
        }
        return vector;
    }
}