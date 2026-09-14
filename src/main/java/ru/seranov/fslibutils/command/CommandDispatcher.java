package ru.seranov.fslibutils.command;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes CLI arguments to the matching {@link CliCommand} implementation.
 *
 * <p>Usage: {@code java -jar fs-lib-utils.jar <command> [options]}
 * <p>Available commands: group
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandDispatcher {

    private final List<CliCommand> commands;
    private Map<String, CliCommand> commandMap;

    @PostConstruct
    void init() {
        commandMap = commands.stream()
                .collect(Collectors.toUnmodifiableMap(CliCommand::name, Function.identity()));
    }

    public int run(String[] args) {
        if (args.length == 0) {
            printHelp();
            return 1;
        }

        String commandName = args[0];
        CliCommand command = commandMap.get(commandName);
        if (command == null) {
            log.error("Unknown command: '{}'. Available commands: {}", commandName, commandMap.keySet());
            return 1;
        }

        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);
        return command.execute(commandArgs);
    }

    private void printHelp() {
        log.info("Usage: fs-lib-utils <command> [options]");
        log.info("Available commands:");
        commands.forEach(c -> log.info("  {}", c.name()));
    }
}
