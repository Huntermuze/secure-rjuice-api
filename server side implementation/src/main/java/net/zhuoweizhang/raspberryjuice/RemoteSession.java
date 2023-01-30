package net.zhuoweizhang.raspberryjuice;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RemoteSession {

	private final LocationType locationType;

	private Location origin;

	private final Socket socket;

	private BufferedReader in;

	private BufferedWriter out;

	private Thread inThread;

	private Thread outThread;

	private final ArrayDeque<String> inQueue = new ArrayDeque<>();

	private final ArrayDeque<String> outQueue = new ArrayDeque<>();

	public boolean running = true;

	public boolean pendingRemoval = false;

	public RaspberryJuicePlugin plugin;

	protected ArrayDeque<PlayerInteractEvent> interactEventQueue = new ArrayDeque<>();

	protected ArrayDeque<AsyncPlayerChatEvent> chatPostedQueue = new ArrayDeque<>();

	protected ArrayDeque<ProjectileHitEvent> projectileHitQueue = new ArrayDeque<>();

	private int maxCommandsPerTick = 9000;

	private boolean closed = false;

	private Player attachedPlayer = null;

	public RemoteSession(RaspberryJuicePlugin plugin, Socket socket) throws IOException {
		this.socket = socket;
		this.plugin = plugin;
		this.locationType = plugin.getLocationType();
		init();
	}

	public void init() throws IOException {
		socket.setTcpNoDelay(true);
		socket.setKeepAlive(true);
		socket.setTrafficClass(0x10);
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
		startThreads();
		plugin.getLogger().info("Opened connection to" + socket.getRemoteSocketAddress() + ".");
	}

	protected void startThreads() {
		inThread = new Thread(new InputThread());
		inThread.start();
		outThread = new Thread(new OutputThread());
		outThread.start();
	}


	public Location getOrigin() {
		return origin;
	}

	public void setOrigin(Location origin) {
		this.origin = origin;
	}

	public Socket getSocket() {
		return socket;
	}

	public void queuePlayerInteractEvent(PlayerInteractEvent event) {
		//plugin.getLogger().info(event.toString());
		interactEventQueue.add(event);
	}

	public void queueChatPostedEvent(AsyncPlayerChatEvent event) {
		//plugin.getLogger().info(event.toString());
		chatPostedQueue.add(event);
	}

	public void queueProjectileHitEvent(ProjectileHitEvent event) {
		//plugin.getLogger().info(event.toString());

		if (event.getEntityType() == EntityType.ARROW) {
			Arrow arrow = (Arrow) event.getEntity();
			if (arrow.getShooter() instanceof Player) {
				projectileHitQueue.add(event);
			}
		}
	}

	/**
	 * called from the server main thread
	 */
	public void tick() {
		if (origin == null) {
			switch (locationType) {
				case ABSOLUTE -> this.origin = new Location(plugin.getServer().getWorlds().get(0), 0, 0, 0);
				case RELATIVE -> this.origin = plugin.getServer().getWorlds().get(0).getSpawnLocation();
				default -> throw new IllegalArgumentException("Unknown location type " + locationType);
			}
		}
		int processedCount = 0;
		String message;
		while ((message = inQueue.poll()) != null) {
			handleLine(message);
			processedCount++;
			if (processedCount >= maxCommandsPerTick) {
				plugin.getLogger().warning("Over " + maxCommandsPerTick +
						" commands were queued - deferring " + inQueue.size() + " to next tick");
				break;
			}
		}

		if (!running && inQueue.size() <= 0) {
			pendingRemoval = true;
		}
	}

	protected void handleLine(String line) {
		//System.out.println(line);
		String methodName = line.substring(0, line.indexOf("("));
		//split string into args, handles , inside " i.e. ","
		String[] args = line.substring(line.indexOf("(") + 1, line.length() - 1).split(",");
		//System.out.println(methodName + ":" + Arrays.toString(args));
		handleCommand(methodName, args);
	}

	protected void handleCommand(String c, String[] args) {

		try {
			// get the server
			Server server = plugin.getServer();

			// get the world
			World world = origin.getWorld();

			// ASSIGNMENT 3 MODIFICATION - Modified by Huntermuze
			// args[0] holds the encrypted message, args[1] holds the mac.
			if (c.equals("decryptionSite")) {
				Encryption encryption = plugin.getEncryptionInstance();
				// Calculate the mac.
				String calculatedMac = encryption.calculateMac(args[0]).orElse("N/A");

				// Check if the MAC calculation failed.
				if (calculatedMac.equals("N/A")) {
					Logger.INSTANCE.logError("MAC CALCULATION FAILED!");
					return;
				}

				// Check if the two MACs match.
				if (!encryption.verifyAuthenticity(args[1], calculatedMac)) {
					Logger.INSTANCE.logError("THE MACS DO NOT MATCH, IMPLYING THE MESSAGE IS NOT AUTHENTIC. " +
							"CAUTION AGAINST TRUSTING SUCH SOURCE.");
					return;
				}

				// Since two MACs match, decrypt the ciphertext into plaintext.
				String plaintext = encryption.decryptCiphertext(args[0], encryption.getPrivateKey()).orElse("N/A");

				// Check if the decryption failed.
				if (plaintext.equals("N/A")) {
					Logger.INSTANCE.logError("THE CIPHERTEXT DECRYPTION FAILED!");
					return;
				}

				// Proceed as normal with the switch statement, but alter the variables to mimic a chat.post request.
				// The decrypted message should contain 3 components separated by a backslash: request + msg + studentIds.
				String[] components = plaintext.split("/");

				// This is the API command requested. It should be "chat.post".
				c = components[0];
				// This is the actual message contents that the API caller provided within the postToChat() function.
				// No point splitting the message up again, and re-processing when it is already in one piece.
				// Just create a new array with one element containing the entirety of the message. Less stress on the
				// chat.post functionality.
				args = new String[]{components[1]};

				Logger.INSTANCE.log("API Command Requested: " + c);
				Logger.INSTANCE.log("Message Contents: " + Arrays.toString(args));
				Logger.INSTANCE.log("The last two digits of the student ID codes are: " + components[2]);
			}

			switch (c) {
				// ASSIGNMENT 3 ADDITION - Created by Huntermuze
				// publicKey.request
				case "publicKey.request" -> {
					//Handles creating and sending the public key back to the client side.
					//Obtain the bytes of the public key.
					byte[] publicKeyBytes = plugin.getEncryptionInstance().getPublicKey().getEncoded();
					// Send the public key as a base64 string.
					send(Base64.getEncoder().encodeToString(publicKeyBytes));
				}
				// world.removeBlocksInRegion
				case "world.removeBlocksInRegion" -> {
					int[] blocks_to_remove = parseBlockIds(Arrays.copyOfRange(args, 6, args.length));
					System.out.println(Arrays.toString(blocks_to_remove));

					for (int x = Integer.parseInt(args[0]); x <= Integer.parseInt(args[3]); ++x) {
						for (int y = Integer.parseInt(args[1]); y <= Integer.parseInt(args[4]); ++y) {
							for (int z = Integer.parseInt(args[2]); z <= Integer.parseInt(args[5]); ++z) {

								Block block = world.getBlockAt(x, y, z);

								if (iterateAndCheckIfInside(blocks_to_remove, block.getType().getId())) {
									System.out.println("REMOVED BLOCK!");
									block.setType(Material.AIR);
								}
							}
						}
					}
				}
				// world.getHighestAndLowestYInRegion.
				case "world.getHighestAndLowestYInRegion" -> {
					int highestY = -1;
					int lowestY = 100;

					for (int x = Integer.parseInt(args[0]); x <= Integer.parseInt(args[2]); ++x) {
						for (int z = Integer.parseInt(args[1]); z <= Integer.parseInt(args[3]); ++z) {
							int currentY = world.getHighestBlockYAt(x, z);

							if (currentY > highestY) {
								highestY = currentY;
							}

							if (currentY < lowestY) {
								lowestY = currentY;
							}
						}
					}

					System.out.println("LOG >> FINAL highestY = " + highestY + " LOWEST " + lowestY);

					send(highestY + "," + lowestY);
				}
				// world.getLowestYInLine
				case "world.getLowestYInLine" -> {
					int lowest = Integer.parseInt(args[1]);

					for (int k = Integer.parseInt(args[2]); k <= Integer.parseInt(args[3]); ++k) {
						int currentY = world.getHighestBlockYAt(Integer.parseInt(args[0]), k);

						if (currentY < lowest) lowest = currentY;
					}

					send(lowest);
				}
				// world.getBlock
				case "world.getBlock" -> {
					Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
					loc.getBlock();
					send(world.getBlockTypeIdAt(loc));
				}
				// world.getBlocks
				case "world.getBlocks" -> {
					Location loc1 = parseRelativeBlockLocation(args[0], args[1], args[2]);
					Location loc2 = parseRelativeBlockLocation(args[3], args[4], args[5]);
					send(getBlocks(loc1, loc2));
				}
				// world.getBlockWithData
				case "world.getBlockWithData" -> {
					Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
					send(world.getBlockTypeIdAt(loc) + "," + world.getBlockAt(loc).getData());
				}
				// world.setBlock
				case "world.setBlock" -> {
					Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
					updateBlock(world, loc, Integer.parseInt(args[3]), (args.length > 4 ? Byte.parseByte(args[4]) : (byte) 0));
				}
				// world.setBlocks
				case "world.setBlocks" -> {
					Location loc1 = parseRelativeBlockLocation(args[0], args[1], args[2]);
					Location loc2 = parseRelativeBlockLocation(args[3], args[4], args[5]);
					int blockType = Integer.parseInt(args[6]);
					byte data = args.length > 7 ? Byte.parseByte(args[7]) : (byte) 0;
					setCuboid(loc1, loc2, blockType, data);
				}
				// world.getPlayerIds
				case "world.getPlayerIds" -> {
					StringBuilder bdr = new StringBuilder();
					Collection<? extends Player> players = Bukkit.getOnlinePlayers();
					if (players.size() > 0) {
						for (Player p : players) {
							bdr.append(p.getEntityId());
							bdr.append("|");
						}
						bdr.deleteCharAt(bdr.length() - 1);
						send(bdr.toString());
					} else {
						send("Fail");
					}
				}
				// world.getPlayerId
				case "world.getPlayerId" -> {
					Player p = plugin.getNamedPlayer(args[0]);
					if (p != null) {
						send(p.getEntityId());
					} else {
						plugin.getLogger().info("Player [" + args[0] + "] not found.");
						send("Fail");
					}
				}
				// entity.getListName
				case "entity.getName" -> {
					Entity e = plugin.getEntity(Integer.parseInt(args[0]));
					if (e == null) {
						plugin.getLogger().info("Player (or Entity) [" + args[0] + "] not found in entity.getName.");
					} else if (e instanceof Player) {
						Player p = (Player) e;
						//sending list name because plugin.getNamedPlayer() uses list name
						send(p.getPlayerListName());
					} else send(e.getName());

				}
				// world.getEntities
				case "world.getEntities" -> {
					int entityType = Integer.parseInt(args[0]);
					send(getEntities(world, entityType));
				}
				// world.removeEntity
				case "world.removeEntity" -> {
					int result = 0;
					for (Entity e : world.getEntities()) {
						if (e.getEntityId() == Integer.parseInt(args[0])) {
							e.remove();
							result = 1;
							break;
						}
					}
					send(result);
				}
				// world.removeEntities
				case "world.removeEntities" -> {
					int entityType = Integer.parseInt(args[0]);
					int removedEntitiesCount = 0;
					for (Entity e : world.getEntities()) {
						if (entityType == -1 || e.getType().getTypeId() == entityType) {
							e.remove();
							removedEntitiesCount++;
						}
					}
					send(removedEntitiesCount);
				}
				// chat.post
				case "chat.post" -> {
					//create chat message from args as it was split by ,
					StringBuilder chatMessage = new StringBuilder();
					int count;
					for (count = 0; count < args.length; count++) {
						chatMessage.append(args[count]).append(",");
					}
					chatMessage = new StringBuilder(chatMessage.substring(0, chatMessage.length() - 1));
					server.broadcastMessage(chatMessage.toString());
				}
				// events.clear
				case "events.clear" -> {
					interactEventQueue.clear();
					chatPostedQueue.clear();
				}
				// events.block.hits
				case "events.block.hits" -> send(getBlockHits());
				// events.chat.posts
				case "events.chat.posts" -> send(getChatPosts());
				// events.projectile.hits
				case "events.projectile.hits" -> send(getProjectileHits());
				// entity.events.clear
				case "entity.events.clear" -> {
					int entityId = Integer.parseInt(args[0]);
					clearEntityEvents(entityId);
				}
				// entity.events.block.hits
				case "entity.events.block.hits" -> {
					int entityId = Integer.parseInt(args[0]);
					send(getBlockHits(entityId));
				}
				// entity.events.chat.posts
				case "entity.events.chat.posts" -> {
					int entityId = Integer.parseInt(args[0]);
					send(getChatPosts(entityId));
				}
				// entity.events.projectile.hits
				case "entity.events.projectile.hits" -> {
					int entityId = Integer.parseInt(args[0]);
					send(getProjectileHits(entityId));
				}
				// player.getTile
				case "player.getTile" -> {
					Player currentPlayer = getCurrentPlayer();
					send(blockLocationToRelative(currentPlayer.getLocation()));
				}
				// player.setTile
				case "player.setTile" -> {
					String x = args[0], y = args[1], z = args[2];
					Player currentPlayer = getCurrentPlayer();
					//get players current location, so when they are moved we will use the same pitch and yaw (rotation)
					Location loc = currentPlayer.getLocation();
					currentPlayer.teleport(parseRelativeBlockLocation(x, y, z, loc.getPitch(), loc.getYaw()));
				}
				// player.getAbsPos
				case "player.getAbsPos" -> {
					Player currentPlayer = getCurrentPlayer();
					send(currentPlayer.getLocation());
				}
				// player.setAbsPos
				case "player.setAbsPos" -> {
					String x = args[0], y = args[1], z = args[2];
					Player currentPlayer = getCurrentPlayer();
					//get players current location, so when they are moved we will use the same pitch and yaw (rotation)
					Location loc = currentPlayer.getLocation();
					loc.setX(Double.parseDouble(x));
					loc.setY(Double.parseDouble(y));
					loc.setZ(Double.parseDouble(z));
					currentPlayer.teleport(loc);
				}
				// player.getPos
				case "player.getPos" -> {
					Player currentPlayer = getCurrentPlayer();
					send(locationToRelative(currentPlayer.getLocation()));
				}
				// player.setPos
				case "player.setPos" -> {
					String x = args[0], y = args[1], z = args[2];
					Player currentPlayer = getCurrentPlayer();
					//get players current location, so when they are moved we will use the same pitch and yaw (rotation)
					Location loc = currentPlayer.getLocation();
					currentPlayer.teleport(parseRelativeLocation(x, y, z, loc.getPitch(), loc.getYaw()));
				}
				// player.setDirection
				case "player.setDirection" -> {
					double x = Double.parseDouble(args[0]);
					double y = Double.parseDouble(args[1]);
					double z = Double.parseDouble(args[2]);
					Player currentPlayer = getCurrentPlayer();
					Location loc = currentPlayer.getLocation();
					loc.setDirection(new Vector(x, y, z));
					currentPlayer.teleport(loc);
				}
				// player.getDirection
				case "player.getDirection" -> {
					Player currentPlayer = getCurrentPlayer();
					send(currentPlayer.getLocation().getDirection().toString());
				}
				// player.setRotation
				case "player.setRotation" -> {
					float yaw = Float.parseFloat(args[0]);
					Player currentPlayer = getCurrentPlayer();
					Location loc = currentPlayer.getLocation();
					loc.setYaw(yaw);
					currentPlayer.teleport(loc);
				}
				// player.getRotation
				case "player.getRotation" -> {
					Player currentPlayer = getCurrentPlayer();
					float yaw = currentPlayer.getLocation().getYaw();
					// turn bukkit's 0 - -360 to positive numbers
					if (yaw < 0) yaw = yaw * -1;
					send(yaw);
				}
				// player.setPitch
				case "player.setPitch" -> {
					float pitch = Float.parseFloat(args[0]);
					Player currentPlayer = getCurrentPlayer();
					Location loc = currentPlayer.getLocation();
					loc.setPitch(pitch);
					currentPlayer.teleport(loc);
				}
				// player.getPitch
				case "player.getPitch" -> {
					Player currentPlayer = getCurrentPlayer();
					send(currentPlayer.getLocation().getPitch());
				}
				// player.getEntities
				case "player.getEntities" -> {
					Player currentPlayer = getCurrentPlayer();
					int distance = Integer.parseInt(args[0]);
					int entityTypeId = Integer.parseInt(args[1]);

					send(getEntities(world, currentPlayer.getEntityId(), distance, entityTypeId));
				}
				// player.removeEntities
				case "player.removeEntities" -> {
					Player currentPlayer = getCurrentPlayer();
					int distance = Integer.parseInt(args[0]);
					int entityType = Integer.parseInt(args[1]);

					send(removeEntities(world, currentPlayer.getEntityId(), distance, entityType));
				}
				// player.events.block.hits
				case "player.events.block.hits" -> {
					Player currentPlayer = getCurrentPlayer();
					send(getBlockHits(currentPlayer.getEntityId()));
				}
				// player.events.chat.posts
				case "player.events.chat.posts" -> {
					Player currentPlayer = getCurrentPlayer();
					send(getChatPosts(currentPlayer.getEntityId()));
				}
				// player.events.projectile.hits
				case "player.events.projectile.hits" -> {
					Player currentPlayer = getCurrentPlayer();
					send(getProjectileHits(currentPlayer.getEntityId()));
				}
				// player.events.clear
				case "player.events.clear" -> {
					Player currentPlayer = getCurrentPlayer();
					clearEntityEvents(currentPlayer.getEntityId());
				}
				// world.getHeight
				case "world.getHeight" -> send(world.getHighestBlockYAt(parseRelativeBlockLocation(args[0], "0", args[1])) - origin.getBlockY());


				// entity.getTile
				case "entity.getTile" -> {
					//get entity based on id
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						send(blockLocationToRelative(entity.getLocation()));
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
						send("Fail");
					}
				}
				// entity.setTile
				case "entity.setTile" -> {
					String x = args[1], y = args[2], z = args[3];
					//get entity based on id
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						//get entity's current location, so when they are moved we will use the same pitch and yaw (rotation)
						Location loc = entity.getLocation();
						entity.teleport(parseRelativeBlockLocation(x, y, z, loc.getPitch(), loc.getYaw()));
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
						send("Fail");
					}

				}
				// entity.getPos
				case "entity.getPos" -> {
					//get entity based on id
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					//Player entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						send(locationToRelative(entity.getLocation()));
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
						send("Fail");
					}
				}
				// entity.setPos
				case "entity.setPos" -> {
					String x = args[1], y = args[2], z = args[3];
					//get entity based on id
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						//get entity's current location, so when they are moved we will use the same pitch and yaw (rotation)
						Location loc = entity.getLocation();
						entity.teleport(parseRelativeLocation(x, y, z, loc.getPitch(), loc.getYaw()));
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
						send("Fail");
					}
				}
				// entity.setDirection
				case "entity.setDirection" -> {
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						double x = Double.parseDouble(args[1]);
						double y = Double.parseDouble(args[2]);
						double z = Double.parseDouble(args[3]);
						Location loc = entity.getLocation();
						loc.setDirection(new Vector(x, y, z));
						entity.teleport(loc);
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
					}
				}
				// entity.getDirection
				case "entity.getDirection" -> {
					//get entity based on id
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						send(entity.getLocation().getDirection().toString());
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
						send("Fail");
					}
				}
				// entity.setRotation
				case "entity.setRotation" -> {
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						float yaw = Float.parseFloat(args[1]);
						Location loc = entity.getLocation();
						loc.setYaw(yaw);
						entity.teleport(loc);
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
					}
				}
				// entity.getRotation
				case "entity.getRotation" -> {
					//get entity based on id
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						send(entity.getLocation().getYaw());
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
						send("Fail");
					}
				}
				// entity.setPitch
				case "entity.setPitch" -> {
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						float pitch = Float.parseFloat(args[1]);
						Location loc = entity.getLocation();
						loc.setPitch(pitch);
						entity.teleport(loc);
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
					}
				}
				// entity.getPitch
				case "entity.getPitch" -> {
					//get entity based on id
					Entity entity = plugin.getEntity(Integer.parseInt(args[0]));
					if (entity != null) {
						send(entity.getLocation().getPitch());
					} else {
						plugin.getLogger().info("Entity [" + args[0] + "] not found.");
						send("Fail");
					}
				}
				// entity.getEntities
				case "entity.getEntities" -> {
					int entityId = Integer.parseInt(args[0]);
					int distance = Integer.parseInt(args[1]);
					int entityTypeId = Integer.parseInt(args[2]);

					send(getEntities(world, entityId, distance, entityTypeId));
				}
				// entity.removeEntities
				case "entity.removeEntities" -> {
					int entityId = Integer.parseInt(args[0]);
					int distance = Integer.parseInt(args[1]);
					int entityType = Integer.parseInt(args[2]);

					send(removeEntities(world, entityId, distance, entityType));

				}
				// world.setSign
				case "world.setSign" -> {
					Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
					Block thisBlock = world.getBlockAt(loc);
					//blockType should be 68 for wall sign or 63 for standing sign
					int blockType = Integer.parseInt(args[3]);
					//facing direction for wall sign : 2=north, 3=south, 4=west, 5=east
					//rotation 0 - to 15 for standing sign : 0=south, 4=west, 8=north, 12=east
					byte blockData = Byte.parseByte(args[4]);
					if ((thisBlock.getTypeId() != blockType) || (thisBlock.getData() != blockData)) {
						thisBlock.setTypeIdAndData(blockType, blockData, true);
					}
					//plugin.getLogger().info("Creating sign at " + loc);
					if (thisBlock.getState() instanceof Sign) {
						Sign sign = (Sign) thisBlock.getState();
						for (int i = 5; i - 5 < 4 && i < args.length; i++) {
							sign.setLine(i - 5, args[i]);
						}
						sign.update();
					}
				}
				// world.spawnEntity
				case "world.spawnEntity" -> {
					Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
					Entity entity = world.spawnEntity(loc, EntityType.fromId(Integer.parseInt(args[3])));
					send(entity.getEntityId());
				}
				case "player.getBiome" -> {
					Location loc = parseRelativeBlockLocation(args[0], args[1], args[2]);
					send(loc.getBlock().getBiome().toString());
				}
				// world.getEntityTypes
				// not a command which is supported
				case "world.getEntityTypes" -> {
					StringBuilder bdr = new StringBuilder();
					for (EntityType entityType : EntityType.values()) {
						if (entityType.isSpawnable() && entityType.getTypeId() >= 0) {
							bdr.append(entityType.getTypeId());
							bdr.append(",");
							bdr.append(entityType.toString());
							bdr.append("|");
						}
					}
					send(bdr.toString());
				}
				default -> {
					plugin.getLogger().warning(c + " is not supported.");
					send("Fail");
				}
			}
		} catch (Exception e) {

			plugin.getLogger().warning("Error occured handling command");
			e.printStackTrace();
			send("Fail");

		}
	}

	// create a cuboid of lots of blocks 
	private void setCuboid(Location pos1, Location pos2, int blockType, byte data) {
		int minX, maxX, minY, maxY, minZ, maxZ;
		World world = pos1.getWorld();
		minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
		maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
		minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
		maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
		minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
		maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

		for (int x = minX; x <= maxX; ++x) {
			for (int z = minZ; z <= maxZ; ++z) {
				for (int y = minY; y <= maxY; ++y) {
					updateBlock(world, x, y, z, blockType, data);
				}
			}
		}
	}

	// get a cuboid of lots of blocks
	private String getBlocks(Location pos1, Location pos2) {
		StringBuilder blockData = new StringBuilder();

		int minX, maxX, minY, maxY, minZ, maxZ;
		World world = pos1.getWorld();
		minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
		maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
		minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
		maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
		minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
		maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

		for (int y = minY; y <= maxY; ++y) {
			for (int x = minX; x <= maxX; ++x) {
				for (int z = minZ; z <= maxZ; ++z) {
					blockData.append(world.getBlockTypeIdAt(x, y, z)).append(",");
				}
			}
		}

		return blockData.substring(0, blockData.length() > 0 ? blockData.length() - 1 : 0);    // We don't want last comma
	}

	// updates a block
	private void updateBlock(World world, Location loc, int blockType, byte blockData) {
		Block thisBlock = world.getBlockAt(loc);
		updateBlock(thisBlock, blockType, blockData);
	}

	private void updateBlock(World world, int x, int y, int z, int blockType, byte blockData) {
		Block thisBlock = world.getBlockAt(x, y, z);
		updateBlock(thisBlock, blockType, blockData);
	}

	private void updateBlock(Block thisBlock, int blockType, byte blockData) {
		// check to see if the block is different - otherwise leave it
		if ((thisBlock.getTypeId() != blockType) || (thisBlock.getData() != blockData)) {
			thisBlock.setTypeIdAndData(blockType, blockData, true);
		}
	}

	// gets the current player
	public Player getCurrentPlayer() {
		Player player = attachedPlayer;
		// if the player hasnt already been retreived for this session, go and get it.
		if (player == null) {
			player = plugin.getHostPlayer();
			attachedPlayer = player;
		}
		return player;
	}

	public Player getCurrentPlayer(String name) {
		// if a named player is returned use that
		Player player = plugin.getNamedPlayer(name);
		// otherwise if there is an attached player for this session use that
		if (player == null) {
			player = attachedPlayer;
			// otherwise go and get the host player and make that the attached player
			if (player == null) {
				player = plugin.getHostPlayer();
				attachedPlayer = player;
			}
		}
		return player;
	}


	public Location parseRelativeBlockLocation(String xstr, String ystr, String zstr) {
		int x = (int) Double.parseDouble(xstr);
		int y = (int) Double.parseDouble(ystr);
		int z = (int) Double.parseDouble(zstr);
		return parseLocation(origin.getWorld(), x, y, z, origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
	}

	public Location parseRelativeLocation(String xstr, String ystr, String zstr) {
		double x = Double.parseDouble(xstr);
		double y = Double.parseDouble(ystr);
		double z = Double.parseDouble(zstr);
		return parseLocation(origin.getWorld(), x, y, z, origin.getX(), origin.getY(), origin.getZ());
	}

	public Location parseRelativeBlockLocation(String xstr, String ystr, String zstr, float pitch, float yaw) {
		Location loc = parseRelativeBlockLocation(xstr, ystr, zstr);
		loc.setPitch(pitch);
		loc.setYaw(yaw);
		return loc;
	}

	public Location parseRelativeLocation(String xstr, String ystr, String zstr, float pitch, float yaw) {
		Location loc = parseRelativeLocation(xstr, ystr, zstr);
		loc.setPitch(pitch);
		loc.setYaw(yaw);
		return loc;
	}

	public String blockLocationToRelative(Location loc) {
		return parseLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
	}

	public String locationToRelative(Location loc) {
		return parseLocation(loc.getX(), loc.getY(), loc.getZ(), origin.getX(), origin.getY(), origin.getZ());
	}

	private String parseLocation(double x, double y, double z, double originX, double originY, double originZ) {
		return (x - originX) + "," + (y - originY) + "," + (z - originZ);
	}

	private Location parseLocation(World world, double x, double y, double z, double originX, double originY, double originZ) {
		return new Location(world, originX + x, originY + y, originZ + z);
	}

	private String parseLocation(int x, int y, int z, int originX, int originY, int originZ) {
		return (x - originX) + "," + (y - originY) + "," + (z - originZ);
	}

	private Location parseLocation(World world, int x, int y, int z, int originX, int originY, int originZ) {
		return new Location(world, originX + x, originY + y, originZ + z);
	}

	private int[] parseBlockIds(String[] args) {
		int[] temp = new int[args.length];

		for (int i = 0; i < args.length; ++i) temp[i] = Integer.parseInt(args[i]);

		return temp;
	}

	private boolean iterateAndCheckIfInside(int[] array, int criterion) {
		for (int j : array) {
			if (criterion == j) return true;
		}

		return false;
	}

	private double getDistance(Entity ent1, Entity ent2) {
		if (ent1 == null || ent2 == null)
			return -1;
		double dx = ent2.getLocation().getX() - ent1.getLocation().getX();
		double dy = ent2.getLocation().getY() - ent1.getLocation().getY();
		double dz = ent2.getLocation().getZ() - ent1.getLocation().getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private String getEntities(World world, int entityType) {
		StringBuilder bdr = new StringBuilder();
		for (Entity e : world.getEntities()) {
			if (((entityType == -1 && e.getType().getTypeId() >= 0) || e.getType().getTypeId() == entityType) &&
					e.getType().isSpawnable()) {
				bdr.append(getEntityMsg(e));
			}
		}
		return bdr.toString();
	}

	private String getEntities(World world, int entityId, int distance, int entityType) {
		Entity playerEntity = plugin.getEntity(entityId);
		StringBuilder bdr = new StringBuilder();
		for (Entity e : world.getEntities()) {
			if (((entityType == -1 && e.getType().getTypeId() >= 0) || e.getType().getTypeId() == entityType) &&
					e.getType().isSpawnable() &&
					getDistance(playerEntity, e) <= distance) {
				bdr.append(getEntityMsg(e));
			}
		}
		return bdr.toString();
	}

	private String getEntityMsg(Entity entity) {
		return entity.getEntityId() +
				"," +
				entity.getType().getTypeId() +
				"," +
				entity.getType() +
				"," +
				entity.getLocation().getX() +
				"," +
				entity.getLocation().getY() +
				"," +
				entity.getLocation().getZ() +
				"|";
	}

	private int removeEntities(World world, int entityId, int distance, int entityType) {
		int removedEntitiesCount = 0;
		Entity playerEntityId = plugin.getEntity(entityId);
		for (Entity e : world.getEntities()) {
			if ((entityType == -1 || e.getType().getTypeId() == entityType) && getDistance(playerEntityId, e) <= distance) {
				e.remove();
				removedEntitiesCount++;
			}
		}
		return removedEntitiesCount;
	}

	private String getBlockHits() {
		return getBlockHits(-1);
	}

	private String getBlockHits(int entityId) {
		StringBuilder b = new StringBuilder();
		for (Iterator<PlayerInteractEvent> iter = interactEventQueue.iterator(); iter.hasNext(); ) {
			PlayerInteractEvent event = iter.next();
			if (entityId == -1 || event.getPlayer().getEntityId() == entityId) {
				Block block = event.getClickedBlock();
				Location loc = block.getLocation();
				b.append(blockLocationToRelative(loc));
				b.append(",");
				b.append(blockFaceToNotch(event.getBlockFace()));
				b.append(",");
				b.append(event.getPlayer().getEntityId());
				b.append("|");
				iter.remove();
			}
		}
		if (b.length() > 0)
			b.deleteCharAt(b.length() - 1);

		return b.toString();
	}

	private String getChatPosts() {
		return getChatPosts(-1);
	}

	private String getChatPosts(int entityId) {
		StringBuilder b = new StringBuilder();
		for (Iterator<AsyncPlayerChatEvent> iter = chatPostedQueue.iterator(); iter.hasNext(); ) {
			AsyncPlayerChatEvent event = iter.next();
			if (entityId == -1 || event.getPlayer().getEntityId() == entityId) {
				b.append(event.getPlayer().getEntityId());
				b.append(",");
				b.append(event.getMessage());
				b.append("|");
				iter.remove();
			}
		}
		if (b.length() > 0)
			b.deleteCharAt(b.length() - 1);
		return b.toString();
	}

	private String getProjectileHits() {
		return getProjectileHits(-1);
	}

	private String getProjectileHits(int entityId) {
		StringBuilder b = new StringBuilder();
		for (Iterator<ProjectileHitEvent> iter = projectileHitQueue.iterator(); iter.hasNext(); ) {
			ProjectileHitEvent event = iter.next();
			Arrow arrow = (Arrow) event.getEntity();
			LivingEntity shooter = (LivingEntity) arrow.getShooter();
			if (entityId == -1 || shooter.getEntityId() == entityId) {
				if (shooter instanceof Player) {
					Player player = (Player) shooter;
					Block block = arrow.getAttachedBlock();
					if (block == null)
						block = arrow.getLocation().getBlock();
					Location loc = block.getLocation();
					b.append(blockLocationToRelative(loc));
					b.append(",");
					b.append(1); //blockFaceToNotch(event.getBlockFace()), but don't really care
					b.append(",");
					b.append(player.getPlayerListName());
					b.append(",");
					Entity hitEntity = event.getHitEntity();
					if (hitEntity != null) {
						if (hitEntity instanceof Player) {
							Player hitPlayer = (Player) hitEntity;
							b.append(hitPlayer.getPlayerListName());
						} else {
							b.append(hitEntity.getName());
						}
					}
				}
				b.append("|");
				arrow.remove();
				iter.remove();
			}
		}
		if (b.length() > 0)
			b.deleteCharAt(b.length() - 1);
		return b.toString();

	}

	private void clearEntityEvents(int entityId) {
		interactEventQueue.removeIf(event -> event.getPlayer().getEntityId() == entityId);
		chatPostedQueue.removeIf(event -> event.getPlayer().getEntityId() == entityId);

		for (Iterator<ProjectileHitEvent> iter = projectileHitQueue.iterator(); iter.hasNext(); ) {
			ProjectileHitEvent event = iter.next();
			Arrow arrow = (Arrow) event.getEntity();
			LivingEntity shooter = (LivingEntity) arrow.getShooter();
			if (shooter.getEntityId() == entityId)
				iter.remove();
		}
	}

	public void send(Object a) {
		System.out.println("LOG >> SENDING: " + a);
		send(a.toString());
	}

	public void send(String a) {
		if (pendingRemoval) return;
		synchronized (outQueue) {
			outQueue.add(a);
		}
	}

	public void close() {
		if (closed) return;
		running = false;
		pendingRemoval = true;

		//wait for threads to stop
		try {
			inThread.join(2000);
			outThread.join(2000);
		} catch (InterruptedException e) {
			plugin.getLogger().warning("Failed to stop in/out thread");
			e.printStackTrace();
		}

		try {
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		plugin.getLogger().info("Closed connection to" + socket.getRemoteSocketAddress() + ".");
	}

	public void kick(String reason) {
		try {
			out.write(reason);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		close();
	}

	/**
	 * socket listening thread
	 */
	private class InputThread implements Runnable {
		public void run() {
			plugin.getLogger().info("Starting input thread");
			while (running) {
				try {
					String newLine = in.readLine();
					//System.out.println(newLine);
					if (newLine == null) {
						running = false;
					} else {
						inQueue.add(newLine);
						//System.out.println("Added to in queue");
					}
				} catch (Exception e) {
					// if its running raise an error
					if (running) {
						if (e.getMessage().equals("Connection reset")) {
							plugin.getLogger().info("Connection reset");
						} else {
							e.printStackTrace();
						}
						running = false;
					}
				}
			}
			//close in buffer
			try {
				in.close();
			} catch (Exception e) {
				plugin.getLogger().warning("Failed to close in buffer");
				e.printStackTrace();
			}
		}
	}

	private class OutputThread implements Runnable {
		public void run() {
			plugin.getLogger().info("Starting output thread!");
			while (running) {
				try {
					String line;
					while ((line = outQueue.poll()) != null) {
						out.write(line);
						out.write('\n');
					}
					out.flush();
					Thread.yield();
					Thread.sleep(1L);
				} catch (Exception e) {
					// if its running raise an error
					if (running) {
						e.printStackTrace();
						running = false;
					}
				}
			}
			//close out buffer
			try {
				out.close();
			} catch (Exception e) {
				plugin.getLogger().warning("Failed to close out buffer");
				e.printStackTrace();
			}
		}
	}

	/**
	 * from CraftBukkit's org.bukkit.craftbukkit.block.CraftBlock.blockFactToNotch
	 */
	public static int blockFaceToNotch(BlockFace face) {
		return switch (face) {
			case DOWN -> 0;
			case UP -> 1;
			case NORTH -> 2;
			case SOUTH -> 3;
			case WEST -> 4;
			case EAST -> 5;
			default -> 7; // Good as anything here, but technically invalid
		};
	}

}
