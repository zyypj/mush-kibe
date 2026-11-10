package br.com.muxe.core.common.language.application.query;

import br.com.muxe.core.common.cqrs.query.QueryHandler;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.LanguageRepository;
import br.com.muxe.core.common.persistence.transaction.Transactions;

public final class FindLanguageHandler implements QueryHandler<FindLanguageQuery, Language> {

    private final LanguageRepository languages;
    private final Transactions transactions;

    public FindLanguageHandler(LanguageRepository languages, Transactions transactions) {
        this.languages = languages;
        this.transactions = transactions;
    }

    @Override
    public Language handle(FindLanguageQuery query) {
        return transactions.readOnly("language.find", () -> languages.find(query.playerId())).orElse(Language.DEFAULT);
    }
}
