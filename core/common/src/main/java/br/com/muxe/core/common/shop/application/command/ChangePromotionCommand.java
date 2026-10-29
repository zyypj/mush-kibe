package br.com.muxe.core.common.shop.application.command;

import br.com.muxe.core.common.cqrs.command.Command;
import br.com.muxe.core.common.result.Result;
import br.com.muxe.core.common.shop.domain.Promotion;
import java.util.Objects;

public record ChangePromotionCommand(Promotion promotion) implements Command<Result<Promotion>> {

    public ChangePromotionCommand {
        Objects.requireNonNull(promotion, "promotion");
    }
}
