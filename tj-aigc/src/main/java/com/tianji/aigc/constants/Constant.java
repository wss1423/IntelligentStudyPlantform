package com.tianji.aigc.constants;

public interface Constant {

    String REQUEST_ID = "requestId";

    String ORDER_ID = "orderId";

    String USER_ID = "userId";

    String STOP = "STOP";

    String ID = "id";

    interface Tools {
        String QUERY_COURSE_BY_ID = "根据课程id查询课程详细信息";
        String SEARCH_COURSES = "根据关键词搜索课程，当知识库中没有匹配的课程时可调用此方法从数据库中搜索";
        String PRE_PLACE_ORGER = " 购买课程预下单操作";
    }

    interface ToolParams {
        String COURSE_ID = "课程id";
        String COURSE_IDS = "课程id列表";
    }

}