import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.BasicStroke;

public class Boss {

    private static final int SCREEN_WIDTH = 1200;
    private static final int SCREEN_HEIGHT = 700;

    private static final int DRAW_WIDTH = 87;
    private static final int DRAW_HEIGHT = 116;
    private static final int HITBOX_WIDTH = 62;
    private static final int HITBOX_HEIGHT = 76;

    // Logical animation clock, not the number of columns in any source image.
    private static final int ANIMATION_TICKS = 16;
    private static final int ROW_COUNT = 8;
    private static final int ROW_IDLE = 0;
    private static final int ROW_WALK = 1;
    private static final int ROW_RUN = 2;
    private static final int ROW_ATTACK_1 = 3;
    private static final int ROW_ATTACK_2 = 4;
    private static final int ROW_HURT = 5;
    private static final int ROW_DIE = 6;
    private static final int ROW_SKILL = 7;

    private static final String[] BOSS_NAMES = {
        "Flame Knight",
        "Frost Warden",
        "Storm Reaper",
        "Demon King"
    };

    private static final String[] BOSS_FILES = {
        "res/boss/boss1.png",
        "res/boss/boss2.png",
        "res/boss/boss3.png",
        "res/boss/boss4.png"
    };

    private static final int[] BOSS_HP = {
        1000, 1250, 1500, 2000
    };

    private static final int[] BOSS_DAMAGE = {
        18, 22, 26, 32
    };

    private static final int[] BOSS_SPEED = {
        2, 2, 3, 3
    };

    private static final long[] ATTACK_COOLDOWN = {
        1200, 1100, 1000, 900
    };

    private static final long ATTACK_WINDUP = 450;
    private static final long ATTACK_RECOVERY = 350;
    private static final int ATTACK_RANGE = 112;

    private final int bossNumber;
    private final String name;
    private final int maxHP;
    private final int attackDamage;
    private final int speed;
    private final long attackCooldown;

    private int x;
    private int y;
    private int hp;

    private SpriteSheet.Frame[][] frames =
            new SpriteSheet.Frame[ROW_COUNT][ANIMATION_TICKS];

    private int currentFrame = 0;
    private int animationCounter = 0;
    private int currentRow = ROW_IDLE;
    private int attackCounter = 0;

    private boolean attacking = false;
    private boolean attackDamageApplied = false;
    private boolean facingLeft = false;
    private boolean deathAnimationFinished = false;
    private boolean attackingSummon = false;

    private long lastAttackTime = 0;
    private long attackStartTime = 0;

    public Boss(int x, int y) {

        this(x, y, 1);
    }

    public Boss(int x, int y, int bossNumber) {

        this.bossNumber = Math.max(1, Math.min(4, bossNumber));
        this.name = BOSS_NAMES[this.bossNumber - 1];
        this.maxHP = BOSS_HP[this.bossNumber - 1];
        this.attackDamage = BOSS_DAMAGE[this.bossNumber - 1];
        this.speed = BOSS_SPEED[this.bossNumber - 1];
        this.attackCooldown = ATTACK_COOLDOWN[this.bossNumber - 1];

        this.x = x;
        this.y = y;
        this.hp = maxHP;

        loadSpriteSheet();
        keepInsideScreen();
    }

    private void loadSpriteSheet() {
        try {
            frames = SpriteSheet.load(BOSS_FILES[bossNumber - 1]).timeline(ANIMATION_TICKS);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not load annotated boss sprites", e);
        }
    }

    public void update(Player player) {

        update(player, null);
    }

    public void update(Player player, Summon summon) {

        if (isDead()) {

            updateDeathAnimation();
            return;
        }

        if (player == null || player.isDead()) {

            currentRow = ROW_IDLE;
            advanceAnimation(5, 8);
            return;
        }

        int dx = player.getX() + 31 - (x + DRAW_WIDTH / 2);
        int dy = player.getY() + 42 - (y + DRAW_HEIGHT / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);

        int summonDx = summon == null || summon.isDead()
                ? Integer.MAX_VALUE
                : summon.getX() + 37 - (x + DRAW_WIDTH / 2);
        int summonDy = summon == null || summon.isDead()
                ? Integer.MAX_VALUE
                : summon.getY() + 50 - (y + DRAW_HEIGHT / 2);
        double summonDistance = summon == null || summon.isDead()
                ? Double.MAX_VALUE
                : Math.sqrt(summonDx * summonDx + summonDy * summonDy);

        facingLeft = dx < 0;

        if (attacking) {

            updateAttack(player, summon);
            return;
        }

        if (
                summonDistance <= ATTACK_RANGE &&
                summonDistance < distance
        ) {

            facingLeft = summonDx < 0;
            startAttack(true);
            return;
        }

        if (distance <= ATTACK_RANGE) {

            startAttack(false);
            return;
        }

        currentRow = speed >= 3 ? ROW_RUN : ROW_WALK;

        if (distance > 0) {

            x += (int) Math.round((dx / distance) * speed);
            y += (int) Math.round((dy / distance) * speed);
        }

        keepInsideScreen();
        advanceAnimation(4, 8);
    }

    private void startAttack(boolean targetSummon) {

        long now = System.currentTimeMillis();

        if (now - lastAttackTime < attackCooldown) {

            currentRow = ROW_IDLE;
            advanceAnimation(6, 8);
            return;
        }

        attacking = true;
        attackingSummon = targetSummon;
        attackDamageApplied = false;
        attackStartTime = now;
        lastAttackTime = now;
        attackCounter = 0;
        currentFrame = 0;

        int attacksSoFar = (int) (now / attackCooldown);

        if (bossNumber == 4 && attacksSoFar % 3 == 2) {

            currentRow = ROW_SKILL;

        } else if (attacksSoFar % 2 == 1) {

            currentRow = ROW_ATTACK_2;

        } else {

            currentRow = ROW_ATTACK_1;
        }
    }

    private void updateAttack(Player player, Summon summon) {

        long elapsed = System.currentTimeMillis() - attackStartTime;

        attackCounter++;

        if (attackCounter >= 3) {

            attackCounter = 0;
            currentFrame++;

            if (currentFrame >= ANIMATION_TICKS) {

                currentFrame = ANIMATION_TICKS - 1;
            }
        }

        if (elapsed >= ATTACK_WINDUP && !attackDamageApplied) {

            attackDamageApplied = true;

            double distance;

            if (attackingSummon && summon != null && !summon.isDead()) {

                int dx = summon.getX() + 37 - (x + DRAW_WIDTH / 2);
                int dy = summon.getY() + 50 - (y + DRAW_HEIGHT / 2);
                distance = Math.sqrt(dx * dx + dy * dy);

            } else {

                int dx = player.getX() + 31 - (x + DRAW_WIDTH / 2);
                int dy = player.getY() + 42 - (y + DRAW_HEIGHT / 2);
                distance = Math.sqrt(dx * dx + dy * dy);
                attackingSummon = false;
            }

            if (distance <= ATTACK_RANGE + 36) {

                int damage = attackDamage;

                if (currentRow == ROW_SKILL) {

                    damage += 12;
                }

                if (attackingSummon && summon != null) {

                    summon.takeDamage(damage);

                } else {

                    player.takeDamage(damage);
                }

                System.out.println(name + " hits for " + damage + " damage.");
            }
        }

        if (elapsed >= ATTACK_WINDUP + ATTACK_RECOVERY) {

            attacking = false;
            currentRow = ROW_IDLE;
            currentFrame = 0;
            animationCounter = 0;
        }
    }

    private void updateDeathAnimation() {

        if (deathAnimationFinished) {

            return;
        }

        currentRow = ROW_DIE;
        advanceAnimation(5, ANIMATION_TICKS);

        if (currentFrame == ANIMATION_TICKS - 1) {

            deathAnimationFinished = true;
        }
    }

    private void advanceAnimation(int frameSpeed, int frameLimit) {

        animationCounter++;

        if (animationCounter >= frameSpeed) {

            animationCounter = 0;
            currentFrame++;

            if (currentFrame >= frameLimit) {

                currentFrame = 0;
            }
        }
    }

    private void keepInsideScreen() {

        x = Math.max(0, Math.min(SCREEN_WIDTH - DRAW_WIDTH, x));
        y = Math.max(0, Math.min(SCREEN_HEIGHT - DRAW_HEIGHT, y));
    }

    public void draw(Graphics2D g2) {

        SpriteSheet.Frame image = getCurrentFrame();

        if (image != null) {

            SpriteSheet.draw(g2, image, x, y, DRAW_WIDTH, DRAW_HEIGHT, facingLeft);
        } else if (!isDead()) {

            g2.setColor(new Color(160, 30, 30));
            g2.fillOval(x, y, DRAW_WIDTH, DRAW_HEIGHT);
        }

        drawBossHealthBar(g2);
    }

    private SpriteSheet.Frame getCurrentFrame() {

        int safeRow = Math.max(0, Math.min(frames.length - 1, currentRow));
        int safeFrame = Math.max(0, Math.min(ANIMATION_TICKS - 1, currentFrame));

        return frames[safeRow][safeFrame];
    }

    private void drawBossHealthBar(Graphics2D g2) {

        int barWidth = 330;
        int barHeight = 16;
        int barX = (SCREEN_WIDTH - barWidth) / 2;
        int barY = 37;
        int panelX = barX - 36;
        int panelY = 8;
        int panelWidth = barWidth + 72;
        int panelHeight = 58;
        int healthWidth = (int) ((double) hp / maxHP * barWidth);

        g2.setColor(new Color(12, 15, 22, 225));
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);
        g2.setColor(new Color(191, 149, 78, 210));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);

        g2.setColor(new Color(244, 214, 153));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        g2.drawString("BOSS " + bossNumber + "  ·  " + name, barX, 28);
        String healthText = hp + " / " + maxHP;
        g2.setColor(new Color(240, 233, 217));
        g2.setFont(g2.getFont().deriveFont(java.awt.Font.PLAIN, 11f));
        g2.drawString(healthText, barX + barWidth - g2.getFontMetrics().stringWidth(healthText), 28);

        g2.setColor(new Color(37, 35, 38));
        g2.fillRoundRect(barX, barY, barWidth, barHeight, 8, 8);
        int visibleHealthWidth = Math.max(0, Math.min(barWidth, healthWidth));
        if (visibleHealthWidth > 0) {
            g2.setPaint(new GradientPaint(barX, 0, new Color(230, 75, 66), barX + barWidth, 0, new Color(137, 24, 37)));
            g2.fillRoundRect(barX, barY, visibleHealthWidth, barHeight, 8, 8);
        }

        if (attacking) {

            g2.setColor(new Color(255, 194, 97));
            g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 11f));
            g2.drawString("ATTACK!", barX + barWidth - 54, barY + 21);
        }
    }

    public void takeDamage(int damage) {

        if (isDead() || damage <= 0) {

            return;
        }

        hp = Math.max(0, hp - damage);

        if (isDead()) {

            currentRow = ROW_DIE;
            currentFrame = 0;
            animationCounter = 0;
            deathAnimationFinished = false;

        } else {

            currentRow = ROW_HURT;
            currentFrame = 0;
            animationCounter = 0;
        }
    }

    public Rectangle getHitbox() {

        int hitboxX = x + (DRAW_WIDTH - HITBOX_WIDTH) / 2;
        int hitboxY = y + DRAW_HEIGHT - HITBOX_HEIGHT;

        return new Rectangle(hitboxX, hitboxY, HITBOX_WIDTH, HITBOX_HEIGHT);
    }

    public boolean isDead() {

        return hp <= 0;
    }

    public int getHP() {

        return hp;
    }

    public int getMaxHP() {

        return maxHP;
    }

    public int getX() {

        return x;
    }

    public int getY() {

        return y;
    }

    public String getName() {

        return name;
    }

    public int getBossNumber() {

        return bossNumber;
    }

    public boolean isAttacking() {

        return attacking;
    }
}
