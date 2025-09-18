import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SettingsFrame extends JFrame {
    private GamePanel gamePanel; // 新增：用來儲存 GamePanel 的參考

    // 將視窗模式和全螢幕模式的邏輯整合
    private static final String[] displayModes = {
            "800x600 (無邊框視窗)",
            "1025x775 (無邊框視窗)",
            "1200x900 (無邊框視窗)",
            "1280x720 (無邊框視窗)",
            "1920x1080 (全螢幕)",
    };

    public SettingsFrame(GamePanel gamePanel) {
        this.gamePanel = gamePanel; // 接收 GamePanel 實例

        this.setTitle("Setting");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setLocationRelativeTo(null);

        // 視窗關閉事件監聽器
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // 如果是透過點擊視窗的 "X" 來關閉，則繼續遊戲
                if (gamePanel.paused) {
                    gamePanel.resumeGame();
                }
            }
        });

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 速度設定
        JLabel speedLabel = new JLabel("Speed (ms):");
        String[] speedOptions = new String[GameSettings.GAME_SPEEDS.length];
        for (int i = 0; i < GameSettings.GAME_SPEEDS.length; i++) {
            speedOptions[i] = GameSettings.GAME_SPEEDS[i] + " ms";
        }
        JComboBox<String> speedComboBox = new JComboBox<>(speedOptions);
        speedComboBox.setSelectedIndex(GameSettings.speedIndex);
        panel.add(speedLabel);
        panel.add(speedComboBox);

        // --- 畫面模式設定 ---
        JLabel modeLabel = new JLabel("畫面模式:");
        JComboBox<String> modeComboBox = new JComboBox<>(displayModes);
        // 根據目前的設定來選擇正確的模式
        int currentSizeIndex = GameSettings.sizeIndex;
        if (GameSettings.screenWidth == 1920 && GameSettings.screenHeight == 1080) {
            // 處理全螢幕情況
            modeComboBox.setSelectedIndex(4);
        } else {
            // 處理視窗情況
            // 遍歷所有視窗選項以找到對應的索引
            for(int i = 0; i < 4; i++) {
                if (GameSettings.screenWidth == GameSettings.SCREEN_SIZES[i][0] &&
                        GameSettings.screenHeight == GameSettings.SCREEN_SIZES[i][1]) {
                    modeComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        panel.add(modeLabel);
        panel.add(modeComboBox);

        JPanel buttonPanel = new JPanel(new FlowLayout());

        // 確認按鈕
        JButton applyButton = new JButton("Save and Restart");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // 更新 GameSettings 的值
                    GameSettings.speedIndex = speedComboBox.getSelectedIndex();
                    GameSettings.sizeIndex = modeComboBox.getSelectedIndex();

                    // 更新參數並儲存
                    GameSettings.updateGameParameters();
                    GameSettings.saveSettings();
                    // 呼叫 GamePanel 的方法來重新啟動遊戲
                    gamePanel.applyAndRestart();
                    // 關閉當前視窗
                    dispose();

                    // 重新啟動遊戲
                    restartGame();

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Invalid Type Param！", "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 取消調整設定參數
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            dispose(); // 關閉視窗，windowClosed 會觸發繼續遊戲
        });

        // 將面板和按鈕新增到視窗
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        this.add(panel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.pack();
        this.setVisible(true);
    }

    private void restartGame() {
        // 關閉所有現有視窗
        for (Window window : Window.getWindows()) {
            window.dispose();
        }
        // 重新啟動遊戲
        new SnakeGame();
    }
}