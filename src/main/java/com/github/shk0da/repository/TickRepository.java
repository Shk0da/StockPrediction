package com.github.shk0da.repository;

import com.github.shk0da.domain.Tick;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface TickRepository extends JpaRepository<Tick, Tick.PK> {
    List<Tick> findAllBySymbolAndTimeFrameAndDatetimeIsBetweenOrderByDatetimeAsc(String symbol, Integer tf, Timestamp start, Timestamp end);
    Tick findFirstBySymbolAndTimeFrameAndDatetimeBetweenOrderByOpenDesc(String symbol, Integer tf, Timestamp start, Timestamp end);
    Tick findFirstBySymbolAndTimeFrameAndDatetimeBetweenOrderByOpenAsc(String symbol, Integer tf, Timestamp start, Timestamp end);
    Tick findFirstBySymbolAndTimeFrameAndDatetimeBetweenOrderByCloseDesc(String symbol, Integer tf, Timestamp start, Timestamp end);
    Tick findFirstBySymbolAndTimeFrameAndDatetimeBetweenOrderByCloseAsc(String symbol, Integer tf, Timestamp start, Timestamp end);
}
