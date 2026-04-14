import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unused")
public class GeneticAlgorithmComparison {

    enum SelectionType {
        ELITE,
        ROULETTE
    }

    static class Individual {
        double x;
        double y;
        double fitness;

        Individual(double x, double y) {
            this.x = x;
            this.y = y;
            this.fitness = objective(x, y);
        }

        Individual copy() {
            Individual c = new Individual(this.x, this.y);
            c.fitness = this.fitness;
            return c;
        }
    }

    static class Result {
        SelectionType type;
        Individual best;
        long timeNs;
        int generations;
        double avgFitness;

        Result(SelectionType type, Individual best, long timeNs, int generations, double avgFitness) {
            this.type = type;
            this.best = best;
            this.timeNs = timeNs;
            this.generations = generations;
            this.avgFitness = avgFitness;
        }
    }

    // Целевая функция
    static double objective(double x, double y) {
        return 1.0 / (1.0 + x * x + y * y);
    }

    static class GeneticAlgorithm {
        private final int populationSize;
        private final int generations;
        private final double mutationRate;
        private final double mutationStep;
        private final double minValue;
        private final double maxValue;
        private final int eliteCount;
        private final SelectionType selectionType;
        private final Random random = new Random();

        GeneticAlgorithm(int populationSize,
                         int generations,
                         double mutationRate,
                         double mutationStep,
                         double minValue,
                         double maxValue,
                         int eliteCount,
                         SelectionType selectionType) {
            this.populationSize = populationSize;
            this.generations = generations;
            this.mutationRate = mutationRate;
            this.mutationStep = mutationStep;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.eliteCount = eliteCount;
            this.selectionType = selectionType;
        }

        Result run() {
            long start = System.nanoTime();

            List<Individual> population = initPopulation();
            Individual globalBest = getBest(population).copy();

            for (int gen = 0; gen < generations; gen++) {
                evaluate(population);

                Individual currentBest = getBest(population);
                if (currentBest.fitness > globalBest.fitness) {
                    globalBest = currentBest.copy();
                }

                List<Individual> newPopulation = new ArrayList<>();

                if (selectionType == SelectionType.ELITE) {
                    population.sort((a, b) -> Double.compare(b.fitness, a.fitness));
                    for (int i = 0; i < eliteCount; i++) {
                        newPopulation.add(population.get(i).copy());
                    }

                    while (newPopulation.size() < populationSize) {
                        Individual parent1 = population.get(random.nextInt(eliteCount));
                        Individual parent2 = population.get(random.nextInt(eliteCount));
                        Individual child = crossover(parent1, parent2);
                        mutate(child);
                        child.fitness = objective(child.x, child.y);
                        newPopulation.add(child);
                    }
                } else {
                    while (newPopulation.size() < populationSize) {
                        Individual parent1 = rouletteSelect(population);
                        Individual parent2 = rouletteSelect(population);
                        Individual child = crossover(parent1, parent2);
                        mutate(child);
                        child.fitness = objective(child.x, child.y);
                        newPopulation.add(child);
                    }
                }

                population = newPopulation;
            }

            evaluate(population);
            Individual finalBest = getBest(population);
            if (finalBest.fitness > globalBest.fitness) {
                globalBest = finalBest.copy();
            }

            long end = System.nanoTime();
            double avgFitness = population.stream().mapToDouble(ind -> ind.fitness).average().orElse(0.0);

            return new Result(selectionType, globalBest, end - start, generations, avgFitness);
        }

        private List<Individual> initPopulation() {
            List<Individual> population = new ArrayList<>();
            for (int i = 0; i < populationSize; i++) {
                double x = randomRange(minValue, maxValue);
                double y = randomRange(minValue, maxValue);
                population.add(new Individual(x, y));
            }
            return population;
        }

        private void evaluate(List<Individual> population) {
            for (Individual ind : population) {
                ind.fitness = objective(ind.x, ind.y);
            }
        }

        private Individual getBest(List<Individual> population) {
            return Collections.max(population, Comparator.comparingDouble(ind -> ind.fitness));
        }

        private Individual crossover(Individual p1, Individual p2) {
            double alpha = random.nextDouble();
            double childX = alpha * p1.x + (1 - alpha) * p2.x;
            double childY = alpha * p1.y + (1 - alpha) * p2.y;
            return new Individual(childX, childY);
        }

        private void mutate(Individual ind) {
            if (random.nextDouble() < mutationRate) {
                ind.x += random.nextGaussian() * mutationStep;
            }
            if (random.nextDouble() < mutationRate) {
                ind.y += random.nextGaussian() * mutationStep;
            }

            ind.x = clamp(ind.x, minValue, maxValue);
            ind.y = clamp(ind.y, minValue, maxValue);
        }

        private Individual rouletteSelect(List<Individual> population) {
            double totalFitness = 0.0;
            for (Individual ind : population) {
                totalFitness += ind.fitness;
            }

            double r = random.nextDouble() * totalFitness;
            double cumulative = 0.0;

            for (Individual ind : population) {
                cumulative += ind.fitness;
                if (cumulative >= r) {
                    return ind;
                }
            }

            return population.get(population.size() - 1);
        }

        private double randomRange(double min, double max) {
            return min + (max - min) * random.nextDouble();
        }

        private double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    public static void main(String[] args) {
        int populationSize = 200;
        int generations = 500;
        double mutationRate = 0.15;
        double mutationStep = 0.2;
        double minValue = -10.0;
        double maxValue = 10.0;
        int eliteCount = 20;

        GeneticAlgorithm eliteGA = new GeneticAlgorithm(
                populationSize,
                generations,
                mutationRate,
                mutationStep,
                minValue,
                maxValue,
                eliteCount,
                SelectionType.ELITE
        );

        GeneticAlgorithm rouletteGA = new GeneticAlgorithm(
                populationSize,
                generations,
                mutationRate,
                mutationStep,
                minValue,
                maxValue,
                eliteCount,
                SelectionType.ROULETTE
        );

        Result eliteResult = eliteGA.run();
        Result rouletteResult = rouletteGA.run();

        printResult(eliteResult);
        printResult(rouletteResult);

        compare(eliteResult, rouletteResult);
    }

    private static void printResult(Result result) {
        System.out.println("Метод отбора: " + result.type);
        System.out.printf(Locale.US, "Лучшее x = %.8f%n", result.best.x);
        System.out.printf(Locale.US, "Лучшее y = %.8f%n", result.best.y);
        System.out.printf(Locale.US, "Максимум f(x,y) = %.10f%n", result.best.fitness);
        System.out.printf(Locale.US, "Средняя приспособленность = %.10f%n", result.avgFitness);
        System.out.printf(Locale.US, "Время работы = %.3f мс%n", result.timeNs / 1_000_000.0);
        System.out.println("Поколений: " + result.generations);
        System.out.println("----------------------------------------");
    }

    private static void compare(Result elite, Result roulette) {
        System.out.println("Сравнение:");
        if (elite.best.fitness > roulette.best.fitness) {
            System.out.println("По качеству решения лучше: ELITE");
        } else if (elite.best.fitness < roulette.best.fitness) {
            System.out.println("По качеству решения лучше: ROULETTE");
        } else {
            System.out.println("По качеству решения методы примерно равны");
        }

        if (elite.timeNs < roulette.timeNs) {
            System.out.println("По времени быстрее: ELITE");
        } else if (elite.timeNs > roulette.timeNs) {
            System.out.println("По времени быстрее: ROULETTE");
        } else {
            System.out.println("По времени методы примерно равны");
        }
    }
}