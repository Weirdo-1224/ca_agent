package org.example.ca_agent.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.ca_agent.entity.ClaimEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClaimRepository extends BaseMapper<ClaimEntity> {
}
