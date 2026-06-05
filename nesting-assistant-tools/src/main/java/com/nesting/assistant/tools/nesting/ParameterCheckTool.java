package com.nesting.assistant.tools.nesting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nesting.assistant.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 套料参数检查工具
 */
@Slf4j
@Service
public class ParameterCheckTool {

    @Tool(description = "检查套料参数配置是否合理。" +
            "分析间距、边距、旋转角度等参数，返回参数校验结果和优化建议。" +
            "当用户询问套料参数设置或效果不佳时使用此工具。")
    public Map<String, Object> checkNestingParameters(
            @ToolParam(description = "参数配置JSON，包含spacing(间距), margin(边距), rotationAngles(旋转角度数组), targetUtilization(目标利用率)等") String params
    ) {
        log.info("Checking nesting parameters: {}", params);

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // 解析参数
        Map<String, Object> parameters;
        try {
            parameters = JsonUtils.fromJson(params, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            parameters = new HashMap<>();
        }

        // 检查间距参数
        Object spacingObj = parameters.get("spacing");
        double spacing = 0;
        if (spacingObj != null) {
            spacing = Double.parseDouble(spacingObj.toString());
            if (spacing < 0.5) {
                warnings.add("间距过小（" + spacing + "mm），可能导致切割头碰撞");
            } else if (spacing > 10) {
                warnings.add("间距过大（" + spacing + "mm），可能降低材料利用率");
                suggestions.add("建议间距范围：0.5-3mm，激光切割推荐1mm");
            } else {
                suggestions.add("间距设置合理（" + spacing + "mm）");
            }
        } else {
            warnings.add("未设置间距参数，将使用默认值1mm");
        }

        // 检查边距参数
        Object marginObj = parameters.get("margin");
        double margin = 0;
        if (marginObj != null) {
            margin = Double.parseDouble(marginObj.toString());
            if (margin < 5) {
                warnings.add("边距过小（" + margin + "mm），可能导致边缘切割不完整");
            } else if (margin > 50) {
                warnings.add("边距过大（" + margin + "mm），浪费材料");
                suggestions.add("建议边距范围：5-15mm");
            }
        }

        // 检查旋转角度
        Object rotationObj = parameters.get("rotationAngles");
        if (rotationObj != null) {
            List<?> angles = (List<?>) rotationObj;
            if (angles.size() < 4) {
                suggestions.add("增加旋转角度选项可以提高套料利用率");
            }
        }

        // 检查目标利用率
        Object utilizationObj = parameters.get("targetUtilization");
        if (utilizationObj != null) {
            double targetUtil = Double.parseDouble(utilizationObj.toString().replace("%", ""));
            if (targetUtil > 95) {
                warnings.add("目标利用率过高（" + targetUtil + "%），可能导致计算时间过长");
                suggestions.add("建议目标利用率设置在85-92%之间");
            }
        }

        result.put("valid", warnings.isEmpty() || warnings.stream().noneMatch(w -> w.contains("过小") || w.contains("过大")));
        result.put("parameters", parameters);
        result.put("warnings", warnings);
        result.put("suggestions", suggestions);

        // 推荐配置
        Map<String, Object> recommended = new LinkedHashMap<>();
        recommended.put("spacing", "1-2mm (激光切割)");
        recommended.put("margin", "5-10mm");
        recommended.put("rotationAngles", List.of(0, 90, 180, 270));
        recommended.put("targetUtilization", "88-92%");
        recommended.put("algorithm", "遗传算法");
        result.put("recommendedConfig", recommended);

        // 预期效果
        result.put("expectedUtilization", "85-92%");
        result.put("estimatedProcessingTime", "取决于零件数量和复杂度");

        return result;
    }
}
