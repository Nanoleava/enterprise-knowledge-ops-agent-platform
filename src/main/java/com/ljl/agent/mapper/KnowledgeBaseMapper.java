package com.ljl.agent.mapper;

import com.ljl.agent.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper {

    int insert(KnowledgeBase knowledgeBase);

    KnowledgeBase selectById(Long id);

    List<KnowledgeBase> selectAll();

    List<KnowledgeBase> selectByUserId(@Param("userId") Long userId);

    KnowledgeBase selectByUserIdAndName(
            @Param("userId") Long userId,
            @Param("name") String name
    );
}
