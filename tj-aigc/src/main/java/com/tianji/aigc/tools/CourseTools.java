package com.tianji.aigc.tools;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseTools {

    private final CourseClient courseClient;

    private final static String FIELD_NAME_FORMAT = "{}_{}"; //提取格式字符常量

    /**
     * 根据课程id查询课程信息
     *
     * @param courseId 课程id
     * @return 课程信息
     */
    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId,ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> CourseInfo.of(this.courseClient.baseInfo(id, true)))
                // 存储课程相关信息
                .map(courseInfo -> {
                    // 存储数据的字段名
                    String field = StrUtil.format(
                            FIELD_NAME_FORMAT,  // 格式
                            StrUtil.lowerFirst(CourseInfo.class.getSimpleName()),
                            courseInfo.getId());
                    // 存储的key
                    var requestId = Convert.toStr(toolContext.getContext().get(Constant.REQUEST_ID));
                    ToolResultHolder.put(requestId,field,courseInfo);
                    return courseInfo;
                })
                .orElse(null);
    }

    /**
     * 根据关键词搜索课程，当知识库中没有匹配的课程时可调用此方法从数据库中搜索
     *
     * @param keyword 搜索关键词，如课程名称、技术方向等
     * @return 匹配的课程列表，每行包含课程id、名称、价格、适用人群等信息
     */
    @Tool(description = Constant.Tools.SEARCH_COURSES)
    public String searchCourses(@ToolParam(description = "搜索关键词，如课程名称、技术方向") String keyword) {
        var ids = this.courseClient.queryCourseIdByName(keyword);
        if (ids == null || ids.isEmpty()) {
            return "未找到匹配「" + keyword + "」的课程";
        }

        var sb = new StringBuilder();
        sb.append("找到以下与「").append(keyword).append("」相关的课程：\n\n");

        for (var id : ids) {
            try {
                var baseInfo = this.courseClient.baseInfo(id, true);
                if (baseInfo != null) {
                    sb.append("课程ID：").append(baseInfo.getId()).append("\n");
                    sb.append("课程名称：").append(baseInfo.getName()).append("\n");
                    sb.append("价格：").append(baseInfo.getPrice() != null ? baseInfo.getPrice() / 100.0 : 0).append("元\n");
                    sb.append("适用人群：").append(baseInfo.getUsePeople() != null ? baseInfo.getUsePeople() : "无").append("\n");
                    sb.append("课程介绍：").append(baseInfo.getIntroduce() != null ? baseInfo.getIntroduce() : "无").append("\n");
                    sb.append("---\n");
                }
            } catch (Exception e) {
                log.error("查询课程{}详情失败", id, e);
            }
        }

        return sb.toString();
    }
}