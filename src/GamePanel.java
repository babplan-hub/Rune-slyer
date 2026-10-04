import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.GradientPaint;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.io.File;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements KeyListener {

    // =========================================================
    // GAME SIZE
    // =========================================================

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;

    // =========================================================
    // MAP
    // =========================================================

    private BufferedImage forest;

    private BufferedImage[] bossRooms = new BufferedImage[4];

    private boolean inBossDungeon = false;

    // =========================================================
    // PLAYER
    // =========================================================

    private Player player;

    // =========================================================
    // MONSTER
    // =========================================================

    private static final int MAX_MONSTERS = 8;

    private Monster[] monsters =
            new Monster[MAX_MONSTERS];

    // =========================================================
    // BOSS
    // =========================================================

    private Boss boss;

    private boolean bossAttackHitApplied = false;

    private int bossesDefeated = 0;

    private boolean gameStarted = false;

    private boolean gameOver = false;

    private boolean gameWon = false;

    private Summon summon;

    private long summonAvailableAt = 0;

    private String statusMessage = "";

    private long statusMessageUntil = 0;

    // =========================================================
    // RANDOM
    // =========================================================

    private Random random =
            new Random();

    // =========================================================
    // MONSTER SPAWN
    // =========================================================

    private static final int MONSTER_SIZE = 60;

    private static final int SPAWN_MARGIN = 50;

    // =========================================================
    // MONSTER SEPARATION
    // =========================================================

    private static final int SEPARATION_DISTANCE = 90;

    private static final int SEPARATION_FORCE = 2;

    // =========================================================
    // MONSTER RESPAWN
    // =========================================================

    private static final long RESPAWN_TIME = 3000;

    private long[] respawnStartTime =
            new long[MAX_MONSTERS];

    // =========================================================
    // ATTACK CONTROL
    // =========================================================

    private boolean[] attackHitApplied =
            new boolean[MAX_MONSTERS];

    // =========================================================
    // ITEM TYPE
    // =========================================================

    private static final int POTION =
            Item.POTION;

    private static final int RUNE =
            Item.RUNE;

    // =========================================================
    // ITEM DROP
    // =========================================================

    private List<DropItem> items =
            new ArrayList<>();

    // =========================================================
    // DROP CONTROL
    // =========================================================

    private boolean[] dropCreated =
            new boolean[MAX_MONSTERS];

    // =========================================================
    // UI
    // =========================================================

    private static final int UI_X = 25;
    private static final int UI_Y = 25;

    private static final int HP_BAR_WIDTH = 250;
    private static final int HP_BAR_HEIGHT = 24;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public GamePanel() {

        setPreferredSize(
                new Dimension(
                        WIDTH,
                        HEIGHT
                )
        );

        setFocusable(true);

        addKeyListener(this);

        // =====================================================
        // LOAD MAP
        // =====================================================

        loadForest();

        loadBossRooms();

        // =====================================================
        // CREATE PLAYER
        // =====================================================

        player = new Player(
                500,
                400
        );

        // =====================================================
        // CREATE MONSTERS
        // =====================================================

        spawnInitialMonsters();

        // =====================================================
        // FOCUS
        // =====================================================

        javax.swing.SwingUtilities.invokeLater(() -> {

            requestFocusInWindow();

        });
    }

    // =========================================================
    // START GAME
    // =========================================================

    public void startGame() {

        requestFocusInWindow();

        System.out.println(
                "GamePanel Ready!"
        );
    }

    // =========================================================
    // LOAD FOREST
    // =========================================================

    private void loadForest() {

        try {

            File file = new File(
                    "res/map/forest.png"
            );

            if (!file.exists()) {

                System.out.println(
                        "หาไฟล์ forest.png ไม่เจอ!"
                );

                System.out.println(
                        file.getAbsolutePath()
                );

                return;
            }

            forest = ImageIO.read(file);

            System.out.println(
                    "โหลด forest.png สำเร็จ!"
            );

        } catch (Exception e) {

            System.out.println(
                    "โหลด forest.png ไม่สำเร็จ!"
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // LOAD BOSS ROOMS
    // =========================================================

    private void loadBossRooms() {

        String[] paths = {
            "res/map/boosroom1.png",
            "res/map/bossroom2.png",
            "res/map/bossroom3.png",
            "res/map/bossroom4.png"
        };

        for (int i = 0; i < paths.length; i++) {

            File file = new File(paths[i]);

            if (!file.exists()) {

                System.out.println("ERROR: " + paths[i] + " not found!");
                continue;
            }

            try {

                bossRooms[i] = ImageIO.read(file);
                System.out.println("Loaded " + paths[i]);

            } catch (Exception e) {

                System.out.println("ERROR: Failed to load " + paths[i]);
                e.printStackTrace();
            }
        }
    }

    // =========================================================
    // ENTER BOSS DUNGEON
    // =========================================================

    private void enterBossDungeon() {

        if (inBossDungeon || gameOver || gameWon) {

            return;
        }

        if (bossesDefeated >= 4) {

            gameWon = true;
            return;
        }

        inBossDungeon = true;

        player.setX(520);
        player.setY(500);

        if (boss == null || boss.isDead()) {

            boss = new Boss(
                    540,
                    120,
                    bossesDefeated + 1
            );
        }

        bossAttackHitApplied = false;

        System.out.println(
                "Entered Boss Room " + boss.getBossNumber()
                + ": " + boss.getName()
        );
    }

    // =========================================================
    // EXIT BOSS DUNGEON
    // =========================================================

    private void exitBossDungeon() {

        if (!inBossDungeon) {

            return;
        }

        inBossDungeon = false;

        player.setX(500);
        player.setY(400);

        /*
         * ถ้าออกจากห้องบอสก่อนชนะ การต่อสู้ครั้งต่อไป
         * ต้องเริ่มใหม่ด้วยบอส HP เต็ม
         */

        if (boss != null && !boss.isDead()) {

            boss = null;

            bossAttackHitApplied = false;
        }

        System.out.println(
                "กลับสู่ Forest"
        );
    }

    // =========================================================
    // SPAWN INITIAL MONSTERS
    // =========================================================

    private void spawnInitialMonsters() {

        for (
                int i = 0;
                i < MAX_MONSTERS;
                i++
        ) {

            spawnMonster(i);
        }

        System.out.println(
                "สร้าง Monster "
                + MAX_MONSTERS
                + " ตัวแล้ว!"
        );
    }

    // =========================================================
    // SPAWN MONSTER
    // =========================================================

    private void spawnMonster(
            int index
    ) {

        if (
                index < 0 ||
                index >= MAX_MONSTERS
        ) {

            return;
        }

        int x = 0;
        int y = 0;

        int attempts = 0;

        do {

            x =
                    SPAWN_MARGIN
                    + random.nextInt(
                            WIDTH
                            - MONSTER_SIZE
                            - (SPAWN_MARGIN * 2)
                    );

            y =
                    SPAWN_MARGIN
                    + random.nextInt(
                            HEIGHT
                            - MONSTER_SIZE
                            - (SPAWN_MARGIN * 2)
                    );

            attempts++;

            if (attempts >= 100) {

                break;
            }

        } while (
                isTooCloseToPlayer(x, y)
                ||
                isTooCloseToOtherMonsters(
                        x,
                        y,
                        index
                )
        );

        monsters[index] =
                new Monster(
                        x,
                        y
                );

        respawnStartTime[index] = 0;

        attackHitApplied[index] = false;

        dropCreated[index] = false;

        System.out.println(
                "Spawn Monster #"
                + (index + 1)
                + " at ("
                + x
                + ", "
                + y
                + ")"
        );
    }

    // =========================================================
    // PLAYER DISTANCE
    // =========================================================

    private boolean isTooCloseToPlayer(
            int x,
            int y
    ) {

        if (player == null) {

            return false;
        }

        int dx =
                x - player.getX();

        int dy =
                y - player.getY();

        int distanceSquared =
                (dx * dx)
                + (dy * dy);

        int minimumDistance = 180;

        return distanceSquared
                < minimumDistance
                * minimumDistance;
    }

    // =========================================================
    // MONSTER DISTANCE
    // =========================================================

    private boolean isTooCloseToOtherMonsters(
            int x,
            int y,
            int currentIndex
    ) {

        for (
                int i = 0;
                i < MAX_MONSTERS;
                i++
        ) {

            if (i == currentIndex) {

                continue;
            }

            Monster monster =
                    monsters[i];

            if (monster == null) {

                continue;
            }

            if (monster.isDead()) {

                continue;
            }

            int dx =
                    x - monster.getX();

            int dy =
                    y - monster.getY();

            int distanceSquared =
                    (dx * dx)
                    + (dy * dy);

            int minimumDistance = 100;

            if (
                    distanceSquared
                    < minimumDistance
                    * minimumDistance
            ) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public void update() {

        if (!gameStarted || gameOver || gameWon) {
            return;
        }

        // =====================================================
        // PLAYER
        // =====================================================

        if (player != null) {

            player.update();

            if (player.isDead()) {

                gameOver = true;
                return;
            }
        }

        // =====================================================
        // BOSS
        // =====================================================

        if (
                inBossDungeon &&
                boss != null &&
                player != null
        ) {

            boss.update(player, summon);
        }

        if (summon != null && !summon.isDead()) {

            summon.update(
                    player,
                    inBossDungeon ? null : monsters,
                    inBossDungeon ? boss : null
            );

        } else if (summon != null) {

            summon = null;
            summonAvailableAt = System.currentTimeMillis() + 8000;
            showStatus("Summon is recovering. Ready again in 8 seconds.");
        }

        // =====================================================
        // MONSTERS
        // =====================================================

        for (
                int i = 0;
                i < MAX_MONSTERS && !inBossDungeon;
                i++
        ) {

            Monster monster =
                    monsters[i];

            // =================================================
            // MONSTER NULL
            // =================================================

            if (monster == null) {

                spawnMonster(i);

                continue;
            }

            // =================================================
            // MONSTER DEAD
            // =================================================

            if (monster.isDead()) {

                // =============================================
                // CREATE DROP
                // =============================================

                createDrop(
                        i,
                        monster
                );

                // =============================================
                // START RESPAWN TIMER
                // =============================================

                if (
                        respawnStartTime[i]
                        == 0
                ) {

                    respawnStartTime[i] =
                            System.currentTimeMillis();

                    System.out.println(
                            "Monster #"
                            + (i + 1)
                            + " ตาย!"
                    );

                    System.out.println(
                            "รอ 3 วินาทีก่อน Respawn..."
                    );
                }

                // =============================================
                // CHECK RESPAWN
                // =============================================

                long elapsed =
                        System.currentTimeMillis()
                        -
                        respawnStartTime[i];

                if (
                        elapsed >=
                        RESPAWN_TIME
                ) {

                    spawnMonster(i);

                    System.out.println(
                            "Monster #"
                            + (i + 1)
                            + " Respawn แล้ว!"
                    );
                }

                continue;
            }

            // =================================================
            // MONSTER UPDATE
            // =================================================

            if (player != null) {

                monster.update(player);
            }
        }

        // =====================================================
        // SEPARATE MONSTERS
        // =====================================================

        if (!inBossDungeon) {

            separateMonsters();
        }

        // =====================================================
        // PLAYER ATTACK
        // =====================================================

        checkPlayerAttack();

        // =====================================================
        // BOSS DEFEAT
        // =====================================================

        checkBossDefeated();

        // =====================================================
        // ITEM PICKUP
        // =====================================================

        if (!inBossDungeon) {

            checkItemPickup();
        }

    }

    // =========================================================
    // CREATE DROP
    // =========================================================

    private void createDrop(
            int index,
            Monster monster
    ) {

        if (
                index < 0 ||
                index >= MAX_MONSTERS
        ) {

            return;
        }

        if (monster == null) {

            return;
        }

        if (dropCreated[index]) {

            return;
        }

        int dropType =
                monster.getDropType();

        // =====================================================
        // NO DROP
        // =====================================================

        if (dropType == -1) {

            dropCreated[index] = true;

            System.out.println(
                    "Monster #"
                    + (index + 1)
                    + " ไม่ดรอป Item"
            );

            return;
        }

        dropCreated[index] = true;

        // =====================================================
        // DROP POSITION
        // =====================================================

        int dropX =
                monster.getDropX();

        int dropY =
                monster.getDropY();

        // =====================================================
        // POTION
        // =====================================================

        if (dropType == POTION) {

            Item potion =
                    new Item(
                            "Potion",
                            POTION
                    );

            potion.setX(
                    dropX - 24
            );

            potion.setY(
                    dropY - 24
            );

            items.add(
                    new DropItem(
                            potion,
                            dropX,
                            dropY
                    )
            );

            System.out.println(
                    "Monster #"
                    + (index + 1)
                    + " ดรอป Potion!"
            );

            return;
        }

        // =====================================================
        // RUNE
        // =====================================================

        if (dropType == RUNE) {

            Item rune =
                    new Item(
                            "Rune",
                            RUNE
                    );

            rune.setX(
                    dropX - 24
            );

            rune.setY(
                    dropY - 24
            );

            items.add(
                    new DropItem(
                            rune,
                            dropX,
                            dropY
                    )
            );

            System.out.println(
                    "Monster #"
                    + (index + 1)
                    + " ดรอป Rune!"
            );
        }
    }

    // =========================================================
    // ITEM PICKUP
    // =========================================================

    private void checkItemPickup() {

        if (player == null) {

            return;
        }

        for (
                int i = items.size() - 1;
                i >= 0;
                i--
        ) {

            DropItem drop =
                    items.get(i);

            if (
                    drop == null ||
                    drop.item == null
            ) {

                items.remove(i);

                continue;
            }

            Rectangle itemBounds =
                    new Rectangle(
                            drop.x - 20,
                            drop.y - 20,
                            40,
                            40
                    );

            Rectangle playerBounds =
                    new Rectangle(
                            player.getX(),
                            player.getY(),
                            80,
                            100
                    );

            if (
                    playerBounds.intersects(
                            itemBounds
                    )
            ) {

                Item item =
                        drop.item;

                if (
                        item.getType()
                        == POTION
                ) {

                    player.addPotion();

                    System.out.println(
                            "เก็บ Potion แล้ว!"
                    );

                } else if (
                        item.getType()
                        == RUNE
                ) {

                    player.addRune();

                    System.out.println(
                            "เก็บ Rune แล้ว!"
                    );
                }

                items.remove(i);
            }
        }
    }

    // =========================================================
    // SEPARATE MONSTERS
    // =========================================================

    private void separateMonsters() {

        for (
                int i = 0;
                i < MAX_MONSTERS && !inBossDungeon;
                i++
        ) {

            Monster monsterA =
                    monsters[i];

            if (
                    monsterA == null ||
                    monsterA.isDead()
            ) {

                continue;
            }

            for (
                    int j = i + 1;
                    j < MAX_MONSTERS;
                    j++
            ) {

                Monster monsterB =
                        monsters[j];

                if (
                        monsterB == null ||
                        monsterB.isDead()
                ) {

                    continue;
                }

                int dx =
                        monsterB.getX()
                        - monsterA.getX();

                int dy =
                        monsterB.getY()
                        - monsterA.getY();

                double distance =
                        Math.sqrt(
                                dx * dx
                                + dy * dy
                        );

                if (
                        distance
                        < SEPARATION_DISTANCE
                ) {

                    if (distance == 0) {

                        dx = 1;
                        dy = 0;
                        distance = 1;
                    }

                    double pushX =
                            dx / distance;

                    double pushY =
                            dy / distance;

                    int newAX =
                            monsterA.getX()
                            - (int) Math.round(
                                    pushX
                                    * SEPARATION_FORCE
                            );

                    int newAY =
                            monsterA.getY()
                            - (int) Math.round(
                                    pushY
                                    * SEPARATION_FORCE
                            );

                    int newBX =
                            monsterB.getX()
                            + (int) Math.round(
                                    pushX
                                    * SEPARATION_FORCE
                            );

                    int newBY =
                            monsterB.getY()
                            + (int) Math.round(
                                    pushY
                                    * SEPARATION_FORCE
                            );

                    monsterA.setX(newAX);
                    monsterA.setY(newAY);

                    monsterB.setX(newBX);
                    monsterB.setY(newBY);
                }
            }
        }
    }

    // =========================================================
    // PLAYER ATTACK
    // =========================================================

    private void checkPlayerAttack() {

        if (player == null) {

            return;
        }

        // =====================================================
        // RESET HIT CONTROL
        // =====================================================

        if (!player.isAttackHitboxActive()) {

            for (
                    int i = 0;
                    i < MAX_MONSTERS;
                    i++
            ) {

                attackHitApplied[i] = false;
            }

            bossAttackHitApplied = false;

            return;
        }

        Rectangle playerHitbox =
                player.getAttackHitbox();

        // =====================================================
        // CHECK EVERY MONSTER
        // =====================================================

        for (
                int i = 0;
                i < MAX_MONSTERS && !inBossDungeon;
                i++
        ) {

            Monster monster =
                    monsters[i];

            if (monster == null) {

                continue;
            }

            if (monster.isDead()) {

                continue;
            }

            // =================================================
            // ALREADY HIT DURING THIS ATTACK
            // =================================================

            if (attackHitApplied[i]) {

                continue;
            }

            Rectangle monsterHitbox =
                    monster.getHitbox();

            // =================================================
            // COLLISION
            // =================================================

            if (
                    playerHitbox.intersects(
                            monsterHitbox
                    )
            ) {

                int damage =
                        player.getCurrentAttackDamage();

                // =================================================
                // SAFETY
                // =================================================

                if (damage <= 0) {

                    continue;
                }

                // =================================================
                // DAMAGE MONSTER
                // =================================================

                monster.takeDamage(
                        damage
                );

                attackHitApplied[i] = true;

                // =================================================
                // LIFE STEAL
                // =================================================
                //
                // Player.java จะเช็กเองว่า
                // เป็น Evolution 1 หรือไม่
                //
                // Form 2 = ดูดเลือด 10%, Form 3 = 20%
                // Form 1 = ไม่มี
                //
                // =================================================

                player.onAttackHit(
                        damage
                );

                // =================================================
                // DEBUG
                // =================================================

                System.out.println(
                        "Player โจมตี Monster #"
                        + (i + 1)
                );

                System.out.println(
                        "Damage = "
                        + damage
                );

                System.out.println(
                        "Monster HP = "
                        + monster.getHP()
                );
            }
        }

        // =====================================================
        // CHECK BOSS
        // =====================================================

        if (
                !inBossDungeon ||
                boss == null ||
                boss.isDead() ||
                bossAttackHitApplied
        ) {

            return;
        }

        if (
                playerHitbox.intersects(
                        boss.getHitbox()
                )
        ) {

            int damage =
                    player.getCurrentAttackDamage();

            if (damage <= 0) {

                return;
            }

            boss.takeDamage(damage);

            bossAttackHitApplied = true;

            player.onAttackHit(damage);

            System.out.println(
                    "Player โจมตี Boss 1"
            );

            System.out.println(
                    "Damage = " + damage
            );

            System.out.println(
                    "Boss HP = " + boss.getHP()
            );
        }
    }

    // =========================================================
    // BOSS DEFEAT
    // =========================================================

    private void checkBossDefeated() {

        if (boss == null || !boss.isDead()) {

            return;
        }

        if (boss.getBossNumber() <= bossesDefeated) {

            return;
        }

        bossesDefeated = boss.getBossNumber();

        showStatus(boss.getName() + " defeated!");

        if (player != null && bossesDefeated == 1) {

            player.unlockSummon();
        }

        if (bossesDefeated >= 4) {

            gameWon = true;
            showStatus("All bosses defeated!");
        }

        System.out.println(boss.getName() + " defeated!");
    }

    private void showStatus(String message) {

        statusMessage = message;
        statusMessageUntil = System.currentTimeMillis() + 3000;
    }

    // =========================================================
    // DRAW
    // =========================================================

    @Override
    protected void paintComponent(
            Graphics g
    ) {

        super.paintComponent(g);

        Graphics2D g2 =
                (Graphics2D) g;

        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        // =====================================================
        // MAP
        // =====================================================

        int activeRoom =
                boss != null
                ? boss.getBossNumber() - 1
                : bossesDefeated;

        if (
                inBossDungeon &&
                activeRoom >= 0 &&
                activeRoom < bossRooms.length &&
                bossRooms[activeRoom] != null
        ) {

            g2.drawImage(
                    bossRooms[activeRoom],
                    0,
                    0,
                    WIDTH,
                    HEIGHT,
                    null
            );

        } else if (forest != null) {

            g2.drawImage(
                    forest,
                    0,
                    0,
                    WIDTH,
                    HEIGHT,
                    null
            );
        }

        // =====================================================
        // ITEMS
        // =====================================================

        if (!inBossDungeon) {

            drawItems(g2);
        }

        // =====================================================
        // MONSTERS
        // =====================================================

        for (
                int i = 0;
                i < MAX_MONSTERS && !inBossDungeon;
                i++
        ) {

            Monster monster =
                    monsters[i];

            if (monster != null) {

                monster.draw(g2);
            }
        }

        // =====================================================
        // BOSS
        // =====================================================

        if (inBossDungeon && boss != null) {

            boss.draw(g2);
        }

        if (summon != null && !summon.isDead()) {

            summon.draw(g2);
        }

        // =====================================================
        // PLAYER
        // =====================================================

        if (player != null) {

            player.draw(g2);
            drawSkillRange(g2);
        }

        // =====================================================
        // UI
        // =====================================================

        drawGameUI(g2);

        if (gameOver || gameWon) {

            drawEndScreen(g2);

        } else if (!gameStarted) {

            drawTitleScreen(g2);
        }
    }

    // =========================================================
    // DRAW GAME UI
    // =========================================================

    private void drawSkillRange(Graphics2D g2) {
        if (player == null || !player.isSkillActive() || !player.isAttackHitboxActive()) {
            return;
        }

        Rectangle range = player.getAttackHitbox();
        if (range.isEmpty()) return;

        int form = player.getEvolutionStage() + 1;
        Color edge;
        if (form == 1) {
            edge = new Color(94, 190, 255, 235);
        } else if (form == 2) {
            edge = new Color(103, 245, 151, 235);
        } else {
            edge = new Color(224, 133, 255, 245);
        }
        Color fill = new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 42);

        g2.setColor(fill);
        g2.fillRect(range.x, range.y, range.width, range.height);
        g2.setColor(edge);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRect(range.x, range.y, range.width - 1, range.height - 1);

        String shape = form == 3 ? " AOE" : " FORWARD";
        String label = "E SKILL · FORM " + form + shape + " · " + range.width + " x " + range.height;
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 11f));
        int labelWidth = g2.getFontMetrics().stringWidth(label) + 14;
        int labelX = Math.max(4, Math.min(range.x + 4, WIDTH - labelWidth - 4));
        int labelY = range.y - 22;
        if (labelY < 4) labelY = Math.min(HEIGHT - 21, range.y + 4);

        g2.setColor(new Color(12, 15, 22, 225));
        g2.fillRoundRect(labelX, labelY, labelWidth, 19, 8, 8);
        g2.setColor(edge);
        g2.drawRoundRect(labelX, labelY, labelWidth, 19, 8, 8);
        g2.drawString(label, labelX + 7, labelY + 13);
    }

    private void drawGameUI(
            Graphics2D g2
    ) {

        if (player == null) {

            return;
        }

        drawOrnatePanel(g2, 16, 16, 326, 145);

        g2.setColor(new Color(244, 207, 124));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 15f));
        g2.drawString("RUNE SLAYER", 30, 40);
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.PLAIN, 12f));
        g2.setColor(new Color(216, 205, 181));
        g2.drawString("BOSSES  " + bossesDefeated + " / 4", 242, 39);

        int currentHP = Math.max(0, player.getHP());
        int maxHP = Math.max(1, player.getMaxHP());
        g2.setColor(new Color(236, 226, 205));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        g2.drawString("VITALITY", 30, 66);
        g2.setColor(new Color(29, 26, 28));
        g2.fillRoundRect(94, 53, 184, 17, 8, 8);
        if (currentHP > 0) {
            g2.setPaint(new GradientPaint(94, 0, new Color(88, 190, 255), 278, 0, new Color(35, 82, 205)));
            g2.fillRoundRect(94, 53, Math.min(184, (int) (184.0 * currentHP / maxHP)), 17, 8, 8);
        }
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 11f));
        String hpText = currentHP + " / " + maxHP;
        g2.drawString(hpText, 184 - g2.getFontMetrics().stringWidth(hpText) / 2, 66);

        g2.setPaint(new GradientPaint(30, 0, new Color(220, 190, 126), 320, 0, new Color(83, 69, 53)));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(30, 81, 326, 81);
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.PLAIN, 12f));
        g2.setColor(new Color(238, 230, 213));
        g2.drawString("POTION  " + player.getPotionCount(), 30, 102);
        g2.drawString("RUNES  " + player.getRuneCount(), 143, 102);
        g2.drawString("FORM  " + (player.getEvolutionStage() + 1), 244, 102);
        g2.setColor(new Color(192, 183, 164));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.PLAIN, 11f));
        g2.drawString("WASD / ARROWS  MOVE", 30, 132);
        g2.drawString("SPACE ATTACK   F HEAVY   E SKILL   Q DASH", 30, 150);

        drawOrnatePanel(g2, 16, HEIGHT - 47, 1168, 31);
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 11f));
        g2.setColor(new Color(239, 222, 187));
        g2.drawString("J  POTION     K  EVOLVE  (5 / 10 RUNES)     R  SUMMON (FORM 3 + BOSS 1)", 30, HEIGHT - 27);
        String dungeonHint = inBossDungeon
                ? "X  RETURN TO FOREST"
                : "X  ENTER BOSS " + (bossesDefeated + 1);
        int hintWidth = g2.getFontMetrics().stringWidth(dungeonHint);
        g2.drawString(dungeonHint, WIDTH - hintWidth - 34, HEIGHT - 27);

        if (statusMessage != null && !statusMessage.isEmpty()
                && System.currentTimeMillis() < statusMessageUntil) {
            int messageWidth = g2.getFontMetrics().stringWidth(statusMessage);
            int toastWidth = Math.min(WIDTH - 40, messageWidth + 38);
            int toastX = (WIDTH - toastWidth) / 2;
            drawOrnatePanel(g2, toastX, HEIGHT - 91, toastWidth, 32);
            g2.setColor(new Color(255, 238, 198));
            g2.drawString(statusMessage, (WIDTH - messageWidth) / 2, HEIGHT - 70);
        }
    }

    private void drawOrnatePanel(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(new Color(13, 17, 25, 224));
        g2.fillRoundRect(x, y, width, height, 16, 16);
        g2.setColor(new Color(196, 157, 86, 205));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x, y, width, height, 16, 16);
        g2.setColor(new Color(250, 224, 164, 90));
        g2.drawRoundRect(x + 3, y + 3, width - 6, height - 6, 13, 13);
    }

    private void drawEndScreen(Graphics2D g2) {

        g2.setColor(new Color(5, 8, 14, 190));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        drawOrnatePanel(g2, 270, 190, 660, 320);
        g2.setColor(new Color(244, 207, 124));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 44f));

        String title = gameWon ? "VICTORY" : "GAME OVER";
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (WIDTH - titleWidth) / 2, 300);

        g2.setColor(new Color(236, 229, 212));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.PLAIN, 18f));

        String detail = gameWon
                ? "You defeated every boss in Rune Slayer."
                : "The forest has claimed another slayer.";
        int detailWidth = g2.getFontMetrics().stringWidth(detail);
        g2.drawString(detail, (WIDTH - detailWidth) / 2, 345);

        String prompt = "Press ENTER to play again";
        int promptWidth = g2.getFontMetrics().stringWidth(prompt);
        g2.drawString(prompt, (WIDTH - promptWidth) / 2, 395);
    }

    private void drawTitleScreen(Graphics2D g2) {

        g2.setColor(new Color(5, 8, 14, 190));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        drawOrnatePanel(g2, 190, 120, 820, 480);

        g2.setColor(new Color(244, 207, 124));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 54f));

        String title = "RUNE SLAYER";
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (WIDTH - titleWidth) / 2, 210);

        g2.setColor(new Color(236, 229, 212));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.PLAIN, 19f));
        drawCentered(g2, "Gather runes. Evolve your power. Defeat the four guardians.", 270);
        g2.setColor(new Color(216, 190, 135));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        drawCentered(g2, "MOVE  WASD / ARROWS     ATTACK  SPACE / F     SKILL  E     DASH  Q", 342);
        drawCentered(g2, "POTION  J     EVOLVE  K     SUMMON  R     BOSS DUNGEON  X", 375);

        g2.setColor(new Color(245, 214, 145));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 24f));

        String prompt = "Press ENTER to begin";
        int promptWidth = g2.getFontMetrics().stringWidth(prompt);
        g2.drawString(prompt, (WIDTH - promptWidth) / 2, 480);
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        g2.drawString(text, (WIDTH - g2.getFontMetrics().stringWidth(text)) / 2, y);
    }

    // =========================================================
    // DRAW ITEMS
    // =========================================================

    private void drawItems(
            Graphics2D g2
    ) {

        for (DropItem drop : items) {

            if (
                    drop == null ||
                    drop.item == null
            ) {

                continue;
            }

            drop.item.setX(
                    drop.x - 24
            );

            drop.item.setY(
                    drop.y - 24
            );

            drop.item.draw(g2);
        }
    }

    // =========================================================
    // KEY PRESSED
    // =========================================================

    @Override
    public void keyPressed(
            KeyEvent e
    ) {

        int key =
                e.getKeyCode();

        if (player == null) {

            return;
        }

        if (gameOver || gameWon) {

            if (key == KeyEvent.VK_ENTER) {

                restartGame();
            }

            return;
        }

        if (!gameStarted) {

            if (key == KeyEvent.VK_ENTER) {

                gameStarted = true;
                showStatus("Farm runes in the forest, evolve, and defeat all four bosses.");
            }

            return;
        }

        // =====================================================
        // MOVEMENT
        // =====================================================

        if (
                key == KeyEvent.VK_W ||
                key == KeyEvent.VK_UP
        ) {

            player.setUp(true);
        }

        if (
                key == KeyEvent.VK_S ||
                key == KeyEvent.VK_DOWN
        ) {

            player.setDown(true);
        }

        if (
                key == KeyEvent.VK_A ||
                key == KeyEvent.VK_LEFT
        ) {

            player.setLeft(true);
        }

        if (
                key == KeyEvent.VK_D ||
                key == KeyEvent.VK_RIGHT
        ) {

            player.setRight(true);
        }

        // =====================================================
        // ATTACK 1
        // =====================================================

        if (key == KeyEvent.VK_SPACE) {

            player.attack();
        }

        // =====================================================
        // ATTACK 2
        // =====================================================

        if (key == KeyEvent.VK_F) {

            player.attack2();
        }

        // =====================================================
        // SKILL
        // =====================================================

        if (key == KeyEvent.VK_E) {

            player.skill();
        }

        // =====================================================
        // DASH
        // =====================================================

        if (key == KeyEvent.VK_Q) {

            player.dashAttack();
        }

        // =====================================================
        // POTION
        // =====================================================

        if (key == KeyEvent.VK_J) {

            player.heal();
        }

        // =====================================================
        // EVOLUTION
        // =====================================================

        if (key == KeyEvent.VK_K) {

            player.evolve();
        }

        // =====================================================
        // SUMMON
        // =====================================================

        if (key == KeyEvent.VK_R) {

            summonCompanion();
        }

        // =====================================================
        // DUNGEON TRANSITION
        // =====================================================

        if (key == KeyEvent.VK_X) {

            if (inBossDungeon) {

                exitBossDungeon();

            } else {

                enterBossDungeon();
            }
        }
    }

    // =========================================================
    // SUMMON COMPANION
    // =========================================================

    private void summonCompanion() {

        if (!player.isSummonUnlocked()) {

            showStatus("Summon is locked. Defeat Boss 1 first.");
            return;
        }

        if (!player.isEvolution2()) {

            showStatus("Summon requires Form 3.");
            return;
        }

        if (summon != null && !summon.isDead()) {

            showStatus("Your Summon is already with you.");
            return;
        }

        long now = System.currentTimeMillis();

        if (now < summonAvailableAt) {

            long secondsLeft = (summonAvailableAt - now + 999) / 1000;
            showStatus("Summon recovering: " + secondsLeft + "s.");
            return;
        }

        summon = new Summon(
                player.getX() + 50,
                player.getY()
        );

        showStatus("Summon joined the battle!");
    }

    // =========================================================
    // RESTART GAME
    // =========================================================

    private void restartGame() {

        player = new Player(500, 400);
        monsters = new Monster[MAX_MONSTERS];
        items.clear();

        for (int i = 0; i < MAX_MONSTERS; i++) {

            respawnStartTime[i] = 0;
            attackHitApplied[i] = false;
            dropCreated[i] = false;
        }

        boss = null;
        summon = null;
        summonAvailableAt = 0;
        bossAttackHitApplied = false;
        bossesDefeated = 0;
        inBossDungeon = false;
        gameStarted = true;
        gameOver = false;
        gameWon = false;
        statusMessage = "";

        spawnInitialMonsters();
        requestFocusInWindow();
    }

    // =========================================================
    // KEY RELEASED
    // =========================================================

    @Override
    public void keyReleased(
            KeyEvent e
    ) {

        int key =
                e.getKeyCode();

        if (player == null) {

            return;
        }

        if (
                key == KeyEvent.VK_W ||
                key == KeyEvent.VK_UP
        ) {

            player.setUp(false);
        }

        if (
                key == KeyEvent.VK_S ||
                key == KeyEvent.VK_DOWN
        ) {

            player.setDown(false);
        }

        if (
                key == KeyEvent.VK_A ||
                key == KeyEvent.VK_LEFT
        ) {

            player.setLeft(false);
        }

        if (
                key == KeyEvent.VK_D ||
                key == KeyEvent.VK_RIGHT
        ) {

            player.setRight(false);
        }
    }

    // =========================================================
    // KEY TYPED
    // =========================================================

    @Override
    public void keyTyped(
            KeyEvent e
    ) {

    }

    // =========================================================
    // FOCUS
    // =========================================================

    @Override
    public void addNotify() {

        super.addNotify();

        requestFocusInWindow();
    }

    // =========================================================
    // DROP ITEM CLASS
    // =========================================================

    private static class DropItem {

        private Item item;

        private int x;

        private int y;

        public DropItem(
                Item item,
                int x,
                int y
        ) {

            this.item = item;

            this.x = x;

            this.y = y;
        }
    }
}
