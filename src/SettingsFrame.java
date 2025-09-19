import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

public class SettingsFrame extends JFrame {

    // 將視窗模式和全螢幕模式的邏輯整合
    private final GamePanel gamePanel;
    private final Consumer<Void> restartAction;

    private static final String[] displayModes = {
            "800x600 (4:3)",
            "1025x775 (4:3)",
            "1200x900 (4:3)",
            "1280x720 (16:9)",
            "1920x1080 (16:9)",
    };

    public SettingsFrame(GamePanel gamePanel, Consumer<Void> restartAction) {
        this.gamePanel = gamePanel;
        this.restartAction = restartAction;

        this.setTitle("Settings");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setLocationRelativeTo(null);

        // 視窗關閉事件監聽器 (點擊 'X')
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // 如果是透過點擊視窗的 "X" 來關閉，則繼續遊戲
                gamePanel.resumeGame();
            }
        });

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Game Speed:"));
        JComboBox<String> speedComboBox = new JComboBox<>();
        for (int speed : GameSettings.GAME_SPEEDS) {
            speedComboBox.addItem(speed + "ms");
        }
        speedComboBox.setSelectedIndex(GameSettings.speedIndex);
        panel.add(speedComboBox);

        panel.add(new JLabel("Screen Mode:"));
        JComboBox<String> modeComboBox = new JComboBox<>(displayModes);
        modeComboBox.setSelectedIndex(GameSettings.sizeIndex);
        panel.add(modeComboBox);

        JPanel buttonPanel = new JPanel();

        // 套用並重啟
        JButton applyButton = new JButton("Apply & Restart");
        applyButton.addActionListener(e -> {
            GameSettings.speedIndex = speedComboBox.getSelectedIndex();
            GameSettings.sizeIndex = modeComboBox.getSelectedIndex();
            GameSettings.updateGameParameters();
            GameSettings.saveSettings();

            // 關閉當前視窗
            dispose();
            if (restartAction != null) {
                restartAction.accept(null); // 執行重啟
            }
        });

        // 取消調整設定參數
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            dispose(); // 直接關閉視窗，windowClosed監聽器會處理後續
        });

        // 將面板和按鈕新增到視窗
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        this.add(panel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.pack();
        this.setVisible(true);
    }
}