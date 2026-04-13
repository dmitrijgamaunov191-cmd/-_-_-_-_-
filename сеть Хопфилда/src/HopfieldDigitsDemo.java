import java.util.Arrays;
import java.util.Random;

public class HopfieldDigitsDemo {

    public static void main(String[] args) {
        int size = 100;
        HopfieldNetwork network = new HopfieldNetwork(size);

        int[][] patterns = new int[10][];
        for (int d = 0; d < 10; d++) {
            patterns[d] = DigitPatterns.getDigitVector(d);
        }

        // Обучение сети
        network.train(patterns);

        // Проверка восстановления
        for (int digit = 0; digit < 10; digit++) {
            System.out.println("======================================");
            System.out.println("Эталон цифры: " + digit);
            DigitPatterns.print(patterns[digit]);

            int[] damaged = Arrays.copyOf(patterns[digit], size);
            addNoise(damaged, 15); // портим 15 пикселей

            System.out.println("\nПоврежденный образ:");
            DigitPatterns.print(damaged);

            int[] restored = network.recall(damaged, 20);

            System.out.println("\nВосстановленный образ:");
            DigitPatterns.print(restored);

            int recognized = recognize(restored, patterns);
            System.out.println("\nРаспознано как цифра: " + recognized);
        }
    }

    // Добавить шум: случайно инвертировать несколько пикселей
    private static void addNoise(int[] pattern, int noiseCount) {
        Random random = new Random();
        for (int i = 0; i < noiseCount; i++) {
            int index = random.nextInt(pattern.length);
            pattern[index] = -pattern[index];
        }
    }

    // Поиск ближайшего эталона
    private static int recognize(int[] restored, int[][] patterns) {
        int bestDigit = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int d = 0; d < patterns.length; d++) {
            int score = 0;
            for (int i = 0; i < restored.length; i++) {
                score += restored[i] * patterns[d][i];
            }
            if (score > bestScore) {
                bestScore = score;
                bestDigit = d;
            }
        }
        return bestDigit;
    }
}

class HopfieldNetwork {
    private final int size;
    private final int[][] weights;

    public HopfieldNetwork(int size) {
        this.size = size;
        this.weights = new int[size][size];
    }

    // Обучение по правилу Хебба
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

    // Восстановление образа
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
        for (int i = 0; i < 100; i++) {
            System.out.print(vector[i] == 1 ? "##" : "..");
            if ((i + 1) % 10 == 0) {
                System.out.println();
            }
        }
    }
}