import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class PopulationManager {

    // 演算法參數
    private final int populationSize;
    private final double mutationRate;
    private final Random random;

    // 儲存所有蛇的族群
    private ArrayList<NeuralNetwork> population;
    private ArrayList<Double> fitnessScores;

    public PopulationManager(int populationSize, double mutationRate) {
        this.populationSize = populationSize;
        this.mutationRate = mutationRate;
        this.random = new Random();
        this.population = new ArrayList<>();
        this.fitnessScores = new ArrayList<>();
    }

    // 初始化第一代隨機的族群
    public void initializePopulation(int numInputs, int numHidden, int numOutputs) {
        for (int i = 0; i < populationSize; i++) {
            NeuralNetwork brain = new NeuralNetwork(numInputs, numHidden, numOutputs);
            brain.randomizeGenome(); // 我們需要為 NeuralNetwork 增加這個方法
            population.add(brain);
        }
    }

    // 執行一個世代的訓練
    public void runGeneration() {
        fitnessScores.clear();

        // 讓族群中的每一條蛇都玩一次遊戲
        for (NeuralNetwork brain : population) {
            GamePanel trainer = new GamePanel(brain);
            double fitness = trainer.runSimulation();
            fitnessScores.add(fitness);
        }
    }

    // 取得當前族群中表現最好的蛇
    public NeuralNetwork getBestPerformer() {
        int bestIndex = 0;
        double maxFitness = -1.0;
        for (int i = 0; i < fitnessScores.size(); i++) {
            if (fitnessScores.get(i) > maxFitness) {
                maxFitness = fitnessScores.get(i);
                bestIndex = i;
            }
        }
        return population.get(bestIndex);
    }

    public double getBestFitness() {
        if (fitnessScores.isEmpty()) {
            return 0;
        }
        return Collections.max(fitnessScores);
    }

    // 根據適應度產生下一代
    public void createNextGeneration() {
        ArrayList<NeuralNetwork> nextGeneration = new ArrayList<>();

        // 複製最佳表現者 (精英主義)
        // 確保表現最好的蛇能直接進入下一代，這樣最好的基因就不會丟失
        nextGeneration.add(getBestPerformer());

        // 根據適應度進行選擇、交配與突變
        for (int i = 1; i < populationSize; i++) {
            // 選擇兩個父母
            NeuralNetwork parent1 = selectParent();
            NeuralNetwork parent2 = selectParent();

            // 進行交配
            NeuralNetwork child = crossover(parent1, parent2);

            // 進行突變
            mutate(child);

            nextGeneration.add(child);
        }

        // 將新世代替換舊世代
        population = nextGeneration;
    }

    // 選擇父母 (使用輪盤選擇法)
    private NeuralNetwork selectParent() {
        double totalFitness = 0;
        for (double fitness : fitnessScores) {
            totalFitness += fitness;
        }
        double pick = random.nextDouble() * totalFitness;
        double currentSum = 0;
        for (int i = 0; i < populationSize; i++) {
            currentSum += fitnessScores.get(i);
            if (currentSum > pick) {
                return population.get(i);
            }
        }
        return population.get(0); // 備用
    }

    // 交配 (單點交配)
    private NeuralNetwork crossover(NeuralNetwork parent1, NeuralNetwork parent2) {
        NeuralNetwork child = new NeuralNetwork(parent1.getNumInputs(), parent1.getNumHidden(), parent1.getNumOutputs());
        double[] genome1 = parent1.getGenome();
        double[] genome2 = parent2.getGenome();
        double[] childGenome = new double[genome1.length];

        // 隨機選擇一個交配點
        int crossoverPoint = random.nextInt(genome1.length);

        // 組合父母的基因組
        for (int i = 0; i < genome1.length; i++) {
            if (i < crossoverPoint) {
                childGenome[i] = genome1[i];
            } else {
                childGenome[i] = genome2[i];
            }
        }

        child.setGenome(childGenome);
        return child;
    }

    // 突變
    private void mutate(NeuralNetwork network) {
        double[] genome = network.getGenome();
        for (int i = 0; i < genome.length; i++) {
            if (random.nextDouble() < mutationRate) {
                // 在原始權重上增加一個小的隨機值
                genome[i] += random.nextGaussian() * 0.1;
            }
        }
        network.setGenome(genome);
    }
}