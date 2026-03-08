package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class RouletteScreen extends HandledScreen<RouletteScreenHandler> {

    private static final Identifier BG_TEXTURE =
            new Identifier(Szar.MOD_ID, "textures/gui/roulette.png");

    // Add these fields to the class
    private int dotAnimFrame = 0;
    private int dotAnimTick  = 0;
    private static final int DOT_FRAME_DURATION = 20;
    private static final String[] DOT_FRAMES = {
            "...", "⋅..", "⋅⋅.", ".⋅⋅", "..⋅", "...", "...", "...", "..."
    };
    private boolean showingWinner = false;
    private int     stoppedTick   = 0; // elapsed ticks since spin stopped

    private final PlayerInventory inventory;
    private final RouletteBlockEntity blockEntity;
    private final RouletteScreenHandler handler;

    public String spinString  = "Next spin in: 0s";
    public int nextspinTime   = 0;
    public boolean isIntermission = true;
    private int winnernum     = 0;

    private float wheelDeg = 0f;
    private float ballDeg  = 0f;

    // Spin start positions (captured when spin begins)
    private float wheelStartDeg = 0f;
    private float ballStartDeg  = 0f;

    // Total distance each will travel over the entire spin duration
    private float wheelTotalDist = 0f;
    private float ballTotalDist  = 0f;

    // Total ticks for the active spin (rollingTicks + halfWaitTicks)
    private int spinTotalTicks = 0;

    // How many ticks of the spin are "locked still" at the end (second half of wait)
    private int spinStopTicks  = 0;

    private boolean spinInitialized = false;
    private int lastElapsed = -1;

    // Full speed in degrees/tick — used to compute how far wheel/ball travel
    // during the constant-speed portion so total distance lands on target
    private static final float WHEEL_FULL_SPEED = 8.0f;
    private static final float BALL_FULL_SPEED  = 14.0f;

    public RouletteScreen(RouletteScreenHandler handler,
                          PlayerInventory inventory,
                          Text title) {
        super(handler, inventory, title);
        this.handler     = handler;
        this.blockEntity = handler.blockEntity;
        this.backgroundWidth  = 326;
        this.backgroundHeight = 194;
        this.inventory   = inventory;
    }

    // Ease curve: constant speed phase (0→splitT) then ease-out (splitT→1)
    // At t=splitT the curve transitions smoothly — the ease-out's initial
    // derivative is set to match FULL_SPEED so there is no speed discontinuity.
    //
    // For t in [0, splitT]:   pos(t) = fullSpeed * t * totalTicks
    // For t in [splitT, 1]:   pos(t) = pos(splitT) + decelDist * easeOut((t-splitT)/(1-splitT))
    //
    // easeOut(u) = 2u - u²  →  derivative at u=0 is 2/totalDecelTicks * decelDist
    // We need that to equal fullSpeed * totalTicks (per-tick speed at split point).
    // This is satisfied automatically because we set decelDist so the curve lands on target,
    // and the split is at the rollingTicks boundary.

    private void onSpinStart(int rollingTicks, int halfWaitTicks) {
        spinTotalTicks = rollingTicks + halfWaitTicks;
        spinStopTicks  = blockEntity.wheelWaitSeconds * 20 - halfWaitTicks; // second half of wait

        wheelStartDeg = wheelDeg;
        ballStartDeg  = ballDeg;

        float targetWheelDeg = RouletteBlockEntity.getWheelDegrees(winnernum);
        float targetBallDeg  = 0f;

        // Distance covered during constant phase (phase1)
        float wheelPhase1Dist = WHEEL_FULL_SPEED * rollingTicks;
        float ballPhase1Dist  = BALL_FULL_SPEED  * rollingTicks;

        // Where each ends up after phase1
        float wheelAfterPhase1 = (wheelStartDeg + wheelPhase1Dist) % 360f;
        float ballAfterPhase1  = (ballStartDeg  + ballPhase1Dist)  % 360f;

        // Phase2 must cover exactly the right distance to land on target.
        // Pick the smallest distance >= a minimum so the wheel visibly decelerates.
        float minDecelRotations = 1.0f; // at least 1 full extra rotation during decel
        float wheelPhase2Dist = positiveMod(targetWheelDeg - wheelAfterPhase1, 360f);
        if (wheelPhase2Dist < 360f * minDecelRotations) wheelPhase2Dist += 360f;

        float ballPhase2Dist = positiveMod(targetBallDeg - ballAfterPhase1, 360f);
        if (ballPhase2Dist < 360f * minDecelRotations) ballPhase2Dist += 360f;

        wheelTotalDist = wheelPhase1Dist + wheelPhase2Dist;
        ballTotalDist  = ballPhase1Dist  + ballPhase2Dist;

        spinInitialized = true;
    }

    // Returns position fraction [0,1] along the total distance given elapsed ticks.
    // Uses a piecewise curve: linear up to splitTick, then ease-out after.
    // The ease-out's initial slope matches the linear slope → no speed jump.
    private float curvePosition(int elapsed, int splitTick, int totalTicks,
                                float phase1Dist, float totalDist) {
        if (elapsed <= 0)          return 0f;
        if (elapsed >= totalTicks) return 1f;

        float phase2Dist = totalDist - phase1Dist;

        if (elapsed <= splitTick) {
            // Linear phase
            return (phase1Dist * ((float) elapsed / splitTick)) / totalDist;
        } else {
            // Ease-out phase: u goes 0→1 over (totalTicks - splitTick) ticks
            float u = (float)(elapsed - splitTick) / (totalTicks - splitTick);
            // easeOut(u) = 2u - u²  (starts at slope 2, ends at slope 0)
            float eased = 2f * u - u * u;
            return (phase1Dist + eased * phase2Dist) / totalDist;
        }
    }

    public void tickScreen() {
        nextspinTime   = handler.getPropertyDelegate().get(1);
        isIntermission = handler.getPropertyDelegate().get(0) == 1;
        winnernum      = handler.getPropertyDelegate().get(2);

        if (isIntermission) {
            spinString      = "Next spin in: " + (nextspinTime + 19) / 20 + "s";
            spinInitialized = false;
            lastElapsed     = -1;
            showingWinner   = false;
            stoppedTick     = 0;
            wheelDeg        = RouletteBlockEntity.getWheelDegrees(winnernum);
            ballDeg         = 0f;
            return;
        }

        int rollingTicks  = blockEntity.wheelRollingSeconds * 20;
        int waitTicks     = blockEntity.wheelWaitSeconds    * 20;
        int halfWaitTicks = waitTicks / 2;
        int elapsed       = -nextspinTime;

        if (!spinInitialized || lastElapsed < 0) {
            dotAnimFrame  = 0;
            dotAnimTick   = 0;
            showingWinner = false;
            stoppedTick   = 0;
            onSpinStart(rollingTicks, halfWaitTicks);
        }
        lastElapsed = elapsed;

        if (elapsed >= spinTotalTicks) {
            // Wheel has stopped — count ticks and show winner at halfWaitTicks
            stoppedTick++;
            wheelDeg = RouletteBlockEntity.getWheelDegrees(winnernum);
            ballDeg  = 0f;
            if (stoppedTick >= halfWaitTicks) {
                showingWinner = true;
            }
            spinString = showingWinner ? "Winner: " + winnernum + " (" + RouletteBlockEntity.NUMBER_COLORS[winnernum].toUpperCase() + ")" : "Spinning" + DOT_FRAMES[dotAnimFrame];
            return;
        }

        // Animate dots while spinning
        dotAnimTick++;
        if (dotAnimTick >= DOT_FRAME_DURATION) {
            dotAnimTick  = 0;
            dotAnimFrame = (dotAnimFrame + 1) % DOT_FRAMES.length;
        }
        spinString = "Spinning" + DOT_FRAMES[dotAnimFrame];

        float wheelFrac = curvePosition(elapsed, rollingTicks, spinTotalTicks,
                WHEEL_FULL_SPEED * rollingTicks, wheelTotalDist);
        float ballFrac  = curvePosition(elapsed, rollingTicks, spinTotalTicks,
                BALL_FULL_SPEED  * rollingTicks, ballTotalDist);

        wheelDeg = (wheelStartDeg + wheelFrac * wheelTotalDist) % 360f;
        ballDeg  = (ballStartDeg  + ballFrac  * ballTotalDist)  % 360f;
    }

    private static float positiveMod(float value, float mod) {
        return ((value % mod) + mod) % mod;
    }

    // ----------------------------
    // BACKGROUND
    // ----------------------------

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop  = (height - backgroundHeight) / 2;
        context.getMatrices().push();
        context.getMatrices().translate(guiLeft, guiTop, 0);
        context.getMatrices().scale(2.0f, 2.0f, 1.0f);
        context.drawTexture(BG_TEXTURE, 0, 0, 0, 0, backgroundWidth, backgroundHeight);
        context.getMatrices().pop();
    }

    protected void drawText(DrawContext context) {
        int guiLeft = (width - backgroundWidth) / 2;
        int guiTop  = (height - backgroundHeight) / 2;
        context.drawText(textRenderer, Text.literal(spinString),
                guiLeft + 190, guiTop + 115, 0x373737, false);
    }

    protected void drawWheel(DrawContext context) {
        int cx = ((width  - backgroundWidth)  / 2) + 255;
        int cy = ((height - backgroundHeight) / 2) + 155;

        Identifier wheelTex = new Identifier(Szar.MOD_ID, "textures/gui/roulette_wheel.png");
        Identifier ballTex  = new Identifier(Szar.MOD_ID, "textures/gui/roulette_ball.png");
        int imgWidth  = 64;
        int imgHeight = 64;

        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(wheelDeg));
        context.drawTexture(wheelTex, -imgWidth / 2, -imgHeight / 2, 0, 0,
                imgWidth, imgHeight, imgWidth, imgHeight);
        context.getMatrices().pop();

        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ballDeg));
        context.drawTexture(ballTex, -imgWidth / 2, -imgHeight / 2, 0, 0,
                imgWidth, imgHeight, imgWidth, imgHeight);
        context.getMatrices().pop();
    }

    // ----------------------------
    // RENDER
    // ----------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawWheel(context);
        drawText(context);
        drawMouseoverTooltip(context, mouseX, mouseY);
        tickScreen();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        super.removed();
    }
}