package ph.dlsu.edu.ccs.stdiscm.jgang.probset3;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ProducerConfig {
    private static final Properties CONFIG = new Properties();
    private static boolean initialized = false;

    public static boolean init() {
        if (initialized) return true;

        try {
            InputStream inputStream = ProducerConfig.class.getClassLoader().getResourceAsStream("producer.properties");
            if (inputStream == null) {
                System.err.println("Error: producer.properties not found in classpath");
                return false;
            }

            CONFIG.load(inputStream);
            initialized = true;
            return true;
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            return false;
        }
    }

    public static String get(String key) {
        return CONFIG.getProperty(key);
    }
}
