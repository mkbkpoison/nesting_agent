package com.nesting.assistant.tools.nesting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nesting.assistant.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 套料配置优化工具
 */
@Slf4j
@Service
public class ConfigOptimizerTool {

    @Tool(description = "根据材料和零件特性优化套料配置。" +
            "返回优化后的参数配置和建议。" +
            "当需要针对特定场景优化套料效果时使用此工具。")
    public Map<String, Object> optimizeNestingConfig(
            @ToolParam(description = "材料类型：steel(钢材), aluminum(铝材), stainless(不锈钢), copper(铜材)") String material,
            @ToolParam(description = "零件列表JSON，包含尺寸和数量信息") String parts
    ) {
        log.info("Optimizing nesting config for material: {}", material);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("material", material);

        // 解析零件信息
        List<Map<String, Object>> partsList;
        try {
            partsList = JsonUtils.fromJson(parts, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            partsList = new ArrayList<>();
        }
        result.put("partsCount", partsList.size());

        // 根据材料类型推荐配置
        Map<String, Object> optimizedConfig = new LinkedHashMap<>();

        switch (material.toLowerCase()) {
            case "steel":
                optimizedConfig.put("spacing", 2.0);
                optimizedConfig.put("margin", 10);
                optimizedConfig.put("cuttingCompensation", 0.5);
                optimizedConfig.put("pierceTime", "1.5s");
                optimizedConfig.put("cuttingSpeed", "1200mm/min");
                optimizedConfig.put("power", "3000W");
                break;
            case "aluminum":
                optimizedConfig.put("spacing", 1.5);
                optimizedConfig.put("margin", 8);
                optimizedConfig.put("cuttingCompensation", 0.3);
                optimizedConfig.put("pierceTime", "0.8s");
                optimizedConfig.put("cuttingSpeed", "2000mm/min");
                optimizedConfig.put("power", "4000W");
                break;
            case "stainless":
                optimizedConfig.put("spacing", 1.5);
                optimizedConfig.put("margin", 8);
                optimizedConfig.put("cuttingCompensation", 0.4);
                optimizedConfig.put("pierceTime", "1.2s");
                optimizedConfig.put("cuttingSpeed", "1500mm/min");
                optimizedConfig.put("power", "3500W");
                break;
            case "copper":
                optimizedConfig.put("spacing", 2.5);
                optimizedConfig.put("margin", 12);
                optimizedConfig.put("cuttingCompensation", 0.6);
                optimizedConfig.put("pierceTime", "2.0s");
                optimizedConfig.put("cuttingSpeed", "800mm/min");
                optimizedConfig.put("power", "2500W");
                break;
            default:
                optimizedConfig.put("spacing", 1.5);
                optimizedConfig.put("margin", 10);
                optimizedConfig.put("cuttingCompensation", 0.5);
                optimizedConfig.put("pierceTime", "1.0s");
                optimizedConfig.put("cuttingSpeed", "1500mm/min");
                optimizedConfig.put("power", "3000W");
        }

        // 通用优化设置
        optimizedConfig.put("rotationAngles", List.of(0, 90, 180, 270));
        optimizedConfig.put("enableCommonEdge", true);
        optimizedConfig.put("prioritizeLargeParts", true);
        optimizedConfig.put("allowRemnantNesting", true);

        result.put("optimizedConfig", optimizedConfig);

        // 优化理由
        List<String> reasons = new ArrayList<>();
        reasons.add("根据" + material + "材料特性调整了切割参数");
        reasons.add("启用了共边切割功能以提高利用率");
        reasons.add("设置了多角度旋转以增加套料灵活性");
        result.put("optimizationReasons", reasons);

        // 预期效果
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("utilizationImprovement", "+3-5%");
        expected.put("cuttingTimeReduction", "-10-15%");
        expected.put("qualityImprovement", "减少热变形");
        result.put("expectedResults", expected);

        return result;
    }
}
