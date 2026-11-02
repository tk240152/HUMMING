package org.example.player;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

import java.util.function.Predicate;

import org.example.core.Entity;
import org.example.core.Game;
import org.example.state.GameState;
import org.example.core.DevMode;
import org.example.core.Input;
import org.example.media.Assets;
import org.example.media.Sound;

public class Player extends Entity {

    // 見た目の画像（resources/images/）。体と羽は同じ大きさ・同じ位置で重なるように描かれている。
    // 画像が無いときは、図形で描く（drawShapes）
    private static final String BODY_TEXTURE = "hummingbird_body";
    private static final String WING_TEXTURE = "hummingbird_wing";
    /** 画像の1辺の大きさ（px） */
    private static final double TEXTURE_SIZE = 2048;
    /** 画面に描くときの倍率（画像の 1px → 画面の何 px か）。大きくするとハチドリが大きくなる */
    private static final double TEXTURE_SCALE = 0.108;
    // 画像の中の位置（px）。くちばしの先を getBeakTip() の位置に合わせて描く
    private static final double BEAK_TIP_X = 1871, BEAK_TIP_Y = 845;
    private static final double EYE_X = 1242, EYE_Y = 811;
    /** 羽の付け根（羽ばたくときの回転の中心） */
    private static final double WING_ROOT_X = 860, WING_ROOT_Y = 1070;
    // 羽ばたきの速さと角度は player_config.properties の wing.flapsPerSecond・wing.flapDegrees
    /** 画像が無いときの図形のハチドリを拡大する倍率 */
    private static final double SHAPE_SCALE = 2.16;

    private final PlayerParams params;
    private double vx = 0;
    private double vy = 0;
    private double elapsedTime = 0;

    // 押下時間は左右・上下で独立させ、加速とブレーキの計算は共通にする。
    private final AxisAcceleration horizontalAcceleration = new AxisAcceleration();
    private final AxisAcceleration verticalAcceleration = new AxisAcceleration();
    
    private boolean rKeyWasPressed = false;

    // trueを返した場合は、通常の外見の代わりに描画する。
    private Predicate<GraphicsContext> appearanceRenderer;

    /** 一時的な外見を設定する。nullで通常の描画へ戻す。 */
    public void setAppearanceRenderer(Predicate<GraphicsContext> renderer) {
        appearanceRenderer = renderer;
    }

    // ノックバック（ぶつかって弾き飛ばされている間は操作できない。回転しながら飛ばされ、目が×になる）
    private boolean knockedBack = false;
    private double knockTime;
    private double knockSeconds;
    private double knockStartX, knockStartY;
    private double knockEndX, knockEndY;
    private double knockSpins;

    // 追い風など（ステージ2）。操作の効き方と、風で流される速さを外から変える
    private double accelScale = 1.0;
    private double maxSpeedScale = 1.0;   
    private double windVx = 0.0;          

    public void setGainScale(double accel, double maxSpeed) {
        accelScale = accel;
        maxSpeedScale = maxSpeed;
    }

    public void setWindVelocity(double vx) {
        windVx = vx;
    }

    public Player() {
        super("player", 86, 86);
        fallbackColor = Color.TEAL;
        this.params = new PlayerParams();
        x = params.startX;
        y = params.startY;
    }

    @Override
    public void update() {
        elapsedTime += Game.STEP;

        // [R] キーで設定ファイルをホットリロード
        boolean rIsDown = DevMode.ENABLED && Input.isDown(KeyCode.R); // 開発用
        if (rIsDown && !rKeyWasPressed) {
            params.reload();
            horizontalAcceleration.reset();
            verticalAcceleration.reset();
            System.out.println("[Player] 設定をリロードしました！");
        }
        rKeyWasPressed = rIsDown;

        // ノックバック中は操作を受け付けず、決まった動きだけをする
        if (knockedBack) {
            updateKnockback();
            return;
        }

        // 1. WASD / 矢印キー入力による加速度の加算

        boolean up = Input.isDown(KeyCode.W) || Input.isDown(KeyCode.UP);
        boolean down = Input.isDown(KeyCode.S) || Input.isDown(KeyCode.DOWN);
        boolean left = Input.isDown(KeyCode.A) || Input.isDown(KeyCode.LEFT);
        boolean right = Input.isDown(KeyCode.D) || Input.isDown(KeyCode.RIGHT);
        vx = horizontalAcceleration.update(vx, (right ? 1 : 0) - (left ? 1 : 0), params, accelScale);
        vy = verticalAcceleration.update(vy, (down ? 1 : 0) - (up ? 1 : 0), params, accelScale);

        // 2. 空気抵抗（慣性の減衰）
        vx *= params.baseFriction;
        vy *= params.baseFriction;   

        // 3. 最高速度の制限
        double speed = Math.hypot(vx, vy);               
        double limit = params.maxSpeed * maxSpeedScale;
        if (speed > limit) {
            vx = (vx / speed) * limit;
            vy = (vy / speed) * limit;
        }


        // 4. 慣性移動の芯となる座標を更新
        x += vx + windVx;
        y += vy;

        // 5. 機体の傾き（ピッチ制御）
        angle = Math.max(-params.maxTiltAngle, Math.min(params.maxTiltAngle, (vx + windVx) * params.tiltFactor));

        // 6. 画面端のバウンド処理
        double bounce = -params.wallBounce;
        if (x < width / 2) { x = width / 2; vx *= bounce; }
        if (x > Game.WIDTH - width / 2) { x = Game.WIDTH - width / 2; vx *= bounce; }
        if (y < height / 2) { y = height / 2; vy *= bounce; }
        if (y > Game.HEIGHT - height / 2) { y = Game.HEIGHT - height / 2; vy *= bounce; }
    }

    /** 1軸分の加速。速度そのものは Player が保持し、ここでは入力の継続時間だけを管理する。 */
    private static final class AxisAcceleration {
        private int direction;
        private double heldSeconds;

        private double update(double velocity, int inputDirection, PlayerParams params, double accelScale) {
            if (inputDirection == 0) {
                reset();
                return velocity; // キーを離しても慣性は残す。抵抗は従来どおり update() 側でかける。
            }
            if (inputDirection != direction) {
                reset();
                direction = inputDirection;
            }

            if (velocity * inputDirection < 0) {
                // 逆入力は減速だけに使う。0を越えて強い逆加速にならないようにする。
                heldSeconds = 0;
                double brake = Math.min(Math.abs(velocity), params.brakeAcc * accelScale);
                return velocity + inputDirection * brake;
            }

            // ブレーキ中は時間を蓄積しないので、停止後は必ず初期加速度から発進する。
            double acc = Math.min(params.maxAcc, params.baseAcc + params.accGrowthPerSecond * heldSeconds);
            heldSeconds += Game.STEP;
            return velocity + inputDirection * acc * accelScale;
        }

        private void reset() {
            direction = 0;
            heldSeconds = 0;
        }
    }

    /**
     * ノックバックさせる。seconds 秒かけて (dx, dy) だけ弾き飛ばされ、その間 spins 回転する。
     * その間は操作できず、目が×になる。すでにノックバック中なら何もしない。
     * dx・dy は px（右・下がプラス）。飛ばされる先は画面の中に収める。
     */
    public void knockback(double dx, double dy, double seconds, double spins) {
        if (knockedBack) return;
        Sound.se("damage"); // ぶつかった音（どのステージでも、弾き飛ばされ始めたときに1回）
        knockedBack = true;
        knockTime = 0;
        knockSeconds = Math.max(Game.STEP, seconds);
        knockSpins = spins;
        knockStartX = x;
        knockStartY = y;
        knockEndX = Math.clamp(x + dx, width / 2, Game.WIDTH - width / 2);
        knockEndY = Math.clamp(y + dy, height / 2, Game.HEIGHT - height / 2);
        vx = 0;
        vy = 0;
        horizontalAcceleration.reset();
        verticalAcceleration.reset();
    }

    /** ノックバック中か（操作できない間 true）。 */
    public boolean isKnockedBack() {
        return knockedBack;
    }

    private void updateKnockback() {
        knockTime += Game.STEP;
        double p = Math.min(1, knockTime / knockSeconds);
        double eased = 1 - (1 - p) * (1 - p); // 最初は速く、だんだんゆっくり止まる
        x = knockStartX + (knockEndX - knockStartX) * eased;
        y = knockStartY + (knockEndY - knockStartY) * eased;

        // 飛ばされる向きに転がるように回る（左へ飛ばされるなら反時計回り）
        double direction = knockEndX < knockStartX ? -1 : 1;
        angle = direction * 2 * Math.PI * knockSpins * eased;

        if (p >= 1) {
            knockedBack = false;
            angle = 0;
        }
    }

    public void addForce(double fx, double fy) {
        this.vx += fx;
        this.vy += fy;
    }

    /**
     * 画面に見えている体の中心。アイドル円運動のずれを含むので、x, y とは違うことがある。
     * 当たり判定（ぶつかる・ぶつからない）はこの位置を使う。
     */
    public Point2D getBodyCenter() {
        double circleOffsetX = Math.cos(elapsedTime * params.idleSpeed) * params.idleRadiusX;
        double circleOffsetY = Math.sin(elapsedTime * params.idleSpeed) * params.idleRadiusY;
        return new Point2D(x + circleOffsetX, y + circleOffsetY);
    }

    public Point2D getBeakTip() {
        // 円運動オフセットを考慮した先端位置
        double circleOffsetX = Math.cos(elapsedTime * params.idleSpeed) * params.idleRadiusX;
        double circleOffsetY = Math.sin(elapsedTime * params.idleSpeed) * params.idleRadiusY;
        
        double tipX = (x + circleOffsetX) + Math.cos(angle) * params.beakLength;
        double tipY = (y + circleOffsetY) + Math.sin(angle) * params.beakLength;
        return new Point2D(tipX, tipY);
    }

    public void writeTo(GameState state) {
        state.birdX = x;
        state.birdY = y;
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();
        
        // 【重要】描画の基準位置（x, y）に対して、常にサイン・コサインの円運動オフセットを足し合わせます
        double circleOffsetX = Math.cos(elapsedTime * params.idleSpeed) * params.idleRadiusX;
        double circleOffsetY = Math.sin(elapsedTime * params.idleSpeed) * params.idleRadiusY;
        
        gc.translate(x + circleOffsetX, y + circleOffsetY);
        gc.rotate(Math.toDegrees(angle));

        // 位置と傾きを合わせた状態で、専用の外見を描く。
        if (appearanceRenderer != null && appearanceRenderer.test(gc)) {
            gc.restore();
            return;
        }

        Image body = Assets.image(BODY_TEXTURE);
        if (body != null && !body.isError()) {
            drawTexture(gc, body, Assets.image(WING_TEXTURE));
        } else {
            drawShapes(gc);
        }

        gc.restore();
    }

    /** 羽の振れ具合（-1.0〜1.0）。1秒に wing.flapsPerSecond 回、上下に往復する。 */
    private double wingSwing() {
        return Math.sin(elapsedTime * 2 * Math.PI * params.wingFlapsPerSecond);
    }

    /** 画像の中の位置 (u, v) を、ハチドリの中心を原点とした画面の位置に変える。 */
    private double toLocalX(double u) {
        return params.beakLength + (u - BEAK_TIP_X) * TEXTURE_SCALE;
    }

    private double toLocalY(double v) {
        return (v - BEAK_TIP_Y) * TEXTURE_SCALE;
    }

    /** 画像で描く。くちばしの先がちょうど getBeakTip() の位置に来る。 */
    private void drawTexture(GraphicsContext gc, Image body, Image wing) {
        double left = toLocalX(0);
        double top = toLocalY(0);
        double size = TEXTURE_SIZE * TEXTURE_SCALE;

        gc.drawImage(body, left, top, size, size);

        // 目（ノックバック中は×を重ねる）
        if (knockedBack) {
            double ex = toLocalX(EYE_X);
            double ey = toLocalY(EYE_Y);
            gc.setStroke(Color.web("#2b2d42"));
            gc.setLineWidth(3.2);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeLine(ex - 6.5, ey - 6.5, ex + 6.5, ey + 6.5);
            gc.strokeLine(ex + 6.5, ey - 6.5, ex - 6.5, ey + 6.5);
        }

        // 羽ばたき（付け根を中心に回す）
        if (wing != null && !wing.isError()) {
            double rootX = toLocalX(WING_ROOT_X);
            double rootY = toLocalY(WING_ROOT_Y);
            gc.save();
            gc.translate(rootX, rootY);
            gc.rotate(wingSwing() * params.wingFlapDegrees);
            gc.drawImage(wing, left - rootX, top - rootY, size, size);
            gc.restore();
        }
    }

    /** 画像が無いときの図形のハチドリ。 */
    private void drawShapes(GraphicsContext gc) {
        // 図形の数値は 800×500 の画面のときのもの。1920×1080 に合わせて拡大して描く
        double s = SHAPE_SCALE;
        gc.scale(s, s);

        // 尻尾
        gc.setFill(Color.web("#0096c7"));
        gc.fillPolygon(new double[]{-18, -36, -32}, new double[]{4, -3, 14}, 3);

        // クチバシ
        gc.setStroke(Color.web("#4a4e69"));
        gc.setLineWidth(3.5);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(10, -1, params.beakLength / s, 0);

        // 胴体
        gc.setFill(Color.web("#38b000"));
        gc.fillOval(-24, -7, 40, 24);

        // お腹
        gc.setFill(Color.web("#d8f3dc"));
        gc.fillOval(-17, 1, 26, 14);

        // 頭
        gc.setFill(Color.web("#00b4d8"));
        gc.fillOval(-1, -15, 24, 24);

        // ほっぺ
        gc.setFill(Color.web("#ff758f"));
        gc.fillOval(11, -3, 8, 5);

        // 目（ノックバック中は×）
        if (knockedBack) {
            gc.setStroke(Color.web("#2b2d42"));
            gc.setLineWidth(2);
            gc.strokeLine(11, -9, 18, -2);
            gc.strokeLine(18, -9, 11, -2);
        } else {
            gc.setFill(Color.web("#2b2d42"));
            gc.fillOval(11, -9, 7, 7);
            gc.setFill(Color.WHITE);
            gc.fillOval(14, -8, 2.5, 2.5);
        }

        // 羽ばたき
        double wingAngle = wingSwing() * 50.0;
        gc.save();
        gc.translate(-6, -2);
        gc.rotate(wingAngle);
        gc.setFill(Color.rgb(144, 224, 239, 0.85));
        gc.setStroke(Color.web("#0077b6"));
        gc.setLineWidth(1.5);
        gc.fillOval(-9, -40, 18, 40);
        gc.strokeOval(-9, -40, 18, 40);
        gc.restore();
    }
}
