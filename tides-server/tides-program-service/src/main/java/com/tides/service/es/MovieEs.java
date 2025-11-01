package com.tides.service.es;

import cn.hutool.core.collection.CollectionUtil;
import com.tides.core.SpringUtil;
import com.tides.page.PageUtil;
import com.tides.page.PageVo;
import com.tides.service.init.MovieDocumentParamName;
import com.tides.service.tool.SearchKeywordUtil;
import com.tides.util.BusinessEsHandle;
import com.tides.util.StringUtil;
import com.tides.vo.MovieListVo;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @description: 电影 ES 搜索
 */
@Slf4j
@Component
public class MovieEs {

    private static final int SEARCH_MAX_SIZE = 500;

    private static final int MAX_FRAGMENT_QUERY_SIZE = 24;

    private static final int MIN_SEARCH_CONTENT_LENGTH = 2;

    private static final List<String> HIGHLIGHT_FIELDS = Arrays.asList(
            MovieDocumentParamName.MOVIE_NAME,
            MovieDocumentParamName.MOVIE_ALIAS,
            MovieDocumentParamName.DIRECTOR,
            MovieDocumentParamName.ACTORS);

    @Autowired
    private BusinessEsHandle businessEsHandle;

    public List<MovieListVo> searchAll(String content) {
        if (!validSearchContent(content)) {
            return new ArrayList<>();
        }
        List<MovieListVo> list = new ArrayList<>();
        try {
            SearchSourceBuilder searchSourceBuilder = buildSearchSource(content.trim(), 1, SEARCH_MAX_SIZE);
            PageInfo<MovieListVo> pageInfo = new PageInfo<>(list);
            pageInfo.setPageNum(1);
            pageInfo.setPageSize(SEARCH_MAX_SIZE);
            businessEsHandle.executeQuery(indexName(), MovieDocumentParamName.INDEX_TYPE, list, pageInfo,
                    MovieListVo.class, searchSourceBuilder, HIGHLIGHT_FIELDS);
        } catch (Exception exception) {
            log.error("movie es searchAll error", exception);
        }
        return list;
    }

    public PageVo<MovieListVo> search(String content, Integer pageNumber, Integer pageSize) {
        PageVo<MovieListVo> pageVo = new PageVo<>();
        if (!validSearchContent(content)) {
            return pageVo;
        }
        List<MovieListVo> list = new ArrayList<>();
        try {
            int currentPage = Objects.isNull(pageNumber) || pageNumber < 1 ? 1 : pageNumber;
            int currentSize = Objects.isNull(pageSize) || pageSize < 1 ? 10 : pageSize;
            SearchSourceBuilder searchSourceBuilder = buildSearchSource(content.trim(), currentPage, currentSize);
            PageInfo<MovieListVo> pageInfo = new PageInfo<>(list);
            pageInfo.setPageNum(currentPage);
            pageInfo.setPageSize(currentSize);
            businessEsHandle.executeQuery(indexName(), MovieDocumentParamName.INDEX_TYPE, list, pageInfo,
                    MovieListVo.class, searchSourceBuilder, HIGHLIGHT_FIELDS);
            pageVo = PageUtil.convertPage(pageInfo, movieListVo -> movieListVo);
        } catch (Exception exception) {
            log.error("movie es search error", exception);
        }
        return pageVo;
    }

    private SearchSourceBuilder buildSearchSource(String content, int pageNumber, int pageSize) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        BoolQueryBuilder keywordQuery = QueryBuilders.boolQuery();
        List<String> fragments = SearchKeywordUtil.fragments(content);
        for (int index = 0; index < fragments.size() && index < MAX_FRAGMENT_QUERY_SIZE; index++) {
            String fragment = fragments.get(index);
            addMovieSearchField(keywordQuery, MovieDocumentParamName.MOVIE_NAME, fragment, 8.0f, 3.0f, 6.0f);
            addMovieSearchField(keywordQuery, MovieDocumentParamName.ACTORS, fragment, 7.0f, 3.0f, 6.0f);
            addMovieSearchField(keywordQuery, MovieDocumentParamName.MOVIE_ALIAS, fragment, 5.0f, 2.0f, 4.0f);
            addMovieSearchField(keywordQuery, MovieDocumentParamName.DIRECTOR, fragment, 4.0f, 2.0f, 3.0f);
        }
        keywordQuery.minimumShouldMatch(1);
        boolQuery.must(keywordQuery);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.trackTotalHits(true);
        searchSourceBuilder.from((pageNumber - 1) * pageSize);
        searchSourceBuilder.size(pageSize);
        searchSourceBuilder.highlighter(getHighlightBuilder(HIGHLIGHT_FIELDS));
        return searchSourceBuilder;
    }

    private void addMovieSearchField(BoolQueryBuilder keywordQuery, String fieldName, String content,
                                     float phraseBoost, float matchBoost, float keywordBoost) {
        if (!SearchKeywordUtil.containsWildcard(content)) {
            keywordQuery.should(QueryBuilders.matchPhraseQuery(fieldName, content).boost(phraseBoost));
            keywordQuery.should(QueryBuilders.matchQuery(fieldName, content)
                    .operator(Operator.OR).minimumShouldMatch("2<70%")
                    .fuzziness(Fuzziness.AUTO).prefixLength(0).maxExpansions(50)
                    .boost(matchBoost));
        }
        keywordQuery.should(QueryBuilders.wildcardQuery(fieldName + ".keyword",
                SearchKeywordUtil.toEsWildcardPattern(content)).boost(keywordBoost));
    }

    private boolean validSearchContent(String content) {
        return SearchKeywordUtil.validContent(content);
    }

    public HighlightBuilder getHighlightBuilder(List<String> fieldNameList) {
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for (String fieldName : fieldNameList) {
            HighlightBuilder.Field highlightField = new HighlightBuilder.Field(fieldName);
            highlightField.preTags("<em>");
            highlightField.postTags("</em>");
            highlightBuilder.field(highlightField);
        }
        return highlightBuilder;
    }

    public String indexName() {
        return SpringUtil.getPrefixDistinctionName() + "-" + MovieDocumentParamName.INDEX_NAME;
    }

    public boolean hasSearchResult(List<MovieListVo> list) {
        return CollectionUtil.isNotEmpty(list);
    }
}
