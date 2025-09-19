/*
負責啟動 GeneticAlgorithmAI，並提供一個介面來設定訓練的世代數或從上次的訓練進度繼續。
將訓練進度印在終端機上，同時將進度記錄到一個日誌檔案中。
在訓練結束後，將訓練好的基因儲存到 best_genes.txt 中。
 */

import javax.swing.*;

public class Train {
    public static void main(String[] args) {
        // 設定遊戲環境
//        GameSettings.screenWidth = 800;
//        GameSettings.screenHeight = 600;
        GameSettings.loadSettings();

        GeneticAlgorithmAI trainer = new GeneticAlgorithmAI(GameSettings.screenWidth, GameSettings.screenHeight);
        trainer.startTraining();

        // 在訓練完成後，啟動遊戲主程式
        SwingUtilities.invokeLater(() -> {
            GameSettings.loadSettings();
            new Main().createAndShowGUI();
        });
    }
}