package acore.aurora.utility.discord.callbacks;

import acore.aurora.utility.discord.DiscordUser;
import com.sun.jna.Callback;

public interface ReadyCallback extends Callback {
    void apply(final DiscordUser p0);
}
