package com.avpuser.telegram;

import org.apache.logging.log4j.LogManager;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class TelegramBotCommandRegistrar {

    private static final Logger logger = LogManager.getLogger(TelegramBotCommandRegistrar.class);

    public static void registerMenuCommands(TelegramLongPollingBot bot, List<TelegramMenuCommand> commandsToShow) {
        try {
            List<BotCommand> commands = commandsToShow.stream()
                    .map(cmd -> new BotCommand(cmd.getCommandName(), cmd.getMenuDescription().orElse("")))
                    .toList();

            SetMyCommands setMyCommands = new SetMyCommands();
            setMyCommands.setCommands(commands);
            bot.execute(setMyCommands);

            String commandsStr = commandsToShow.stream()
                    .map(TelegramMenuCommand::getCommandName)
                    .collect(Collectors.joining(", "));

            logger.info("Telegram bot commands registered successfully: " + commandsStr);
        } catch (TelegramApiException e) {
            logger.error("Failed to register Telegram bot commands", e);
        }
    }
}
