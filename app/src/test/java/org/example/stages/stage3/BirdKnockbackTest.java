package org.example.stages.stage3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.example.core.Game;
import org.example.state.GameState;
import org.example.game.flower.Flower;
import org.example.game.flower.FlowerConfig;
import org.example.game.flower.Wind;
import org.example.game.event.EventContext;
import org.example.game.event.EventTypes;
import org.example.game.event.StageEvents;
import org.example.player.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 渡り鳥とハチドリがぶつかったときのノックバック。 */
class BirdKnockbackTest {

    // Player を作ると、テストを動かしている場所（app/）に player_config.properties が作られるので、あとで消す
    private static final Path STRAY_CONFIG = Path.of("player_config.properties");
    private static boolean existedBefore;

    @BeforeAll
    static void remember() {
        existedBefore = Files.exists(STRAY_CONFIG);
    }

    @AfterAll
    static void cleanUp() throws Exception {
        if (!existedBefore) Files.deleteIfExists(STRAY_CONFIG);
    }

    @Test
    void playerKnockbackMovesSpinsAndBlocksControl() {
        Player p = new Player();
        p.x = 400;
        p.y = 200;
        double w = p.width;
        p.knockback(-3 * w, 3 * w, 0.8, 3);
        assertTrue(p.isKnockedBack());

        double maxTurn = 0;
        for (int i = 0; i < 48; i++) { // 0.8秒
            p.update();
            maxTurn = Math.max(maxTurn, Math.abs(p.angle));
        }
        assertFalse(p.isKnockedBack());                 // 0.8秒で操作できるようになる
        assertEquals(400 - 3 * w, p.x, 1e-6);           // 左へプレイヤー3体分
        assertEquals(200 + 3 * w, p.y, 1e-6);           // 下へプレイヤー3体分
        assertTrue(maxTurn > 2 * Math.PI * 2.9, "3回転していない: " + maxTurn);
        assertEquals(0, p.angle, 1e-9);                 // 終わったら向きは元どおり
    }

    @Test
    void flockFliesAtPlayerHeight() {
        Player player = new Player();
        FlowerConfig c = new FlowerConfig();
        EventContext ctx = new EventContext(new GameState(), player, new Flower(c), c, new Wind());

        player.y = 330;
        MigratingBirdsEvent birds = new MigratingBirdsEvent(Map.of("type", "birds"));
        birds.onStart(ctx);
        assertEquals(330, birds.centerY, 1e-9); // 現れた瞬間のプレイヤーの高さ

        player.y = 5; // 画面のいちばん上でも、群れははみ出さない
        birds.onStart(ctx);
        assertEquals(birds.flockSize / 2, birds.centerY, 1e-9);

        player.y = 432;
        MigratingBirdsEvent shifted = new MigratingBirdsEvent(Map.of("type", "birds", "yOffset", -108.0));
        shifted.onStart(ctx);
        assertEquals(324, shifted.centerY, 1e-9); // yOffset でずらせる
    }

    @Test
    void birdHittingPlayerKnocksBackOnlyPlayerOncePerPass() {
        Player player = new Player();
        FlowerConfig c = new FlowerConfig();
        EventContext ctx = new EventContext(new GameState(), player, new Flower(c), c, new Wind());
        player.x = 400;
        player.y = 250;

        MigratingBirdsEvent[] created = new MigratingBirdsEvent[1];
        EventTypes types = EventTypes.common();
        types.register("birds", p -> created[0] = new MigratingBirdsEvent(p));
        // 1羽が、プレイヤーの高さをちょうど通る
        StageEvents events = StageEvents.create(List.of(Map.of(
                "type", "birds", "duration", 3.0, "count", 1.0, "flockScale", 1.2,
                "minY", 250.0, "maxY", 250.0)), types);

        double t = 0;
        boolean everKnocked = false;
        for (int i = 0; i < 60 * 3; i++, t += Game.STEP) {
            events.update(ctx, t);
            if (!player.isKnockedBack() && !everKnocked) {
                player.x = 400; // 操作していない間は同じ場所で待つ
                player.y = 250;
            }
            player.update();
            everKnocked |= player.isKnockedBack();
        }
        assertTrue(everKnocked, "ぶつかったのにノックバックしなかった");
        assertTrue(created[0].hitPlayer);
        assertTrue(player.x < 400 && player.y > 250, "左下へ飛ばされていない: " + player.x + ", " + player.y);
    }
}
