import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AntColonyTSP {

    private final double[][] distances;
    private final double[][] pheromones;
    private final int cityCount;
    private final int antCount;
    private final int iterations;

    // Параметры алгоритма
    private final double alpha;       // важность феромона
    private final double beta;        // важность расстояния
    private final double evaporation; // коэффициент испарения
    private final double q;           // количество откладываемого феромона

    private final Random random = new Random();

    private int[] bestTour;
    private double bestTourLength = Double.MAX_VALUE;

    public AntColonyTSP(double[][] distances,
                        int antCount,
                        int iterations,
                        double alpha,
                        double beta,
                        double evaporation,
                        double q) {
        this.distances = distances;
        this.cityCount = distances.length;
        this.antCount = antCount;
        this.iterations = iterations;
        this.alpha = alpha;
        this.beta = beta;
        this.evaporation = evaporation;
        this.q = q;

        this.pheromones = new double[cityCount][cityCount];
        initializePheromones();
    }

    private void initializePheromones() {
        for (int i = 0; i < cityCount; i++) {
            for (int j = 0; j < cityCount; j++) {
                pheromones[i][j] = 1.0;
            }
        }
    }

    public void solve() {
        for (int iter = 0; iter < iterations; iter++) {
            List<Ant> ants = new ArrayList<>();

            for (int i = 0; i < antCount; i++) {
                Ant ant = new Ant(cityCount);
                ant.buildTour();
                ants.add(ant);

                double length = ant.getTourLength();
                if (length < bestTourLength) {
                    bestTourLength = length;
                    bestTour = ant.tour.clone();
                }
            }

            evaporatePheromones();
            depositPheromones(ants);

            System.out.println("Итерация " + (iter + 1) + ": лучшая длина = " + bestTourLength);
        }
    }

    private void evaporatePheromones() {
        for (int i = 0; i < cityCount; i++) {
            for (int j = 0; j < cityCount; j++) {
                pheromones[i][j] *= (1.0 - evaporation);
                if (pheromones[i][j] < 0.0001) {
                    pheromones[i][j] = 0.0001;
                }
            }
        }
    }

    private void depositPheromones(List<Ant> ants) {
        for (Ant ant : ants) {
            double contribution = q / ant.getTourLength();

            for (int i = 0; i < cityCount - 1; i++) {
                int from = ant.tour[i];
                int to = ant.tour[i + 1];
                pheromones[from][to] += contribution;
                pheromones[to][from] += contribution;
            }

            // Замыкаем маршрут
            int last = ant.tour[cityCount - 1];
            int first = ant.tour[0];
            pheromones[last][first] += contribution;
            pheromones[first][last] += contribution;
        }
    }

    public int[] getBestTour() {
        return bestTour;
    }

    public double getBestTourLength() {
        return bestTourLength;
    }

    private class Ant {
        private final int[] tour;
        private final boolean[] visited;

        public Ant(int cityCount) {
            this.tour = new int[cityCount];
            this.visited = new boolean[cityCount];
        }

        public void buildTour() {
            int startCity = random.nextInt(cityCount);
            tour[0] = startCity;
            visited[startCity] = true;

            for (int step = 1; step < cityCount; step++) {
                int currentCity = tour[step - 1];
                int nextCity = selectNextCity(currentCity);
                tour[step] = nextCity;
                visited[nextCity] = true;
            }
        }

        private int selectNextCity(int currentCity) {
            double[] probabilities = new double[cityCount];
            double sum = 0.0;

            for (int city = 0; city < cityCount; city++) {
                if (!visited[city]) {
                    double tau = Math.pow(pheromones[currentCity][city], alpha);
                    double eta = Math.pow(1.0 / distances[currentCity][city], beta);
                    probabilities[city] = tau * eta;
                    sum += probabilities[city];
                }
            }

            double rand = random.nextDouble() * sum;
            double cumulative = 0.0;

            for (int city = 0; city < cityCount; city++) {
                if (!visited[city]) {
                    cumulative += probabilities[city];
                    if (cumulative >= rand) {
                        return city;
                    }
                }
            }

            // Запасной вариант
            for (int city = 0; city < cityCount; city++) {
                if (!visited[city]) {
                    return city;
                }
            }

            throw new IllegalStateException("Не удалось выбрать следующий город");
        }

        public double getTourLength() {
            double length = 0.0;

            for (int i = 0; i < cityCount - 1; i++) {
                length += distances[tour[i]][tour[i + 1]];
            }

            // Возврат в начальный город
            length += distances[tour[cityCount - 1]][tour[0]];
            return length;
        }
    }

    public static void main(String[] args) {
        double[][] distances = {
                {0, 2, 9, 10, 7},
                {2, 0, 6, 4, 3},
                {9, 6, 0, 8, 5},
                {10, 4, 8, 0, 6},
                {7, 3, 5, 6, 0}
        };

        AntColonyTSP colony = new AntColonyTSP(
                distances,
                20,     // количество муравьев
                100,    // количество итераций
                1.0,    // alpha
                5.0,    // beta
                0.5,    // evaporation
                100.0   // q
        );

        colony.solve();

        System.out.println("\nЛучший маршрут:");
        System.out.println(Arrays.toString(colony.getBestTour()));
        System.out.println("Длина маршрута: " + colony.getBestTourLength());
    }
}
