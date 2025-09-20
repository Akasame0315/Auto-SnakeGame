import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.List;

public class GeneticAlgorithmAI {

    private static final String GENE_FILE = "best_genes.txt";
    private static final String LOG_FILE = "training_log.txt";

    private static final int POPULATION_SIZE = 250; // 族群大小
    private static final double MUTATION_RATE = 0.1; //通常突變率會設定在 1% 到 5% (0.01 ~ 0.05) 之間。過高的突變率會讓演算法難以收斂到最佳解；過低則可能陷入局部最優解
    private static final int MAX_GENERATIONS = 1000; // 最大世代數
    private static final int ELITE_COUNT = (int) (POPULATION_SIZE * 0.1); // 保留前 10% 的精英。這是一個非常標準且有效的做法（稱為「精英主義」），可以確保每一代的最優個體不會因為交叉或突變而丟失。

    private final Random random = new Random();
    // 訓練好的最佳基因
    private double[] bestGenes;
    private double bestFitness = -1.0;
    private final SnakeSimulator simulator;

    public GeneticAlgorithmAI(int width, int height) {
        // 注意：建構子中不再進行訓練，它只負責初始化
        this.simulator = new SnakeSimulator(width, height);
    }

    //  支援儲存和載入基因，並將輸出轉為日誌檔案，同時在訓練中途也能夠記錄進度。
    public void startTraining() {
        writeToLog("AI Training started...");

        CompletableFuture.runAsync(() -> {
            Individual[] population = initializePopulation();

            // 嘗試從檔案載入上次訓練的進度
            double[] loadedGenes = loadGenes();
            if (loadedGenes != null) {
                Individual bestIndividual = new Individual(GameSettings.GENE_SIZE);
                bestIndividual.genes = loadedGenes;
                population[0] = bestIndividual; // 將載入的基因作為精英個體
                System.out.println("Resuming training from a previously saved model.");
                writeToLog("Resuming training from a previously saved model.");
            }

            for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
                evaluatePopulation(population);

                Arrays.sort(population, Comparator.comparingDouble(Individual::getFitness).reversed());

                Individual bestInGeneration = population[0];

                if (bestInGeneration.getFitness() > bestFitness) {
                    bestFitness = bestInGeneration.getFitness();
                    bestGenes = bestInGeneration.genes;
                    writeToLog("Generation " + (generation + 1) + " - New Best Fitness: " + bestFitness);
                    saveBestGenes(); // 找到更好的基因時立即保存
                } else {
                    writeToLog("Generation " + (generation + 1) + " - Best Fitness: " + bestInGeneration.getFitness());
                }
                if(generation % 100 == 0){
                    System.out.println("Generation " +(generation + 1) + "：Best Fitness = " +  bestInGeneration.getFitness());
                }
//                System.out.println("Generation " +(generation + 1) + "：Best Fitness = " +  bestInGeneration.getFitness());

                Individual[] nextPopulation = new Individual[POPULATION_SIZE];
                for (int i = 0; i < ELITE_COUNT; i++) {
                    nextPopulation[i] = population[i];
                }

                // 交叉與突變...
                for (int i = ELITE_COUNT; i < POPULATION_SIZE; i++) {
                    Individual parent1 = selectParent(population);
                    Individual parent2 = selectParent(population);
                    nextPopulation[i] = crossover(parent1, parent2);
                    mutate(nextPopulation[i]);
                }
                population = nextPopulation;
            }

            writeToLog("AI Training Finished. Final Best Fitness: " + bestFitness);
            writeToLog("Best Genes have been saved to " + GENE_FILE);
            System.out.println("AI Training Finished. Final Best Fitness: " + bestFitness);
            System.out.println("Best Genes have been saved to " + GENE_FILE);
        }).join();
    }

    private Individual[] initializePopulation() {
        Individual[] population = new Individual[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = new Individual(GameSettings.GENE_SIZE);
            // *** 為新個體隨機化基因 ***
            for (int j = 0; j < GameSettings.GENE_SIZE; j++) {
                // 隨機數介於 -1.0 到 1.0 之間
                population[i].genes[j] = random.nextDouble() * 2 - 1;
            }
        }
        return population;
    }

    // 在類別初始化時計算 board 的理想值（不必每個個體都算）
    private int computeIdealStepsPerFood(int boardWidth, int boardHeight, double scaleFactor, int buffer) {
        double expectedDx = ((boardWidth + 1.0) * (boardWidth - 1.0)) / (3.0 * boardWidth);
        double expectedDy = ((boardHeight + 1.0) * (boardHeight - 1.0)) / (3.0 * boardHeight);
        double expectedManhattan = expectedDx + expectedDy;
        int upperClamp = (boardWidth - 1) + (boardHeight - 1);
        int ideal = (int) Math.ceil(expectedManhattan * scaleFactor + buffer);
        return Math.max(1, Math.min(ideal, upperClamp));
    }

    // 根據模擬結果計算個體的適應度
    private void evaluatePopulation(Individual[] population) {
        /*
        scaleFactor = 1.2：遊戲地形通暢、蛇較容易直達食物 → 懲罰較嚴格（期望步數低）
        scaleFactor = 1.5：一般情況（建議起始值）
        scaleFactor = 1.8 ~ 2.0：若蛇常被自己擋路、或障礙多，給更寬鬆的步數
         */
        double scaleFactor = 1.3;  // 放大因子：1.2~2.0 範圍常用，視遊戲難度調整
        int buffer = 3;           // 額外容忍步數(0~50)
        int idealStepsPerFood = computeIdealStepsPerFood(GameSettings.screenWidth, GameSettings.screenHeight, scaleFactor, buffer);

        for (Individual individual : population) {
            AI_Decision_Maker ai = new NeuralNetworkAI(individual.getGenes());
            SnakeSimulator.SimulationResult result = simulator.run(ai);

            double fitness = 0;
            if (result.score == 0) {
                // 沒吃到食物的懲罰減輕，但仍要鼓勵存活
//                fitness = Math.max(1, result.steps / 4); // 最少給1分，避免完全沒獎勵
                fitness = Math.log(result.steps + 1) * 2.0; // 對數增長的生存獎勵
            } else {
                // 效率計算
                double avgStepsPerFood = (double) result.steps / result.score;
                double idealSteps = 15.0; // 假設理想平均步數

                // 主要分數：食物 × 效率獎勵
                double efficiencyMultiplier = Math.max(0.1, idealSteps / avgStepsPerFood);
                if (efficiencyMultiplier > 1.0) {
                    efficiencyMultiplier = Math.pow(efficiencyMultiplier, 2.0); // 高效率平方獎勵
                }

                fitness = result.score * 1000.0 * efficiencyMultiplier;

                // 微小的生存獎勵（避免完全忽略）
                fitness += Math.log(result.steps + 1) * 5.0;

                // 長度獎勵
                fitness += Math.pow(result.score + 3, 1.2) * 50.0;
            }

            // 確保適應度不會是負數
            individual.setFitness(Math.max(1.0, fitness));
        }
    }

    // ============== 改進的選擇機制 ==============
    private Individual selectParent(Individual[] population) {
        // 使用錦標賽選擇，但增加多樣性
        Individual best = null;
        int tournamentSize = Math.max(3, POPULATION_SIZE / 20); // 動態錦標賽大小

        for (int i = 0; i < tournamentSize; i++) {
            Individual contender = population[random.nextInt(POPULATION_SIZE)];
            if (best == null || contender.getFitness() > best.getFitness()) {
                best = contender;
            }
        }
        return best;
    }

    // ============== 改進的交叉機制 ==============
    private Individual crossover(Individual parent1, Individual parent2) {
        Individual child = new Individual(GameSettings.GENE_SIZE);

        // 使用多點交叉而非單點交叉
        for (int i = 0; i < GameSettings.GENE_SIZE; i++) {
            if (random.nextDouble() < 0.5) {
                child.genes[i] = parent1.genes[i];
            } else {
                child.genes[i] = parent2.genes[i];
            }

            // 小幅度的隨機擾動，增加探索性
            if (random.nextDouble() < 0.1) {
                child.genes[i] += (random.nextGaussian() * 0.1);
                child.genes[i] = Math.max(-1.0, Math.min(1.0, child.genes[i])); // 限制範圍
            }
        }

        return child;
    }

    // ============== 改進的突變機制 ==============
    private void mutate(Individual individual) {
        // 自適應突變率
        double adaptiveMutationRate = MUTATION_RATE;

        for (int i = 0; i < GameSettings.GENE_SIZE; i++) {
            if (random.nextDouble() < adaptiveMutationRate) {
                // 使用高斯突變而非均勻突變
                double mutation = random.nextGaussian() * 0.2; // 標準差為0.2
                individual.genes[i] += mutation;
                individual.genes[i] = Math.max(-1.0, Math.min(1.0, individual.genes[i])); // 限制範圍
            }
        }
    }

    private double[] loadGenes() {
        NeuralNetwork nn = NeuralNetwork.loadFromFile(GENE_FILE,22, 16, 3);
        return (nn != null) ? nn.getGenome() : null;
    }

    // 將最佳基因儲存到檔案中
    private void saveBestGenes() {
        if (bestGenes != null) {
            // 根據您的神經網路結構，建立一個新的 NeuralNetwork 實例
            NeuralNetwork bestAI = new NeuralNetwork(22, 16, 3);
            // 將最佳基因設定為此神經網路的基因組
            bestAI.setGenome(bestGenes);
            // 呼叫實例方法來儲存檔案
            bestAI.saveToFile(GENE_FILE);
            System.out.println("Best gene save to: " + GENE_FILE);
        }
    }

    private void writeToLog(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

}