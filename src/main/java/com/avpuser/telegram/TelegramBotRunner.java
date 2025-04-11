package com.avpuser.telegram;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramBotRunner {

    private static final Logger logger = LogManager.getLogger(TelegramBotRunner.class);

    private final TelegramLongPollingBot bot;

    private BotSession botSession;

    public TelegramBotRunner(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public void stop() {
        if (botSession == null || !botSession.isRunning()) {
            logger.info(bot.getBotUsername() + " already stopped");
        }
        botSession.stop();
    }

    public void run() {
        if (botSession != null && botSession.isRunning()) {
            logger.info(bot.getBotUsername() + " already running");
            return;
        }

        TelegramBotsApi botsApi;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        try {
            botSession = botsApi.registerBot(bot);
            logger.info(bot.getBotUsername() + " started");
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }
}
