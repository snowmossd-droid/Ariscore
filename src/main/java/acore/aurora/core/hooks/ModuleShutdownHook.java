package acore.aurora.core.hooks;

import acore.aurora.core.manager.client.ModuleManager;

public class ModuleShutdownHook extends Thread {
    @Override
    public void run() {
        if (ModuleManager.unHook.isEnabled())
            ModuleManager.unHook.disable();
    }
}
