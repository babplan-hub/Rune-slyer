import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;

import java.util.Random;

public class Monster {

    // =========================================================
    // POSITION
    // =========================================================

    private int x;
    private int y;

    // =========================================================
    // DRAW SIZE
    // =========================================================

    private static final int DRAW_WIDTH = 54;
    private static final int DRAW_HEIGHT = 72;

    // =========================================================
    // SPRITE SHEET
    // =========================================================

    // Logical animation clock, not the number of columns in any source image.
    private static final int ANIMATION_TICKS = 16;

    // =========================================================
    // ROW
    // =========================================================

    private static final int ROW_IDLE = 0;
    private static final int ROW_WALK = 1;
    private static final int ROW_RUN = 2;
    private static final int ROW_ATTACK_1 = 3;
    private static final int ROW_ATTACK_2 = 4;
    private static final int ROW_HURT = 5;
    private static final int ROW_DIE = 6;
    private static final int ROW_SKILL = 7;

    // =========================================================
    // SPRITES
    // =========================================================

    private SpriteSheet.Frame[] idleFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] walkFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] runFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] attack1Frames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] attack2Frames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] hurtFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] dieFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] skillFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    // =========================================================
    // ANIMATION
    // =========================================================

    private int currentFrame = 0;

    private boolean facingLeft = false;

    private int animationCounter = 0;

    private static final int ANIMATION_SPEED = 6;

    private int currentRow = ROW_IDLE;

    // =========================================================
    // HP
    // =========================================================

    private int hp = 100;

    private static final int MAX_HP = 100;

    // =========================================================
    // MOVEMENT
    // =========================================================

    private int speed = 1;

    private boolean moving = false;

    private final Random random = new Random();

    // =========================================================
    // RANDOM WALK
    // =========================================================

    private int randomDirectionX = 0;

    private int randomDirectionY = 0;

    private int randomMoveTimer = 0;

    private static final int RANDOM_MOVE_TIME_MIN = 40;

    private static final int RANDOM_MOVE_TIME_MAX = 120;

    // =========================================================
    // FOLLOW PLAYER
    // =========================================================

    private static final int FOLLOW_DISTANCE = 180;

    // =========================================================
    // ATTACK
    // =========================================================

    private static final int ATTACK_DISTANCE = 90;

    private static final int ATTACK_DAMAGE = 10;

    private static final long ATTACK_COOLDOWN = 1000;

    private long lastAttackTime = 0;

    private boolean attacking = false;

    /*
     * ใช้ attackFrame สำหรับ Animation Attack โดยเฉพาะ
     */
    private int attackFrame = 0;

    private int attackCounter = 0;

    private static final int ATTACK_SPEED = 6;

    private boolean attackDamageApplied = false;

    // =========================================================
    // DROP
    // =========================================================

    private static final int DROP_NONE = -1;

    private static final int DROP_RUNE =
            Item.RUNE;

    private static final int DROP_POTION =
            Item.POTION;

    private int dropType = DROP_NONE;

    private boolean dropChecked = false;

    private boolean dropConsumed = false;

    private static final int POTION_DROP_CHANCE = 50;

    private static final int RUNE_DROP_CHANCE = 30;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Monster(int x, int y) {

        this.x = x;

        this.y = y;

        loadSpriteSheet();

        chooseRandomDirection();
    }

    // =========================================================
    // LOAD SPRITE SHEET
    // =========================================================

    private void loadSpriteSheet() {
        try {
            SpriteSheet.Frame[][] rows = SpriteSheet.load("res/monster/gg.png").timeline(ANIMATION_TICKS);
            idleFrames = rows[0];
            walkFrames = rows[1];
            runFrames = rows[2];
            attack1Frames = rows[3];
            attack2Frames = rows[4];
            hurtFrames = rows[5];
            dieFrames = rows[6];
            skillFrames = rows[7];
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not load annotated monster sprites", e);
        }
    }

    public void update(Player player) {

        // =====================================================
        // DEAD
        // =====================================================

        if (isDead()) {

            currentRow =
                    ROW_DIE;

            updateAnimation();

            return;
        }

        // =====================================================
        // NO PLAYER
        // =====================================================

        if (
                player == null ||
                player.isDead()
        ) {

            currentRow =
                    ROW_IDLE;

            moving = false;

            updateAnimation();

            return;
        }

        // =====================================================
        // ATTACKING
        // =====================================================

        if (attacking) {

            updateAttack(player);

            return;
        }

        // =====================================================
        // DISTANCE
        // =====================================================

        int dx =
                player.getX() - x;

        int dy =
                player.getY() - y;

        double distance =
                Math.sqrt(
                        dx * dx +
                        dy * dy
                );

        // =====================================================
        // ATTACK RANGE
        // =====================================================

        if (
                distance <=
                ATTACK_DISTANCE
        ) {

            moving = false;

            randomDirectionX = 0;

            randomDirectionY = 0;

            long now =
                    System.currentTimeMillis();

            // =================================================
            // READY TO ATTACK
            // =================================================

            if (
                    now - lastAttackTime
                    >= ATTACK_COOLDOWN
            ) {

                startAttack();

            } else {

                currentRow =
                        ROW_IDLE;

                currentFrame = 0;

                animationCounter = 0;
            }

            return;
        }

        // =====================================================
        // FOLLOW PLAYER
        // =====================================================

        if (
                distance >
                FOLLOW_DISTANCE
        ) {

            moveTowardPlayer(
                    player
            );

            moving = true;

            currentRow =
                    ROW_WALK;

        } else {

            randomWalk();

            moving =
                    randomDirectionX != 0 ||
                    randomDirectionY != 0;

            if (moving) {

                currentRow =
                        ROW_WALK;

            } else {

                currentRow =
                        ROW_IDLE;
            }
        }

        // =====================================================
        // MAP LIMIT
        // =====================================================

        keepInsideMap();

        // =====================================================
        // NORMAL ANIMATION
        // =====================================================

        updateAnimation();
    }

    // =========================================================
    // START ATTACK
    // =========================================================

    private void startAttack() {

        attacking = true;

        attackFrame = 0;

        attackCounter = 0;

        attackDamageApplied = false;

        currentRow =
                ROW_ATTACK_1;

        /*
         * สำคัญ:
         * เริ่มวาดจาก Attack Frame 0
         */

        currentFrame = 0;

        animationCounter = 0;

        lastAttackTime =
                System.currentTimeMillis();
    }

    // =========================================================
    // UPDATE ATTACK
    // =========================================================

    private void updateAttack(
            Player player
    ) {

        // =====================================================
        // ATTACK ROW
        // =====================================================

        currentRow =
                ROW_ATTACK_1;

        // =====================================================
        // SYNC FRAME
        // =====================================================

        /*
         * ใช้ attackFrame เป็นตัวควบคุม Animation Attack
         *
         * ทำให้ภาพที่วาดตรงกับเฟรมโจมตีจริง
         */

        currentFrame =
                attackFrame;

        // =====================================================
        // ATTACK COUNTER
        // =====================================================

        attackCounter++;

        // =====================================================
        // DAMAGE FRAME
        // =====================================================

        /*
         * จาก Sprite Sheet:
         *
         * Frame 0-4
         * เตรียมโจมตี
         *
         * Frame 5-7
         * ช่วงฟันโดน
         *
         * Frame 8+
         * เก็บท่าโจมตี
         */

        if (
                attackFrame >= 5 &&
                attackFrame <= 7 &&
                !attackDamageApplied
        ) {

            attackDamageApplied = true;

            if (
                    player != null &&
                    !player.isDead()
            ) {

                int dx =
                        player.getX() - x;

                int dy =
                        player.getY() - y;

                double distance =
                        Math.sqrt(
                                dx * dx +
                                dy * dy
                        );

                if (
                        distance
                        <= ATTACK_DISTANCE + 25
                ) {

                    player.takeDamage(
                            ATTACK_DAMAGE
                    );

                    System.out.println(
                            "Monster โจมตี Player!"
                    );

                    System.out.println(
                            "Damage = "
                            + ATTACK_DAMAGE
                    );

                    System.out.println(
                            "Player HP = "
                            + player.getHP()
                    );
                }
            }
        }

        // =====================================================
        // NEXT FRAME
        // =====================================================

        if (
                attackCounter >=
                ATTACK_SPEED
        ) {

            attackCounter = 0;

            attackFrame++;

            // =================================================
            // ATTACK CONTINUE
            // =================================================

            if (
                    attackFrame <
                    ANIMATION_TICKS
            ) {

                currentFrame =
                        attackFrame;
            }

            // =================================================
            // ATTACK END
            // =================================================

            if (
                    attackFrame >=
                    ANIMATION_TICKS
            ) {

                attackFrame = 0;

                attackCounter = 0;

                attacking = false;

                attackDamageApplied = false;

                currentRow =
                        ROW_IDLE;

                currentFrame = 0;

                animationCounter = 0;
            }
        }
    }

    // =========================================================
    // MOVE TOWARD PLAYER
    // =========================================================

    private void moveTowardPlayer(
            Player player
    ) {

        int dx =
                player.getX() - x;

        int dy =
                player.getY() - y;

        double distance =
                Math.sqrt(
                        dx * dx +
                        dy * dy
                );

        if (distance <= 0) {

            return;
        }

        facingLeft = dx < 0;

        x +=
                (int) Math.round(
                        dx / distance * speed
                );

        y +=
                (int) Math.round(
                        dy / distance * speed
                );
    }

    // =========================================================
    // RANDOM WALK
    // =========================================================

    private void randomWalk() {

        randomMoveTimer--;

        if (
                randomMoveTimer <= 0
        ) {

            chooseRandomDirection();
        }

        if (randomDirectionX != 0) {
            facingLeft = randomDirectionX < 0;
        }

        x += randomDirectionX;

        y += randomDirectionY;
    }

    // =========================================================
    // RANDOM DIRECTION
    // =========================================================

    private void chooseRandomDirection() {

        randomMoveTimer =
                RANDOM_MOVE_TIME_MIN
                + random.nextInt(
                        RANDOM_MOVE_TIME_MAX
                        - RANDOM_MOVE_TIME_MIN
                );

        int direction =
                random.nextInt(9);

        switch (direction) {

            case 0:

                randomDirectionX = 0;

                randomDirectionY = 0;

                break;

            case 1:

                randomDirectionX = 0;

                randomDirectionY = -1;

                break;

            case 2:

                randomDirectionX = 1;

                randomDirectionY = -1;

                break;

            case 3:

                randomDirectionX = 1;

                randomDirectionY = 0;

                break;

            case 4:

                randomDirectionX = 1;

                randomDirectionY = 1;

                break;

            case 5:

                randomDirectionX = 0;

                randomDirectionY = 1;

                break;

            case 6:

                randomDirectionX = -1;

                randomDirectionY = 1;

                break;

            case 7:

                randomDirectionX = -1;

                randomDirectionY = 0;

                break;

            case 8:

                randomDirectionX = -1;

                randomDirectionY = -1;

                break;
        }
    }

    // =========================================================
    // KEEP INSIDE MAP
    // =========================================================

    private void keepInsideMap() {

        if (x < 0) {

            x = 0;
        }

        if (y < 0) {

            y = 0;
        }

        if (
                x >
                1200 - DRAW_WIDTH
        ) {

            x =
                    1200 - DRAW_WIDTH;
        }

        if (
                y >
                700 - DRAW_HEIGHT
        ) {

            y =
                    700 - DRAW_HEIGHT;
        }
    }

    // =========================================================
    // NORMAL ANIMATION
    // =========================================================

    private void updateAnimation() {

        animationCounter++;

        if (
                animationCounter >=
                ANIMATION_SPEED
        ) {

            animationCounter = 0;

            currentFrame++;

            // =================================================
            // DIE
            // =================================================

            if (
                    currentRow ==
                    ROW_DIE
            ) {

                if (
                        currentFrame >=
                        ANIMATION_TICKS
                ) {

                    currentFrame =
                            ANIMATION_TICKS - 1;
                }

                return;
            }

            // =================================================
            // LOOP
            // =================================================

            int frameLimit = currentRow == ROW_DIE || currentRow == ROW_SKILL
                    ? ANIMATION_TICKS
                    : 8;

            if (currentFrame >= frameLimit) {

                currentFrame = currentRow == ROW_DIE
                        ? ANIMATION_TICKS - 1
                        : 0;
            }
        }
    }

    // =========================================================
    // DRAW
    // =========================================================

    public void draw(
            Graphics2D g2
    ) {

        SpriteSheet.Frame frame =
                getCurrentFrame();

        if (frame == null) {

            drawFallback(g2);

            return;
        }

        // Share the collision box's origin; the old -28 offset floated the sprite above it.
        SpriteSheet.draw(g2, frame, x, y, DRAW_WIDTH, DRAW_HEIGHT, facingLeft);

        if (!isDead()) {

            drawHPBar(g2);
        }
    }

    // =========================================================
    // CURRENT FRAME
    // =========================================================

    private SpriteSheet.Frame getCurrentFrame() {

        /*
         * ป้องกัน ArrayIndexOutOfBounds
         */

        if (
                currentFrame < 0 ||
                currentFrame >= ANIMATION_TICKS
        ) {

            currentFrame = 0;
        }

        switch (currentRow) {

            case ROW_IDLE:

                return idleFrames[
                        currentFrame
                ];

            case ROW_WALK:

                return walkFrames[
                        currentFrame
                ];

            case ROW_RUN:

                return runFrames[
                        currentFrame
                ];

            case ROW_ATTACK_1:

                return attack1Frames[
                        currentFrame
                ];

            case ROW_ATTACK_2:

                return attack2Frames[
                        currentFrame
                ];

            case ROW_HURT:

                return hurtFrames[
                        currentFrame
                ];

            case ROW_DIE:

                return dieFrames[
                        currentFrame
                ];

            case ROW_SKILL:

                return skillFrames[
                        currentFrame
                ];

            default:

                return idleFrames[
                        currentFrame
                ];
        }
    }

    // =========================================================
    // FALLBACK
    // =========================================================

    private void drawFallback(
            Graphics2D g2
    ) {

        g2.setColor(
                Color.DARK_GRAY
        );

        g2.fillRect(
                x,
                y,
                DRAW_WIDTH,
                DRAW_HEIGHT
        );

        drawHPBar(g2);
    }

    // =========================================================
    // HP BAR
    // =========================================================

    private void drawHPBar(
            Graphics2D g2
    ) {

        g2.setColor(
                Color.BLACK
        );

        g2.fillRect(
                x,
                y - 10,
                DRAW_WIDTH,
                6
        );

        g2.setColor(
                Color.RED
        );

        int hpWidth =
                (int) (
                        (double) hp
                        / MAX_HP
                        * DRAW_WIDTH
                );

        g2.fillRect(
                x,
                y - 10,
                hpWidth,
                6
        );
    }

    // =========================================================
    // HITBOX
    // =========================================================

    public Rectangle getHitbox() {

        return new Rectangle(
                x + 12,
                y + 10,
                DRAW_WIDTH - 24,
                DRAW_HEIGHT - 20
        );
    }

    // =========================================================
    // DAMAGE
    // =========================================================

    public void takeDamage(
            int damage
    ) {

        if (isDead()) {

            return;
        }

        if (damage <= 0) {

            return;
        }

        hp -= damage;

        if (hp < 0) {

            hp = 0;
        }

        // =====================================================
        // DEAD
        // =====================================================

        if (hp <= 0) {

            currentRow =
                    ROW_DIE;

            currentFrame = 0;

            animationCounter = 0;

            attacking = false;

            attackFrame = 0;

            attackCounter = 0;

            attackDamageApplied = false;

            checkDrop();

        }

        // =====================================================
        // HURT
        // =====================================================

        else {

            currentRow =
                    ROW_HURT;

            currentFrame = 0;

            animationCounter = 0;

            attacking = false;

            attackFrame = 0;

            attackCounter = 0;

            attackDamageApplied = false;
        }

        System.out.println(
                "Monster HP = "
                + hp
        );
    }

    // =========================================================
    // RANDOM DROP
    // =========================================================

    private void checkDrop() {

        if (dropChecked) {

            return;
        }

        dropChecked = true;

        int chance =
                random.nextInt(100);

        // =====================================================
        // POTION
        // =====================================================

        if (
                chance <
                POTION_DROP_CHANCE
        ) {

            dropType =
                    DROP_POTION;

            System.out.println(
                    "Monster ดรอป Potion!"
            );
        }

        // =====================================================
        // RUNE
        // =====================================================

        else if (
                chance <
                POTION_DROP_CHANCE
                + RUNE_DROP_CHANCE
        ) {

            dropType =
                    DROP_RUNE;

            System.out.println(
                    "Monster ดรอป Rune!"
            );
        }

        // =====================================================
        // NONE
        // =====================================================

        else {

            dropType =
                    DROP_NONE;

            System.out.println(
                    "Monster ไม่ดรอปของ"
            );
        }
    }

    // =========================================================
    // DROP
    // =========================================================

    public boolean hasDrop() {

        return dropType !=
                DROP_NONE;
    }

    public boolean hasPotionDrop() {

        return dropType ==
                DROP_POTION;
    }

    public boolean hasRuneDrop() {

        return dropType ==
                DROP_RUNE;
    }

    public int getDropType() {

        return dropType;
    }

    public boolean isDropConsumed() {

        return dropConsumed;
    }

    public void consumeDrop() {

        dropConsumed = true;
    }

    public int getDropX() {

        return x +
                DRAW_WIDTH / 2;
    }

    public int getDropY() {

        return y +
                DRAW_HEIGHT / 2;
    }

    // =========================================================
    // DEAD
    // =========================================================

    public boolean isDead() {

        return hp <= 0;
    }

    // =========================================================
    // HP
    // =========================================================

    public int getHP() {

        return hp;
    }

    // =========================================================
    // X
    // =========================================================

    public int getX() {

        return x;
    }

    // =========================================================
    // Y
    // =========================================================

    public int getY() {

        return y;
    }

    // =========================================================
    // SET X
    // =========================================================

    public void setX(int x) {

        this.x = x;

        keepInsideMap();
    }

    // =========================================================
    // SET Y
    // =========================================================

    public void setY(int y) {

        this.y = y;

        keepInsideMap();
    }
}
