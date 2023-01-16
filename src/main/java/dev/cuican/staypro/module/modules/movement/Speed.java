package dev.cuican.staypro.module.modules.movement;



import dev.cuican.staypro.Stay;
import dev.cuican.staypro.common.annotations.ModuleInfo;
import dev.cuican.staypro.event.events.client.EventPreMotion;
import dev.cuican.staypro.event.events.network.EventMove;
import dev.cuican.staypro.event.events.network.PacketEvent;
import dev.cuican.staypro.module.Category;
import dev.cuican.staypro.module.Module;
import dev.cuican.staypro.setting.Setting;
import dev.cuican.staypro.utils.MovementUtils;
import dev.cuican.staypro.utils.PlayerUtil;
import dev.cuican.staypro.utils.Timer;
import net.minecraft.init.MobEffects;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import static dev.cuican.staypro.utils.PyroSpeed.*;


@ModuleInfo(name = "Speed", description = "Speed", category = Category.MOVEMENT)

public class Speed extends Module {

    public final Setting<String> Mode = setting("Mode", "Default",listOf( "Default","Grief", "StrafeStrict","ReallyWorld"));

    public final Setting<Integer> bticks = setting("boostTicks", 10, 1, 40);


    public final Setting<Boolean> strafeBoost = setting("StrafeBoost", false);
    public final Setting<Float> reduction = setting("reduction", 2f, 1f, 10f);
    public final Setting<Boolean> usver = setting("use", false);
    public final Setting<Boolean> autoWalk = setting("AutoWalk", false);
    public final Setting<Boolean> uav = setting("UseAllVelocity", false);




    public double defaultBaseSpeed = getBaseMoveSpeed();
    public double distance;
    public int Field2015 = 4;
    public int FunnyGameStage;
    public boolean flip;
    int velocity = 0;
    int boostticks = 0;
    boolean isBoosting = false;

    private double prevMotion = 0.0D;
    private double strictBaseSpeed = 0.2873D;
    private int strictCounter;
    private int strictStage = 4;
    private int ticksPassed = 0;
    private double maxVelocity = 0;
    private Timer velocityTimer = new Timer();

    @Override
    public void onDisable() {
        defaultBaseSpeed = getBaseMoveSpeed();
        this.Field2015 = 4;
        distance = 0.0;
        FunnyGameStage = 0;

        mc.player.speedInAir = 0.02f;
        Stay.TICK_TIMER = 1f;
        velocity = 0;
    }


    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        maxVelocity = 0;
    }


    @SubscribeEvent
    public void onUpdateWalkingPlayerPre(EventPreMotion event) {
        if (Mode.getValue().equals("StrafeStrict")) {
            double dX = mc.player.posX - mc.player.prevPosX;
            double dZ = mc.player.posZ - mc.player.prevPosZ;
            prevMotion = Math.sqrt(dX * dX + dZ * dZ);
        }

        if(strafeBoost.getValue() && isBoosting){
            return;
        }
        if(Mode.getValue().equals("Grief")){
            return;
        }
        double d2 = mc.player.posX - mc.player.prevPosX;
        double d3 = mc.player.posZ - mc.player.prevPosZ;
        double d4 = d2 * d2 + d3 * d3;
        distance = Math.sqrt(d4);
    }


    @SubscribeEvent( priority = EventPriority.HIGHEST)
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof SPacketEntityVelocity) {
            if(((SPacketEntityVelocity) event.getPacket()).getEntityID() == mc.player.getEntityId()) {
                SPacketEntityVelocity pack = (SPacketEntityVelocity) event.getPacket();
                int vX = pack.getMotionX();
                int vZ = pack.getMotionZ();
                if (vX < 0) vX *= -1;
                if (vZ < 0) vZ *= -1;

                if((vX + vZ) < 3000 && !uav.getValue()) return;
                velocity = vX + vZ;

                boostticks = bticks.getValue();
            }
        }
        if (event.getPacket() instanceof SPacketPlayerPosLook) {
            Stay.TICK_TIMER = 1f;
            strictBaseSpeed = 0.2873;
            strictStage = 4;
            prevMotion = 0;
            strictCounter = 0;
            maxVelocity = 0;
        } else if (event.getPacket() instanceof SPacketExplosion) {
            SPacketExplosion velocity = (SPacketExplosion) event.getPacket();
            maxVelocity = Math.sqrt(velocity.getMotionX() * velocity.getMotionX() + velocity.getMotionZ() * velocity.getMotionZ());
            velocityTimer.reset();
        }
    }

    @Override
    public void onUpdate(){
        if(autoWalk.getValue()){
            mc.gameSettings.keyBindForward.pressed = true;
        }
        if(Mode.getValue().equals("Grief")){
            if (!MovementUtils.isMoving()) {
                return;
            }
            if (mc.player.onGround) {
                mc.player.jump();
                mc.player.speedInAir = 0.0201F;
                Stay.TICK_TIMER = 0.94F;
            }
            if (mc.player.fallDistance > 0.7 && mc.player.fallDistance < 1.3) {
                mc.player.speedInAir = 0.02F;
                Stay.TICK_TIMER = 1.8F;
            }
        }
        if(Mode.getValue().equals("ReallyWorld")){
            if (!MovementUtils.isMoving()) return;

            if (mc.player.onGround) {mc.player.jump();}
            if (mc.player.fallDistance <= 0.22) {
                Stay.TICK_TIMER = 3.5f;
                mc.player.jumpMovementFactor = 0.026523f;
            } else if ((double) mc.player.fallDistance < 1.25) {
                Stay.TICK_TIMER = 0.47f;
            }
        }
    }


    public double getBaseMoveSpeed() {
        if(mc.player == null || mc.world == null){
            return 0.2873;
        }

        int n;
        double d = 0.2873;
        if (mc.player.isPotionActive(MobEffects.SPEED)) {
            n = Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.SPEED)).getAmplifier();
            d *= 1.0 + 0.2 * (double)(n + 1);
        }
        if (mc.player.isPotionActive(MobEffects.JUMP_BOOST) && usver.getValue()) {
            n = Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST)).getAmplifier();
            d /= 1.0 + 0.2 * (double)(n + 1);
        }
        if(strafeBoost.getValue() && velocity > 0 && boostticks > 0){
            d += (velocity / 8000f) / reduction.getValue();
            boostticks--;
        }
        if(boostticks == 1){
            velocity = 0;
        }
        return d;
    }



    public double isJumpBoost(){
        if (mc.player.isPotionActive(MobEffects.JUMP_BOOST)) {
            return 0.2;
        } else {
            return 0;
        }
    }




    @SubscribeEvent
    public void onMove(EventMove event) {
        if (mc.player == null || mc.world == null) return;
        switch (Mode.getValue()) {
            case "StrafeStrict": {
                strictCounter++;
                strictCounter %= 5;

                if (strictCounter != 0) {
                    Stay.TICK_TIMER = 1f;
                } else if (PlayerUtil.isPlayerMoving()) {
                    Stay.TICK_TIMER = 1.3f;
                    mc.player.motionX *= 1.0199999809265137D;
                    mc.player.motionZ *= 1.0199999809265137D;
                }

                if (mc.player.onGround && PlayerUtil.isPlayerMoving()) {
                    strictStage = 2;
                }

                if (round(mc.player.posY - (int)mc.player.posY) == round(0.138D)) {
                    mc.player.motionY -= 0.08D;
                    event.setY(event.get_y() - 0.09316090325960147D);
                    mc.player.posY -= 0.09316090325960147D;
                }

                if (strictStage == 1 && (mc.player.moveForward != 0.0F || mc.player.moveStrafing != 0.0F)) {
                    strictStage = 2;
                    strictBaseSpeed = 1.38D * getBaseMotionSpeed() - 0.01D;
                } else if (strictStage == 2) {
                    strictStage = 3;
                    mc.player.motionY = 0.399399995803833D;
                    event.setY(0.399399995803833D);
                    strictBaseSpeed *= 2.149D;
                } else if (strictStage == 3) {
                    strictStage = 4;
                    double adjustedMotion = 0.66D * (prevMotion - getBaseMotionSpeed());
                    strictBaseSpeed = prevMotion - adjustedMotion;
                } else {
                    if (mc.world.getCollisionBoxes(mc.player, mc.player
                            .getEntityBoundingBox().offset(0.0D, mc.player.motionY, 0.0D)).size() > 0 || mc.player.collidedVertically)
                        strictStage = 1;
                    strictBaseSpeed = prevMotion - prevMotion / 159.0D;
                }

                strictBaseSpeed = Math.max(strictBaseSpeed, getBaseMotionSpeed());

                if (maxVelocity > 0 && strafeBoost.getValue() && !velocityTimer.passedMs(75) && !mc.player.collidedHorizontally) {
                    strictBaseSpeed = Math.max(strictBaseSpeed, maxVelocity);
                } else {
                    strictBaseSpeed = Math.min(strictBaseSpeed, (ticksPassed > 25) ? 0.449D : 0.433D);
                }

                float forward = mc.player.movementInput.moveForward;
                float strafe = mc.player.movementInput.moveStrafe;
                float yaw = mc.player.rotationYaw;

                ticksPassed++;

                if (ticksPassed > 50)
                    ticksPassed = 0;
                if (forward == 0.0F && strafe == 0.0F) {
                    event.setX(0.0D);
                    event.setZ(0.0D);
                } else if (forward != 0.0F) {
                    if (strafe >= 1.0F) {
                        yaw += ((forward > 0.0F) ? -45 : 45);
                        strafe = 0.0F;
                    } else if (strafe <= -1.0F) {
                        yaw += ((forward > 0.0F) ? 45 : -45);
                        strafe = 0.0F;
                    }
                    if (forward > 0.0F) {
                        forward = 1.0F;
                    } else if (forward < 0.0F) {
                        forward = -1.0F;
                    }
                }

                double cos = Math.cos(Math.toRadians((yaw + 90.0F)));
                double sin = Math.sin(Math.toRadians((yaw + 90.0F)));

                event.setX(forward * strictBaseSpeed * cos + strafe * strictBaseSpeed * sin);
                event.setZ(forward * strictBaseSpeed * sin - strafe * strictBaseSpeed * cos);

                if (forward == 0.0F && strafe == 0.0F) {
                    event.setX(0.0D);
                    event.setZ(0.0D);
                }

                break;
            }
            case "Default": {
                double d;
                if (event.getStage() != 0) return;
                if (event.isCanceled()) {
                    return;
                }
                if (!isMovingClient() || mc.player.fallDistance > 5.0f) {
                    return;
                }
                if (mc.player.collidedHorizontally) {
                    if (mc.player.onGround && (d = Method5402(1.0)) == 1.0) {
                        FunnyGameStage++;
                    }
                    if (FunnyGameStage > 0) {
                        switch (FunnyGameStage) {
                            case 1: {
                                event.setCanceled(true);

                                event.set_y(0.41999998688698);
                                int n2 = FunnyGameStage;
                                FunnyGameStage = n2 + 1;
                                return;
                            }
                            case 2: {
                                event.setCanceled(true);

                                event.set_y(0.33319999363422);
                                int n3 = FunnyGameStage;
                                FunnyGameStage = n3 + 1;
                                return;
                            }
                            case 3: {
                                float f = (float)Method718();
                                event.set_y(0.24813599859094704);
                                event.set_x((double) (-MathHelper.sin(f)) * 0.2);
                                event.set_z((double) MathHelper.cos(f) * 0.2);
                                FunnyGameStage = 0;
                                mc.player.motionY = 0.0;
                                event.setCanceled(true);
                                return;
                            }
                        }
                        return;
                    }
                }
                FunnyGameStage = 0;
                if (this.Field2015 == 1 && (mc.player.moveForward != 0.0f || mc.player.moveStrafing != 0.0f)) {
                    defaultBaseSpeed = 1.35 * getBaseMoveSpeed() - 0.01;
                } else if (this.Field2015 == 2 && mc.player.collidedVertically) {
                    d = 0.4;
                    double d2 = d;
                    mc.player.motionY = d2 + isJumpBoost();
                    double d3 = d;
                    event.set_y(d3 + isJumpBoost());
                    flip = !flip;
                    defaultBaseSpeed *= flip ? 1.6835 : 1.395;
                } else if (this.Field2015 == 3) {
                    d = 0.66 * (distance - getBaseMoveSpeed());
                    defaultBaseSpeed = distance - d;
                } else {
                    List<AxisAlignedBB> list = mc.world.getCollisionBoxes(mc.player, mc.player.getEntityBoundingBox().offset(0.0, mc.player.motionY, 0.0));
                    if ((list.size() > 0 || mc.player.collidedVertically) && this.Field2015 > 0) {
                        this.Field2015 = 1;
                    }
                    defaultBaseSpeed = distance - distance / 159.0;
                }
                event.setCanceled(true);
                defaultBaseSpeed = Math.max(defaultBaseSpeed, getBaseMoveSpeed());
                Method744(event, defaultBaseSpeed);
                ++this.Field2015;
                break;
            }
        }
        if(Mode.getValue()!="Default") {
            event.setCanceled(true);
        }
    }






    private double getBaseMotionSpeed() {
        double baseSpeed =  0.2873D;
        if (mc.player.isPotionActive(MobEffects.SPEED)) {
            int amplifier = Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.SPEED)).getAmplifier();
            baseSpeed *= 1.0D + 0.2D * ((double) amplifier + 1);
        }
        return baseSpeed;
    }

    private double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(3, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
