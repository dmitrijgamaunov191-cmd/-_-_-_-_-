import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Модель биоценоза: растения -> травоядные -> хищники.
 *
 * Особенности:
 * - растения растут, размножаются и запасают энергию;
 * - животные управляются однослойными перцептронами;
 * - у животных есть метаболизм, старение, голод, размножение;
 * - графический интерфейс на Swing;
 * - справа отображаются параметры и статистика.
 *
 * Для запуска:
 * javac BiocenosisApp.java
 * java BiocenosisApp
 */
@SuppressWarnings("serial")
public class BiocenosisApp extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BiocenosisApp app = new BiocenosisApp();
            app.setVisible(true);
        });
    }

    private final World world;
    private final WorldPanel worldPanel;
    private final JLabel statsLabel;
    private final Timer timer;

    public BiocenosisApp() {
        super("Биоценоз: растения - травоядные - хищники");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        this.world = new World(920, 760);
        this.worldPanel = new WorldPanel(world);
        this.statsLabel = new JLabel();
        this.statsLabel.setVerticalAlignment(SwingConstants.TOP);

        JPanel rightPanel = buildControlPanel();

        add(worldPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        timer = new Timer(33, (ActionEvent e) -> {
            world.update();
            updateStats();
            worldPanel.repaint();
        });
        timer.start();

        updateStats();
    }

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(330, 760));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 8, 8));
        buttons.setBorder(BorderFactory.createTitledBorder("Управление"));

        JButton pauseBtn = new JButton("Пауза / Продолжить");
        JButton resetBtn = new JButton("Сброс мира");
        JButton addPlantsBtn = new JButton("+ 50 растений");
        JButton addHerbBtn = new JButton("+ 10 травоядных");
        JButton addPredBtn = new JButton("+ 5 хищников");

        pauseBtn.addActionListener(e -> {
            if (timer.isRunning()) timer.stop(); else timer.start();
        });
        resetBtn.addActionListener(e -> {
            world.reset();
            updateStats();
            worldPanel.repaint();
        });
        addPlantsBtn.addActionListener(e -> world.spawnPlants(50));
        addHerbBtn.addActionListener(e -> world.spawnHerbivores(10));
        addPredBtn.addActionListener(e -> world.spawnPredators(5));

        buttons.add(pauseBtn);
        buttons.add(resetBtn);
        buttons.add(addPlantsBtn);
        buttons.add(addHerbBtn);
        buttons.add(addPredBtn);

        JTextArea help = new JTextArea();
        help.setEditable(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        help.setText(
                "Модель метаболизма:\n" +
                "- каждое животное тратит энергию на базовый обмен и движение;\n" +
                "- при голоде падает жизнеспособность;\n" +
                "- при избытке энергии агент может размножаться;\n" +
                "- травоядные едят растения;\n" +
                "- хищники едят травоядных.\n\n" +
                "Перцептрон получает входы:\n" +
                "1) нормализованную энергию;\n" +
                "2) близость пищи;\n" +
                "3) близость угрозы/цели;\n" +
                "4) случайный шум;\n" +
                "5) смещение (bias).\n\n" +
                "Выходы перцептрона:\n" +
                "- dx, dy — направление движения;\n" +
                "- desire — желание ускоряться/преследовать.\n");

        JScrollPane scrollPane = new JScrollPane(help);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Описание модели"));

        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(BorderFactory.createTitledBorder("Статистика"));
        statsPanel.add(statsLabel, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(statsPanel, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);

        panel.add(buttons, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);

        return panel;
    }

    private void updateStats() {
        String html = "<html><body style='font-family:sans-serif;font-size:12px'>" +
                "<b>Тик:</b> " + world.tick + "<br>" +
                "<b>Растения:</b> " + world.plants.size() + "<br>" +
                "<b>Травоядные:</b> " + world.herbivores.size() + "<br>" +
                "<b>Хищники:</b> " + world.predators.size() + "<br><br>" +
                "<b>Средняя энергия травоядных:</b> " + format(world.avgEnergy(world.herbivores)) + "<br>" +
                "<b>Средняя энергия хищников:</b> " + format(world.avgEnergy(world.predators)) + "<br><br>" +
                "<b>Параметры:</b><br>" +
                "Размер мира: " + world.width + " x " + world.height + "<br>" +
                "Шагов в секунду: ~30<br>" +
                "Рост растений: каждые 15 тиков<br>" +
                "Размножение растений: каждые 40 тиков<br>" +
                "</body></html>";
        statsLabel.setText(html);
    }

    private String format(double v) {
        return String.format("%.2f", v);
    }

    static class WorldPanel extends JPanel {
        private final World world;

        public WorldPanel(World world) {
            this.world = world;
            setPreferredSize(new Dimension(world.width, world.height));
            setBackground(new Color(245, 247, 244));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(220, 230, 220));
            for (int x = 0; x < world.width; x += 40) g2.drawLine(x, 0, x, world.height);
            for (int y = 0; y < world.height; y += 40) g2.drawLine(0, y, world.width, y);

            for (Plant p : world.plants) drawPlant(g2, p);
            for (Herbivore h : world.herbivores) drawAnimal(g2, h, new Color(60, 120, 255));
            for (Predator p : world.predators) drawAnimal(g2, p, new Color(220, 70, 70));

            g2.dispose();
        }

        private void drawPlant(Graphics2D g2, Plant p) {
            float energyNorm = (float) Math.min(1.0, p.energy / Plant.MAX_ENERGY);
            int size = (int) (4 + energyNorm * 10);
            g2.setColor(new Color(40, (int) (140 + 80 * energyNorm), 50));
            g2.fill(new Ellipse2D.Double(p.x - size / 2.0, p.y - size / 2.0, size, size));
        }

        private void drawAnimal(Graphics2D g2, Animal a, Color color) {
            int size = (int) Math.max(8, a.radius * 2);
            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(a.x - size / 2.0, a.y - size / 2.0, size, size));

            double ex = a.x + a.vx * 6;
            double ey = a.y + a.vy * 6;
            g2.setColor(Color.BLACK);
            g2.drawLine((int) a.x, (int) a.y, (int) ex, (int) ey);

            // полоса энергии
            int bw = 18;
            int bh = 4;
            int bx = (int) a.x - bw / 2;
            int by = (int) a.y - size / 2 - 8;
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(bx, by, bw, bh);
            g2.setColor(new Color(70, 200, 90));
            int fill = (int) (bw * Math.max(0.0, Math.min(1.0, a.energy / a.maxEnergy)));
            g2.fillRect(bx + 1, by + 1, Math.max(0, fill - 1), bh - 1);
        }
    }

    static class World {
        final int width;
        final int height;
        final Random rnd = new Random();

        final List<Plant> plants = new ArrayList<>();
        final List<Herbivore> herbivores = new ArrayList<>();
        final List<Predator> predators = new ArrayList<>();

        long tick = 0;

        World(int width, int height) {
            this.width = width;
            this.height = height;
            reset();
        }

        void reset() {
            tick = 0;
            plants.clear();
            herbivores.clear();
            predators.clear();
            spawnPlants(160);
            spawnHerbivores(28);
            spawnPredators(10);
        }

        void spawnPlants(int count) {
            for (int i = 0; i < count; i++) {
                plants.add(new Plant(randX(), randY(), 8 + rnd.nextDouble() * 15));
            }
        }

        void spawnHerbivores(int count) {
            for (int i = 0; i < count; i++) {
                herbivores.add(new Herbivore(this, randX(), randY()));
            }
        }

        void spawnPredators(int count) {
            for (int i = 0; i < count; i++) {
                predators.add(new Predator(this, randX(), randY()));
            }
        }

        double randX() { return 20 + rnd.nextDouble() * (width - 40); }
        double randY() { return 20 + rnd.nextDouble() * (height - 40); }

        void update() {
            tick++;

            if (tick % 15 == 0) {
                for (Plant p : plants) p.grow();
            }
            if (tick % 40 == 0 && plants.size() < 400) {
                int births = 8 + rnd.nextInt(8);
                for (int i = 0; i < births; i++) {
                    plants.add(new Plant(randX(), randY(), 5 + rnd.nextDouble() * 10));
                }
            }

            List<Herbivore> newbornHerb = new ArrayList<>();
            List<Predator> newbornPred = new ArrayList<>();

            Iterator<Herbivore> itH = herbivores.iterator();
            while (itH.hasNext()) {
                Herbivore h = itH.next();
                h.update();
                if (!h.alive) {
                    itH.remove();
                } else {
                    Herbivore child = h.tryReproduce();
                    if (child != null) newbornHerb.add(child);
                }
            }

            Iterator<Predator> itP = predators.iterator();
            while (itP.hasNext()) {
                Predator p = itP.next();
                p.update();
                if (!p.alive) {
                    itP.remove();
                } else {
                    Predator child = p.tryReproduce();
                    if (child != null) newbornPred.add(child);
                }
            }

            herbivores.addAll(newbornHerb);
            predators.addAll(newbornPred);

            // поддерживаем минимальный уровень жизни в системе
            if (plants.size() < 40) spawnPlants(30);
            if (herbivores.size() < 4) spawnHerbivores(4);
            if (predators.size() < 2) spawnPredators(2);
        }

        Plant nearestPlant(double x, double y, double maxDist) {
            Plant best = null;
            double bestD2 = maxDist * maxDist;
            for (Plant p : plants) {
                double d2 = sqr(p.x - x) + sqr(p.y - y);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = p;
                }
            }
            return best;
        }

        Herbivore nearestHerbivore(double x, double y, double maxDist, Herbivore exclude) {
            Herbivore best = null;
            double bestD2 = maxDist * maxDist;
            for (Herbivore h : herbivores) {
                if (h == exclude) continue;
                double d2 = sqr(h.x - x) + sqr(h.y - y);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = h;
                }
            }
            return best;
        }

        Predator nearestPredator(double x, double y, double maxDist, Predator exclude) {
            Predator best = null;
            double bestD2 = maxDist * maxDist;
            for (Predator p : predators) {
                if (p == exclude) continue;
                double d2 = sqr(p.x - x) + sqr(p.y - y);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = p;
                }
            }
            return best;
        }

        Predator nearestPredatorTo(double x, double y, double maxDist) {
            Predator best = null;
            double bestD2 = maxDist * maxDist;
            for (Predator p : predators) {
                double d2 = sqr(p.x - x) + sqr(p.y - y);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    best = p;
                }
            }
            return best;
        }

        double avgEnergy(List<? extends Animal> animals) {
            if (animals.isEmpty()) return 0.0;
            double sum = 0.0;
            for (Animal a : animals) sum += a.energy;
            return sum / animals.size();
        }

        double sqr(double v) { return v * v; }
    }

    static class Plant {
        static final double MAX_ENERGY = 30.0;
        double x;
        double y;
        double energy;

        Plant(double x, double y, double energy) {
            this.x = x;
            this.y = y;
            this.energy = energy;
        }

        void grow() {
            energy = Math.min(MAX_ENERGY, energy + 1.5);
        }
    }

    static abstract class Animal {
        protected final World world;
        protected final Random rnd;

        protected double x;
        protected double y;
        protected double vx;
        protected double vy;
        protected double radius;
        protected double energy;
        protected double maxEnergy;
        protected int age;
        protected int maxAge;
        protected boolean alive = true;

        protected final Perceptron controller;

        protected Animal(World world, double x, double y) {
            this.world = world;
            this.rnd = world.rnd;
            this.x = x;
            this.y = y;
            this.vx = rnd.nextDouble() * 2 - 1;
            this.vy = rnd.nextDouble() * 2 - 1;
            this.controller = Perceptron.random(rnd, 5, 3);
        }

        protected void applyMetabolism(double speedFactor) {
            double basal = 0.06;              // базовый обмен
            double movement = 0.025 * speedFactor; // цена активности
            double aging = age > maxAge * 0.7 ? 0.02 : 0.0;
            energy -= (basal + movement + aging);
            age++;
            if (energy <= 0 || age > maxAge) alive = false;
        }

        protected void clampAndBounce() {
            if (x < 5) { x = 5; vx *= -0.7; }
            if (y < 5) { y = 5; vy *= -0.7; }
            if (x > world.width - 5) { x = world.width - 5; vx *= -0.7; }
            if (y > world.height - 5) { y = world.height - 5; vy *= -0.7; }
        }

        protected double distance(double x1, double y1, double x2, double y2) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            return Math.sqrt(dx * dx + dy * dy);
        }

        abstract void update();
    }

    static class Herbivore extends Animal {
        static final double VISION_FOOD = 120;
        static final double VISION_PRED = 150;
        static final double EAT_DISTANCE = 10;
        static final double REPRODUCE_THRESHOLD = 75;

        Herbivore(World world, double x, double y) {
            super(world, x, y);
            this.radius = 7;
            this.maxEnergy = 100;
            this.energy = 45 + rnd.nextDouble() * 25;
            this.maxAge = 3000 + rnd.nextInt(1200);
        }

        @Override
        void update() {
            Plant food = world.nearestPlant(x, y, VISION_FOOD);
            Predator danger = world.nearestPredatorTo(x, y, VISION_PRED);

            double foodProximity = 0.0;
            double foodDx = 0.0;
            double foodDy = 0.0;
            if (food != null) {
                double d = distance(x, y, food.x, food.y);
                foodProximity = 1.0 - Math.min(1.0, d / VISION_FOOD);
                foodDx = (food.x - x) / Math.max(1.0, d);
                foodDy = (food.y - y) / Math.max(1.0, d);
            }

            double dangerProximity = 0.0;
            double dangerDx = 0.0;
            double dangerDy = 0.0;
            if (danger != null) {
                double d = distance(x, y, danger.x, danger.y);
                dangerProximity = 1.0 - Math.min(1.0, d / VISION_PRED);
                dangerDx = (x - danger.x) / Math.max(1.0, d); // убегаем
                dangerDy = (y - danger.y) / Math.max(1.0, d);
            }

            double[] inputs = {
                    energy / maxEnergy,
                    foodProximity,
                    dangerProximity,
                    rnd.nextDouble() * 2 - 1,
                    1.0
            };

            double[] out = controller.forward(inputs);

            double dirX = out[0] + foodDx * foodProximity * 1.5 + dangerDx * dangerProximity * 2.3;
            double dirY = out[1] + foodDy * foodProximity * 1.5 + dangerDy * dangerProximity * 2.3;
            double desire = Math.max(0.15, (out[2] + 1.0) / 2.0);

            vx = 0.65 * vx + 0.35 * dirX;
            vy = 0.65 * vy + 0.35 * dirY;

            double norm = Math.sqrt(vx * vx + vy * vy);
            if (norm > 0.0001) {
                vx /= norm;
                vy /= norm;
            }

            double speed = 0.6 + 1.8 * desire;
            x += vx * speed;
            y += vy * speed;
            clampAndBounce();

            applyMetabolism(speed);

            if (food != null && distance(x, y, food.x, food.y) < EAT_DISTANCE) {
                double bite = Math.min(8.0, food.energy);
                energy = Math.min(maxEnergy, energy + bite);
                food.energy -= bite;
                if (food.energy <= 0.5) world.plants.remove(food);
            }
        }

        Herbivore tryReproduce() {
            if (!alive || energy < REPRODUCE_THRESHOLD || world.herbivores.size() > 120) return null;
            if (rnd.nextDouble() > 0.008) return null;

            energy -= 24;
            Herbivore child = new Herbivore(world, x + rnd.nextGaussian() * 8, y + rnd.nextGaussian() * 8);
            child.energy = 28;
            child.controller.copyMutatedFrom(this.controller, rnd, 0.18);
            return child;
        }
    }

    static class Predator extends Animal {
        static final double VISION_PREY = 160;
        static final double EAT_DISTANCE = 11;
        static final double REPRODUCE_THRESHOLD = 95;

        Predator(World world, double x, double y) {
            super(world, x, y);
            this.radius = 9;
            this.maxEnergy = 130;
            this.energy = 55 + rnd.nextDouble() * 30;
            this.maxAge = 3600 + rnd.nextInt(1200);
        }

        @Override
        void update() {
            Herbivore prey = world.nearestHerbivore(x, y, VISION_PREY, null);
            Predator neighbor = world.nearestPredator(x, y, 70, this);

            double preyProximity = 0.0;
            double preyDx = 0.0;
            double preyDy = 0.0;
            if (prey != null) {
                double d = distance(x, y, prey.x, prey.y);
                preyProximity = 1.0 - Math.min(1.0, d / VISION_PREY);
                preyDx = (prey.x - x) / Math.max(1.0, d);
                preyDy = (prey.y - y) / Math.max(1.0, d);
            }

            double crowd = 0.0;
            double crowdDx = 0.0;
            double crowdDy = 0.0;
            if (neighbor != null) {
                double d = distance(x, y, neighbor.x, neighbor.y);
                crowd = 1.0 - Math.min(1.0, d / 70.0);
                crowdDx = (x - neighbor.x) / Math.max(1.0, d); // избегаем скученности
                crowdDy = (y - neighbor.y) / Math.max(1.0, d);
            }

            double[] inputs = {
                    energy / maxEnergy,
                    preyProximity,
                    crowd,
                    rnd.nextDouble() * 2 - 1,
                    1.0
            };

            double[] out = controller.forward(inputs);

            double dirX = out[0] + preyDx * preyProximity * 2.0 + crowdDx * crowd * 0.8;
            double dirY = out[1] + preyDy * preyProximity * 2.0 + crowdDy * crowd * 0.8;
            double desire = Math.max(0.2, (out[2] + 1.0) / 2.0);

            vx = 0.58 * vx + 0.42 * dirX;
            vy = 0.58 * vy + 0.42 * dirY;

            double norm = Math.sqrt(vx * vx + vy * vy);
            if (norm > 0.0001) {
                vx /= norm;
                vy /= norm;
            }

            double speed = 0.9 + 2.4 * desire;
            x += vx * speed;
            y += vy * speed;
            clampAndBounce();

            applyMetabolism(speed * 1.25);

            if (prey != null && prey.alive && distance(x, y, prey.x, prey.y) < EAT_DISTANCE) {
                prey.alive = false;
                energy = Math.min(maxEnergy, energy + 35);
            }
        }

        Predator tryReproduce() {
            if (!alive || energy < REPRODUCE_THRESHOLD || world.predators.size() > 40) return null;
            if (rnd.nextDouble() > 0.005) return null;

            energy -= 30;
            Predator child = new Predator(world, x + rnd.nextGaussian() * 10, y + rnd.nextGaussian() * 10);
            child.energy = 35;
            child.controller.copyMutatedFrom(this.controller, rnd, 0.14);
            return child;
        }
    }

    static class Perceptron {
        private final int inputSize;
        private final int outputSize;
        private final double[][] w;

        private Perceptron(int inputSize, int outputSize) {
            this.inputSize = inputSize;
            this.outputSize = outputSize;
            this.w = new double[outputSize][inputSize];
        }

        static Perceptron random(Random rnd, int inputSize, int outputSize) {
            Perceptron p = new Perceptron(inputSize, outputSize);
            for (int o = 0; o < outputSize; o++) {
                for (int i = 0; i < inputSize; i++) {
                    p.w[o][i] = rnd.nextGaussian() * 0.9;
                }
            }
            return p;
        }

        double[] forward(double[] input) {
            double[] out = new double[outputSize];
            for (int o = 0; o < outputSize; o++) {
                double s = 0.0;
                for (int i = 0; i < inputSize; i++) {
                    s += w[o][i] * input[i];
                }
                out[o] = Math.tanh(s);
            }
            return out;
        }

        void copyMutatedFrom(Perceptron other, Random rnd, double mutationSigma) {
            for (int o = 0; o < outputSize; o++) {
                for (int i = 0; i < inputSize; i++) {
                    this.w[o][i] = other.w[o][i] + rnd.nextGaussian() * mutationSigma;
                }
            }
        }
    }
}

