package com.easterlyn.kitchensink.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Syntax;
import com.easterlyn.EasterlynCore;
import com.easterlyn.command.CoreContexts;
import com.easterlyn.user.User;
import com.easterlyn.util.Request;

public class ManageRequestCommand extends BaseCommand {

	@Dependency
	EasterlynCore core;

	@CommandAlias("accept|yes|y|tpyes|tpaccept")
	@Description("{@@sink.module.request.accept.description}")
	@CommandCompletion("")
	@Syntax("")
	@CommandPermission("easterlyn.command.request")
	public void accept(@Flags(CoreContexts.SELF) User user) {
		Request request = user.pollPendingRequest();
		if (request == null) {
			core.getLocaleManager().sendMessage(user.getPlayer(), "sink.module.request.error.no_pending");
			return;
		}
		request.accept();
	}

	@CommandAlias("decline|no|n|tpno|tpdeny")
	@Description("{@@sink.module.request.decline.description}")
	@CommandCompletion("")
	@Syntax("")
	@CommandPermission("easterlyn.command.request")
	public void decline(@Flags(CoreContexts.SELF) User user) {
		Request request = user.pollPendingRequest();
		if (request == null) {
			core.getLocaleManager().sendMessage(user.getPlayer(), "sink.module.request.error.no_pending");
			return;
		}
		request.decline();
	}

}
