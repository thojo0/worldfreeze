package de.thojo0.worldfreeze;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Streams;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.serialization.Codec;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class WorldFreeze implements ModInitializer {
	public static final String MOD_ID = "worldfreeze";
	public static final ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).get();
	public static final ModMetadata MOD_META = MOD_CONTAINER.getMetadata();
	public static final String MOD_NAME = MOD_META.getName();
	public static final String MOD_VERSION = MOD_META.getVersion().getFriendlyString();

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final MutableText WORLD_FROZEN_MSG = Text.literal("World is frozen!").formatted(Formatting.RED);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing " + MOD_NAME + " " + MOD_VERSION);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal(MOD_ID)
					.requires(source -> source.hasPermissionLevel(4))
					.then(CommandManager.literal("add")
							.executes(context -> {
								addWorld(context.getSource(), context.getSource().getWorld());
								return 0;
							})
							.then(getDimensionArgument(false, WorldFreeze::addWorld)))
					.then(CommandManager.literal("remove")
							.executes(context -> {
								removeWorld(context.getSource(), context.getSource().getWorld());
								return 0;
							})
							.then(getDimensionArgument(true, WorldFreeze::removeWorld)))
					.then(CommandManager.literal("list")
							.executes(context -> list(context.getSource()))));
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (isFrozen(world)) {
				sendMessage(player, WORLD_FROZEN_MSG);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!player.isSpectator() && isFrozen(world)) {
				sendMessage(player, WORLD_FROZEN_MSG);
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
			if (isFrozen(world)) {
				sendMessage(player, WORLD_FROZEN_MSG);
				return false;
			}
			return true;
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, oldWorld, newWorld) -> {
			updatePlayerAbilities(player);
		});
	}

	private static RequiredArgumentBuilder<ServerCommandSource, Identifier> getDimensionArgument(
			boolean frozen, BiConsumer<ServerCommandSource, ServerWorld> consumer) {
		return CommandManager.argument("dimension", DimensionArgumentType.dimension())
				.executes(context -> {
					consumer.accept(context.getSource(),
							DimensionArgumentType.getDimensionArgument(context, "dimension"));
					return 0;
				}).suggests((context, builder) -> {
					return CommandSource.suggestIdentifiers(context.getSource().getWorldKeys().stream()
							.filter(k -> frozen == isFrozen(context.getSource().getServer().getWorld(k)))
							.map(RegistryKey::getValue), builder);
				});
	}

	public static final AttachmentType<Boolean> IS_FROZEN = AttachmentRegistry
			.createPersistent(Identifier.of(MOD_ID, "frozen"), Codec.BOOL);

	private static void addWorld(ServerCommandSource source, ServerWorld world) {
		if (world.setAttached(IS_FROZEN, true)) {
			sendMessage(source, "World is already frozen");
		} else {
			updatePlayerAbilities(world);
			sendMessage(source, "Added world: " + world.getRegistryKey().getValue());
		}
	}

	private static void removeWorld(ServerCommandSource source, ServerWorld world) {
		if (world.removeAttached(IS_FROZEN)) {
			updatePlayerAbilities(world);
			sendMessage(source, "Removed world: " + world.getRegistryKey().getValue());
		} else {
			sendMessage(source, "World is not frozen");
		}
	}

	private static void updatePlayerAbilities(ServerWorld world) {
		world.getPlayers().forEach(WorldFreeze::updatePlayerAbilities);
	}

	private static void updatePlayerAbilities(ServerPlayerEntity player) {
		player.interactionManager.getGameMode().setAbilities(player.getAbilities());
		if (isFrozen(player.getEntityWorld())) {
			player.getAbilities().allowModifyWorld = false;
		}
		player.sendAbilitiesUpdate();
	}

	private static int list(ServerCommandSource source) {
		StringBuilder sb = new StringBuilder("Current worlds:");
		int count[] = { 0 };
		Streams.stream(source.getServer().getWorlds())
				.filter(WorldFreeze::isFrozen)
				.map(w -> w.getRegistryKey().getValue().toString())
				.sorted()
				.forEachOrdered(s -> {
					count[0]++;
					sb.append("\n - " + s);
				});
		if (count[0] == 0) {
			sb.append("\n No worlds are frozen");
		}
		sendMessage(source, sb.toString());
		return count[0];
	}

	public static void sendMessage(PlayerEntity source, String message) {
		sendMessage(source.getCommandSource((ServerWorld) source.getEntityWorld()), message);
	}

	public static void sendMessage(PlayerEntity source, Text... message) {
		sendMessage(source.getCommandSource((ServerWorld) source.getEntityWorld()), message);
	}

	public static void sendMessage(ServerCommandSource source, String message) {
		sendMessage(source, Text.literal(message).formatted(Formatting.GRAY));
	}

	public static void sendMessage(ServerCommandSource source, Text... message) {
		MutableText msg = Text.empty()
				.append(Text.literal("[" + MOD_NAME + "] ").formatted(Formatting.DARK_PURPLE));
		for (Text text : message) {
			msg.append(text);
		}
		source.sendMessage(msg);
	}

	public static boolean isFrozen(World world) {
		return world.getAttachedOrElse(IS_FROZEN, false);
	}
}