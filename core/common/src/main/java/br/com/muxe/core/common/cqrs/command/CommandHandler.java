package br.com.muxe.core.common.cqrs.command;

@FunctionalInterface
public interface CommandHandler<C extends Command<R>, R> {

    R handle(C command) throws Exception;
}
