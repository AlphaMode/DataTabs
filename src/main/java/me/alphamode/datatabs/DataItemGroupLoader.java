package me.alphamode.datatabs;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.io.Reader;

public enum DataItemGroupLoader implements ResourceManagerReloadListener, IdentifiableResourceReloadListener {
    INSTANCE;

    public final Int2ObjectMap<DataItemGroupOutput> results = new Int2ObjectLinkedOpenHashMap<>();

    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation("data-tabs", "item_group_loader");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        FileToIdConverter fileToIdConverter = FileToIdConverter.json("item_groups");
        results.clear();
        fileToIdConverter.listMatchingResourceStacks(resourceManager).forEach((resourceLocation, resources) -> {
            for (Resource resource : resources) {
                try {
                    Reader reader = resource.openAsReader();
                    JsonElement jsonElement = JsonParser.parseReader(reader);

                    DataResult<DataItemGroupOutput> result = DataItemGroupOutput.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, jsonElement));
                    DataItemGroupOutput groupOutput = result.getOrThrow(false, System.out::println);
                    if (results.containsKey(DataItemGroupOutput.getIndexFromString(groupOutput.getId())))
                        results.get(DataItemGroupOutput.getIndexFromString(groupOutput.getId())).getTabContents().addAll(groupOutput.getEntries());
                    else
                        results.put(DataItemGroupOutput.getIndexFromString(groupOutput.getId()), groupOutput);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
