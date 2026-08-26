package ch.njol.skript.util.dialog;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.log.SkriptLogger;
import net.minestom.server.dialog.DialogInput;

/**
 * {@link DialogInput}'s record constructors throw for a key outside {@code [A-Za-z0-9_]+}, which
 * would abort the whole trigger. Input expressions check here instead: at parse time for a literal
 * key, at runtime otherwise.
 */
public final class DialogInputValidation {

	private DialogInputValidation() {}

	/** An empty key passes, matching {@code validateKey}, whose loop is a no-op for it. */
	public static boolean isValidKey(String key) {
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '_') return false;
		}
		return true;
	}

	/** @return false if a literal key is invalid; the caller should fail {@code init}. */
	public static boolean checkLiteralKey(Expression<String> keyExpression, String syntaxName) {
		if (keyExpression instanceof Literal<String> literal) {
			String key = literal.getSingle();
			if (key != null && !isValidKey(key)) {
				Skript.error("'" + key + "' is not a valid name for a " + syntaxName
					+ ". Names may only contain letters, digits and underscores.");
				return false;
			}
		}
		return true;
	}

	public static boolean checkRuntimeKey(String key, String syntaxName) {
		if (isValidKey(key)) return true;
		SkriptLogger.LOGGER.error("'{}' is not a valid name for a {}. Names may only contain letters, digits and underscores.", key, syntaxName);
		return false;
	}

}
