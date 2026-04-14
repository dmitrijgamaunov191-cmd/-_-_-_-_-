import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Полноценная учебная модель когнитрона на Java.
 *
 * Возможности:
 * 1) Рисование образов на сетке 16x16.
 * 2) Формирование обучающего набора по классам.
 * 3) Обучение многослойного когнитрона (S/C-слои).
 * 4) Распознавание произвольных образов.
 * 5) Сохранение и загрузка образов из PNG.
 * 6) Графический интерфейс Swing.
 *
 * Важно:
 * Это полноценная учебная реализация в духе когнитрона/неокогнитрона:
 * - S-слои выделяют локальные признаки;
 * - C-слои выполняют локальную агрегацию/инвариантность;
 * - обучение выполняется конкурентным правилом с учителем по классам;
 * - финальный слой принимает решение по максимуму отклика.
 *
 * Для запуска:
 * javac CognitronApp.java
 * java CognitronApp
 */
@SuppressWarnings("serial")
public class CognitronApp extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CognitronApp app = new CognitronApp();
            app.setVisible(true);
        });
    }

    // ---------------------------
    // GUI fields
    // ---------------------------

    private final DrawPanel drawPanel;
    private final JTextArea logArea;
    private final DefaultListModel<String> datasetListModel;
    private final JLabel statusLabel;
    private final JTextField classField;
    private final JSpinner epochSpinner;
    private final JSpinner lrSpinner;
    private final JSpinner noiseSpinner;

    private final TrainingSet trainingSet;
    private Cognitron cognitron;

    public CognitronApp() {
        super("Когнитрон: распознавание произвольных образов");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        this.trainingSet = new TrainingSet();
        this.cognitron = Cognitron.defaultModel(16, 16, 8);

        this.drawPanel = new DrawPanel(16, 16, 28);
        this.logArea = new JTextArea();
        this.logArea.setEditable(false);
        this.logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        this.datasetListModel = new DefaultListModel<>();
        this.statusLabel = new JLabel("Готово.");
        this.classField = new JTextField("0", 6);
        this.epochSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 1000, 1));
        this.lrSpinner = new JSpinner(new SpinnerNumberModel(0.08, 0.001, 2.0, 0.01));
        this.noiseSpinner = new JSpinner(new SpinnerNumberModel(0.03, 0.0, 0.5, 0.01));

        add(buildLeftPanel(), BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        appendLog("Создан когнитрон с архитектурой: 16x16 -> S1/C1 -> S2/C2 -> выход.");
        appendLog("Готов к добавлению образов и обучению.");
    }

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createTitledBorder("Редактор образа"));
        top.add(drawPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(0, 2, 6, 6));
        JButton clearBtn = new JButton("Очистить");
        JButton invertBtn = new JButton("Инвертировать");
        JButton randomBtn = new JButton("Случайный шум");
        JButton centerBtn = new JButton("Центрировать");
        JButton savePngBtn = new JButton("Сохранить PNG");
        JButton loadPngBtn = new JButton("Загрузить PNG");

        clearBtn.addActionListener(e -> drawPanel.clear());
        invertBtn.addActionListener(e -> drawPanel.invert());
        randomBtn.addActionListener(e -> drawPanel.addNoise((double) noiseSpinner.getValue()));
        centerBtn.addActionListener(e -> drawPanel.centerMass());
        savePngBtn.addActionListener(e -> saveCurrentImage());
        loadPngBtn.addActionListener(e -> loadImage());

        actions.add(clearBtn);
        actions.add(invertBtn);
        actions.add(randomBtn);
        actions.add(centerBtn);
        actions.add(savePngBtn);
        actions.add(loadPngBtn);
        top.add(actions, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBorder(BorderFactory.createTitledBorder("Журнал"));
        bottom.add(new JScrollPane(logArea), BorderLayout.CENTER);

        panel.add(top, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setPreferredSize(new Dimension(410, 800));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));

        JPanel controls = new JPanel(new GridLayout(0, 2, 6, 6));
        controls.setBorder(BorderFactory.createTitledBorder("Управление обучением"));

        JButton addSampleBtn = new JButton("Добавить образ");
        JButton trainBtn = new JButton("Обучить");
        JButton recognizeBtn = new JButton("Распознать");
        JButton resetNetBtn = new JButton("Сбросить сеть");
        JButton removeSampleBtn = new JButton("Удалить последний");
        JButton synthBtn = new JButton("Синтез цифр");

        addSampleBtn.addActionListener(e -> addCurrentSample());
        trainBtn.addActionListener(e -> trainNetwork());
        recognizeBtn.addActionListener(e -> recognizeCurrent());
        resetNetBtn.addActionListener(e -> resetNetwork());
        removeSampleBtn.addActionListener(e -> removeLastSample());
        synthBtn.addActionListener(e -> synthesizeDataset());

        controls.add(new JLabel("Класс:"));
        controls.add(classField);
        controls.add(new JLabel("Эпохи:"));
        controls.add(epochSpinner);
        controls.add(new JLabel("Скорость обучения:"));
        controls.add(lrSpinner);
        controls.add(new JLabel("Шум:"));
        controls.add(noiseSpinner);
        controls.add(addSampleBtn);
        controls.add(removeSampleBtn);
        controls.add(trainBtn);
        controls.add(recognizeBtn);
        controls.add(resetNetBtn);
        controls.add(synthBtn);

        JList<String> sampleList = new JList<>(datasetListModel);
        JScrollPane datasetScroll = new JScrollPane(sampleList);
        datasetScroll.setBorder(BorderFactory.createTitledBorder("Обучающая выборка"));

        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        infoArea.setText(
                "Модель когнитрона:\n\n" +
                "S-слой: набор карт признаков. Каждый нейрон анализирует локальное окно входа.\n\n" +
                "C-слой: локальное усреднение/максимизация откликов, что повышает устойчивость к сдвигам и деформациям.\n\n" +
                "Обучение:\n" +
                "- конкурентное в S-слоях;\n" +
                "- только карты, назначенные классу, усиливаются сильнее;\n" +
                "- финальный слой агрегирует отклики карт класса.\n\n" +
                "Рекомендуемый сценарий:\n" +
                "1) добавить несколько образов для каждого класса;\n" +
                "2) обучить сеть;\n" +
                "3) рисовать новые образцы и распознавать.\n\n" +
                "Для быстрого старта нажмите 'Синтез цифр'."
        );
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setBorder(BorderFactory.createTitledBorder("Описание"));

        JPanel center = new JPanel(new GridLayout(2, 1, 8, 8));
        center.add(datasetScroll);
        center.add(infoScroll);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private void addCurrentSample() {
        try {
            int label = Integer.parseInt(classField.getText().trim());
            double[][] img = drawPanel.getImageData();
            trainingSet.add(new Sample(img, label));
            datasetListModel.addElement("Образ " + trainingSet.size() + " -> класс " + label);
            appendLog("Добавлен образ класса " + label + ". Всего образов: " + trainingSet.size());
            setStatus("Образ добавлен.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Введите целочисленный класс.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeLastSample() {
        if (trainingSet.size() == 0) return;
        trainingSet.removeLast();
        datasetListModel.remove(datasetListModel.size() - 1);
        appendLog("Удалён последний образ. Осталось: " + trainingSet.size());
        setStatus("Последний образ удалён.");
    }

    private void trainNetwork() {
        if (trainingSet.size() == 0) {
            JOptionPane.showMessageDialog(this, "Добавьте обучающие образы.", "Нет данных", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int epochs = (int) epochSpinner.getValue();
        double lr = (double) lrSpinner.getValue();

        appendLog("Запуск обучения. Эпох: " + epochs + ", learningRate=" + String.format(Locale.US, "%.3f", lr));
        setStatus("Идёт обучение...");

        SwingWorker<String, String> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                cognitron.train(trainingSet, epochs, lr, msg -> publish(msg));
                return "Обучение завершено.";
            }

            @Override
            protected void process(List<String> chunks) {
                for (String s : chunks) appendLog(s);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    appendLog(result);
                    setStatus(result);
                } catch (Exception ex) {
                    appendLog("Ошибка обучения: " + ex.getMessage());
                    setStatus("Ошибка обучения.");
                }
            }
        };
        worker.execute();
    }

    private void recognizeCurrent() {
        double[][] image = drawPanel.getImageData();
        CognitronResult result = cognitron.recognize(image);

        StringBuilder sb = new StringBuilder();
        sb.append("Распознан класс: ").append(result.predictedClass).append("\n");
        sb.append("Уверенности по классам:\n");
        for (int i = 0; i < result.classScores.length; i++) {
            sb.append("  ").append(i).append(": ")
                    .append(String.format(Locale.US, "%.4f", result.classScores[i]))
                    .append("\n");
        }
        appendLog(sb.toString());
        setStatus("Распознан класс: " + result.predictedClass);
        JOptionPane.showMessageDialog(this, sb.toString(), "Распознавание", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetNetwork() {
        cognitron = Cognitron.defaultModel(16, 16, 8);
        appendLog("Сеть сброшена и инициализирована заново.");
        setStatus("Сеть сброшена.");
    }

    private void synthesizeDataset() {
        int[] digits = {0,1,2,3,4,5,6,7,8,9};
        int added = 0;
        for (int d : digits) {
            for (int i = 0; i < 8; i++) {
                double[][] img = SyntheticDigits.generateDigit16x16(d, i);
                trainingSet.add(new Sample(img, d));
                datasetListModel.addElement("Образ " + trainingSet.size() + " -> класс " + d + " (синтез)");
                added++;
            }
        }
        appendLog("Синтезирован обучающий набор: " + added + " образов.");
        setStatus("Синтетическая выборка добавлена.");
    }

    private void saveCurrentImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage img = drawPanel.toBufferedImage(16);
                ImageIO.write(img, "png", file);
                appendLog("Образ сохранён: " + file.getAbsolutePath());
                setStatus("PNG сохранён.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(file);
                drawPanel.loadFromImage(img);
                appendLog("Образ загружен: " + file.getAbsolutePath());
                setStatus("PNG загружен.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void appendLog(String text) {
        logArea.append(text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    // ============================================================
    // Data model
    // ============================================================

    static class Sample {
        final double[][] image;
        final int label;

        Sample(double[][] image, int label) {
            this.image = Matrix.copy(image);
            this.label = label;
        }
    }

    static class TrainingSet {
        final List<Sample> samples = new ArrayList<>();

        void add(Sample s) { samples.add(s); }
        int size() { return samples.size(); }
        void removeLast() { if (!samples.isEmpty()) samples.remove(samples.size() - 1); }
        List<Sample> all() { return samples; }
        int maxLabel() {
            int max = 0;
            for (Sample s : samples) max = Math.max(max, s.label);
            return max;
        }
    }

    interface TrainingListener {
        void onMessage(String message);
    }

    static class CognitronResult {
        final int predictedClass;
        final double[] classScores;

        CognitronResult(int predictedClass, double[] classScores) {
            this.predictedClass = predictedClass;
            this.classScores = classScores;
        }
    }

    // ============================================================
    // Cognitron
    // ============================================================

    static class Cognitron {
        @SuppressWarnings("unused")
		private final int inputW;
        @SuppressWarnings("unused")
		private final int inputH;
        private final int classCount;

        private final SLayer s1;
        private final CLayer c1;
        private final SLayer s2;
        private final CLayer c2;
        private final double[][] classifierWeights; // class x featureMap
        private final Random rnd = new Random();

        Cognitron(int inputW, int inputH, int classCount,
                  SLayer s1, CLayer c1, SLayer s2, CLayer c2) {
            this.inputW = inputW;
            this.inputH = inputH;
            this.classCount = classCount;
            this.s1 = s1;
            this.c1 = c1;
            this.s2 = s2;
            this.c2 = c2;
            this.classifierWeights = new double[classCount][s2.mapCount];
            initClassifier();
        }

        static Cognitron defaultModel(int w, int h, int classCount) {
            // S1: 8 карт 3x3, C1: pooling 2x2
            // S2: 12 карт 3x3, C2: pooling global-like local 2x2
            SLayer s1 = new SLayer(1, 8, 3, 3, 0.10, 0.35);
            CLayer c1 = new CLayer(8, 2, 2, CPoolMode.AVG);
            SLayer s2 = new SLayer(8, 12, 3, 3, 0.12, 0.40);
            CLayer c2 = new CLayer(12, 2, 2, CPoolMode.MAX);
            return new Cognitron(w, h, classCount, s1, c1, s2, c2);
        }

        private void initClassifier() {
            for (int c = 0; c < classifierWeights.length; c++) {
                for (int i = 0; i < classifierWeights[c].length; i++) {
                    classifierWeights[c][i] = rnd.nextGaussian() * 0.05;
                }
            }
        }

        void train(TrainingSet set, int epochs, double lr, TrainingListener listener) {
            if (set.size() == 0) return;
            List<Sample> data = new ArrayList<>(set.all());
            int effectiveClasses = Math.max(classCount - 1, set.maxLabel()) + 1;

            for (int epoch = 1; epoch <= epochs; epoch++) {
                java.util.Collections.shuffle(data, rnd);
                int correct = 0;

                for (Sample sample : data) {
                    double[][][] inputMaps = new double[][][]{Matrix.normalize(sample.image)};

                    // прямой проход
                    double[][][] s1Out = s1.forward(inputMaps);
                    double[][][] c1Out = c1.forward(s1Out);
                    double[][][] s2Out = s2.forward(c1Out);
                    double[][][] c2Out = c2.forward(s2Out);
                    double[] features = collapseMaps(c2Out);

                    int predicted = argMax(scoreClasses(features));
                    if (predicted == sample.label) correct++;

                    // обучение S1 по образцу: усиливаем победителей и их карты
                    s1.trainCompetitive(inputMaps, lr * 0.8, sample.label);

                    // после обновления S1 снова пересчитываем c1
                    s1Out = s1.forward(inputMaps);
                    c1Out = c1.forward(s1Out);

                    // обучение S2 на признаках S1/C1
                    s2.trainCompetitive(c1Out, lr * 0.6, sample.label);

                    // классификатор по отклику карт S2/C2
                    s2Out = s2.forward(c1Out);
                    c2Out = c2.forward(s2Out);
                    features = collapseMaps(c2Out);
                    trainClassifier(features, sample.label, lr * 0.5, effectiveClasses);
                }

                double acc = (double) correct / data.size();
                if (listener != null) {
                    listener.onMessage(String.format(Locale.US,
                            "Эпоха %d/%d | точность до обновлений: %.2f%%",
                            epoch, epochs, acc * 100.0));
                }
            }
        }

        CognitronResult recognize(double[][] image) {
            double[][][] inputMaps = new double[][][]{Matrix.normalize(image)};
            double[][][] s1Out = s1.forward(inputMaps);
            double[][][] c1Out = c1.forward(s1Out);
            double[][][] s2Out = s2.forward(c1Out);
            double[][][] c2Out = c2.forward(s2Out);
            double[] features = collapseMaps(c2Out);
            double[] scores = scoreClasses(features);
            return new CognitronResult(argMax(scores), scores);
        }

        private void trainClassifier(double[] features, int label, double lr, int effectiveClasses) {
            double[] scores = scoreClasses(features);
            int predicted = argMax(scores);

            if (label >= classifierWeights.length) return;

            // Усиливаем правильный класс
            for (int i = 0; i < features.length; i++) {
                classifierWeights[label][i] += lr * features[i];
            }

            // Немного ослабляем ошибочный победивший класс
            if (predicted != label && predicted < effectiveClasses) {
                for (int i = 0; i < features.length; i++) {
                    classifierWeights[predicted][i] -= lr * 0.35 * features[i];
                }
            }

            normalizeClassifier();
        }

        private void normalizeClassifier() {
            for (int c = 0; c < classifierWeights.length; c++) {
                double norm = 1e-9;
                for (double v : classifierWeights[c]) norm += v * v;
                norm = Math.sqrt(norm);
                for (int i = 0; i < classifierWeights[c].length; i++) {
                    classifierWeights[c][i] /= norm;
                }
            }
        }

        private double[] scoreClasses(double[] features) {
            double[] scores = new double[classifierWeights.length];
            for (int c = 0; c < classifierWeights.length; c++) {
                double s = 0.0;
                for (int i = 0; i < features.length; i++) {
                    s += classifierWeights[c][i] * features[i];
                }
                scores[c] = s;
            }
            return softmax(scores);
        }

        private double[] collapseMaps(double[][][] maps) {
            double[] features = new double[maps.length];
            for (int m = 0; m < maps.length; m++) {
                double sum = 0.0;
                for (int y = 0; y < maps[m].length; y++) {
                    for (int x = 0; x < maps[m][0].length; x++) {
                        sum += maps[m][y][x];
                    }
                }
                features[m] = sum / (maps[m].length * maps[m][0].length);
            }
            return features;
        }

        private double[] softmax(double[] z) {
            double max = Arrays.stream(z).max().orElse(0.0);
            double sum = 0.0;
            double[] ex = new double[z.length];
            for (int i = 0; i < z.length; i++) {
                ex[i] = Math.exp(z[i] - max);
                sum += ex[i];
            }
            for (int i = 0; i < ex.length; i++) ex[i] /= sum;
            return ex;
        }

        private int argMax(double[] a) {
            int idx = 0;
            for (int i = 1; i < a.length; i++) {
                if (a[i] > a[idx]) idx = i;
            }
            return idx;
        }
    }

    static class SLayer {
        final int inMaps;
        final int mapCount;
        final int kernelH;
        final int kernelW;
        final double threshold;
        final double inhibition;
        final double[][][][] kernels; // outMap x inMap x kh x kw
        final Random rnd = new Random();

        SLayer(int inMaps, int mapCount, int kernelH, int kernelW, double threshold, double inhibition) {
            this.inMaps = inMaps;
            this.mapCount = mapCount;
            this.kernelH = kernelH;
            this.kernelW = kernelW;
            this.threshold = threshold;
            this.inhibition = inhibition;
            this.kernels = new double[mapCount][inMaps][kernelH][kernelW];
            randomInit();
        }

        private void randomInit() {
            for (int o = 0; o < mapCount; o++) {
                for (int i = 0; i < inMaps; i++) {
                    for (int y = 0; y < kernelH; y++) {
                        for (int x = 0; x < kernelW; x++) {
                            kernels[o][i][y][x] = rnd.nextGaussian() * 0.15;
                        }
                    }
                }
                normalizeKernel(o);
            }
        }

        double[][][] forward(double[][][] input) {
            int inH = input[0].length;
            int inW = input[0][0].length;
            int outH = inH - kernelH + 1;
            int outW = inW - kernelW + 1;
            double[][][] out = new double[mapCount][outH][outW];

            for (int o = 0; o < mapCount; o++) {
                for (int oy = 0; oy < outH; oy++) {
                    for (int ox = 0; ox < outW; ox++) {
                        double s = 0.0;
                        double normInput = 1e-9;
                        for (int i = 0; i < inMaps; i++) {
                            for (int ky = 0; ky < kernelH; ky++) {
                                for (int kx = 0; kx < kernelW; kx++) {
                                    double v = input[i][oy + ky][ox + kx];
                                    s += v * kernels[o][i][ky][kx];
                                    normInput += v * v;
                                }
                            }
                        }
                        normInput = Math.sqrt(normInput);
                        double u = s / normInput - inhibition * localEnergy(input, oy, ox);
                        out[o][oy][ox] = activation(u - threshold);
                    }
                }
            }
            return out;
        }

        void trainCompetitive(double[][][] input, double lr, int label) {
            int inH = input[0].length;
            int inW = input[0][0].length;
            int outH = inH - kernelH + 1;
            int outW = inW - kernelW + 1;

            double[][][] responses = forward(input);
            int bandSize = Math.max(1, mapCount / 4);
            int preferredStart = (label * bandSize) % mapCount;
            int preferredEnd = Math.min(mapCount, preferredStart + bandSize);

            for (int oy = 0; oy < outH; oy++) {
                for (int ox = 0; ox < outW; ox++) {
                    int winner = preferredStart;
                    double best = -Double.MAX_VALUE;
                    for (int o = preferredStart; o < preferredEnd; o++) {
                        if (responses[o][oy][ox] > best) {
                            best = responses[o][oy][ox];
                            winner = o;
                        }
                    }
                    if (best > 0.02) {
                        updateKernelTowardPatch(winner, input, oy, ox, lr);
                    }
                }
            }
        }

        private void updateKernelTowardPatch(int outMap, double[][][] input, int oy, int ox, double lr) {
            for (int i = 0; i < inMaps; i++) {
                for (int ky = 0; ky < kernelH; ky++) {
                    for (int kx = 0; kx < kernelW; kx++) {
                        double target = input[i][oy + ky][ox + kx];
                        kernels[outMap][i][ky][kx] += lr * (target - kernels[outMap][i][ky][kx]);
                    }
                }
            }
            normalizeKernel(outMap);
        }

        private void normalizeKernel(int outMap) {
            double norm = 1e-9;
            for (int i = 0; i < inMaps; i++) {
                for (int y = 0; y < kernelH; y++) {
                    for (int x = 0; x < kernelW; x++) {
                        norm += kernels[outMap][i][y][x] * kernels[outMap][i][y][x];
                    }
                }
            }
            norm = Math.sqrt(norm);
            for (int i = 0; i < inMaps; i++) {
                for (int y = 0; y < kernelH; y++) {
                    for (int x = 0; x < kernelW; x++) {
                        kernels[outMap][i][y][x] /= norm;
                    }
                }
            }
        }

        private double localEnergy(double[][][] input, int oy, int ox) {
            double sum = 0.0;
            for (int i = 0; i < inMaps; i++) {
                for (int ky = 0; ky < kernelH; ky++) {
                    for (int kx = 0; kx < kernelW; kx++) {
                        sum += Math.abs(input[i][oy + ky][ox + kx]);
                    }
                }
            }
            return sum / (inMaps * kernelH * kernelW);
        }

        private double activation(double x) {
            return x > 0 ? Math.tanh(1.6 * x) : 0.0;
        }
    }

    enum CPoolMode { AVG, MAX }

    static class CLayer {
        final int mapCount;
        final int poolH;
        final int poolW;
        final CPoolMode mode;

        CLayer(int mapCount, int poolH, int poolW, CPoolMode mode) {
            this.mapCount = mapCount;
            this.poolH = poolH;
            this.poolW = poolW;
            this.mode = mode;
        }

        double[][][] forward(double[][][] input) {
            int inH = input[0].length;
            int inW = input[0][0].length;
            int outH = Math.max(1, inH / poolH);
            int outW = Math.max(1, inW / poolW);
            double[][][] out = new double[mapCount][outH][outW];

            for (int m = 0; m < mapCount; m++) {
                for (int oy = 0; oy < outH; oy++) {
                    for (int ox = 0; ox < outW; ox++) {
                        double agg = (mode == CPoolMode.MAX) ? -Double.MAX_VALUE : 0.0;
                        int count = 0;
                        for (int py = 0; py < poolH; py++) {
                            for (int px = 0; px < poolW; px++) {
                                int iy = oy * poolH + py;
                                int ix = ox * poolW + px;
                                if (iy >= inH || ix >= inW) continue;
                                double v = input[m][iy][ix];
                                if (mode == CPoolMode.MAX) {
                                    agg = Math.max(agg, v);
                                } else {
                                    agg += v;
                                }
                                count++;
                            }
                        }
                        out[m][oy][ox] = (mode == CPoolMode.MAX) ? agg : agg / Math.max(1, count);
                    }
                }
            }
            return out;
        }
    }

    // ============================================================
    // Draw panel
    // ============================================================

    static class DrawPanel extends JPanel {
        final int rows;
        final int cols;
        final int cellSize;
        final double[][] pixels;
        boolean drawingMode = true;

        DrawPanel(int rows, int cols, int cellSize) {
            this.rows = rows;
            this.cols = cols;
            this.cellSize = cellSize;
            this.pixels = new double[rows][cols];
            setPreferredSize(new Dimension(cols * cellSize, rows * cellSize));
            setBackground(Color.WHITE);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    drawingMode = SwingUtilities.isLeftMouseButton(e);
                    applyBrush(e.getX(), e.getY(), drawingMode ? 1.0 : 0.0);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    applyBrush(e.getX(), e.getY(), drawingMode ? 1.0 : 0.0);
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        void applyBrush(int px, int py, double value) {
            int c = px / cellSize;
            int r = py / cellSize;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int rr = r + dr;
                    int cc = c + dc;
                    if (rr >= 0 && rr < rows && cc >= 0 && cc < cols) {
                        double dist = Math.sqrt(dr * dr + dc * dc);
                        double blend = dist == 0 ? 1.0 : 0.55;
                        pixels[rr][cc] = clamp(value * blend + pixels[rr][cc] * (1 - blend));
                    }
                }
            }
            repaint();
        }

        void clear() {
            for (int r = 0; r < rows; r++) Arrays.fill(pixels[r], 0.0);
            repaint();
        }

        void invert() {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    pixels[r][c] = 1.0 - pixels[r][c];
                }
            }
            repaint();
        }

        void addNoise(double p) {
            Random rnd = new Random();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (rnd.nextDouble() < p) pixels[r][c] = 1.0 - pixels[r][c];
                }
            }
            repaint();
        }

        void centerMass() {
            double sum = 0.0, sumR = 0.0, sumC = 0.0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    double v = pixels[r][c];
                    sum += v;
                    sumR += v * r;
                    sumC += v * c;
                }
            }
            if (sum < 1e-9) return;
            int cr = (int) Math.round(sumR / sum);
            int cc = (int) Math.round(sumC / sum);
            int dr = rows / 2 - cr;
            int dc = cols / 2 - cc;
            double[][] copy = Matrix.copy(pixels);
            clear();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int rr = r + dr;
                    int cc2 = c + dc;
                    if (rr >= 0 && rr < rows && cc2 >= 0 && cc2 < cols) {
                        pixels[rr][cc2] = copy[r][c];
                    }
                }
            }
            repaint();
        }

        double[][] getImageData() {
            return Matrix.copy(pixels);
        }

        BufferedImage toBufferedImage(int size) {
            BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_BYTE_GRAY);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int g = (int) (pixels[r][c] * 255);
                    int rgb = (g << 16) | (g << 8) | g;
                    img.setRGB(c, r, rgb);
                }
            }
            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2 = scaled.createGraphics();
            g2.drawImage(img, 0, 0, size, size, null);
            g2.dispose();
            return scaled;
        }

        void loadFromImage(BufferedImage img) {
            BufferedImage scaled = new BufferedImage(cols, rows, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2 = scaled.createGraphics();
            g2.drawImage(img, 0, 0, cols, rows, null);
            g2.dispose();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int rgb = scaled.getRGB(c, r) & 0xFF;
                    pixels[r][c] = rgb / 255.0;
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int gray = 255 - (int) (pixels[r][c] * 255);
                    g2.setColor(new Color(gray, gray, gray));
                    g2.fillRect(c * cellSize, r * cellSize, cellSize, cellSize);
                    g2.setColor(new Color(220, 220, 220));
                    g2.drawRect(c * cellSize, r * cellSize, cellSize, cellSize);
                }
            }
            g2.dispose();
        }

        private double clamp(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }

    // ============================================================
    // Utilities
    // ============================================================

    static class Matrix {
        static double[][] copy(double[][] src) {
            double[][] dst = new double[src.length][src[0].length];
            for (int i = 0; i < src.length; i++) {
                System.arraycopy(src[i], 0, dst[i], 0, src[i].length);
            }
            return dst;
        }

        static double[][] normalize(double[][] src) {
            double[][] out = copy(src);
            double mean = 0.0;
            int n = src.length * src[0].length;
            for (double[] row : src) for (double v : row) mean += v;
            mean /= n;
            double std = 1e-9;
            for (double[] row : src) for (double v : row) std += (v - mean) * (v - mean);
            std = Math.sqrt(std / n);
            for (int y = 0; y < out.length; y++) {
                for (int x = 0; x < out[0].length; x++) {
                    out[y][x] = (out[y][x] - mean) / std;
                }
            }
            return out;
        }
    }

    // ============================================================
    // Synthetic digits generator
    // ============================================================

    static class SyntheticDigits {
        static double[][] generateDigit16x16(int digit, int variant) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 16, 16);
            g.setColor(Color.WHITE);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

            int dx = (variant % 3) - 1;
            int dy = ((variant / 3) % 3) - 1;
            g.drawString(String.valueOf(digit), 3 + dx, 13 + dy);

            if (variant % 2 == 0) {
                g.drawLine(1, 1, 1 + variant % 4, 1);
            }
            if (variant % 5 == 0) {
                g.drawLine(14, 14, 12, 14);
            }
            g.dispose();

            double[][] out = new double[16][16];
            Random rnd = new Random(digit * 100 + variant);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int gray = img.getRGB(x, y) & 0xFF;
                    double v = gray / 255.0;
                    if (rnd.nextDouble() < 0.03) v = 1.0 - v;
                    out[y][x] = v;
                }
            }
            return out;
        }
    }
}
