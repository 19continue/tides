package com.tides.service.init;

import com.tides.BusinessThreadPool;
import com.tides.dto.EsDocumentMappingDto;
import com.tides.initialize.base.AbstractApplicationPostConstructHandler;
import com.tides.service.MovieService;
import com.tides.service.es.MovieEs;
import com.tides.util.BusinessEsHandle;
import com.tides.vo.MovieListVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 电影 ES 索引初始化
 */
@Slf4j
@Component
public class MovieElasticsearchInitData extends AbstractApplicationPostConstructHandler {

    @Autowired
    private BusinessEsHandle businessEsHandle;

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieEs movieEs;

    @Override
    public Integer executeOrder() {
        return 4;
    }

    @Override
    public void executeInit(ConfigurableApplicationContext context) {
        BusinessThreadPool.execute(() -> {
            try {
                initElasticsearchData();
            } catch (Exception exception) {
                log.error("movie executeInit error", exception);
            }
        });
    }

    public void initElasticsearchData() {
        if (!indexAdd()) {
            return;
        }
        for (MovieListVo movieListVo : movieService.listForSearchIndex()) {
            businessEsHandle.add(movieEs.indexName(), MovieDocumentParamName.INDEX_TYPE, buildDocument(movieListVo));
        }
    }

    public boolean indexAdd() {
        boolean result = businessEsHandle.checkIndex(movieEs.indexName(), MovieDocumentParamName.INDEX_TYPE);
        if (result) {
            businessEsHandle.deleteIndex(movieEs.indexName());
        }
        try {
            businessEsHandle.createIndex(movieEs.indexName(), MovieDocumentParamName.INDEX_TYPE, getEsMapping());
            return true;
        } catch (Exception exception) {
            log.error("movie createIndex error", exception);
        }
        return false;
    }

    private Map<String, Object> buildDocument(MovieListVo movieListVo) {
        Map<String, Object> map = new HashMap<>(32);
        map.put(MovieDocumentParamName.MOVIE_ID, movieListVo.getMovieId());
        map.put(MovieDocumentParamName.PROGRAM_ID, movieListVo.getProgramId());
        map.put(MovieDocumentParamName.MOVIE_NAME, movieListVo.getMovieName());
        map.put(MovieDocumentParamName.MOVIE_ALIAS, movieListVo.getMovieAlias());
        map.put(MovieDocumentParamName.DIRECTOR, movieListVo.getDirector());
        map.put(MovieDocumentParamName.ACTORS, movieListVo.getActors());
        map.put(MovieDocumentParamName.DURATION_MINUTES, movieListVo.getDurationMinutes());
        map.put(MovieDocumentParamName.LANGUAGE, movieListVo.getLanguage());
        map.put(MovieDocumentParamName.REGION, movieListVo.getRegion());
        map.put(MovieDocumentParamName.RELEASE_DATE, movieListVo.getReleaseDate());
        map.put(MovieDocumentParamName.POSTER, movieListVo.getPoster());
        map.put(MovieDocumentParamName.DESCRIPTION, movieListVo.getDescription());
        map.put(MovieDocumentParamName.GENRE, movieListVo.getGenre());
        map.put(MovieDocumentParamName.RELEASE_STATUS, movieListVo.getReleaseStatus());
        map.put(MovieDocumentParamName.WANT_WATCH_COUNT, movieListVo.getWantWatchCount());
        map.put(MovieDocumentParamName.WATCHED_COUNT, movieListVo.getWatchedCount());
        map.put(MovieDocumentParamName.RATING_SCORE, movieListVo.getRatingScore());
        map.put(MovieDocumentParamName.LOWEST_PRICE, movieListVo.getLowestPrice());
        map.put(MovieDocumentParamName.NEAREST_SHOW_TIME, movieListVo.getNearestShowTime());
        map.put(MovieDocumentParamName.CINEMA_COUNT, movieListVo.getCinemaCount());
        map.put(MovieDocumentParamName.SCREENING_COUNT, movieListVo.getScreeningCount());
        return map;
    }

    public List<EsDocumentMappingDto> getEsMapping() {
        List<EsDocumentMappingDto> list = new ArrayList<>();
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.MOVIE_ID, "long"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.PROGRAM_ID, "long"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.MOVIE_NAME, "text"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.MOVIE_ALIAS, "text"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.DIRECTOR, "text"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.ACTORS, "text"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.DURATION_MINUTES, "integer"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.LANGUAGE, "text"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.REGION, "text"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.RELEASE_DATE, "date"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.POSTER, "text"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.DESCRIPTION, "text"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.GENRE, "text"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.RELEASE_STATUS, "integer"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.WANT_WATCH_COUNT, "long"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.WATCHED_COUNT, "long"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.RATING_SCORE, "double"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.LOWEST_PRICE, "double"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.NEAREST_SHOW_TIME, "date"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.CINEMA_COUNT, "integer"));
        list.add(new EsDocumentMappingDto(MovieDocumentParamName.SCREENING_COUNT, "integer"));
        return list;
    }
}
