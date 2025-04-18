package com.avpuser.telegram;

import java.util.Optional;

public interface TelegramMenuCommand {
    String getCommandName();

    Optional<String> getMenuDescription();
}