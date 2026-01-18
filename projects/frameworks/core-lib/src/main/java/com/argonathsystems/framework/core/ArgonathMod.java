package com.argonathsystems.framework.core;

import com.argonathsystems.framework.core.result.Result;

/**
 * Interface for platform-agnostic mods.
 * These are loaded by the implementations Adapter (e.g. HytaleAdapter).
 */
public interface ArgonathMod {
    
    /**
     * Called when the mod is enabled.
     * @return Result indicating success or failure.
     */
    Result<Void> onEnable();

    /**
     * Called when the mod is disabled.
     * @return Result indicating success or failure.
     */
    Result<Void> onDisable();
}
