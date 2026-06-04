package org.example.ca_agent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.ca_agent.entity.EvidenceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EvidenceRepository extends BaseMapper<EvidenceEntity> {
}
