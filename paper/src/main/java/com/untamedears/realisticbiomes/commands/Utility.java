package com.untamedears.realisticbiomes.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.untamedears.realisticbiomes.RealisticBiomes;

public class Utility extends BaseCommand {
	@CommandAlias("rb-reload")
	@Syntax("[filename]")
	@CommandPermission("rb.op")
	@Description("Reloads configuration")
	public void onCommand(@Optional String filename) {
		if (filename != null && filename.length() > 0)
			RealisticBiomes.getInstance().reload(filename);
		else
			RealisticBiomes.getInstance().reload();
	}
}
