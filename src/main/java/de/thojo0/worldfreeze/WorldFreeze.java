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
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class WorldFreeze implements ModInitializer {
	public static final String MOD_ID = "worldfreeze";
	public static final ModContainer MOD_CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).get();
	public static final ModMetadata MOD_META = MOD_CONTAINER.getMetadata();
	public static final String MOD_NAME = MOD_META.getName();
	public static final String MOD_VERSION = MOD_META.getVersion().getFriendlyString();

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final MutableComponent WORLD_FROZEN_MSG = Component.literal("World is frozen!").withStyle(ChatFormatting.RED);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing " + MOD_NAME + " " + MOD_VERSION);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal(MOD_ID)
					.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_OWNER))
					.then(Commands.literal("add")
							.executes(context -> {
								addWorld(context.getSource(), context.getSource().getLevel());
								return 0;
							})
							.then(getDimensionArgument(false, WorldFreeze::addWorld)))
					.then(Commands.literal("remove")
							.executes(context -> {
								removeWorld(context.getSource(), context.getSource().getLevel());
								return 0;
							})
							.then(getDimensionArgument(true, WorldFreeze::removeWorld)))
					.then(Commands.literal("list")
							.executes(context -> list(context.getSource()))));
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (isFrozen(world)) {
				sendMessage(player, WORLD_FROZEN_MSG);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!player.isSpectator() && isFrozen(world)) {
				sendMessage(player, WORLD_FROZEN_MSG);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
			if (isFrozen(world)) {
				sendMessage(player, WORLD_FROZEN_MSG);
				return false;
			}
			return true;
		});
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, oldWorld, newWorld) -> {
			updatePlayerAbilities(player);
		});
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Identifier> getDimensionArgument(
			boolean frozen, BiConsumer<CommandSourceStack, ServerLevel> consumer) {
		return Commands.argument("dimension", DimensionArgument.dimension())
				.executes(context -> {
					consumer.accept(context.getSource(),
							DimensionArgument.getDimension(context, "dimension"));
					return 0;
				}).suggests((context, builder) -> {
					return SharedSuggestionProvider.suggestResource(context.getSource().levels().stream()
							.filter(k -> frozen == isFrozen(context.getSource().getServer().getLevel(k)))
							.map(ResourceKey::identifier), builder);
				});
	}

	public static final AttachmentType<Boolean> IS_FROZEN = AttachmentRegistry
			.createPersistent(Identifier.fromNamespaceAndPath(MOD_ID, "frozen"), Codec.BOOL);

	private static void addWorld(CommandSourceStack source, ServerLevel world) {
		if (world.setAttached(IS_FROZEN, true)) {
			sendMessage(source, "World is already frozen");
		} else {
			updatePlayerAbilities(world);
			sendMessage(source, "Added world: " + world.dimension().identifier());
		}
	}

	private static void removeWorld(CommandSourceStack source, ServerLevel world) {
		if (world.removeAttached(IS_FROZEN)) {
			updatePlayerAbilities(world);
			sendMessage(source, "Removed world: " + world.dimension().identifier());
		} else {
			sendMessage(source, "World is not frozen");
		}
	}

	private static void updatePlayerAbilities(ServerLevel world) {
		world.players().forEach(WorldFreeze::updatePlayerAbilities);
	}

	private static void updatePlayerAbilities(ServerPlayer player) {
		player.gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
		if (isFrozen(player.level())) {
			player.getAbilities().mayBuild = false;
		}
		player.onUpdateAbilities();
	}

	private static int list(CommandSourceStack source) {
		StringBuilder sb = new StringBuilder("Current worlds:");
		int count[] = { 0 };
		Streams.stream(source.getServer().getAllLevels())
				.filter(WorldFreeze::isFrozen)
				.map(w -> w.dimension().identifier().toString())
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

	public static void sendMessage(Player source, String message) {
		sendMessage(source.createCommandSourceStackForNameResolution((ServerLevel) source.level()), message);
	}

	public static void sendMessage(Player source, Component... message) {
		sendMessage(source.createCommandSourceStackForNameResolution((ServerLevel) source.level()), message);
	}

	public static void sendMessage(CommandSourceStack source, String message) {
		sendMessage(source, Component.literal(message).withStyle(ChatFormatting.GRAY));
	}

	public static void sendMessage(CommandSourceStack source, Component... message) {
		MutableComponent msg = Component.empty()
				.append(Component.literal("[" + MOD_NAME + "] ").withStyle(ChatFormatting.DARK_PURPLE));
		for (Component text : message) {
			msg.append(text);
		}
		source.sendSystemMessage(msg);
	}

	public static boolean isFrozen(Level world) {
		return world.getAttachedOrElse(IS_FROZEN, false);
	}
}