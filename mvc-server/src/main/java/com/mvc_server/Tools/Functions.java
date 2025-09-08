package com.mvc_server.Tools;

import cn.hutool.json.JSONUtil;
import com.mvc_server.Mapper.SQLMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class Functions {

    private final SQLMapper sqlMapper;

    ArrayList<Method> Methods = new ArrayList<>();

    Integer lewdValue = 0;

    @Tool(description = "Get the current date and time in the user's timezone")
    public String getTime(){
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("时间工具被调用！");
        return sdf.format(date);
    }

    /*@Tool(description = "Get your lewd value")
    public Integer getLewdValue(){
        System.out.println("获取，当前" + lewdValue);
        return lewdValue;
    }

    @Tool(description = "Increase your lewd value")
    public void increaseLewdValue(@ToolParam(description = "The value you want to increase") Integer increase){
        System.out.println("增加 " + increase);
        lewdValue = Math.min(lewdValue + increase,100);
    }

    @Tool(description = "use this to execute a MySQL query,will return a json array")
    public String runSelectSQL(@ToolParam(description = "the MySQL query") String sql) {
        System.out.println("执行的SQL:" + sql);
        List<Object> o = sqlMapper.runSQL(sql);
        String json = JSONUtil.toJsonStr(o);
        System.out.println("执行结果：" + json);
        return json;
    }*/



}
