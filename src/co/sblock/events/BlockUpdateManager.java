package co.sblock.events;

import java.util.Queue;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import co.sblock.Sblock;
import co.sblock.utilities.HashQueue;

/**
 * Manager for queuing block updates to prevent redundant block updates by Effects such as Tunnel
 * Bore and Liquid Cooled.
 * 
 * @author Jikoo
 */
public class BlockUpdateManager {

	private final Sblock plugin;
	private final Queue<Block> pending;
	private BukkitTask queueDrain;
	private final BlockFace[] adjacent;

	public BlockUpdateManager(Sblock plugin) {
		this.plugin = plugin;
		this.pending = new HashQueue<>();
		this.adjacent = new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
				BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	}

	public void queueBlock(Block block) {
		// Only update blocks with adjacent non-air blocks
		if (block.getType() == Material.AIR) {
			boolean update = false;
			for (BlockFace face : adjacent) {
				update = block.getRelative(face).getType() != Material.AIR;
				if (update) {
					break;
				}
			}
			if (!update) {
				return;
			}
		}
		pending.add(block);
		startTask();
	}

	private void startTask() {
		if (queueDrain == null) {
			queueDrain = new QueueDrainRunnable().runTaskTimer(plugin, 0, 1L);
		}
	}

	private class QueueDrainRunnable extends BukkitRunnable {
		@Override
		public void run() {
			for (int i = 0; i < 50 && !pending.isEmpty(); i++) {
				System.out.println("updating " + pending.peek().getLocation());
				pending.poll().getState().update(true, true);
			}
			if (pending.isEmpty()) {
				this.cancel();
				queueDrain = null;
			}
		}
	}

}
