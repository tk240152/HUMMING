package org.example.state;

/**
 * 担当どうしでやり取りするデータ（3人の約束）。
 * Player と game/ が書き込み、ui/ は読むだけ。
 * 項目を増やす・変えるときは 3人全員でレビューすること。
 */
public class GameState {

    public enum Status {
        FLYING, APPROACH, TOO_FAST, FEEDING, CLEAR
    }

    // プレイヤー（Player が書く）
    public double birdX, birdY;
    public double birdVx, birdVy;

    // 進行状況（game/ が書く）
    public double nectar;
    public double maxNectar = 100;
    public double distance;
    public double relSpeed;
    public Status status = Status.FLYING;

    // クリアの記録（game/ がクリアしたときに書く。リザルト画面で使う）
    /** 遊んでいるステージの番号 */
    public int stage = 1;
    /** 今回のクリアタイム（秒） */
    public double clearTime;
    /** このステージのベストタイム（秒）。今回の記録を反映したあとの値 */
    public double bestTime;
    /** 今回ベストタイムを更新したか（NEW RECORD の表示に使う） */
    public boolean newBest;
    /** 遊んだステージの背景の画像のキー（リザルト画面の背景に使う）。null ならリザルトの共通の背景 */
    public String backgroundKey;

    /** ランクの名前（速い順）。rankTimes はこの順に、D を除いた分の秒数を持つ */
    public static final String[] RANKS = {"SS", "S", "A", "B", "C", "D"};

    /** いつものランクの基準（秒）。SS・S・A・B・C の順 */
    public static final double[] DEFAULT_RANK_TIMES = {30, 40, 60, 80, 100};

    /** このステージのランクの基準（秒）。SS・S・A・B・C の順。ステージの stage.json の rank から GameScene が入れる */
    public double[] rankTimes = DEFAULT_RANK_TIMES.clone();

    /** 今回のクリアタイムに応じた6段階ランク（このステージの基準で判定する）。 */
    public String getRank() {
        return rankFor(clearTime, rankTimes);
    }

    /** 指定された秒数からランクを求める（いつもの基準で判定する）。 */
    public static String rankFor(double seconds) {
        return rankFor(seconds, DEFAULT_RANK_TIMES);
    }

    /** 指定された秒数からランクを求める。times は SS・S・A・B・C の基準（秒）。どれより遅ければ D。 */
    public static String rankFor(double seconds, double[] times) {
        for (int i = 0; i < times.length && i < RANKS.length - 1; i++) {
            if (seconds < times[i]) return RANKS[i];
        }
        return "D";
    }
    /**
     * 【デバッグ用】このステージのベストタイム記録をリセットし、確実に NEW RECORD 演出が出る状態にする。
     */
    public void resetBestTime() {
        // ベストタイムを十分大きい値（未記録扱い）に上書き
        this.bestTime = 999.99;
        this.newBest = true;

        // ※もしファイル等に保存しているセーブデータも消したい場合は、
        // ここで StageSave.clearBest(stage); のようなセーブ削除処理を呼び出します。
    }
}