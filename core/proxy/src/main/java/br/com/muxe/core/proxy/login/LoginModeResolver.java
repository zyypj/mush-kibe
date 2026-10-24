package br.com.muxe.core.proxy.login;

import br.com.muxe.core.common.account.application.query.FindAccountByUsernameQuery;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.cqrs.query.QueryBus;
import br.com.muxe.core.proxy.premium.PremiumLookup;
import br.com.muxe.core.proxy.premium.PremiumStatus;
import java.util.Optional;

public final class LoginModeResolver {

    private final QueryBus queries;
    private final PremiumLookup premium;
    private final boolean premiumEnabled;

    public LoginModeResolver(QueryBus queries, PremiumLookup premium, boolean premiumEnabled) {
        this.queries = queries;
        this.premium = premium;
        this.premiumEnabled = premiumEnabled;
    }

    public LoginMode resolve(String username) {
        Optional<Account> account = queries.ask(new FindAccountByUsernameQuery(username)).join();
        if (account.isPresent() && account.get().premium()) {
            return LoginMode.PREMIUM;
        }
        if (!premiumEnabled) {
            return LoginMode.CRACKED;
        }
        PremiumStatus status = premium.lookup(username);
        if (account.isPresent()) {
            return status == PremiumStatus.PREMIUM ? LoginMode.PREMIUM : LoginMode.CRACKED;
        }
        return switch (status) {
            case PREMIUM -> LoginMode.PREMIUM;
            case CRACKED -> LoginMode.CRACKED;
            case UNKNOWN -> LoginMode.UNDECIDED;
        };
    }
}
