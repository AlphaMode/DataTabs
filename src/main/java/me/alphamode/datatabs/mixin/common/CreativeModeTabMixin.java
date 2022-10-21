package me.alphamode.datatabs.mixin.common;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.alphamode.datatabs.CreativeModeTabExtension;
import me.alphamode.datatabs.DataItemGroupLoader;
import me.alphamode.datatabs.DataItemGroupOutput;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin implements CreativeModeTabExtension {
    @Shadow private @Nullable ItemStackLinkedSet displayItems;

    @Shadow private @Nullable ItemStackLinkedSet displayItemsSearchTab;

    @Shadow protected abstract void generateDisplayItems(FeatureFlagSet featureFlagSet, CreativeModeTab.Output output);

    @Shadow public abstract int getId();

    @ModifyVariable(method = "lazyBuildDisplayItems", at = @At("STORE"))
    private CreativeModeTab.ItemDisplayBuilder datatabs$changeBuilder(CreativeModeTab.ItemDisplayBuilder value) {
        return DataItemGroupLoader.INSTANCE.results.getOrDefault(getId(), new DataItemGroupOutput(value));
    }

    @Inject(method = "lazyBuildDisplayItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;generateDisplayItems(Lnet/minecraft/world/flag/FeatureFlagSet;Lnet/minecraft/world/item/CreativeModeTab$Output;)V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void datatabs$setFeatureFlagSet(FeatureFlagSet featureFlagSet, boolean bl, CallbackInfoReturnable<ItemStackLinkedSet> cir, CreativeModeTab.ItemDisplayBuilder itemDisplayBuilder) {
        if (itemDisplayBuilder instanceof DataItemGroupOutput output) {
            output.setFlagSet(featureFlagSet);
            this.tags = output.getTags();
        }
    }

    @Unique private List<String> tags;
    @Unique private boolean tagsSynced = false;

    @Inject(method = "lazyBuildDisplayItems", at = @At("TAIL"))
    private void datatabs$loadTags(FeatureFlagSet featureFlagSet, boolean bl, CallbackInfoReturnable<ItemStackLinkedSet> cir) {
        if (!getTags().isEmpty() &&!hasTagsSynced() && displayItems != null) {
            getTags().forEach(itemTagKey -> {
                Registry.ITEM.getOrCreateTag(TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(itemTagKey))).forEach(itemHolder -> {
                    if (!itemHolder.tags().toList().isEmpty()) { // There is probably a cleaner way to do this
                        displayItems.add(itemHolder.value().getDefaultInstance());
                        setTagsSynced(true);
                    }
                });
            });
        }
    }

    @Inject(method = "invalidateDisplayListCache", at = @At("TAIL"))
    private void datatabs$resetTags(CallbackInfo ci) {
        setTagsSynced(false);
    }

    @Inject(method = "lazyBuildDisplayItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab$ItemDisplayBuilder;getSearchTabContents()Lnet/minecraft/world/item/ItemStackLinkedSet;"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void datatabs$dumpVanillaTabs(FeatureFlagSet featureFlagSet, boolean bl, CallbackInfoReturnable<ItemStackLinkedSet> cir, CreativeModeTab.ItemDisplayBuilder itemDisplayBuilder) throws IOException {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment())
            return;
        System.out.println("Dumping Item Group: " + getId() + " to Json.");

        DataResult<JsonElement> result = DataItemGroupOutput.CODEC.encodeStart(JsonOps.INSTANCE, (DataItemGroupOutput) itemDisplayBuilder);
        JsonElement element = result.getOrThrow(false, s -> {
           System.out.println("Failed!");
        });

        DataProvider.saveStable(CachedOutput.NO_CACHE, element, Path.of("/home/alpha/github/TagItemGroups/src/main/resources/dumped_vanilla").resolve(getId() + ".json"));
    }

    @Unique
    @Override
    public boolean hasTagsSynced() {
        return tagsSynced;
    }

    @Unique
    @Override
    public void setTagsSynced(boolean tagsSynced) {
        this.tagsSynced = tagsSynced;
    }

    @Unique
    @Override
    public List<String> getTags() {
        return tags;
    }
}
