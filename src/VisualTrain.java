import javax.swing.*;
import java.util.concurrent.Semaphore;

public class VisualTrain {

    public static void main(String[] args) throws InterruptedException {
        // --- 載入遊戲設定 ---
        GameSettings.loadSettings();

        // --- 訓練參數設定 ---
        final int POPULATION_SIZE = 500;
        final double MUTATION_RATE = 0.05;
        final int GENERATIONS = 100;
        final String OUTPUT_FILE = "best_snake_model.txt";

        // --- 神經網路結構 ---
        final int INPUT_NODES = 6;
        final int HIDDEN_NODES = 8;
        final int OUTPUT_NODES = 3;

        // --- 建立 GUI 視窗 ---
        JFrame frame = new JFrame();
        frame.setTitle("AI Training Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        // 建立一個 GamePanel 用於顯示，一開始沒有 AI
        GamePanel visualPanel = new GamePanel(null);
        frame.add(visualPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // --- 建立訓練管理器和信號量 ---
        PopulationManager manager = new PopulationManager(POPULATION_SIZE, MUTATION_RATE);
        manager.initializePopulation(INPUT_NODES, HIDDEN_NODES, OUTPUT_NODES);

        // 建立一個信號量，初始許可為0，表示需要等待 release()
        Semaphore gameSemaphore = new Semaphore(0);
        visualPanel.setGameSemaphore(gameSemaphore);

        System.out.println("視覺化訓練開始...");
        System.out.println("==============================================");

        // --- 訓練迴圈 ---
        for (int generation = 0; generation < GENERATIONS; generation++) {
            // 1. 幕後執行一代的模擬 (無畫面)
            manager.runGeneration();
            System.out.printf("世代 %d / %d 完成。 最佳適應度: %.2f\n",
                    (generation + 1), GENERATIONS, manager.getBestFitness());

            // 2. 取得這一代表現最好的 AI
            NeuralNetwork bestBrain = manager.getBestPerformer();

            // 3. 在 GUI 上展示這一代冠軍的表現
            System.out.println("正在展示第 " + (generation + 1) + " 代的最佳表現...");
            // 使用 invokeLater 確保 GUI 更新在正確的執行緒上
            SwingUtilities.invokeLater(() -> visualPanel.setBrain(bestBrain));

            // 4. 等待畫面上的遊戲結束 (等待 GamePanel 釋放信號)
            gameSemaphore.acquire();
            System.out.println("展示結束。");

            // 5. 產生下一代
            manager.createNextGeneration();
        }

        System.out.println("==============================================");
        System.out.println("訓練結束！");

        // --- 儲存最終表現最好的模型 ---
        NeuralNetwork finalBestBrain = manager.getBestPerformer();
        finalBestBrain.saveGenome(OUTPUT_FILE);

        // 關閉視窗
        frame.dispose();
    }
}