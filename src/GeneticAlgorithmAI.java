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

    private static final int POPULATION_SIZE = 700; // 族群大小
    private static final double MUTATION_RATE = 0.05; //通常突變率會設定在 1% 到 5% (0.01 ~ 0.05) 之間。過高的突變率會讓演算法難以收斂到最佳解；過低則可能陷入局部最優解
    private static final int MAX_GENERATIONS = 10000; // 最大世代數
    private static final int ELITE_COUNT = (int) (POPULATION_SIZE * 0.1); // 保留前 10% 的精英。這是一個非常標準且有效的做法（稱為「精英主義」），可以確保每一代的最優個體不會因為交叉或突變而丟失。

    private final Random random = new Random();
    // 訓練好的最佳基因
    private double[] bestGenes;
    private int bestFitness = -1;
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

                Arrays.sort(population, Comparator.comparingInt(Individual::getFitness).reversed());

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

    // 根據模擬結果計算個體的適應度
    private int calculateFitness(SnakeSimulator.SimulationResult result) {
        int fitness = 0;
        // 增加分數的權重，並扣除步數，鼓勵蛇吃食物且盡可能走得久0m
        fitness += result.score * 1000;
        fitness += result.steps * 1;

        // 給予「活著」的懲罰，鼓勵蛇更有效率地尋找食物
        // 這可以避免蛇漫無目的的活著而不去尋找食物
        fitness -= result.steps;

        // 如果在達到最大步數前沒有吃到食物，適應度會被扣分
        if(result.score == 0 && result.steps >= GameSettings.MAX_STEPS) {
            fitness = fitness / 10;
        }
        
        return fitness;
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

    private void evaluatePopulation(Individual[] population) {
        for (Individual individual : population) {
            AI_Decision_Maker ai = new NeuralNetworkAI(individual.getGenes());

//            individual.setFitness(simulator.run(ai).fitness);
            SnakeSimulator.SimulationResult result = simulator.run(ai);
            int fitness = 0;
//            fitness = (result.score) * 1000 - result.steps;
            fitness = result.score * 1000 + result.steps * 10;

            if(result.steps >= 50) {
                fitness -= 500;
            }
            individual.setFitness(fitness);
        }
    }

    private Individual selectParent(Individual[] population) {
        Individual best = null;
        for (int i = 0; i < 5; i++) {
            Individual contender = population[random.nextInt(POPULATION_SIZE)];
            if (best == null || contender.getFitness() > best.getFitness()) {
                best = contender;
            }
        }
        return best;
    }

    private Individual crossover(Individual parent1, Individual parent2) {
        Individual child = new Individual(GameSettings.GENE_SIZE);
        int crossoverPoint = random.nextInt(GameSettings.GENE_SIZE);
        for (int i = 0; i < GameSettings.GENE_SIZE; i++) {
            child.genes[i] = (i < crossoverPoint) ? parent1.genes[i] : parent2.genes[i];
        }
        return child;
    }

    private void mutate(Individual individual) {
        if (random.nextDouble() < MUTATION_RATE) {
            individual.genes[random.nextInt(GameSettings.GENE_SIZE)] = random.nextDouble() * 2 - 1;
        }
    }

    private double[] loadGenes() {
        NeuralNetwork nn = NeuralNetwork.loadFromFile(GENE_FILE,24, 16, 3);
        return (nn != null) ? nn.getGenome() : null;
    }

    // 將最佳基因儲存到檔案中
    private void saveBestGenes() {
        if (bestGenes != null) {
            // 根據您的神經網路結構，建立一個新的 NeuralNetwork 實例
            NeuralNetwork bestAI = new NeuralNetwork(24, 16, 3);
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