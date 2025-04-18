package com.avpuser.telegram;

import org.apache.logging.log4j.LogManager;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TelegramBotMetadataManager {

    private static final Logger logger = LogManager.getLogger(TelegramBotMetadataManager.class);

    private final TelegramLongPollingBot bot;

    private final TelegramBotApi telegramBotApi;

    public TelegramBotMetadataManager(TelegramLongPollingBot bot, TelegramBotApi telegramBotApi) {
        this.bot = bot;
        this.telegramBotApi = telegramBotApi;
    }

    public void registerMenuCommands(List<TelegramMenuCommand> commandsToShow) {
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

    public void setBotName(String name) {
        updateIfChanged(telegramBotApi::getMyName, telegramBotApi::setMyName, name, "name");
    }

    public void setBotDescription(String description) {
        updateIfChanged(telegramBotApi::getMyDescription, telegramBotApi::setMyDescription, description, "description");
    }

    public void setBotShortDescription(String shortDescription) {
        updateIfChanged(telegramBotApi::getMyShortDescription, telegramBotApi::setMyShortDescription, shortDescription, "short description");
    }

    private void updateIfChanged(
            Supplier<String> getter,
            Consumer<String> setter,
            String newValue,
            String fieldName
    ) {
        try {
            String current = getter.get();
            if (current.equals(newValue)) {
                logger.info("Telegram bot {} already: {}", fieldName, newValue);
                return;
            }

            setter.accept(newValue);
            logger.info("New telegram bot {}: {}", fieldName, newValue);
        } catch (RuntimeException e) {
            logger.error("Error during set telegram " + fieldName, e);
        }
    }
}
