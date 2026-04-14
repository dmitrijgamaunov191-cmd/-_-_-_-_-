import java.util.*;

/**
 * Обучение однослойного и многослойного перцептронов с помощью генетического алгоритма.
 *
 * Что делает программа:
 * 1) обучает однослойный перцептрон на задаче AND;
 * 2) показывает, что на XOR однослойный перцептрон ограничен;
 * 3) обучает многослойный перцептрон на задаче XOR;
 * 4) использует только генетический алгоритм, без градиентного спуска.
 *
 * Запуск:
 * javac GATrainPerceptron.java
 * java GATrainPerceptron
 */
public class GATrainPerceptron {

    public static void main(String[] args) {
        Dataset andSet = Dataset.andDataset();
        Dataset xorSet = Dataset.xorDataset();

        System.out.println("=== Обучение однослойного перцептрона на AND ===");
        SingleLayerPerceptron slp = new SingleLayerPerceptron(2, Activation.SIGMOID);
        GeneticTrainer<SingleLayerPerceptron> slpTrainer = new GeneticTrainer<>(
                () -> SingleLayerPerceptron.random(2, Activation.SIGMOID),
                (net, data) -> 1.0 / (1e-6 + mse(net, data)),
                120,
                200,
                0.12,
                0.70,
                3,
                new Random()
        );

        SingleLayerPerceptron bestAnd = slpTrainer.train(slp, andSet, true);
        printPredictions(bestAnd, andSet);
        System.out.printf("MSE(AND) = %.6f%n%n", mse(bestAnd, andSet));

        System.out.println("=== Обучение однослойного перцептрона на XOR ===");
        SingleLayerPerceptron bestXorSLP = slpTrainer.train(slp, xorSet, true);
        printPredictions(bestXorSLP, xorSet);
        System.out.printf("MSE(XOR, single-layer) = %.6f%n", mse(bestXorSLP, xorSet));
        System.out.println("Однослойный перцептрон не может идеально разделить XOR.\n");

        System.out.println("=== Обучение многослойного перцептрона на XOR ===");
        MultiLayerPerceptron mlp = new MultiLayerPerceptron(new int[]{2, 4, 1}, Activation.TANH, Activation.SIGMOID);
        GeneticTrainer<MultiLayerPerceptron> mlpTrainer = new GeneticTrainer<>(
                () -> MultiLayerPerceptron.random(new int[]{2, 4, 1}, Activation.TANH, Activation.SIGMOID),
                (net, data) -> 1.0 / (1e-6 + mse(net, data)),
                180,
                500,
                0.18,
                0.75,
                4,
                new Random()
        );

        MultiLayerPerceptron bestMLP = mlpTrainer.train(mlp, xorSet, true);
        printPredictions(bestMLP, xorSet);
        System.out.printf("MSE(XOR, multi-layer) = %.6f%n", mse(bestMLP, xorSet));
    }

    // =============================
    // Общие методы оценки качества
    // =============================

    static double mse(Predictor net, Dataset dataset) {
        double sum = 0.0;
        for (int i = 0; i < dataset.inputs.length; i++) {
            double[] out = net.predict(dataset.inputs[i]);
            for (int j = 0; j < out.length; j++) {
                double e = dataset.targets[i][j] - out[j];
                sum += e * e;
            }
        }
        return sum / dataset.inputs.length;
    }

    static void printPredictions(Predictor net, Dataset dataset) {
        for (int i = 0; i < dataset.inputs.length; i++) {
            double[] in = dataset.inputs[i];
            double[] out = net.predict(in);
            System.out.printf(Locale.US,
                    "in=%s -> out=%.4f, target=%.1f%n",
                    Arrays.toString(in), out[0], dataset.targets[i][0]);
        }
    }

    // =============================
    // Базовые интерфейсы
    // =============================

    interface Predictor {
        double[] predict(double[] input);
    }

    interface Evolvable<T> extends Predictor {
        double[] getGenome();
        void setGenome(double[] genome);
        T copy();
    }

    interface Factory<T> {
        T createRandom();
    }

    interface FitnessFunction<T> {
        double fitness(T individual, Dataset dataset);
    }

    // =============================
    // Датасет
    // =============================

    static class Dataset {
        final double[][] inputs;
        final double[][] targets;

        Dataset(double[][] inputs, double[][] targets) {
            this.inputs = inputs;
            this.targets = targets;
        }

        static Dataset andDataset() {
            return new Dataset(
                    new double[][]{
                            {0, 0},
                            {0, 1},
                            {1, 0},
                            {1, 1}
                    },
                    new double[][]{
                            {0},
                            {0},
                            {0},
                            {1}
                    }
            );
        }

        static Dataset xorDataset() {
            return new Dataset(
                    new double[][]{
                            {0, 0},
                            {0, 1},
                            {1, 0},
                            {1, 1}
                    },
                    new double[][]{
                            {0},
                            {1},
                            {1},
                            {0}
                    }
            );
        }
    }

    // =============================
    // Функции активации
    // =============================

    enum Activation {
        SIGMOID {
            @Override
            double apply(double x) {
                return 1.0 / (1.0 + Math.exp(-x));
            }
        },
        TANH {
            @Override
            double apply(double x) {
                return Math.tanh(x);
            }
        },
        RELU {
            @Override
            double apply(double x) {
                return Math.max(0.0, x);
            }
        },
        LINEAR {
            @Override
            double apply(double x) {
                return x;
            }
        };

        abstract double apply(double x);
    }

    // =============================
    // Однослойный перцептрон
    // =============================

    static class SingleLayerPerceptron implements Evolvable<SingleLayerPerceptron> {
        final int inputSize;
        final Activation activation;
        final double[] weights;
        double bias;

        SingleLayerPerceptron(int inputSize, Activation activation) {
            this.inputSize = inputSize;
            this.activation = activation;
            this.weights = new double[inputSize];
        }

        static SingleLayerPerceptron random(int inputSize, Activation activation) {
            Random rnd = new Random();
            SingleLayerPerceptron p = new SingleLayerPerceptron(inputSize, activation);
            for (int i = 0; i < inputSize; i++) {
                p.weights[i] = rnd.nextGaussian();
            }
            p.bias = rnd.nextGaussian();
            return p;
        }

        @Override
        public double[] predict(double[] input) {
            double s = bias;
            for (int i = 0; i < inputSize; i++) {
                s += weights[i] * input[i];
            }
            return new double[]{activation.apply(s)};
        }

        @Override
        public double[] getGenome() {
            double[] genome = new double[inputSize + 1];
            System.arraycopy(weights, 0, genome, 0, inputSize);
            genome[inputSize] = bias;
            return genome;
        }

        @Override
        public void setGenome(double[] genome) {
            System.arraycopy(genome, 0, weights, 0, inputSize);
            bias = genome[inputSize];
        }

        @Override
        public SingleLayerPerceptron copy() {
            SingleLayerPerceptron cp = new SingleLayerPerceptron(inputSize, activation);
            cp.setGenome(getGenome());
            return cp;
        }
    }

    // =============================
    // Многослойный перцептрон
    // =============================

    static class MultiLayerPerceptron implements Evolvable<MultiLayerPerceptron> {
        final int[] layers;
        final Activation hiddenActivation;
        final Activation outputActivation;

        final double[][][] weights; // [layer][from][to]
        final double[][] biases;    // [layer][to]

        MultiLayerPerceptron(int[] layers, Activation hiddenActivation, Activation outputActivation) {
            this.layers = Arrays.copyOf(layers, layers.length);
            this.hiddenActivation = hiddenActivation;
            this.outputActivation = outputActivation;

            this.weights = new double[layers.length - 1][][];
            this.biases = new double[layers.length - 1][];

            for (int l = 0; l < layers.length - 1; l++) {
                weights[l] = new double[layers[l]][layers[l + 1]];
                biases[l] = new double[layers[l + 1]];
            }
        }

        static MultiLayerPerceptron random(int[] layers, Activation hiddenActivation, Activation outputActivation) {
            Random rnd = new Random();
            MultiLayerPerceptron net = new MultiLayerPerceptron(layers, hiddenActivation, outputActivation);
            for (int l = 0; l < net.weights.length; l++) {
                for (int i = 0; i < net.weights[l].length; i++) {
                    for (int j = 0; j < net.weights[l][i].length; j++) {
                        net.weights[l][i][j] = rnd.nextGaussian() * 1.2;
                    }
                }
                for (int j = 0; j < net.biases[l].length; j++) {
                    net.biases[l][j] = rnd.nextGaussian() * 1.2;
                }
            }
            return net;
        }

        @Override
        public double[] predict(double[] input) {
            double[] a = Arrays.copyOf(input, input.length);
            for (int l = 0; l < weights.length; l++) {
                double[] next = new double[layers[l + 1]];
                for (int j = 0; j < next.length; j++) {
                    double s = biases[l][j];
                    for (int i = 0; i < a.length; i++) {
                        s += a[i] * weights[l][i][j];
                    }
                    Activation act = (l == weights.length - 1) ? outputActivation : hiddenActivation;
                    next[j] = act.apply(s);
                }
                a = next;
            }
            return a;
        }

        @Override
        public double[] getGenome() {
            int total = 0;
            for (int l = 0; l < weights.length; l++) {
                total += layers[l] * layers[l + 1];
                total += layers[l + 1];
            }
            double[] genome = new double[total];
            int idx = 0;

            for (int l = 0; l < weights.length; l++) {
                for (int i = 0; i < weights[l].length; i++) {
                    for (int j = 0; j < weights[l][i].length; j++) {
                        genome[idx++] = weights[l][i][j];
                    }
                }
                for (int j = 0; j < biases[l].length; j++) {
                    genome[idx++] = biases[l][j];
                }
            }
            return genome;
        }

        @Override
        public void setGenome(double[] genome) {
            int idx = 0;
            for (int l = 0; l < weights.length; l++) {
                for (int i = 0; i < weights[l].length; i++) {
                    for (int j = 0; j < weights[l][i].length; j++) {
                        weights[l][i][j] = genome[idx++];
                    }
                }
                for (int j = 0; j < biases[l].length; j++) {
                    biases[l][j] = genome[idx++];
                }
            }
        }

        @Override
        public MultiLayerPerceptron copy() {
            MultiLayerPerceptron cp = new MultiLayerPerceptron(layers, hiddenActivation, outputActivation);
            cp.setGenome(getGenome());
            return cp;
        }
    }

    // =============================
    // Генетический алгоритм
    // =============================

    static class GeneticTrainer<T extends Evolvable<T>> {
        private final Factory<T> factory;
        private final FitnessFunction<T> fitnessFunction;
        private final int populationSize;
        private final int generations;
        private final double mutationRate;
        private final double crossoverRate;
        private final int eliteCount;
        private final Random rnd;

        GeneticTrainer(Factory<T> factory,
                       FitnessFunction<T> fitnessFunction,
                       int populationSize,
                       int generations,
                       double mutationRate,
                       double crossoverRate,
                       int eliteCount,
                       Random rnd) {
            this.factory = factory;
            this.fitnessFunction = fitnessFunction;
            this.populationSize = populationSize;
            this.generations = generations;
            this.mutationRate = mutationRate;
            this.crossoverRate = crossoverRate;
            this.eliteCount = eliteCount;
            this.rnd = rnd;
        }

        T train(T prototype, Dataset dataset, boolean verbose) {
            List<Individual<T>> population = new ArrayList<>();
            for (int i = 0; i < populationSize; i++) {
                T ind = factory.createRandom();
                double fit = fitnessFunction.fitness(ind, dataset);
                population.add(new Individual<>(ind, fit));
            }

            Individual<T> globalBest = null;

            for (int gen = 0; gen < generations; gen++) {
                population.sort((a, b) -> Double.compare(b.fitness, a.fitness));
                if (globalBest == null || population.get(0).fitness > globalBest.fitness) {
                    globalBest = new Individual<>(population.get(0).genome.copy(), population.get(0).fitness);
                }

                if (verbose && (gen % 20 == 0 || gen == generations - 1)) {
                    double mse = 1.0 / globalBest.fitness - 1e-6;
                    System.out.printf(Locale.US,
                            "Поколение %d | best fitness = %.6f | approx MSE = %.6f%n",
                            gen, globalBest.fitness, mse);
                }

                List<Individual<T>> next = new ArrayList<>();

                for (int i = 0; i < eliteCount; i++) {
                    next.add(new Individual<>(population.get(i).genome.copy(), population.get(i).fitness));
                }

                while (next.size() < populationSize) {
                    T parent1 = tournament(population, 4).genome;
                    T parent2 = tournament(population, 4).genome;

                    T child = crossover(parent1, parent2);
                    mutate(child);
                    double fit = fitnessFunction.fitness(child, dataset);
                    next.add(new Individual<>(child, fit));
                }

                population = next;
            }

            return globalBest.genome;
        }

        private Individual<T> tournament(List<Individual<T>> population, int size) {
            Individual<T> best = null;
            for (int i = 0; i < size; i++) {
                Individual<T> candidate = population.get(rnd.nextInt(population.size()));
                if (best == null || candidate.fitness > best.fitness) {
                    best = candidate;
                }
            }
            return best;
        }

        private T crossover(T p1, T p2) {
            T child = p1.copy();
            double[] g1 = p1.getGenome();
            double[] g2 = p2.getGenome();
            double[] gc = child.getGenome();

            if (rnd.nextDouble() < crossoverRate) {
                for (int i = 0; i < gc.length; i++) {
                    if (rnd.nextBoolean()) {
                        gc[i] = g1[i];
                    } else {
                        gc[i] = g2[i];
                    }

                    // арифметическое смешивание иногда даёт лучшую сходимость
                    if (rnd.nextDouble() < 0.2) {
                        double alpha = rnd.nextDouble();
                        gc[i] = alpha * g1[i] + (1.0 - alpha) * g2[i];
                    }
                }
            } else {
                System.arraycopy(g1, 0, gc, 0, gc.length);
            }

            child.setGenome(gc);
            return child;
        }

        private void mutate(T individual) {
            double[] genome = individual.getGenome();
            for (int i = 0; i < genome.length; i++) {
                if (rnd.nextDouble() < mutationRate) {
                    genome[i] += rnd.nextGaussian() * 0.8;
                }
                if (rnd.nextDouble() < 0.02) {
                    genome[i] = rnd.nextGaussian() * 1.5;
                }
            }
            individual.setGenome(genome);
        }
    }

    static class Individual<T extends Evolvable<T>> {
        final T genome;
        final double fitness;

        Individual(T genome, double fitness) {
            this.genome = genome;
            this.fitness = fitness;
        }
    }
}
