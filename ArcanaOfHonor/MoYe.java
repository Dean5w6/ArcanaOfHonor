import greenfoot.*;

public class MoYe extends Hero {
    public static int MO_ULT_INTRO_START_Y_OFFSET = -120;
    public static int MO_ULT_INTRO_DROP_PER_STEP = 40;
    public static int MO_ULT_INTRO_STEPS = 3;
    public static int MO_ULT_INTRO_TICKS_PER_STEP = 2;

    public static int MO_ULT_FIRSTFRAME_Y_OFFSET = 0;

    public static int[] MO_ULT_INTRO_Y_OFFSETS = {
        -800,
        -520,
        -190
    };

    public static int MO_ULT_INTRO_TICKS_PER_FRAME = 5;

    public MoYe(boolean isEnemy) {
        super(isEnemy, 2000, "Mo Ye");
        setImage(setupImage("mo_idle.png"));
    }
    @Override protected String getHeroPrefix() {
        return "mo";
    }
}