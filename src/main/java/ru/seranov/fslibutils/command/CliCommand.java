package ru.seranov.fslibutils.command;

/**
 * Contract for a CLI sub-command.
 */
public interface CliCommand {

    /** Returns the command name used on the command line (e.g. "group"). */
    String name();

    /** Executes the command with the remaining arguments and returns an exit code. */
    int execute(String[] args);
}
