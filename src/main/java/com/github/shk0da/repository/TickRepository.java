package com.github.shk0da.repository;

import com.github.shk0da.domain.Tick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface TickRepository extends JpaRepository<Tick, Tick.PK> {
    List<Tick> findAllBySymbolAndDatetimeIsBetweenOrderByDatetimeAsc(String symbol, Timestamp start, Timestamp end);
    Tick findFirstBySymbolAndDatetimeBetweenOrderByOpenDesc(String symbol, Timestamp start, Timestamp end);
    Tick findFirstBySymbolAndDatetimeBetweenOrderByOpenAsc(String symbol, Timestamp start, Timestamp end);
    Tick findFirstBySymbolAndDatetimeBetweenOrderByCloseDesc(String symbol, Timestamp start, Timestamp end);
    Tick findFirstBySymbolAndDatetimeBetweenOrderByCloseAsc(String symbol, Timestamp start, Timestamp end);
}
