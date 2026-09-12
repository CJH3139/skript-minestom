package ch.njol.skript.util;

import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.TooltipDisplay;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Hiding of an item's tooltip through {@link DataComponents#TOOLTIP_DISPLAY}.
 * <p>
 * {@link ItemStack#withoutExtraTooltip()} replaces the whole component, which would drop any other
 * hiding already applied to the item, so the tooltip display is edited in place here instead.
 */
public final class ItemTooltip {

	/**
	 * The components that append the additional part of an item's tooltip, meaning everything
	 * besides its name and lore. Mirrors the set Minestom hides in
	 * {@link ItemStack#withoutExtraTooltip()}.
	 */
	private static final Set<DataComponent<?>> ADDITIONAL_COMPONENTS = Set.of(
		DataComponents.BANNER_PATTERNS, DataComponents.BEES, DataComponents.BLOCK_ENTITY_DATA,
		DataComponents.BLOCK_STATE, DataComponents.BUNDLE_CONTENTS, DataComponents.CHARGED_PROJECTILES,
		DataComponents.CONTAINER, DataComponents.CONTAINER_LOOT, DataComponents.FIREWORK_EXPLOSION,
		DataComponents.FIREWORKS, DataComponents.INSTRUMENT, DataComponents.MAP_ID,
		DataComponents.PAINTING_VARIANT, DataComponents.POT_DECORATIONS, DataComponents.POTION_CONTENTS,
		DataComponents.TROPICAL_FISH_PATTERN, DataComponents.WRITTEN_BOOK_CONTENT,
		DataComponents.UNBREAKABLE, DataComponents.ATTRIBUTE_MODIFIERS);

	private ItemTooltip() { }

	/**
	 * Hides or shows the entire tooltip of an item, leaving the components it hides individually
	 * untouched.
	 */
	public static void setEntireHidden(Item item, boolean hidden) {
		apply(item, display -> new TooltipDisplay(hidden, display.hiddenComponents()));
	}

	/**
	 * Hides or shows everything but the name and lore of an item, leaving the rest of its tooltip
	 * display untouched.
	 */
	public static void setAdditionalHidden(Item item, boolean hidden) {
		apply(item, display -> {
			Set<DataComponent<?>> hiddenComponents = new HashSet<>(display.hiddenComponents());
			if (hidden) {
				hiddenComponents.addAll(ADDITIONAL_COMPONENTS);
			} else {
				hiddenComponents.removeAll(ADDITIONAL_COMPONENTS);
			}
			return new TooltipDisplay(display.hideTooltip(), hiddenComponents);
		});
	}

	public static boolean isEntireHidden(Item item) {
		return getDisplay(item.getItem()).hideTooltip();
	}

	/**
	 * @return whether every component of the additional tooltip is hidden. An item that only has its
	 * entire tooltip hidden doesn't count, as showing the tooltip again would reveal them.
	 */
	public static boolean isAdditionalHidden(Item item) {
		return getDisplay(item.getItem()).hiddenComponents().containsAll(ADDITIONAL_COMPONENTS);
	}

	private static TooltipDisplay getDisplay(DataComponent.Holder holder) {
		return holder.get(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.EMPTY);
	}

	private static void apply(Item item, UnaryOperator<TooltipDisplay> modifyFunction) {
		item.modify(stack -> {
			TooltipDisplay display = modifyFunction.apply(getDisplay(stack));
			TooltipDisplay prototype = getDisplay(stack.material().prototype());
			// don't leave an explicit override behind if it's what the material does anyway,
			// otherwise an item that got its tooltip back wouldn't equal a plain one
			if (display.equals(prototype)) return stack.reset(DataComponents.TOOLTIP_DISPLAY);
			return stack.with(DataComponents.TOOLTIP_DISPLAY, display);
		});
	}

}
