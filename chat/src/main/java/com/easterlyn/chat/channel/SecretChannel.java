package com.easterlyn.chat.channel;

import com.easterlyn.user.User;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class SecretChannel extends Channel {

	public SecretChannel(@NotNull String name, @NotNull UUID owner) {
		super(name, owner);
	}

	@Override
	public boolean isWhitelisted(@NotNull User user) {
		return isModerator(user);
	}

}
