package com.tianji.aigc;

import cn.hutool.core.map.MapUtil;
import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class AppTest {

    @Test

    public void testAppCall() throws Exception {
        // 构造业务参数
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjp7InVzZXJJZCI6Miwicm9sZUlkIjoyLCJyZW1lbWJlck1lIjp0cnVlfSwiZXhwIjoxNzg2NTIwMTQwfQ.Fw3kr9opqo65vbCd7-XIarPpGKeyYic8RzRV9mjoDD9i2cgeXwco99uR3ubtIfXsz4Kb9lTVDQbfde61uqxavyKzDVTjB7aRtF9sjjHZ4QA-z5dalQ_jxdH-tPOn-fo4AQMg5MpQX3-7-UWE1qT9bgxDtdrGGXxmc5s5Rw1EMWeFLF7ZQyr83A0LaPgg2eVdmJrCD3MtImYfI8N4XNkytv921CxckLvMURb_kIZ_0kOUq0gr_RWymc-b0YKLiMM_aR3hF4xiSJOS-_B6d9GUmAp95IRGpcRVaqt7MuoVDKkk-yXZE_8niX7ApR2B1g0qS3QbHfSCu9SUnN3Smu20og";
        Map<String, Object> bizParams = MapUtil.<String, Object>builder()
                .put("user_defined_tokens", MapUtil.of("tool_c176f289-7489-4279-bdd5-bb7100ae9623", // 工具id
                        MapUtil.of("user_token", token)))
                .build();

        // bizParams.add("user_defined_tokens", JsonObject);
        ApplicationParam param = ApplicationParam.builder()
                // 若没有配置环境变量，可用百炼API Key将下行替换为：.apiKey("sk-xxx")。但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
                .apiKey("sk-xxxx")
                .appId("a9f45faeae024c099f6e720569b74744") // 智能体id
                .prompt("查询课程，id为：1880533253575225346")
                .incrementalOutput(true) // 开启增量输出
                .bizParams(JsonUtils.toJsonObject(bizParams))
                .build();

        Application application = new Application();
        Flowable<ApplicationResult> result = application.streamCall(param);

        // 阻塞式的打印内容
        result.blockingForEach(data -> {
            System.out.printf("%s\n",data.getOutput().getText());
        });

    }

}
