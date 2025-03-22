package rs.raf.stock_service.specification;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.stock_service.domain.dto.ListingFilterDto;
import rs.raf.stock_service.domain.entity.*;

import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ListingSpecification {

    public static Specification<Listing> buildSpecification(ListingFilterDto filter, String role) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Pridružujemo ListingDailyPriceInfo koristeći podupit da nađemo najnoviji zapis
            Subquery<LocalDate> subquery = query.subquery(LocalDate.class);
            Root<ListingDailyPriceInfo> subRoot = subquery.from(ListingDailyPriceInfo.class);
            subquery.select(cb.greatest(subRoot.<LocalDate>get("date")));
            subquery.where(cb.equal(subRoot.get("listing"), root));

            // Join sa ListingDailyPriceInfo
            Join<Listing, ListingDailyPriceInfo> dailyInfoJoin = root.join("listingDailyPriceInfos", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.isNull(dailyInfoJoin.get("date")),
                    cb.equal(dailyInfoJoin.get("date"), subquery)
            ));

            // Ograničenje prikaza po roli
            if ("CLIENT".equalsIgnoreCase(role)) {
                Predicate isStock = cb.equal(root.type(), cb.literal(Stock.class));
                Predicate isFutures = cb.equal(root.type(), cb.literal(FuturesContract.class));
                predicates.add(cb.or(isStock, isFutures));
            }

            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchTerm = "%" + filter.getSearch().toLowerCase() + "%";
                Predicate tickerPredicate = cb.like(cb.lower(root.get("ticker")), searchTerm);
                Predicate namePredicate = cb.like(cb.lower(root.get("name")), searchTerm);
                predicates.add(cb.or(tickerPredicate, namePredicate));
            }

            // Filtriranje po Exchange - prefix
            if (filter.getExchangePrefix() != null && !filter.getExchangePrefix().isEmpty()) {
                Join<Listing, Exchange> exchangeJoin = root.join("exchange");
                predicates.add(cb.like(cb.lower(exchangeJoin.get("acronym")), filter.getExchangePrefix().toLowerCase() + "%"));
            }

            // Filtriranje po Price
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            // Filtriranje po Ask
            if (filter.getMinAsk() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("ask"), filter.getMinAsk()));
            }
            if (filter.getMaxAsk() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("ask"), filter.getMaxAsk()));
            }

            // Filtriranje po Low (Bid)
            if (filter.getMinBid() != null) {
                predicates.add(cb.greaterThanOrEqualTo(dailyInfoJoin.get("low"), filter.getMinBid()));
            }
            if (filter.getMaxBid() != null) {
                predicates.add(cb.lessThanOrEqualTo(dailyInfoJoin.get("low"), filter.getMaxBid()));
            }

            // Filtriranje po Volume
            if (filter.getMinVolume() != null) {
                predicates.add(cb.greaterThanOrEqualTo(dailyInfoJoin.get("volume"), filter.getMinVolume()));
            }
            if (filter.getMaxVolume() != null) {
                predicates.add(cb.lessThanOrEqualTo(dailyInfoJoin.get("volume"), filter.getMaxVolume()));
            }

            // Filtriranje po Maintenance Margin (price * 0.1)
            Expression<BigDecimal> maintenanceMarginExp = cb.prod(root.get("price"), BigDecimal.valueOf(0.1));
            if (filter.getMinMaintenanceMargin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(maintenanceMarginExp, filter.getMinMaintenanceMargin()));
            }
            if (filter.getMaxMaintenanceMargin() != null) {
                predicates.add(cb.lessThanOrEqualTo(maintenanceMarginExp, filter.getMaxMaintenanceMargin()));
            }

            // Filtriranje po Settlement Date (samo za Futures i Options)
            if (filter.getSettlementDate() != null) {
                Predicate isFutures = cb.equal(root.type(), cb.literal(FuturesContract.class));
                Predicate settlementPredicate = cb.equal(cb.treat(root, FuturesContract.class).get("settlementDate"), filter.getSettlementDate());
                predicates.add(cb.or(cb.not(isFutures), settlementPredicate));
            }

            // Sortiranje
            if (filter.getSortBy() != null && !filter.getSortBy().isEmpty()) {
                Expression<?> sortExpression;
                if ("volume".equalsIgnoreCase(filter.getSortBy())) {
                    sortExpression = dailyInfoJoin.get("volume");
                } else if ("maintenanceMargin".equalsIgnoreCase(filter.getSortBy())) {
                    sortExpression = maintenanceMarginExp;
                } else if ("low".equalsIgnoreCase(filter.getSortBy())) {
                    sortExpression = dailyInfoJoin.get("low");
                } else {
                    sortExpression = root.get(filter.getSortBy());
                }

                if ("desc".equalsIgnoreCase(filter.getSortOrder())) {
                    query.orderBy(cb.desc(sortExpression));
                } else {
                    query.orderBy(cb.asc(sortExpression));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}