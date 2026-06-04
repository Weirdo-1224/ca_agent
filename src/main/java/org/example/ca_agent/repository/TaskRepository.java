package org.example.ca_agent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskRepository extends BaseMapper<AnalysisTaskEntity> {
}
