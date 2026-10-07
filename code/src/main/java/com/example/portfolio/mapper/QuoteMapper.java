package com.example.portfolio.mapper;

import com.example.portfolio.dto.response.QuoteResponse;
import com.example.portfolio.dto.response.SymbolSuggestionResponse;
import com.example.portfolio.service.market.Quote;
import com.example.portfolio.service.market.SymbolSuggestion;

public class QuoteMapper {

    private QuoteMapper() {
    }

    public static QuoteResponse toResponse(Quote quote) {
        return new QuoteResponse(
                quote.assetId(), quote.symbol(), quote.name(), quote.assetType(), quote.exchange(),
                quote.price(), quote.previousClose(), quote.change(), quote.changePercent(),
                quote.asOf(), quote.priceSource());
    }

    public static SymbolSuggestionResponse toSuggestionResponse(SymbolSuggestion suggestion) {
        return new SymbolSuggestionResponse(
                suggestion.symbol(), suggestion.name(), suggestion.assetType(), suggestion.exchange(),
                suggestion.assetId());
    }
}
