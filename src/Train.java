public class Train {

    public static void main(String[] args) {
        GameSettings.loadSettings();
        // --- 訓練參數設定 ---
        final int POPULATION_SIZE = 500;
        final double MUTATION_RATE = 0.05;
        final int GENERATIONS = 100; // 你可以增加代數以獲得更好的結果
        final String OUTPUT_FILE = "best_snake_model.txt"; // 儲存模型檔案的名稱

        // --- 神經網路結構 ---
        final int INPUT_NODES = 6;
        final int HIDDEN_NODES = 8;
        final int OUTPUT_NODES = 3;

        // --- 開始訓練 ---
        PopulationManager manager = new PopulationManager(POPULATION_SIZE, MUTATION_RATE);
        manager.initializePopulation(INPUT_NODES, HIDDEN_NODES, OUTPUT_NODES);
        System.out.println("AI 訓練開始...");
        System.out.println("==============================================");

        for (int generation = 0; generation < GENERATIONS; generation++) {
            manager.runGeneration();
            System.out.printf("世代 %d / %d 完成。 最佳適應度: %.2f\n",
                    (generation + 1), GENERATIONS, manager.getBestFitness());
            manager.createNextGeneration();
        }

        System.out.println("==============================================");
        System.out.println("訓練結束！");

        // --- 儲存表現最好的模型 ---
        NeuralNetwork bestBrain = manager.getBestPerformer();
        bestBrain.saveGenome(OUTPUT_FILE);
    }
}