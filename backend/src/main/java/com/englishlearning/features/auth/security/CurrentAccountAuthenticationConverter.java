package com.englishlearning.features.auth.security;

import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.user.service.CurrentAccountService;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;

@Component
public class CurrentAccountAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final CurrentAccountService accounts;

    public CurrentAccountAuthenticationConverter(CurrentAccountService accounts) { this.accounts = accounts; }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Called by the resource server only after signature and claim validation.
        AccountSnapshot account;
        try {
            account = accounts.findById(Long.valueOf(jwt.getSubject())).orElse(null);
        } catch (DataAccessResourceFailureException | QueryTimeoutException
                | CannotCreateTransactionException | TransactionTimedOutException exception) {
            throw new AccountLookupFailure(true);
        } catch (RuntimeException exception) {
            throw new AccountLookupFailure(false);
        }
        if (account == null || account.locked()) {
            throw new BadCredentialsException("Authentication required.");
        }
        var principal = new AccountPrincipal(account.id(), account.role(), account.emailVerified());
        // A Teacher role is not approved onboarding, ownership or business-operation permission.
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name())));
    }
}
