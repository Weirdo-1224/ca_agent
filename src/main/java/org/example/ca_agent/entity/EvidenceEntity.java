package org.example.ca_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evidence")
public class EvidenceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String evidenceId;
    private String taskId;
    private String productName;
    private String sourceType;
    private String sourceTitle;
    private String url;
    private String contentSnippet;
    private LocalDateTime collectedAt;
    private String reliability;
    private String usedForJson;
    private LocalDateTime createdAt;
}
