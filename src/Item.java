import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class Item {

    // =========================================================
    // ITEM TYPE
    // =========================================================

    public static final int RUNE = 0;
    public static final int POTION = 1;

    // =========================================================
    // POSITION
    // =========================================================

    private int x;
    private int y;

    // =========================================================
    // NAME
    // =========================================================

    private String name;

    // =========================================================
    // TYPE
    // =========================================================

    private int type;

    // =========================================================
    // IMAGE
    // =========================================================

    private BufferedImage image;

    // =========================================================
    // DRAW SIZE
    // =========================================================

    private static final int WIDTH = 48;
    private static final int HEIGHT = 48;

    // =========================================================
    // SPRITESHEET
    // =========================================================

    private BufferedImage spriteSheet;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Item(String name, int type) {

        this.name = name;
        this.type = type;

        this.x = 0;
        this.y = 0;

        loadImage();
    }

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Item(int x, int y, int type) {

        this.x = x;
        this.y = y;
        this.type = type;

        this.name = getNameFromType();

        loadImage();
    }

    // =========================================================
    // LOAD SPRITESHEET
    // =========================================================

    private void loadImage() {

        try {

            File file =
                    new File(
                            "res/item/item.png"
                    );

            System.out.println(
                    "กำลังโหลด Item Spritesheet:"
            );

            System.out.println(
                    file.getAbsolutePath()
            );

            // =================================================
            // CHECK FILE
            // =================================================

            if (!file.exists()) {

                System.out.println(
                        "ERROR: ไม่พบไฟล์ item.png"
                );

                image = null;

                return;
            }

            // =================================================
            // LOAD ONE IMAGE
            // =================================================

            spriteSheet =
                    ImageIO.read(file);

            if (spriteSheet == null) {

                System.out.println(
                        "ERROR: อ่าน item.png ไม่ได้"
                );

                image = null;

                return;
            }

            System.out.println(
                    "โหลด Spritesheet สำเร็จ"
            );

            System.out.println(
                    "ขนาด: "
                    + spriteSheet.getWidth()
                    + " x "
                    + spriteSheet.getHeight()
            );

            // =================================================
            // CROP ITEM
            // =================================================

            if (type == RUNE) {

                image = cropRune();

            } else if (type == POTION) {

                image = cropPotion();

            } else {

                image = null;
            }

            // =================================================
            // RESULT
            // =================================================

            if (image != null) {

                System.out.println(
                        "โหลด "
                        + getName()
                        + " สำเร็จ!"
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "ERROR: โหลด Item ไม่สำเร็จ!"
            );

            e.printStackTrace();

            image = null;
        }
    }

    // =========================================================
    // CROP RUNE
    // =========================================================

    private BufferedImage cropRune() {

        /*
         * รูป Rune อยู่แถวบนของ spritesheet
         *
         * เลือก Rune ตัวแรก
         */

        int cropX = 45;
        int cropY = 55;

        int cropWidth = 135;
        int cropHeight = 190;

        return spriteSheet.getSubimage(
                cropX,
                cropY,
                cropWidth,
                cropHeight
        );
    }

    // =========================================================
    // CROP POTION
    // =========================================================

    private BufferedImage cropPotion() {

        /*
         * รูป Potion อยู่แถวที่ 3
         *
         * เลือก Potion ตัวแรก
         */

        int cropX = 45;
        int cropY = 460;

        int cropWidth = 135;
        int cropHeight = 190;

        return spriteSheet.getSubimage(
                cropX,
                cropY,
                cropWidth,
                cropHeight
        );
    }

    // =========================================================
    // DRAW
    // =========================================================

    public void draw(Graphics2D g2) {

        // =====================================================
        // ถ้ารูปโหลดไม่ได้
        // =====================================================

        if (image == null) {

            if (type == POTION) {

                g2.setColor(Color.RED);

            } else {

                g2.setColor(Color.BLUE);
            }

            g2.fillOval(
                    x,
                    y,
                    WIDTH,
                    HEIGHT
            );

            return;
        }

        // =====================================================
        // DRAW ITEM
        // =====================================================

        g2.drawImage(
                image,
                x,
                y,
                WIDTH,
                HEIGHT,
                null
        );
    }

    // =========================================================
    // COLLISION
    // =========================================================

    public boolean checkCollision(
            Player player
    ) {

        if (player == null) {

            return false;
        }

        Rectangle itemHitbox =
                new Rectangle(
                        x,
                        y,
                        WIDTH,
                        HEIGHT
                );

        Rectangle playerHitbox =
                new Rectangle(
                        player.getX(),
                        player.getY(),
                        96,
                        128
                );

        return itemHitbox.intersects(
                playerHitbox
        );
    }

    // =========================================================
    // GET NAME
    // =========================================================

    public String getName() {

        if (name != null &&
                !name.isEmpty()) {

            return name;
        }

        return getNameFromType();
    }

    // =========================================================
    // GET NAME FROM TYPE
    // =========================================================

    private String getNameFromType() {

        if (type == POTION) {

            return "Potion";
        }

        if (type == RUNE) {

            return "Rune";
        }

        return "Unknown";
    }

    // =========================================================
    // GET TYPE
    // =========================================================

    public int getType() {

        return type;
    }

    // =========================================================
    // GET X
    // =========================================================

    public int getX() {

        return x;
    }

    // =========================================================
    // GET Y
    // =========================================================

    public int getY() {

        return y;
    }

    // =========================================================
    // SET X
    // =========================================================

    public void setX(int x) {

        this.x = x;
    }

    // =========================================================
    // SET Y
    // =========================================================

    public void setY(int y) {

        this.y = y;
    }

    // =========================================================
    // GET WIDTH
    // =========================================================

    public int getWidth() {

        return WIDTH;
    }

    // =========================================================
    // GET HEIGHT
    // =========================================================

    public int getHeight() {

        return HEIGHT;
    }
}