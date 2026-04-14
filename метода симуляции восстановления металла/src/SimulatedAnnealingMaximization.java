import java.util.Random;

public class SimulatedAnnealingMaximization {

    static class State {
        double x;
        double y;
        double value;

        State(double x, double y) {
            this.x = x;
            this.y = y;
            this.value = objective(x, y);
        }

        State copy() {
            State s = new State(this.x, this.y);
            s.value = this.value;
            return s;
        }
    }

    static final Random random = new Random();

    // Границы области поиска
    static final double MIN_X = -10.0;
    static final double MAX_X = 10.0;
    static final double MIN_Y = -10.0;
    static final double MAX_Y = 10.0;

    // Целевая функция для максимизации
    static double objective(double x, double y) {
        return 1.0 / (1.0 + x * x + y * y);
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // Случайная начальная точка
    static State randomState() {
        double x = MIN_X + (MAX_X - MIN_X) * random.nextDouble();
        double y = MIN_Y + (MAX_Y - MIN_Y) * random.nextDouble();
        return new State(x, y);
    }

    // Генерация соседнего решения
    static State generateNeighbor(State current, double temperature, double initialTemperature) {
        double baseStep = 2.0;
        double step = baseStep * (temperature / initialTemperature) + 0.0001;

        double newX = current.x + random.nextGaussian() * step;
        double newY = current.y + random.nextGaussian() * step;

        newX = clamp(newX, MIN_X, MAX_X);
        newY = clamp(newY, MIN_Y, MAX_Y);

        return new State(newX, newY);
    }

    static State simulatedAnnealing() {
        double temperature = 10.0;       // начальная температура
        double initialTemperature = temperature;
        double minTemperature = 1e-6;    // температура остановки
        double alpha = 0.995;            // коэффициент охлаждения
        int iterationsPerTemp = 200;     // число попыток на каждой температуре

        State current = randomState();
        State best = current.copy();

        while (temperature > minTemperature) {
            for (int i = 0; i < iterationsPerTemp; i++) {
                State next = generateNeighbor(current, temperature, initialTemperature);
                double delta = next.value - current.value;

                // Для максимизации:
                // лучшее решение принимаем всегда,
                // худшее — с вероятностью exp(delta / temperature)
                if (delta >= 0 || Math.exp(delta / temperature) > random.nextDouble()) {
                    current = next;
                }

                if (current.value > best.value) {
                    best = current.copy();
                }
            }

            temperature *= alpha;
        }

        return best;
    }

    public static void main(String[] args) {
        State best = simulatedAnnealing();

        System.out.println("Результат метода имитации отжига:");
        System.out.printf("x = %.8f%n", best.x);
        System.out.printf("y = %.8f%n", best.y);
        System.out.printf("f(x, y) = %.12f%n", best.value);

        System.out.println();
        System.out.println("Точное решение для функции:");
        System.out.println("Максимум достигается в точке x = 0, y = 0");
        System.out.println("f(0, 0) = 1");
    }
}
