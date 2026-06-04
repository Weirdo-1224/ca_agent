package org.example.ca_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class ReportEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reportId;
    private String taskId;
    private String reportTitle;
    private String reportFormat;
    private String sectionsJson;
    private String sourceListJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
