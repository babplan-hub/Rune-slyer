import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Player {

    // =========================================================
    // POSITION
    // =========================================================

    private int x;
    private int y;

    // =========================================================
    // MOVEMENT
    // =========================================================

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    private boolean facingLeft = false;

    private static final int SPEED = 3;

    // =========================================================
    // HP
    // =========================================================

    private static final int MAX_HP = 200;
    private static final int HP_PER_FORM = 100;

    private int hp = MAX_HP;

    private static final int POTION_HEAL_AMOUNT = 30;

    // =========================================================
    // SPRITE SHEET
    // =========================================================

    // Logical animation clock, not the number of columns in any source image.
    private static final int ANIMATION_TICKS = 16;

    // Eight locomotion ticks; source pose sequences are declared per .sprites file.
    private static final int LOCOMOTION_TICKS = 8;

    // =========================================================
    // DRAW SIZE
    // =========================================================

    private static final int DRAW_WIDTH = 63;
    private static final int DRAW_HEIGHT = 84;

    // =========================================================
    // SCREEN
    // =========================================================

    private static final int SCREEN_WIDTH = 1200;
    private static final int SCREEN_HEIGHT = 700;

    // =========================================================
    // SPRITE ROW
    // =========================================================

    private static final int ROW_IDLE = 0;
    private static final int ROW_WALK = 1;
    private static final int ROW_RUN = 2;

    // Attack 1
    private static final int ROW_ATTACK = 3;

    // Attack 2 / Dash
    private static final int ROW_DASH_ATTACK = 4;

    private static final int ROW_HURT = 5;
    private static final int ROW_DIE = 6;

    // Skill / Effect
    private static final int ROW_EFFECT = 7;

    // =========================================================
    // NORMAL SPRITES
    // =========================================================

    private SpriteSheet.Frame[] idleFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] walkFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] runFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] attackFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] dashAttackFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] hurtFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] dieFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] effectFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    // =========================================================
    // EVOLUTION 1 SPRITES
    // =========================================================

    private SpriteSheet.Frame[] evo1IdleFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo1WalkFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo1RunFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo1AttackFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo1DashAttackFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo1HurtFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo1DieFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo1EffectFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    // =========================================================
    // EVOLUTION 2 SPRITES
    // =========================================================

    private SpriteSheet.Frame[] evo2IdleFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo2WalkFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo2RunFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo2AttackFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo2DashAttackFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo2HurtFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo2DieFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    private SpriteSheet.Frame[] evo2EffectFrames =
            new SpriteSheet.Frame[ANIMATION_TICKS];

    // =========================================================
    // NORMAL ANIMATION
    // =========================================================

    private int currentFrame = 0;

    private int animationCounter = 0;

    private static final int ANIMATION_SPEED = 6;

    // =========================================================
    // ATTACK 1
    // =========================================================

    private boolean attacking = false;

    private int attackFrame = 0;

    private int attackCounter = 0;

    private static final int ATTACK_SPEED = 4;

    // =========================================================
    // ATTACK 2
    // =========================================================

    private boolean attack2Active = false;

    private int attack2Frame = 0;

    private int attack2Counter = 0;

    private static final int ATTACK2_SPEED = 4;

    // =========================================================
    // DASH
    // =========================================================

    private boolean dashAttacking = false;

    private int dashAttackFrame = 0;

    private int dashAttackCounter = 0;

    private static final int DASH_ATTACK_SPEED = 5;

    private static final int DASH_DISTANCE = 8;

    // =========================================================
    // SKILL
    // =========================================================

    private boolean skillActive = false;

    private int skillFrame = 0;

    private int skillCounter = 0;

    private static final int SKILL_SPEED = 4;

    // =========================================================
    // EFFECT
    // =========================================================

    private boolean usingEffect = false;

    private int effectFrame = 0;

    private int effectCounter = 0;

    private static final int EFFECT_SPEED = 5;

    private static final int EFFECT_NONE = 0;
    private static final int EFFECT_HEAL = 1;
    private static final int EFFECT_EVOLUTION = 2;

    private int effectType = EFFECT_NONE;

    // =========================================================
    // EVOLUTION
    // =========================================================

    /*
     * 0 = ร่างเริ่มต้น
     * 1 = ร่าง 1
     * 2 = ร่าง 2
     */

    private int evolutionStage = 0;

    private static final int EVOLUTION_0 = 0;
    private static final int EVOLUTION_1 = 1;
    private static final int EVOLUTION_2 = 2;

    // =========================================================
    // EVOLUTION COST
    // =========================================================

    private static final int EVOLUTION_1_COST = 5;
    private static final int EVOLUTION_2_COST = 10;

    // =========================================================
    // DAMAGE
    // =========================================================

    private static final int NORMAL_ATTACK_DAMAGE = 15;

    private static final int EVO1_ATTACK_DAMAGE = 30;

    private static final int EVO2_ATTACK_DAMAGE = 50;

    private static final int ATTACK2_BASE_DAMAGE = 35;

    private static final int EVO1_ATTACK2_DAMAGE = 55;

    private static final int EVO2_ATTACK2_DAMAGE = 80;

    private static final int DASH_ATTACK_DAMAGE = 150;

    private static final int EVO1_DASH_DAMAGE = 190;

    private static final int EVO2_DASH_DAMAGE = 225;

    // =========================================================
    // SKILL DAMAGE
    // =========================================================

    private static final int NORMAL_SKILL_DAMAGE = 35;

    private static final int EVO1_SKILL_DAMAGE = 50;

    private static final int EVO2_SKILL_DAMAGE = 120;

    // =========================================================
    // LIFE STEAL
    // =========================================================

    private static final double EVO1_LIFE_STEAL = 0.10;
    private static final double EVO2_LIFE_STEAL = 0.20;

    // =========================================================
    // HITBOX
    // =========================================================

    private static final int ATTACK_HITBOX_WIDTH = 54;

    private static final int ATTACK_HITBOX_HEIGHT = 54;

    // =========================================================
    // ITEMS
    // =========================================================

    private int potionCount = 0;

    private int runeCount = 0;

    // =========================================================
    // SUMMON
    // =========================================================

    private boolean summonUnlocked = false;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Player(int x, int y) {

        this.x = x;
        this.y = y;

        keepInsideScreen();

        loadAllSpriteSheets();
    }

    // =========================================================
    // LOAD ALL SPRITES
    // =========================================================

    private void loadAllSpriteSheets() {
        try {
            SpriteSheet.Frame[][] base = SpriteSheet.load("res/player/01.png").timeline(ANIMATION_TICKS);
            idleFrames = base[0];
            walkFrames = base[1];
            runFrames = base[2];
            attackFrames = base[3];
            dashAttackFrames = base[4];
            hurtFrames = base[5];
            dieFrames = base[6];
            effectFrames = base[7];
            SpriteSheet.Frame[][] evo1 = SpriteSheet.load("res/player/Evo1.png").timeline(ANIMATION_TICKS);
            evo1IdleFrames = evo1[0];
            evo1WalkFrames = evo1[1];
            evo1RunFrames = evo1[2];
            evo1AttackFrames = evo1[3];
            evo1DashAttackFrames = evo1[4];
            evo1HurtFrames = evo1[5];
            evo1DieFrames = evo1[6];
            evo1EffectFrames = evo1[7];
            SpriteSheet.Frame[][] evo2 = SpriteSheet.load("res/player/Evo2.png").timeline(ANIMATION_TICKS);
            evo2IdleFrames = evo2[0];
            evo2WalkFrames = evo2[1];
            evo2RunFrames = evo2[2];
            evo2AttackFrames = evo2[3];
            evo2DashAttackFrames = evo2[4];
            evo2HurtFrames = evo2[5];
            evo2DieFrames = evo2[6];
            evo2EffectFrames = evo2[7];
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not load annotated player sprites", e);
        }
    }

    public void update() {

        if (isDead()) {

            return;
        }

        // =====================================================
        // SKILL
        // =====================================================

        if (skillActive) {

            updateSkill();

            return;
        }

        // =====================================================
        // ATTACK 2
        // =====================================================

        if (attack2Active) {

            updateAttack2();

            return;
        }

        // =====================================================
        // DASH
        // =====================================================

        if (dashAttacking) {

            updateDashAttack();

            return;
        }

        // =====================================================
        // ATTACK 1
        // =====================================================

        if (attacking) {

            updateAttack();

            return;
        }

        // =====================================================
        // EFFECT
        // =====================================================

        if (usingEffect) {

            updateEffect();

            return;
        }

        // =====================================================
        // MOVEMENT
        // =====================================================

        int moveX = (right ? 1 : 0) - (left ? 1 : 0);
        int moveY = (down ? 1 : 0) - (up ? 1 : 0);
        boolean moving = moveX != 0 || moveY != 0;

        if (moveX < 0) {

            facingLeft = true;

        } else if (moveX > 0) {

            facingLeft = false;
        }

        double movementScale =
                moveX != 0 && moveY != 0
                ? 0.70710678118
                : 1.0;

        x += (int) Math.round(moveX * SPEED * movementScale);
        y += (int) Math.round(moveY * SPEED * movementScale);

        keepInsideScreen();

        // =====================================================
        // WALK ANIMATION
        // =====================================================

        if (moving) {

            animationCounter++;

            if (
                    animationCounter >=
                    ANIMATION_SPEED
            ) {

                animationCounter = 0;

                currentFrame++;

                if (
                currentFrame >=
                LOCOMOTION_TICKS
                ) {

                    currentFrame = 0;
                }
            }

        } else {

            currentFrame = 0;

            animationCounter = 0;
        }
    }

    // =========================================================
    // ATTACK 1 UPDATE
    // =========================================================

    private void updateAttack() {

        attackCounter++;

        if (
                attackCounter >=
                ATTACK_SPEED
        ) {

            attackCounter = 0;

            attackFrame++;

            if (
                    attackFrame >=
                    ANIMATION_TICKS
            ) {

                attackFrame = 0;

                attacking = false;

                currentFrame = 0;

                animationCounter = 0;
            }
        }
    }

    // =========================================================
    // ATTACK 2 UPDATE
    // =========================================================

    private void updateAttack2() {

        attack2Counter++;

        if (
                attack2Counter >=
                ATTACK2_SPEED
        ) {

            attack2Counter = 0;

            attack2Frame++;

            if (
                    attack2Frame >=
                    ANIMATION_TICKS
            ) {

                attack2Frame = 0;

                attack2Active = false;

                currentFrame = 0;

                animationCounter = 0;
            }
        }
    }

    // =========================================================
    // SKILL UPDATE
    // =========================================================

    private void updateSkill() {

        skillCounter++;

        if (
                skillCounter >=
                SKILL_SPEED
        ) {

            skillCounter = 0;

            skillFrame++;

            if (
                    skillFrame >=
                    ANIMATION_TICKS
            ) {

                skillFrame = 0;

                skillActive = false;

                currentFrame = 0;

                animationCounter = 0;
            }
        }
    }

    // =========================================================
    // DASH UPDATE
    // =========================================================

    private void updateDashAttack() {

        dashAttackCounter++;

        if (
                dashAttackFrame < 8
        ) {

            x += facingLeft
                    ? -DASH_DISTANCE
                    : DASH_DISTANCE;

            keepInsideScreen();
        }

        if (
                dashAttackCounter >=
                DASH_ATTACK_SPEED
        ) {

            dashAttackCounter = 0;

            dashAttackFrame++;

            if (
                    dashAttackFrame >=
                    ANIMATION_TICKS
            ) {

                dashAttackFrame = 0;

                dashAttackCounter = 0;

                dashAttacking = false;

                currentFrame = 0;

                animationCounter = 0;

                keepInsideScreen();
            }
        }
    }

    // =========================================================
    // EFFECT UPDATE
    // =========================================================

    private void updateEffect() {

        effectCounter++;

        if (
                effectCounter >=
                EFFECT_SPEED
        ) {

            effectCounter = 0;

            effectFrame++;

            if (
                    effectFrame >=
                    ANIMATION_TICKS
            ) {

                effectFrame = 0;

                effectCounter = 0;

                usingEffect = false;

                effectType = EFFECT_NONE;

                currentFrame = 0;

                animationCounter = 0;
            }
        }
    }

    // =========================================================
    // SCREEN LIMIT
    // =========================================================

    private void keepInsideScreen() {

        if (x < 0) {

            x = 0;
        }

        if (y < 0) {

            y = 0;
        }

        if (
                x >
                SCREEN_WIDTH - DRAW_WIDTH
        ) {

            x =
                    SCREEN_WIDTH - DRAW_WIDTH;
        }

        if (
                y >
                SCREEN_HEIGHT - DRAW_HEIGHT
        ) {

            y =
                    SCREEN_HEIGHT - DRAW_HEIGHT;
        }
    }

    // =========================================================
    // ATTACK HITBOX
    // =========================================================

    public Rectangle getAttackHitbox() {

        if (
                !attacking &&
                !attack2Active &&
                !dashAttacking &&
                !skillActive
        ) {

            return new Rectangle(
                    0,
                    0,
                    0,
                    0
            );
        }

        int width =
                ATTACK_HITBOX_WIDTH;

        // Skill ของแต่ละร่างมีระยะภาพไม่เท่ากัน
        if (skillActive) {

            if (evolutionStage == EVOLUTION_2) {
                int aoeWidth = 260;
                int aoeHeight = 220;
                return new Rectangle(
                        x + DRAW_WIDTH / 2 - aoeWidth / 2,
                        y + DRAW_HEIGHT / 2 - aoeHeight / 2,
                        aoeWidth,
                        aoeHeight
                );

            } else if (evolutionStage == EVOLUTION_1) {

                width = 200;

            } else {

                width = 160;
            }
        }

        int hitboxX = facingLeft
                ? x - width
                : x + DRAW_WIDTH;

        int hitboxY =
                y
                + DRAW_HEIGHT / 2
                - ATTACK_HITBOX_HEIGHT / 2;

        return new Rectangle(
                hitboxX,
                hitboxY,
                width,
                ATTACK_HITBOX_HEIGHT
        );
    }

    // =========================================================
    // HITBOX ACTIVE
    // =========================================================

    public boolean isAttackHitboxActive() {

        if (attacking) {

            /*
             * Row 3: Frame 0-3 เตรียมท่า,
             * Frame 4-12 คือช่วงดาบฟันจริง
             */
            return attackFrame >= 4 &&
                   attackFrame <= 12;
        }

        if (attack2Active) {

            /*
             * Row 4: เอฟเฟกต์โจมตีเริ่มชัดที่ Frame 3
             * และจบก่อน Frame 13 ซึ่งเป็นท่าเก็บดาบ
             */
            return attack2Frame >= 3 &&
                   attack2Frame <= 12;
        }

        if (dashAttacking) {

            return dashAttackFrame >= 3 &&
                   dashAttackFrame <= 12;
        }

        if (skillActive) {

            return isSkillDamageFrame();
        }

        return false;
    }

    // =========================================================
    // SKILL DAMAGE FRAME
    // =========================================================

    private boolean isSkillDamageFrame() {

        /*
         * Sprite Row 7 ของแต่ละร่างมีจังหวะสกิลต่างกัน:
         * Form 0  : ฟันด้วยพลังสีน้ำเงินช่วงท้าย
         * Form 1  : คอมโบพลังสีเขียวช่วงกลาง
         * Form 2  : ปล่อย Dragon Skill ช่วงท้ายสุด
         */

        if (evolutionStage == EVOLUTION_2) {

            return skillFrame >= 11 &&
                   skillFrame <= 15;
        }

        if (evolutionStage == EVOLUTION_1) {

        return skillFrame >= 8 &&
               skillFrame <= 12;
        }

        return skillFrame >= 8 &&
               skillFrame <= 10;
    }

    // =========================================================
    // ATTACK DAMAGE
    // =========================================================

    public int getAttackDamage() {

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return EVO1_ATTACK_DAMAGE;
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return EVO2_ATTACK_DAMAGE;
        }

        return NORMAL_ATTACK_DAMAGE;
    }

    // =========================================================
    // ATTACK 2 DAMAGE
    // =========================================================

    public int getAttack2Damage() {

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return EVO1_ATTACK2_DAMAGE;
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return EVO2_ATTACK2_DAMAGE;
        }

        return ATTACK2_BASE_DAMAGE;
    }

    // =========================================================
    // SKILL DAMAGE
    // =========================================================

    public int getSkillDamage() {

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return EVO1_SKILL_DAMAGE;
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return EVO2_SKILL_DAMAGE;
        }

        return NORMAL_SKILL_DAMAGE;
    }

    // =========================================================
    // DASH DAMAGE
    // =========================================================

    public int getDashAttackDamage() {

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return EVO1_DASH_DAMAGE;
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return EVO2_DASH_DAMAGE;
        }

        return DASH_ATTACK_DAMAGE;
    }

    // =========================================================
    // CURRENT DAMAGE
    // =========================================================

    public int getCurrentAttackDamage() {

        if (skillActive) {

            return getSkillDamage();
        }

        if (attack2Active) {

            return getAttack2Damage();
        }

        if (dashAttacking) {

            return getDashAttackDamage();
        }

        if (attacking) {

            return getAttackDamage();
        }

        return 0;
    }

    // =========================================================
    // LIFE STEAL
    // =========================================================

    public double getLifeStealPercent() {

        if (evolutionStage == EVOLUTION_1) {
            return EVO1_LIFE_STEAL;
        }
        if (evolutionStage == EVOLUTION_2) {
            return EVO2_LIFE_STEAL;
        }

        return 0.0;
    }

    public boolean hasLifeSteal() {

        return evolutionStage == EVOLUTION_1
                || evolutionStage == EVOLUTION_2;
    }

    // =========================================================
    // LIFE STEAL
    // =========================================================

    public void onAttackHit(
            int damage
    ) {

        if (damage <= 0) {

            return;
        }

        if (!hasLifeSteal()) {

            return;
        }

        int healAmount =
                (int) Math.floor(
                        damage
                        * getLifeStealPercent()
                );

        if (healAmount <= 0) {

            healAmount = 1;
        }

        hp += healAmount;

        if (hp > getMaxHP()) {

            hp = getMaxHP();
        }

        System.out.println(
                "Life Steal +"
                + healAmount
                + " HP"
        );
    }

    // =========================================================
    // DRAW
    // =========================================================

    public void draw(
            Graphics2D g2
    ) {

        SpriteSheet.Frame image = null;

        // =====================================================
        // SKILL
        // =====================================================

        if (skillActive) {

            image =
                    getEffectFrame(
                            skillFrame
                    );
        }

        // =====================================================
        // ATTACK 2
        // =====================================================

        else if (attack2Active) {

            /*
             * Attack 2 ใช้ Animation แถว Attack/Dash
             * เดิมก่อน เราจะผูกกับ Sprite ที่มีอยู่
             */

            image =
                    getAttack2Frame(
                            attack2Frame
                    );
        }

        // =====================================================
        // DASH
        // =====================================================

        else if (dashAttacking) {

            image =
                    getDashAttackFrame(
                            dashAttackFrame
                    );
        }

        // =====================================================
        // ATTACK 1
        // =====================================================

        else if (attacking) {

            image =
                    getAttackFrame(
                            attackFrame
                    );
        }

        // =====================================================
        // EFFECT
        // =====================================================

        else if (usingEffect) {

            image =
                    getEffectFrame(
                            effectFrame
                    );
        }

        // =====================================================
        // WALK
        // =====================================================

        else if (isMoving()) {

            image =
                    getRunFrame(
                            currentFrame
                    );
        }

        // =====================================================
        // IDLE
        // =====================================================

        else {

            image =
                    getIdleFrame(0);
        }

        if (image == null) {

            return;
        }

        SpriteSheet.draw(g2, image, x, y, DRAW_WIDTH, DRAW_HEIGHT, facingLeft);
    }

    // =========================================================
    // IDLE FRAME
    // =========================================================

    private SpriteSheet.Frame getIdleFrame(
            int frame
    ) {

        frame =
                safeFrame(frame);

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return evo1IdleFrames[frame];
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return evo2IdleFrames[frame];
        }

        return idleFrames[frame];
    }

    // =========================================================
    // WALK FRAME
    // =========================================================

    private SpriteSheet.Frame getWalkFrame(
            int frame
    ) {

        frame =
                safeFrame(frame);

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return evo1WalkFrames[frame];
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return evo2WalkFrames[frame];
        }

        return walkFrames[frame];
    }

    private SpriteSheet.Frame getRunFrame(int frame) {
        frame = safeFrame(frame);
        if (evolutionStage == EVOLUTION_1) {
            return evo1RunFrames[frame];
        }
        if (evolutionStage == EVOLUTION_2) {
            return evo2RunFrames[frame];
        }
        return runFrames[frame];
    }

    // =========================================================
    // ATTACK 1 FRAME
    // =========================================================

    private SpriteSheet.Frame getAttackFrame(
            int frame
    ) {

        frame =
                safeFrame(frame);

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return evo1AttackFrames[frame];
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return evo2AttackFrames[frame];
        }

        return attackFrames[frame];
    }

    // =========================================================
    // ATTACK 2 FRAME
    // =========================================================

    private SpriteSheet.Frame getAttack2Frame(
            int frame
    ) {

        frame =
                safeFrame(frame);

        /*
         * ตอนนี้ใช้ Row 4
         * ซึ่งเป็น Animation ชุดที่สอง
         */

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return evo1DashAttackFrames[frame];
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return evo2DashAttackFrames[frame];
        }

        return dashAttackFrames[frame];
    }

    // =========================================================
    // DASH FRAME
    // =========================================================

    private SpriteSheet.Frame getDashAttackFrame(
            int frame
    ) {

        frame =
                safeFrame(frame);

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return evo1DashAttackFrames[frame];
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return evo2DashAttackFrames[frame];
        }

        return dashAttackFrames[frame];
    }

    // =========================================================
    // EFFECT / SKILL FRAME
    // =========================================================

    private SpriteSheet.Frame getEffectFrame(
            int frame
    ) {

        frame =
                safeFrame(frame);

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            return evo1EffectFrames[frame];
        }

        if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            return evo2EffectFrames[frame];
        }

        return effectFrames[frame];
    }

    // =========================================================
    // SAFE FRAME
    // =========================================================

    private int safeFrame(
            int frame
    ) {

        if (frame < 0) {

            return 0;
        }

        if (
                frame >= ANIMATION_TICKS
        ) {

            return ANIMATION_TICKS - 1;
        }

        return frame;
    }

    // =========================================================
    // MOVEMENT CHECK
    // =========================================================

    private boolean isMoving() {

        return up ||
               down ||
               left ||
               right;
    }

    // =========================================================
    // ATTACK 1
    // =========================================================

    public void attack() {

        if (isDead()) return;

        if (attacking) return;

        if (attack2Active) return;

        if (dashAttacking) return;

        if (skillActive) return;

        if (usingEffect) return;

        attacking = true;

        attackFrame = 0;

        attackCounter = 0;

        currentFrame = 0;

        animationCounter = 0;
    }

    // =========================================================
    // ATTACK 2
    // =========================================================

    public void attack2() {

        if (isDead()) return;

        if (attacking) return;

        if (attack2Active) return;

        if (dashAttacking) return;

        if (skillActive) return;

        if (usingEffect) return;

        attack2Active = true;

        attack2Frame = 0;

        attack2Counter = 0;

        currentFrame = 0;

        animationCounter = 0;

        System.out.println(
                "Attack 2!"
        );
    }

    // =========================================================
    // SKILL
    // =========================================================

    public void skill() {

        if (isDead()) return;

        if (attacking) return;

        if (attack2Active) return;

        if (dashAttacking) return;

        if (skillActive) return;

        if (usingEffect) return;

        skillActive = true;

        skillFrame = 0;

        skillCounter = 0;

        currentFrame = 0;

        animationCounter = 0;

        System.out.println(
                "SKILL!"
        );

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            System.out.println(
                    "Evolution 1 Green Skill!"
            );
        }

        else if (
                evolutionStage ==
                EVOLUTION_2
        ) {

            System.out.println(
                    "Evolution 2 Dragon Skill!"
            );
        }

        else {

            System.out.println(
                    "Normal Skill!"
            );
        }
    }

    // =========================================================
    // DASH
    // =========================================================

    public void dashAttack() {

        if (isDead()) return;

        if (attacking) return;

        if (attack2Active) return;

        if (dashAttacking) return;

        if (skillActive) return;

        if (usingEffect) return;

        dashAttacking = true;

        dashAttackFrame = 0;

        dashAttackCounter = 0;

        currentFrame = 0;

        animationCounter = 0;
    }

    // =========================================================
    // HEAL
    // =========================================================

    public void heal() {

        if (isDead()) return;

        if (usingEffect) return;

        if (attacking) return;

        if (attack2Active) return;

        if (dashAttacking) return;

        if (skillActive) return;

        if (potionCount <= 0) {

            System.out.println(
                    "ไม่มี Potion!"
            );

            return;
        }

        if (hp >= getMaxHP()) {

            System.out.println(
                    "HP เต็ม!"
            );

            return;
        }

        potionCount--;

        hp += POTION_HEAL_AMOUNT;

        if (hp > getMaxHP()) {

            hp = getMaxHP();
        }

        usingEffect = true;

        effectType =
                EFFECT_HEAL;

        effectFrame = 0;

        effectCounter = 0;

        currentFrame = 0;

        animationCounter = 0;

        System.out.println(
                "ใช้ Potion!"
        );
    }

    // =========================================================
    // EVOLUTION
    // =========================================================

    public void evolve() {

        if (isDead()) return;

        if (usingEffect) return;

        if (attacking) return;

        if (attack2Active) return;

        if (dashAttacking) return;

        if (skillActive) return;

        // =====================================================
        // FORM 1 -> FORM 2
        // =====================================================

        if (
                evolutionStage ==
                EVOLUTION_0
        ) {

            if (
                    runeCount <
                    EVOLUTION_1_COST
            ) {

                System.out.println(
                        "Need more runes to reach Form 2!"
                );

                System.out.println(
                        "ต้องการ "
                        + EVOLUTION_1_COST
                        + " Rune"
                );

                return;
            }

            runeCount -=
                    EVOLUTION_1_COST;

            int previousMaxHP = getMaxHP();
            evolutionStage = EVOLUTION_1;
            hp = Math.min(getMaxHP(), hp + getMaxHP() - previousMaxHP);

            startEvolutionEffect();

            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "FORM 2!"
            );

            System.out.println(
                    "Spent " + EVOLUTION_1_COST + " runes"
            );

            System.out.println(
                    "Life Steal = 10%"
            );

            System.out.println(
                    "===================================="
            );

            return;
        }

        // =====================================================
        // FORM 2 -> FORM 3
        // =====================================================

        if (
                evolutionStage ==
                EVOLUTION_1
        ) {

            if (
                    runeCount <
                    EVOLUTION_2_COST
            ) {

                System.out.println(
                        "Need more runes to reach Form 3!"
                );

                System.out.println(
                        "ต้องการ "
                        + EVOLUTION_2_COST
                        + " Rune"
                );

                return;
            }

            runeCount -=
                    EVOLUTION_2_COST;

            int previousMaxHP = getMaxHP();
            evolutionStage = EVOLUTION_2;
            hp = Math.min(getMaxHP(), hp + getMaxHP() - previousMaxHP);

            startEvolutionEffect();

            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "FORM 3!"
            );

            System.out.println(
                    "Spent " + EVOLUTION_2_COST + " runes"
            );

            System.out.println(
                    "Life Steal = 20%"
            );

            System.out.println(
                    "Dragon Skill พร้อมใช้งาน!"
            );

            System.out.println(
                    "===================================="
            );

            return;
        }

        System.out.println(
                "อยู่ร่างสูงสุดแล้ว!"
        );
    }

    // =========================================================
    // EVOLUTION EFFECT
    // =========================================================

    private void startEvolutionEffect() {

        usingEffect = true;

        effectType =
                EFFECT_EVOLUTION;

        effectFrame = 0;

        effectCounter = 0;

        currentFrame = 0;

        animationCounter = 0;

        attacking = false;

        attack2Active = false;

        dashAttacking = false;

        skillActive = false;
    }

    // =========================================================
    // TAKE DAMAGE
    // =========================================================

    public void takeDamage(
            int damage
    ) {

        if (isDead()) return;

        if (damage <= 0) return;

        hp -= damage;

        if (hp < 0) {

            hp = 0;
        }

        attacking = false;

        attack2Active = false;

        dashAttacking = false;

        skillActive = false;

        if (hp > 0) {

            currentFrame = 0;

            animationCounter = 0;
        }

        if (hp <= 0) {

            hp = 0;

            usingEffect = false;

            effectType =
                    EFFECT_NONE;

            effectFrame = 0;

            effectCounter = 0;

            System.out.println(
                    "Player ตาย!"
            );
        }

        System.out.println(
                "Player HP = "
                + hp
                + " / "
                + getMaxHP()
        );
    }

    // =========================================================
    // HP
    // =========================================================

    public int getHP() {

        return hp;
    }

    public int getMaxHP() {

        return MAX_HP + evolutionStage * HP_PER_FORM;
    }

    public boolean isDead() {

        return hp <= 0;
    }

    // =========================================================
    // EVOLUTION
    // =========================================================

    public int getEvolutionStage() {

        return evolutionStage;
    }

    public boolean isEvolved() {

        return evolutionStage > 0;
    }

    public boolean isEvolution1() {

        return evolutionStage ==
                EVOLUTION_1;
    }

    public boolean isEvolution2() {

        return evolutionStage ==
                EVOLUTION_2;
    }

    // =========================================================
    // COST
    // =========================================================

    public int getEvolution1Cost() {

        return EVOLUTION_1_COST;
    }

    public int getEvolution2Cost() {

        return EVOLUTION_2_COST;
    }

    // =========================================================
    // STATES
    // =========================================================

    public boolean isUsingEffect() {

        return usingEffect;
    }

    public boolean isAttacking() {

        return attacking;
    }

    public boolean isAttack2Active() {

        return attack2Active;
    }

    public boolean isSkillActive() {

        return skillActive;
    }

    public boolean isDashAttacking() {

        return dashAttacking;
    }

    // =========================================================
    // MOVEMENT
    // =========================================================

    public void setUp(
            boolean value
    ) {

        up = value;
    }

    public void setDown(
            boolean value
    ) {

        down = value;
    }

    public void setLeft(
            boolean value
    ) {

        left = value;
    }

    public void setRight(
            boolean value
    ) {

        right = value;
    }

    // =========================================================
    // POSITION
    // =========================================================

    public int getX() {

        return x;
    }

    public int getY() {

        return y;
    }

    public void setX(
            int x
    ) {

        this.x = x;

        keepInsideScreen();
    }

    public void setY(
            int y
    ) {

        this.y = y;

        keepInsideScreen();
    }

    // =========================================================
    // POTION
    // =========================================================

    public void addPotion() {

        potionCount++;

        System.out.println(
                "Potion = "
                + potionCount
        );
    }

    public int getPotionCount() {

        return potionCount;
    }

    // =========================================================
    // RUNE
    // =========================================================

    public void addRune() {

        runeCount++;

        System.out.println(
                "Rune = "
                + runeCount
        );
    }

    public int getRuneCount() {

        return runeCount;
    }

    // =========================================================
    // SUMMON
    // =========================================================

    public boolean isSummonUnlocked() {

        return summonUnlocked;
    }

    public void unlockSummon() {

        summonUnlocked = true;

        System.out.println(
                "===================================="
        );

        System.out.println(
                "SUMMON UNLOCKED!"
        );

        System.out.println(
                "===================================="
        );
    }
}
