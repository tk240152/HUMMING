package org.example.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.example.core.ProjectFiles;

public class PlayerParams {
    
    private static final File CONFIG_FILE = ProjectFiles.resolve("player_config.properties").toFile();

    public double startX = 360.0;
    public double startY = 540.0;
    public double beakLength = 99.0;

    public double baseAcc = 0.475;
    public double maxAcc = baseAcc;
    public double accGrowthPerSecond = 0.0;
    public double brakeAcc = baseAcc;
    public double baseFriction = 0.955;
    public double maxSpeed = 17.3;

    public double tiltFactor = 0.0255;
    public double maxTiltAngle = 0.45;

    public double hoverBobAmount = 0.04;
    public double hoverBobSpeed = 6.0;

    public double wallBounce = 0.4;

    // 👇 ここにアイドル用の変数が定義されている必要があります
    public double idleSpeed = 3.2;
    public double idleRadiusX = 1.7;
    public double idleRadiusY = 3.5;

    // 羽ばたき
    public double wingFlapsPerSecond = 6.7;   // 1秒に羽ばたく回数
    public double wingFlapDegrees = 25.0;     // 羽が上下に振れる角度（度）。画像の羽で使う

    public PlayerParams() {
        reload();
    }

    public boolean reload() {
        if (!CONFIG_FILE.exists()) {
            createDefaultFile(CONFIG_FILE);
        }

        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            props.load(reader);

            startX = parseDouble(props, "bird.startX", startX);
            startY = parseDouble(props, "bird.startY", startY);
            beakLength = parseDouble(props, "bird.beakLength", beakLength);

            baseAcc = parseNonNegativeDouble(props, "bird.baseAcc", baseAcc);
            // 新しい項目が無い設定ファイルは、従来と同じ固定加速度を使う。
            maxAcc = Math.max(baseAcc, parseNonNegativeDouble(props, "bird.maxAcc", baseAcc));
            accGrowthPerSecond = parseNonNegativeDouble(props, "bird.accGrowthPerSecond", 0.0);
            brakeAcc = parseNonNegativeDouble(props, "bird.brakeAcc", baseAcc);
            baseFriction = parseDouble(props, "bird.baseFriction", baseFriction);
            maxSpeed = parseDouble(props, "bird.maxSpeed", maxSpeed);

            tiltFactor = parseDouble(props, "bird.tiltFactor", tiltFactor);
            maxTiltAngle = parseDouble(props, "bird.maxTiltAngle", maxTiltAngle);

            hoverBobAmount = parseDouble(props, "bird.hoverBobAmount", hoverBobAmount);
            hoverBobSpeed = parseDouble(props, "bird.hoverBobSpeed", hoverBobSpeed);

            wallBounce = parseDouble(props, "bird.wallBounce", wallBounce);

            // 👇 プロパティファイルからの読み込み
            idleSpeed = parseDouble(props, "idle.speed", idleSpeed);
            idleRadiusX = parseDouble(props, "idle.radiusX", idleRadiusX);
            idleRadiusY = parseDouble(props, "idle.radiusY", idleRadiusY);

            wingFlapsPerSecond = parseDouble(props, "wing.flapsPerSecond", wingFlapsPerSecond);
            wingFlapDegrees = parseDouble(props, "wing.flapDegrees", wingFlapDegrees);

            System.out.println("[PlayerParams] 設定ファイルを再読み込みしました！");
            return true;
        } catch (Exception e) {
            System.err.println("[PlayerParams] 読み込みエラー: " + e.getMessage());
            return false;
        }
    }

    private double parseDouble(Properties props, String key, double defaultVal) {
        String val = props.getProperty(key);
        if (val == null) return defaultVal;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private double parseNonNegativeDouble(Properties props, String key, double defaultVal) {
        double value = parseDouble(props, key, defaultVal);
        return Double.isFinite(value) && value >= 0 ? value : defaultVal;
    }

    private void createDefaultFile(File file) {
        String content =
                "bird.startX = 150.0\n" +
                "bird.startY = 250.0\n" +
                "bird.beakLength = 46.0\n" +
                "bird.baseAcc = 0.22\n" +
                "bird.maxAcc = 5.0\n" +
                "bird.accGrowthPerSecond = 2.0\n" +
                "bird.brakeAcc = 4.0\n" +
                "bird.baseFriction = 0.955\n" +
                "bird.maxSpeed = 8.0\n" +
                "bird.tiltFactor = 0.055\n" +
                "bird.maxTiltAngle = 0.45\n" +
                "bird.hoverBobAmount = 0.04\n" +
                "bird.hoverBobSpeed = 6.0\n" +
                "bird.wallBounce = 0.4\n" +
                "idle.speed = 3.2\n" +
                "idle.radiusX = 0.8\n" +
                "idle.radiusY = 1.6\n" +
                "wing.flapsPerSecond = 6.7\n" +
                "wing.flapDegrees = 25.0\n";
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        } catch (Exception ignored) {}
    }
}
