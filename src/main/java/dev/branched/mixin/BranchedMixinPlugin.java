package dev.branched.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Conditional Mixin plugin that enables optional tree-physics compatibility mixins
 * only when the corresponding third-party mod classes exist on the runtime classpath.
 */
public final class BranchedMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (targetClassName == null || targetClassName.isEmpty()) {
            return false;
        }
        if (targetClassName.startsWith("net.minecraft.")) {
            return true;
        }
        String resource = targetClassName.replace('.', '/') + ".class";
        ClassLoader primary = BranchedMixinPlugin.class.getClassLoader();
        if (primary != null && primary.getResource(resource) != null) {
            return true;
        }
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return context != null && context.getResource(resource) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
