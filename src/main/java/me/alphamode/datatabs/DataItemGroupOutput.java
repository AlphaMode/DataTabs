package me.alphamode.datatabs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DataItemGroupOutput extends CreativeModeTab.ItemDisplayBuilder {

    public static final Codec<DataItemGroupOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.STRING.fieldOf("id").forGetter(DataItemGroupOutput::getId), ItemStack.CODEC.listOf().fieldOf("entries").forGetter(DataItemGroupOutput::getEntries), Codec.STRING.listOf().fieldOf("tags").forGetter(DataItemGroupOutput::getTags), Codec.BOOL.fieldOf("replace").forGetter(tagItemGroupOutput -> tagItemGroupOutput.replace))
            .apply(instance, DataItemGroupOutput::new));

    private final String id;
    private final boolean replace;
    private final List<ItemStack> entries;
    private final List<String> tags;

    private FeatureFlagSet flagSet;

    public static int getIndexFromString(String id) {
        for (CreativeModeTab tab : CreativeModeTabs.TABS) {
            if (tab.getDisplayName().getContents() instanceof TranslatableContents translatableContents && translatableContents.getKey().equals(id))
                return tab.getId();
            if (tab.getDisplayName().getString().equals(id))
                return tab.getId();
        }

        throw new RuntimeException("No tab found with the id: " + id + "!");
    }

    public DataItemGroupOutput(String id, List<ItemStack> entries, List<String> tags, boolean replace) {
        super(CreativeModeTabs.TABS[getIndexFromString(id)], null);
        this.id = id;
        this.replace = replace;
        this.entries = new ArrayList<>(entries);
        this.tags = new ArrayList<>(tags);
        entries.forEach(getTabContents()::add);
    }

    public DataItemGroupOutput(CreativeModeTab.ItemDisplayBuilder builder) {
        super(builder.tab, builder.featureFlagSet);
        this.id = builder.tab.getDisplayName().getContents() instanceof TranslatableContents translatableContents ? translatableContents.getKey() : builder.tab.getDisplayName().getString();
        this.replace = false;
        this.entries = new LinkedList<>();
        this.tags = List.of();
    }

    public String getId() {
        return id;
    }

    public List<ItemStack> getEntries() {
        return entries;
    }

    public void setFlagSet(FeatureFlagSet flagSet) {
        this.flagSet = flagSet;
    }

    public List<String> getTags() {
        return tags;
    }

    @Override
    public void accept(ItemStack itemStack, CreativeModeTab.TabVisibility tabVisibility) {
         if (replace)
             return;
        boolean bl = this.entries.contains(itemStack) && tabVisibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY;
        if (bl) {
            String var10002 = itemStack.getDisplayName().getString();
            throw new IllegalStateException("Accidentally adding the same item stack twice " + var10002 + " to a Creative Mode Tab: " + this.tab.getDisplayName().getString());
        } else {
            if (itemStack.getItem().isEnabled(this.flagSet)) {
                switch (tabVisibility) {
                    case PARENT_AND_SEARCH_TABS:
                        this.entries.add(itemStack);
                        this.getSearchTabContents().add(itemStack);
                        break;
                    case PARENT_TAB_ONLY:
                        this.entries.add(itemStack);
                        break;
                    case SEARCH_TAB_ONLY:
                        this.getSearchTabContents().add(itemStack);
                }
            }

        }
    }

    @Override
    public ItemStackLinkedSet getTabContents() {
        ItemStackLinkedSet content = new ItemStackLinkedSet();
        content.addAll(getEntries());
        return content;
    }
}
