package org.jeecg.modules.takeaway.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class DictUtil {
    public static List<?> build(List<?> list, String labelRowName, String valueRowName) throws NoSuchFieldException, IllegalAccessException {
        log.info("list {}", list);

        // 此处将传来的 list 构建成，其中 每一条信息的 value 取值需要是 valueRowName，label 取值需要是 labelRowName
        //[{
        //  value: '选项1',
        //  label: '黄金糕'
        //}, {
        //  value: '选项2',
        //  label: '双皮奶'
        //}, {
        //  value: '选项3',
        //  label: '蚵仔煎'
        //}, {
        //  value: '选项4',
        //  label: '龙须面'
        //}, {
        //  value: '选项5',
        //  label: '北京烤鸭'
        //}]

        List<Item> result = new LinkedList<>();

        for (Object object : list) {
            log.info("object {}", object);

            // label
            Field labelField = object.getClass().getDeclaredField(labelRowName);
            log.info("labelField {}", labelField);
            labelField.setAccessible(true);
            Object label = labelField.get(object);
            log.info("label {}", label);

            // value
            Field valueField = object.getClass().getDeclaredField(valueRowName);
            log.info("valueField {}", valueField);
            valueField.setAccessible(true);
            Object value = valueField.get(object);
            log.info("value {}", value);

            // build item
            Item item = new Item(label, value);
            log.info("item {}", item);

            // add item TO result
            result.add(item);
        }
        log.info("result {}", result);

        return result;
    }
}

@Data
@AllArgsConstructor
class Item {
    private Object label;
    private Object value;
}

