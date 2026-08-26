package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.*;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.dialog.ButtonWrapper;
import ch.njol.skript.util.dialog.DialogCallbacks;
import ch.njol.skript.events.DialogClickEvent;
import ch.njol.util.Kleenean;
import net.kyori.adventure.key.Key;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

@Name("Create Dialog Button")
@Description("""
	Creates a dialog button whose code runs when a player clicks it.
	The button sends the dialog's input values back with the click, so 'dialog input' works
	inside the section. The player who clicked is the event-player.
	The section runs on the server, so it can do anything a normal trigger can.
	Warning: the button's callback key is live from parse time onward, and any player can send
	a click for it at any time with arbitrary input values, not just the player it was shown to.
	Always validate anything read from 'dialog input' before trusting it.""")
@Examples("""
	create dialog button labeled "<green>Buy" stored in {_buy}:
		send "You bought %dialog input ""qty""% items!" to player""")
@Keywords({"dialog", "button"})
public class EffSecDialogButton extends EffectSection {

	static {
		Skript.registerSection(EffSecDialogButton.class,
			"create dialog button labeled %component% [with tooltip %-component%] [with width %-number%] "
				+ "[(and store it|stored) in %-objects%]");
	}

	private Expression<ComponentWrapper> label;
	private @Nullable Expression<ComponentWrapper> tooltip;
	private @Nullable Expression<Number> width;
	private @Nullable Expression<Object> storage;
	private Trigger trigger;
	private Key callbackKey;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult,
						@Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
		label = (Expression<ComponentWrapper>) expressions[0];
		tooltip = (Expression<ComponentWrapper>) expressions[1];
		width = (Expression<Number>) expressions[2];
		storage = (Expression<Object>) expressions[3];
		if (sectionNode == null || sectionNode.isEmpty()) {
			Skript.error("A dialog button section needs code inside it.");
			return false;
		}
		callbackKey = DialogCallbacks.nextKey(getParser().getCurrentScript(), sectionNode);
		trigger = loadCode(sectionNode, "dialog button", DialogClickEvent.class);
		DialogCallbacks.register(callbackKey, trigger, getParser().getCurrentScript());
		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		ComponentWrapper labelValue = label.getSingle(event);
		if (labelValue == null) return super.walk(event, false);
		ButtonWrapper button = new ButtonWrapper(labelValue.getComponent());
		if (tooltip != null) {
			ComponentWrapper single = tooltip.getSingle(event);
			if (single != null) button.setTooltip(single.getComponent());
		}
		if (width != null) {
			Number single = width.getSingle(event);
			if (single != null) button.setWidth(single.intValue());
		}
		button.setTrigger(trigger, callbackKey);
		if (storage != null) storage.change(event, new ButtonWrapper[]{button}, Changer.ChangeMode.SET);
		return super.walk(event, false);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "create dialog button labeled " + label.toString(event, debug);
	}

}
