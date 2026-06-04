package org.example.ca_agent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ca_agent.entity.RepairInstructionEntity;

@Mapper
public interface RepairInstructionRepository extends BaseMapper<RepairInstructionEntity> {
}
