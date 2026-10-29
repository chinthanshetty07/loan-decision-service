package com.chinthanshetty.los.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Exact match on a normalized name against a configured list; real screening would use fuzzy matching. */
@Component
public class WatchlistSanctionsScreeningClient implements SanctionsScreeningClient {

    private final Set<String> watchlist;

    public WatchlistSanctionsScreeningClient(@Value("${los.sanctions.watchlist:}") List<String> names) {
        this.watchlist = names.stream().map(WatchlistSanctionsScreeningClient::normalize).collect(Collectors.toSet());
    }

    @Override
    public boolean isPossibleMatch(String fullName) {
        return watchlist.contains(normalize(fullName));
    }

    static String normalize(String name) {
        return name.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
