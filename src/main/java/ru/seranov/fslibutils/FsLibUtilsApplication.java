package ru.seranov.fslibutils;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.seranov.fslibutils.command.CommandDispatcher;

@SpringBootApplication
@RequiredArgsConstructor
public class FsLibUtilsApplication implements ApplicationRunner {

    private final CommandDispatcher dispatcher;

    private static volatile int exitCode = 0;

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(FsLibUtilsApplication.class, args);
        int code = exitCode;
        ctx.close();
        System.exit(code);
    }

    @Override
    public void run(ApplicationArguments args) {
        exitCode = dispatcher.run(args.getSourceArgs());
    }
}
