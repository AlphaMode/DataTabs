package me.alphamode.datatabs;

import java.util.List;

public interface CreativeModeTabExtension {
    default boolean hasTagsSynced() {
        throw new RuntimeException();
    }

    default void setTagsSynced(boolean tagsSynced) {
        throw new RuntimeException();
    }

    default List<String> getTags() {
        throw new RuntimeException();
    }
}
