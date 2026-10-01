package com.github.hapily04.skriptminestom.command;

import com.github.hapily04.skriptminestom.SkriptMinestom;
import com.github.hapily04.skriptminestom.command.reload.ReloadCommand;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;

import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;

public class SkriptCommand extends Command {

	static final Component HELP_MESSAGE = SKRIPT_MINI_MESSAGE.deserialize("""
		<skript_minestom_tag> <base_grey>Help
		  <yellow>reload <all/folder/file/config> <base_grey>- Reload a scripts folder, script file, or the Skript config.
		  <yellow>enable <folder/file> <base_grey>- Enable and load a disabled scripts folder or script file.
		  <yellow>disable <folder/file> <base_grey>- Disable and unload an enabled scripts folder or script file.
		  <yellow>info <base_grey>- Show addon information & server information.
		  <yellow>update [confirm] <base_grey>- Check for and update to a new Skript-Minestom release.
		  <yellow>help <base_grey>- Show this help message.""");

    public SkriptCommand() {
        super("skript", "sk");
        setCondition((sender, _) ->  LuckPermsLookup.hasPermission(sender, "skript.skript"));
        setDefaultExecutor((sender, _) -> sender.sendMessage(HELP_MESSAGE));
        addSubcommand(new ReloadCommand());
		addSubcommand(new DisableCommand());
		addSubcommand(new EnableCommand());
		addSubcommand(new InfoCommand());
		if (SkriptMinestom.isIsServerJar()) addSubcommand(new UpdateCommand());
		addSubcommand(new HelpCommand());
	}

}
