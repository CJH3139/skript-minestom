package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Block")
@Description("The block at a certain point in an instance.")
@Examples("set block at player's position to stone")
public class ExprBlock extends SimpleExpression<Block> {

	static {
		Skript.registerExpression(ExprBlock.class, Block.class, ExpressionType.SIMPLE,
			"block[s] [type[s]] %directions% %points% [in [(world|instance)] %instances%] [1:with updates]");
	}

	private Expression<? extends Point> pointExpr;
	private Expression<Instance> instanceExpr;
	private boolean update;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		pointExpr = Direction.combine((Expression<? extends Direction>) expressions[0], (Expression<? extends Point>) expressions[1]);
		instanceExpr = (Expression<Instance>) expressions[2];
		update = parseResult.mark == 1;
		return true;
	}

	@Override
	protected @Nullable Block[] get(Event event) {
		Instance[] instances = instanceExpr.getArray(event);
		Point[] points = pointExpr.getArray(event);
		List<Block> blocks = new ArrayList<>();
		for (Instance instance : instances) {
			for (Point point : points) {
				if (!instance.isChunkLoaded(point)) continue;
				blocks.add(instance.getBlock(point));
			}
		}
		return blocks.toArray(new Block[0]);
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET) return CollectionUtils.array(Block.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		if (delta[0] == null) return;
		Block block = (Block) delta[0];
		Instance instance = instanceExpr.getSingle(event);
		if (instance == null) return;
		Point[] points = pointExpr.getArray(event);
		boolean autoLoad = instance.hasEnabledAutoChunkLoad();
		for (Point p : points) {
			// minestom loads the chunk itself when auto chunk load is enabled and throws otherwise
			if (!autoLoad && !instance.isChunkLoaded(p)) continue;
			instance.setBlock(p, block, update);
		}
	}

	@Override
	public boolean isSingle() {
		return pointExpr.isSingle();
	}

	@Override
	public Class<? extends Block> getReturnType() {
		return Block.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "block " + pointExpr.toString(event, debug) + " in instance " + instanceExpr.toString(event, debug);
	}

}
