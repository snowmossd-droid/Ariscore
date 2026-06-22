package acore.aurora.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.AnimalEntity;
import acore.aurora.events.impl.EventTick;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;
import acore.aurora.utility.Timer;

public class Optimizer extends Module {

    private final Setting<Boolean> freeMemory     = new Setting<>("FreeMemory",      true);
    private final Setting<Integer> gcInterval     = new Setting<>("GCInterval",       3, 1, 10, v -> freeMemory.getValue());

    private final Setting<Boolean> lowGraphics    = new Setting<>("LowGraphics",     true);
    private final Setting<Boolean> noParticles    = new Setting<>("NoParticles",     true);
    private final Setting<Boolean> noWeather      = new Setting<>("NoWeather",       true);
    private final Setting<Boolean> noBobbing      = new Setting<>("NoBobbing",       true);
    private final Setting<Boolean> noEntityModels = new Setting<>("NoEntityModels",  false);

    private final Setting<Boolean> maxFPS         = new Setting<>("MaxFPS",          true);
    private final Setting<Integer> fpsLimit       = new Setting<>("FPSLimit",        60, 30, 260, v -> maxFPS.getValue());

    private final Setting<Boolean> reduceDist     = new Setting<>("ReduceDistance",  true);
    private final Setting<Integer> renderDist     = new Setting<>("RenderDist",      4, 2, 16, v -> reduceDist.getValue());
    private final Setting<Integer> entityDist     = new Setting<>("EntityDist",      32, 8, 64, v -> reduceDist.getValue());

    private final Setting<Boolean> entityCulling  = new Setting<>("EntityCulling",   true);
    private final Setting<Boolean> hideAnimals    = new Setting<>("HideAnimals",     false);
    private final Setting<Boolean> hideArmorStand = new Setting<>("HideArmorStand",  false);
    private final Setting<Boolean> hideAmbient    = new Setting<>("HideAmbient",     true);

    private final Setting<Boolean> aggressiveGC   = new Setting<>("AggressiveGC",   false);

    private final Timer gcTimer     = new Timer();
    private final Timer aggrGCTimer = new Timer();

    private int savedRenderDist  = -1;
    private boolean savedBobbing = true;

    public Optimizer() {
        super("Optimizer", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (fullNullCheck()) return;
        savedRenderDist = mc.options.getViewDistance().getValue();
        savedBobbing    = mc.options.getBobView().getValue();
    }

    @Override
    public void onDisable() {
        mc.options.getParticles().setValue(ParticlesMode.ALL);
        mc.options.getBobView().setValue(savedBobbing);
        if (savedRenderDist != -1)
            mc.options.getViewDistance().setValue(savedRenderDist);
        if (mc.world != null) {
            mc.options.getGraphicsMode().setValue(GraphicsMode.FANCY);
            mc.options.getCloudRenderMode().setValue(CloudRenderMode.FANCY);
            mc.options.getEntityShadows().setValue(true);
            mc.options.getEnableVsync().setValue(true);
        }
        if (mc.world != null) {
            for (Entity en : mc.world.getEntities()) en.setInvisible(false);
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (fullNullCheck()) return;

        if (freeMemory.getValue() && gcTimer.every((long) gcInterval.getValue() * 60000L)) {
            System.gc();
            Runtime.getRuntime().freeMemory();
        }

        if (aggressiveGC.getValue() && aggrGCTimer.every(5000)) {
            System.gc();
        }

        if (lowGraphics.getValue()) {
            mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            mc.options.getGraphicsMode().setValue(GraphicsMode.FAST);
            mc.options.getEntityShadows().setValue(false);
            mc.options.getBiomeBlendRadius().setValue(0);
        }

        if (noParticles.getValue()) {
            mc.options.getParticles().setValue(ParticlesMode.MINIMAL);
        }

        if (noWeather.getValue() && mc.world != null) {
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }

        if (noBobbing.getValue()) {
            mc.options.getBobView().setValue(false);
        }

        if (maxFPS.getValue()) {
            mc.options.getEnableVsync().setValue(false);
            mc.options.getMaxFps().setValue(fpsLimit.getValue());
        }

        if (reduceDist.getValue()) {
            mc.options.getViewDistance().setValue(renderDist.getValue());
        }

        if (entityCulling.getValue() && mc.world != null && mc.player != null) {
            int distSq = entityDist.getValue() * entityDist.getValue();
            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player) continue;
                double d = mc.player.squaredDistanceTo(entity);

                boolean hide = false;
                if (d > distSq && !entity.isPlayer()) hide = true;
                if (hideAnimals.getValue() && entity instanceof AnimalEntity) hide = true;
                if (hideArmorStand.getValue() && entity instanceof ArmorStandEntity) hide = true;
                if (hideAmbient.getValue() && entity instanceof BatEntity) hide = true;

                entity.setInvisible(hide);
            }
        }

        if (noEntityModels.getValue() && mc.world != null && mc.player != null) {
            int distSq = 16 * 16;
            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player || entity.isPlayer()) continue;
                if (mc.player.squaredDistanceTo(entity) > distSq) {
                    entity.setInvisible(true);
                }
            }
        }
    }
    }
            
