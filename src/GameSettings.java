import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class GameSettings {

    private static final String SETTINGS_FILE = "game_settings.properties";

    public static int gameSpeed = 75;
    public static int screenWidth = 800;
    public static int screenHeight = 600;

    // 讀取設定
    public static void loadSettings() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            props.load(fis);
            gameSpeed = Integer.parseInt(props.getProperty("gameSpeed", "75"));
            screenWidth = Integer.parseInt(props.getProperty("screenWidth", "800"));
            screenHeight = Integer.parseInt(props.getProperty("screenHeight", "600"));
        } catch (IOException | NumberFormatException e) {
            System.err.println("Can't find the setting file, will use default setting.");
        }
    }

    // 寫入設定
    public static void saveSettings() {
        Properties props = new Properties();
        props.setProperty("gameSpeed", String.valueOf(gameSpeed));
        props.setProperty("screenWidth", String.valueOf(screenWidth));
        props.setProperty("screenHeight", String.valueOf(screenHeight));

        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "Game Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}