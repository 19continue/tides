package com.tides.service.es;

import cn.hutool.core.collection.CollectionUtil;
import com.tides.core.SpringUtil;
import com.tides.dto.EsDataQueryDto;
import com.tides.dto.ProgramListDto;
import com.tides.dto.ProgramPageListDto;
import com.tides.dto.ProgramRecommendListDto;
import com.tides.dto.ProgramSearchDto;
import com.tides.enums.BusinessStatus;
import com.tides.page.PageUtil;
import com.tides.page.PageVo;
import com.tides.service.init.ProgramDocumentParamName;
import com.tides.service.tool.ProgramPageOrder;
import com.tides.service.tool.SearchKeywordUtil;
import com.tides.util.BusinessEsHandle;
import com.tides.util.StringUtil;
import com.tides.vo.ProgramHomeVo;
import com.tides.vo.ProgramListVo;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @description: es查询
 * @author: 19continue
 **/
@Slf4j
@Component
public class ProgramEs {

    private static final int MAX_FRAGMENT_QUERY_SIZE = 24;
    
    @Autowired
    private BusinessEsHandle businessEsHandle;
    
    public List<ProgramHomeVo> selectHomeList(ProgramListDto programListDto) {
        List<ProgramHomeVo> programHomeVoList = new ArrayList<>();
        
        try {
            //按照父节目id集合来进行循环从es中查询，主页页面是4个父节目，也就是循环4次
            for (Long parentProgramCategoryId : programListDto.getParentProgramCategoryIds()) {
                List<EsDataQueryDto> programEsQueryDto = new ArrayList<>();
                if (CollectionUtil.isNotEmpty(programListDto.getAreaIds())) {
                    //地区id
                    EsDataQueryDto areaIdQueryDto = new EsDataQueryDto();
                    areaIdQueryDto.setParamName(ProgramDocumentParamName.AREA_ID);
                    areaIdQueryDto.setParamValue(programListDto.getAreaIds());
                    programEsQueryDto.add(areaIdQueryDto);
                } else if (Objects.nonNull(programListDto.getAreaId())) {
                    //地区id
                    EsDataQueryDto areaIdQueryDto = new EsDataQueryDto();
                    areaIdQueryDto.setParamName(ProgramDocumentParamName.AREA_ID);
                    areaIdQueryDto.setParamValue(programListDto.getAreaId());
                    programEsQueryDto.add(areaIdQueryDto);
                }else {
                    EsDataQueryDto primeQueryDto = new EsDataQueryDto();
                    primeQueryDto.setParamName(ProgramDocumentParamName.PRIME);
                    primeQueryDto.setParamValue(BusinessStatus.YES.getCode());
                    programEsQueryDto.add(primeQueryDto);
                }
                
                //父节目类型id集合
                EsDataQueryDto parentProgramCategoryIdQueryDto = new EsDataQueryDto();
                parentProgramCategoryIdQueryDto.setParamName(ProgramDocumentParamName.PARENT_PROGRAM_CATEGORY_ID);
                parentProgramCategoryIdQueryDto.setParamValue(parentProgramCategoryId);
                programEsQueryDto.add(parentProgramCategoryIdQueryDto);
                //查询前7条
                PageInfo<ProgramListVo> pageInfo = businessEsHandle.queryPage(
                        SpringUtil.getPrefixDistinctionName() + "-" + ProgramDocumentParamName.INDEX_NAME,
                        ProgramDocumentParamName.INDEX_TYPE, programEsQueryDto, 1, 7, ProgramListVo.class);
                if (!pageInfo.getList().isEmpty()) {
                    ProgramHomeVo programHomeVo = new ProgramHomeVo();
                    programHomeVo.setCategoryName(pageInfo.getList().get(0).getParentProgramCategoryName());
                    programHomeVo.setCategoryId(pageInfo.getList().get(0).getParentProgramCategoryId());
                    programHomeVo.setProgramListVoList(pageInfo.getList());
                    programHomeVoList.add(programHomeVo);
                }
            }
        }catch (Exception e) {
            log.error("businessEsHandle.queryPage error",e);
        }
        return programHomeVoList;
    }
    
    public List<ProgramListVo> recommendList(ProgramRecommendListDto programRecommendListDto) {
        List<ProgramListVo> programListVoList = new ArrayList<>();
        try {
            boolean allQueryFlag = true;
            MatchAllQueryBuilder matchAllQueryBuilder = QueryBuilders.matchAllQuery();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (CollectionUtil.isNotEmpty(programRecommendListDto.getAreaIds())) {
                allQueryFlag = false;
                QueryBuilder builds = QueryBuilders.termsQuery(ProgramDocumentParamName.AREA_ID,
                        programRecommendListDto.getAreaIds());
                boolQuery.must(builds);
            } else if (Objects.nonNull(programRecommendListDto.getAreaId())) {
                allQueryFlag = false;
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.AREA_ID, 
                        programRecommendListDto.getAreaId());
                boolQuery.must(builds);
            }
            if (Objects.nonNull(programRecommendListDto.getParentProgramCategoryId())) {
                allQueryFlag = false;
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.PARENT_PROGRAM_CATEGORY_ID, 
                        programRecommendListDto.getParentProgramCategoryId());
                boolQuery.must(builds);
            }
            if (Objects.nonNull(programRecommendListDto.getProgramId())) {
                allQueryFlag = false;
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.ID,
                        programRecommendListDto.getProgramId());
                boolQuery.mustNot(builds);
            }
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(allQueryFlag ? matchAllQueryBuilder : boolQuery);
            searchSourceBuilder.trackTotalHits(true);
            searchSourceBuilder.from(1);
            searchSourceBuilder.size(10);
           
            Script script = new Script("Math.random()");
            ScriptSortBuilder scriptSortBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
            scriptSortBuilder.order(SortOrder.ASC);
            
            searchSourceBuilder.sort(scriptSortBuilder);
            
            businessEsHandle.executeQuery(
                    SpringUtil.getPrefixDistinctionName() + "-" + ProgramDocumentParamName.INDEX_NAME,
                    ProgramDocumentParamName.INDEX_TYPE,programListVoList,null,ProgramListVo.class,
                    searchSourceBuilder,null);
        }catch (Exception e) {
            log.error("recommendList error",e);
        }
        return programListVoList;
    }
    
    
    public PageVo<ProgramListVo> selectPage(ProgramPageListDto programPageListDto) {
        PageVo<ProgramListVo> pageVo = new PageVo<>();
        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (CollectionUtil.isNotEmpty(programPageListDto.getAreaIds())) {
                QueryBuilder builds = QueryBuilders.termsQuery(ProgramDocumentParamName.AREA_ID,
                        programPageListDto.getAreaIds());
                boolQuery.must(builds);
            } else if (Objects.nonNull(programPageListDto.getAreaId())) {
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.AREA_ID,
                        programPageListDto.getAreaId());
                boolQuery.must(builds);
            }else {
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.PRIME,
                        BusinessStatus.YES.getCode());
                boolQuery.must(builds);
            }
            if (Objects.nonNull(programPageListDto.getParentProgramCategoryId())) {
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.PARENT_PROGRAM_CATEGORY_ID,
                        programPageListDto.getParentProgramCategoryId());
                boolQuery.must(builds);
            } else {
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.PARENT_PROGRAM_CATEGORY_ID, 22L);
                boolQuery.mustNot(builds);
            }
            if (Objects.nonNull(programPageListDto.getProgramCategoryId())) {
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.PROGRAM_CATEGORY_ID,
                        programPageListDto.getProgramCategoryId());
                boolQuery.must(builds);
            }
            if (Objects.nonNull(programPageListDto.getStartDateTime()) && 
                    Objects.nonNull(programPageListDto.getEndDateTime())) {
                QueryBuilder builds = QueryBuilders.rangeQuery(ProgramDocumentParamName.SHOW_DAY_TIME)
                        .from(programPageListDto.getStartDateTime()).to(programPageListDto.getEndDateTime())
                        .includeLower(true);
                boolQuery.must(builds);
            }
            
            ProgramPageOrder programPageOrder = getProgramPageOrder(programPageListDto);
            if (Objects.nonNull(programPageOrder.sortParam) && Objects.nonNull(programPageOrder.sortOrder)) {
                FieldSortBuilder sort = SortBuilders.fieldSort(programPageOrder.sortParam);
                sort.order(programPageOrder.sortOrder);
                searchSourceBuilder.sort(sort);
            }
            searchSourceBuilder.query(boolQuery);
            searchSourceBuilder.trackTotalHits(true);
            searchSourceBuilder.from((programPageListDto.getPageNumber() - 1) * programPageListDto.getPageSize());
            searchSourceBuilder.size(programPageListDto.getPageSize());

            List<ProgramListVo> list = new ArrayList<>();
            PageInfo<ProgramListVo> pageInfo = new PageInfo<>(list);
            pageInfo.setPageNum(programPageListDto.getPageNumber());
            pageInfo.setPageSize(programPageListDto.getPageSize());
            
            businessEsHandle.executeQuery(
                    SpringUtil.getPrefixDistinctionName() + "-" + ProgramDocumentParamName.INDEX_NAME,
                    ProgramDocumentParamName.INDEX_TYPE, list, pageInfo, ProgramListVo.class, searchSourceBuilder, null);
            pageVo = PageUtil.convertPage(pageInfo, programListVo -> programListVo);
        }catch (Exception e) {
            log.error("selectPage error",e);
        }
        return pageVo;
    }
    
    public ProgramPageOrder getProgramPageOrder(ProgramPageListDto programPageListDto){
        ProgramPageOrder programPageOrder = new ProgramPageOrder();
        switch (programPageListDto.getType()) {
            //推荐排序
            case 2:
                programPageOrder.sortParam = ProgramDocumentParamName.HIGH_HEAT;
                programPageOrder.sortOrder = SortOrder.DESC;
                break;
            //最近开场    
            case 3:
                programPageOrder.sortParam = ProgramDocumentParamName.SHOW_TIME;
                programPageOrder.sortOrder = SortOrder.ASC;
                break;
            //最新上架    
            case 4:
                programPageOrder.sortParam = ProgramDocumentParamName.ISSUE_TIME;
                programPageOrder.sortOrder = SortOrder.ASC;
                break;
            //相关度排序    
            default:
                programPageOrder.sortParam = null;
                programPageOrder.sortOrder = null;
        }
        return programPageOrder;
    }
    
    public PageVo<ProgramListVo> search(ProgramSearchDto programSearchDto) {
        PageVo<ProgramListVo> pageVo = new PageVo<>();
        try {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            if (CollectionUtil.isNotEmpty(programSearchDto.getAreaIds())) {
                QueryBuilder builds = QueryBuilders.termsQuery(ProgramDocumentParamName.AREA_ID,
                        programSearchDto.getAreaIds());
                boolQuery.must(builds);
            } else if (Objects.nonNull(programSearchDto.getAreaId())) {
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.AREA_ID, programSearchDto.getAreaId());
                boolQuery.must(builds);
            }
            if (Objects.nonNull(programSearchDto.getParentProgramCategoryId())) {
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.PARENT_PROGRAM_CATEGORY_ID, programSearchDto.getParentProgramCategoryId());
                boolQuery.must(builds);
            }
            QueryBuilder movieBuilds = QueryBuilders.termQuery(ProgramDocumentParamName.PARENT_PROGRAM_CATEGORY_ID, 22L);
            boolQuery.mustNot(movieBuilds);
            if (Objects.nonNull(programSearchDto.getProgramCategoryId())) {
                QueryBuilder builds = QueryBuilders.termQuery(ProgramDocumentParamName.PROGRAM_CATEGORY_ID, programSearchDto.getProgramCategoryId());
                boolQuery.must(builds);
            }
            if (Objects.nonNull(programSearchDto.getStartDateTime()) &&
                    Objects.nonNull(programSearchDto.getEndDateTime())) {
                QueryBuilder builds = QueryBuilders.rangeQuery(ProgramDocumentParamName.SHOW_DAY_TIME)
                        .from(programSearchDto.getStartDateTime()).to(programSearchDto.getEndDateTime()).includeLower(true);
                boolQuery.must(builds);
            }
            if (StringUtil.isNotEmpty(programSearchDto.getContent())) {
                String searchContent = programSearchDto.getContent().trim();
                if (StringUtil.isNotEmpty(searchContent)) {
                    BoolQueryBuilder innerBoolQuery = QueryBuilders.boolQuery();
                    List<String> fragments = SearchKeywordUtil.fragments(searchContent);
                    for (int index = 0; index < fragments.size() && index < MAX_FRAGMENT_QUERY_SIZE; index++) {
                        String fragment = fragments.get(index);
                        addProgramSearchField(innerBoolQuery, ProgramDocumentParamName.TITLE, fragment,
                                6.0f, 2.0f, 4.0f);
                        addProgramSearchField(innerBoolQuery, ProgramDocumentParamName.ACTOR, fragment,
                                5.0f, 2.0f, 4.0f);
                    }
                    innerBoolQuery.minimumShouldMatch(1);
                    boolQuery.must(innerBoolQuery);
                }
            }
            
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            ProgramPageOrder programPageOrder = getProgramPageOrder(programSearchDto);
            if (Objects.nonNull(programPageOrder.sortParam) && Objects.nonNull(programPageOrder.sortOrder)) {
                FieldSortBuilder sort = SortBuilders.fieldSort(programPageOrder.sortParam);
                sort.order(programPageOrder.sortOrder);
                searchSourceBuilder.sort(sort);
            }
            searchSourceBuilder.query(boolQuery);
            searchSourceBuilder.trackTotalHits(true);
            searchSourceBuilder.from((programSearchDto.getPageNumber() - 1) * programSearchDto.getPageSize());
            searchSourceBuilder.size(programSearchDto.getPageSize());
            searchSourceBuilder.highlighter(getHighlightBuilder(Arrays.asList(ProgramDocumentParamName.TITLE,
                    ProgramDocumentParamName.ACTOR)));
            List<ProgramListVo> list = new ArrayList<>();
            PageInfo<ProgramListVo> pageInfo = new PageInfo<>(list);
            pageInfo.setPageNum(programSearchDto.getPageNumber());
            pageInfo.setPageSize(programSearchDto.getPageSize());
            businessEsHandle.executeQuery(SpringUtil.getPrefixDistinctionName() + "-" + ProgramDocumentParamName.INDEX_NAME,
                    ProgramDocumentParamName.INDEX_TYPE,list,pageInfo,ProgramListVo.class,
                    searchSourceBuilder,Arrays.asList(ProgramDocumentParamName.TITLE,ProgramDocumentParamName.ACTOR));
            pageVo = PageUtil.convertPage(pageInfo,programListVo -> programListVo);
        }catch (Exception e) {
            log.error("search error",e);
        }
        return pageVo;
    }

    private void addProgramSearchField(BoolQueryBuilder keywordQuery, String fieldName, String content,
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
    
    public HighlightBuilder getHighlightBuilder(List<String> fieldNameList){
        // 创建一个HighlightBuilder
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for (String fieldName : fieldNameList) {
            // 为特定字段添加高亮设置
            HighlightBuilder.Field highlightTitle = new HighlightBuilder.Field(fieldName);
            highlightTitle.preTags("<em>");
            highlightTitle.postTags("</em>");
            highlightBuilder.field(highlightTitle);
        }
        return highlightBuilder;
    }
    
    public void deleteByProgramId(Long programId){
        try {
            List<EsDataQueryDto> esDataQueryDtoList = new ArrayList<>();
            EsDataQueryDto programIdDto = new EsDataQueryDto();
            programIdDto.setParamName(ProgramDocumentParamName.ID);
            programIdDto.setParamValue(programId);
            esDataQueryDtoList.add(programIdDto);
            
            List<ProgramListVo> programListVos = 
                    businessEsHandle.query(
                            SpringUtil.getPrefixDistinctionName() + "-" + ProgramDocumentParamName.INDEX_NAME, 
                            ProgramDocumentParamName.INDEX_TYPE, 
                            esDataQueryDtoList, 
                            ProgramListVo.class);
            if (CollectionUtil.isNotEmpty(programListVos)) {
                for (ProgramListVo programListVo : programListVos) {
                    businessEsHandle.deleteByDocumentId(
                            SpringUtil.getPrefixDistinctionName() + "-" + ProgramDocumentParamName.INDEX_NAME,
                            programListVo.getEsId());
                }
            }
        }catch (Exception e) {
            log.error("deleteByProgramId error",e);
        }
    }
}
