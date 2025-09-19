import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

// 繼承 AI_Decision_Maker 介面
public class GeneticAlgorithmAI {

    // 演算法參數
    private static final int POPULATION_SIZE = 100;  // 族群大小
//    private static final int GENE_SIZE = 9;         // 基因長度（與食物、牆壁、身體的距離權重，每個因素各有三個方向）
    private static final double MUTATION_RATE = 0.1; // 突變機率
    private static final int MAX_GENERATIONS = 500; // 最大世代數

    // 遊戲環境參數（與 GamePanel 相同）
    private static final int UNIT_SIZE = 25;

    // 遊戲環境參數
//    private static final int GAME_WIDTH = 800; // 你可以根據需求調整
//    private static final int GAME_HEIGHT = 600; // 你可以根據需求調整
    private static final int GAME_WIDTH = GameSettings.screenWidth;
    private static final int GAME_HEIGHT = GameSettings.screenHeight;

    // 隨機數生成器
    private static final Random random = new Random();
    // 訓練好的最佳基因
    private double[] bestGenes;
    private TrainingCompletionListener listener;

    public GeneticAlgorithmAI(TrainingCompletionListener listener) {
        this.listener = listener;
        // 注意：建構子中不再進行訓練，它只負責初始化
    }

    // 定義一個回呼介面，用於在訓練完成時通知遊戲
    public interface TrainingCompletionListener {
        void onTrainingComplete(double[] bestGenes);
    }

//    // 個體類別，代表一條蛇和它的基因
//    private static class Individual {
//        double[] genes = new double[GameSettings.GENE_SIZE]; // 基因組：權重
//        int fitness; // 適應度：遊戲得分
//
//        // You can also add a constructor for easy initialization.
//        public Individual(int geneSize) {
//            this.genes = new double[geneSize];
//        }
//    }

    // 訓練紀錄
    private static class TrainingRecord {
        int generation;
        double fitness;
        double[] genes;

        public TrainingRecord(int generation, double fitness, double[] genes) {
            this.generation = generation;
            this.fitness = fitness;
            this.genes = genes;
        }
    }

    /**
     * 遺傳演算法訓練流程
     */
    public void startTraining() { // 使用 CompletableFuture 在背景執行緒中執行訓練
        CompletableFuture.runAsync(() -> {
            Individual[] population = new Individual[POPULATION_SIZE];
            for (int i = 0; i < POPULATION_SIZE; i++) {
                population[i] = new Individual(GameSettings.GENE_SIZE);
                for (int j = 0; j < GameSettings.GENE_SIZE; j++) {
                    population[i].genes[j] = random.nextDouble() * 2 - 1;
                }
            }

            for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
//                for (Individual individual : population) {
//                    individual.fitness = simulateGame(individual.genes);
//                }
                // 2. Evaluation - Call the new, refactored method here.
                evaluatePopulation(population);

                Arrays.sort(population, Comparator.comparingInt(i -> i.fitness));

                // 記錄當前世代的最佳適應度和基因
                Individual bestIndividual = population[POPULATION_SIZE - 1];
//                trainingRecords.add(new TrainingRecord(generation, bestIndividual.fitness, bestIndividual.genes));
                System.out.println("Generation " + generation + "：Best Fitness = " + bestIndividual.fitness);

                // 交叉與突變...
                Individual[] nextPopulation = new Individual[POPULATION_SIZE];
                nextPopulation[0] = population[POPULATION_SIZE - 1];
                for (int i = 1; i < POPULATION_SIZE; i++) {
                    Individual parent1 = population[random.nextInt(POPULATION_SIZE / 2) + POPULATION_SIZE / 2];
                    Individual parent2 = population[random.nextInt(POPULATION_SIZE / 2) + POPULATION_SIZE / 2];

                    Individual child = new Individual(GameSettings.GENE_SIZE);
                    int crossoverPoint = random.nextInt(GameSettings.GENE_SIZE);
                    for (int j = 0; j < GameSettings.GENE_SIZE; j++) {
                        child.genes[j] = (j < crossoverPoint) ? parent1.genes[j] : parent2.genes[j];
                    }
                    if (random.nextDouble() < MUTATION_RATE) {
                        child.genes[random.nextInt(GameSettings.GENE_SIZE)] = random.nextDouble() * 2 - 1;
                    }
                    nextPopulation[i] = child;
                }
                population = nextPopulation;
                System.out.println("Generation " + generation + "：Best Fitness = " + population[0].fitness);
            }

            // 4. Finalization
            Individual finalBestIndividual = population[POPULATION_SIZE - 1];
            this.bestGenes = finalBestIndividual.getGenes();

//            this.bestGenes = population[0].genes;
            System.out.println("AI Training Finished. Best Genes: " + Arrays.toString(bestGenes));

//            exportTrainingHistory();

            // 訓練完成後，通知遊戲
            if (listener != null) {
                listener.onTrainingComplete(bestGenes);
            }
        });
    }

    /**
     *注意：這裡需要一個完整的遊戲模擬器**
     * 模擬遊戲來評估基因的適應度
     * 這是你論文的重點：你需要設計一個能夠讓蛇根據基因移動的演算法
//     * @param genes 基因組
     * @return 遊戲得分（適應度）
     */
    private int simulateGame(AI_Decision_Maker ai) {
        SnakeSimulator simulator = new SnakeSimulator(GameSettings.screenWidth, GameSettings.screenHeight);
        SnakeSimulator.SimulationResult result = simulator.run(ai);
        // 適應度計算公式：得分*1000 + 步數
        int fitness = 0;
        fitness += result.score * 1000;
        fitness += result.steps * 300;
        return fitness;
    }
    // This method's sole purpose is to evaluate the fitness of the current population.
    private void evaluatePopulation(Individual[] population) {
        for (Individual individual : population) {
            // Here, we create an AI for this specific individual's genes
            AI_Decision_Maker ai = new NeuralNetworkAI(individual.getGenes());

            // And then simulate the game with that AI to get its fitness
            individual.setFitness(simulateGame(ai));
        }
    }

//    @Override
//    public char decideDirection(List<Point> snakeBody, Point food, Dimension boardSize) {
//        // 你需要在這裡實現根據基因決策的邏輯
//        // 這是你論文的重點之一，需要仔細設計
//        if (bestGenes == null) {
//            // 如果還沒訓練好，使用隨機方向
//            return new char[]{'U', 'D', 'L', 'R'}[random.nextInt(4)];
//        }
//
//        return getBestDirection(bestGenes, snakeBody, food, boardSize);
//    }

    // 將訓練歷史匯出至 JSON 檔案
//    private void exportTrainingHistory() {
//        Gson gson = new Gson();
//        try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
//            gson.toJson(trainingRecords, writer);
//            System.out.println("訓練記錄已匯出至 " + HISTORY_FILE);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}