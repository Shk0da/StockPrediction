package com.github.shk0da.repository;

import com.github.shk0da.domain.Tick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface TickRepository extends JpaRepository<Tick, Tick.PK> {
    List<Tick> findAllBySymbolAndDatetimeIsBetweenOrderByDatetimeAsc(String symbol, Timestamp start, Timestamp end);
    Tick findTickBySymbolAndDatetimeBetweenOrderByOpenDesc(String symbol, Timestamp start, Timestamp end);
    Tick findTickBySymbolAndDatetimeBetweenOrderByOpenAsc(String symbol, Timestamp start, Timestamp end);
    Tick findTickBySymbolAndDatetimeBetweenOrderByCloseDesc(String symbol, Timestamp start, Timestamp end);
    Tick findTickBySymbolAndDatetimeBetweenOrderByCloseAsc(String symbol, Timestamp start, Timestamp end);
}
