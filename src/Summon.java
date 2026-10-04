import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Summon {

    private static final int DRAW_WIDTH = 75;
    private static final int DRAW_HEIGHT = 100;
    private static final int MOVE_SPEED = 3;
    private static final int FOLLOW_DISTANCE = 105;
    private static final int ATTACK_RANGE = 70;
    private static final int ATTACK_DAMAGE = 35;
    private static final int MAX_HP = 120;
    private static final long ATTACK_COOLDOWN = 850;
    // Logical animation clock, not the number of columns in any source image.
    private static final int ANIMATION_TICKS = 16;
    private static final int LOCOMOTION_TICKS = 8;
    private static final int ROW_IDLE = 0;
    private static final int ROW_WALK = 1;
    private static final int ROW_RUN = 2;
    private static final int ROW_ATTACK_1 = 3;
    private static final int ROW_ATTACK_2 = 4;
    private static final int ROW_SKILL = 7;
    private static final long ATTACK_ANIMATION_DURATION = 560;
    private static final long ATTACK_FRAME_DURATION = 35;

    private SpriteSheet.Frame[][] frames = new SpriteSheet.Frame[8][ANIMATION_TICKS];

    private int x;
    private int y;
    private int hp = MAX_HP;
    private long lastAttackTime = 0;
    private boolean moving = false;
    private boolean running = false;
    private boolean facingLeft = false;
    private int attackCount = 0;

    public Summon(int x, int y) {

        this.x = x;
        this.y = y;
        loadSpriteSheet();
    }

    private void loadSpriteSheet() {
        try {
            frames = SpriteSheet.load("res/summon/summon.png").timeline(ANIMATION_TICKS);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not load annotated summon sprites", e);
        }
    }

    public void update(Player player, Monster[] monsters, Boss boss) {

        moving = false;
        running = false;

        if (isDead() || player == null || player.isDead()) {

            return;
        }

        int targetX = -1;
        int targetY = -1;
        int nearestDistanceSquared = 260 * 260;
        Monster targetMonster = null;
        boolean targetBoss = false;

        if (boss != null && !boss.isDead()) {

            int dx = boss.getX() + 43 - (x + DRAW_WIDTH / 2);
            int dy = boss.getY() + 58 - (y + DRAW_HEIGHT / 2);
            int distanceSquared = dx * dx + dy * dy;

            if (distanceSquared <= nearestDistanceSquared) {

                nearestDistanceSquared = distanceSquared;
                targetX = boss.getX() + 43;
                targetY = boss.getY() + 58;
                targetBoss = true;
            }

        } else if (monsters != null) {

            for (Monster monster : monsters) {

                if (monster == null || monster.isDead()) {

                    continue;
                }

                int dx = monster.getX() + 27 - (x + DRAW_WIDTH / 2);
                int dy = monster.getY() + 36 - (y + DRAW_HEIGHT / 2);
                int distanceSquared = dx * dx + dy * dy;

                if (distanceSquared < nearestDistanceSquared) {

                    nearestDistanceSquared = distanceSquared;
                    targetMonster = monster;
                    targetX = monster.getX() + 27;
                    targetY = monster.getY() + 36;
                    targetBoss = false;
                }
            }
        }

        if (targetX >= 0) {

            int dx = targetX - (x + DRAW_WIDTH / 2);
            int dy = targetY - (y + DRAW_HEIGHT / 2);
            double distance = Math.sqrt(dx * dx + dy * dy);
            facingLeft = dx < 0;

            if (distance <= ATTACK_RANGE) {

                attack(targetMonster, boss, targetBoss);

            } else if (distance > 0) {

                facingLeft = dx < 0;
                running = true;
                move(dx, dy, distance);
                moving = true;
            }

            return;
        }

        int playerCenterX = player.getX() + 31;
        int playerCenterY = player.getY() + 42;
        int dx = playerCenterX - (x + DRAW_WIDTH / 2);
        int dy = playerCenterY - (y + DRAW_HEIGHT / 2);
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > FOLLOW_DISTANCE && distance > 0) {

            facingLeft = dx < 0;
            running = true;
            move(dx, dy, distance);
            moving = true;
        }
    }

    private void move(int dx, int dy, double distance) {

        x += (int) Math.round(dx / distance * MOVE_SPEED);
        y += (int) Math.round(dy / distance * MOVE_SPEED);

        x = Math.max(0, Math.min(1200 - DRAW_WIDTH, x));
        y = Math.max(0, Math.min(700 - DRAW_HEIGHT, y));
    }

    private void attack(Monster monster, Boss boss, boolean targetBoss) {

        long now = System.currentTimeMillis();

        if (now - lastAttackTime < ATTACK_COOLDOWN) {

            return;
        }

        lastAttackTime = now;
        attackCount++;

        if (targetBoss && boss != null && !boss.isDead()) {

            boss.takeDamage(ATTACK_DAMAGE);

        } else if (monster != null && !monster.isDead()) {

            monster.takeDamage(ATTACK_DAMAGE);
        }
    }

    public void takeDamage(int damage) {

        if (damage > 0 && !isDead()) {

            hp = Math.max(0, hp - damage);
        }
    }

    public Rectangle getHitbox() {

        return new Rectangle(x + 12, y + 15, DRAW_WIDTH - 24, DRAW_HEIGHT - 28);
    }

    public void draw(Graphics2D g2) {

        if (isDead()) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lastAttackTime;
        boolean attacking = lastAttackTime > 0 && elapsed < ATTACK_ANIMATION_DURATION;
        int row;
        int frame;

        if (attacking) {
            row = attackCount % 3 == 0 ? ROW_SKILL
                    : (attackCount % 2 == 0 ? ROW_ATTACK_2 : ROW_ATTACK_1);
            frame = Math.min(ANIMATION_TICKS - 1, (int) (elapsed / ATTACK_FRAME_DURATION));
        } else if (moving) {
            row = running ? ROW_RUN : ROW_WALK;
            frame = (int) ((now / 90) % LOCOMOTION_TICKS);
        } else {
            row = ROW_IDLE;
            frame = (int) ((now / 180) % LOCOMOTION_TICKS);
        }

        SpriteSheet.Frame image = frames[row][frame];

        if (image != null) {
            SpriteSheet.draw(g2, image, x, y, DRAW_WIDTH, DRAW_HEIGHT, facingLeft);
        } else {
            g2.setColor(new Color(100, 210, 255));
            g2.fillOval(x, y, DRAW_WIDTH, DRAW_HEIGHT);
        }

        int barWidth = DRAW_WIDTH;
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, y + DRAW_HEIGHT + 3, barWidth, 5);

        g2.setColor(new Color(40, 210, 140));
        g2.fillRect(x, y + DRAW_HEIGHT + 3, (int) ((double) hp / MAX_HP * barWidth), 5);
    }

    public boolean isDead() {

        return hp <= 0;
    }

    public int getX() {

        return x;
    }

    public int getY() {

        return y;
    }

    public int getHP() {

        return hp;
    }
}
