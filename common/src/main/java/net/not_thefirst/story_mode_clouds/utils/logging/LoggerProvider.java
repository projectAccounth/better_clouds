package net.not_thefirst.story_mode_clouds.utils.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.not_thefirst.story_mode_clouds.Initializer;

public class LoggerProvider {
    private LoggerProvider() {}

    private static final Logger LOGGER = LogManager.getLogger(Initializer.MOD_ID);

    public static Logger get() { return LOGGER; }
}
