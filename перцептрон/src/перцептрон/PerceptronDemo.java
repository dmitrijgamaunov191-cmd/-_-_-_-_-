package перцептрон;
import java.util.Arrays;

/**
 * Пример реализации однослойного перцептрона на Java.
 * Обучение выполняется по правилу коррекции весов.
 */
public class PerceptronDemo {

    /**
     * Класс перцептрона.
     */
    static class Perceptron {
        private final double[] weights;
        private double bias;
        private final double learningRate;

        public Perceptron(int inputSize, double learningRate) {
            this.weights = new double[inputSize];
            this.bias = 0.0;
            this.learningRate = learningRate;
        }

        /**
         * Функция активации: пороговая.
         */
        public int predict(int[] inputs) {
            if (inputs.length != weights.length) {
                throw new IllegalArgumentException("Неверное число входов.");
            }

            double sum = bias;
            for (int i = 0; i < inputs.length; i++) {
                sum += inputs[i] * weights[i];
            }

            return sum >= 0 ? 1 : 0;
        }

        /**
         * Обучение перцептрона.
         *
         * @param trainingInputs обучающие входные данные
         * @param targets ожидаемые ответы
         * @param epochs количество эпох
         */
        public void train(int[][] trainingInputs, int[] targets, int epochs) {
            if (trainingInputs.length != targets.length) {
                throw new IllegalArgumentException("Число примеров и ответов не совпадает.");
            }

            for (int epoch = 0; epoch < epochs; epoch++) {
                int totalError = 0;

                for (int i = 0; i < trainingInputs.length; i++) {
                    int[] inputs = trainingInputs[i];
                    int target = targets[i];

                    int prediction = predict(inputs);
                    int error = target - prediction;

                    totalError += Math.abs(error);

                    // Коррекция весов
                    for (int j = 0; j < weights.length; j++) {
                        weights[j] += learningRate * error * inputs[j];
                    }

                    // Коррекция смещения
                    bias += learningRate * error;
                }

                System.out.printf("Эпоха %d, суммарная ошибка = %d%n", epoch + 1, totalError);

                // Если ошибок нет, можно завершить раньше
                if (totalError == 0) {
                    System.out.println("Обучение завершено: ошибок нет.\n");
                    break;
                }
            }
        }

        public void printParameters() {
            System.out.println("Весы: " + Arrays.toString(weights));
            System.out.println("Смещение: " + bias);
        }
    }

    public static void main(String[] args) {
        trainAND();
        trainOR();
        trainNOT();
    }

    /**
     * Обучение функции И.
     */
    private static void trainAND() {
        System.out.println("=== Обучение функции И ===");

        int[][] inputs = {
                {0, 0},
                {0, 1},
                {1, 0},
                {1, 1}
        };

        int[] targets = {0, 0, 0, 1};

        Perceptron perceptron = new Perceptron(2, 0.1);
        perceptron.train(inputs, targets, 20);
        perceptron.printParameters();

        System.out.println("Проверка:");
        for (int[] input : inputs) {
            System.out.printf("%d И %d = %d%n",
                    input[0], input[1], perceptron.predict(input));
        }
        System.out.println();
    }

    /**
     * Обучение функции ИЛИ.
     */
    private static void trainOR() {
        System.out.println("=== Обучение функции ИЛИ ===");

        int[][] inputs = {
                {0, 0},
                {0, 1},
                {1, 0},
                {1, 1}
        };

        int[] targets = {0, 1, 1, 1};

        Perceptron perceptron = new Perceptron(2, 0.1);
        perceptron.train(inputs, targets, 20);
        perceptron.printParameters();

        System.out.println("Проверка:");
        for (int[] input : inputs) {
            System.out.printf("%d ИЛИ %d = %d%n",
                    input[0], input[1], perceptron.predict(input));
        }
        System.out.println();
    }

    /**
     * Обучение функции НЕ.
     */
    private static void trainNOT() {
        System.out.println("=== Обучение функции НЕ ===");

        int[][] inputs = {
                {0},
                {1}
        };

        int[] targets = {1, 0};

        Perceptron perceptron = new Perceptron(1, 0.1);
        perceptron.train(inputs, targets, 20);
        perceptron.printParameters();

        System.out.println("Проверка:");
        for (int[] input : inputs) {
            System.out.printf("НЕ %d = %d%n",
                    input[0], perceptron.predict(input));
        }
        System.out.println();
    }
}