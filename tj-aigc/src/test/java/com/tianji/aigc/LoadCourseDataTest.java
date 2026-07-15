package com.tianji.aigc;

import com.tianji.api.client.course.CourseClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
class LoadCourseDataTest {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private CourseClient courseClient;

    @Test
    public void loadCourses() {
        // 从course-service查询所有课程
        var ids = List.of(
                1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
                1549025085494521857L, // java泛型
                1552558707325374467L, // 前端工程师
                1589888774267072513L, // Java程序员的前端课程
                1589905661084430337L, // 微服务技术栈
                1880521847886917634L, // 互联网产品运营实战
                1880522777017528321L, // 高级产品运营策略与实践
                1880524734406930434L, // 产品运营入门实战
                1880528608253521922L, // 游戏运营与用户增长策略
                1880529463279169537L, // Java高级架构与微服务
                1880529742615621634L, // Java进阶编程实战
                1880531521654829057L, // 高级Java开发与架构设计
                1880532172006830082L, // Java大数据处理与分析
                1880532674207625218L, // Java进阶与企业级应用开发
                1880533253575225346L  // Java开发零基础入门
        );

        List<Document> documents = new ArrayList<>();

        for (Long id : ids) {
            try {
                var baseInfo = courseClient.baseInfo(id, true);
                if (baseInfo == null) {
                    log.warn("课程{}不存在", id);
                    continue;
                }
                // 构建文档内容
                var docText = String.format(
                        "课程ID：%s\n课程名称：%s\n价格：%s元\n适用人群：%s\n课程介绍：%s\n课程详情：%s",
                        baseInfo.getId(),
                        baseInfo.getName(),
                        baseInfo.getPrice() != null ? baseInfo.getPrice() / 100.0 : 0,
                        baseInfo.getUsePeople() != null ? baseInfo.getUsePeople() : "",
                        baseInfo.getIntroduce() != null ? baseInfo.getIntroduce() : "",
                        baseInfo.getDetail() != null ? baseInfo.getDetail() : ""
                );
                documents.add(Document.builder()
                        .text(docText)
                        .metadata(Map.of(
                                "courseId", baseInfo.getId(),
                                "name", baseInfo.getName() != null ? baseInfo.getName() : ""
                        ))
                        .build());
                log.info("已加载课程：{} - {}", baseInfo.getId(), baseInfo.getName());
            } catch (Exception e) {
                log.error("查询课程{}失败: {}", id, e.getMessage());
            }
        }

        if (!documents.isEmpty()) {
            // DashScope embedding API 每次最多处理10条，分批添加
            int batchSize = 10;
            for (int i = 0; i < documents.size(); i += batchSize) {
                var batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
                vectorStore.add(batch);
                log.info("已加载第{}-{}条（共{}条）", i + 1, i + batch.size(), documents.size());
            }
            log.info("成功加载{}门课程到向量库", documents.size());
        }
    }
}
